package com.zhiplatform.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * 登录鉴权开关配置。
 *
 * <p>AUTH_ENABLED=false 时进入“演示模式”：接口不再强制登录，
 * 由 {@code JwtFilter} 为未携带有效 token 的请求自动注入
 * {@code DEMO_USERNAME}（默认 admin）对应的演示账号身份。</p>
 *
 * <p>两个变量均可通过环境变量覆盖，application.yml 中的值仅是默认值。</p>
 */
@Component
public class AuthModeProperties {

    /** 是否要求登录鉴权：true=正常登录（默认），false=演示模式（免登录浏览） */
    @Value("${app.auth.enabled:true}")
    private boolean authEnabled;

    /** 演示模式自动使用的账号（须为 sys_user 中已存在且启用的用户名） */
    @Value("${app.auth.demo-username:admin}")
    private String demoUsername;

    public boolean isAuthEnabled() {
        return authEnabled;
    }

    public void setAuthEnabled(boolean authEnabled) {
        this.authEnabled = authEnabled;
    }

    public String getDemoUsername() {
        return demoUsername;
    }

    public void setDemoUsername(String demoUsername) {
        this.demoUsername = demoUsername;
    }
}
