package com.zhiplatform.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zhiplatform.common.Result;
import com.zhiplatform.dto.AssignmentRequest;
import com.zhiplatform.dto.AssignmentVO;
import com.zhiplatform.dto.ClazzVO;
import com.zhiplatform.dto.TaskVO;
import com.zhiplatform.entity.mysql.Clazz;
import com.zhiplatform.entity.mysql.User;
import com.zhiplatform.repository.mysql.UserRepository;
import com.zhiplatform.service.TeachingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.security.Principal;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class TeachingControllerTest {

    private MockMvc mockMvc;
    private ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());

    @Mock
    private TeachingService teachingService;
    @Mock
    private UserRepository userRepository;

    private Principal principal = () -> "teacher1";

    @BeforeEach
    void setUp() {
        TeachingController controller = new TeachingController(teachingService, userRepository);
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    private void mockCurrentUser(String role) {
        User user = new User();
        user.setId(10L);
        user.setUsername("teacher1");
        user.setRole(role);
        when(userRepository.findByUsername("teacher1")).thenReturn(Optional.of(user));
    }

    @Test
    void createClazz_success() throws Exception {
        mockCurrentUser("TEACHER");
        ClazzVO vo = new ClazzVO();
        vo.setId(1L);
        vo.setName("高一(1)班");
        when(teachingService.createClazz(any(Clazz.class), eq(10L))).thenReturn(vo);

        Clazz clazz = new Clazz();
        clazz.setName("高一(1)班");

        mockMvc.perform(post("/api/teaching/manage/clazz")
                        .principal(principal)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(clazz)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.name").value("高一(1)班"));
    }

    @Test
    void listClazzes_success() throws Exception {
        mockCurrentUser("TEACHER");
        ClazzVO vo = new ClazzVO();
        vo.setId(1L);
        when(teachingService.listClazzes(10L, "TEACHER")).thenReturn(List.of(vo));

        mockMvc.perform(get("/api/teaching/manage/clazz/list").principal(principal))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1));
    }

    @Test
    void addClassMember_success() throws Exception {
        when(teachingService.addClassMember(1L, 2L)).thenReturn(Result.success());

        mockMvc.perform(post("/api/teaching/manage/clazz/1/member")
                        .principal(principal)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"userId\":2}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    void createAssignment_success() throws Exception {
        mockCurrentUser("TEACHER");
        AssignmentVO vo = new AssignmentVO();
        vo.setId(1L);
        vo.setTitle("课堂任务");
        when(teachingService.createAssignment(any(AssignmentRequest.class), eq(10L))).thenReturn(vo);

        mockMvc.perform(post("/api/teaching/manage/assignment")
                        .principal(principal)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"课堂任务\",\"itemType\":\"KNOWLEDGE\",\"itemIds\":\"1,2\",\"targetType\":\"CLASS\",\"targetId\":1}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.title").value("课堂任务"));
    }

    @Test
    void listStudentTasks_success() throws Exception {
        User student = new User();
        student.setId(21L);
        student.setUsername("stu1");
        student.setRole("STUDENT");
        when(userRepository.findByUsername("stu1")).thenReturn(Optional.of(student));
        TaskVO task = new TaskVO();
        task.setId(1L);
        task.setTitle("我的任务");
        when(teachingService.listStudentTasks(21L)).thenReturn(List.of(task));

        mockMvc.perform(get("/api/teaching/task/list").principal(() -> "stu1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].title").value("我的任务"));
    }

    @Test
    void submitTask_success() throws Exception {
        User student = new User();
        student.setId(21L);
        student.setUsername("stu1");
        student.setRole("STUDENT");
        when(userRepository.findByUsername("stu1")).thenReturn(Optional.of(student));
        when(teachingService.submitTask(1L, 21L, "答案")).thenReturn(Result.success());

        mockMvc.perform(post("/api/teaching/task/submit")
                        .principal(() -> "stu1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"assignmentId\":1,\"answer\":\"答案\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }
}