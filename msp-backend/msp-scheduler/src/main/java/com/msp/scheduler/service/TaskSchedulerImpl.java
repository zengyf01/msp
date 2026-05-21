package com.msp.scheduler.service;

import com.msp.common.core.*;
import com.msp.kuscia.client.KusciaClient;
import com.msp.scheduler.repository.TaskRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * 任务调度核心实现
 */
@Service
public class TaskSchedulerImpl implements TaskScheduler {

    private static final Logger log = LoggerFactory.getLogger(TaskSchedulerImpl.class);

    private final TaskRepository taskRepository;
    private final KusciaClient kusciaClient;

    public TaskSchedulerImpl(TaskRepository taskRepository, KusciaClient kusciaClient) {
        this.taskRepository = taskRepository;
        this.kusciaClient = kusciaClient;
    }

    @Override
    public String submitTask(TaskRequest request) {
        String taskId = UUID.randomUUID().toString();
        long now = System.currentTimeMillis();

        Task task = new Task();
        task.setTaskId(taskId);
        task.setName(request.getName());
        task.setType(request.getType());
        task.setAlgorithm(request.getAlgorithm());
        task.setStatus(TaskStatus.CREATED);
        task.setParticipants(request.getParticipants());
        task.setInputs(request.getInputs());
        task.setParameters(request.getParameters());
        task.setDescription(request.getDescription());
        task.setCreateTime(now);
        task.setUpdateTime(now);

        taskRepository.save(task);
        taskRepository.updateStatus(taskId, TaskStatus.PENDING);

        // 异步提交任务到Kuscia，不阻塞主线程
        CompletableFuture.runAsync(() -> {
            try {
                kusciaClient.submitTask(taskId, request);
                log.info("Task {} submitted to Kuscia asynchronously", taskId);
            } catch (Exception e) {
                log.error("Failed to submit task {} to Kuscia: {}", taskId, e.getMessage());
                taskRepository.updateStatus(taskId, TaskStatus.FAILED);
            }
        });

        return taskId;
    }

    @Override
    public TaskStatus queryStatus(String taskId) {
        Task task = taskRepository.findById(taskId)
            .orElseThrow(() -> new MspException(ErrorCode.TASK_NOT_FOUND, "Task not found: " + taskId));

        // 同步Kuscia状态
        TaskStatus status = kusciaClient.getTaskStatus(taskId);
        if (status != task.getStatus()) {
            taskRepository.updateStatus(taskId, status);
        }

        return status;
    }

    @Override
    public boolean cancelTask(String taskId) {
        Task task = taskRepository.findById(taskId)
            .orElseThrow(() -> new MspException(ErrorCode.TASK_NOT_FOUND, "Task not found: " + taskId));

        if (task.getStatus() == TaskStatus.COMPLETED || task.getStatus() == TaskStatus.FAILED) {
            return false;
        }

        // 使用KusciaClient接口取消任务
        kusciaClient.cancelTask(taskId);
        taskRepository.updateStatus(taskId, TaskStatus.CANCELLED);

        return true;
    }

    @Override
    public String retryTask(String taskId) {
        Task task = taskRepository.findById(taskId)
            .orElseThrow(() -> new MspException(ErrorCode.TASK_NOT_FOUND, "Task not found: " + taskId));

        if (task.getStatus() != TaskStatus.FAILED) {
            throw new MspException(ErrorCode.INVALID_TASK_STATUS, "Only failed tasks can be retried");
        }

        // 创建新的请求对象
        TaskRequest request = new TaskRequest();
        request.setName(task.getName());
        request.setType(task.getType());
        request.setAlgorithm(task.getAlgorithm());
        request.setParticipants(task.getParticipants());
        request.setInputs(task.getInputs());
        request.setParameters(task.getParameters());
        request.setDescription(task.getDescription());

        // 提交新任务
        String newTaskId = submitTask(request);

        return newTaskId;
    }

    @Override
    public Task getTask(String taskId) {
        return taskRepository.findById(taskId)
            .orElseThrow(() -> new MspException(ErrorCode.TASK_NOT_FOUND, "Task not found: " + taskId));
    }

    @Override
    public Page<Task> listTasks(TaskStatus statusFilter, TaskType typeFilter, int page, int size) {
        List<Task> content = taskRepository.findAll(statusFilter, typeFilter, page, size);
        long total = taskRepository.count(statusFilter, typeFilter);
        return new Page<>(content, total, page, size);
    }
}