package com.zhiplatform.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zhiplatform.dto.LoginRequest;
import com.zhiplatform.dto.LoginResponse;
import com.zhiplatform.dto.RegisterRequest;
import com.zhiplatform.entity.mysql.User;
import com.zhiplatform.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class UserControllerTest {

    private MockMvc mockMvc;
    private ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());

    @Mock
    private UserService userService;

    @BeforeEach
    void setUp() {
        UserController controller = new UserController(userService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    void login_success() throws Exception {
        LoginResponse response = LoginResponse.builder()
                .token("token")
                .role("TEACHER")
                .username("admin")
                .userId(1L)
                .build();

        when(userService.login(any(LoginRequest.class))).thenReturn(response);

        LoginRequest request = new LoginRequest();
        request.setUsername("admin");
        request.setPassword("123456");

        mockMvc.perform(post("/api/user/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.token").value("token"));
    }

    @Test
    void login_failure() throws Exception {
        when(userService.login(any(LoginRequest.class))).thenThrow(new RuntimeException("bad"));

        LoginRequest request = new LoginRequest();
        request.setUsername("admin");
        request.setPassword("wrong");

        mockMvc.perform(post("/api/user/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(401));
    }

    @Test
    void register_success() throws Exception {
        when(userService.register(any(RegisterRequest.class))).thenReturn(com.zhiplatform.common.Result.success());

        RegisterRequest request = new RegisterRequest();
        request.setUsername("newuser");
        request.setPassword("password");
        request.setRole("STUDENT");

        mockMvc.perform(post("/api/user/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    void getUserInfo_returnsUser() throws Exception {
        User user = new User();
        user.setId(1L);
        user.setUsername("admin");
        user.setPassword("secret");

        when(userService.getUserByUsername("admin")).thenReturn(user);

        mockMvc.perform(get("/api/user/info")
                        .principal(() -> "admin"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.username").value("admin"))
                .andExpect(jsonPath("$.data.password").isEmpty());
    }

    @Test
    void updateStatus_success() throws Exception {
        Map<String, Object> body = Map.of("userId", 1L, "status", 0);

        mockMvc.perform(put("/api/user/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        verify(userService).updateUserStatus(1L, 0);
    }
}
