package com.msp.scheduler.controller;

import com.msp.common.core.ApiResponse;
import com.msp.common.core.User;
import com.msp.scheduler.service.AuditLogService;
import com.msp.scheduler.service.UserService;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 认证REST接口
 */
@RestController
@RequestMapping("/api/v1/msp/auth")
public class AuthController {

    private final UserService userService;
    private final AuditLogService auditLogService;

    public AuthController(UserService userService, AuditLogService auditLogService) {
        this.userService = userService;
        this.auditLogService = auditLogService;
    }

    /**
     * 用户登录
     */
    @PostMapping("/login")
    public ApiResponse<LoginResponse> login(@RequestBody LoginRequest request, @RequestHeader(value = "X-Forwarded-For", required = false) String ip) {
        UserService.LoginResult result = userService.login(request.getUsername(), request.getPassword());

        if (!result.isSuccess()) {
            return ApiResponse.error("AUTH_FAILED", result.getMessage());
        }

        User user = (User) result.getData();
        Map<String, Object> data = new HashMap<>();
        data.put("token", result.getToken());
        data.put("user", Map.of(
            "userId", user.getUserId(),
            "username", user.getUsername(),
            "role", user.getRole() != null ? user.getRole().name() : "USER"
        ));

        // 记录登录审计日志
        auditLogService.log(user.getUserId(), "LOGIN", "AUTH", user.getUserId(),
            Map.of("username", request.getUsername(), "success", true), ip);

        return ApiResponse.success(new LoginResponse(result.getToken(), user.getUserId(), user.getUsername()));
    }

    /**
     * 用户登出
     */
    @PostMapping("/logout")
    public ApiResponse<Boolean> logout(@RequestHeader(value = "Authorization", required = false) String token,
                                        @RequestHeader(value = "X-Forwarded-For", required = false) String ip) {
        String userId = null;
        if (token != null && token.startsWith("Bearer ")) {
            token = token.substring(7);
        }
        // 获取当前用户ID用于审计
        userId = userService.getCurrentUser(token).map(User::getUserId).orElse(null);
        userService.logout(token);

        // 记录登出审计日志
        if (userId != null) {
            auditLogService.log(userId, "LOGOUT", "AUTH", userId,
                Map.of("success", true), ip);
        }

        return ApiResponse.success(true);
    }

    /**
     * 获取当前用户
     */
    @GetMapping("/me")
    public ApiResponse<Map<String, Object>> getCurrentUser(@RequestHeader(value = "Authorization", required = false) String token) {
        if (token != null && token.startsWith("Bearer ")) {
            token = token.substring(7);
        }

        return userService.getCurrentUser(token)
            .map(user -> {
                Map<String, Object> data = Map.of(
                    "userId", user.getUserId(),
                    "username", user.getUsername(),
                    "role", user.getRole() != null ? user.getRole().name() : "USER"
                );
                return ApiResponse.success(data);
            })
            .orElse(ApiResponse.error("UNAUTHORIZED", "未登录或登录已过期"));
    }

    // 内部类
    public static class LoginRequest {
        private String username;
        private String password;

        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }
        public String getPassword() { return password; }
        public void setPassword(String password) { this.password = password; }
    }

    public static class LoginResponse {
        private String token;
        private String userId;
        private String username;

        public LoginResponse(String token, String userId, String username) {
            this.token = token;
            this.userId = userId;
            this.username = username;
        }

        public String getToken() { return token; }
        public String getUserId() { return userId; }
        public String getUsername() { return username; }
    }
}