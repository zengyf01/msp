package com.msp.common.core;

import java.util.List;
import java.util.Set;

/**
 * 节点能力
 */
public class NodeCapability {
    private String nodeId;
    private Set<DeviceType> supportedDevices;
    private Integer maxParallelTasks;
    private Integer cpuCores;
    private Long memoryMB;
    private Boolean gpuAvailable;

    // Getters and Setters
    public String getNodeId() { return nodeId; }
    public void setNodeId(String nodeId) { this.nodeId = nodeId; }
    public Set<DeviceType> getSupportedDevices() { return supportedDevices; }
    public void setSupportedDevices(Set<DeviceType> supportedDevices) { this.supportedDevices = supportedDevices; }
    public Integer getMaxParallelTasks() { return maxParallelTasks; }
    public void setMaxParallelTasks(Integer maxParallelTasks) { this.maxParallelTasks = maxParallelTasks; }
    public Integer getCpuCores() { return cpuCores; }
    public void setCpuCores(Integer cpuCores) { this.cpuCores = cpuCores; }
    public Long getMemoryMB() { return memoryMB; }
    public void setMemoryMB(Long memoryMB) { this.memoryMB = memoryMB; }
    public Boolean getGpuAvailable() { return gpuAvailable; }
    public void setGpuAvailable(Boolean gpuAvailable) { this.gpuAvailable = gpuAvailable; }
}