package com.zhiplatform.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

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
}
