package com.msp.common.core;

import java.util.List;
import java.util.Map;

/**
 * 任务请求
 */
public class TaskRequest {
    private String name;
    private TaskType type;
    private String algorithm;
    private List<String> participants;
    private Map<String, DataSource> inputs;
    private Map<String, String> parameters;
    private String description;

    // Getters and Setters
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public TaskType getType() { return type; }
    public void setType(TaskType type) { this.type = type; }
    public String getAlgorithm() { return algorithm; }
    public void setAlgorithm(String algorithm) { this.algorithm = algorithm; }
    public List<String> getParticipants() { return participants; }
    public void setParticipants(List<String> participants) { this.participants = participants; }
    public Map<String, DataSource> getInputs() { return inputs; }
    public void setInputs(Map<String, DataSource> inputs) { this.inputs = inputs; }
    public Map<String, String> getParameters() { return parameters; }
    public void setParameters(Map<String, String> parameters) { this.parameters = parameters; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
}