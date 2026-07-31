package com.zhiplatform.service;

import com.zhiplatform.entity.mysql.Subject;
import com.zhiplatform.entity.neo4j.KnowledgeNode;
import com.zhiplatform.repository.neo4j.KnowledgeNodeRepository;
import com.zhiplatform.repository.mysql.SubjectRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class KnowledgeServiceTest {

    @Mock
    private KnowledgeNodeRepository knowledgeRepo;
    @Mock
    private SubjectRepository subjectRepo;

    private KnowledgeService knowledgeService;

    @BeforeEach
    void setUp() {
        knowledgeService = new KnowledgeService(knowledgeRepo, subjectRepo);
    }

    @Test
    void getAllSubjects_returnsList() {
        Subject s = new Subject();
        s.setName("高中物理");
        when(subjectRepo.findAll()).thenReturn(List.of(s));

        List<Subject> subjects = knowledgeService.getAllSubjects();
        assertEquals(1, subjects.size());
        assertEquals("高中物理", subjects.get(0).getName());
    }

    @Test
    void getSubjectsByType_filtersByType() {
        Subject s = new Subject();
        s.setType("交叉专题");
        when(subjectRepo.findByType("交叉专题")).thenReturn(List.of(s));

        List<Subject> subjects = knowledgeService.getSubjectsByType("交叉专题");
        assertEquals(1, subjects.size());
    }

    @Test
    void addKnowledge_setsCreateTimeAndSaves() {
        KnowledgeNode node = new KnowledgeNode();
        node.setName("新知识点");
        when(knowledgeRepo.save(any(KnowledgeNode.class))).thenAnswer(inv -> inv.getArgument(0));

        KnowledgeNode saved = knowledgeService.addKnowledge(node);

        assertNotNull(saved.getCreateTime());
        verify(knowledgeRepo).save(node);
    }

    @Test
    void updateKnowledge_savesNode() {
        KnowledgeNode node = new KnowledgeNode();
        node.setId(1L);
        node.setName("更新");
        when(knowledgeRepo.save(node)).thenReturn(node);

        KnowledgeNode updated = knowledgeService.updateKnowledge(node);

        assertEquals("更新", updated.getName());
        verify(knowledgeRepo).save(node);
    }

    @Test
    void deleteKnowledge_callsRepository() {
        knowledgeService.deleteKnowledge(5L);
        verify(knowledgeRepo).deleteById(5L);
    }

    @Test
    void getKnowledge_returnsNodeWhenFound() {
        KnowledgeNode node = new KnowledgeNode();
        node.setId(1L);
        when(knowledgeRepo.findById(1L)).thenReturn(Optional.of(node));

        KnowledgeNode found = knowledgeService.getKnowledge(1L);
        assertNotNull(found);
        assertEquals(1L, found.getId());
    }

    @Test
    void searchKnowledge_bySubjectId() {
        KnowledgeNode node = new KnowledgeNode();
        node.setSubjectId(2L);
        when(knowledgeRepo.findBySubjectId(2L)).thenReturn(List.of(node));

        List<KnowledgeNode> result = knowledgeService.searchKnowledge(null, 2L, null);
        assertEquals(1, result.size());
    }

    @Test
    void searchKnowledge_byKeyword() {
        KnowledgeNode node = new KnowledgeNode();
        node.setName("牛顿定律");
        when(knowledgeRepo.findByNameContaining("牛顿")).thenReturn(List.of(node));

        List<KnowledgeNode> result = knowledgeService.searchKnowledge("牛顿", null, null);
        assertEquals(1, result.size());
    }

    @Test
    void searchKnowledge_byType() {
        KnowledgeNode node = new KnowledgeNode();
        node.setType("概念");
        when(knowledgeRepo.findByType("概念")).thenReturn(List.of(node));

        List<KnowledgeNode> result = knowledgeService.searchKnowledge(null, null, "概念");
        assertEquals(1, result.size());
    }

    @Test
    void getCrossKnowledge_returnsCrossNodes() {
        KnowledgeNode node = new KnowledgeNode();
        node.setIsCross(true);
        when(knowledgeRepo.findByIsCross(true)).thenReturn(List.of(node));

        List<KnowledgeNode> result = knowledgeService.getCrossKnowledge();
        assertEquals(1, result.size());
        assertTrue(result.get(0).getIsCross());
    }
}
