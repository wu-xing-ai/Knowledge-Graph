package com.zhiplatform.service;

import com.zhiplatform.config.AuthModeProperties;
import com.zhiplatform.entity.mysql.User;
import com.zhiplatform.repository.mysql.UserRepository;
import com.zhiplatform.security.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DemoLoginServiceTest {

    private AuthModeProperties authMode;
    private UserRepository userRepository;
    private DemoLoginService service;

    private User admin;

    @BeforeEach
    void setUp() {
        authMode = new AuthModeProperties();
        ReflectionTestUtils.setField(authMode, "authEnabled", false);
        ReflectionTestUtils.setField(authMode, "demoUsername", "admin");
        userRepository = mock(UserRepository.class);

        JwtUtil jwtUtil = new JwtUtil();
        ReflectionTestUtils.setField(jwtUtil, "secret", "test-secret-key-for-jwt-util-unit-test-123456");
        ReflectionTestUtils.setField(jwtUtil, "expiration", 3600000L);
        jwtUtil.init();

        service = new DemoLoginService(authMode, userRepository, jwtUtil);

        admin = new User();
        admin.setId(1L);
        admin.setUsername("admin");
        admin.setRole("ADMIN");
        admin.setStatus(1);
    }

    @Test
    void authRequired_true_whenAuthEnabled() {
        ReflectionTestUtils.setField(authMode, "authEnabled", true);
        assertTrue(service.isAuthRequired());
        // 正常鉴权模式下不解析演示账号
        assertTrue(service.resolveDemoUser().isEmpty());
        assertTrue(service.getDemoAuthentication().isEmpty());
    }

    @Test
    void resolveDemoUser_returnsPreferredDemoAccount() {
        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(admin));

        assertEquals("admin", service.resolveDemoUser().orElseThrow().getUsername());

        Optional<?> login = service.buildDemoLogin();
        assertTrue(login.isPresent());
        assertNotNull(((com.zhiplatform.dto.LoginResponse) login.get()).getToken());
        assertEquals("ADMIN", ((com.zhiplatform.dto.LoginResponse) login.get()).getRole());
    }

    @Test
    void resolveDemoUser_fallsBackToFirstActiveAdmin() {
        when(userRepository.findByUsername("admin")).thenReturn(Optional.empty());
        User firstAdmin = new User();
        firstAdmin.setId(2L);
        firstAdmin.setUsername("root");
        firstAdmin.setRole("ADMIN");
        firstAdmin.setStatus(1);
        when(userRepository.findByRoleOrderByIdAsc("ADMIN")).thenReturn(List.of(firstAdmin));

        assertEquals("root", service.resolveDemoUser().orElseThrow().getUsername());
    }

    @Test
    void resolveDemoUser_skipsDisabledDemoAccount() {
        admin.setStatus(0);
        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(admin));
        when(userRepository.findByRoleOrderByIdAsc("ADMIN")).thenReturn(List.of());

        assertTrue(service.resolveDemoUser().isEmpty());
        assertTrue(service.getDemoAuthentication().isEmpty());
    }
}
