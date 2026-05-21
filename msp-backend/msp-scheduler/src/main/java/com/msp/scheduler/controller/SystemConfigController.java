package com.msp.scheduler.controller;

import com.msp.common.core.ApiResponse;
import com.msp.common.core.SystemConfig;
import com.msp.scheduler.service.SystemConfigService;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 系统配置REST接口
 */
@RestController
@RequestMapping("/api/v1/msp/system-config")
public class SystemConfigController {

    private final SystemConfigService systemConfigService;

    public SystemConfigController(SystemConfigService systemConfigService) {
        this.systemConfigService = systemConfigService;
    }

    @GetMapping("/platform")
    public ApiResponse<SystemConfig> getPlatformConfig() {
        return ApiResponse.success(systemConfigService.getPlatformConfig());
    }

    @PutMapping("/platform")
    public ApiResponse<Boolean> updatePlatformConfig(@RequestBody SystemConfig config) {
        systemConfigService.savePlatformConfig(config);
        return ApiResponse.success(true);
    }

    @GetMapping("/security")
    public ApiResponse<SystemConfig> getSecurityConfig() {
        return ApiResponse.success(systemConfigService.getSecurityConfig());
    }

    @PutMapping("/security")
    public ApiResponse<Boolean> updateSecurityConfig(@RequestBody SystemConfig config) {
        systemConfigService.saveSecurityConfig(config);
        return ApiResponse.success(true);
    }

    @GetMapping("/computing")
    public ApiResponse<SystemConfig> getComputingConfig() {
        return ApiResponse.success(systemConfigService.getComputingConfig());
    }

    @PutMapping("/computing")
    public ApiResponse<Boolean> updateComputingConfig(@RequestBody SystemConfig config) {
        systemConfigService.saveComputingConfig(config);
        return ApiResponse.success(true);
    }

    @GetMapping("/notification")
    public ApiResponse<SystemConfig> getNotificationConfig() {
        return ApiResponse.success(systemConfigService.getNotificationConfig());
    }

    @PutMapping("/notification")
    public ApiResponse<Boolean> updateNotificationConfig(@RequestBody SystemConfig config) {
        systemConfigService.saveNotificationConfig(config);
        return ApiResponse.success(true);
    }

    @GetMapping
    public ApiResponse<Map<String, SystemConfig>> getAllConfigs() {
        return ApiResponse.success(systemConfigService.getAllConfigs());
    }
}