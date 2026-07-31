package com.zhiplatform.controller;

import com.zhiplatform.entity.mysql.OperationLog;
import com.zhiplatform.entity.mysql.User;
import com.zhiplatform.service.LogService;
import com.zhiplatform.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class AdminControllerTest {

    private MockMvc mockMvc;

    @Mock
    private LogService logService;
    @Mock
    private UserService userService;

    @BeforeEach
    void setUp() {
        AdminController controller = new AdminController(logService, userService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    void getAllLogs_success() throws Exception {
        when(logService.getAllLogs()).thenReturn(List.of(new OperationLog(), new OperationLog()));

        mockMvc.perform(get("/api/admin/logs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.length()").value(2));
    }

    @Test
    void getUserLogs_success() throws Exception {
        when(logService.getUserLogs(1L)).thenReturn(List.of(new OperationLog()));

        mockMvc.perform(get("/api/admin/logs/user/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    void getUsers_success() throws Exception {
        User user = new User();
        user.setId(1L);
        user.setUsername("admin");
        user.setPassword("secret");
        when(userService.getAllUsers()).thenReturn(List.of(user));

        mockMvc.perform(get("/api/admin/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data[0].password").isEmpty());
    }
}
