package com.msp.scheduler.repository;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.msp.common.core.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 任务仓储层
 */
@Repository
public class TaskRepository {

    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;

    public TaskRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = new ObjectMapper();
    }

    public void save(Task task) {
        String sql = """
            INSERT INTO msp_tasks (task_id, name, type, algorithm, status, participants, inputs, parameters, description, create_time, update_time)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """;
        jdbcTemplate.update(sql,
            task.getTaskId(),
            task.getName(),
            task.getType() != null ? task.getType().name() : null,
            task.getAlgorithm(),
            task.getStatus() != null ? task.getStatus().name() : null,
            toJson(task.getParticipants()),
            toJson(task.getInputs()),
            toJson(task.getParameters()),
            task.getDescription(),
            task.getCreateTime() != null ? new java.sql.Timestamp(task.getCreateTime()) : null,
            task.getUpdateTime() != null ? new java.sql.Timestamp(task.getUpdateTime()) : null
        );
    }

    public void update(Task task) {
        String sql = """
            UPDATE msp_tasks
            SET name = ?, type = ?, algorithm = ?, status = ?, participants = ?, inputs = ?, parameters = ?, description = ?, update_time = ?
            WHERE task_id = ?
            """;
        jdbcTemplate.update(sql,
            task.getName(),
            task.getType() != null ? task.getType().name() : null,
            task.getAlgorithm(),
            task.getStatus() != null ? task.getStatus().name() : null,
            toJson(task.getParticipants()),
            toJson(task.getInputs()),
            toJson(task.getParameters()),
            task.getDescription(),
            new java.sql.Timestamp(System.currentTimeMillis()),
            task.getTaskId()
        );
    }

    public Optional<Task> findById(String taskId) {
        String sql = "SELECT * FROM msp_tasks WHERE task_id = ?";
        List<Task> results = jdbcTemplate.query(sql, new TaskRowMapper(objectMapper), taskId);
        return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
    }

    public List<Task> findAll(TaskStatus status, TaskType type, int page, int size) {
        StringBuilder sql = new StringBuilder("SELECT * FROM msp_tasks WHERE 1=1");
        if (status != null) {
            sql.append(" AND status = ?");
        }
        if (type != null) {
            sql.append(" AND type = ?");
        }
        sql.append(" ORDER BY create_time DESC LIMIT ? OFFSET ?");

        Object[] params = buildParams(status, type, size, page * size);
        return jdbcTemplate.query(sql.toString(), new TaskRowMapper(objectMapper), params);
    }

    public long count(TaskStatus status, TaskType type) {
        StringBuilder sql = new StringBuilder("SELECT COUNT(*) FROM msp_tasks WHERE 1=1");
        if (status != null) {
            sql.append(" AND status = ?");
        }
        if (type != null) {
            sql.append(" AND type = ?");
        }
        Object[] params = buildCountParams(status, type);
        Long count = jdbcTemplate.queryForObject(sql.toString(), Long.class, params);
        return count != null ? count : 0;
    }

    public void updateStatus(String taskId, TaskStatus status) {
        String sql = "UPDATE msp_tasks SET status = ?, update_time = ? WHERE task_id = ?";
        jdbcTemplate.update(sql, status.name(), new java.sql.Timestamp(System.currentTimeMillis()), taskId);
    }

    public void delete(String taskId) {
        String sql = "DELETE FROM msp_tasks WHERE task_id = ?";
        jdbcTemplate.update(sql, taskId);
    }

    /**
     * 查询活跃任务（用于状态轮询）
     * @param limit 返回数量限制
     * @return 活跃任务列表
     */
    public List<Task> findActiveTasks(int limit) {
        String sql = """
            SELECT * FROM msp_tasks
            WHERE status IN ('PENDING', 'RUNNING')
            ORDER BY create_time ASC
            LIMIT ?
            """;
        return jdbcTemplate.query(sql, new TaskRowMapper(objectMapper), limit);
    }

    private Object[] buildParams(TaskStatus status, TaskType type, int size, int offset) {
        java.util.List<Object> params = new java.util.ArrayList<>();
        if (status != null) {
            params.add(status.name());
        }
        if (type != null) {
            params.add(type.name());
        }
        params.add(size);
        params.add(offset);
        return params.toArray();
    }

    private Object[] buildCountParams(TaskStatus status, TaskType type) {
        java.util.List<Object> params = new java.util.ArrayList<>();
        if (status != null) {
            params.add(status.name());
        }
        if (type != null) {
            params.add(type.name());
        }
        return params.toArray();
    }

    private String toJson(Object obj) {
        if (obj == null) return null;
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            return null;
        }
    }

    private static class TaskRowMapper implements RowMapper<Task> {
        private final ObjectMapper objectMapper;

        public TaskRowMapper(ObjectMapper objectMapper) {
            this.objectMapper = objectMapper;
        }

        @Override
        public Task mapRow(java.sql.ResultSet rs, int rowNum) throws java.sql.SQLException {
            Task task = new Task();
            task.setTaskId(rs.getString("task_id"));
            task.setName(rs.getString("name"));
            task.setType(parseEnum(TaskType.class, rs.getString("type")));
            task.setAlgorithm(rs.getString("algorithm"));
            task.setStatus(parseEnum(TaskStatus.class, rs.getString("status")));
            task.setParticipants(parseStringList(rs.getString("participants")));
            task.setInputs(parseDataSourceMap(rs.getString("inputs")));
            task.setParameters(parseParametersMap(rs.getString("parameters")));
            task.setDescription(rs.getString("description"));

            java.sql.Timestamp createTime = rs.getTimestamp("create_time");
            task.setCreateTime(createTime != null ? createTime.getTime() : null);

            java.sql.Timestamp updateTime = rs.getTimestamp("update_time");
            task.setUpdateTime(updateTime != null ? updateTime.getTime() : null);

            return task;
        }

        private <T extends Enum<T>> T parseEnum(Class<T> clazz, String value) {
            if (value == null || value.isEmpty()) return null;
            try {
                return Enum.valueOf(clazz, value);
            } catch (IllegalArgumentException e) {
                return null;
            }
        }

        @SuppressWarnings("unchecked")
        private java.util.List<String> parseStringList(String json) {
            if (json == null || json.isEmpty()) return null;
            try {
                return objectMapper.readValue(json,
                    objectMapper.getTypeFactory().constructCollectionType(java.util.List.class, String.class));
            } catch (JsonProcessingException e) {
                return null;
            }
        }

        @SuppressWarnings("unchecked")
        private java.util.Map<String, DataSource> parseDataSourceMap(String json) {
            if (json == null || json.isEmpty()) return null;
            try {
                return (java.util.Map<String, DataSource>) objectMapper.readValue(json,
                    objectMapper.getTypeFactory().constructMapType(java.util.Map.class, String.class, DataSource.class));
            } catch (JsonProcessingException e) {
                return null;
            }
        }

        @SuppressWarnings("unchecked")
        private java.util.Map<String, String> parseParametersMap(String json) {
            if (json == null || json.isEmpty()) return null;
            try {
                return (java.util.Map<String, String>) objectMapper.readValue(json,
                    objectMapper.getTypeFactory().constructMapType(java.util.Map.class, String.class, String.class));
            } catch (JsonProcessingException e) {
                return null;
            }
        }
    }
}