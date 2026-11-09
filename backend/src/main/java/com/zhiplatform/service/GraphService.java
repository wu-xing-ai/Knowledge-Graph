package com.zhiplatform.service;

import com.zhiplatform.dto.GraphDataDTO;
import com.zhiplatform.dto.RelationDTO;
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
            "高中物理", "#5B9BD5", "高中化学", "#ED7D31", "高中生物", "#70AD47",
            "航天拓展", "#8859A3", "环境科普", "#39B5A8", "水污染治理", "#39B5A8");

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
                    "MATCH (n)-[r]-(m:Knowledge) RETURN n, coalesce(r.relationType, type(r)) AS relationType, m LIMIT 20",
                    Map.of("keyword", keyword));
        } else if (subjectIds != null && !subjectIds.isEmpty()) {
            rawData = executeGraphQuery(
                    "MATCH (n:Knowledge)-[r]->(m:Knowledge) WHERE n.subjectId IN $subjectIds " +
                    "RETURN n, coalesce(r.relationType, type(r)) AS relationType, m LIMIT $limit",
                    Map.of("subjectIds", subjectIds, "limit", limit));
        } else {
            rawData = executeGraphQuery(
                    "MATCH (n:Knowledge)-[r]->(m:Knowledge) RETURN n, coalesce(r.relationType, type(r)) AS relationType, m LIMIT $limit",
                    Map.of("limit", limit));
        }
        return convertToDTO(rawData);
    }

    public GraphDataDTO expandNode(Long nodeId) {
        List<Map<String, Object>> rawData = executeGraphQuery(
                "MATCH (n:Knowledge)-[r]-(m:Knowledge) WHERE id(n) = $nodeId " +
                "RETURN n, coalesce(r.relationType, type(r)) AS relationType, m",
                Map.of("nodeId", nodeId));
        return convertToDTO(rawData);
    }

    /**
     * 多关键词关联图谱：先按关键词匹配知识点，再返回这些节点间的全部关系。
     * 未命中任何关键词时返回空图；最多保留 40 个种子节点，避免大库下生成过大的结果。
     */
    public GraphDataDTO generateByKeywords(List<String> keywords, int limit) {
        List<String> terms = keywords == null ? List.of() : keywords.stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(keyword -> !keyword.isEmpty())
                .distinct()
                .toList();
        if (terms.isEmpty()) {
            GraphDataDTO empty = new GraphDataDTO();
            empty.setNodes(new ArrayList<>());
            empty.setEdges(new ArrayList<>());
            return empty;
        }
        int maxNodes = Math.max(1, Math.min(limit <= 0 ? 20 : limit, 40));
        List<Map<String, Object>> seedRows = executeGraphQuery(
                "MATCH (n:Knowledge) WHERE ANY(term IN $keywords WHERE n.name CONTAINS term " +
                "OR n.description CONTAINS term OR n.tags CONTAINS term) " +
                "RETURN n LIMIT $limit",
                Map.of("keywords", terms, "limit", maxNodes));
        List<Long> seedIds = seedRows.stream()
                .map(row -> extractNodeId(row.get("n")))
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        if (seedIds.isEmpty()) {
            GraphDataDTO empty = new GraphDataDTO();
            empty.setNodes(new ArrayList<>());
            empty.setEdges(new ArrayList<>());
            return empty;
        }
        List<Map<String, Object>> relationRows = executeGraphQuery(
                "MATCH (a:Knowledge)-[r]->(b:Knowledge) WHERE id(a) IN $ids AND id(b) IN $ids " +
                "RETURN a, coalesce(r.relationType, type(r)) AS relationType, b",
                Map.of("ids", seedIds));
        return convertToDTO(relationRows);
    }

    /**
     * 真·知识溯源：从指定知识点出发，沿「前置知识 / 基于原理 / 共通原理」关系向上游
     * 遍历 depth 跳，返回与 /api/graph/data 相同结构的子图。未命中任何路径时，
     * 至少返回起点节点，保证前端有可渲染内容。
     */
    public GraphDataDTO traceKnowledge(Long nodeId, int depth) {
        int hops = Math.max(1, Math.min(depth <= 0 ? 3 : depth, 6));
        List<String> relationTypes = List.of("前置知识", "基于原理", "共通原理");
        List<Map<String, Object>> rawData = executeGraphQuery(
                "MATCH path = (start:Knowledge)-[r*1.." + hops + "]->(up:Knowledge) " +
                "WHERE id(start) = $nodeId " +
                "AND all(rel IN r WHERE coalesce(rel.relationType, type(rel)) IN $relationTypes) " +
                "UNWIND r AS edge " +
                "WITH DISTINCT startNode(edge) AS n, coalesce(edge.relationType, type(edge)) AS relationType, " +
                "endNode(edge) AS m " +
                "RETURN n, relationType, m",
                Map.of("nodeId", nodeId, "relationTypes", relationTypes));
        GraphDataDTO dto = convertToDTO(rawData);
        KnowledgeNode start = knowledgeRepo.findById(nodeId).orElse(null);
        if (start != null) {
            boolean hasStart = dto.getNodes().stream().anyMatch(n -> n.getId().equals(nodeId));
            if (!hasStart) {
                dto.getNodes().add(0, toNodeDTO(start));
            }
        }
        return dto;
    }

    // ==================== 交叉关系管理 ====================

    /**
     * 查询全部交叉关系（含源/目标节点名称与关系说明）
     */
    public List<RelationDTO> getRelations() {
        List<Map<String, Object>> rows = executeGraphQuery(
                "MATCH (a:Knowledge)-[r:CROSS_RELATE]->(b:Knowledge) " +
                "RETURN id(a) AS sourceId, a.name AS sourceName, " +
                "id(b) AS targetId, b.name AS targetName, " +
                "coalesce(r.relationType, type(r)) AS relationType, " +
                "coalesce(r.description, '') AS description " +
                "ORDER BY sourceName, relationType",
                Map.of());
        List<RelationDTO> relations = new ArrayList<>();
        for (Map<String, Object> row : rows) {
            RelationDTO dto = new RelationDTO();
            dto.setSourceId(asLong(row.get("sourceId")));
            dto.setSourceName(str(row.get("sourceName")));
            dto.setTargetId(asLong(row.get("targetId")));
            dto.setTargetName(str(row.get("targetName")));
            dto.setRelationType(str(row.get("relationType")));
            dto.setDescription(str(row.get("description")));
            relations.add(dto);
        }
        return relations;
    }

    /**
     * 新增交叉关系（MERGE 幂等，description 始终刷新）
     */
    public boolean addRelation(RelationDTO dto) {
        if (dto == null || dto.getSourceId() == null || dto.getTargetId() == null) {
            return false;
        }
        if (Objects.equals(dto.getSourceId(), dto.getTargetId())) {
            return false;
        }
        String relationType = dto.getRelationType() == null || dto.getRelationType().isBlank()
                ? "关联" : dto.getRelationType().trim();
        String description = dto.getDescription() == null ? "" : dto.getDescription().trim();
        Map<String, Object> params = Map.of(
                "sourceId", dto.getSourceId(),
                "targetId", dto.getTargetId(),
                "relationType", relationType,
                "description", description);
        executeGraphUpdate(
                "MATCH (a:Knowledge) WHERE id(a) = $sourceId " +
                "WITH a " +
                "MATCH (b:Knowledge) WHERE id(b) = $targetId " +
                "MERGE (a)-[r:CROSS_RELATE {relationType: $relationType}]->(b) " +
                "SET r.description = $description",
                params);
        return true;
    }

    /**
     * 删除指定交叉关系
     */
    public boolean deleteRelation(Long sourceId, Long targetId, String relationType) {
        if (sourceId == null || targetId == null) return false;
        String type = relationType == null || relationType.isBlank() ? "关联" : relationType.trim();
        Map<String, Object> params = Map.of("sourceId", sourceId, "targetId", targetId, "relationType", type);
        executeGraphUpdate(
                "MATCH (a:Knowledge)-[r:CROSS_RELATE]->(b:Knowledge) " +
                "WHERE id(a) = $sourceId AND id(b) = $targetId AND coalesce(r.relationType, type(r)) = $relationType " +
                "DELETE r",
                params);
        return true;
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

    void executeGraphUpdate(String cypher, Map<String, Object> params) {
        if (neo4jClient == null) return;
        neo4jClient.query(cypher).bindAll(params).run();
    }

    private Long asLong(Object o) {
        if (o == null) return null;
        if (o instanceof Number n) return n.longValue();
        try { return Long.parseLong(o.toString()); } catch (NumberFormatException e) { return null; }
    }

    private String str(Object o) {
        return o == null ? "" : o.toString();
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

    private GraphDataDTO.NodeDTO toNodeDTO(KnowledgeNode kn) {
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
        return dto;
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
            nodes.add(toNodeDTO(kn));
        }
        GraphDataDTO result = new GraphDataDTO();
        result.setNodes(nodes);
        result.setEdges(edges);
        return result;
    }

    private String getNodeColor(KnowledgeNode node) {
        // 节点底色按学科区分；跨学科属性不覆盖学科色，
        // 跨学科节点由前端以紫色描边 + 更大尺寸标识。
        String subject = node.getSubject();
        return subject == null ? "#8F959E" : SUBJECT_COLORS.getOrDefault(subject, "#8F959E");
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

