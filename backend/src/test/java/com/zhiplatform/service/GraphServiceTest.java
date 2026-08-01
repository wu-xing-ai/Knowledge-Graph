package com.zhiplatform.service;

import com.zhiplatform.dto.GraphDataDTO;
import com.zhiplatform.entity.neo4j.KnowledgeNode;
import com.zhiplatform.repository.neo4j.KnowledgeNodeRepository;
import com.zhiplatform.repository.mysql.GraphConfigRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GraphServiceTest {

    @Mock
    private KnowledgeNodeRepository knowledgeRepo;
    @Mock
    private GraphConfigRepository configRepo;

    private GraphService graphService;
    private List<Map<String, Object>> mockQueryResult;

    @BeforeEach
    void setUp() {
        graphService = new GraphService(knowledgeRepo, configRepo, null) {
            @Override
            List<Map<String, Object>> executeGraphQuery(String cypher, Map<String, Object> params) {
                return mockQueryResult;
            }
        };
    }

    @Test
    void getGraphData_returnsNodesAndEdges() {
        KnowledgeNode n1 = new KnowledgeNode();
        n1.setId(1L);
        n1.setName("A");
        n1.setSubject("高中物理");
        KnowledgeNode n2 = new KnowledgeNode();
        n2.setId(2L);
        n2.setName("B");
        n2.setSubject("高中化学");

        mockQueryResult = List.of(Map.of("n", n1, "relationType", "跨学科关联", "m", n2));
        when(knowledgeRepo.findById(1L)).thenReturn(Optional.of(n1));
        when(knowledgeRepo.findById(2L)).thenReturn(Optional.of(n2));

        GraphDataDTO dto = graphService.getGraphData(null, null, 200);

        assertEquals(2, dto.getNodes().size());
        assertEquals(1, dto.getEdges().size());
        assertEquals("跨学科关联", dto.getEdges().get(0).getRelationType());
    }

    @Test
    void getGraphData_withKeywordUsesSearch() {
        KnowledgeNode n1 = new KnowledgeNode();
        n1.setId(1L);
        n1.setName("光合作用");
        KnowledgeNode n2 = new KnowledgeNode();
        n2.setId(2L);
        n2.setName("呼吸作用");

        mockQueryResult = List.of(Map.of("n", n1, "relationType", "关联", "m", n2));
        when(knowledgeRepo.findById(1L)).thenReturn(Optional.of(n1));
        when(knowledgeRepo.findById(2L)).thenReturn(Optional.of(n2));

        GraphDataDTO dto = graphService.getGraphData(null, "光合", 200);

        assertEquals(2, dto.getNodes().size());
        assertEquals("光合作用", dto.getNodes().get(0).getName());
    }

    @Test
    void expandNode_returnsRelatedGraph() {
        KnowledgeNode n1 = new KnowledgeNode();
        n1.setId(10L);
        n1.setName("酶催化");
        n1.setSubject("高中生物");
        n1.setIsCross(true);
        KnowledgeNode n2 = new KnowledgeNode();
        n2.setId(11L);
        n2.setName("催化剂");

        mockQueryResult = List.of(Map.of("n", n1, "relationType", "共通原理", "m", n2));
        when(knowledgeRepo.findById(10L)).thenReturn(Optional.of(n1));
        when(knowledgeRepo.findById(11L)).thenReturn(Optional.of(n2));

        GraphDataDTO dto = graphService.expandNode(10L);

        assertEquals(2, dto.getNodes().size());
        GraphDataDTO.NodeDTO crossNode = dto.getNodes().stream()
                .filter(n -> n.getId().equals(10L)).findFirst().orElseThrow();
        assertEquals("#70AD47", crossNode.getColor());
        assertEquals(40, crossNode.getSize());
    }

    @Test
    void getNodeColor_usesSubjectColor() {
        KnowledgeNode node = new KnowledgeNode();
        node.setId(1L);
        node.setName("测试");
        node.setSubject("高中物理");
        node.setIsCross(false);

        mockQueryResult = List.of(Map.of("n", node, "relationType", "关联", "m", node));
        when(knowledgeRepo.findById(1L)).thenReturn(Optional.of(node));

        GraphDataDTO dto = graphService.getGraphData(null, null, 200);
        assertEquals("#5B9BD5", dto.getNodes().get(0).getColor());
    }

    @Test
    void getNodeColor_crossNodeKeepsSubjectColor() {
        KnowledgeNode node = new KnowledgeNode();
        node.setId(2L);
        node.setName("光合作用");
        node.setSubject("高中生物");
        node.setIsCross(true);

        mockQueryResult = List.of(Map.of("n", node, "relationType", "关联", "m", node));
        when(knowledgeRepo.findById(2L)).thenReturn(Optional.of(node));

        GraphDataDTO dto = graphService.getGraphData(null, null, 200);
        assertEquals("#70AD47", dto.getNodes().get(0).getColor());
    }
}
