package com.msp.node.controller;

import com.msp.common.core.*;
import com.msp.node.repository.NodeRepository;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Set;

/**
 * 节点管理REST接口
 */
@RestController
@RequestMapping("/api/v1/msp/nodes")
public class NodeController {

    private final NodeRepository nodeRepository;

    public NodeController(NodeRepository nodeRepository) {
        this.nodeRepository = nodeRepository;
    }

    /**
     * 注册节点
     */
    @PostMapping("/register")
    public ApiResponse<NodeRegisterResponse> registerNode(@RequestBody NodeRegisterRequest request) {
        long now = System.currentTimeMillis();

        Node node = new Node();
        node.setNodeId(request.getNodeId());
        node.setNodeName(request.getNodeName());
        node.setStatus(NodeStatus.ONLINE);
        node.setEndpoint(request.getEndpoint());
        node.setCapabilities(request.getCapabilities());
        node.setTags(request.getTags());
        node.setCreateTime(now);
        node.setUpdateTime(now);

        nodeRepository.save(node);

        return ApiResponse.success(new NodeRegisterResponse(node.getNodeId(), node.getStatus()));
    }

    /**
     * 查询节点列表
     */
    @GetMapping
    public ApiResponse<Page<Node>> listNodes(
            @RequestParam(value = "status", required = false) NodeStatus status,
            @RequestParam(value = "capability", required = false) DeviceType capability,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "20") int size) {
        List<Node> content = nodeRepository.findAll(status, capability, page, size);
        long total = nodeRepository.count(status);
        return ApiResponse.success(new Page<>(content, total, page, size));
    }

    /**
     * 查询节点状态
     */
    @GetMapping("/{nodeId}")
    public ApiResponse<Node> getNode(@PathVariable String nodeId) {
        Node node = nodeRepository.findById(nodeId)
            .orElseThrow(() -> new MspException(ErrorCode.NODE_NOT_FOUND, "Node not found: " + nodeId));
        return ApiResponse.success(node);
    }

    /**
     * 心跳上报
     */
    @PostMapping("/{nodeId}/heartbeat")
    public ApiResponse<Boolean> heartbeat(@PathVariable String nodeId) {
        if (nodeRepository.findById(nodeId).isEmpty()) {
            throw new MspException(ErrorCode.NODE_NOT_FOUND, "Node not found: " + nodeId);
        }
        nodeRepository.updateStatus(nodeId, NodeStatus.ONLINE);
        return ApiResponse.success(true);
    }

    /**
     * 注销节点
     */
    @DeleteMapping("/{nodeId}")
    public ApiResponse<Boolean> unregisterNode(@PathVariable String nodeId) {
        if (nodeRepository.findById(nodeId).isEmpty()) {
            throw new MspException(ErrorCode.NODE_NOT_FOUND, "Node not found: " + nodeId);
        }
        nodeRepository.delete(nodeId);
        return ApiResponse.success(true);
    }

    // 内部类
    public static class NodeRegisterRequest {
        private String nodeId;
        private String nodeName;
        private Set<DeviceType> capabilities;
        private String endpoint;
        private List<String> tags;

        public String getNodeId() { return nodeId; }
        public void setNodeId(String nodeId) { this.nodeId = nodeId; }
        public String getNodeName() { return nodeName; }
        public void setNodeName(String nodeName) { this.nodeName = nodeName; }
        public Set<DeviceType> getCapabilities() { return capabilities; }
        public void setCapabilities(Set<DeviceType> capabilities) { this.capabilities = capabilities; }
        public String getEndpoint() { return endpoint; }
        public void setEndpoint(String endpoint) { this.endpoint = endpoint; }
        public List<String> getTags() { return tags; }
        public void setTags(List<String> tags) { this.tags = tags; }
    }

    public static class NodeRegisterResponse {
        private String nodeId;
        private NodeStatus status;

        public NodeRegisterResponse(String nodeId, NodeStatus status) {
            this.nodeId = nodeId;
            this.status = status;
        }

        public String getNodeId() { return nodeId; }
        public NodeStatus getStatus() { return status; }
    }
}