package com.msp.scheduler.service;

import com.msp.common.core.DataSource;
import com.msp.common.core.Page;

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
     * 创建模拟节点（演示用）
     * 在数据库中创建节点数据库、示例数据表
     * @param nodeId 节点ID
     * @param dbName 数据库名
     * @param tableName 表名
     * @param columnName 匹配用列名
     * @return 创建成功返回true
     */
    boolean createSimulatedNode(String nodeId, String dbName, String tableName, String columnName);

    /**
     * 删除模拟节点（演示用）
     * @param nodeId 节点ID
     * @return 删除成功返回true
     */
    boolean deleteSimulatedNode(String nodeId);

    /**
     * 获取模拟节点示例数据
     * @param dbName 数据库名
     * @param tableName 表名
     * @return 示例数据列表（每行是一个列表）
     */
    List<List<Object>> getSimulatedNodeSampleData(String dbName, String tableName);
}