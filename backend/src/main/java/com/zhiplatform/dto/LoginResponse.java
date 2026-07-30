package com.zhiplatform.dto;

public class LoginResponse {

    private String token;
    private String role;
    private String username;
    private String realName;
    private Long userId;

    public LoginResponse() {}

    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }
    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getRealName() { return realName; }
    public void setRealName(String realName) { this.realName = realName; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private final LoginResponse r = new LoginResponse();
        public Builder token(String v) { r.token = v; return this; }
        public Builder role(String v) { r.role = v; return this; }
        public Builder username(String v) { r.username = v; return this; }
        public Builder realName(String v) { r.realName = v; return this; }
        public Builder userId(Long v) { r.userId = v; return this; }
        public LoginResponse build() { return r; }
    }
}
