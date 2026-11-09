package com.zhiplatform.controller;

import com.zhiplatform.common.Result;
import com.zhiplatform.dto.LoginResponse;
import com.zhiplatform.service.DemoLoginService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * 应用运行状态接口（公开）。
 *
 * <p>前端启动时调用一次，用于判断是否处于演示模式：
 * 正常模式仅返回 {@code authRequired=true}；
 * 演示模式（AUTH_ENABLED=false）额外附带回演示账号的登录信息，
 * 前端据此自动进入系统、跳过登录页。</p>
 */
@RestController
@RequestMapping("/api/app")
public class AppStatusController {

    private final DemoLoginService demoLoginService;

    public AppStatusController(DemoLoginService demoLoginService) {
        this.demoLoginService = demoLoginService;
    }

    @GetMapping("/status")
    public Result<Map<String, Object>> status() {
        Map<String, Object> data = new HashMap<>();
        boolean authRequired = demoLoginService.isAuthRequired();
        data.put("authRequired", authRequired);
        if (!authRequired) {
            demoLoginService.buildDemoLogin().ifPresent(login -> {
                data.put("token", login.getToken());
                data.put("role", login.getRole());
                data.put("username", login.getUsername());
                data.put("realName", login.getRealName());
                data.put("userId", login.getUserId());
            });
        }
        return Result.success(data);
    }
}
