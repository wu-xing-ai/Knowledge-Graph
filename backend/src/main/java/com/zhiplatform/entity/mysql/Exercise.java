package com.zhiplatform.entity.mysql;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "exercise")
public class Exercise {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String content;

    @Column(columnDefinition = "TEXT")
    private String answer;

    @Column(columnDefinition = "TEXT")
    private String analysis;

    private Integer difficulty = 1;

    @Column(name = "subject_ids", length = 200)
    private String subjectIds;

    @Column(name = "knowledge_ids", length = 500)
    private String knowledgeIds;

    @Column(name = "exam_outline", length = 200)
    private String examOutline;

    @Column(length = 30)
    private String type = "CHOICE";

    @Column(name = "create_by")
    private Long createBy;

    @Column(name = "create_time")
    private LocalDateTime createTime = LocalDateTime.now();

    @Column(name = "update_time")
    private LocalDateTime updateTime = LocalDateTime.now();

    public Exercise() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public String getAnswer() { return answer; }
    public void setAnswer(String answer) { this.answer = answer; }
    public String getAnalysis() { return analysis; }
    public void setAnalysis(String analysis) { this.analysis = analysis; }
    public Integer getDifficulty() { return difficulty; }
    public void setDifficulty(Integer difficulty) { this.difficulty = difficulty; }
    public String getSubjectIds() { return subjectIds; }
    public void setSubjectIds(String subjectIds) { this.subjectIds = subjectIds; }
    public String getKnowledgeIds() { return knowledgeIds; }
    public void setKnowledgeIds(String knowledgeIds) { this.knowledgeIds = knowledgeIds; }
    public String getExamOutline() { return examOutline; }
    public void setExamOutline(String examOutline) { this.examOutline = examOutline; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public Long getCreateBy() { return createBy; }
    public void setCreateBy(Long createBy) { this.createBy = createBy; }
    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
    public LocalDateTime getUpdateTime() { return updateTime; }
    public void setUpdateTime(LocalDateTime updateTime) { this.updateTime = updateTime; }
}
