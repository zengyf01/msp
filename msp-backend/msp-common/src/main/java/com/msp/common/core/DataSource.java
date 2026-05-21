package com.msp.common.core;

import java.util.List;
import java.util.Map;

/**
 * 数据源定义
 */
public class DataSource {
    private String dataSourceId;
    private String nodeId;
    private String name;
    private DataSourceType type;
    private String host;
    private Integer port;
    private String database;
    private String tableName;
    private List<String> columns;
    private Long createTime;
    private Long updateTime;

    public enum DataSourceType {
        MYSQL,
        POSTGRESQL,
        API,
        FILE
    }

    // Getters and Setters
    public String getDataSourceId() { return dataSourceId; }
    public void setDataSourceId(String dataSourceId) { this.dataSourceId = dataSourceId; }
    public String getNodeId() { return nodeId; }
    public void setNodeId(String nodeId) { this.nodeId = nodeId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public DataSourceType getType() { return type; }
    public void setType(DataSourceType type) { this.type = type; }
    public String getHost() { return host; }
    public void setHost(String host) { this.host = host; }
    public Integer getPort() { return port; }
    public void setPort(Integer port) { this.port = port; }
    public String getDatabase() { return database; }
    public void setDatabase(String database) { this.database = database; }
    public String getTableName() { return tableName; }
    public void setTableName(String tableName) { this.tableName = tableName; }
    public List<String> getColumns() { return columns; }
    public void setColumns(List<String> columns) { this.columns = columns; }
    public Long getCreateTime() { return createTime; }
    public void setCreateTime(Long createTime) { this.createTime = createTime; }
    public Long getUpdateTime() { return updateTime; }
    public void setUpdateTime(Long updateTime) { this.updateTime = updateTime; }
}