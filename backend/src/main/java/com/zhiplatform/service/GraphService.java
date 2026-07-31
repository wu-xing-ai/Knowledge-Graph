package com.zhiplatform.service;

import com.zhiplatform.dto.GraphDataDTO;
import com.zhiplatform.entity.neo4j.KnowledgeNode;
import com.zhiplatform.entity.mysql.GraphConfig;
import com.zhiplatform.repository.neo4j.KnowledgeNodeRepository;
import com.zhiplatform.repository.mysql.GraphConfigRepository;
import org.springframework.data.neo4j.core.Neo4jClient;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class GraphService {
    private final KnowledgeNodeRepository knowledgeRepo;
    private final GraphConfigRepository configRepo;
    private final Neo4jClient neo4jClient;
    private static final Map<String, String> SUBJECT_COLORS = Map.of(
            "高中物理", "#4A90D9", "高中化学", "#F5A623", "高中生物", "#7ED321",
            "航天拓展", "#9B59B6", "环境科普", "#50E3C2");

    public GraphService(KnowledgeNodeRepository knowledgeRepo,
                        GraphConfigRepository configRepo,
                        Neo4jClient neo4jClient) {
        this.knowledgeRepo = knowledgeRepo;
        this.configRepo = configRepo;
        this.neo4jClient = neo4jClient;
    }

    public GraphDataDTO getGraphData(List<Long> subjectIds, String keyword, int limit) {
        List<Map<String, Object>> rawData;
        if (keyword != null && !keyword.isEmpty()) {
            rawData = executeGraphQuery(
                    "MATCH (n:Knowledge) WHERE n.name CONTAINS $keyword OR n.description CONTAINS $keyword " +
                    "MATCH (n)-[r]-(m:Knowledge) RETURN n, type(r) AS relationType, m LIMIT 20",
                    Map.of("keyword", keyword));
        } else if (subjectIds != null && !subjectIds.isEmpty()) {
            rawData = executeGraphQuery(
                    "MATCH (n:Knowledge)-[r]->(m:Knowledge) WHERE n.subjectId IN $subjectIds " +
                    "RETURN n, type(r) AS relationType, m LIMIT $limit",
                    Map.of("subjectIds", subjectIds, "limit", limit));
        } else {
            rawData = executeGraphQuery(
                    "MATCH (n:Knowledge)-[r]->(m:Knowledge) RETURN n, type(r) AS relationType, m LIMIT $limit",
                    Map.of("limit", limit));
        }
        return convertToDTO(rawData);
    }

    public GraphDataDTO expandNode(Long nodeId) {
        List<Map<String, Object>> rawData = executeGraphQuery(
                "MATCH (n:Knowledge)-[r]-(m:Knowledge) WHERE id(n) = $nodeId " +
                "RETURN n, type(r) AS relationType, m",
                Map.of("nodeId", nodeId));
        return convertToDTO(rawData);
    }

    List<Map<String, Object>> executeGraphQuery(String cypher, Map<String, Object> params) {
        return neo4jClient.query(cypher)
                .bindAll(params)
                .fetch()
                .all()
                .stream()
                .map(record -> {
                    Map<String, Object> row = new HashMap<>();
                    record.forEach(row::put);
                    return row;
                })
                .toList();
    }

    private Long extractNodeId(Object nodeObj) {
        if (nodeObj == null) return null;
        if (nodeObj instanceof KnowledgeNode) {
            return ((KnowledgeNode) nodeObj).getId();
        }
        if (nodeObj instanceof org.neo4j.driver.types.Node) {
            return ((org.neo4j.driver.types.Node) nodeObj).id();
        }
        return null;
    }

    private GraphDataDTO convertToDTO(List<Map<String, Object>> rawData) {
        Set<Long> nodeIds = new LinkedHashSet<>();
        List<GraphDataDTO.EdgeDTO> edges = new ArrayList<>();
        for (Map<String, Object> row : rawData) {
            Long sourceId = extractNodeId(row.get("n"));
            Long targetId = extractNodeId(row.get("m"));
            if (sourceId == null || targetId == null) continue;
            nodeIds.add(sourceId);
            nodeIds.add(targetId);
            GraphDataDTO.EdgeDTO edge = new GraphDataDTO.EdgeDTO();
            edge.setSource(sourceId);
            edge.setTarget(targetId);
            edge.setRelationType((String) row.getOrDefault("relationType", "关联"));
            edges.add(edge);
        }
        List<GraphDataDTO.NodeDTO> nodes = new ArrayList<>();
        for (Long nid : nodeIds) {
            KnowledgeNode kn = knowledgeRepo.findById(nid).orElse(null);
            if (kn == null) continue;
            GraphDataDTO.NodeDTO dto = new GraphDataDTO.NodeDTO();
            dto.setId(kn.getId());
            dto.setName(kn.getName());
            dto.setType(kn.getType());
            dto.setSubject(kn.getSubject());
            dto.setSubjectId(kn.getSubjectId());
            dto.setDifficulty(kn.getDifficulty());
            dto.setDescription(kn.getDescription());
            dto.setTags(kn.getTags());
            dto.setIsCross(kn.getIsCross());
            dto.setColor(getNodeColor(kn));
            dto.setSize(kn.getIsCross() != null && kn.getIsCross() ? 40 : 30);
            nodes.add(dto);
        }
        GraphDataDTO result = new GraphDataDTO();
        result.setNodes(nodes);
        result.setEdges(edges);
        return result;
    }

    private String getNodeColor(KnowledgeNode node) {
        if (node.getIsCross() != null && node.getIsCross()) return "#6C5CE7";
        String subject = node.getSubject();
        return subject == null ? "#BDC3C7" : SUBJECT_COLORS.getOrDefault(subject, "#BDC3C7");
    }

    public GraphConfig saveConfig(GraphConfig config) {
        return configRepo.save(config);
    }

    public List<GraphConfig> getUserConfigs(Long userId) {
        return configRepo.findByUserId(userId);
    }

    public void deleteConfig(Long id) {
        configRepo.deleteById(id);
    }
}
