package com.zhiplatform.dto;

import java.util.List;

public class GraphDataDTO {

    private List<NodeDTO> nodes;
    private List<EdgeDTO> edges;

    public GraphDataDTO() {}

    public List<NodeDTO> getNodes() { return nodes; }
    public void setNodes(List<NodeDTO> nodes) { this.nodes = nodes; }
    public List<EdgeDTO> getEdges() { return edges; }
    public void setEdges(List<EdgeDTO> edges) { this.edges = edges; }

    public static class NodeDTO {
        private Long id;
        private String name;
        private String type;
        private String subject;
        private Long subjectId;
        private Integer difficulty;
        private String description;
        private String tags;
        private Boolean isCross;
        private String color;
        private Integer size;

        public NodeDTO() {}

        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getType() { return type; }
        public void setType(String type) { this.type = type; }
        public String getSubject() { return subject; }
        public void setSubject(String subject) { this.subject = subject; }
        public Long getSubjectId() { return subjectId; }
        public void setSubjectId(Long subjectId) { this.subjectId = subjectId; }
        public Integer getDifficulty() { return difficulty; }
        public void setDifficulty(Integer difficulty) { this.difficulty = difficulty; }
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        public String getTags() { return tags; }
        public void setTags(String tags) { this.tags = tags; }
        public Boolean getIsCross() { return isCross; }
        public void setIsCross(Boolean isCross) { this.isCross = isCross; }
        public String getColor() { return color; }
        public void setColor(String color) { this.color = color; }
        public Integer getSize() { return size; }
        public void setSize(Integer size) { this.size = size; }
    }

    public static class EdgeDTO {
        private Long id;
        private Long source;
        private Long target;
        private String relationType;
        private String description;

        public EdgeDTO() {}

        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public Long getSource() { return source; }
        public void setSource(Long source) { this.source = source; }
        public Long getTarget() { return target; }
        public void setTarget(Long target) { this.target = target; }
        public String getRelationType() { return relationType; }
        public void setRelationType(String relationType) { this.relationType = relationType; }
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
    }
}
