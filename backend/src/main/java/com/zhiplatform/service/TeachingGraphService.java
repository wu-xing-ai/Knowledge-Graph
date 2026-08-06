package com.zhiplatform.service;

import org.springframework.data.neo4j.core.Neo4jClient;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * 教学组织（教师 - 班级 - 小组 - 学生 - 布置任务）在图数据库中的镜像同步。
 * 图中保留核心关系，便于后续做路径查询。Neo4j 不可用时静默降级，不影响 MySQL 业务。
 */
@Service
public class TeachingGraphService {

    private final Neo4jClient neo4jClient;

    public TeachingGraphService(Neo4jClient neo4jClient) {
        this.neo4jClient = neo4jClient;
    }

    public void syncTeacherToClazz(Long clazzId, String clazzName, Long teacherId, String teacherName) {
        safeRun("MERGE (c:Clazz {id: $clazzId}) SET c.name = $clazzName " +
                "WITH c " +
                "MERGE (t:Teacher {userId: $teacherId}) SET t.name = $teacherName " +
                "MERGE (t)-[:TEACHES]->(c)",
                Map.of("clazzId", clazzId, "clazzName", clazzName,
                        "teacherId", teacherId, "teacherName", teacherName));
    }

    public void upsertClazz(Long clazzId, String clazzName) {
        safeRun("MERGE (c:Clazz {id: $clazzId}) SET c.name = $clazzName",
                Map.of("clazzId", clazzId, "clazzName", clazzName));
    }

    public void removeClazz(Long clazzId) {
        safeRun("MATCH (c:Clazz {id: $clazzId}) DETACH DELETE c", Map.of("clazzId", clazzId));
    }

    public void upsertStudent(Long userId, String userName, Long clazzId) {
        if (clazzId == null) {
            safeRun("MATCH (s:Student {userId: $userId}) " +
                            "OPTIONAL MATCH (s)-[r:BELONGS_TO]->(:Clazz) DELETE r " +
                            "SET s.name = $userName",
                    Map.of("userId", userId, "userName", userName));
            return;
        }
        safeRun("MERGE (s:Student {userId: $userId}) SET s.name = $userName " +
                "WITH s " +
                "MATCH (c:Clazz {id: $clazzId}) " +
                "MERGE (s)-[:BELONGS_TO]->(c)",
                Map.of("userId", userId, "userName", userName, "clazzId", clazzId));
    }

    public void removeStudentFromClazz(Long userId) {
        safeRun("MATCH (s:Student {userId: $userId}) " +
                "OPTIONAL MATCH (s)-[r:BELONGS_TO]->(:Clazz) DELETE r",
                Map.of("userId", userId));
    }

    public void upsertGroupMembers(Long groupId, String groupName, Long clazzId,
                                   List<Long> memberIds, List<String> memberNames) {
        safeRun("MERGE (g:StudyGroup {id: $groupId}) SET g.name = $groupName, g.clazzId = $clazzId",
                Map.of("groupId", groupId, "groupName", groupName, "clazzId", clazzId));
        for (int i = 0; i < memberIds.size(); i++) {
            Long uid = memberIds.get(i);
            String name = (memberNames != null && i < memberNames.size() && memberNames.get(i) != null)
                    ? memberNames.get(i) : "";
            safeRun("MERGE (s:Student {userId: $uid}) SET s.name = $name " +
                    "WITH s " +
                    "MATCH (g:StudyGroup {id: $groupId}) " +
                    "MERGE (s)-[:MEMBER_OF]->(g)",
                    Map.of("uid", uid, "name", name, "groupId", groupId));
        }
    }

    public void removeGroupMember(Long groupId, Long userId) {
        safeRun("MATCH (s:Student {userId: $userId})-[r:MEMBER_OF]->(g:StudyGroup {id: $groupId}) DELETE r",
                Map.of("groupId", groupId, "userId", userId));
    }

    public void removeGroup(Long groupId) {
        safeRun("MATCH (g:StudyGroup {id: $groupId}) DETACH DELETE g", Map.of("groupId", groupId));
    }

    /**
     * 布置任务在图中的落点：目标（班级/小组）--[HAS_ASSIGNMENT]-> 内容（知识点/习题节点）。
     */
    public void syncAssignment(Long assignmentId, String targetType, Long targetId,
                               String itemType, List<Long> itemIds) {
        String targetLabel = "CLASS".equalsIgnoreCase(targetType) ? "Clazz" : "StudyGroup";
        if ("KNOWLEDGE".equalsIgnoreCase(itemType)) {
            safeRun("MERGE (t:" + targetLabel + " {id: $targetId}) WITH t UNWIND $itemIds AS iid " +
                            "MATCH (k:Knowledge {id: iid}) " +
                            "MERGE (t)-[r:HAS_ASSIGNMENT {assignmentId: $assignmentId}]->(k)",
                    Map.of("targetId", targetId, "assignmentId", assignmentId, "itemIds", itemIds));
        } else {
            safeRun("MERGE (t:" + targetLabel + " {id: $targetId}) WITH t UNWIND $itemIds AS iid " +
                            "MERGE (e:Exercise {id: iid}) " +
                            "MERGE (t)-[r:HAS_ASSIGNMENT {assignmentId: $assignmentId}]->(e)",
                    Map.of("targetId", targetId, "assignmentId", assignmentId, "itemIds", itemIds));
        }
    }

    public void removeAssignment(Long assignmentId) {
        safeRun("MATCH ()-[r:HAS_ASSIGNMENT {assignmentId: $assignmentId}]->() DELETE r",
                Map.of("assignmentId", assignmentId));
    }

    private void safeRun(String cypher, Map<String, Object> params) {
        try {
            if (neo4jClient == null) return;
            neo4jClient.query(cypher).bindAll(params).run();
        } catch (Exception ex) {
            // 图库不可用 / 结构变化时静默降级
        }
    }
}