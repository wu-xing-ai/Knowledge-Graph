package com.zhiplatform.dto;

/**
 * 抽取出的知识点实体候选
 */
public class ExtractEntityDTO {
    private String name;
    private String type;
    private String subject;
    private String description;

    public ExtractEntityDTO() {}
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public String getSubject() { return subject; }
    public void setSubject(String subject) { this.subject = subject; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
}
