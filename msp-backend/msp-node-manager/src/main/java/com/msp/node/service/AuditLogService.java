package com.msp.node.service;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.UUID;

/**
 * 审计日志服务（node-manager模块独立实现）
 */
@Service
public class AuditLogService {

    private final JdbcTemplate jdbcTemplate;

    public AuditLogService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * 记录审计日志
     */
    public void log(String userId, String action, String resourceType, String resourceId, Object details, String ipAddress) {
        String detailsJson = details instanceof Map ? mapToJson((Map<?, ?>) details) : null;

        String sql = "INSERT INTO msp_audit_logs (log_id, user_id, action, resource_type, resource_id, details, ip_address, create_time) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

        jdbcTemplate.update(sql,
            UUID.randomUUID().toString(),
            userId,
            action,
            resourceType,
            resourceId,
            detailsJson,
            ipAddress,
            System.currentTimeMillis()
        );
    }

    private String mapToJson(Map<?, ?> map) {
        StringBuilder sb = new StringBuilder("{");
        boolean first = true;
        for (Map.Entry<?, ?> entry : map.entrySet()) {
            if (!first) sb.append(",");
            sb.append("\"").append(entry.getKey()).append("\":");
            Object value = entry.getValue();
            if (value instanceof String) {
                sb.append("\"").append(value).append("\"");
            } else if (value instanceof Number) {
                sb.append(value);
            } else if (value instanceof Boolean) {
                sb.append(value);
            } else {
                sb.append("\"").append(value).append("\"");
            }
            first = false;
        }
        sb.append("}");
        return sb.toString();
    }
}