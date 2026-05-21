package com.msp.scheduler.controller;

import com.msp.common.core.ApiResponse;
import com.msp.common.core.AuditLog;
import com.msp.common.core.Page;
import com.msp.scheduler.service.AuditLogService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 审计日志REST接口
 */
@RestController
@RequestMapping("/api/v1/msp/audit-logs")
public class AuditLogController {

    private final AuditLogService auditLogService;

    public AuditLogController(AuditLogService auditLogService) {
        this.auditLogService = auditLogService;
    }

    /**
     * 查询审计日志列表
     */
    @GetMapping
    public ApiResponse<Page<AuditLog>> listAuditLogs(
            @RequestParam(value = "userId", required = false) String userId,
            @RequestParam(value = "action", required = false) String action,
            @RequestParam(value = "resourceType", required = false) String resourceType,
            @RequestParam(value = "startTime", required = false) Long startTime,
            @RequestParam(value = "endTime", required = false) Long endTime,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "20") int size) {

        long start = startTime != null ? startTime : 0;
        long end = endTime != null ? endTime : 0;

        Page<AuditLog> result = auditLogService.listLogs(userId, action, resourceType, start, end, page, size);
        return ApiResponse.success(result);
    }

    /**
     * 记录审计日志
     */
    @PostMapping
    public ApiResponse<Boolean> createAuditLog(@RequestBody AuditLogRequest request) {
        auditLogService.log(
            request.getUserId(),
            request.getAction(),
            request.getResourceType(),
            request.getResourceId(),
            request.getDetails(),
            request.getIpAddress()
        );
        return ApiResponse.success(true);
    }

    /**
     * 导出审计日志 (CSV格式)
     */
    @GetMapping("/export")
    public ApiResponse<List<AuditLog>> exportAuditLogs(
            @RequestParam(value = "userId", required = false) String userId,
            @RequestParam(value = "action", required = false) String action,
            @RequestParam(value = "resourceType", required = false) String resourceType,
            @RequestParam(value = "startTime", required = false) Long startTime,
            @RequestParam(value = "endTime", required = false) Long endTime) {

        long start = startTime != null ? startTime : 0;
        long end = endTime != null ? endTime : 0;

        // 导出限制10000条
        Page<AuditLog> result = auditLogService.listLogs(userId, action, resourceType, start, end, 0, 10000);
        return ApiResponse.success(result.getContent());
    }

    // 内部类
    public static class AuditLogRequest {
        private String userId;
        private String action;
        private String resourceType;
        private String resourceId;
        private Object details;
        private String ipAddress;

        public String getUserId() { return userId; }
        public void setUserId(String userId) { this.userId = userId; }
        public String getAction() { return action; }
        public void setAction(String action) { this.action = action; }
        public String getResourceType() { return resourceType; }
        public void setResourceType(String resourceType) { this.resourceType = resourceType; }
        public String getResourceId() { return resourceId; }
        public void setResourceId(String resourceId) { this.resourceId = resourceId; }
        public Object getDetails() { return details; }
        public void setDetails(Object details) { this.details = details; }
        public String getIpAddress() { return ipAddress; }
        public void setIpAddress(String ipAddress) { this.ipAddress = ipAddress; }
    }
}