package com.zhiplatform.entity.mysql;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * 文档导入记录：记录某账号导入过的文档，支持一键删除其解析出的数据
 */
@Entity
@Table(name = "document_import")
public class DocumentImport {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "file_name", nullable = false, length = 200)
    private String fileName;

    @Column(name = "node_count")
    private Integer nodeCount = 0;

    @Column(name = "relation_count")
    private Integer relationCount = 0;

    @Column(name = "create_time")
    private LocalDateTime createTime = LocalDateTime.now();

    public DocumentImport() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public String getFileName() { return fileName; }
    public void setFileName(String fileName) { this.fileName = fileName; }
    public Integer getNodeCount() { return nodeCount; }
    public void setNodeCount(Integer nodeCount) { this.nodeCount = nodeCount; }
    public Integer getRelationCount() { return relationCount; }
    public void setRelationCount(Integer relationCount) { this.relationCount = relationCount; }
    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
}
