package com.zhiplatform.service;

import com.zhiplatform.common.Result;
import com.zhiplatform.dto.AssignmentRecordVO;
import com.zhiplatform.dto.AssignmentRequest;
import com.zhiplatform.dto.AssignmentVO;
import com.zhiplatform.dto.ClazzVO;
import com.zhiplatform.dto.GroupVO;
import com.zhiplatform.dto.TaskVO;
import com.zhiplatform.entity.mysql.*;
import com.zhiplatform.entity.neo4j.KnowledgeNode;
import com.zhiplatform.repository.mysql.*;
import com.zhiplatform.repository.neo4j.KnowledgeNodeRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 教学组织与任务布置：教师创建班级/小组管理学生，把知识点或习题布置到班级/小组，
 * 学生端查看任务并提交，教师端查看/批改完成情况。MySQL 为数据源，同时同步镜像到图中。
 */
@Service
public class TeachingService {

    private final ClazzRepository clazzRepo;
    private final StudyGroupRepository groupRepo;
    private final GroupMemberRepository memberRepo;
    private final AssignmentRepository assignmentRepo;
    private final AssignmentRecordRepository recordRepo;
    private final UserRepository userRepo;
    private final KnowledgeNodeRepository knowledgeRepo;
    private final ExerciseRepository exerciseRepo;
    private final TeachingGraphService graphService;

    public TeachingService(ClazzRepository clazzRepo, StudyGroupRepository groupRepo,
                           GroupMemberRepository memberRepo, AssignmentRepository assignmentRepo,
                           AssignmentRecordRepository recordRepo, UserRepository userRepo,
                           KnowledgeNodeRepository knowledgeRepo, ExerciseRepository exerciseRepo,
                           TeachingGraphService graphService) {
        this.clazzRepo = clazzRepo;
        this.groupRepo = groupRepo;
        this.memberRepo = memberRepo;
        this.assignmentRepo = assignmentRepo;
        this.recordRepo = recordRepo;
        this.userRepo = userRepo;
        this.knowledgeRepo = knowledgeRepo;
        this.exerciseRepo = exerciseRepo;
        this.graphService = graphService;
    }

    // ==================== 班级 ====================

    public List<ClazzVO> listClazzes(Long currentUserId, String role) {
        List<Clazz> clazzes = "ADMIN".equalsIgnoreCase(role)
                ? clazzRepo.findAll()
                : clazzRepo.findByOwnerId(currentUserId);
        return clazzes.stream().map(this::toClazzVO).toList();
    }

    public ClazzVO createClazz(Clazz clazz, Long ownerId) {
        if (clazz.getName() == null || clazz.getName().isBlank()) {
            throw new IllegalArgumentException("班级名称不能为空");
        }
        clazz.setId(null);
        clazz.setOwnerId(ownerId);
        Clazz saved = clazzRepo.save(clazz);
        User teacher = userRepo.findById(ownerId).orElse(null);
        graphService.upsertClazz(saved.getId(), saved.getName());
        if (teacher != null) {
            graphService.syncTeacherToClazz(saved.getId(), saved.getName(), teacher.getId(),
                    displayName(teacher));
        }
        return toClazzVO(saved);
    }

    public ClazzVO updateClazz(Clazz clazz) {
        Clazz exist = clazzRepo.findById(clazz.getId())
                .orElseThrow(() -> new IllegalArgumentException("班级不存在"));
        exist.setName(clazz.getName() == null ? exist.getName() : clazz.getName());
        exist.setGrade(clazz.getGrade());
        exist.setSchool(clazz.getSchool());
        if (clazz.getOwnerId() != null) {
            exist.setOwnerId(clazz.getOwnerId());
        }
        Clazz saved = clazzRepo.save(exist);
        graphService.upsertClazz(saved.getId(), saved.getName());
        User teacher = userRepo.findById(saved.getOwnerId()).orElse(null);
        if (teacher != null) {
            graphService.syncTeacherToClazz(saved.getId(), saved.getName(), teacher.getId(),
                    displayName(teacher));
        }
        return toClazzVO(saved);
    }

    @Transactional
    public void deleteClazz(Long clazzId) {
        Clazz clazz = clazzRepo.findById(clazzId)
                .orElseThrow(() -> new IllegalArgumentException("班级不存在"));
        // 学生移出班级
        List<User> students = userRepo.findStudentsByClazzId(clazzId);
        students.forEach(s -> {
            s.setClazzId(null);
            s.setClassName(null);
            userRepo.save(s);
            graphService.removeStudentFromClazz(s.getId());
        });
        // 删除该班级的小组及其成员
        List<StudyGroup> groups = groupRepo.findByClazzIdOrderByIdDesc(clazzId);
        for (StudyGroup g : groups) {
            deleteGroupInternal(g.getId());
        }
        // 删除布置给该班级的任务及记录
        List<Assignment> listed = assignmentRepo.findByTargetTypeAndTargetId("CLASS", clazzId);
        for (Assignment a : listed) {
            deleteAssignmentInternal(a.getId());
        }
        clazzRepo.deleteById(clazzId);
        graphService.removeClazz(clazzId);
    }

    public List<User> listClassStudents(Long clazzId) {
        List<User> students = userRepo.findStudentsByClazzId(clazzId);
        students.forEach(u -> u.setPassword(null));
        return students;
    }

    public Result<Void> addClassMember(Long clazzId, Long userId) {
        Clazz clazz = clazzRepo.findById(clazzId)
                .orElseThrow(() -> new IllegalArgumentException("班级不存在"));
        User student = userRepo.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("学生不存在"));
        if (!"STUDENT".equalsIgnoreCase(student.getRole())) {
            return Result.error(400, "只能将学生加入班级");
        }
        if (student.getClazzId() != null && !student.getClazzId().equals(clazzId)) {
            return Result.error(400, "该学生已属于其他班级");
        }
        student.setClazzId(clazzId);
        student.setClassName(clazz.getName());
        userRepo.save(student);
        graphService.upsertStudent(student.getId(), displayName(student), clazzId);
        return Result.success();
    }

    public Result<Void> removeClassMember(Long clazzId, Long userId) {
        User student = userRepo.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("学生不存在"));
        if (student.getClazzId() == null || !student.getClazzId().equals(clazzId)) {
            return Result.error(400, "该学生不在此班级");
        }
        student.setClazzId(null);
        student.setClassName(null);
        userRepo.save(student);
        graphService.removeStudentFromClazz(userId);
        // 同时退出该班级下所有小组
        List<StudyGroup> groups = groupRepo.findByClazzIdOrderByIdDesc(clazzId);
        for (StudyGroup g : groups) {
            memberRepo.findByGroupIdAndUserId(g.getId(), userId).ifPresent(m -> {
                memberRepo.delete(m);
                graphService.removeGroupMember(g.getId(), userId);
            });
        }
        return Result.success();
    }

    // ==================== 小组 ====================

    public List<GroupVO> listGroups(Long clazzId) {
        List<StudyGroup> groups = clazzId == null
                ? groupRepo.findAll()
                : groupRepo.findByClazzIdOrderByIdDesc(clazzId);
        return groups.stream().map(this::toGroupVO).toList();
    }

    public GroupVO createGroup(StudyGroup group) {
        if (group.getName() == null || group.getName().isBlank()) {
            throw new IllegalArgumentException("小组名称不能为空");
        }
        if (group.getClazzId() == null) {
            throw new IllegalArgumentException("请选择所属班级");
        }
        group.setId(null);
        StudyGroup saved = groupRepo.save(group);
        graphService.upsertGroupMembers(saved.getId(), saved.getName(), saved.getClazzId(),
                Collections.emptyList(), Collections.emptyList());
        return toGroupVO(saved);
    }

    public GroupVO updateGroup(StudyGroup group) {
        StudyGroup exist = groupRepo.findById(group.getId())
                .orElseThrow(() -> new IllegalArgumentException("小组不存在"));
        if (group.getName() != null && !group.getName().isBlank()) {
            exist.setName(group.getName());
        }
        if (group.getLeaderId() != null) {
            exist.setLeaderId(group.getLeaderId());
        }
        if (group.getClazzId() != null) {
            exist.setClazzId(group.getClazzId());
        }
        StudyGroup saved = groupRepo.save(exist);
        List<GroupMember> members = memberRepo.findByGroupId(saved.getId());
        Map<Long, User> userMap = usersByIds(members.stream().map(GroupMember::getUserId).toList());
        List<Long> memberIds = members.stream().map(GroupMember::getUserId).toList();
        List<String> names = members.stream().map(m -> displayName(userMap.get(m.getUserId()))).toList();
        graphService.upsertGroupMembers(saved.getId(), saved.getName(), saved.getClazzId(),
                memberIds, names);
        return toGroupVO(saved);
    }

    @Transactional
    public void deleteGroup(Long groupId) {
        deleteGroupInternal(groupId);
    }

    private void deleteGroupInternal(Long groupId) {
        memberRepo.deleteByGroupId(groupId);
        // 删除布置到该小组的任务及记录
        List<Assignment> listed = assignmentRepo.findByTargetTypeAndTargetId("GROUP", groupId);
        for (Assignment a : listed) {
            deleteAssignmentInternal(a.getId());
        }
        groupRepo.deleteById(groupId);
        graphService.removeGroup(groupId);
    }

    public Map<String, Object> groupMembers(Long groupId) {
        StudyGroup group = groupRepo.findById(groupId)
                .orElseThrow(() -> new IllegalArgumentException("小组不存在"));
        List<GroupMember> members = memberRepo.findByGroupId(groupId);
        List<Long> memberIds = members.stream().map(GroupMember::getUserId).toList();
        Map<Long, User> memberUsers = usersByIds(memberIds);
        List<User> memberList = memberIds.stream().map(memberUsers::get)
                .filter(Objects::nonNull).toList();
        memberList.forEach(u -> u.setPassword(null));

        Set<Long> memberIdSet = new HashSet<>(memberIds);
        List<User> available = new ArrayList<>();
        if (group.getClazzId() != null) {
            List<User> clazzStudents = userRepo.findStudentsByClazzId(group.getClazzId());
            for (User s : clazzStudents) {
                if (!memberIdSet.contains(s.getId())) {
                    available.add(s);
                }
            }
        }
        available.forEach(u -> u.setPassword(null));

        Map<String, Object> result = new HashMap<>();
        result.put("group", toGroupVO(group));
        result.put("members", memberList);
        result.put("available", available);
        return result;
    }

    public Result<Void> addGroupMember(Long groupId, Long userId) {
        StudyGroup group = groupRepo.findById(groupId)
                .orElseThrow(() -> new IllegalArgumentException("小组不存在"));
        User student = userRepo.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("学生不存在"));
        if (!"STUDENT".equalsIgnoreCase(student.getRole())) {
            return Result.error(400, "只能添加学生到小组");
        }
        if (memberRepo.findByGroupIdAndUserId(groupId, userId).isPresent()) {
            return Result.error(400, "该学生已在小组中");
        }
        GroupMember member = new GroupMember();
        member.setGroupId(groupId);
        member.setUserId(userId);
        memberRepo.save(member);
        graphService.upsertGroupMembers(groupId, group.getName(), group.getClazzId(),
                List.of(userId), List.of(displayName(student)));
        return Result.success();
    }

    public Result<Void> removeGroupMember(Long groupId, Long userId) {
        GroupMember member = memberRepo.findByGroupIdAndUserId(groupId, userId)
                .orElseThrow(() -> new IllegalArgumentException("该学生不在小组中"));
        memberRepo.delete(member);
        graphService.removeGroupMember(groupId, userId);
        return Result.success();
    }

    // ==================== 学生候选 ====================

    public List<User> listStudents() {
        List<User> students = userRepo.findAllStudents();
        students.forEach(u -> {
            u.setPassword(null);
            if (u.getClazzId() != null) {
                clazzRepo.findById(u.getClazzId()).ifPresent(c -> u.setClassName(c.getName()));
            }
        });
        students.sort(Comparator.comparing(User::getId));
        return students;
    }

    public List<User> listClassCandidates(Long clazzId) {
        List<User> students = clazzId == null
                ? userRepo.findAllStudents()
                : userRepo.findStudentsExcludeClazz(clazzId);
        students.forEach(u -> u.setPassword(null));
        students.sort(Comparator.comparing(User::getId));
        return students;
    }

    // ==================== 布置任务 ====================

    public AssignmentVO createAssignment(AssignmentRequest req, Long teacherId) {
        if (req.getTitle() == null || req.getTitle().isBlank()) {
            throw new IllegalArgumentException("任务标题不能为空");
        }
        String itemType = req.getItemType() == null ? "KNOWLEDGE" : req.getItemType().toUpperCase();
        if (!"KNOWLEDGE".equals(itemType) && !"EXERCISE".equals(itemType)) {
            throw new IllegalArgumentException("任务内容类型不合法");
        }
        if (!"CLASS".equalsIgnoreCase(req.getTargetType()) && !"GROUP".equalsIgnoreCase(req.getTargetType())) {
            throw new IllegalArgumentException("布置对象类型不合法");
        }
        List<Long> itemIds = parseIds(req.getItemIds());
        if (itemIds.isEmpty()) {
            throw new IllegalArgumentException("请选择任务内容");
        }
        Assignment a = new Assignment();
        a.setTitle(req.getTitle());
        a.setItemType(itemType);
        a.setItemIds(joinIds(itemIds));
        a.setTargetType(req.getTargetType().toUpperCase());
        a.setTargetId(req.getTargetId());
        a.setDeadline(parseDeadline(req.getDeadline()));
        a.setCreateBy(teacherId);
        a.setStatus(1);
        Assignment saved = assignmentRepo.save(a);

        // 为该布置面向的学生生成待办记录
        List<Long> studentIds = targetStudents(req.getTargetType(), req.getTargetId());
        for (Long sid : studentIds) {
            AssignmentRecord r = new AssignmentRecord();
            r.setAssignmentId(saved.getId());
            r.setUserId(sid);
            r.setStatus("PENDING");
            recordRepo.save(r);
        }

        graphService.syncAssignment(saved.getId(), req.getTargetType().toUpperCase(),
                req.getTargetId(), itemType, itemIds);
        return toAssignmentVO(saved);
    }

    public List<AssignmentVO> listAssignments(Long teacherId, String role) {
        List<Assignment> list = "ADMIN".equalsIgnoreCase(role)
                ? assignmentRepo.findAll()
                : assignmentRepo.findByCreateByOrderByCreateTimeDesc(teacherId);
        return list.stream().map(this::toAssignmentVO).toList();
    }

    @Transactional
    public void deleteAssignment(Long assignmentId) {
        deleteAssignmentInternal(assignmentId);
    }

    private void deleteAssignmentInternal(Long assignmentId) {
        recordRepo.deleteByAssignmentId(assignmentId);
        assignmentRepo.deleteById(assignmentId);
        graphService.removeAssignment(assignmentId);
    }

    public List<AssignmentRecordVO> listAssignmentRecords(Long assignmentId) {
        List<AssignmentRecord> records = recordRepo.findByAssignmentId(assignmentId);
        Map<Long, User> users = usersByIds(records.stream().map(AssignmentRecord::getUserId).toList());
        return records.stream().map(r -> toRecordVO(r, users.get(r.getUserId()))).toList();
    }

    public Result<Void> gradeRecord(Long assignmentId, Long userId, Integer score) {
        AssignmentRecord record = recordRepo.findByAssignmentIdAndUserId(assignmentId, userId)
                .orElseThrow(() -> new IllegalArgumentException("记录不存在"));
        if (score != null && (score < 0 || score > 100)) {
            return Result.error(400, "分数需在 0 - 100 之间");
        }
        record.setScore(score);
        record.setStatus("GRADED");
        recordRepo.save(record);
        return Result.success();
    }

    // ==================== 学生任务 ====================

    public List<TaskVO> listStudentTasks(Long userId) {
        User student = userRepo.findById(userId).orElse(null);
        if (student == null || !"STUDENT".equalsIgnoreCase(student.getRole())) {
            return Collections.emptyList();
        }
        Set<Assignment> assignments = new LinkedHashSet<>();
        if (student.getClazzId() != null) {
            assignments.addAll(assignmentRepo.findByTargetTypeAndTargetId("CLASS", student.getClazzId()));
        }
        List<GroupMember> groupMembers = memberRepo.findByUserId(userId);
        for (GroupMember m : groupMembers) {
            assignments.addAll(assignmentRepo.findByTargetTypeAndTargetId("GROUP", m.getGroupId()));
        }

        List<TaskVO> tasks = new ArrayList<>();
        Map<Long, User> teacherMap = userRepo.findAll().stream()
                .collect(Collectors.toMap(User::getId, Function.identity()));
        Map<Long, AssignmentRecord> recordMap = new HashMap<>();
        recordRepo.findByUserId(userId).forEach(r -> recordMap.put(r.getAssignmentId(), r));

        Map<String, Object> targetNames = new HashMap<>();
        for (Assignment a : assignments) {
            if (a.getStatus() == null || a.getStatus() != 1) continue;
            TaskVO vo = new TaskVO();
            vo.setId(a.getId());
            vo.setTitle(a.getTitle());
            vo.setItemType(a.getItemType());
            vo.setItemIds(parseIds(a.getItemIds()));
            vo.setItemNames(resolveItemNames(a.getItemType(), vo.getItemIds()));
            vo.setTargetType(a.getTargetType());
            vo.setTargetId(a.getTargetId());
            vo.setTargetName(resolveTargetName(a));
            vo.setCreateTime(a.getCreateTime());
            vo.setDeadline(a.getDeadline());
            User teacher = teacherMap.get(a.getCreateBy());
            vo.setTeacherName(teacher == null ? "" : displayName(teacher));
            AssignmentRecord record = recordMap.get(a.getId());
            if (record != null) {
                vo.setRecordId(record.getId());
                vo.setStatus(record.getStatus());
                vo.setScore(record.getScore());
                vo.setSubmitTime(record.getSubmitTime());
            } else {
                vo.setStatus("PENDING");
            }
            tasks.add(vo);
        }
        tasks.sort(Comparator.comparing(TaskVO::getCreateTime,
                Comparator.nullsLast(Comparator.reverseOrder())));
        return tasks;
    }

    public Result<Void> submitTask(Long assignmentId, Long userId, String answer) {
        Assignment a = assignmentRepo.findById(assignmentId)
                .orElseThrow(() -> new IllegalArgumentException("任务不存在"));
        User student = userRepo.findById(userId).orElse(null);
        if (student == null || !"STUDENT".equalsIgnoreCase(student.getRole())) {
            return Result.error(400, "仅学生可提交任务");
        }
        if (!isTaskTarget(userId, student, a)) {
            return Result.error(400, "该任务未布置给你");
        }
        AssignmentRecord record = recordRepo.findByAssignmentIdAndUserId(assignmentId, userId)
                .orElse(null);
        if (record == null) {
            record = new AssignmentRecord();
            record.setAssignmentId(assignmentId);
            record.setUserId(userId);
        }
        record.setAnswer(answer);
        record.setStatus("SUBMITTED");
        record.setSubmitTime(LocalDateTime.now());
        recordRepo.save(record);
        return Result.success();
    }

    private boolean isTaskTarget(Long userId, User student, Assignment a) {
        if ("CLASS".equalsIgnoreCase(a.getTargetType())) {
            return student.getClazzId() != null
                    && student.getClazzId().equals(a.getTargetId());
        }
        if ("GROUP".equalsIgnoreCase(a.getTargetType())) {
            return memberRepo.findByGroupIdAndUserId(a.getTargetId(), userId).isPresent();
        }
        return false;
    }

    // ==================== 组装工具 ====================

    private List<Long> targetStudents(String targetType, Long targetId) {
        List<Long> ids = new ArrayList<>();
        if ("CLASS".equalsIgnoreCase(targetType)) {
            userRepo.findStudentsByClazzId(targetId).forEach(u -> ids.add(u.getId()));
        } else {
            memberRepo.findByGroupId(targetId).forEach(m -> ids.add(m.getUserId()));
        }
        return ids;
    }

    private ClazzVO toClazzVO(Clazz c) {
        ClazzVO vo = new ClazzVO();
        vo.setId(c.getId());
        vo.setName(c.getName());
        vo.setGrade(c.getGrade());
        vo.setSchool(c.getSchool());
        vo.setOwnerId(c.getOwnerId());
        vo.setCreateTime(c.getCreateTime());
        if (c.getOwnerId() != null) {
            userRepo.findById(c.getOwnerId())
                    .ifPresent(t -> vo.setOwnerName(displayName(t)));
        }
        vo.setStudentCount((long) userRepo.findStudentsByClazzId(c.getId()).size());
        return vo;
    }

    private GroupVO toGroupVO(StudyGroup g) {
        GroupVO vo = new GroupVO();
        vo.setId(g.getId());
        vo.setName(g.getName());
        vo.setClazzId(g.getClazzId());
        if (g.getClazzId() != null) {
            clazzRepo.findById(g.getClazzId()).ifPresent(c -> vo.setClazzName(c.getName()));
        }
        vo.setLeaderId(g.getLeaderId());
        if (g.getLeaderId() != null) {
            userRepo.findById(g.getLeaderId()).ifPresent(u -> vo.setLeaderName(displayName(u)));
        }
        vo.setMemberCount((long) memberRepo.findByGroupId(g.getId()).size());
        return vo;
    }

    private AssignmentVO toAssignmentVO(Assignment a) {
        AssignmentVO vo = new AssignmentVO();
        vo.setId(a.getId());
        vo.setTitle(a.getTitle());
        vo.setItemType(a.getItemType());
        List<Long> ids = parseIds(a.getItemIds());
        vo.setItemIds(ids);
        vo.setItemNames(resolveItemNames(a.getItemType(), ids));
        vo.setTargetType(a.getTargetType());
        vo.setTargetId(a.getTargetId());
        vo.setTargetName(resolveTargetName(a));
        vo.setDeadline(a.getDeadline());
        vo.setStatus(a.getStatus());
        vo.setCreateBy(a.getCreateBy());
        if (a.getCreateBy() != null) {
            userRepo.findById(a.getCreateBy()).ifPresent(u -> vo.setCreateByName(displayName(u)));
        }
        vo.setCreateTime(a.getCreateTime());
        return vo;
    }

    private AssignmentRecordVO toRecordVO(AssignmentRecord r, User u) {
        AssignmentRecordVO vo = new AssignmentRecordVO();
        vo.setId(r.getId());
        vo.setAssignmentId(r.getAssignmentId());
        vo.setUserId(r.getUserId());
        vo.setUserName(u == null ? "" : displayName(u));
        vo.setStatus(r.getStatus());
        vo.setAnswer(r.getAnswer());
        vo.setScore(r.getScore());
        vo.setSubmitTime(r.getSubmitTime());
        return vo;
    }

    private String resolveTargetName(Assignment a) {
        if (a.getTargetId() == null) return "";
        if ("CLASS".equalsIgnoreCase(a.getTargetType())) {
            return clazzRepo.findById(a.getTargetId()).map(Clazz::getName).orElse("");
        }
        return groupRepo.findById(a.getTargetId()).map(StudyGroup::getName).orElse("");
    }

    private List<String> resolveItemNames(String itemType, List<Long> ids) {
        List<String> names = new ArrayList<>();
        if ("KNOWLEDGE".equalsIgnoreCase(itemType)) {
            for (Long id : ids) {
                names.add(safeKnowledgeName(id));
            }
        } else {
            for (Long id : ids) {
                names.add(safeExerciseName(id));
            }
        }
        return names;
    }

    // 名称解析仅供展示；Neo4j/MySQL 不可用时降级为 id 占位，不阻断布置/列表等核心业务
    private String safeKnowledgeName(Long id) {
        try {
            KnowledgeNode node = knowledgeRepo.findById(id).orElse(null);
            return node == null ? "知识点 #" + id : node.getName();
        } catch (Exception e) {
            return "知识点 #" + id;
        }
    }

    private String safeExerciseName(Long id) {
        try {
            Exercise ex = exerciseRepo.findById(id).orElse(null);
            if (ex == null) return "习题 #" + id;
            return ex.getContent().replaceAll("\\r?\\n", " ");
        } catch (Exception e) {
            return "习题 #" + id;
        }
    }

    private Map<Long, User> usersByIds(List<Long> ids) {
        if (ids == null || ids.isEmpty()) return Map.of();
        return userRepo.findAllById(ids).stream()
                .collect(Collectors.toMap(User::getId, Function.identity()));
    }

    private String displayName(User u) {
        if (u == null) return "";
        return u.getRealName() == null || u.getRealName().isBlank()
                ? u.getUsername() : u.getRealName();
    }

    private List<Long> parseIds(String ids) {
        List<Long> result = new ArrayList<>();
        if (ids == null || ids.isBlank()) return result;
        for (String token : ids.split(",")) {
            try {
                result.add(Long.valueOf(token.trim()));
            } catch (NumberFormatException ignored) {
            }
        }
        return result;
    }

    private String joinIds(List<Long> ids) {
        return ids.stream().map(String::valueOf).reduce((a, b) -> a + "," + b).orElse("");
    }

    private LocalDateTime parseDeadline(String deadline) {
        if (deadline == null || deadline.isBlank()) return null;
        try {
            return LocalDateTime.parse(deadline.trim(),
                    DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
        } catch (Exception e) {
            try {
                return LocalDateTime.parse(deadline.trim());
            } catch (Exception ex) {
                return null;
            }
        }
    }
}