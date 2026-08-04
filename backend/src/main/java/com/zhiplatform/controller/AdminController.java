package com.zhiplatform.controller;

import com.zhiplatform.common.Result;
import com.zhiplatform.entity.mysql.OperationLog;
import com.zhiplatform.entity.mysql.User;
import com.zhiplatform.service.LogService;
import com.zhiplatform.service.UserService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

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

    // 用户列表（不返回密码哈希，密码仅可重置不可查看）
    @GetMapping("/users")
    public Result<List<User>> getUsers() {
        List<User> users = userService.getAllUsers();
        users.forEach(u -> u.setPassword(null));
        return Result.success(users);
    }

    // 管理员新增账号（教师/学生）
    @PostMapping("/user/add")
    public Result<User> addUser(@RequestBody Map<String, String> params) {
        return userService.addUser(
                params.get("username"),
                params.get("password"),
                params.get("realName"),
                params.get("role"));
    }

    // 管理员重置密码
    @PutMapping("/user/password")
    public Result<Void> resetPassword(@RequestBody Map<String, Object> params) {
        Long userId = Long.valueOf(params.get("userId").toString());
        String newPassword = params.get("password") == null ? "" : params.get("password").toString();
        return userService.resetPassword(userId, newPassword);
    }

    // 启用/禁用/审核通过账号（status: 1 正常 / 0 禁用；审核通过 = 设为 1）
    @PutMapping("/user/status")
    public Result<Void> updateStatus(@RequestBody Map<String, Object> params) {
        Long userId = Long.valueOf(params.get("userId").toString());
        Integer status = Integer.valueOf(params.get("status").toString());
        userService.updateUserStatus(userId, status);
        return Result.success();
    }

    // 修改用户身份（角色）
    @PutMapping("/user/role")
    public Result<Void> updateRole(@RequestBody Map<String, Object> params) {
        Long userId = Long.valueOf(params.get("userId").toString());
        String role = params.get("role") == null ? "" : params.get("role").toString();
        return userService.updateUserRole(userId, role);
    }

    // 删除账号（拒绝教师注册申请等）
    @DeleteMapping("/user/{id}")
    public Result<Void> deleteUser(@PathVariable Long id, java.security.Principal principal) {
        Long currentId = userService.getUserByUsername(principal.getName()).getId();
        if (id.equals(currentId)) {
            return Result.error(400, "不能删除自己的账号");
        }
        return userService.deleteUser(id);
    }
}


