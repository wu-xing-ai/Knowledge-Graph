package com.zhiplatform.security;

import com.zhiplatform.config.AuthModeProperties;
import com.zhiplatform.service.DemoLoginService;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Collections;

@Component
public class JwtFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final AuthModeProperties authMode;
    private final DemoLoginService demoLoginService;

    public JwtFilter(JwtUtil jwtUtil, AuthModeProperties authMode, DemoLoginService demoLoginService) {
        this.jwtUtil = jwtUtil;
        this.authMode = authMode;
        this.demoLoginService = demoLoginService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String header = request.getHeader("Authorization");
        if (StringUtils.hasText(header) && header.startsWith("Bearer ")) {
            String token = header.substring(7);
            if (jwtUtil.isTokenValid(token)) {
                String username = jwtUtil.getUsername(token);
                String role = jwtUtil.getRole(token);
                UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                        username, null,
                        Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + role))
                );
                SecurityContextHolder.getContext().setAuthentication(auth);
            }
        }
        // 演示模式兜底（AUTH_ENABLED=false）：携带有效 token 的请求保持原身份；
        // 未认证的请求（无 token 或 token 失效）自动注入演示账号，保证 Principal 可用
        if (SecurityContextHolder.getContext().getAuthentication() == null && !authMode.isAuthEnabled()) {
            demoLoginService.getDemoAuthentication()
                    .ifPresent(auth -> SecurityContextHolder.getContext().setAuthentication(auth));
        }
        filterChain.doFilter(request, response);
    }
}
