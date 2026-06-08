package com.msp.scheduler.service;

import com.msp.common.core.ColumnInfo;
import com.msp.common.core.DataSource;
import com.msp.common.core.Page;
import com.msp.common.core.TableInfo;

import java.util.List;

/**
 * 数据源服务接口
 */
public interface DataSourceService {

    /**
     * 创建数据源
     * @param dataSource 数据源
     * @return 数据源ID
     */
    String createDataSource(DataSource dataSource);

    /**
     * 更新数据源
     * @param dataSource 数据源
     */
    void updateDataSource(DataSource dataSource);

    /**
     * 删除数据源
     * @param datasourceId 数据源ID
     */
    void deleteDataSource(String datasourceId);

    /**
     * 获取数据源详情
     * @param datasourceId 数据源ID
     * @return 数据源
     */
    DataSource getDataSource(String datasourceId);

    /**
     * 查询节点下的数据源列表
     * @param nodeId 节点ID
     * @return 数据源列表
     */
    List<DataSource> getDataSourcesByNodeId(String nodeId);

    /**
     * 分页查询数据源列表
     * @param type 数据源类型
     * @param nodeId 节点ID
     * @param page 页码
     * @param size 每页大小
     * @return 分页结果
     */
    Page<DataSource> listDataSources(DataSource.DataSourceType type, String nodeId, int page, int size);

    /**
     * 测试数据源连接
     * @param dataSource 数据源
     * @return 是否连接成功
     */
    boolean testConnection(DataSource dataSource);

    /**
     * 获取示例数据
     * @param dbName 数据库名
     * @param tableName 表名
     * @return 示例数据列表（每行是一个列表）
     */
    List<List<Object>> getSampleData(String dbName, String tableName);

    /**
     * 获取数据源的所有表（含表名 + 表注释，从 INFORMATION_SCHEMA 实时读取）
     * @param datasourceId 数据源ID
     * @return 表信息列表
     */
    List<TableInfo> getDataSourceTables(String datasourceId);

    /**
     * 获取数据源的表字段信息（含字段名 + 字段注释，从 INFORMATION_SCHEMA 实时读取）
     * @param datasourceId 数据源ID
     * @param tableName 表名
     * @return 字段信息列表
     */
    List<ColumnInfo> getDataSourceColumns(String datasourceId, String tableName);
}