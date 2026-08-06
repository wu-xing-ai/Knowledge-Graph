package com.zhiplatform.dto;

public class GroupVO {

    private Long id;
    private String name;
    private Long clazzId;
    private String clazzName;
    private Long leaderId;
    private String leaderName;
    private Long memberCount;

    public GroupVO() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public Long getClazzId() { return clazzId; }
    public void setClazzId(Long clazzId) { this.clazzId = clazzId; }
    public String getClazzName() { return clazzName; }
    public void setClazzName(String clazzName) { this.clazzName = clazzName; }
    public Long getLeaderId() { return leaderId; }
    public void setLeaderId(Long leaderId) { this.leaderId = leaderId; }
    public String getLeaderName() { return leaderName; }
    public void setLeaderName(String leaderName) { this.leaderName = leaderName; }
    public Long getMemberCount() { return memberCount; }
    public void setMemberCount(Long memberCount) { this.memberCount = memberCount; }
}