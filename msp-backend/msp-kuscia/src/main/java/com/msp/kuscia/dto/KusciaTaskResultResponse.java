package com.msp.kuscia.dto;

/**
 * Kuscia任务结果响应DTO
 */
public class KusciaTaskResultResponse {
    private String taskId;
    private String format;
    private byte[] data;
    private String filePath;

    public String getTaskId() { return taskId; }
    public void setTaskId(String taskId) { this.taskId = taskId; }
    public String getFormat() { return format; }
    public void setFormat(String format) { this.format = format; }
    public byte[] getData() { return data; }
    public void setData(byte[] data) { this.data = data; }
    public String getFilePath() { return filePath; }
    public void setFilePath(String filePath) { this.filePath = filePath; }
}