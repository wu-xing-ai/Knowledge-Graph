package com.zhiplatform.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zhiplatform.dto.QARequest;
import com.zhiplatform.dto.QAResponse;
import com.zhiplatform.service.QAService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class QAControllerTest {

    private MockMvc mockMvc;
    private ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());

    @Mock
    private QAService qaService;

    @BeforeEach
    void setUp() {
        QAController controller = new QAController(qaService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    void kbqa_success() throws Exception {
        QAResponse response = QAResponse.builder()
                .answer("答案")
                .relatedNodeIds(List.of(1L))
                .sources(List.of("来源"))
                .mode("kbqa")
                .build();

        when(qaService.kbqa(any(QARequest.class))).thenReturn(response);

        QARequest request = new QARequest();
        request.setQuestion("问题");

        mockMvc.perform(post("/api/qa/kbqa")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.mode").value("kbqa"));
    }

    @Test
    void graphRag_success() throws Exception {
        QAResponse response = QAResponse.builder()
                .answer("答案")
                .mode("graphrag")
                .build();

        when(qaService.graphRag(any(QARequest.class))).thenReturn(response);

        QARequest request = new QARequest();
        request.setQuestion("问题");

        mockMvc.perform(post("/api/qa/graphrag")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.mode").value("graphrag"));
    }

    @Test
    void getTemplates_success() throws Exception {
        when(qaService.getTemplates()).thenReturn(List.of());

        mockMvc.perform(get("/api/qa/template/list"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }
}
