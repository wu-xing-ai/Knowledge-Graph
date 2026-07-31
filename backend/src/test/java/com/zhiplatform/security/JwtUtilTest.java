package com.zhiplatform.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;

class JwtUtilTest {

    private JwtUtil jwtUtil;

    @BeforeEach
    void setUp() {
        jwtUtil = new JwtUtil();
        ReflectionTestUtils.setField(jwtUtil, "secret", "test-secret-key-for-jwt-util-unit-test-123456");
        ReflectionTestUtils.setField(jwtUtil, "expiration", 3600000L);
        jwtUtil.init();
    }

    @Test
    void generateToken_andParseToken() {
        String token = jwtUtil.generateToken("admin", "TEACHER");

        assertNotNull(token);
        assertTrue(jwtUtil.isTokenValid(token));
        assertEquals("admin", jwtUtil.getUsername(token));
        assertEquals("TEACHER", jwtUtil.getRole(token));
    }

    @Test
    void isTokenValid_invalidTokenReturnsFalse() {
        assertFalse(jwtUtil.isTokenValid("invalid-token"));
    }

    @Test
    void isTokenValid_expiredTokenReturnsFalse() throws InterruptedException {
        JwtUtil shortLived = new JwtUtil();
        ReflectionTestUtils.setField(shortLived, "secret", "test-secret-key-for-jwt-util-unit-test-123456");
        ReflectionTestUtils.setField(shortLived, "expiration", 1L);
        shortLived.init();

        String token = shortLived.generateToken("user", "STUDENT");
        Thread.sleep(50);
        assertFalse(shortLived.isTokenValid(token));
    }
}
