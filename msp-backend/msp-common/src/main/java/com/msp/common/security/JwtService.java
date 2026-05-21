package com.msp.common.security;

import com.msp.common.core.User;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * JWT认证服务
 * 提供token生成、验证、解析功能
 */
public class JwtService {

    private static final Logger log = LoggerFactory.getLogger(JwtService.class);

    private final SecretKey secretKey;
    private final long expirationMs;

    public JwtService(String secret, long expirationMs) {
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expirationMs = expirationMs;
    }

    /**
     * 生成JWT token
     */
    public String generateToken(User user) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + expirationMs);

        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", user.getUserId());
        claims.put("username", user.getUsername());
        claims.put("role", user.getRole() != null ? user.getRole().name() : "USER");

        return Jwts.builder()
            .claims(claims)
            .subject(user.getUserId())
            .issuedAt(now)
            .expiration(expiry)
            .signWith(secretKey)
            .compact();
    }

    /**
     * 验证token
     */
    public boolean validateToken(String token) {
        try {
            Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token);
            return true;
        } catch (JwtException e) {
            log.warn("Invalid JWT token: {}", e.getMessage());
            return false;
        }
    }

    /**
     * 解析token获取用户ID
     */
    public Optional<String> getUserIdFromToken(String token) {
        try {
            Claims claims = Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
            return Optional.ofNullable(claims.getSubject());
        } catch (JwtException e) {
            log.warn("Failed to parse JWT token: {}", e.getMessage());
            return Optional.empty();
        }
    }

    /**
     * 解析token获取用户名
     */
    public Optional<String> getUsernameFromToken(String token) {
        try {
            Claims claims = Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
            return Optional.ofNullable(claims.get("username", String.class));
        } catch (JwtException e) {
            log.warn("Failed to parse JWT token: {}", e.getMessage());
            return Optional.empty();
        }
    }

    /**
     * 解析token获取角色
     */
    public Optional<String> getRoleFromToken(String token) {
        try {
            Claims claims = Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
            return Optional.ofNullable(claims.get("role", String.class));
        } catch (JwtException e) {
            log.warn("Failed to parse JWT token: {}", e.getMessage());
            return Optional.empty();
        }
    }

    /**
     * 解析完整的claims
     */
    public Optional<Claims> parseToken(String token) {
        try {
            Claims claims = Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
            return Optional.of(claims);
        } catch (JwtException e) {
            log.warn("Failed to parse JWT token: {}", e.getMessage());
            return Optional.empty();
        }
    }
}