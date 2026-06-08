package com.msp.common.core;

import java.util.Map;

/**
 * 系统配置实体
 */
public class SystemConfig {
    private Long id;
    private String category;        // platform, security, computing, notification
    private String configKey;
    private String configValue;
    private String description;
    private Long createTime;
    private Long updateTime;

    // 平台信息配置
    private String platformName;
    private String platformDescription;
    private String supportEmail;
    private String contactPhone;

    // 安全配置
    private Integer jwtExpiration;      // JWT有效期(分钟)
    private Integer minPasswordLength;
    private Integer sessionTimeout;     // 会话超时(分钟)
    private Boolean passwordStrengthCheck;
    private Boolean loginFailLock;
    private Integer maxLoginAttempts;

    // 计算配置
    private String psiDefaultAlgorithm;
    private Integer psiMaxResultSize;
    private String flDefaultAggregation;
    private Integer flDefaultRounds;
    private Integer flRoundTimeout;
    private String mpcProtocol;
    private Integer mpcConcurrency;

    // 通知配置
    private Boolean taskCompleteNotify;
    private Boolean taskFailNotify;
    private Boolean nodeOfflineNotify;
    private String notifyChannels;      // email,sms,webhook
    private String webhookUrl;

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String getConfigKey() { return configKey; }
    public void setConfigKey(String configKey) { this.configKey = configKey; }

    public String getConfigValue() { return configValue; }
    public void setConfigValue(String configValue) { this.configValue = configValue; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public Long getCreateTime() { return createTime; }
    public void setCreateTime(Long createTime) { this.createTime = createTime; }

    public Long getUpdateTime() { return updateTime; }
    public void setUpdateTime(Long updateTime) { this.updateTime = updateTime; }

    public String getPlatformName() { return platformName; }
    public void setPlatformName(String platformName) { this.platformName = platformName; }

    public String getPlatformDescription() { return platformDescription; }
    public void setPlatformDescription(String platformDescription) { this.platformDescription = platformDescription; }

    public String getSupportEmail() { return supportEmail; }
    public void setSupportEmail(String supportEmail) { this.supportEmail = supportEmail; }

    public String getContactPhone() { return contactPhone; }
    public void setContactPhone(String contactPhone) { this.contactPhone = contactPhone; }

    public Integer getJwtExpiration() { return jwtExpiration; }
    public void setJwtExpiration(Integer jwtExpiration) { this.jwtExpiration = jwtExpiration; }

    public Integer getMinPasswordLength() { return minPasswordLength; }
    public void setMinPasswordLength(Integer minPasswordLength) { this.minPasswordLength = minPasswordLength; }

    public Integer getSessionTimeout() { return sessionTimeout; }
    public void setSessionTimeout(Integer sessionTimeout) { this.sessionTimeout = sessionTimeout; }

    public Boolean getPasswordStrengthCheck() { return passwordStrengthCheck; }
    public void setPasswordStrengthCheck(Boolean passwordStrengthCheck) { this.passwordStrengthCheck = passwordStrengthCheck; }

    public Boolean getLoginFailLock() { return loginFailLock; }
    public void setLoginFailLock(Boolean loginFailLock) { this.loginFailLock = loginFailLock; }

    public Integer getMaxLoginAttempts() { return maxLoginAttempts; }
    public void setMaxLoginAttempts(Integer maxLoginAttempts) { this.maxLoginAttempts = maxLoginAttempts; }

    public String getPsiDefaultAlgorithm() { return psiDefaultAlgorithm; }
    public void setPsiDefaultAlgorithm(String psiDefaultAlgorithm) { this.psiDefaultAlgorithm = psiDefaultAlgorithm; }

    public Integer getPsiMaxResultSize() { return psiMaxResultSize; }
    public void setPsiMaxResultSize(Integer psiMaxResultSize) { this.psiMaxResultSize = psiMaxResultSize; }

    public String getFlDefaultAggregation() { return flDefaultAggregation; }
    public void setFlDefaultAggregation(String flDefaultAggregation) { this.flDefaultAggregation = flDefaultAggregation; }

    public Integer getFlDefaultRounds() { return flDefaultRounds; }
    public void setFlDefaultRounds(Integer flDefaultRounds) { this.flDefaultRounds = flDefaultRounds; }

    public Integer getFlRoundTimeout() { return flRoundTimeout; }
    public void setFlRoundTimeout(Integer flRoundTimeout) { this.flRoundTimeout = flRoundTimeout; }

    public String getMpcProtocol() { return mpcProtocol; }
    public void setMpcProtocol(String mpcProtocol) { this.mpcProtocol = mpcProtocol; }

    public Integer getMpcConcurrency() { return mpcConcurrency; }
    public void setMpcConcurrency(Integer mpcConcurrency) { this.mpcConcurrency = mpcConcurrency; }

    public Boolean getTaskCompleteNotify() { return taskCompleteNotify; }
    public void setTaskCompleteNotify(Boolean taskCompleteNotify) { this.taskCompleteNotify = taskCompleteNotify; }

    public Boolean getTaskFailNotify() { return taskFailNotify; }
    public void setTaskFailNotify(Boolean taskFailNotify) { this.taskFailNotify = taskFailNotify; }

    public Boolean getNodeOfflineNotify() { return nodeOfflineNotify; }
    public void setNodeOfflineNotify(Boolean nodeOfflineNotify) { this.nodeOfflineNotify = nodeOfflineNotify; }

    public String getNotifyChannels() { return notifyChannels; }
    public void setNotifyChannels(String notifyChannels) { this.notifyChannels = notifyChannels; }

    public String getWebhookUrl() { return webhookUrl; }
    public void setWebhookUrl(String webhookUrl) { this.webhookUrl = webhookUrl; }
}