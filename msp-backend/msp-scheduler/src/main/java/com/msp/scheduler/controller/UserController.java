package com.msp.scheduler.controller;

import com.msp.common.core.ApiResponse;
import com.msp.common.core.Page;
import com.msp.common.core.User;
import com.msp.scheduler.service.UserService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

/**
 * 用户管理REST接口
 */
@RestController
@RequestMapping("/api/v1/msp/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping
    public ApiResponse<String> create(@RequestBody UserCreateRequest request) {
        User user = new User();
        user.setUsername(request.getUsername());
        user.setEmail(request.getEmail());
        user.setPhone(request.getPhone());
        user.setRole(request.getRole());

        String userId = userService.createUser(user, request.getPassword());
        return ApiResponse.success(userId);
    }

    @PutMapping("/{userId}")
    public ApiResponse<Boolean> update(@PathVariable(name = "userId") String userId, @RequestBody UserUpdateRequest request) {
        Optional<User> existingOpt = userService.getUser(userId);
        if (existingOpt.isEmpty()) {
            return ApiResponse.error("USER_NOT_FOUND", "用户不存在");
        }
        User existing = existingOpt.get();
        if (request.getEmail() != null) {
            existing.setEmail(request.getEmail());
        }
        if (request.getPhone() != null) {
            existing.setPhone(request.getPhone());
        }
        if (request.getRole() != null) {
            existing.setRole(request.getRole());
        }
        if (request.getEnabled() != null) {
            existing.setEnabled(request.getEnabled());
        }
        existing.setUpdateTime(System.currentTimeMillis());
        userService.updateUserDirectly(existing);
        return ApiResponse.success(true);
    }

    @GetMapping("/{userId}")
    public ApiResponse<User> getById(@PathVariable(name = "userId") String userId) {
        return userService.getUser(userId)
            .map(ApiResponse::success)
            .orElse(ApiResponse.error("USER_NOT_FOUND", "用户不存在"));
    }

    @GetMapping
    public ApiResponse<Page<User>> list(
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "10") int size) {
        List<User> content = userService.listUsers(page, size);
        long total = userService.countUsers();
        return ApiResponse.success(new Page<>(content, total, page, size));
    }

    @DeleteMapping("/{userId}")
    public ApiResponse<Boolean> delete(@PathVariable(name = "userId") String userId) {
        userService.deleteUser(userId);
        return ApiResponse.success(true);
    }

    @PutMapping("/{userId}/password")
    public ApiResponse<Boolean> resetPassword(@PathVariable(name = "userId") String userId, @RequestBody ResetPasswordRequest request) {
        if (request.getPassword() == null || request.getPassword().isEmpty()) {
            return ApiResponse.error("INVALID_REQUEST", "密码不能为空");
        }
        userService.resetPassword(userId, request.getPassword());
        return ApiResponse.success(true);
    }

    @PutMapping("/{userId}/status")
    public ApiResponse<Boolean> setEnabled(@PathVariable(name = "userId") String userId, @RequestBody SetEnabledRequest request) {
        if (request.getEnabled() == null) {
            return ApiResponse.error("INVALID_REQUEST", "enabled不能为空");
        }
        userService.setUserEnabled(userId, request.getEnabled());
        return ApiResponse.success(true);
    }

    @PutMapping("/{userId}/roles")
    public ApiResponse<Boolean> assignRoles(@PathVariable(name = "userId") String userId, @RequestBody List<String> roleIds) {
        userService.assignRoles(userId, roleIds);
        return ApiResponse.success(true);
    }

    @GetMapping("/{userId}/roles")
    public ApiResponse<List<String>> getRoles(@PathVariable(name = "userId") String userId) {
        List<String> roles = userService.getUserRoles(userId);
        return ApiResponse.success(roles);
    }

    public static class UserCreateRequest {
        private String username;
        private String password;
        private String email;
        private String phone;
        private User.UserRole role;

        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }

        public String getPassword() { return password; }
        public void setPassword(String password) { this.password = password; }

        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }

        public String getPhone() { return phone; }
        public void setPhone(String phone) { this.phone = phone; }

        public User.UserRole getRole() { return role; }
        public void setRole(User.UserRole role) { this.role = role; }
    }

    public static class UserUpdateRequest {
        private String email;
        private String phone;
        private User.UserRole role;
        private Boolean enabled;

        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }

        public String getPhone() { return phone; }
        public void setPhone(String phone) { this.phone = phone; }

        public User.UserRole getRole() { return role; }
        public void setRole(User.UserRole role) { this.role = role; }

        public Boolean getEnabled() { return enabled; }
        public void setEnabled(Boolean enabled) { this.enabled = enabled; }
    }

    public static class ResetPasswordRequest {
        private String password;

        public String getPassword() { return password; }
        public void setPassword(String password) { this.password = password; }
    }

    public static class SetEnabledRequest {
        private Boolean enabled;

        public Boolean getEnabled() { return enabled; }
        public void setEnabled(Boolean enabled) { this.enabled = enabled; }
    }
}