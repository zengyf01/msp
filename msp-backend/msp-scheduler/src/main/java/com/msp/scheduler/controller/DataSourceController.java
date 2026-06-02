package com.msp.scheduler.controller;

import com.msp.common.core.ApiResponse;
import com.msp.common.core.DataSource;
import com.msp.common.core.Page;
import com.msp.scheduler.service.DataSourceService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 数据源管理REST接口
 */
@RestController
@RequestMapping("/api/v1/msp/datasources")
public class DataSourceController {

    private final DataSourceService dataSourceService;

    public DataSourceController(DataSourceService dataSourceService) {
        this.dataSourceService = dataSourceService;
    }

    /**
     * 创建数据源
     */
    @PostMapping
    public ApiResponse<DataSourceCreateResponse> createDataSource(@RequestBody DataSourceRequest request) {
        DataSource ds = new DataSource();
        ds.setNodeId(request.getNodeId());
        ds.setName(request.getName());
        ds.setType(request.getType());
        ds.setHost(request.getHost());
        ds.setPort(request.getPort());
        ds.setDatabase(request.getDatabase());
        ds.setTableName(request.getTableName());
        ds.setColumns(request.getColumns());

        String datasourceId = dataSourceService.createDataSource(ds);
        return ApiResponse.success(new DataSourceCreateResponse(datasourceId));
    }

    /**
     * 查询数据源列表
     */
    @GetMapping
    public ApiResponse<Page<DataSource>> listDataSources(
            @RequestParam(value = "type", required = false) DataSource.DataSourceType type,
            @RequestParam(value = "nodeId", required = false) String nodeId,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "20") int size) {
        Page<DataSource> result = dataSourceService.listDataSources(type, nodeId, page, size);
        return ApiResponse.success(result);
    }

    /**
     * 查询数据源详情
     */
    @GetMapping("/{datasourceId}")
    public ApiResponse<DataSource> getDataSource(@PathVariable(name = "datasourceId") String datasourceId) {
        DataSource ds = dataSourceService.getDataSource(datasourceId);
        return ApiResponse.success(ds);
    }

    /**
     * 更新数据源
     */
    @PutMapping("/{datasourceId}")
    public ApiResponse<Boolean> updateDataSource(@PathVariable(name = "datasourceId") String datasourceId, @RequestBody DataSourceRequest request) {
        DataSource ds = new DataSource();
        ds.setDataSourceId(datasourceId);
        ds.setNodeId(request.getNodeId());
        ds.setName(request.getName());
        ds.setType(request.getType());
        ds.setHost(request.getHost());
        ds.setPort(request.getPort());
        ds.setDatabase(request.getDatabase());
        ds.setTableName(request.getTableName());
        ds.setColumns(request.getColumns());

        dataSourceService.updateDataSource(ds);
        return ApiResponse.success(true);
    }

    /**
     * 删除数据源
     */
    @DeleteMapping("/{datasourceId}")
    public ApiResponse<Boolean> deleteDataSource(@PathVariable(name = "datasourceId") String datasourceId) {
        dataSourceService.deleteDataSource(datasourceId);
        return ApiResponse.success(true);
    }

    /**
     * 测试数据源连接
     */
    @PostMapping("/test-connection")
    public ApiResponse<ConnectionTestResponse> testConnection(@RequestBody DataSourceRequest request) {
        DataSource ds = new DataSource();
        ds.setType(request.getType());
        ds.setHost(request.getHost());
        ds.setPort(request.getPort());
        ds.setDatabase(request.getDatabase());

        boolean success = dataSourceService.testConnection(ds);
        return ApiResponse.success(new ConnectionTestResponse(success, success ? "连接成功" : "连接失败"));
    }

    /**
     * 查询节点下的数据源
     */
    @GetMapping("/by-node/{nodeId}")
    public ApiResponse<List<DataSource>> getDataSourcesByNode(@PathVariable(name = "nodeId") String nodeId) {
        List<DataSource> result = dataSourceService.getDataSourcesByNodeId(nodeId);
        return ApiResponse.success(result);
    }

    /**
     * 获取数据源的所有表名
     */
    @GetMapping("/{datasourceId}/tables")
    public ApiResponse<List<String>> getDataSourceTables(@PathVariable(name = "datasourceId") String datasourceId) {
        List<String> tables = dataSourceService.getDataSourceTables(datasourceId);
        return ApiResponse.success(tables);
    }

    /**
     * 获取数据源的表字段信息
     */
    @GetMapping("/{datasourceId}/columns")
    public ApiResponse<List<String>> getDataSourceColumns(
            @PathVariable(name = "datasourceId") String datasourceId,
            @RequestParam(name = "tableName") String tableName) {
        List<String> columns = dataSourceService.getDataSourceColumns(datasourceId, tableName);
        return ApiResponse.success(columns);
    }

    /**
     * 创建模拟节点（演示用）
     */
    @PostMapping("/simulate")
    public ApiResponse<Boolean> createSimulatedNode(@RequestBody SimulateNodeRequest request) {
        boolean success = dataSourceService.createSimulatedNode(
            request.getNodeId(),
            request.getDbName(),
            request.getTableName(),
            request.getColumnName()
        );
        return ApiResponse.success(success);
    }

    /**
     * 删除模拟节点（演示用）
     */
    @DeleteMapping("/simulate/{nodeId}")
    public ApiResponse<Boolean> deleteSimulatedNode(@PathVariable(name = "nodeId") String nodeId) {
        boolean success = dataSourceService.deleteSimulatedNode(nodeId);
        return ApiResponse.success(success);
    }

    /**
     * 获取模拟节点示例数据
     */
    @PostMapping("/simulate/sample-data")
    public ApiResponse<List<List<Object>>> getSimulatedNodeSampleData(@RequestBody SampleDataRequest request) {
        List<List<Object>> data = dataSourceService.getSimulatedNodeSampleData(
            request.getDbName(),
            request.getTableName()
        );
        return ApiResponse.success(data);
    }

    // 内部类
    public static class DataSourceRequest {
        private String nodeId;
        private String name;
        private DataSource.DataSourceType type;
        private String host;
        private Integer port;
        private String database;
        private String tableName;
        private List<String> columns;

        public String getNodeId() { return nodeId; }
        public void setNodeId(String nodeId) { this.nodeId = nodeId; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public DataSource.DataSourceType getType() { return type; }
        public void setType(DataSource.DataSourceType type) { this.type = type; }
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
    }

    public static class SimulateNodeRequest {
        private String nodeId;
        private String dbName;
        private String tableName;
        private String columnName;

        public String getNodeId() { return nodeId; }
        public void setNodeId(String nodeId) { this.nodeId = nodeId; }
        public String getDbName() { return dbName; }
        public void setDbName(String dbName) { this.dbName = dbName; }
        public String getTableName() { return tableName; }
        public void setTableName(String tableName) { this.tableName = tableName; }
        public String getColumnName() { return columnName; }
        public void setColumnName(String columnName) { this.columnName = columnName; }
    }

    public static class DataSourceCreateResponse {
        private String datasourceId;

        public DataSourceCreateResponse(String datasourceId) {
            this.datasourceId = datasourceId;
        }

        public String getDatasourceId() { return datasourceId; }
    }

    public static class ConnectionTestResponse {
        private boolean success;
        private String message;

        public ConnectionTestResponse(boolean success, String message) {
            this.success = success;
            this.message = message;
        }

        public boolean isSuccess() { return success; }
        public String getMessage() { return message; }
    }

    public static class SampleDataRequest {
        private String dbName;
        private String tableName;

        public String getDbName() { return dbName; }
        public void setDbName(String dbName) { this.dbName = dbName; }
        public String getTableName() { return tableName; }
        public void setTableName(String tableName) { this.tableName = tableName; }
    }
}