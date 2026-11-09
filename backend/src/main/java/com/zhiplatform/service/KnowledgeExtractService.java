package com.zhiplatform.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zhiplatform.dto.DocumentParseDTO;
import com.zhiplatform.dto.ExtractEntityDTO;
import com.zhiplatform.dto.ExtractRelationDTO;
import com.zhiplatform.dto.ExtractResultDTO;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.regex.Pattern;

/**
 * 文档知识抽取服务：把文档解析出的文本交给模型微服务抽取实体 + 交叉关系。
 */
@Service
public class KnowledgeExtractService {

    private static final String SYSTEM_PROMPT =
            "你是高中交叉学科知识图谱构建助手。从用户提供的文本中抽取知识点实体和交叉关系。" +
            "严格要求：最终回答只输出一个 JSON 对象，禁止输出 JSON 以外的任何内容（包括不要输出<think>推理过程）。\n" +
            "entities 字段：name(知识点名称)、type(概念|公式|实验|考点|拓展素材|交叉应用案例)、" +
            "subject(高中物理|高中化学|高中生物|航天拓展|环境科普)、description(一句话说明)。\n" +
            "relations 字段：source(实体名)、target(实体名)、relationType(跨学科关联|共通原理|交叉应用|基于原理|应用|关联|协同应用)、description(一句话说明)。\n" +
            "只抽取文本中确实出现或明显蕴含的知识点，不要臆造；每条关系的 source/target 必须在 entities 中。\n" +
            "输出格式：{\"entities\":[{\"name\":\"\",\"type\":\"\",\"subject\":\"\",\"description\":\"\"}]," +
            "\"relations\":[{\"source\":\"\",\"target\":\"\",\"relationType\":\"\",\"description\":\"\"}]}";

    private final ModelServiceClient llmClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private static final Pattern THINK_BLOCK = Pattern.compile("<think>.*?</think>", Pattern.DOTALL);

    public KnowledgeExtractService(ModelServiceClient llmClient) {
        this.llmClient = llmClient;
    }

    /**
     * 对已解析的文档文本执行 LLM 知识抽取。
     */
    public ExtractResultDTO extract(DocumentParseDTO parsed) throws IOException, InterruptedException {
        String text = parsed.getText() == null ? "" : parsed.getText().trim();
        if (text.isEmpty()) {
            throw new IllegalArgumentException("文档未提取到文本，无法抽取知识");
        }
        String llmOutput = llmClient.chat(SYSTEM_PROMPT, text);
        ExtractResultDTO result = parseLlmResponse(llmOutput);
        result.setFileName(parsed.getFileName());
        result.setSource(parsed.getSource());
        result.setCharCount(text.length());
        result.setText(text.length() > 3000 ? text.substring(0, 3000) : text);
        return result;
    }

    /**
     * 解析 LLM 返回：剥离 <think> 推理块，提取 JSON，映射为 DTO。
     */
    ExtractResultDTO parseLlmResponse(String raw) throws IOException {
        String cleaned = THINK_BLOCK.matcher(raw == null ? "" : raw).replaceAll("");
        int start = cleaned.indexOf('{');
        int end = cleaned.lastIndexOf('}');
        if (start < 0 || end <= start) {
            throw new IOException("LLM 未返回有效 JSON: " + (cleaned.length() > 200 ? cleaned.substring(0, 200) : cleaned));
        }
        JsonNode root = objectMapper.readTree(cleaned.substring(start, end + 1));

        ExtractResultDTO result = new ExtractResultDTO();
        JsonNode entities = root.path("entities");
        if (entities.isArray()) {
            for (JsonNode e : entities) {
                ExtractEntityDTO dto = new ExtractEntityDTO();
                dto.setName(e.path("name").asText(""));
                dto.setType(e.path("type").asText("概念"));
                dto.setSubject(e.path("subject").asText(""));
                dto.setDescription(e.path("description").asText(""));
                if (!dto.getName().isBlank()) result.getEntities().add(dto);
            }
        }
        JsonNode relations = root.path("relations");
        if (relations.isArray()) {
            for (JsonNode r : relations) {
                ExtractRelationDTO dto = new ExtractRelationDTO();
                dto.setSource(r.path("source").asText(""));
                dto.setTarget(r.path("target").asText(""));
                dto.setRelationType(r.path("relationType").asText("关联"));
                dto.setDescription(r.path("description").asText(""));
                if (!dto.getSource().isBlank() && !dto.getTarget().isBlank()) {
                    result.getRelations().add(dto);
                }
            }
        }
        return result;
    }
}
