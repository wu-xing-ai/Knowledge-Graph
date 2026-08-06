package com.zhiplatform.controller;

import com.zhiplatform.common.Result;
import com.zhiplatform.dto.AssignmentRecordVO;
import com.zhiplatform.dto.AssignmentRequest;
import com.zhiplatform.dto.AssignmentVO;
import com.zhiplatform.dto.ClazzVO;
import com.zhiplatform.dto.GroupVO;
import com.zhiplatform.dto.TaskVO;
import com.zhiplatform.entity.mysql.Clazz;
import com.zhiplatform.entity.mysql.StudyGroup;
import com.zhiplatform.entity.mysql.User;
import com.zhiplatform.repository.mysql.UserRepository;
import com.zhiplatform.service.TeachingService;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;
import java.util.Map;

/**
 * 教学组织与任务布置。教师/管理员：班级、小组、布置任务、查看批改；
 * 学生：查看我的任务并提交。
 */
@RestController
@RequestMapping("/api/teaching")
public class TeachingController {

    private final TeachingService teachingService;
    private final UserRepository userRepository;

    public TeachingController(TeachingService teachingService,
                              UserRepository userRepository) {
        this.teachingService = teachingService;
        this.userRepository = userRepository;
    }

    private User currentUser(Principal principal) {
        if (principal == null || principal.getName() == null) return null;
        return userRepository.findByUsername(principal.getName()).orElse(null);
    }

    // ==================== 班级 ====================

    @GetMapping("/manage/clazz/list")
    public Result<List<ClazzVO>> listClazzes(Principal principal) {
        User user = currentUser(principal);
        if (user == null) return Result.error(401, "未登录");
        return Result.success(teachingService.listClazzes(user.getId(), user.getRole()));
    }

    @PostMapping("/manage/clazz")
    public Result<ClazzVO> createClazz(@RequestBody Clazz clazz, Principal principal) {
        User user = currentUser(principal);
        if (user == null) return Result.error(401, "未登录");
        return Result.success(teachingService.createClazz(clazz, user.getId()));
    }

    @PutMapping("/manage/clazz")
    public Result<ClazzVO> updateClazz(@RequestBody Clazz clazz) {
        return Result.success(teachingService.updateClazz(clazz));
    }

    @DeleteMapping("/manage/clazz/{id}")
    public Result<Void> deleteClazz(@PathVariable Long id) {
        teachingService.deleteClazz(id);
        return Result.success();
    }

    @GetMapping("/manage/clazz/{id}/students")
    public Result<List<User>> listClassStudents(@PathVariable Long id) {
        return Result.success(teachingService.listClassStudents(id));
    }

    @PostMapping("/manage/clazz/{id}/member")
    public Result<Void> addClassMember(@PathVariable Long id, @RequestBody Map<String, Object> params) {
        Long userId = Long.valueOf(params.get("userId").toString());
        return teachingService.addClassMember(id, userId);
    }

    @DeleteMapping("/manage/clazz/{id}/member/{userId}")
    public Result<Void> removeClassMember(@PathVariable Long id, @PathVariable Long userId) {
        return teachingService.removeClassMember(id, userId);
    }

    // ==================== 小组 ====================

    @GetMapping("/manage/group/list")
    public Result<List<GroupVO>> listGroups(@RequestParam(required = false) Long clazzId) {
        return Result.success(teachingService.listGroups(clazzId));
    }

    @PostMapping("/manage/group")
    public Result<GroupVO> createGroup(@RequestBody StudyGroup group) {
        return Result.success(teachingService.createGroup(group));
    }

    @PutMapping("/manage/group")
    public Result<GroupVO> updateGroup(@RequestBody StudyGroup group) {
        return Result.success(teachingService.updateGroup(group));
    }

    @DeleteMapping("/manage/group/{id}")
    public Result<Void> deleteGroup(@PathVariable Long id) {
        teachingService.deleteGroup(id);
        return Result.success();
    }

    @GetMapping("/manage/group/{id}/members")
    public Result<Map<String, Object>> groupMembers(@PathVariable Long id) {
        return Result.success(teachingService.groupMembers(id));
    }

    @PostMapping("/manage/group/{id}/member")
    public Result<Void> addGroupMember(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        Long userId = Long.valueOf(body.get("userId").toString());
        return teachingService.addGroupMember(id, userId);
    }

    @DeleteMapping("/manage/group/{id}/member/{userId}")
    public Result<Void> removeGroupMember(@PathVariable Long id, @PathVariable Long userId) {
        return teachingService.removeGroupMember(id, userId);
    }

    // ==================== 学生候选 ====================

    @GetMapping("/manage/students")
    public Result<List<User>> listStudents() {
        return Result.success(teachingService.listStudents());
    }

    @GetMapping("/manage/students/candidates")
    public Result<List<User>> listCandidates(@RequestParam(required = false) Long clazzId) {
        return Result.success(teachingService.listClassCandidates(clazzId));
    }

    // ==================== 布置任务（教师） ====================

    @GetMapping("/manage/assignment/list")
    public Result<List<AssignmentVO>> listAssignments(Principal principal) {
        User user = currentUser(principal);
        if (user == null) return Result.error(401, "未登录");
        return Result.success(teachingService.listAssignments(user.getId(), user.getRole()));
    }

    @PostMapping("/manage/assignment")
    public Result<AssignmentVO> createAssignment(@RequestBody AssignmentRequest request,
                                                 Principal principal) {
        User user = currentUser(principal);
        if (user == null) return Result.error(401, "未登录");
        return Result.success(teachingService.createAssignment(request, user.getId()));
    }

    @DeleteMapping("/manage/assignment/{id}")
    public Result<Void> deleteAssignment(@PathVariable Long id) {
        teachingService.deleteAssignment(id);
        return Result.success();
    }

    @GetMapping("/manage/assignment/{id}/records")
    public Result<List<AssignmentRecordVO>> listAssignmentRecords(@PathVariable Long id) {
        return Result.success(teachingService.listAssignmentRecords(id));
    }

    @PutMapping("/manage/assignment/{id}/grade")
    public Result<Void> gradeRecord(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        Long userId = Long.valueOf(body.get("userId").toString());
        Integer score = body.get("score") == null ? null : Integer.valueOf(body.get("score").toString());
        return teachingService.gradeRecord(id, userId, score);
    }

    // ==================== 学生任务 ====================

    @GetMapping("/task/list")
    public Result<List<TaskVO>> listStudentTasks(Principal principal) {
        User user = currentUser(principal);
        if (user == null) return Result.error(401, "未登录");
        return Result.success(teachingService.listStudentTasks(user.getId()));
    }

    @PostMapping("/task/submit")
    public Result<Void> submitTask(Principal principal, @RequestBody Map<String, Object> body) {
        User user = currentUser(principal);
        if (user == null) return Result.error(401, "未登录");
        Long assignmentId = Long.valueOf(body.get("assignmentId").toString());
        String answer = body.get("answer") == null ? "" : body.get("answer").toString();
        return teachingService.submitTask(assignmentId, user.getId(), answer);
    }
}