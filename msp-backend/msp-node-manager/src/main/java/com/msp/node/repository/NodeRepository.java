package com.msp.node.repository;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.msp.common.core.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * 节点仓储层
 */
@Repository
public class NodeRepository {

    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;

    public NodeRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = new ObjectMapper();
    }

    public void save(Node node) {
        String sql = """
            INSERT INTO msp_nodes (node_id, node_name, status, node_mode, endpoint, external_endpoint, capabilities, tags, create_time, update_time)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            ON DUPLICATE KEY UPDATE
                node_name = VALUES(node_name),
                status = VALUES(status),
                node_mode = VALUES(node_mode),
                endpoint = VALUES(endpoint),
                external_endpoint = VALUES(external_endpoint),
                capabilities = VALUES(capabilities),
                tags = VALUES(tags),
                update_time = VALUES(update_time)
            """;
        jdbcTemplate.update(sql,
            node.getNodeId(),
            node.getNodeName(),
            node.getStatus() != null ? node.getStatus().name() : null,
            node.getNodeMode(),
            node.getEndpoint(),
            node.getExternalEndpoint(),
            toJson(node.getCapabilities()),
            toJson(node.getTags()),
            node.getCreateTime() != null ? new java.sql.Timestamp(node.getCreateTime()) : null,
            node.getUpdateTime() != null ? new java.sql.Timestamp(node.getUpdateTime()) : null
        );
    }

    public void update(Node node) {
        String sql = """
            UPDATE msp_nodes
            SET node_name = ?, status = ?, node_mode = ?, endpoint = ?, external_endpoint = ?, capabilities = ?, tags = ?, update_time = ?
            WHERE node_id = ?
            """;
        jdbcTemplate.update(sql,
            node.getNodeName(),
            node.getStatus() != null ? node.getStatus().name() : null,
            node.getNodeMode(),
            node.getEndpoint(),
            node.getExternalEndpoint(),
            toJson(node.getCapabilities()),
            toJson(node.getTags()),
            new java.sql.Timestamp(System.currentTimeMillis()),
            node.getNodeId()
        );
    }

    public Optional<Node> findById(String nodeId) {
        String sql = "SELECT * FROM msp_nodes WHERE node_id = ?";
        List<Node> results = jdbcTemplate.query(sql, new NodeRowMapper(objectMapper), nodeId);
        return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
    }

    public List<Node> findAll(NodeStatus status, DeviceType capability, int page, int size) {
        StringBuilder sql = new StringBuilder("SELECT * FROM msp_nodes WHERE 1=1");
        if (status != null) {
            sql.append(" AND status = ?");
        }
        sql.append(" ORDER BY create_time DESC LIMIT ? OFFSET ?");

        Object[] params = buildParams(status, size, page * size);
        List<Node> results = jdbcTemplate.query(sql.toString(), new NodeRowMapper(objectMapper), params);

        if (capability != null && results.size() > 0) {
            results = results.stream()
                .filter(n -> n.getCapabilities() != null && n.getCapabilities().contains(capability))
                .toList();
        }

        return results;
    }

    public long count(NodeStatus status) {
        String sql = "SELECT COUNT(*) FROM msp_nodes WHERE 1=1";
        if (status != null) {
            sql += " AND status = ?";
            Long count = jdbcTemplate.queryForObject(sql, Long.class, status.name());
            return count != null ? count : 0;
        }
        Long count = jdbcTemplate.queryForObject(sql, Long.class);
        return count != null ? count : 0;
    }

    public void updateStatus(String nodeId, NodeStatus status) {
        String sql = "UPDATE msp_nodes SET status = ?, update_time = ? WHERE node_id = ?";
        jdbcTemplate.update(sql, status.name(), new java.sql.Timestamp(System.currentTimeMillis()), nodeId);
    }

    public void delete(String nodeId) {
        String sql = "DELETE FROM msp_nodes WHERE node_id = ?";
        jdbcTemplate.update(sql, nodeId);
    }

    private Object[] buildParams(NodeStatus status, int size, int offset) {
        if (status != null) {
            return new Object[]{status.name(), size, offset};
        }
        return new Object[]{size, offset};
    }

    private String toJson(Object obj) {
        if (obj == null) return null;
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            return null;
        }
    }

    private static class NodeRowMapper implements RowMapper<Node> {
        private final ObjectMapper objectMapper;

        public NodeRowMapper(ObjectMapper objectMapper) {
            this.objectMapper = objectMapper;
        }

        @Override
        public Node mapRow(java.sql.ResultSet rs, int rowNum) throws java.sql.SQLException {
            Node node = new Node();
            node.setNodeId(rs.getString("node_id"));
            node.setNodeName(rs.getString("node_name"));
            node.setStatus(parseEnum(NodeStatus.class, rs.getString("status")));
            node.setNodeMode(rs.getString("node_mode"));
            node.setEndpoint(rs.getString("endpoint"));
            node.setExternalEndpoint(rs.getString("external_endpoint"));
            node.setCapabilities(parseSet(rs.getString("capabilities")));
            node.setTags(parseStringList(rs.getString("tags")));

            java.sql.Timestamp createTime = rs.getTimestamp("create_time");
            node.setCreateTime(createTime != null ? createTime.getTime() : null);

            java.sql.Timestamp updateTime = rs.getTimestamp("update_time");
            node.setUpdateTime(updateTime != null ? updateTime.getTime() : null);

            return node;
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

        @SuppressWarnings("unchecked")
        private Set<DeviceType> parseSet(String json) {
            if (json == null || json.isEmpty()) return null;
            try {
                return objectMapper.readValue(json,
                    objectMapper.getTypeFactory().constructCollectionType(Set.class, DeviceType.class));
            } catch (JsonProcessingException e) {
                return null;
            }
        }
    }
}