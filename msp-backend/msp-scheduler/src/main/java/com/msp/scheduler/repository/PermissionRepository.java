package com.msp.scheduler.repository;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.msp.common.core.Permission;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

/**
 * 权限仓储层
 */
@Repository
public class PermissionRepository {

    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;

    public PermissionRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = new ObjectMapper();
    }

    public void save(Permission permission) {
        String sql = """
            INSERT INTO msp_permissions (permission_id, permission_code, permission_name, resource_type, parent_id, path, icon, sort_order, create_time)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
            """;
        jdbcTemplate.update(sql,
            permission.getPermissionId(),
            permission.getPermissionCode(),
            permission.getPermissionName(),
            permission.getResourceType() != null ? permission.getResourceType().name() : null,
            permission.getParentId(),
            permission.getPath(),
            permission.getIcon(),
            permission.getSortOrder(),
            new java.sql.Timestamp(System.currentTimeMillis())
        );
    }

    public void update(Permission permission) {
        String sql = """
            UPDATE msp_permissions
            SET permission_name = ?, resource_type = ?, parent_id = ?, path = ?, icon = ?, sort_order = ?
            WHERE permission_id = ?
            """;
        jdbcTemplate.update(sql,
            permission.getPermissionName(),
            permission.getResourceType() != null ? permission.getResourceType().name() : null,
            permission.getParentId(),
            permission.getPath(),
            permission.getIcon(),
            permission.getSortOrder(),
            permission.getPermissionId()
        );
    }

    public Optional<Permission> findById(String permissionId) {
        String sql = "SELECT * FROM msp_permissions WHERE permission_id = ?";
        List<Permission> results = jdbcTemplate.query(sql, new PermissionRowMapper(objectMapper), permissionId);
        return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
    }

    public Optional<Permission> findByCode(String permissionCode) {
        String sql = "SELECT * FROM msp_permissions WHERE permission_code = ?";
        List<Permission> results = jdbcTemplate.query(sql, new PermissionRowMapper(objectMapper), permissionCode);
        return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
    }

    public List<Permission> findAll() {
        String sql = "SELECT * FROM msp_permissions ORDER BY sort_order ASC, create_time ASC";
        return jdbcTemplate.query(sql, new PermissionRowMapper(objectMapper));
    }

    public List<Permission> findByParentId(String parentId) {
        String sql = "SELECT * FROM msp_permissions WHERE parent_id = ? OR (parent_id IS NULL AND ? IS NULL) ORDER BY sort_order ASC";
        return jdbcTemplate.query(sql, new PermissionRowMapper(objectMapper), parentId, parentId);
    }

    public void delete(String permissionId) {
        String sql = "DELETE FROM msp_permissions WHERE permission_id = ?";
        jdbcTemplate.update(sql, permissionId);
    }

    public void saveRolePermission(String roleId, String permissionId) {
        String sql = "INSERT INTO msp_role_permissions (role_id, permission_id, create_time) VALUES (?, ?, ?)";
        jdbcTemplate.update(sql, roleId, permissionId, new java.sql.Timestamp(System.currentTimeMillis()));
    }

    public void deleteRolePermissions(String roleId) {
        String sql = "DELETE FROM msp_role_permissions WHERE role_id = ?";
        jdbcTemplate.update(sql, roleId);
    }

    public List<String> findPermissionCodesByRoleId(String roleId) {
        String sql = """
            SELECT p.permission_code FROM msp_permissions p
            INNER JOIN msp_role_permissions rp ON p.permission_id = rp.permission_id
            WHERE rp.role_id = ?
            """;
        return jdbcTemplate.queryForList(sql, String.class, roleId);
    }

    private static class PermissionRowMapper implements RowMapper<Permission> {
        private final ObjectMapper objectMapper;

        public PermissionRowMapper(ObjectMapper objectMapper) {
            this.objectMapper = objectMapper;
        }

        @Override
        public Permission mapRow(ResultSet rs, int rowNum) throws SQLException {
            Permission permission = new Permission();
            permission.setPermissionId(rs.getString("permission_id"));
            permission.setPermissionCode(rs.getString("permission_code"));
            permission.setPermissionName(rs.getString("permission_name"));

            String resourceType = rs.getString("resource_type");
            if (resourceType != null && !resourceType.isEmpty()) {
                permission.setResourceType(Permission.ResourceType.valueOf(resourceType));
            }

            permission.setParentId(rs.getString("parent_id"));
            permission.setPath(rs.getString("path"));
            permission.setIcon(rs.getString("icon"));
            permission.setSortOrder(rs.getInt("sort_order"));

            return permission;
        }
    }
}