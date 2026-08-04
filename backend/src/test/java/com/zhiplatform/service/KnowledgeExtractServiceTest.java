package com.zhiplatform.service;

import com.zhiplatform.dto.ExtractResultDTO;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

class KnowledgeExtractServiceTest {

    private final KnowledgeExtractService service = new KnowledgeExtractService(null);

    @Test
    void parseLlmResponse_stripsThinkAndParsesJson() throws Exception {
        String raw = "<think>让我分析这段文本……</think>\n" +
                "{\"entities\":[{\"name\":\"光合作用\",\"type\":\"概念\",\"subject\":\"高中生物\"," +
                "\"description\":\"绿色植物利用光能转化有机物\"}]," +
                "\"relations\":[{\"source\":\"光合作用\",\"target\":\"细胞呼吸\"," +
                "\"relationType\":\"对立过程\",\"description\":\"互逆的代谢过程\"}]}";

        ExtractResultDTO dto = service.parseLlmResponse(raw);

        assertEquals(1, dto.getEntities().size());
        assertEquals("光合作用", dto.getEntities().get(0).getName());
        assertEquals("高中生物", dto.getEntities().get(0).getSubject());
        assertEquals(1, dto.getRelations().size());
        assertEquals("对立过程", dto.getRelations().get(0).getRelationType());
    }

    @Test
    void parseLlmResponse_ignoresBlankEntities() throws Exception {
        String raw = "{\"entities\":[{\"name\":\"\",\"type\":\"概念\"},{\"name\":\"催化剂原理\"," +
                "\"type\":\"概念\",\"subject\":\"高中化学\"}],\"relations\":[]}";

        ExtractResultDTO dto = service.parseLlmResponse(raw);

        assertEquals(1, dto.getEntities().size());
        assertEquals("催化剂原理", dto.getEntities().get(0).getName());
        assertTrue(dto.getRelations().isEmpty());
    }

    @Test
    void parseLlmResponse_invalidJsonThrows() {
        assertThrows(IOException.class, () -> service.parseLlmResponse("这不是 JSON"));
    }
}

