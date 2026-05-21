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