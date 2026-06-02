package com.msp.scheduler.service;

import com.msp.common.core.Task;
import com.msp.common.core.TaskStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 告警服务
 * 负责检测异常情况并发送告警通知
 */
@Service
public class AlertService {

    private static final Logger log = LoggerFactory.getLogger(AlertService.class);

    // 告警级别
    public enum AlertLevel {
        INFO,
        WARNING,
        ERROR,
        CRITICAL
    }

    // 告警类型
    public enum AlertType {
        TASK_FAILED,
        TASK_TIMEOUT,
        NODE_OFFLINE,
        NODE_ERROR,
        SYSTEM_ERROR
    }

    // 告警记录
    private final Map<String, AlertRecord> alertHistory = new ConcurrentHashMap<>();

    // 是否启用告警
    @Value("${alert.enabled:true}")
    private boolean alertEnabled;

    // 告警阈值配置
    @Value("${alert.task-timeout-minutes:30}")
    private int taskTimeoutMinutes;

    @Value("${alert.max-failed-tasks:10}")
    private int maxFailedTasks;

    @Value("${alert.check-interval-seconds:60}")
    private int checkIntervalSeconds;

    public AlertService() {
    }

    /**
     * 检测并生成告警
     */
    public List<AlertRecord> checkAndGenerateAlerts(List<Task> activeTasks, Map<String, TaskStatus> nodeStatuses) {
        if (!alertEnabled) {
            return List.of();
        }

        List<AlertRecord> alerts = new ArrayList<>();

        // 检查任务超时
        for (Task task : activeTasks) {
            if (isTaskTimedOut(task)) {
                alerts.add(createAlert(AlertType.TASK_TIMEOUT, AlertLevel.WARNING,
                        "Task " + task.getTaskId() + " (" + task.getName() + ") has timed out",
                        task.getTaskId()));
            }
        }

        // 检查节点状态
        for (Map.Entry<String, TaskStatus> entry : nodeStatuses.entrySet()) {
            if (entry.getValue() == TaskStatus.FAILED || entry.getValue() == TaskStatus.CANCELLED) {
                alerts.add(createAlert(AlertType.NODE_ERROR, AlertLevel.ERROR,
                        "Node " + entry.getKey() + " is in error state",
                        entry.getKey()));
            }
        }

        // 记录告警历史
        for (AlertRecord alert : alerts) {
            alertHistory.put(alert.getAlertId(), alert);
        }

        return alerts;
    }

    /**
     * 处理任务失败告警
     */
    public void onTaskFailed(String taskId, String taskName, String reason) {
        if (!alertEnabled) {
            return;
        }

        AlertRecord alert = createAlert(
                AlertType.TASK_FAILED,
                AlertLevel.ERROR,
                "Task " + taskName + " (" + taskId + ") failed: " + reason,
                taskId
        );

        alertHistory.put(alert.getAlertId(), alert);
        log.warn("ALERT: Task failed - {} ({})", taskName, taskId);

        // 发送通知（这里只是记录，实际实现可以通过邮件、短信等发送）
        sendNotification(alert);
    }

    /**
     * 处理任务超时告警
     */
    public void onTaskTimeout(String taskId, String taskName) {
        if (!alertEnabled) {
            return;
        }

        AlertRecord alert = createAlert(
                AlertType.TASK_TIMEOUT,
                AlertLevel.WARNING,
                "Task " + taskName + " (" + taskId + ") has timed out after " + taskTimeoutMinutes + " minutes",
                taskId
        );

        alertHistory.put(alert.getAlertId(), alert);
        log.warn("ALERT: Task timeout - {} ({})", taskName, taskId);

        sendNotification(alert);
    }

    /**
     * 处理节点掉线告警
     */
    public void onNodeOffline(String nodeId, String nodeName) {
        if (!alertEnabled) {
            return;
        }

        AlertRecord alert = createAlert(
                AlertType.NODE_OFFLINE,
                AlertLevel.CRITICAL,
                "Node " + nodeName + " (" + nodeId + ") is offline",
                nodeId
        );

        alertHistory.put(alert.getAlertId(), alert);
        log.error("ALERT: Node offline - {} ({})", nodeName, nodeId);

        sendNotification(alert);
    }

    /**
     * 发送通知（实际实现可以对接邮件、短信、Webhook等）
     */
    private void sendNotification(AlertRecord alert) {
        log.info("Sending alert notification: [{}] {} - {}",
                alert.getLevel(), alert.getType(), alert.getMessage());

        // 实际实现可以包括:
        // - 发送邮件
        // - 发送短信
        // - 发送 webhook
        // - 发送钉钉/飞书消息
    }

    /**
     * 检查任务是否超时
     */
    private boolean isTaskTimedOut(Task task) {
        long elapsed = System.currentTimeMillis() - task.getUpdateTime();
        return elapsed > (taskTimeoutMinutes * 60 * 1000L);
    }

    /**
     * 创建告警记录
     */
    private AlertRecord createAlert(AlertType type, AlertLevel level, String message, String resourceId) {
        return new AlertRecord(type, level, message, resourceId);
    }

    /**
     * 获取告警历史
     */
    public List<AlertRecord> getAlertHistory(int limit) {
        return alertHistory.values().stream()
                .sorted((a, b) -> Long.compare(b.getTimestamp(), a.getTimestamp()))
                .limit(limit)
                .toList();
    }

    /**
     * 清除已解决的告警
     */
    public void clearResolvedAlerts() {
        alertHistory.entrySet().removeIf(entry -> entry.getValue().isResolved());
    }

    /**
     * 告警记录
     */
    public static class AlertRecord {
        private final String alertId;
        private final AlertType type;
        private final AlertLevel level;
        private final String message;
        private final String resourceId;
        private final long timestamp;
        private boolean resolved;

        public AlertRecord(AlertType type, AlertLevel level, String message, String resourceId) {
            this.alertId = java.util.UUID.randomUUID().toString();
            this.type = type;
            this.level = level;
            this.message = message;
            this.resourceId = resourceId;
            this.timestamp = System.currentTimeMillis();
            this.resolved = false;
        }

        public String getAlertId() { return alertId; }
        public AlertType getType() { return type; }
        public AlertLevel getLevel() { return level; }
        public String getMessage() { return message; }
        public String getResourceId() { return resourceId; }
        public long getTimestamp() { return timestamp; }
        public boolean isResolved() { return resolved; }
        public void setResolved(boolean resolved) { this.resolved = resolved; }
    }
}