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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
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
    void kbqa_noMatchWithoutAi_returnsNotFound() {
        when(knowledgeRepo.findByNameInQuestion(anyString())).thenReturn(List.of());
        when(knowledgeRepo.findByNameContaining(anyString())).thenReturn(List.of());
        when(knowledgeRepo.findByKeywordAnywhere(anyString())).thenReturn(List.of());

        QARequest request = new QARequest();
        request.setQuestion("酶如何参与反应？");

        QAResponse response = qaService.kbqa(request);

        // 无 AI 配置且无匹配时：明确提示未找到，不再按学科返回无关知识点
        assertEquals("kbqa", response.getMode());
        assertTrue(response.getRelatedNodeIds().isEmpty());
        assertTrue(response.getAnswer().contains("未找到"));
    }

    @Test
    void graphRag_usesKnowledgeContextAndReturnsAnswer() throws Exception {
        ModelscopeLlmClient llmClient = mock(ModelscopeLlmClient.class);
        KnowledgeNode node = new KnowledgeNode();
        node.setId(1L);
        node.setName("酶的特性");
        node.setSubject("高中生物");
        node.setDescription("酶是生物催化剂，具有高效性和专一性");
        when(knowledgeRepo.findByNameContaining(anyString())).thenReturn(List.of(node));
        when(llmClient.chat(anyString(), anyString())).thenReturn(
                "酶是生物催化剂，与化学中的催化剂原理相通。\n引用：酶的特性、催化剂原理");

        QAService service3 = new QAService(knowledgeRepo, templateRepo, llmClient);
        QARequest request = new QARequest();
        request.setQuestion("酶和催化剂有什么关系？");

        QAResponse response = service3.graphRag(request);

        assertEquals("graphrag", response.getMode());
        assertTrue(response.getAnswer().contains("酶"));
        assertEquals(List.of(1L), response.getRelatedNodeIds());
        assertEquals(List.of("酶的特性"), response.getSources());
    }

    @Test
    void graphRag_withoutLlm_returnsNotConfigured() {
        QAService service2 = new QAService(knowledgeRepo, templateRepo, null);
        QARequest request = new QARequest();
        request.setQuestion("测试问题");

        QAResponse response = service2.graphRag(request);

        assertTrue(response.getAnswer().contains("未配置"));
        assertEquals("graphrag", response.getMode());
    }

    @Test
    void kbqa_fallsBackToAiWhenNoMatch() throws Exception {
        ModelscopeLlmClient llmClient = mock(ModelscopeLlmClient.class);
        when(knowledgeRepo.findByNameContaining(anyString())).thenReturn(List.of());
        when(llmClient.chat(anyString(), anyString())).thenReturn(
                "AI 回答：光合作用是将光能转化为化学能并合成有机物的过程。知识库暂未收录该条目的直接描述。");

        QAService service3 = new QAService(knowledgeRepo, templateRepo, llmClient);
        QARequest request = new QARequest();
        request.setQuestion("完全无关的冷门问题xyz");

        QAResponse response = service3.kbqa(request);

        assertEquals("kbqa_ai", response.getMode());
        assertTrue(response.getAnswer().contains("光合作用"));
        assertTrue(response.getRelatedNodeIds().isEmpty());
    }

    @Test
    void kbqa_matchesByNameInQuestion() {
        KnowledgeNode node = new KnowledgeNode();
        node.setId(7L);
        node.setName("牛顿第二定律");
        node.setSubject("高中物理");
        node.setDescription("F=ma，力等于质量乘以加速度");
        when(knowledgeRepo.findByNameInQuestion("牛顿第二定律的内容是什么？")).thenReturn(List.of(node));

        QARequest request = new QARequest();
        request.setQuestion("牛顿第二定律的内容是什么？");

        QAResponse response = qaService.kbqa(request);

        assertEquals("kbqa", response.getMode());
        assertEquals(List.of(7L), response.getRelatedNodeIds());
        assertTrue(response.getAnswer().contains("牛顿第二定律"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void streamGraphRag_pushesMetaDeltaThenDone() throws Exception {
        ModelscopeLlmClient llmClient = mock(ModelscopeLlmClient.class);
        KnowledgeNode node = new KnowledgeNode();
        node.setId(1L);
        node.setName("酶的特性");
        node.setSubject("高中生物");
        node.setDescription("酶是生物催化剂，具有高效性和专一性");
        when(knowledgeRepo.findByNameContaining(anyString())).thenReturn(List.of(node));
        doAnswer(inv -> {
            java.util.function.Consumer<String> onToken = inv.getArgument(2);
            onToken.accept("酶是");
            onToken.accept("生物催化剂");
            return null;
        }).when(llmClient).chatStream(anyString(), anyString(), any());

        QAService service3 = new QAService(knowledgeRepo, templateRepo, llmClient);
        QARequest request = new QARequest();
        request.setQuestion("酶和催化剂有什么关系？");

        List<QAService.QaStreamEvent> events = new ArrayList<>();
        service3.streamGraphRag(request, events::add);

        assertEquals("meta", events.get(0).name());
        Map<String, Object> meta = (Map<String, Object>) events.get(0).data();
        assertEquals(List.of(1L), meta.get("relatedNodeIds"));
        assertEquals(List.of("酶的特性"), meta.get("sources"));
        assertTrue(events.stream().anyMatch(e -> "delta".equals(e.name())
                && "生物催化剂".equals(((Map<String, Object>) e.data()).get("content"))));
        assertEquals("done", events.get(events.size() - 1).name());
    }

    @Test
    void streamGraphRag_withoutLlm_returnsNotConfigured() {
        QAService service2 = new QAService(knowledgeRepo, templateRepo, null);
        QARequest request = new QARequest();
        request.setQuestion("测试问题");

        List<QAService.QaStreamEvent> events = new ArrayList<>();
        service2.streamGraphRag(request, events::add);

        assertEquals("meta", events.get(0).name());
        QAService.QaStreamEvent last = events.get(events.size() - 1);
        assertEquals("done", last.name());
        assertTrue(events.stream().anyMatch(e -> "delta".equals(e.name())
                && ((Map<?, ?>) e.data()).get("content").toString().contains("未配置")));
    }

    @Test
    void streamGraphRag_llmError_sendsErrorEvent() throws Exception {
        ModelscopeLlmClient llmClient = mock(ModelscopeLlmClient.class);
        when(knowledgeRepo.findByNameContaining(anyString())).thenReturn(List.of());
        doAnswer(inv -> {
            throw new java.io.IOException("LLM boom");
        }).when(llmClient).chatStream(anyString(), anyString(), any());

        QAService service3 = new QAService(knowledgeRepo, templateRepo, llmClient);
        QARequest request = new QARequest();
        request.setQuestion("测试问题");

        List<QAService.QaStreamEvent> events = new ArrayList<>();
        service3.streamGraphRag(request, events::add);

        QAService.QaStreamEvent last = events.get(events.size() - 1);
        assertEquals("error", last.name());
        assertTrue(((Map<?, ?>) last.data()).get("message").toString().contains("LLM boom"));
    }
}
