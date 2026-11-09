package com.zhiplatform.service;

import com.zhiplatform.config.AuthModeProperties;
import com.zhiplatform.dto.LoginResponse;
import com.zhiplatform.entity.mysql.User;
import com.zhiplatform.repository.mysql.UserRepository;
import com.zhiplatform.security.JwtUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.Optional;

/**
 * 演示模式（AUTH_ENABLED=false）下的演示账号解析与登录。
 *
 * <p>演示账号优先取 {@code DEMO_USERNAME}（默认 admin）；若该账号不存在或未启用，
 * 则回退到系统中 id 最小的启用状态管理员，保证演示模式始终有可用身份。
 * 找不到任何可用账号时不会注入身份，并只告警一次。</p>
 */
@Service
public class DemoLoginService {

    private static final Logger log = LoggerFactory.getLogger(DemoLoginService.class);

    private final AuthModeProperties authMode;
    private final UserRepository userRepository;
    private final JwtUtil jwtUtil;

    private volatile boolean warnedMissingDemoUser = false;

    public DemoLoginService(AuthModeProperties authMode, UserRepository userRepository, JwtUtil jwtUtil) {
        this.authMode = authMode;
        this.userRepository = userRepository;
        this.jwtUtil = jwtUtil;
    }

    /** 是否仍要求正常登录鉴权（演示模式为 false） */
    public boolean isAuthRequired() {
        return authMode.isAuthEnabled();
    }

    /** 解析当前演示账号；未开启演示模式或找不到可用账号时返回 empty */
    public Optional<User> resolveDemoUser() {
        if (authMode.isAuthEnabled()) {
            return Optional.empty();
        }
        Optional<User> preferred = userRepository.findByUsername(authMode.getDemoUsername())
                .filter(this::isActive);
        if (preferred.isPresent()) {
            return preferred;
        }
        Optional<User> fallback = userRepository.findByRoleOrderByIdAsc("ADMIN").stream()
                .filter(this::isActive)
                .findFirst();
        if (fallback.isPresent()) {
            if (!warnedMissingDemoUser) {
                log.warn("演示账号 '{}' 不存在或未启用，已回退使用管理员 '{}' 进行演示",
                        authMode.getDemoUsername(), fallback.get().getUsername());
                warnedMissingDemoUser = true;
            }
            return fallback;
        }
        if (!warnedMissingDemoUser) {
            log.warn("演示模式已开启（AUTH_ENABLED=false），但未找到可用演示账号 '{}'，也没有可回退的管理员账号",
                    authMode.getDemoUsername());
            warnedMissingDemoUser = true;
        }
        return Optional.empty();
    }

    /** 生成演示模式的 Spring Security 认证对象（供 JwtFilter 注入请求上下文） */
    public Optional<UsernamePasswordAuthenticationToken> getDemoAuthentication() {
        return resolveDemoUser().map(user -> new UsernamePasswordAuthenticationToken(
                user.getUsername(), null,
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + user.getRole()))));
    }

    /** 演示模式登录：直接为演示账号签发 JWT（与正常登录的响应结构一致） */
    public Optional<LoginResponse> buildDemoLogin() {
        return resolveDemoUser().map(user -> LoginResponse.builder()
                .token(jwtUtil.generateToken(user.getUsername(), user.getRole()))
                .role(user.getRole())
                .username(user.getUsername())
                .realName(user.getRealName())
                .userId(user.getId())
                .build());
    }

    /** 仅启用（status=1）的账号可作为演示身份 */
    private boolean isActive(User user) {
        return user.getStatus() != null && user.getStatus() == 1;
    }
}
