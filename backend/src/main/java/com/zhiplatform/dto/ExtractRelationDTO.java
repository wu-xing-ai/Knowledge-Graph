package com.zhiplatform.dto;

/**
 * 抽取出的交叉关系候选
 */
public class ExtractRelationDTO {
    private String source;
    private String target;
    private String relationType;
    private String description;

    public ExtractRelationDTO() {}
    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }
    public String getTarget() { return target; }
    public void setTarget(String target) { this.target = target; }
    public String getRelationType() { return relationType; }
    public void setRelationType(String relationType) { this.relationType = relationType; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
}
