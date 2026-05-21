package com.msp.scheduler.service;

import com.msp.common.core.SystemConfig;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 系统配置服务
 */
@Service
public class SystemConfigService {

    // 内存存储，实际生产应使用数据库
    private final Map<String, SystemConfig> configStore = new ConcurrentHashMap<>();

    public SystemConfigService() {
        // 初始化默认配置
        initDefaultConfig();
    }

    private void initDefaultConfig() {
        SystemConfig platform = new SystemConfig();
        platform.setCategory("platform");
        platform.setPlatformName("密算平台");
        platform.setPlatformDescription("可信数据空间的隐私计算平台");
        platform.setSupportEmail("support@msp.com");
        platform.setContactPhone("400-123-4567");
        configStore.put("platform", platform);

        SystemConfig security = new SystemConfig();
        security.setCategory("security");
        security.setJwtExpiration(60);
        security.setMinPasswordLength(8);
        security.setSessionTimeout(30);
        security.setPasswordStrengthCheck(true);
        security.setLoginFailLock(true);
        security.setMaxLoginAttempts(5);
        configStore.put("security", security);

        SystemConfig computing = new SystemConfig();
        computing.setCategory("computing");
        computing.setPsiDefaultAlgorithm("KKRT");
        computing.setPsiMaxResultSize(100000);
        computing.setFlDefaultAggregation("FED_AVG");
        computing.setFlDefaultRounds(100);
        computing.setFlRoundTimeout(300);
        computing.setMpcProtocol("ABY3");
        computing.setMpcConcurrency(4);
        configStore.put("computing", computing);

        SystemConfig notification = new SystemConfig();
        notification.setCategory("notification");
        notification.setTaskCompleteNotify(true);
        notification.setTaskFailNotify(true);
        notification.setNodeOfflineNotify(true);
        notification.setNotifyChannels("email");
        configStore.put("notification", notification);
    }

    public SystemConfig getPlatformConfig() {
        return configStore.get("platform");
    }

    public SystemConfig getSecurityConfig() {
        return configStore.get("security");
    }

    public SystemConfig getComputingConfig() {
        return configStore.get("computing");
    }

    public SystemConfig getNotificationConfig() {
        return configStore.get("notification");
    }

    public SystemConfig getConfigByCategory(String category) {
        return configStore.get(category);
    }

    public void savePlatformConfig(SystemConfig config) {
        config.setCategory("platform");
        configStore.put("platform", config);
    }

    public void saveSecurityConfig(SystemConfig config) {
        config.setCategory("security");
        configStore.put("security", config);
    }

    public void saveComputingConfig(SystemConfig config) {
        config.setCategory("computing");
        configStore.put("computing", config);
    }

    public void saveNotificationConfig(SystemConfig config) {
        config.setCategory("notification");
        configStore.put("notification", config);
    }

    public Map<String, SystemConfig> getAllConfigs() {
        return Map.copyOf(configStore);
    }
}