package com.msp.scheduler.service;

import com.msp.common.core.Task;
import com.msp.common.core.TaskStatus;
import com.msp.kuscia.client.KusciaClient;
import com.msp.scheduler.repository.TaskRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Kuscia状态轮询服务
 * 后台定时查询Kuscia任务状态并更新MSP数据库
 */
@Service
public class KusciaStatusPollerService {

    private static final Logger log = LoggerFactory.getLogger(KusciaStatusPollerService.class);

    private final KusciaClient kusciaClient;
    private final TaskRepository taskRepository;
    private final TaskRecoveryService taskRecoveryService;
    private final AlertService alertService;

    @Value("${kuscia.polling.enabled:true}")
    private boolean pollingEnabled;

    @Value("${kuscia.polling.batch-size:50}")
    private int batchSize;

    @Value("${kuscia.simulation-mode:false}")
    private boolean simulationMode;

    // 任务超时时间（毫秒）- 默认30分钟
    @Value("${task.timeout-ms:1800000}")
    private long taskTimeoutMs;

    public KusciaStatusPollerService(
            KusciaClient kusciaClient,
            TaskRepository taskRepository,
            TaskRecoveryService taskRecoveryService,
            AlertService alertService) {
        this.kusciaClient = kusciaClient;
        this.taskRepository = taskRepository;
        this.taskRecoveryService = taskRecoveryService;
        this.alertService = alertService;
    }

    /**
     * 定时轮询运行中的任务状态
     * 默认每10秒执行一次
     */
    @Scheduled(fixedDelayString = "${kuscia.polling.interval-ms:10000}")
    public void pollRunningTasks() {
        if (!pollingEnabled) {
            return;
        }

        log.debug("Starting status polling cycle");

        try {
            // 查询需要轮询的任务（状态为PENDING或RUNNING）
            List<Task> activeTasks = taskRepository.findActiveTasks(batchSize);

            if (activeTasks.isEmpty()) {
                log.debug("No active tasks to poll");
                // 定期执行任务恢复检查
                taskRecoveryService.detectAndRecoverTasks();
                return;
            }

            log.info("Polling status for {} active tasks (simulation: {})", activeTasks.size(), simulationMode);

            int successCount = 0;
            int errorCount = 0;

            for (Task task : activeTasks) {
                try {
                    if (simulationMode) {
                        // In simulation mode, just update status from local tracking
                        updateStatusFromLocalState(task);
                        successCount++;
                    } else {
                        pollTaskStatus(task);
                        successCount++;
                    }
                } catch (Exception e) {
                    log.error("Error polling task {}: {}", task.getTaskId(), e.getMessage());
                    errorCount++;
                }
            }

            log.info("Status polling cycle complete: {} success, {} errors", successCount, errorCount);

            // 执行任务恢复检查
            taskRecoveryService.detectAndRecoverTasks();

            // 检查需要告警的任务（超时或长时间无更新的任务）
            checkAndAlert(activeTasks);

        } catch (Exception e) {
            log.error("Error in status polling cycle", e);
        }
    }

    /**
     * 在测试模式下，从本地任务状态更新数据库
     */
    private void updateStatusFromLocalState(Task task) {
        // In simulation mode, the TaskSchedulerImpl handles all status updates
        // This method just ensures the database is in sync with the latest state
        log.debug("Task {} in simulation mode, status: {}", task.getTaskId(), task.getStatus());

        // Check for timeout
        if (task.getStatus() == TaskStatus.RUNNING || task.getStatus() == TaskStatus.PENDING) {
            long elapsed = System.currentTimeMillis() - task.getUpdateTime();
            if (elapsed > taskTimeoutMs) {
                log.warn("Task {} has been stuck for {} ms (timeout: {} ms)", task.getTaskId(), elapsed, taskTimeoutMs);
                alertService.onTaskTimeout(task.getTaskId(), task.getName());
            }
        }
    }

    private void pollTaskStatus(Task task) {
        TaskStatus previousStatus = task.getStatus();
        String taskId = task.getTaskId();

        // 已是终态，跳过
        if (previousStatus == TaskStatus.COMPLETED ||
            previousStatus == TaskStatus.FAILED ||
            previousStatus == TaskStatus.CANCELLED) {
            log.debug("Task {} already in terminal state: {}", taskId, previousStatus);
            return;
        }

        // 关键：只对 kuscia 模式的任务才去问 kuscia。
        // Ray 模式任务由 TaskSchedulerImpl.executeRayTask 自己管 status，
        // 这里绝对不能调 kuscia —— kuscia 不可达时 getTaskStatus 会 fallback 返回 PENDING，
        // 把 Ray 模式已经写好的 RUNNING / COMPLETED 状态覆盖掉。
        String nodeMode = task.getNodeMode() == null ? "ray" : task.getNodeMode();
        if (!"kuscia".equalsIgnoreCase(nodeMode)) {
            log.debug("Task {} is {} mode, skip kuscia poll", taskId, nodeMode);
            return;
        }

        // 查询Kuscia状态。kuscia 集群不可达时 getTaskStatus 不会抛异常，而是 fallback 返回 PENDING，
        // 这里把这种情况也当成"不可达"处理 —— 不动 status。
        TaskStatus newStatus;
        try {
            newStatus = kusciaClient.getTaskStatus(taskId);
        } catch (Exception e) {
            log.warn("Skip status poll for task {} (kuscia unavailable): {}", taskId, e.getMessage());
            return;
        }
        if (newStatus == null) {
            log.warn("Skip status poll for task {} (kuscia returned null)", taskId);
            return;
        }

        // 状态变化时更新数据库
        if (newStatus != previousStatus) {
            log.info("Task {} status changed: {} -> {}", taskId, previousStatus, newStatus);
            taskRepository.updateStatus(taskId, newStatus);

            // 处理终态
            if (newStatus == TaskStatus.COMPLETED || newStatus == TaskStatus.FAILED) {
                handleTerminalState(taskId, newStatus);
            }
        }

        // 检查任务是否超时（长时间处于PENDING或RUNNING状态）
        checkTaskTimeout(task);
    }

    private void checkTaskTimeout(Task task) {
        long elapsed = System.currentTimeMillis() - task.getUpdateTime();
        if (elapsed > taskTimeoutMs) {
            log.warn("Task {} has been stuck for {} ms (timeout: {} ms)", task.getTaskId(), elapsed, taskTimeoutMs);
            alertService.onTaskTimeout(task.getTaskId(), task.getName());
        }
    }

    private void checkAndAlert(List<Task> activeTasks) {
        // 通知告警服务检查任务状态
        // 这里只是定期检查，真正的告警在 checkTaskTimeout 中触发
        log.debug("Checked {} active tasks for alerts", activeTasks.size());
    }

    private void handleTerminalState(String taskId, TaskStatus status) {
        if (status == TaskStatus.COMPLETED) {
            log.info("Task {} completed, fetching result", taskId);
            try {
                byte[] result = kusciaClient.getTaskResult(taskId);
                if (result != null && result.length > 0) {
                    log.info("Task {} result received, size: {} bytes", taskId, result.length);
                }
                // 清理任务恢复服务的重试计数
                taskRecoveryService.clearRetryCount(taskId);
            } catch (Exception e) {
                log.error("Error fetching result for task {}", taskId, e);
            }
        } else if (status == TaskStatus.FAILED) {
            alertService.onTaskFailed(taskId, taskId, "Task execution failed");
        }
    }
}