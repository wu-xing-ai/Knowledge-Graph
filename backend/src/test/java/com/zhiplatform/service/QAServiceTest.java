package com.zhiplatform.service;

import com.zhiplatform.dto.QARequest;
import com.zhiplatform.dto.QAResponse;
import com.zhiplatform.entity.neo4j.KnowledgeNode;
import com.zhiplatform.repository.neo4j.KnowledgeNodeRepository;
import com.zhiplatform.repository.mysql.QaTemplateRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class QAServiceTest {

    @Mock
    private KnowledgeNodeRepository knowledgeRepo;
    @Mock
    private QaTemplateRepository templateRepo;

    private QAService qaService;

    @BeforeEach
    void setUp() {
        qaService = new QAService(knowledgeRepo, templateRepo);
    }

    @Test
    void kbqa_returnsAnswerForPhysicsQuestion() {
        KnowledgeNode node = new KnowledgeNode();
        node.setId(1L);
        node.setName("牛顿第二定律");
        node.setSubject("高中物理");
        node.setDescription("物体加速度与作用力成正比");

        when(knowledgeRepo.findByNameContaining(anyString())).thenReturn(List.of(node));

        QARequest request = new QARequest();
        request.setQuestion("什么是牛顿第二定律？");

        QAResponse response = qaService.kbqa(request);

        assertNotNull(response.getAnswer());
        assertTrue(response.getAnswer().contains("牛顿第二定律"));
        assertEquals("kbqa", response.getMode());
        assertEquals(List.of(1L), response.getRelatedNodeIds());
        assertEquals(List.of("牛顿第二定律"), response.getSources());
    }

    @Test
    void kbqa_returnsEmptyMessageWhenNoMatch() {
        when(knowledgeRepo.findByNameContaining(anyString())).thenReturn(List.of());

        QARequest request = new QARequest();
        request.setQuestion("完全不相关的问题");

        QAResponse response = qaService.kbqa(request);

        assertTrue(response.getAnswer().contains("未找到相关知识点"));
        assertTrue(response.getRelatedNodeIds().isEmpty());
        assertTrue(response.getSources().isEmpty());
    }

    @Test
    void kbqa_findsCrossSubject() {
        KnowledgeNode node = new KnowledgeNode();
        node.setId(2L);
        node.setName("火箭推进原理");
        node.setSubject("航天拓展");

        when(knowledgeRepo.findByNameContaining(anyString())).thenReturn(List.of(node));

        QARequest request = new QARequest();
        request.setQuestion("火箭发射涉及哪些物理知识？");

        QAResponse response = qaService.kbqa(request);

        assertNotNull(response.getAnswer());
        assertFalse(response.getRelatedNodeIds().isEmpty());
    }

    @Test
    void kbqa_fallsBackToSubjectWhenNoKeywordMatch() {
        KnowledgeNode node = new KnowledgeNode();
        node.setId(3L);
        node.setName("酶催化");
        node.setSubject("高中生物");

        when(knowledgeRepo.findByNameContaining(anyString())).thenReturn(List.of());
        when(knowledgeRepo.findBySubject("高中生物")).thenReturn(List.of(node));

        QARequest request = new QARequest();
        request.setQuestion("酶如何参与反应？");

        QAResponse response = qaService.kbqa(request);

        assertNotNull(response.getAnswer());
        assertFalse(response.getRelatedNodeIds().isEmpty());
    }

    @Test
    void graphRag_wrapsKbqaAnswer() {
        KnowledgeNode node = new KnowledgeNode();
        node.setId(4L);
        node.setName("光合作用");
        node.setSubject("高中生物");

        when(knowledgeRepo.findByNameContaining(anyString())).thenReturn(List.of(node));

        QARequest request = new QARequest();
        request.setQuestion("光合作用是什么？");

        QAResponse response = qaService.graphRag(request);

        assertEquals("graphrag", response.getMode());
        assertTrue(response.getAnswer().contains("GraphRAG"));
        assertEquals(List.of(4L), response.getRelatedNodeIds());
    }
}
