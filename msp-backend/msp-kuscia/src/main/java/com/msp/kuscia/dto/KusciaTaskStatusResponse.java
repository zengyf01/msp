package com.msp.kuscia.dto;

/**
 * Kuscia任务状态响应DTO
 */
public class KusciaTaskStatusResponse {
    private String taskId;
    private String name;
    private String phase;
    private String message;
    private Long startTime;
    private Long completionTime;

    public String getTaskId() { return taskId; }
    public void setTaskId(String taskId) { this.taskId = taskId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getPhase() { return phase; }
    public void setPhase(String phase) { this.phase = phase; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public Long getStartTime() { return startTime; }
    public void setStartTime(Long startTime) { this.startTime = startTime; }
    public Long getCompletionTime() { return completionTime; }
    public void setCompletionTime(Long completionTime) { this.completionTime = completionTime; }
}