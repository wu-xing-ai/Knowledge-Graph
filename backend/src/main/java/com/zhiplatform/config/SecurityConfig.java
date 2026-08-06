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

    public SecurityConfig(JwtFilter jwtFilter) {
        this.jwtFilter = jwtFilter;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http.csrf(csrf -> csrf.disable())
            .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/user/login", "/api/user/register", "/error").permitAll()
                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                // 用户管理：仅管理员
                .requestMatchers("/api/user/list").hasRole("ADMIN")
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
            )
            .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
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


