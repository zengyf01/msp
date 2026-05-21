package com.msp.kuscia.mapper;

import com.msp.common.core.TaskStatus;

import java.util.Map;

/**
 * Kuscia状态到MSP状态的映射
 */
public class KusciaStatusMapper {

    private static final Map<String, TaskStatus> STATUS_MAP = Map.of(
        "Pending", TaskStatus.PENDING,
        "Running", TaskStatus.RUNNING,
        "Succeeded", TaskStatus.COMPLETED,
        "Failed", TaskStatus.FAILED,
        "Cancelled", TaskStatus.CANCELLED,
        "Deleted", TaskStatus.CANCELLED
    );

    /**
     * 将Kuscia状态映射为MSP状态
     */
    public static TaskStatus map(String kusciaPhase) {
        if (kusciaPhase == null || kusciaPhase.isEmpty()) {
            return TaskStatus.PENDING;
        }
        return STATUS_MAP.getOrDefault(kusciaPhase, TaskStatus.PENDING);
    }

    /**
     * 判断是否为终态
     */
    public static boolean isTerminalState(String kusciaPhase) {
        return "Succeeded".equals(kusciaPhase)
            || "Failed".equals(kusciaPhase)
            || "Cancelled".equals(kusciaPhase);
    }
}