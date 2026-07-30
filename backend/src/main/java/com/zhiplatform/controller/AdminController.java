package com.zhiplatform.controller;

import com.zhiplatform.common.Result;
import com.zhiplatform.entity.mysql.OperationLog;
import com.zhiplatform.entity.mysql.User;
import com.zhiplatform.service.LogService;
import com.zhiplatform.service.UserService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final LogService logService;
    private final UserService userService;

    public AdminController(LogService logService, UserService userService) {
        this.logService = logService;
        this.userService = userService;
    }

    @GetMapping("/logs")
    public Result<List<OperationLog>> getAllLogs() {
        return Result.success(logService.getAllLogs());
    }

    @GetMapping("/logs/user/{userId}")
    public Result<List<OperationLog>> getUserLogs(@PathVariable Long userId) {
        return Result.success(logService.getUserLogs(userId));
    }

    @GetMapping("/users")
    public Result<List<User>> getUsers() {
        List<User> users = userService.getAllUsers();
        users.forEach(u -> u.setPassword(null));
        return Result.success(users);
    }
}
