package com.zhiplatform.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.function.Consumer;

/**
 * 魔搭 ModelScope 云端 LLM（OpenAI 兼容 chat/completions）通用客户端。
 * 供智能问答（GraphRAG）与文档知识抽取共用。
 */
@Service
public class ModelscopeLlmClient {

    private final String baseUrl;
    private final String apiKey;
    private final String model;
    private final long timeoutSeconds;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(30))
            .build();

    public ModelscopeLlmClient(
            @Value("${modelscope.base-url}") String baseUrl,
            @Value("${modelscope.api-key}") String apiKey,
            @Value("${modelscope.model}") String model,
            @Value("${modelscope.timeout-seconds:240}") long timeoutSeconds) {
        this.baseUrl = baseUrl;
        this.apiKey = apiKey;
        this.model = model;
        this.timeoutSeconds = timeoutSeconds;
    }

    /**
     * 单轮对话，返回助手文本内容。
     */
    public String chat(String system, String user) throws IOException, InterruptedException {
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException("未配置 MODELSCOPE_API_KEY，请在 .env 中设置");
        }
        ObjectNode root = objectMapper.createObjectNode();
        root.put("model", model);
        root.put("temperature", 0.3);
        root.put("max_tokens", 4096);
        ArrayNode messages = root.putArray("messages");
        if (system != null && !system.isBlank()) {
            messages.addObject().put("role", "system").put("content", system);
        }
        messages.addObject().put("role", "user").put("content", user);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/chat/completions"))
                .header("Authorization", "Bearer " + apiKey)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(root), StandardCharsets.UTF_8))
                .timeout(Duration.ofSeconds(timeoutSeconds))
                .build();

        HttpResponse<String> resp = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (resp.statusCode() != 200) {
            throw new IOException("LLM 调用失败 (HTTP " + resp.statusCode() + "): " + resp.body());
        }
        JsonNode content = objectMapper.readTree(resp.body())
                .path("choices").path(0).path("message").path("content");
        if (content.isMissingNode() || content.asText().isBlank()) {
            throw new IOException("LLM 返回内容为空");
        }
        return content.asText();
    }

    /**
     * 流式对话：stream=true，逐行解析 SSE 增量，每个内容片段回调 onToken。
     */
    public void chatStream(String system, String user, Consumer<String> onToken)
            throws IOException, InterruptedException {
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException("未配置 MODELSCOPE_API_KEY，请在 .env 中设置");
        }
        ObjectNode root = objectMapper.createObjectNode();
        root.put("model", model);
        root.put("temperature", 0.3);
        root.put("max_tokens", 4096);
        root.put("stream", true);
        ArrayNode messages = root.putArray("messages");
        if (system != null && !system.isBlank()) {
            messages.addObject().put("role", "system").put("content", system);
        }
        messages.addObject().put("role", "user").put("content", user);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/chat/completions"))
                .header("Authorization", "Bearer " + apiKey)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(root), StandardCharsets.UTF_8))
                .timeout(Duration.ofSeconds(timeoutSeconds))
                .build();

        HttpResponse<InputStream> resp = httpClient.send(request, HttpResponse.BodyHandlers.ofInputStream());
        if (resp.statusCode() != 200) {
            String errBody;
            try (InputStream is = resp.body()) {
                errBody = new String(is.readAllBytes(), StandardCharsets.UTF_8);
            }
            throw new IOException("LLM 流式调用失败 (HTTP " + resp.statusCode() + "): " + errBody);
        }
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(resp.body(), StandardCharsets.UTF_8))) {
            ThinkFilter filter = new ThinkFilter();
            String line;
            while ((line = reader.readLine()) != null) {
                String trimmed = line.trim();
                if (!trimmed.startsWith("data:")) continue;
                String data = trimmed.substring(5).trim();
                if (data.isEmpty()) continue;
                if ("[DONE]".equals(data)) break;
                try {
                    JsonNode delta = objectMapper.readTree(data)
                            .path("choices").path(0).path("delta").path("content");
                    if (!delta.isMissingNode() && !delta.asText().isEmpty()) {
                        String filtered = filter.feed(delta.asText());
                        if (!filtered.isEmpty()) {
                            onToken.accept(filtered);
                        }
                    }
                } catch (Exception ignored) {
                    // 跳过无法解析的中间行
                }
            }
        }
    }

    /**
     * 流式增量 <think> 草稿过滤：跨 token 保持状态，think 块内容不对外回调。
     */
    public static class ThinkFilter {
        private final StringBuilder pending = new StringBuilder();
        private boolean inThink = false;

        public synchronized String feed(String token) {
            pending.append(token);
            StringBuilder out = new StringBuilder();
            while (true) {
                if (!inThink) {
                    int idx = pending.indexOf("<think");
                    if (idx >= 0) {
                        out.append(pending, 0, idx);
                        pending.delete(0, idx);
                        inThink = true;
                        continue;
                    }
                    String s = pending.toString();
                    int keep = 0;
                    for (int len = Math.min(6, s.length()); len >= 1; len--) {
                        if ("<think".startsWith(s.substring(s.length() - len))) {
                            keep = len;
                            break;
                        }
                    }
                    if (keep < s.length()) {
                        out.append(s, 0, s.length() - keep);
                        pending.delete(0, s.length() - keep);
                    }
                    break;
                }
                int end = pending.indexOf("</think>");
                if (end >= 0) {
                    pending.delete(0, end + "</think>".length());
                    inThink = false;
                } else {
                    pending.setLength(0);
                    break;
                }
            }
            return out.toString();
        }
    }
}
