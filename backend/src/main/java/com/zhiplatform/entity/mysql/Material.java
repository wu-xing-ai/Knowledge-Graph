package com.zhiplatform.entity.mysql;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * 学习资料：教师按班级分享的轻量资料（标题 + 说明/链接文本）。
 */
@Entity
@Table(name = "material")
public class Material {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 资料标题 */
    @Column(nullable = false, length = 200)
    private String title;

    /** 资料说明 / 链接文本 */
    @Column(columnDefinition = "TEXT")
    private String content;

    /** 归属班级 */
    @Column(name = "clazz_id", nullable = false)
    private Long clazzId;

    /** 创建人（教师） */
    @Column(name = "create_by")
    private Long createBy;

    @Column(name = "create_time")
    private LocalDateTime createTime = LocalDateTime.now();

    public Material() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public Long getClazzId() { return clazzId; }
    public void setClazzId(Long clazzId) { this.clazzId = clazzId; }
    public Long getCreateBy() { return createBy; }
    public void setCreateBy(Long createBy) { this.createBy = createBy; }
    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
}
