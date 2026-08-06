package com.zhiplatform.dto;

import java.time.LocalDateTime;

public class ClazzVO {

    private Long id;
    private String name;
    private String grade;
    private String school;
    private Long ownerId;
    private String ownerName;
    private Long studentCount;
    private LocalDateTime createTime;

    public ClazzVO() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getGrade() { return grade; }
    public void setGrade(String grade) { this.grade = grade; }
    public String getSchool() { return school; }
    public void setSchool(String school) { this.school = school; }
    public Long getOwnerId() { return ownerId; }
    public void setOwnerId(Long ownerId) { this.ownerId = ownerId; }
    public String getOwnerName() { return ownerName; }
    public void setOwnerName(String ownerName) { this.ownerName = ownerName; }
    public Long getStudentCount() { return studentCount; }
    public void setStudentCount(Long studentCount) { this.studentCount = studentCount; }
    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
}