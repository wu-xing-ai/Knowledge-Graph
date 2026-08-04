package com.zhiplatform.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zhiplatform.dto.DocumentParseDTO;
import com.zhiplatform.entity.mysql.Subject;
import com.zhiplatform.entity.neo4j.KnowledgeNode;
import com.zhiplatform.service.DocumentDraftService;
import com.zhiplatform.service.DocumentParseService;
import com.zhiplatform.service.DocumentExtractJobService;
import com.zhiplatform.service.KnowledgeExtractService;
import com.zhiplatform.service.KnowledgeService;
import com.zhiplatform.service.UserService;
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
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class KnowledgeControllerTest {

    private MockMvc mockMvc;
    private ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());

    @Mock
    private KnowledgeService knowledgeService;

    @Mock
    private DocumentParseService documentParseService;

    @Mock
    private DocumentExtractJobService documentExtractJobService;

    @Mock
    private UserService userService;

    @Mock
    private DocumentDraftService documentDraftService;

    @BeforeEach
    void setUp() {
        KnowledgeController controller = new KnowledgeController(knowledgeService, documentParseService, documentExtractJobService, documentDraftService, userService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    void getSubjects_all() throws Exception {
        Subject s = new Subject();
        s.setId(1L);
        s.setName("高中物理");
        when(knowledgeService.getAllSubjects()).thenReturn(List.of(s));

        mockMvc.perform(get("/api/knowledge/subject/list"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data[0].name").value("高中物理"));
    }

    @Test
    void getSubjects_byType() throws Exception {
        Subject s = new Subject();
        s.setType("交叉专题");
        when(knowledgeService.getSubjectsByType("交叉专题")).thenReturn(List.of(s));

        mockMvc.perform(get("/api/knowledge/subject/list").param("type", "交叉专题"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    void addSubject_success() throws Exception {
        Subject s = new Subject();
        s.setName("新学科");
        when(knowledgeService.saveSubject(any(Subject.class))).thenReturn(s);

        mockMvc.perform(post("/api/knowledge/subject/add")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(s)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.name").value("新学科"));
    }

    @Test
    void deleteSubject_success() throws Exception {
        mockMvc.perform(delete("/api/knowledge/subject/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        verify(knowledgeService).deleteSubject(1L);
    }

    @Test
    void addKnowledge_success() throws Exception {
        KnowledgeNode node = new KnowledgeNode();
        node.setName("测试知识点");
        when(knowledgeService.addKnowledge(any(KnowledgeNode.class))).thenReturn(node);

        mockMvc.perform(post("/api/knowledge/add")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(node)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.name").value("测试知识点"));
    }

    @Test
    void getKnowledge_found() throws Exception {
        KnowledgeNode node = new KnowledgeNode();
        node.setId(1L);
        node.setName("A");
        when(knowledgeService.getKnowledge(1L)).thenReturn(node);

        mockMvc.perform(get("/api/knowledge/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.name").value("A"));
    }

    @Test
    void queryKnowledge_byKeyword() throws Exception {
        KnowledgeNode node = new KnowledgeNode();
        node.setName("牛顿");
        when(knowledgeService.searchKnowledge("牛顿", null, null)).thenReturn(List.of(node));

        mockMvc.perform(get("/api/knowledge/query").param("keyword", "牛顿"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data[0].name").value("牛顿"));
    }

    @Test
    void parseDocument_returnsExtractedText() throws Exception {
        DocumentParseDTO dto = new DocumentParseDTO();
        dto.setFileName("测试.txt");
        dto.setSource("本地解析(文本)");
        dto.setText("光合作用是绿色植物利用光能转化有机物的过程。");
        dto.setCharCount(25);
        when(documentParseService.parse(any())).thenReturn(dto);

        mockMvc.perform(multipart("/api/knowledge/document/parse")
                        .file(new org.springframework.mock.web.MockMultipartFile(
                                "file", "测试.txt", "text/plain",
                                "光合作用是绿色植物利用光能转化有机物的过程。".getBytes(java.nio.charset.StandardCharsets.UTF_8))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.fileName").value("测试.txt"))
                .andExpect(jsonPath("$.data.charCount").value(25));
    }
}




