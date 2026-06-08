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
    // 纵向联邦学习字段
    private String labelParty;                              // 标签提供方
    private Map<String, List<String>> featureParties;       // 各方特征列: {"party_a": ["col1", "col2"]}
    private String labelColumn;                              // 标签列名
    private String modelType;                               // 模型类型: logistic_regression / secureboost
    private String nodeMode;                                // 节点模式: ray 或 kuscia

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
    public String getLabelParty() { return labelParty; }
    public void setLabelParty(String labelParty) { this.labelParty = labelParty; }
    public Map<String, List<String>> getFeatureParties() { return featureParties; }
    public void setFeatureParties(Map<String, List<String>> featureParties) { this.featureParties = featureParties; }
    public String getLabelColumn() { return labelColumn; }
    public void setLabelColumn(String labelColumn) { this.labelColumn = labelColumn; }
    public String getModelType() { return modelType; }
    public void setModelType(String modelType) { this.modelType = modelType; }
    public String getNodeMode() { return nodeMode; }
    public void setNodeMode(String nodeMode) { this.nodeMode = nodeMode; }
}