package com.msp.scheduler.service;

import com.msp.common.core.Task;
import com.msp.common.core.TaskStatus;
import com.msp.scheduler.repository.TaskRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 任务恢复服务
 * 负责检测任务异常并尝试自动恢复失败的任务
 */
@Service
public class TaskRecoveryService {

    private static final Logger log = LoggerFactory.getLogger(TaskRecoveryService.class);

    private final TaskRepository taskRepository;
    private final TaskScheduler taskScheduler;

    // 记录任务重试次数，防止无限重试
    private final Map<String, Integer> taskRetryCount = new ConcurrentHashMap<>();

    // 最大重试次数
    private static final int MAX_RETRY_COUNT = 3;

    // 任务超时时间（毫秒）- 默认30分钟
    @Value("${task.timeout-ms:1800000}")
    private long taskTimeoutMs;

    // 是否启用自动恢复
    @Value("${task.recovery.enabled:true}")
    private boolean recoveryEnabled;

    public TaskRecoveryService(TaskRepository taskRepository, TaskScheduler taskScheduler) {
        this.taskRepository = taskRepository;
        this.taskScheduler = taskScheduler;
    }

    /**
     * 检测并恢复超时或失败的任务
     */
    public void detectAndRecoverTasks() {
        if (!recoveryEnabled) {
            return;
        }

        log.debug("Starting task recovery check");

        try {
            // 查找超时或失败的任务
            List<Task> tasksToRecover = taskRepository.findTasksNeedingRecovery(taskTimeoutMs);

            for (Task task : tasksToRecover) {
                try {
                    recoverTask(task);
                } catch (Exception e) {
                    log.error("Error recovering task {}: {}", task.getTaskId(), e.getMessage());
                }
            }

            if (!tasksToRecover.isEmpty()) {
                log.info("Task recovery check complete: {} tasks processed", tasksToRecover.size());
            }

        } catch (Exception e) {
            log.error("Error in task recovery check", e);
        }
    }

    /**
     * 恢复单个任务
     */
    private void recoverTask(Task task) {
        String taskId = task.getTaskId();
        int currentRetryCount = taskRetryCount.getOrDefault(taskId, 0);

        if (currentRetryCount >= MAX_RETRY_COUNT) {
            log.warn("Task {} has exceeded max retry count ({}), manual intervention required",
                    taskId, MAX_RETRY_COUNT);
            return;
        }

        TaskStatus status = task.getStatus();

        if (status == TaskStatus.FAILED) {
            log.info("Attempting to recover failed task: {}", taskId);
            retryTask(taskId);
        } else if (status == TaskStatus.RUNNING || status == TaskStatus.PENDING) {
            // 检查是否超时
            long elapsed = System.currentTimeMillis() - task.getUpdateTime();
            if (elapsed > taskTimeoutMs) {
                log.info("Task {} appears to be stuck (elapsed {}ms > {}ms), marking as failed",
                        taskId, elapsed, taskTimeoutMs);
                taskRepository.updateStatus(taskId, TaskStatus.FAILED);
                handleTaskFailure(taskId, "Task timeout");
            }
        }
    }

    /**
     * 重试任务
     */
    private void retryTask(String taskId) {
        int currentRetryCount = taskRetryCount.getOrDefault(taskId, 0);
        taskRetryCount.put(taskId, currentRetryCount + 1);

        log.info("Retrying task {} (attempt {}/{})", taskId, currentRetryCount + 1, MAX_RETRY_COUNT);

        try {
            // 调用任务调度器重试
            String newTaskId = taskScheduler.retryTask(taskId);
            log.info("Task {} retry scheduled, new task ID: {}", taskId, newTaskId);
        } catch (Exception e) {
            log.error("Failed to retry task {}: {}", taskId, e.getMessage());
        }
    }

    /**
     * 处理任务失败
     */
    private void handleTaskFailure(String taskId, String reason) {
        log.warn("Task {} failed: {}", taskId, reason);
        taskRetryCount.remove(taskId); // 清理重试计数
    }

    /**
     * 获取任务重试次数
     */
    public int getRetryCount(String taskId) {
        return taskRetryCount.getOrDefault(taskId, 0);
    }

    /**
     * 清理任务重试计数（任务成功时调用）
     */
    public void clearRetryCount(String taskId) {
        taskRetryCount.remove(taskId);
    }
}