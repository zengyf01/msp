package com.msp.node.service;

import com.msp.common.core.Node;
import com.msp.common.core.NodeStatus;
import com.msp.node.repository.NodeRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 节点心跳健康监控
 *
 * <p>节点 Python 端每 30s 上报一次心跳（{@code NodeController#heartbeat} 仅置 ONLINE，不感知超时）。
 * 本服务定时扫描：状态为 ONLINE 但 {@code update_time} 超过阈值未更新的节点 → 置为 OFFLINE。</p>
 *
 * <p>阈值与检查周期通过 {@code application.yml} 的 {@code msp.node.heartbeat.*} 注入，运行时可调。</p>
 */
@Service
public class NodeHealthMonitor {

    private static final Logger log = LoggerFactory.getLogger(NodeHealthMonitor.class);

    private final NodeRepository nodeRepository;
    private final long staleThresholdMs;

    public NodeHealthMonitor(NodeRepository nodeRepository,
                             @Value("${msp.node.heartbeat.stale-threshold-seconds:90}") long staleThresholdSeconds) {
        this.nodeRepository = nodeRepository;
        this.staleThresholdMs = staleThresholdSeconds * 1000L;
    }

    @Scheduled(fixedRateString = "${msp.node.heartbeat.check-interval-ms:15000}")
    public void detectStaleNodes() {
        long now = System.currentTimeMillis();
        long cutoff = now - staleThresholdMs;
        List<Node> stale = nodeRepository.findStaleOnlineNodes(cutoff);
        if (stale.isEmpty()) {
            return;
        }
        for (Node n : stale) {
            long age = n.getUpdateTime() != null ? now - n.getUpdateTime() : -1L;
            log.warn("Node {} heartbeat stale (last update {} ms ago, threshold {} ms), marking OFFLINE",
                n.getNodeId(), age, staleThresholdMs);
            nodeRepository.updateStatus(n.getNodeId(), NodeStatus.OFFLINE);
        }
    }
}
