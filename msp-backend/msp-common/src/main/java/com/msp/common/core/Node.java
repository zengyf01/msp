package com.msp.common.core;

import java.util.List;
import java.util.Set;

/**
 * 节点实体
 */
public class Node {
    private String nodeId;
    private String nodeName;
    private NodeStatus status;
    private String nodeMode;  // 节点部署模式：RAY / KUSCIA
    private String endpoint;
    private String externalEndpoint;
    private Set<DeviceType> capabilities;
    private List<String> tags;
    private Long createTime;
    private Long updateTime;

    public Node() {}

    public Node(String nodeId, String nodeName, NodeStatus status) {
        this.nodeId = nodeId;
        this.nodeName = nodeName;
        this.status = status;
    }

    public String getNodeId() { return nodeId; }
    public void setNodeId(String nodeId) { this.nodeId = nodeId; }

    public String getNodeName() { return nodeName; }
    public void setNodeName(String nodeName) { this.nodeName = nodeName; }

    public NodeStatus getStatus() { return status; }
    public void setStatus(NodeStatus status) { this.status = status; }

    public String getNodeMode() { return nodeMode; }
    public void setNodeMode(String nodeMode) { this.nodeMode = nodeMode; }

    public String getEndpoint() { return endpoint; }
    public void setEndpoint(String endpoint) { this.endpoint = endpoint; }

    public String getExternalEndpoint() { return externalEndpoint; }
    public void setExternalEndpoint(String externalEndpoint) { this.externalEndpoint = externalEndpoint; }

    public Set<DeviceType> getCapabilities() { return capabilities; }
    public void setCapabilities(Set<DeviceType> capabilities) { this.capabilities = capabilities; }

    public List<String> getTags() { return tags; }
    public void setTags(List<String> tags) { this.tags = tags; }

    public Long getCreateTime() { return createTime; }
    public void setCreateTime(Long createTime) { this.createTime = createTime; }

    public Long getUpdateTime() { return updateTime; }
    public void setUpdateTime(Long updateTime) { this.updateTime = updateTime; }
}