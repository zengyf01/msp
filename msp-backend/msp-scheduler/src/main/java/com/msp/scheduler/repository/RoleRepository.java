package com.msp.scheduler.repository;

import com.msp.common.core.Role;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

/**
 * 角色仓储层
 */
@Repository
public class RoleRepository {

    private final JdbcTemplate jdbcTemplate;

    public RoleRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void save(Role role) {
        String sql = """
            INSERT INTO msp_roles (role_id, role_code, role_name, description, status, create_time, update_time)
            VALUES (?, ?, ?, ?, ?, ?, ?)
            """;
        jdbcTemplate.update(sql,
            role.getRoleId(),
            role.getRoleCode(),
            role.getRoleName(),
            role.getDescription(),
            role.getStatus() != null ? role.getStatus().name() : null,
            role.getCreateTime() != null ? new java.sql.Timestamp(role.getCreateTime()) : null,
            role.getUpdateTime() != null ? new java.sql.Timestamp(role.getUpdateTime()) : null
        );
    }

    public void update(Role role) {
        String sql = """
            UPDATE msp_roles
            SET role_name = ?, description = ?, status = ?, update_time = ?
            WHERE role_id = ?
            """;
        jdbcTemplate.update(sql,
            role.getRoleName(),
            role.getDescription(),
            role.getStatus() != null ? role.getStatus().name() : null,
            new java.sql.Timestamp(System.currentTimeMillis()),
            role.getRoleId()
        );
    }

    public Optional<Role> findById(String roleId) {
        String sql = "SELECT * FROM msp_roles WHERE role_id = ?";
        List<Role> results = jdbcTemplate.query(sql, new RoleRowMapper(), roleId);
        return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
    }

    public Optional<Role> findByCode(String roleCode) {
        String sql = "SELECT * FROM msp_roles WHERE role_code = ?";
        List<Role> results = jdbcTemplate.query(sql, new RoleRowMapper(), roleCode);
        return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
    }

    public List<Role> findAll(int page, int size) {
        String sql = "SELECT * FROM msp_roles ORDER BY create_time DESC LIMIT ? OFFSET ?";
        return jdbcTemplate.query(sql, new RoleRowMapper(), size, page * size);
    }

    public long count() {
        String sql = "SELECT COUNT(*) FROM msp_roles";
        Long count = jdbcTemplate.queryForObject(sql, Long.class);
        return count != null ? count : 0;
    }

    public void delete(String roleId) {
        String sql = "DELETE FROM msp_roles WHERE role_id = ?";
        jdbcTemplate.update(sql, roleId);
    }

    public void saveUserRole(String userId, String roleId) {
        String sql = "INSERT INTO msp_user_roles (user_id, role_id, create_time) VALUES (?, ?, ?)";
        jdbcTemplate.update(sql, userId, roleId, new java.sql.Timestamp(System.currentTimeMillis()));
    }

    public void deleteUserRole(String userId, String roleId) {
        String sql = "DELETE FROM msp_user_roles WHERE user_id = ? AND role_id = ?";
        jdbcTemplate.update(sql, userId, roleId);
    }

    public List<String> findRoleCodesByUserId(String userId) {
        String sql = """
            SELECT r.role_code FROM msp_roles r
            INNER JOIN msp_user_roles ur ON r.role_id = ur.role_id
            WHERE ur.user_id = ? AND r.status = 'ACTIVE'
            """;
        return jdbcTemplate.queryForList(sql, String.class, userId);
    }

    private static class RoleRowMapper implements RowMapper<Role> {
        @Override
        public Role mapRow(ResultSet rs, int rowNum) throws SQLException {
            Role role = new Role();
            role.setRoleId(rs.getString("role_id"));
            role.setRoleCode(rs.getString("role_code"));
            role.setRoleName(rs.getString("role_name"));
            role.setDescription(rs.getString("description"));
            role.setStatus(Role.RoleStatus.valueOf(rs.getString("status")));

            java.sql.Timestamp createTime = rs.getTimestamp("create_time");
            role.setCreateTime(createTime != null ? createTime.getTime() : null);

            java.sql.Timestamp updateTime = rs.getTimestamp("update_time");
            role.setUpdateTime(updateTime != null ? updateTime.getTime() : null);

            return role;
        }
    }
}