package com.msp.common.core;

import java.util.List;
import java.util.Map;

/**
 * 任务实体
 */
public class Task {
    private String taskId;
    private String name;
    private TaskType type;
    private String algorithm;
    private TaskStatus status;
    private List<String> participants;
    private Map<String, DataSource> inputs;
    private Map<String, String> parameters;
    private String description;
    private Long createTime;
    private Long updateTime;

    public Task() {}

    public Task(String taskId, String name, TaskType type, TaskStatus status) {
        this.taskId = taskId;
        this.name = name;
        this.type = type;
        this.status = status;
    }

    public String getTaskId() { return taskId; }
    public void setTaskId(String taskId) { this.taskId = taskId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public TaskType getType() { return type; }
    public void setType(TaskType type) { this.type = type; }

    public String getAlgorithm() { return algorithm; }
    public void setAlgorithm(String algorithm) { this.algorithm = algorithm; }

    public TaskStatus getStatus() { return status; }
    public void setStatus(TaskStatus status) { this.status = status; }

    public List<String> getParticipants() { return participants; }
    public void setParticipants(List<String> participants) { this.participants = participants; }

    public Map<String, DataSource> getInputs() { return inputs; }
    public void setInputs(Map<String, DataSource> inputs) { this.inputs = inputs; }

    public Map<String, String> getParameters() { return parameters; }
    public void setParameters(Map<String, String> parameters) { this.parameters = parameters; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public Long getCreateTime() { return createTime; }
    public void setCreateTime(Long createTime) { this.createTime = createTime; }

    public Long getUpdateTime() { return updateTime; }
    public void setUpdateTime(Long updateTime) { this.updateTime = updateTime; }
}