package com.msp.scheduler.service;

import com.msp.common.core.ColumnInfo;
import com.msp.common.core.DataSource;
import com.msp.common.core.ErrorCode;
import com.msp.common.core.MspException;
import com.msp.common.core.Page;
import com.msp.common.core.TableInfo;
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
    public List<List<Object>> getSampleData(String dbName, String tableName) {
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
    public List<TableInfo> getDataSourceTables(String datasourceId) {
        DataSource ds = dataSourceRepository.findById(datasourceId)
            .orElseThrow(() -> new MspException(ErrorCode.DATA_SOURCE_ERROR, "DataSource not found: " + datasourceId));

        try {
            String url = String.format("jdbc:mysql://%s:%d/%s?useUnicode=true&characterEncoding=UTF-8&serverTimezone=Asia/Shanghai",
                ds.getHost(), ds.getPort() != null ? ds.getPort() : 3306, ds.getDatabase());

            try (Connection conn = DriverManager.getConnection(url, resolveUser(ds), resolvePass(ds))) {
                // 修复 MySQL Connector/J 元数据查询 REMARKS 用 Latin-1 解析的 bug：
                // 直接 getBytes 拿原始字节（实为 UTF-8），再按 UTF-8 重新解码
                var rs = conn.getMetaData().getTables(ds.getDatabase(), null, null, new String[]{"TABLE"});
                java.util.List<TableInfo> tables = new java.util.ArrayList<>();
                while (rs.next()) {
                    String name = rs.getString("TABLE_NAME");
                    String comment = decodeRemarks(rs.getBytes("REMARKS"));
                    tables.add(new TableInfo(name, comment));
                }
                return tables;
            }
        } catch (Exception e) {
            throw new RuntimeException("获取表名失败: " + e.getMessage(), e);
        }
    }

    @Override
    public List<ColumnInfo> getDataSourceColumns(String datasourceId, String tableName) {
        DataSource ds = dataSourceRepository.findById(datasourceId)
            .orElseThrow(() -> new MspException(ErrorCode.DATA_SOURCE_ERROR, "DataSource not found: " + datasourceId));

        try {
            String url = String.format("jdbc:mysql://%s:%d/%s?useUnicode=true&characterEncoding=UTF-8&serverTimezone=Asia/Shanghai",
                ds.getHost(), ds.getPort() != null ? ds.getPort() : 3306, ds.getDatabase());

            try (Connection conn = DriverManager.getConnection(url, resolveUser(ds), resolvePass(ds))) {
                // 同上：getBytes + UTF-8 重新解码
                var rs = conn.getMetaData().getColumns(ds.getDatabase(), null, tableName, null);
                java.util.List<ColumnInfo> columns = new java.util.ArrayList<>();
                while (rs.next()) {
                    String name = rs.getString("COLUMN_NAME");
                    String comment = decodeRemarks(rs.getBytes("REMARKS"));
                    columns.add(new ColumnInfo(name, comment));
                }
                return columns;
            }
        } catch (Exception e) {
            throw new RuntimeException("获取字段失败: " + e.getMessage(), e);
        }
    }

    /**
     * 修复 MySQL Connector/J DatabaseMetaData 返回的 REMARKS 字段被当成 Latin-1 解码的 bug。
     * <p>实际字节是 UTF-8，但 getString() 把每个字节当一个 char。getBytes() 拿到的是原始字节流，
     * 用 UTF-8 重新 new String 即可恢复正确字符。</p>
     */
    private String decodeRemarks(byte[] bytes) {
        if (bytes == null) {
            return null;
        }
        try {
            return new String(bytes, "UTF-8");
        } catch (java.io.UnsupportedEncodingException e) {
            return new String(bytes);
        }
    }

    /**
     * 解析数据源连接用户名：优先使用数据源自身的 username，
     * 为空时回退到环境变量或默认值（兼容历史数据）。
     */
    private String resolveUser(DataSource ds) {
        if (ds.getUsername() != null && !ds.getUsername().isEmpty()) {
            return ds.getUsername();
        }
        String envUser = System.getenv("DATA_SOURCE_DB_USER");
        return envUser != null ? envUser : "msp";
    }

    /**
     * 解析数据源连接密码：优先使用数据源自身的 password。
     */
    private String resolvePass(DataSource ds) {
        if (ds.getPassword() != null && !ds.getPassword().isEmpty()) {
            return ds.getPassword();
        }
        String envPass = System.getenv("DATA_SOURCE_DB_PASSWORD");
        return envPass != null ? envPass : "msp123456";
    }

    private boolean testMySQLConnection(DataSource ds) {
        String url = String.format("jdbc:mysql://%s:%d/%s",
            ds.getHost(), ds.getPort() != null ? ds.getPort() : 3306, ds.getDatabase());
        try (Connection conn = DriverManager.getConnection(url, resolveUser(ds), resolvePass(ds))) {
            return conn.isValid(5);
        } catch (Exception e) {
            return false;
        }
    }

    private boolean testPostgreSQLConnection(DataSource ds) {
        String url = String.format("jdbc:postgresql://%s:%d/%s",
            ds.getHost(), ds.getPort() != null ? ds.getPort() : 5432, ds.getDatabase());
        try (Connection conn = DriverManager.getConnection(url, resolveUser(ds), resolvePass(ds))) {
            return conn.isValid(5);
        } catch (Exception e) {
            return false;
        }
    }
}