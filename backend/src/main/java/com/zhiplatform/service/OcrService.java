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
 * OCR 客户端：把文件转发给模型微服务（Python FastAPI /v1/ocr），
 * 由 Python 侧统一对接云端 PaddleOCR（AI Studio），返回 Markdown 文本。
 */
@Service
public class OcrService {

    private final String serviceUrl;
    private final ObjectMapper objectMapper = new ObjectMapper();
    // 强制 HTTP/1.1：默认 HTTP/2 会在明文连接上发 h2c Upgrade，uvicorn 不支持会直接 400
    private final HttpClient httpClient = HttpClient.newBuilder()
            .version(HttpClient.Version.HTTP_1_1)
            .connectTimeout(Duration.ofSeconds(30))
            .build();

    public OcrService(@Value("${model-service.base-url}") String serviceUrl) {
        this.serviceUrl = serviceUrl;
    }

    /**
     * 对文档/图片执行 OCR，返回识别出的 Markdown 文本。
     */
    public String recognize(String filename, byte[] fileBytes) throws IOException, InterruptedException {
        String boundary = "----ZhiPlatformOcr" + UUID.randomUUID();
        StringBuilder sb = new StringBuilder();
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
                .uri(URI.create(serviceUrl + "/v1/ocr"))
                .header("Content-Type", "multipart/form-data; boundary=" + boundary)
                .POST(HttpRequest.BodyPublishers.ofByteArray(body.toByteArray()))
                .timeout(Duration.ofSeconds(420))
                .build();

        HttpResponse<String> resp = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (resp.statusCode() != 200) {
            throw new IOException("OCR 服务调用失败 (HTTP " + resp.statusCode() + "): " + resp.body());
        }
        JsonNode root = objectMapper.readTree(resp.body());
        String text = root.path("text").asText("");
        if (text.isBlank()) {
            throw new IOException("OCR 返回内容为空: " + resp.body());
        }
        return text;
    }

    private String sanitize(String filename) {
        if (filename == null) return "file";
        return filename.replaceAll("[\\r\\n\"]", "_");
    }
}
