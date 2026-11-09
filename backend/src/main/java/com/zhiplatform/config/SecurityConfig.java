package com.zhiplatform.config;

import com.zhiplatform.security.JwtFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private final JwtFilter jwtFilter;
    private final AuthModeProperties authMode;

    public SecurityConfig(JwtFilter jwtFilter, AuthModeProperties authMode) {
        this.jwtFilter = jwtFilter;
        this.authMode = authMode;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http.csrf(csrf -> csrf.disable())
            .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS));
        if (authMode.isAuthEnabled()) {
            configureProtectedRoutes(http);
        } else {
            // 演示模式（AUTH_ENABLED=false）：接口全部放行，
            // 请求身份由 JwtFilter 注入演示账号，具体身份仍可用（Principal 非空）
            http.authorizeHttpRequests(auth -> auth
                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                .anyRequest().permitAll()
            );
        }
        http.addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    /** 正常鉴权模式下的路由规则 */
    private void configureProtectedRoutes(HttpSecurity http) throws Exception {
        http.authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/user/login", "/api/user/register", "/error").permitAll()
                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                // 用户管理：仅管理员
                .requestMatchers("/api/user/list").hasRole("ADMIN")
                .requestMatchers(HttpMethod.POST, "/api/user/batch-import").hasAnyRole("TEACHER", "ADMIN")
                .requestMatchers(HttpMethod.PUT, "/api/user/status").hasRole("ADMIN")
                // 教师功能：教师或管理员
                .requestMatchers(HttpMethod.POST, "/api/knowledge/**").hasAnyRole("TEACHER", "ADMIN")
                .requestMatchers(HttpMethod.PUT, "/api/knowledge/**").hasAnyRole("TEACHER", "ADMIN")
                .requestMatchers(HttpMethod.DELETE, "/api/knowledge/**").hasAnyRole("TEACHER", "ADMIN")
                .requestMatchers("/api/graph/config/**").hasAnyRole("TEACHER", "ADMIN")
                .requestMatchers(HttpMethod.POST, "/api/graph/relation/**").hasAnyRole("TEACHER", "ADMIN")
                .requestMatchers(HttpMethod.DELETE, "/api/graph/relation/**").hasAnyRole("TEACHER", "ADMIN")
                .requestMatchers(HttpMethod.GET, "/api/graph/data").authenticated()
                .requestMatchers(HttpMethod.GET, "/api/graph/expand").authenticated()
                // 学习记录与笔记：登录用户可维护（学生可保存问答记录/笔记）
                .requestMatchers(HttpMethod.POST, "/api/exercise/record/**").authenticated()
                .requestMatchers(HttpMethod.POST, "/api/exercise/note/**").authenticated()
                .requestMatchers(HttpMethod.PUT, "/api/exercise/note/**").authenticated()
                .requestMatchers(HttpMethod.DELETE, "/api/exercise/note/**").authenticated()
                // 收藏/刷题提交/错题：学生也可读写，需登录
                .requestMatchers(HttpMethod.POST, "/api/exercise/favorite/**").authenticated()
                .requestMatchers(HttpMethod.DELETE, "/api/exercise/favorite/**").authenticated()
                .requestMatchers(HttpMethod.POST, "/api/exercise/practice/submit").authenticated()
                .requestMatchers(HttpMethod.DELETE, "/api/exercise/mistake/**").authenticated()
                // 作答统计：仅教师或管理员
                .requestMatchers(HttpMethod.GET, "/api/exercise/stats").hasAnyRole("TEACHER", "ADMIN")
// 习题管理：教师或管理员
                .requestMatchers(HttpMethod.POST, "/api/exercise/**").hasAnyRole("TEACHER", "ADMIN")
                .requestMatchers(HttpMethod.PUT, "/api/exercise/**").hasAnyRole("TEACHER", "ADMIN")
                .requestMatchers(HttpMethod.DELETE, "/api/exercise/**").hasAnyRole("TEACHER", "ADMIN")
                // 教学组织（班级/小组/布置任务/批改）：教师或管理员；学生任务查看与提交：登录即可
                .requestMatchers("/api/teaching/manage/**").hasAnyRole("TEACHER", "ADMIN")
                .requestMatchers("/api/teaching/task/**").authenticated()
                // 系统管理：仅管理员
                .requestMatchers("/api/admin/**").hasRole("ADMIN")
                .anyRequest().authenticated()
            );
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }
}


