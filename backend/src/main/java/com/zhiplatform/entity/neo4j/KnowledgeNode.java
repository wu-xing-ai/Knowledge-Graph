package com.zhiplatform.entity.neo4j;

import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;
import org.springframework.data.neo4j.core.schema.Property;
import org.springframework.data.neo4j.core.schema.GeneratedValue;
import java.time.LocalDateTime;

@Node("Knowledge")
public class KnowledgeNode {

    @Id
    @GeneratedValue
    private Long id;

    @Property("name")
    private String name;

    @Property("type")
    private String type;

    @Property("subject")
    private String subject;

    @Property("subjectId")
    private Long subjectId;

    @Property("difficulty")
    private Integer difficulty;

    @Property("examOutline")
    private String examOutline;

    @Property("description")
    private String description;

    @Property("tags")
    private String tags;

    @Property("isCross")
    private Boolean isCross;

    @Property("createTime")
    private LocalDateTime createTime;

    @Property("createBy")
    private Long createBy;

    public KnowledgeNode() {}

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
    public String getExamOutline() { return examOutline; }
    public void setExamOutline(String examOutline) { this.examOutline = examOutline; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getTags() { return tags; }
    public void setTags(String tags) { this.tags = tags; }
    public Boolean getIsCross() { return isCross; }
    public void setIsCross(Boolean isCross) { this.isCross = isCross; }
    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
    public Long getCreateBy() { return createBy; }
    public void setCreateBy(Long createBy) { this.createBy = createBy; }
}
