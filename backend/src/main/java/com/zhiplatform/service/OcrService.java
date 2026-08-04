package com.zhiplatform.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.UUID;

/**
 * 云端 PaddleOCR（AI Studio）调用服务。
 * 流程：提交任务(异步) -> 轮询状态 -> 拉取 JSONL 结果 -> 拼接 Markdown 文本。
 */
@Service
public class OcrService {

    private final String jobUrl;
    private final String token;
    private final String model;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(30))
            .build();

    private static final long OCR_TIMEOUT_MS = 6 * 60 * 1000L; // 6 分钟
    private static final long POLL_INTERVAL_MS = 3000L;

    public OcrService(@Value("${paddle-ocr.job-url}") String jobUrl,
                      @Value("${paddle-ocr.token}") String token,
                      @Value("${paddle-ocr.model}") String model) {
        this.jobUrl = jobUrl;
        this.token = token;
        this.model = model;
    }

    /**
     * 对文档/图片执行 OCR，返回识别出的 Markdown 文本。
     */
    public String recognize(String filename, byte[] fileBytes) throws IOException, InterruptedException {
        String jobId = submitJob(filename, fileBytes);
        String jsonlUrl = pollJob(jobId);
        return fetchMarkdown(jsonlUrl);
    }

    private String submitJob(String filename, byte[] fileBytes) throws IOException, InterruptedException {
        String boundary = "----ZhiPlatformOcr" + UUID.randomUUID();
        String optional = "{\"useDocOrientationClassify\":false,\"useDocUnwarping\":false,\"useChartRecognition\":false}";

        StringBuilder sb = new StringBuilder();
        sb.append("--").append(boundary).append("\r\n");
        sb.append("Content-Disposition: form-data; name=\"model\"\r\n\r\n").append(model).append("\r\n");
        sb.append("--").append(boundary).append("\r\n");
        sb.append("Content-Disposition: form-data; name=\"optionalPayload\"\r\n\r\n").append(optional).append("\r\n");
        sb.append("--").append(boundary).append("\r\n");
        sb.append("Content-Disposition: form-data; name=\"file\"; filename=\"").append(sanitize(filename)).append("\"\r\n");
        sb.append("Content-Type: application/octet-stream\r\n\r\n");
        byte[] head = sb.toString().getBytes(StandardCharsets.UTF_8);
        byte[] tail = ("\r\n--" + boundary + "--\r\n").getBytes(StandardCharsets.UTF_8);

        ByteArrayOutputStream body = new ByteArrayOutputStream();
        body.write(head);
        body.write(fileBytes);
        body.write(tail);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(jobUrl))
                .header("Authorization", "bearer " + token)
                .header("Content-Type", "multipart/form-data; boundary=" + boundary)
                .POST(HttpRequest.BodyPublishers.ofByteArray(body.toByteArray()))
                .timeout(Duration.ofSeconds(120))
                .build();

        HttpResponse<String> resp = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        JsonNode root = objectMapper.readTree(resp.body());
        String jobId = root.path("data").path("jobId").asText("");
        if (jobId.isEmpty()) {
            throw new IOException("OCR 提交失败: " + resp.body());
        }
        return jobId;
    }

    private String pollJob(String jobId) throws IOException, InterruptedException {
        long deadline = System.currentTimeMillis() + OCR_TIMEOUT_MS;
        while (System.currentTimeMillis() < deadline) {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(jobUrl + "/" + jobId))
                    .header("Authorization", "bearer " + token)
                    .GET()
                    .timeout(Duration.ofSeconds(60))
                    .build();
            HttpResponse<String> resp = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            JsonNode root = objectMapper.readTree(resp.body());
            String state = root.path("data").path("state").asText("");
            if ("done".equals(state)) {
                return root.path("data").path("resultUrl").path("jsonUrl").asText("");
            }
            if ("failed".equals(state)) {
                throw new IOException("OCR 任务失败: " + root.path("data").path("errorMsg").asText("未知错误"));
            }
            Thread.sleep(POLL_INTERVAL_MS);
        }
        throw new IOException("OCR 任务超时（超过 6 分钟）");
    }

    private String fetchMarkdown(String jsonlUrl) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(jsonlUrl))
                .GET()
                .timeout(Duration.ofSeconds(120))
                .build();
        HttpResponse<String> resp = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        StringBuilder text = new StringBuilder();
        for (String line : resp.body().split("\\n")) {
            String l = line.trim();
            if (l.isEmpty()) continue;
            try {
                JsonNode node = objectMapper.readTree(l);
                JsonNode results = node.path("result").path("layoutParsingResults");
                for (JsonNode r : results) {
                    text.append(r.path("markdown").path("text").asText("")).append("\n\n");
                }
            } catch (IOException ignored) {
                // 跳过无法解析的行
            }
        }
        return text.toString().trim();
    }

    private String sanitize(String filename) {
        if (filename == null) return "file";
        return filename.replaceAll("[\\r\\n\"]", "_");
    }
}
