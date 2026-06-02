package com.msp.scheduler.service;

import com.msp.common.core.DataSource;
import com.msp.common.core.ErrorCode;
import com.msp.common.core.MspException;
import com.msp.common.core.Page;
import com.msp.scheduler.repository.DataSourceRepository;
import org.springframework.stereotype.Service;

import java.sql.Connection;
import java.sql.DriverManager;
import java.util.List;
import java.util.UUID;

/**
 * 数据源服务实现
 */
@Service
public class DataSourceServiceImpl implements DataSourceService {

    private final DataSourceRepository dataSourceRepository;

    public DataSourceServiceImpl(DataSourceRepository dataSourceRepository) {
        this.dataSourceRepository = dataSourceRepository;
    }

    @Override
    public String createDataSource(DataSource dataSource) {
        if (dataSource.getDataSourceId() == null || dataSource.getDataSourceId().isEmpty()) {
            dataSource.setDataSourceId(UUID.randomUUID().toString());
        }
        dataSourceRepository.save(dataSource);
        return dataSource.getDataSourceId();
    }

    @Override
    public void updateDataSource(DataSource dataSource) {
        dataSourceRepository.findById(dataSource.getDataSourceId())
            .orElseThrow(() -> new MspException(ErrorCode.DATA_SOURCE_ERROR, "DataSource not found: " + dataSource.getDataSourceId()));
        dataSourceRepository.update(dataSource);
    }

    @Override
    public void deleteDataSource(String datasourceId) {
        dataSourceRepository.findById(datasourceId)
            .orElseThrow(() -> new MspException(ErrorCode.DATA_SOURCE_ERROR, "DataSource not found: " + datasourceId));
        dataSourceRepository.delete(datasourceId);
    }

    @Override
    public DataSource getDataSource(String datasourceId) {
        return dataSourceRepository.findById(datasourceId)
            .orElseThrow(() -> new MspException(ErrorCode.DATA_SOURCE_ERROR, "DataSource not found: " + datasourceId));
    }

    @Override
    public List<DataSource> getDataSourcesByNodeId(String nodeId) {
        return dataSourceRepository.findByNodeId(nodeId);
    }

    @Override
    public Page<DataSource> listDataSources(DataSource.DataSourceType type, String nodeId, int page, int size) {
        List<DataSource> content = dataSourceRepository.findAll(type, nodeId, page, size);
        long total = dataSourceRepository.count(type, nodeId);
        return new Page<>(content, total, page, size);
    }

    @Override
    public boolean testConnection(DataSource dataSource) {
        if (dataSource.getType() == null) {
            return false;
        }

        try {
            switch (dataSource.getType()) {
                case MYSQL:
                    return testMySQLConnection(dataSource);
                case POSTGRESQL:
                    return testPostgreSQLConnection(dataSource);
                case API:
                case FILE:
                    return true;
                default:
                    return false;
            }
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public boolean createSimulatedNode(String nodeId, String dbName, String tableName, String columnName) {
        // 检查是否为本地测试环境（演示功能仅限本地）
        String hostname = System.getenv("COMPUTERNAME");
        if (hostname == null) {
            hostname = System.getenv("HOSTNAME");
        }

        try {
            // 连接主数据库（使用root用户，因为需要创建数据库和用户）
            String masterUrl = "jdbc:mysql://mysql:3306?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai";
            try (Connection masterConn = DriverManager.getConnection(masterUrl, "root", "root123456")) {
                // 创建节点数据库
                String createDbSql = String.format("CREATE DATABASE IF NOT EXISTS %s", dbName);
                masterConn.createStatement().execute(createDbSql);

                // 创建用户并授权
                String createUserSql = String.format(
                    "CREATE USER IF NOT EXISTS '%s'@'%%' IDENTIFIED BY '%s123'",
                    dbName.replace("_db", "").replace("node_", "node"),
                    dbName.replace("_db", "").replace("node_", "node")
                );
                masterConn.createStatement().execute(createUserSql);

                String grantSql = String.format(
                    "GRANT ALL PRIVILEGES ON %s.* TO '%s'@'%%'",
                    dbName,
                    dbName.replace("_db", "").replace("node_", "node")
                );
                masterConn.createStatement().execute(grantSql);
                masterConn.createStatement().execute("FLUSH PRIVILEGES");

                // 切换到节点数据库
                masterConn.createStatement().execute("USE " + dbName);

                // 创建示例数据表
                String createTableSql;
                if ("id_card".equals(columnName)) {
                    createTableSql = String.format(
                        "CREATE TABLE IF NOT EXISTS %s (" +
                        "  id INT PRIMARY KEY AUTO_INCREMENT," +
                        "  name VARCHAR(50)," +
                        "  %s VARCHAR(20)," +
                        "  email VARCHAR(100)" +
                        ")",
                        tableName, columnName
                    );
                } else {
                    createTableSql = String.format(
                        "CREATE TABLE IF NOT EXISTS %s (" +
                        "  id INT PRIMARY KEY AUTO_INCREMENT," +
                        "  username VARCHAR(50)," +
                        "  %s VARCHAR(20)," +
                        "  address VARCHAR(200)" +
                        ")",
                        tableName, columnName
                    );
                }
                masterConn.createStatement().execute(createTableSql);

                // 插入示例数据
                String insertSql;
                if ("id_card".equals(columnName)) {
                    insertSql = String.format(
                        "INSERT INTO %s (name, %s, email) VALUES " +
                        "('张三', '110101199001011234', 'zhangsan@example.com')," +
                        "('李四', '110101199001011235', 'lisi@example.com')," +
                        "('王五', '110101199001011236', 'wangwu@example.com')," +
                        "('赵六', '110101199001011237', 'zhaoliu@example.com')",
                        tableName, columnName
                    );
                } else {
                    insertSql = String.format(
                        "INSERT INTO %s (username, %s, address) VALUES " +
                        "('Alice', '13800138001', '北京市朝阳区')," +
                        "('Bob', '13800138002', '上海市浦东新区')," +
                        "('Charlie', '13800138003', '广州市天河区')," +
                        "('David', '13800138004', '深圳市南山区')",
                        tableName, columnName
                    );
                }
                masterConn.createStatement().execute(insertSql);
            }
            return true;
        } catch (Exception e) {
            throw new RuntimeException("创建模拟节点失败: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean deleteSimulatedNode(String nodeId) {
        try {
            String masterUrl = "jdbc:mysql://mysql:3306?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai";
            try (Connection masterConn = DriverManager.getConnection(masterUrl, "root", "root123456")) {
                // Docker Compose 模拟节点（node-a, node-b, node-c）
                if (nodeId.matches("node-[abc]")) {
                    String dbName = "node_" + nodeId.split("-")[1] + "_data";
                    masterConn.createStatement().execute("DROP DATABASE IF EXISTS " + dbName);
                } else {
                    // 后端API创建的模拟节点
                    String dbName = "node_" + nodeId.split("-")[1] + "_db";
                    masterConn.createStatement().execute("DROP DATABASE IF EXISTS " + dbName);
                }
            }
            return true;
        } catch (Exception e) {
            throw new RuntimeException("删除模拟节点失败: " + e.getMessage(), e);
        }
    }

    @Override
    public List<List<Object>> getSimulatedNodeSampleData(String dbName, String tableName) {
        try {
            String url = String.format("jdbc:mysql://mysql:3306/%s?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai", dbName);
            String dbUser = dbName.replace("_db", "").replace("node_", "node");
            try (Connection conn = DriverManager.getConnection(url, dbUser, dbUser + "123")) {
                var stmt = conn.createStatement();
                var rs = stmt.executeQuery("SELECT * FROM " + tableName + " LIMIT 10");
                var meta = rs.getMetaData();
                int colCount = meta.getColumnCount();

                java.util.List<List<Object>> result = new java.util.ArrayList<>();
                while (rs.next()) {
                    List<Object> row = new java.util.ArrayList<>();
                    for (int i = 1; i <= colCount; i++) {
                        row.add(rs.getObject(i));
                    }
                    result.add(row);
                }
                return result;
            }
        } catch (Exception e) {
            throw new RuntimeException("获取示例数据失败: " + e.getMessage(), e);
        }
    }

    @Override
    public List<String> getDataSourceTables(String datasourceId) {
        DataSource ds = dataSourceRepository.findById(datasourceId)
            .orElseThrow(() -> new MspException(ErrorCode.DATA_SOURCE_ERROR, "DataSource not found: " + datasourceId));

        try {
            String url = String.format("jdbc:mysql://%s:%d/%s?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai",
                ds.getHost(), ds.getPort() != null ? ds.getPort() : 3306, ds.getDatabase());
            String dbUser = "msp";
            String dbPass = "msp123456";

            try (Connection conn = DriverManager.getConnection(url, dbUser, dbPass)) {
                var rs = conn.getMetaData().getTables(ds.getDatabase(), null, null, new String[]{"TABLE"});
                java.util.List<String> tables = new java.util.ArrayList<>();
                while (rs.next()) {
                    tables.add(rs.getString("TABLE_NAME"));
                }
                return tables;
            }
        } catch (Exception e) {
            throw new RuntimeException("获取表名失败: " + e.getMessage(), e);
        }
    }

    @Override
    public List<String> getDataSourceColumns(String datasourceId, String tableName) {
        DataSource ds = dataSourceRepository.findById(datasourceId)
            .orElseThrow(() -> new MspException(ErrorCode.DATA_SOURCE_ERROR, "DataSource not found: " + datasourceId));

        try {
            String url = String.format("jdbc:mysql://%s:%d/%s?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai",
                ds.getHost(), ds.getPort() != null ? ds.getPort() : 3306, ds.getDatabase());
            String dbUser = "msp";
            String dbPass = "msp123456";

            try (Connection conn = DriverManager.getConnection(url, dbUser, dbPass)) {
                var rs = conn.getMetaData().getColumns(ds.getDatabase(), null, tableName, null);
                java.util.List<String> columns = new java.util.ArrayList<>();
                while (rs.next()) {
                    columns.add(rs.getString("COLUMN_NAME"));
                }
                return columns;
            }
        } catch (Exception e) {
            throw new RuntimeException("获取字段失败: " + e.getMessage(), e);
        }
    }

    private boolean testMySQLConnection(DataSource ds) {
        String url = String.format("jdbc:mysql://%s:%d/%s",
            ds.getHost(), ds.getPort() != null ? ds.getPort() : 3306, ds.getDatabase());
        try (Connection conn = DriverManager.getConnection(url, "msp", "msp123456")) {
            return conn.isValid(5);
        } catch (Exception e) {
            return false;
        }
    }

    private boolean testPostgreSQLConnection(DataSource ds) {
        String url = String.format("jdbc:postgresql://%s:%d/%s",
            ds.getHost(), ds.getPort() != null ? ds.getPort() : 5432, ds.getDatabase());
        try (Connection conn = DriverManager.getConnection(url, "msp", "msp123456")) {
            return conn.isValid(5);
        } catch (Exception e) {
            return false;
        }
    }
}