package com.zhiplatform.service;

import com.zhiplatform.dto.LoginRequest;
import com.zhiplatform.dto.LoginResponse;
import com.zhiplatform.dto.RegisterRequest;
import com.zhiplatform.entity.mysql.User;
import com.zhiplatform.repository.mysql.UserRepository;
import com.zhiplatform.security.JwtUtil;
import com.zhiplatform.common.Result;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final AuthenticationManager authenticationManager;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder,
                       JwtUtil jwtUtil, AuthenticationManager authenticationManager) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
        this.authenticationManager = authenticationManager;
    }

    public LoginResponse login(LoginRequest request) {
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword()));
        } catch (org.springframework.security.authentication.DisabledException e) {
            // 待审核 / 禁用账号：单独抛出以便控制器给出明确提示
            throw e;
        }
        User user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new RuntimeException("用户不存在"));
        String token = jwtUtil.generateToken(user.getUsername(), user.getRole());
        return LoginResponse.builder()
                .token(token)
                .role(user.getRole())
                .username(user.getUsername())
                .realName(user.getRealName())
                .userId(user.getId())
                .build();
    }

    public Result<Void> register(RegisterRequest request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            return Result.error(400, "用户名已存在");
        }
        User user = new User();
        user.setUsername(request.getUsername());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setRealName(request.getRealName());
        user.setClassName(request.getClassName());
        user.setSchool(request.getSchool());
        boolean isTeacher = "TEACHER".equalsIgnoreCase(request.getRole());
        user.setRole(isTeacher ? "TEACHER" : "STUDENT");
        // 教师注册需管理员审核（status=2 待审核，未通过无法登录）；学生直接可用
        user.setStatus(isTeacher ? 2 : 1);
        userRepository.save(user);
        Result<Void> result = Result.success();
        result.setMessage(isTeacher ? "注册成功，教师账号需管理员审核通过后方可登录" : "注册成功，请登录");
        return result;
    }

    /**
     * 管理员新增账号（教师/学生）。
     */
    public Result<User> addUser(String username, String password, String realName, String role) {
        if (username == null || username.isBlank() || password == null || password.length() < 6) {
            return Result.error(400, "账号或密码不合法（密码至少 6 位）");
        }
        if (!"TEACHER".equalsIgnoreCase(role) && !"STUDENT".equalsIgnoreCase(role) && !"ADMIN".equalsIgnoreCase(role)) {
            return Result.error(400, "角色不合法");
        }
        if (userRepository.existsByUsername(username)) {
            return Result.error(400, "用户名已存在");
        }
        User user = new User();
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(password));
        user.setRole(role.toUpperCase());
        user.setRealName(realName);
        user.setStatus(1);
        userRepository.save(user);
        return Result.success(user);
    }

    /**
     * 管理员重置密码。
     */
    public Result<Void> resetPassword(Long userId, String newPassword) {
        if (newPassword == null || newPassword.length() < 6) {
            return Result.error(400, "新密码至少 6 位");
        }
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("用户不存在"));
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
        return Result.success();
    }
    /**
     * 管理员修改用户身份（角色）。
     */
    public Result<Void> updateUserRole(Long userId, String role) {
        if (role == null || (!"ADMIN".equalsIgnoreCase(role)
                && !"TEACHER".equalsIgnoreCase(role) && !"STUDENT".equalsIgnoreCase(role))) {
            return Result.error(400, "角色不合法");
        }
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("用户不存在"));
        if ("admin".equals(user.getUsername())) {
            return Result.error(400, "内置管理员账号不可修改身份");
        }
        user.setRole(role.toUpperCase());
        userRepository.save(user);
        return Result.success();
    }

    /**
     * 管理员删除账号（如拒绝教师注册申请）。
     */
    public Result<Void> deleteUser(Long id) {
        User user = userRepository.findById(id).orElse(null);
        if (user == null) return Result.success();
        if ("admin".equals(user.getUsername())) {
            return Result.error(400, "内置管理员账号不可删除");
        }
        userRepository.deleteById(id);
        return Result.success();
    }

    public User getUserByUsername(String username) {
        return userRepository.findByUsername(username).orElse(null);
    }

    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    public void updateUserStatus(Long userId, Integer status) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("用户不存在"));
        user.setStatus(status);
        userRepository.save(user);
    }
}



