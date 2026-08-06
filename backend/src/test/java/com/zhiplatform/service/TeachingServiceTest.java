package com.zhiplatform.service;

import com.zhiplatform.dto.AssignmentRequest;
import com.zhiplatform.dto.GroupVO;
import com.zhiplatform.entity.mysql.*;
import com.zhiplatform.repository.mysql.*;
import com.zhiplatform.repository.neo4j.KnowledgeNodeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TeachingServiceTest {

    @Mock
    private ClazzRepository clazzRepo;
    @Mock
    private StudyGroupRepository groupRepo;
    @Mock
    private GroupMemberRepository memberRepo;
    @Mock
    private AssignmentRepository assignmentRepo;
    @Mock
    private AssignmentRecordRepository recordRepo;
    @Mock
    private com.zhiplatform.repository.mysql.UserRepository userRepo;
    @Mock
    private KnowledgeNodeRepository knowledgeRepo;
    @Mock
    private ExerciseRepository exerciseRepo;
    @Mock
    private TeachingGraphService graphService;

    private TeachingService teachingService;

    @BeforeEach
    void setUp() {
        teachingService = new TeachingService(clazzRepo, groupRepo, memberRepo, assignmentRepo,
                recordRepo, userRepo, knowledgeRepo, exerciseRepo, graphService);
    }

    @Test
    void createClazz_setsOwnerAndSyncsGraph() {
        Clazz clazz = new Clazz();
        clazz.setName("高一(1)班");
        User teacher = new User();
        teacher.setId(10L);
        teacher.setRealName("王老师");
        when(clazzRepo.save(any(Clazz.class))).thenAnswer(inv -> {
            Clazz c = inv.getArgument(0);
            c.setId(1L);
            return c;
        });
        when(userRepo.findById(10L)).thenReturn(Optional.of(teacher));
        when(userRepo.findStudentsByClazzId(1L)).thenReturn(List.of());

        var vo = teachingService.createClazz(clazz, 10L);

        assertEquals("高一(1)班", vo.getName());
        assertEquals(10L, vo.getOwnerId());
        assertEquals("王老师", vo.getOwnerName());
        verify(graphService).upsertClazz(1L, "高一(1)班");
        verify(graphService).syncTeacherToClazz(1L, "高一(1)班", 10L, "王老师");
    }

    @Test
    void addClassMember_duplicateClass_rejected() {
        User student = new User();
        student.setId(2L);
        student.setRole("STUDENT");
        student.setClazzId(99L);
        when(clazzRepo.findById(1L)).thenReturn(Optional.of(new Clazz()));
        when(userRepo.findById(2L)).thenReturn(Optional.of(student));

        var result = teachingService.addClassMember(1L, 2L);

        assertEquals(400, result.getCode());
        verify(userRepo, never()).save(any());
    }

    @Test
    void addClassMember_success_syncsGraph() {
        Clazz clazz = new Clazz();
        clazz.setId(1L);
        clazz.setName("高一(1)班");
        User student = new User();
        student.setId(2L);
        student.setRole("STUDENT");
        student.setUsername("stu1");
        when(clazzRepo.findById(1L)).thenReturn(Optional.of(clazz));
        when(userRepo.findById(2L)).thenReturn(Optional.of(student));

        var result = teachingService.addClassMember(1L, 2L);

        assertEquals(200, result.getCode());
        assertEquals(1L, student.getClazzId());
        assertEquals("高一(1)班", student.getClassName());
        verify(graphService).upsertStudent(2L, "stu1", 1L);
    }

    @Test
    void deleteClazz_cleansUpStudentsGroupsAndAssignments() {
        Clazz clazz = new Clazz();
        clazz.setId(1L);
        User student = new User();
        student.setId(2L);
        student.setClazzId(1L);
        StudyGroup group = new StudyGroup();
        group.setId(5L);
        Assignment assignment = new Assignment();
        assignment.setId(7L);
        when(clazzRepo.findById(1L)).thenReturn(Optional.of(clazz));
        when(userRepo.findStudentsByClazzId(1L)).thenReturn(List.of(student));
        when(groupRepo.findByClazzIdOrderByIdDesc(1L)).thenReturn(List.of(group));
        when(assignmentRepo.findByTargetTypeAndTargetId("CLASS", 1L)).thenReturn(List.of(assignment));
        when(assignmentRepo.findByTargetTypeAndTargetId("GROUP", 5L)).thenReturn(List.of());

        teachingService.deleteClazz(1L);

        assertNull(student.getClazzId());
        verify(groupRepo).deleteById(5L);
        verify(assignmentRepo).deleteById(7L);
        verify(recordRepo).deleteByAssignmentId(7L);
        verify(clazzRepo).deleteById(1L);
        verify(graphService).removeClazz(1L);
    }

    @Test
    void createAssignment_generatesPendingRecordsForClassStudents() {
        AssignmentRequest req = new AssignmentRequest();
        req.setTitle("牛顿第二定律预习");
        req.setItemType("KNOWLEDGE");
        req.setItemIds("100,101");
        req.setTargetType("CLASS");
        req.setTargetId(1L);

        User s1 = new User();
        s1.setId(21L);
        User s2 = new User();
        s2.setId(22L);
        when(userRepo.findStudentsByClazzId(1L)).thenReturn(List.of(s1, s2));
        when(assignmentRepo.save(any(Assignment.class))).thenAnswer(inv -> {
            Assignment a = inv.getArgument(0);
            a.setId(1L);
            return a;
        });

        var vo = teachingService.createAssignment(req, 10L);

        assertEquals(1L, vo.getId());
        assertEquals("牛顿第二定律预习", vo.getTitle());
        verify(recordRepo, times(2)).save(any(AssignmentRecord.class));
        verify(graphService).syncAssignment(1L, "CLASS", 1L, "KNOWLEDGE", List.of(100L, 101L));
    }

    @Test
    void listStudentTasks_onlyReturnsAssignmentsForOwnClassAndGroups() {
        User student = new User();
        student.setId(21L);
        student.setRole("STUDENT");
        student.setClazzId(1L);

        Assignment classAssignment = new Assignment();
        classAssignment.setId(1L);
        classAssignment.setTitle("班级任务");
        classAssignment.setItemType("KNOWLEDGE");
        classAssignment.setItemIds("100");
        classAssignment.setTargetType("CLASS");
        classAssignment.setTargetId(1L);
        classAssignment.setStatus(1);

        GroupMember member = new GroupMember();
        member.setGroupId(5L);
        member.setUserId(21L);

        when(userRepo.findById(21L)).thenReturn(Optional.of(student));
        when(assignmentRepo.findByTargetTypeAndTargetId("CLASS", 1L)).thenReturn(List.of(classAssignment));
        when(memberRepo.findByUserId(21L)).thenReturn(List.of(member));
        when(assignmentRepo.findByTargetTypeAndTargetId("GROUP", 5L)).thenReturn(List.of());
        when(recordRepo.findByUserId(21L)).thenReturn(List.of());
        when(userRepo.findAll()).thenReturn(List.of());
        when(knowledgeRepo.findById(100L)).thenReturn(Optional.empty());

        var tasks = teachingService.listStudentTasks(21L);

        assertEquals(1, tasks.size());
        assertEquals("班级任务", tasks.get(0).getTitle());
        assertEquals("PENDING", tasks.get(0).getStatus());
    }

    @Test
    void submitTask_marksRecordSubmitted() {
        Assignment assignment = new Assignment();
        assignment.setId(1L);
        assignment.setTargetType("CLASS");
        assignment.setTargetId(1L);
        User student = new User();
        student.setId(21L);
        student.setRole("STUDENT");
        student.setClazzId(1L);
        when(assignmentRepo.findById(1L)).thenReturn(Optional.of(assignment));
        when(userRepo.findById(21L)).thenReturn(Optional.of(student));
        when(recordRepo.findByAssignmentIdAndUserId(1L, 21L)).thenReturn(Optional.empty());

        var result = teachingService.submitTask(1L, 21L, "我的答案");

        assertEquals(200, result.getCode());
        verify(recordRepo).save(any(AssignmentRecord.class));
    }

    @Test
    void submitTask_notTarget_rejected() {
        Assignment assignment = new Assignment();
        assignment.setId(1L);
        assignment.setTargetType("GROUP");
        assignment.setTargetId(5L);
        User student = new User();
        student.setId(21L);
        student.setRole("STUDENT");
        student.setClazzId(1L);
        when(assignmentRepo.findById(1L)).thenReturn(Optional.of(assignment));
        when(userRepo.findById(21L)).thenReturn(Optional.of(student));
        when(memberRepo.findByGroupIdAndUserId(5L, 21L)).thenReturn(Optional.empty());

        var result = teachingService.submitTask(1L, 21L, "我的答案");

        assertEquals(400, result.getCode());
        verify(recordRepo, never()).save(any());
    }

    @Test
    void groupMembers_returnsMembersAndAvailable() {
        StudyGroup group = new StudyGroup();
        group.setId(5L);
        group.setName("火箭小组");
        group.setClazzId(1L);
        GroupMember member = new GroupMember();
        member.setGroupId(5L);
        member.setUserId(21L);
        User memberUser = new User();
        memberUser.setId(21L);
        memberUser.setUsername("stu1");
        User availableUser = new User();
        availableUser.setId(22L);
        availableUser.setUsername("stu2");

        when(groupRepo.findById(5L)).thenReturn(Optional.of(group));
        when(memberRepo.findByGroupId(5L)).thenReturn(List.of(member));
        when(userRepo.findAllById(List.of(21L))).thenReturn(List.of(memberUser));
        when(userRepo.findStudentsByClazzId(1L)).thenReturn(List.of(memberUser, availableUser));

        Map<String, Object> result = teachingService.groupMembers(5L);

        assertEquals("火箭小组", ((GroupVO) result.get("group")).getName());
        assertEquals(1, ((List<?>) result.get("members")).size());
        assertEquals(1, ((List<?>) result.get("available")).size());
    }
}