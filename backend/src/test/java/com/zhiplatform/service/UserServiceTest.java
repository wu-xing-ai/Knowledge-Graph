package com.zhiplatform.service;

import com.zhiplatform.common.Result;
import com.zhiplatform.dto.LoginRequest;
import com.zhiplatform.dto.LoginResponse;
import com.zhiplatform.dto.RegisterRequest;
import com.zhiplatform.entity.mysql.User;
import com.zhiplatform.repository.mysql.ClazzRepository;
import com.zhiplatform.repository.mysql.UserRepository;
import com.zhiplatform.security.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private JwtUtil jwtUtil;
    @Mock
    private AuthenticationManager authenticationManager;
    @Mock
    private ClazzRepository clazzRepository;
    @Mock
    private TeachingService teachingService;

    private UserService userService;

    @BeforeEach
    void setUp() {
        userService = new UserService(userRepository, passwordEncoder, jwtUtil, authenticationManager, clazzRepository, teachingService);
    }

    @Test
    void login_success() {
        LoginRequest request = new LoginRequest();
        request.setUsername("admin");
        request.setPassword("123456");

        User user = new User();
        user.setId(1L);
        user.setUsername("admin");
        user.setRole("TEACHER");
        user.setRealName("管理员");

        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(user));
        when(jwtUtil.generateToken("admin", "TEACHER")).thenReturn("token123");

        LoginResponse response = userService.login(request);

        assertEquals("token123", response.getToken());
        assertEquals("TEACHER", response.getRole());
        assertEquals("admin", response.getUsername());
        assertEquals(1L, response.getUserId());
        verify(authenticationManager).authenticate(any());
    }

    @Test
    void login_userNotFound() {
        LoginRequest request = new LoginRequest();
        request.setUsername("nobody");
        request.setPassword("123456");

        when(userRepository.findByUsername("nobody")).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> userService.login(request));
    }

    @Test
    void register_success() {
        RegisterRequest request = new RegisterRequest();
        request.setUsername("student1");
        request.setPassword("password");
        request.setRole("STUDENT");
        request.setRealName("张三");

        when(userRepository.existsByUsername("student1")).thenReturn(false);
        when(passwordEncoder.encode("password")).thenReturn("encoded");

        Result<Void> result = userService.register(request);

        assertEquals(200, result.getCode());
        verify(userRepository).save(any(User.class));
    }

    @Test
    void register_duplicateUsername() {
        RegisterRequest request = new RegisterRequest();
        request.setUsername("student1");
        request.setPassword("password");
        request.setRole("STUDENT");

        when(userRepository.existsByUsername("student1")).thenReturn(true);

        Result<Void> result = userService.register(request);

        assertEquals(400, result.getCode());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void register_teacher_createsPendingAccount() {
        RegisterRequest request = new RegisterRequest();
        request.setUsername("teacher1");
        request.setPassword("password");
        request.setRole("TEACHER");

        when(userRepository.existsByUsername("teacher1")).thenReturn(false);
        when(passwordEncoder.encode("password")).thenReturn("encoded");

        Result<Void> result = userService.register(request);

        assertEquals(200, result.getCode());
        assertTrue(result.getMessage().contains("审核"));
        verify(userRepository).save(argThat(u ->
                ((User) u).getRole().equals("TEACHER") && ((User) u).getStatus() == 2));
    }

    @Test
    void getAllUsers_returnsList() {
        User u1 = new User();
        u1.setUsername("u1");
        User u2 = new User();
        u2.setUsername("u2");
        when(userRepository.findAll()).thenReturn(List.of(u1, u2));

        List<User> users = userService.getAllUsers();
        assertEquals(2, users.size());
    }

    @Test
    void updateUserStatus_success() {
        User user = new User();
        user.setId(5L);
        user.setStatus(1);
        when(userRepository.findById(5L)).thenReturn(Optional.of(user));

        userService.updateUserStatus(5L, 0);

        assertEquals(0, user.getStatus());
        verify(userRepository).save(user);
    }
}


