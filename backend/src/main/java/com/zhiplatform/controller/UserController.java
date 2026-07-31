package com.zhiplatform.controller;

import com.zhiplatform.common.Result;
import com.zhiplatform.dto.LoginRequest;
import com.zhiplatform.dto.LoginResponse;
import com.zhiplatform.dto.RegisterRequest;
import com.zhiplatform.entity.mysql.User;
import com.zhiplatform.service.UserService;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.security.Principal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/user")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping("/login")
    public Result<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        try {
            LoginResponse response = userService.login(request);
            return Result.success(response);
        } catch (Exception e) {
            return Result.error(401, "用户名或密码错误");
        }
    }

    @PostMapping("/register")
    public Result<Void> register(@Valid @RequestBody RegisterRequest request) {
        return userService.register(request);
    }

    @GetMapping("/info")
    public Result<User> getUserInfo(Principal principal) {
        String username = principal.getName();
        User user = userService.getUserByUsername(username);
        if (user != null) {
            user.setPassword(null);
        }
        return Result.success(user);
    }

    @GetMapping("/list")
    public Result<List<User>> getAllUsers() {
        List<User> users = userService.getAllUsers();
        users.forEach(u -> u.setPassword(null));
        return Result.success(users);
    }

    @PutMapping("/status")
    public Result<Void> updateStatus(@RequestBody Map<String, Object> params) {
        Long userId = Long.valueOf(params.get("userId").toString());
        Integer status = Integer.valueOf(params.get("status").toString());
        userService.updateUserStatus(userId, status);
        return Result.success();
    }
}
