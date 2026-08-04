package com.zhiplatform.entity.mysql;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * 文档抽取草稿：抽取完成先落库，供教师稍后确认导入（切换页面不丢失）
 */
@Entity
@Table(name = "document_draft")
public class DocumentDraft {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "file_name", nullable = false, length = 200)
    private String fileName;

    @Column(name = "source", length = 50)
    private String source;

    @Column(name = "char_count")
    private Integer charCount = 0;

    @Column(name = "entity_count")
    private Integer entityCount = 0;

    @Column(name = "relation_count")
    private Integer relationCount = 0;

    @Column(name = "data_json", columnDefinition = "LONGTEXT")
    private String dataJson;

    @Column(name = "create_time")
    private LocalDateTime createTime = LocalDateTime.now();

    public DocumentDraft() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public String getFileName() { return fileName; }
    public void setFileName(String fileName) { this.fileName = fileName; }
    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }
    public Integer getCharCount() { return charCount; }
    public void setCharCount(Integer charCount) { this.charCount = charCount; }
    public Integer getEntityCount() { return entityCount; }
    public void setEntityCount(Integer entityCount) { this.entityCount = entityCount; }
    public Integer getRelationCount() { return relationCount; }
    public void setRelationCount(Integer relationCount) { this.relationCount = relationCount; }
    public String getDataJson() { return dataJson; }
    public void setDataJson(String dataJson) { this.dataJson = dataJson; }
    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
}
