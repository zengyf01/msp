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

    @Value("${kuscia.polling.enabled:true}")
    private boolean pollingEnabled;

    @Value("${kuscia.polling.batch-size:50}")
    private int batchSize;

    public KusciaStatusPollerService(KusciaClient kusciaClient, TaskRepository taskRepository) {
        this.kusciaClient = kusciaClient;
        this.taskRepository = taskRepository;
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
                return;
            }

            log.info("Polling status for {} active tasks", activeTasks.size());

            int successCount = 0;
            int errorCount = 0;

            for (Task task : activeTasks) {
                try {
                    pollTaskStatus(task.getTaskId());
                    successCount++;
                } catch (Exception e) {
                    log.error("Error polling task {}: {}", task.getTaskId(), e.getMessage());
                    errorCount++;
                }
            }

            log.info("Status polling cycle complete: {} success, {} errors", successCount, errorCount);

        } catch (Exception e) {
            log.error("Error in status polling cycle", e);
        }
    }

    private void pollTaskStatus(String taskId) {
        TaskStatus previousStatus = taskRepository.findById(taskId)
            .map(Task::getStatus)
            .orElse(null);

        // 已是终态，跳过
        if (previousStatus == TaskStatus.COMPLETED ||
            previousStatus == TaskStatus.FAILED ||
            previousStatus == TaskStatus.CANCELLED) {
            log.debug("Task {} already in terminal state: {}", taskId, previousStatus);
            return;
        }

        // 查询Kuscia状态
        TaskStatus newStatus = kusciaClient.getTaskStatus(taskId);

        // 状态变化时更新数据库
        if (newStatus != previousStatus) {
            log.info("Task {} status changed: {} -> {}", taskId, previousStatus, newStatus);
            taskRepository.updateStatus(taskId, newStatus);

            // 处理终态
            if (newStatus == TaskStatus.COMPLETED || newStatus == TaskStatus.FAILED) {
                handleTerminalState(taskId, newStatus);
            }
        }
    }

    private void handleTerminalState(String taskId, TaskStatus status) {
        if (status == TaskStatus.COMPLETED) {
            log.info("Task {} completed, fetching result", taskId);
            try {
                byte[] result = kusciaClient.getTaskResult(taskId);
                if (result != null && result.length > 0) {
                    log.info("Task {} result received, size: {} bytes", taskId, result.length);
                }
            } catch (Exception e) {
                log.error("Error fetching result for task {}", taskId, e);
            }
        }
    }
}