package com.zhiplatform.service;

import com.zhiplatform.dto.GraphDataDTO;
import com.zhiplatform.entity.neo4j.KnowledgeNode;
import com.zhiplatform.entity.mysql.GraphConfig;
import com.zhiplatform.entity.mysql.Subject;
import com.zhiplatform.repository.neo4j.GraphDataRepository;
import com.zhiplatform.repository.neo4j.KnowledgeNodeRepository;
import com.zhiplatform.repository.mysql.GraphConfigRepository;
import com.zhiplatform.repository.mysql.SubjectRepository;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class GraphService {

    private final KnowledgeNodeRepository knowledgeRepo;
    private final GraphDataRepository graphDataRepo;
    private final SubjectRepository subjectRepo;
    private final GraphConfigRepository configRepo;

    private static final Map<String, String> SUBJECT_COLORS = Map.of(
            "高中物理", "#4A90D9",
            "高中化学", "#F5A623",
            "高中生物", "#7ED321",
            "航天拓展", "#9B59B6",
            "环境科普", "#50E3C2"
    );

    public GraphService(KnowledgeNodeRepository knowledgeRepo, GraphDataRepository graphDataRepo,
                        SubjectRepository subjectRepo, GraphConfigRepository configRepo) {
        this.knowledgeRepo = knowledgeRepo;
        this.graphDataRepo = graphDataRepo;
        this.subjectRepo = subjectRepo;
        this.configRepo = configRepo;
    }

    public GraphDataDTO getGraphData(List<Long> subjectIds, String keyword, int limit) {
        List<Map<String, Object>> rawData;
        if (keyword != null && !keyword.isEmpty()) {
            rawData = graphDataRepo.searchByKeyword(keyword);
        } else if (subjectIds != null && !subjectIds.isEmpty()) {
            rawData = graphDataRepo.getGraphData(subjectIds, limit);
        } else {
            rawData = graphDataRepo.getAllGraphData(limit);
        }
        return convertToDTO(rawData);
    }

    public GraphDataDTO expandNode(Long nodeId) {
        List<Map<String, Object>> rawData = graphDataRepo.expandNode(nodeId);
        return convertToDTO(rawData);
    }

    private GraphDataDTO convertToDTO(List<Map<String, Object>> rawData) {
        Set<Long> nodeIds = new LinkedHashSet<>();
        List<GraphDataDTO.EdgeDTO> edges = new ArrayList<>();

        for (Map<String, Object> row : rawData) {
            KnowledgeNode source = (KnowledgeNode) row.get("n");
            KnowledgeNode target = (KnowledgeNode) row.get("m");
            if (source == null || target == null) continue;

            nodeIds.add(source.getId());
            nodeIds.add(target.getId());

            GraphDataDTO.EdgeDTO edge = new GraphDataDTO.EdgeDTO();
            edge.setSource(source.getId());
            edge.setTarget(target.getId());

            Object r = row.get("r");
            if (r instanceof org.neo4j.driver.types.Relationship) {
                org.neo4j.driver.types.Relationship rel = (org.neo4j.driver.types.Relationship) r;
                edge.setRelationType(rel.type());
            } else {
                edge.setRelationType("关联");
            }
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
        if (node.getIsCross() != null && node.getIsCross()) {
            return "#6C5CE7";
        }
        return SUBJECT_COLORS.getOrDefault(node.getSubject(), "#BDC3C7");
    }

    // 图谱配置
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
