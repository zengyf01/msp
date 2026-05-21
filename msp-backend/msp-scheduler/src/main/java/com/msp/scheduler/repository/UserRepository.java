package com.msp.scheduler.repository;

import com.msp.common.core.User;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * 用户仓储层
 */
@Repository
public class UserRepository {

    private final JdbcTemplate jdbcTemplate;

    public UserRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void save(User user) {
        String sql = """
            INSERT INTO msp_users (user_id, username, password, email, phone, role, status, enabled, create_time, update_time)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """;
        jdbcTemplate.update(sql,
            user.getUserId(),
            user.getUsername(),
            user.getPassword(),
            user.getEmail(),
            user.getPhone(),
            user.getRole() != null ? user.getRole().name() : User.UserRole.USER.name(),
            user.getStatus() != null ? user.getStatus() : "ACTIVE",
            user.isEnabled(),
            user.getCreateTime() != null ? new java.sql.Timestamp(user.getCreateTime()) : null,
            user.getUpdateTime() != null ? new java.sql.Timestamp(user.getUpdateTime()) : null
        );
    }

    public void update(User user) {
        String sql = """
            UPDATE msp_users
            SET username = ?, password = ?, role = ?, enabled = ?, update_time = ?
            WHERE user_id = ?
            """;
        jdbcTemplate.update(sql,
            user.getUsername(),
            user.getPassword(),
            user.getRole() != null ? user.getRole().name() : User.UserRole.USER.name(),
            user.isEnabled(),
            System.currentTimeMillis(),
            user.getUserId()
        );
    }

    public Optional<User> findById(String userId) {
        String sql = "SELECT * FROM msp_users WHERE user_id = ?";
        var results = jdbcTemplate.query(sql, new UserRowMapper(), userId);
        return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
    }

    public Optional<User> findByUsername(String username) {
        String sql = "SELECT * FROM msp_users WHERE username = ?";
        var results = jdbcTemplate.query(sql, new UserRowMapper(), username);
        return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
    }

    public void delete(String userId) {
        String sql = "DELETE FROM msp_users WHERE user_id = ?";
        jdbcTemplate.update(sql, userId);
    }

    public java.util.List<User> findAll(int page, int size) {
        String sql = "SELECT * FROM msp_users ORDER BY create_time DESC LIMIT ? OFFSET ?";
        return jdbcTemplate.query(sql, new UserRowMapper(), size, page * size);
    }

    public long count() {
        String sql = "SELECT COUNT(*) FROM msp_users";
        Long result = jdbcTemplate.queryForObject(sql, Long.class);
        return result != null ? result : 0;
    }

    public java.util.List<String> findRoleCodesByUserId(String userId) {
        String sql = """
            SELECT r.role_code FROM msp_roles r
            INNER JOIN msp_user_roles ur ON r.role_id = ur.role_id
            WHERE ur.user_id = ?
            """;
        return jdbcTemplate.queryForList(sql, String.class, userId);
    }

    private static class UserRowMapper implements RowMapper<User> {
        @Override
        public User mapRow(java.sql.ResultSet rs, int rowNum) throws java.sql.SQLException {
            User user = new User();
            user.setUserId(rs.getString("user_id"));
            user.setUsername(rs.getString("username"));
            user.setPassword(rs.getString("password"));
            user.setEmail(rs.getString("email"));
            user.setPhone(rs.getString("phone"));
            user.setRole(parseEnum(User.UserRole.class, rs.getString("role")));
            user.setStatus(rs.getString("status"));
            user.setEnabled(rs.getBoolean("enabled"));

            java.sql.Timestamp createTime = rs.getTimestamp("create_time");
            user.setCreateTime(createTime != null ? createTime.getTime() : null);

            java.sql.Timestamp updateTime = rs.getTimestamp("update_time");
            user.setUpdateTime(updateTime != null ? updateTime.getTime() : null);

            return user;
        }

        private <T extends Enum<T>> T parseEnum(Class<T> clazz, String value) {
            if (value == null || value.isEmpty()) return null;
            try {
                return Enum.valueOf(clazz, value);
            } catch (IllegalArgumentException e) {
                return null;
            }
        }
    }
}