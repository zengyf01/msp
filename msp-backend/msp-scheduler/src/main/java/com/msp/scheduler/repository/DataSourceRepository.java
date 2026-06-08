package com.msp.scheduler.repository;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.msp.common.core.DataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 数据源仓储层
 */
@Repository
public class DataSourceRepository {

    private static final Logger log = LoggerFactory.getLogger(DataSourceRepository.class);

    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;

    public DataSourceRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = new ObjectMapper();
    }

    public void save(DataSource dataSource) {
        String sql = """
            INSERT INTO msp_datasources (datasource_id, node_id, name, type, host, port, database_name, username, password, table_name, columns, create_time, update_time)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """;
        log.info("[SQL] INSERT | table=msp_datasources | datasource_id={}", dataSource.getDataSourceId());
        long now = System.currentTimeMillis();
        jdbcTemplate.update(sql,
            dataSource.getDataSourceId(),
            dataSource.getNodeId(),
            dataSource.getName(),
            dataSource.getType() != null ? dataSource.getType().name() : null,
            dataSource.getHost(),
            dataSource.getPort(),
            dataSource.getDatabase(),
            dataSource.getUsername(),
            dataSource.getPassword(),
            dataSource.getTableName(),
            toJson(dataSource.getColumns()),
            new java.sql.Timestamp(now),
            new java.sql.Timestamp(now)
        );
    }

    public void update(DataSource dataSource) {
        String sql = """
            UPDATE msp_datasources
            SET node_id = ?, name = ?, type = ?, host = ?, port = ?, database_name = ?, username = ?, password = ?, table_name = ?, columns = ?, update_time = ?
            WHERE datasource_id = ?
            """;
        log.info("[SQL] UPDATE | table=msp_datasources | datasource_id={}", dataSource.getDataSourceId());
        jdbcTemplate.update(sql,
            dataSource.getNodeId(),
            dataSource.getName(),
            dataSource.getType() != null ? dataSource.getType().name() : null,
            dataSource.getHost(),
            dataSource.getPort(),
            dataSource.getDatabase(),
            dataSource.getUsername(),
            dataSource.getPassword(),
            dataSource.getTableName(),
            toJson(dataSource.getColumns()),
            new java.sql.Timestamp(System.currentTimeMillis()),
            dataSource.getDataSourceId()
        );
    }

    public Optional<DataSource> findById(String datasourceId) {
        String sql = "SELECT * FROM msp_datasources WHERE datasource_id = ?";
        log.info("[SQL] SELECT | table=msp_datasources | WHERE datasource_id={}", datasourceId);
        List<DataSource> results = jdbcTemplate.query(sql, new DataSourceRowMapper(objectMapper), datasourceId);
        return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
    }

    public List<DataSource> findByNodeId(String nodeId) {
        String sql = "SELECT * FROM msp_datasources WHERE node_id = ? ORDER BY create_time DESC";
        log.info("[SQL] SELECT | table=msp_datasources | WHERE node_id={}", nodeId);
        return jdbcTemplate.query(sql, new DataSourceRowMapper(objectMapper), nodeId);
    }

    public List<DataSource> findAll(DataSource.DataSourceType type, String nodeId, int page, int size) {
        StringBuilder sql = new StringBuilder("SELECT * FROM msp_datasources WHERE 1=1");
        if (type != null) {
            sql.append(" AND type = ?");
        }
        if (nodeId != null && !nodeId.isEmpty()) {
            sql.append(" AND node_id = ?");
        }
        sql.append(" ORDER BY create_time DESC LIMIT ? OFFSET ?");

        List<Object> params = new java.util.ArrayList<>();
        if (type != null) {
            params.add(type.name());
        }
        if (nodeId != null && !nodeId.isEmpty()) {
            params.add(nodeId);
        }
        params.add(size);
        params.add(page * size);

        return jdbcTemplate.query(sql.toString(), new DataSourceRowMapper(objectMapper), params.toArray());
    }

    public long count(DataSource.DataSourceType type, String nodeId) {
        StringBuilder sql = new StringBuilder("SELECT COUNT(*) FROM msp_datasources WHERE 1=1");
        List<Object> params = new java.util.ArrayList<>();
        if (type != null) {
            sql.append(" AND type = ?");
            params.add(type.name());
        }
        if (nodeId != null && !nodeId.isEmpty()) {
            sql.append(" AND node_id = ?");
            params.add(nodeId);
        }
        Long count = jdbcTemplate.queryForObject(sql.toString(), Long.class, params.toArray());
        return count != null ? count : 0;
    }

    public void delete(String datasourceId) {
        String sql = "DELETE FROM msp_datasources WHERE datasource_id = ?";
        log.info("[SQL] DELETE | table=msp_datasources | WHERE datasource_id={}", datasourceId);
        jdbcTemplate.update(sql, datasourceId);
    }

    private String toJson(Object obj) {
        if (obj == null) return null;
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            return null;
        }
    }

    private static class DataSourceRowMapper implements RowMapper<DataSource> {
        private final ObjectMapper objectMapper;

        public DataSourceRowMapper(ObjectMapper objectMapper) {
            this.objectMapper = objectMapper;
        }

        @Override
        public DataSource mapRow(java.sql.ResultSet rs, int rowNum) throws java.sql.SQLException {
            DataSource ds = new DataSource();
            ds.setDataSourceId(rs.getString("datasource_id"));
            ds.setNodeId(rs.getString("node_id"));
            ds.setName(rs.getString("name"));
            ds.setType(parseEnum(DataSource.DataSourceType.class, rs.getString("type")));
            ds.setHost(rs.getString("host"));
            ds.setPort(rs.getObject("port") != null ? rs.getInt("port") : null);
            ds.setDatabase(rs.getString("database_name"));
            ds.setUsername(rs.getString("username"));
            ds.setPassword(rs.getString("password"));
            ds.setTableName(rs.getString("table_name"));
            ds.setColumns(parseStringList(rs.getString("columns")));

            java.sql.Timestamp createTime = rs.getTimestamp("create_time");
            ds.setCreateTime(createTime != null ? createTime.getTime() : null);

            java.sql.Timestamp updateTime = rs.getTimestamp("update_time");
            ds.setUpdateTime(updateTime != null ? updateTime.getTime() : null);

            return ds;
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
        private List<String> parseStringList(String json) {
            if (json == null || json.isEmpty()) return null;
            try {
                return objectMapper.readValue(json,
                    objectMapper.getTypeFactory().constructCollectionType(List.class, String.class));
            } catch (JsonProcessingException e) {
                return null;
            }
        }
    }
}