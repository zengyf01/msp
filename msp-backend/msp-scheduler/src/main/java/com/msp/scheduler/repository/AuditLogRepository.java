package com.msp.scheduler.repository;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.msp.common.core.AuditLog;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 审计日志仓储层
 */
@Repository
public class AuditLogRepository {

    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;

    public AuditLogRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = new ObjectMapper();
    }

    public void save(AuditLog auditLog) {
        String sql = """
            INSERT INTO msp_audit_logs (log_id, user_id, action, resource_type, resource_id, details, ip_address, create_time)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?)
            """;
        jdbcTemplate.update(sql,
            auditLog.getLogId(),
            auditLog.getUserId(),
            auditLog.getAction(),
            auditLog.getResourceType(),
            auditLog.getResourceId(),
            toJson(auditLog.getDetails()),
            auditLog.getIpAddress(),
            auditLog.getCreateTime() != null ? new java.sql.Timestamp(auditLog.getCreateTime()) : null
        );
    }

    public List<AuditLog> findAll(String userId, String action, String resourceType, long startTime, long endTime, int page, int size) {
        StringBuilder sql = new StringBuilder("SELECT * FROM msp_audit_logs WHERE 1=1");
        java.util.List<Object> params = new java.util.ArrayList<>();

        if (userId != null && !userId.isEmpty()) {
            sql.append(" AND user_id = ?");
            params.add(userId);
        }
        if (action != null && !action.isEmpty()) {
            sql.append(" AND action = ?");
            params.add(action);
        }
        if (resourceType != null && !resourceType.isEmpty()) {
            sql.append(" AND resource_type = ?");
            params.add(resourceType);
        }
        if (startTime > 0) {
            sql.append(" AND create_time >= ?");
            params.add(new java.sql.Timestamp(startTime));
        }
        if (endTime > 0) {
            sql.append(" AND create_time <= ?");
            params.add(new java.sql.Timestamp(endTime));
        }

        sql.append(" ORDER BY create_time DESC LIMIT ? OFFSET ?");
        params.add(size);
        params.add(page * size);

        return jdbcTemplate.query(sql.toString(), new AuditLogRowMapper(objectMapper), params.toArray());
    }

    public long count(String userId, String action, String resourceType, long startTime, long endTime) {
        StringBuilder sql = new StringBuilder("SELECT COUNT(*) FROM msp_audit_logs WHERE 1=1");
        java.util.List<Object> params = new java.util.ArrayList<>();

        if (userId != null && !userId.isEmpty()) {
            sql.append(" AND user_id = ?");
            params.add(userId);
        }
        if (action != null && !action.isEmpty()) {
            sql.append(" AND action = ?");
            params.add(action);
        }
        if (resourceType != null && !resourceType.isEmpty()) {
            sql.append(" AND resource_type = ?");
            params.add(resourceType);
        }
        if (startTime > 0) {
            sql.append(" AND create_time >= ?");
            params.add(startTime);
        }
        if (endTime > 0) {
            sql.append(" AND create_time <= ?");
            params.add(endTime);
        }

        Long count = jdbcTemplate.queryForObject(sql.toString(), Long.class, params.toArray());
        return count != null ? count : 0;
    }

    private String toJson(Object obj) {
        if (obj == null) return null;
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    private static java.util.Map<String, Object> parseDetails(String json) {
        if (json == null || json.isEmpty()) return null;
        try {
            return new ObjectMapper().readValue(json, java.util.Map.class);
        } catch (Exception e) {
            return null;
        }
    }

    private static class AuditLogRowMapper implements RowMapper<AuditLog> {
        private final ObjectMapper objectMapper;

        public AuditLogRowMapper(ObjectMapper objectMapper) {
            this.objectMapper = objectMapper;
        }

        @Override
        public AuditLog mapRow(java.sql.ResultSet rs, int rowNum) throws java.sql.SQLException {
            AuditLog log = new AuditLog();
            log.setLogId(rs.getString("log_id"));
            log.setUserId(rs.getString("user_id"));
            log.setAction(rs.getString("action"));
            log.setResourceType(rs.getString("resource_type"));
            log.setResourceId(rs.getString("resource_id"));
            log.setDetails(parseDetails(rs.getString("details")));
            log.setIpAddress(rs.getString("ip_address"));

            java.sql.Timestamp createTime = rs.getTimestamp("create_time");
            log.setCreateTime(createTime != null ? createTime.getTime() : null);

            return log;
        }

        @SuppressWarnings("unchecked")
        private java.util.Map<String, Object> parseDetails(String json) {
            if (json == null || json.isEmpty()) return null;
            try {
                return objectMapper.readValue(json,
                    objectMapper.getTypeFactory().constructMapType(java.util.Map.class, String.class, Object.class));
            } catch (Exception e) {
                return null;
            }
        }
    }
}