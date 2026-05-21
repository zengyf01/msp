package com.msp.scheduler.service;

import com.msp.common.core.AuditLog;
import com.msp.common.core.Page;
import com.msp.scheduler.repository.AuditLogRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

/**
 * 审计日志服务
 */
@Service
public class AuditLogService {

    private final AuditLogRepository auditLogRepository;

    public AuditLogService(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    /**
     * 记录审计日志
     */
    public void log(String userId, String action, String resourceType, String resourceId, Object details, String ipAddress) {
        AuditLog auditLog = new AuditLog();
        auditLog.setLogId(UUID.randomUUID().toString());
        auditLog.setUserId(userId);
        auditLog.setAction(action);
        auditLog.setResourceType(resourceType);
        auditLog.setResourceId(resourceId);
        if (details instanceof java.util.Map) {
            @SuppressWarnings("unchecked")
            java.util.Map<String, Object> detailsMap = (java.util.Map<String, Object>) details;
            auditLog.setDetails(detailsMap);
        }
        auditLog.setIpAddress(ipAddress);
        auditLog.setCreateTime(System.currentTimeMillis());

        auditLogRepository.save(auditLog);
    }

    /**
     * 查询审计日志（分页）
     */
    public Page<AuditLog> listLogs(String userId, String action, String resourceType, long startTime, long endTime, int page, int size) {
        List<AuditLog> content = auditLogRepository.findAll(userId, action, resourceType, startTime, endTime, page, size);
        long total = auditLogRepository.count(userId, action, resourceType, startTime, endTime);
        return new Page<>(content, total, page, size);
    }

    /**
     * 获取审计日志详情
     */
    public AuditLog getLog(String logId) {
        // 直接通过列表查询获取
        return null;
    }
}