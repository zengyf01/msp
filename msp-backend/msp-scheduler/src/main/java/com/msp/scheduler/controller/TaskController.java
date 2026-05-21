package com.msp.scheduler.controller;

import com.msp.common.core.*;
import com.msp.scheduler.service.TaskScheduler;
import org.springframework.web.bind.annotation.*;

/**
 * 任务调度REST接口
 */
@RestController
@RequestMapping("/api/v1/msp/tasks")
public class TaskController {

    private final TaskScheduler taskScheduler;

    public TaskController(TaskScheduler taskScheduler) {
        this.taskScheduler = taskScheduler;
    }

    private com.msp.kuscia.client.KusciaClient kusciaClient;

    @org.springframework.beans.factory.annotation.Autowired
    public void setKusciaClient(com.msp.kuscia.client.KusciaClient kusciaClient) {
        this.kusciaClient = kusciaClient;
    }

    /**
     * 列出任务（分页）
     */
    @GetMapping
    public ApiResponse<Page<Task>> listTasks(
            @RequestParam(value = "status", required = false) TaskStatus status,
            @RequestParam(value = "type", required = false) TaskType type,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "20") int size) {
        Page<Task> tasks = taskScheduler.listTasks(status, type, page, size);
        return ApiResponse.success(tasks);
    }

    /**
     * 创建任务
     */
    @PostMapping
    public ApiResponse<TaskCreateResponse> createTask(@RequestBody TaskRequest request) {
        String taskId = taskScheduler.submitTask(request);
        return ApiResponse.success(new TaskCreateResponse(taskId, TaskStatus.CREATED));
    }

    /**
     * 查询任务详情
     */
    @GetMapping("/{taskId}")
    public ApiResponse<Task> getTask(@PathVariable String taskId) {
        Task task = taskScheduler.getTask(taskId);
        return ApiResponse.success(task);
    }

    /**
     * 查询任务状态
     */
    @GetMapping("/{taskId}/status")
    public ApiResponse<TaskStatusResponse> getTaskStatus(@PathVariable String taskId) {
        TaskStatus status = taskScheduler.queryStatus(taskId);
        return ApiResponse.success(new TaskStatusResponse(taskId, status, null));
    }

    /**
     * 取消任务
     */
    @DeleteMapping("/{taskId}")
    public ApiResponse<Boolean> cancelTask(@PathVariable String taskId) {
        boolean result = taskScheduler.cancelTask(taskId);
        return ApiResponse.success(result);
    }

    /**
     * 获取任务结果
     */
    @GetMapping("/{taskId}/result")
    public ApiResponse<TaskResultResponse> getTaskResult(@PathVariable String taskId) {
        Task task = taskScheduler.getTask(taskId);
        byte[] resultData = kusciaClient.getTaskResult(taskId);
        return ApiResponse.success(new TaskResultResponse(taskId, task.getStatus(), resultData));
    }

    /**
     * 重试失败任务
     */
    @PostMapping("/{taskId}/retry")
    public ApiResponse<TaskCreateResponse> retryTask(@PathVariable String taskId) {
        String newTaskId = taskScheduler.retryTask(taskId);
        return ApiResponse.success(new TaskCreateResponse(newTaskId, TaskStatus.CREATED));
    }

    // 内部类
    public static class TaskCreateResponse {
        private String taskId;
        private TaskStatus status;

        public TaskCreateResponse(String taskId, TaskStatus status) {
            this.taskId = taskId;
            this.status = status;
        }

        public String getTaskId() { return taskId; }
        public TaskStatus getStatus() { return status; }
    }

    public static class TaskStatusResponse {
        private String taskId;
        private TaskStatus status;
        private Integer progress;

        public TaskStatusResponse(String taskId, TaskStatus status, Integer progress) {
            this.taskId = taskId;
            this.status = status;
            this.progress = progress;
        }

        public String getTaskId() { return taskId; }
        public TaskStatus getStatus() { return status; }
        public Integer getProgress() { return progress; }
    }

    public static class TaskResultResponse {
        private String taskId;
        private TaskStatus status;
        private Object result;

        public TaskResultResponse(String taskId, TaskStatus status, Object result) {
            this.taskId = taskId;
            this.status = status;
            this.result = result;
        }

        public String getTaskId() { return taskId; }
        public TaskStatus getStatus() { return status; }
        public Object getResult() { return result; }
    }
}