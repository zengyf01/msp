package com.msp.scheduler.service;

import com.msp.common.core.User;
import com.msp.common.security.JwtService;
import com.msp.scheduler.repository.RoleRepository;
import com.msp.scheduler.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;

/**
 * 用户服务
 */
@Service
public class UserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final JwtService jwtService;

    public UserService(UserRepository userRepository,
                      RoleRepository roleRepository,
                      @Value("${jwt.secret}") String jwtSecret,
                      @Value("${jwt.expiration:86400000}") long jwtExpiration) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.jwtService = new JwtService(jwtSecret, jwtExpiration);
    }

    /**
     * 用户登录
     */
    public LoginResult login(String username, String password) {
        Optional<User> userOpt = userRepository.findByUsername(username);
        if (userOpt.isEmpty()) {
            return new LoginResult(false, null, "用户名或密码错误");
        }

        User user = userOpt.get();
        if (!user.isEnabled()) {
            return new LoginResult(false, null, "用户已被禁用");
        }

        if (!verifyPassword(password, user.getPassword())) {
            return new LoginResult(false, null, "用户名或密码错误");
        }

        // 生成JWT token（包含用户角色信息）
        String token = jwtService.generateToken(user);
        return new LoginResult(true, token, user);
    }

    /**
     * 用户登出
     */
    public void logout(String token) {
        // JWT无状态，登出时只需要客户端丢弃token
    }

    /**
     * 获取当前用户
     */
    public Optional<User> getCurrentUser(String token) {
        if (token == null || token.isEmpty()) {
            return Optional.empty();
        }

        if (!jwtService.validateToken(token)) {
            return Optional.empty();
        }

        return jwtService.getUserIdFromToken(token)
            .flatMap(userRepository::findById);
    }

    /**
     * 创建用户
     */
    public String createUser(User user, String password) {
        if (userRepository.findByUsername(user.getUsername()).isPresent()) {
            throw new RuntimeException("用户名已存在");
        }

        String userId = UUID.randomUUID().toString();
        long now = System.currentTimeMillis();

        user.setUserId(userId);
        user.setPassword(hashPassword(password));
        user.setEnabled(true);
        user.setCreateTime(now);
        user.setUpdateTime(now);

        userRepository.save(user);
        return userId;
    }

    /**
     * 更新用户
     */
    public void updateUser(User user) {
        Optional<User> existingOpt = userRepository.findById(user.getUserId());
        if (existingOpt.isEmpty()) {
            throw new RuntimeException("用户不存在");
        }
        User existing = existingOpt.get();
        existing.setEmail(user.getEmail());
        existing.setPhone(user.getPhone());
        existing.setRole(user.getRole());
        if (user.isEnabled() != existing.isEnabled()) {
            existing.setEnabled(user.isEnabled());
        }
        existing.setUpdateTime(System.currentTimeMillis());
        userRepository.update(existing);
    }

    /**
     * 直接更新用户（不查询现有数据）
     */
    public void updateUserDirectly(User user) {
        user.setUpdateTime(System.currentTimeMillis());
        userRepository.update(user);
    }

    /**
     * 获取用户
     */
    public Optional<User> getUser(String userId) {
        return userRepository.findById(userId);
    }

    /**
     * 用户列表
     */
    public List<User> listUsers(int page, int size) {
        return userRepository.findAll(page, size);
    }

    /**
     * 用户数量
     */
    public long countUsers() {
        return userRepository.count();
    }

    /**
     * 删除用户
     */
    public void deleteUser(String userId) {
        userRepository.delete(userId);
    }

    /**
     * 重置密码
     */
    public void resetPassword(String userId, String newPassword) {
        Optional<User> userOpt = userRepository.findById(userId);
        if (userOpt.isEmpty()) {
            throw new RuntimeException("用户不存在");
        }

        User user = userOpt.get();
        user.setPassword(hashPassword(newPassword));
        user.setUpdateTime(System.currentTimeMillis());
        userRepository.update(user);
    }

    /**
     * 启用/禁用用户
     */
    public void setUserEnabled(String userId, boolean enabled) {
        Optional<User> userOpt = userRepository.findById(userId);
        if (userOpt.isEmpty()) {
            throw new RuntimeException("用户不存在");
        }

        User user = userOpt.get();
        user.setEnabled(enabled);
        user.setUpdateTime(System.currentTimeMillis());
        userRepository.update(user);
    }

    /**
     * 分配角色给用户
     */
    public void assignRoles(String userId, List<String> roleIds) {
        // 先删除用户现有角色
        roleRepository.findById(userId); // Check user exists
        for (String roleId : roleIds) {
            roleRepository.saveUserRole(userId, roleId);
        }
    }

    /**
     * 获取用户角色列表
     */
    public List<String> getUserRoles(String userId) {
        return roleRepository.findRoleCodesByUserId(userId);
    }

    /**
     * 修改密码
     */
    public void changePassword(String userId, String oldPassword, String newPassword) {
        Optional<User> userOpt = userRepository.findById(userId);
        if (userOpt.isEmpty()) {
            throw new RuntimeException("用户不存在");
        }

        User user = userOpt.get();
        if (!verifyPassword(oldPassword, user.getPassword())) {
            throw new RuntimeException("原密码错误");
        }

        user.setPassword(hashPassword(newPassword));
        user.setUpdateTime(System.currentTimeMillis());
        userRepository.update(user);
    }

    private String hashPassword(String password) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(password.getBytes());
            return Base64.getEncoder().encodeToString(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("密码加密失败", e);
        }
    }

    private boolean verifyPassword(String password, String hashedPassword) {
        return hashPassword(password).equals(hashedPassword);
    }

    public static class LoginResult {
        private final boolean success;
        private final String token;
        private final Object data;
        private final String message;

        public LoginResult(boolean success, String token, Object data) {
            this(success, token, data, success ? "登录成功" : "登录失败");
        }

        public LoginResult(boolean success, String token, Object data, String message) {
            this.success = success;
            this.token = token;
            this.data = data;
            this.message = message;
        }

        public boolean isSuccess() { return success; }
        public String getToken() { return token; }
        public Object getData() { return data; }
        public String getMessage() { return message; }
    }
}