package com.msp.kuscia.dto;

import java.util.List;
import java.util.Map;

/**
 * Kuscia任务规格DTO
 */
public class KusciaTaskSpec {
    private String taskId;
    private String name;
    private String type;
    private String priority;
    private Integer timeoutSeconds;
    private Map<String, String> annotations;
    private List<Party> parties;
    private TaskInput inputs;
    private Map<String, String> parameters;

    public static class Party {
        private String name;
        private String nodeID;
        private String code;

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getNodeID() { return nodeID; }
        public void setNodeID(String nodeID) { this.nodeID = nodeID; }
        public String getCode() { return code; }
        public void setCode(String code) { this.code = code; }
    }

    public static class TaskInput {
        private Map<String, Object> data;

        public Map<String, Object> getData() { return data; }
        public void setData(Map<String, Object> data) { this.data = data; }
    }

    public String getTaskId() { return taskId; }
    public void setTaskId(String taskId) { this.taskId = taskId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public String getPriority() { return priority; }
    public void setPriority(String priority) { this.priority = priority; }
    public Integer getTimeoutSeconds() { return timeoutSeconds; }
    public void setTimeoutSeconds(Integer timeoutSeconds) { this.timeoutSeconds = timeoutSeconds; }
    public Map<String, String> getAnnotations() { return annotations; }
    public void setAnnotations(Map<String, String> annotations) { this.annotations = annotations; }
    public List<Party> getParties() { return parties; }
    public void setParties(List<Party> parties) { this.parties = parties; }
    public TaskInput getInputs() { return inputs; }
    public void setInputs(TaskInput inputs) { this.inputs = inputs; }
    public Map<String, String> getParameters() { return parameters; }
    public void setParameters(Map<String, String> parameters) { this.parameters = parameters; }
}