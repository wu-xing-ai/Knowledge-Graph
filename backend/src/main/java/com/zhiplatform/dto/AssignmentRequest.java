package com.zhiplatform.dto;

public class AssignmentRequest {

    private String title;
    private String itemType;
    private String itemIds;
    private String targetType;
    private Long targetId;
    private String deadline;

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getItemType() { return itemType; }
    public void setItemType(String itemType) { this.itemType = itemType; }
    public String getItemIds() { return itemIds; }
    public void setItemIds(String itemIds) { this.itemIds = itemIds; }
    public String getTargetType() { return targetType; }
    public void setTargetType(String targetType) { this.targetType = targetType; }
    public Long getTargetId() { return targetId; }
    public void setTargetId(Long targetId) { this.targetId = targetId; }
    public String getDeadline() { return deadline; }
    public void setDeadline(String deadline) { this.deadline = deadline; }
}