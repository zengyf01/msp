package com.msp.scheduler.controller;

import com.msp.common.core.ApiResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * PSI 协调控制器 - 协调多方 ECDH PSI 密钥交换和加密ID提交
 *
 * ECDH PSI 流程:
 * 1. 各节点提交公钥到 /key_exchange
 * 2. 所有公钥收集完毕后，各节点提交加密ID到 /submit_encrypted_ids
 * 3. 所有加密ID收集完毕后，计算交集并返回结果
 */
@RestController
@RequestMapping("/api/v1/psi")
public class PsiController {

    private static final Logger log = LoggerFactory.getLogger(PsiController.class);

    // PSI 任务状态存储 {taskId -> PsiTaskState}
    private final Map<String, PsiTaskState> psiTasks = new ConcurrentHashMap<>();

    /**
     * 提交公钥（ECDH 密钥交换第一步）
     *
     * 请求体: { "taskId": "xxx", "party": "node-hospital", "publicKey": "04...", "participants": ["node-hospital", "node-insurance"] }
     * 响应: { "success": true, "publicKeys": {...} } 或 { "success": true, "message": "Waiting for X more parties" }
     */
    @PostMapping("/key_exchange")
    public ApiResponse<Map<String, Object>> submitPublicKey(@RequestBody Map<String, Object> request) {
        String taskId = String.valueOf(request.get("taskId"));
        String party = String.valueOf(request.get("party"));
        String publicKey = String.valueOf(request.get("publicKey"));
        @SuppressWarnings("unchecked")
        List<String> participants = (List<String>) request.get("participants");

        if (taskId == null || party == null || publicKey == null || participants == null) {
            return ApiResponse.error("INVALID_PARAMS", "Missing required fields: taskId, party, publicKey, participants");
        }

        log.info("PSI key_exchange: taskId={}, party={}, participants={}", taskId, party, participants);

        PsiTaskState state = psiTasks.computeIfAbsent(taskId, k -> new PsiTaskState(participants));

        synchronized (state) {
            // 存储公钥
            state.publicKeys.put(party, publicKey);

            // 检查是否收集完所有公钥
            boolean allReceived = state.isPublicKeysComplete();

            if (allReceived) {
                Map<String, String> allKeys = new HashMap<>(state.publicKeys);
                log.info("PSI key_exchange complete for task {}: all {} parties submitted keys",
                    taskId, participants.size());
                return ApiResponse.success(Map.of(
                    "success", true,
                    "publicKeys", allKeys,
                    "message", "All public keys received"
                ));
            } else {
                int waiting = participants.size() - state.publicKeys.size();
                log.info("PSI key_exchange waiting for task {}: {} more parties", taskId, waiting);
                return ApiResponse.success(Map.of(
                    "success", true,
                    "publicKeys", Collections.singletonMap(party, publicKey),
                    "message", "Waiting for " + waiting + " more parties"
                ));
            }
        }
    }

    /**
     * 提交加密ID（ECDH PSI 第二步）
     *
     * 请求体: { "taskId": "xxx", "party": "node-hospital", "encryptedIds": ["hash1", ...], "participants": ["node-hospital", "node-insurance"] }
     * 响应: { "success": true, "intersection": [...] } 或 { "success": true, "message": "Waiting for X more parties" }
     */
    @PostMapping("/submit_encrypted_ids")
    public ApiResponse<Map<String, Object>> submitEncryptedIds(@RequestBody Map<String, Object> request) {
        String taskId = String.valueOf(request.get("taskId"));
        String party = String.valueOf(request.get("party"));
        @SuppressWarnings("unchecked")
        List<String> encryptedIds = (List<String>) request.get("encryptedIds");
        @SuppressWarnings("unchecked")
        List<String> participants = (List<String>) request.get("participants");

        if (taskId == null || party == null || encryptedIds == null || participants == null) {
            return ApiResponse.error("INVALID_PARAMS", "Missing required fields: taskId, party, encryptedIds, participants");
        }

        log.info("PSI submit_encrypted_ids: taskId={}, party={}, idCount={}", taskId, party, encryptedIds.size());

        PsiTaskState state = psiTasks.get(taskId);
        if (state == null) {
            // 如果状态不存在，可能是公钥交换还没开始或已超时，尝试创建新状态
            log.warn("PSI task state not found for {}, creating new state", taskId);
            state = psiTasks.computeIfAbsent(taskId, k -> new PsiTaskState(participants));
        }

        synchronized (state) {
            // 存储加密ID
            state.encryptedIds.put(party, new HashSet<>(encryptedIds));

            // 检查是否收集完所有加密ID
            boolean allReceived = state.isEncryptedIdsComplete();

            if (allReceived) {
                // 计算交集
                Set<String> intersection = state.computeIntersection();
                List<String> result = new ArrayList<>(intersection);

                log.info("PSI intersection computed for task {}: {} matched out of {} total",
                    taskId, result.size(),
                    state.encryptedIds.values().stream().mapToInt(Set::size).sum());

                // 清理状态
                psiTasks.remove(taskId);

                return ApiResponse.success(Map.of(
                    "success", true,
                    "intersection", result,
                    "matchedCount", result.size(),
                    "message", "Intersection computed successfully"
                ));
            } else {
                int waiting = participants.size() - state.encryptedIds.size();
                log.info("PSI submit_encrypted_ids waiting for task {}: {} more parties", taskId, waiting);
                return ApiResponse.success(Map.of(
                    "success", true,
                    "message", "Waiting for " + waiting + " more parties"
                ));
            }
        }
    }

    /**
     * 查询任务状态
     */
    @GetMapping("/status/{taskId}")
    public ApiResponse<Map<String, Object>> getStatus(@PathVariable String taskId) {
        PsiTaskState state = psiTasks.get(taskId);
        if (state == null) {
            return ApiResponse.success(Map.of(
                "status", "not_found",
                "message", "Task not found or already completed"
            ));
        }

        synchronized (state) {
            boolean keysComplete = state.isPublicKeysComplete();
            boolean idsComplete = state.isEncryptedIdsComplete();

            String status;
            if (keysComplete && idsComplete) {
                status = "completed";
            } else if (keysComplete) {
                status = "waiting_for_encrypted_ids";
            } else {
                status = "waiting_for_public_keys";
            }

            return ApiResponse.success(Map.of(
                "status", status,
                "publicKeysSubmitted", state.publicKeys.size(),
                "encryptedIdsSubmitted", state.encryptedIds.size(),
                "participantsCount", state.participants.size()
            ));
        }
    }

    /**
     * 重置任务状态
     */
    @PostMapping("/reset/{taskId}")
    public ApiResponse<Map<String, Object>> resetTask(@PathVariable String taskId) {
        psiTasks.remove(taskId);
        log.info("PSI task {} reset", taskId);
        return ApiResponse.success(Map.of(
            "success", true,
            "message", "Task reset successfully"
        ));
    }

    /**
     * PSI 任务状态
     */
    private static class PsiTaskState {
        final List<String> participants;
        final Map<String, String> publicKeys = new ConcurrentHashMap<>();
        final Map<String, Set<String>> encryptedIds = new ConcurrentHashMap<>();

        PsiTaskState(List<String> participants) {
            this.participants = new ArrayList<>(participants);
        }

        boolean isPublicKeysComplete() {
            return participants.stream().allMatch(publicKeys::containsKey);
        }

        boolean isEncryptedIdsComplete() {
            return participants.stream().allMatch(encryptedIds::containsKey);
        }

        Set<String> computeIntersection() {
            if (encryptedIds.isEmpty()) {
                return Collections.emptySet();
            }

            Set<String> intersection = null;
            for (Set<String> ids : encryptedIds.values()) {
                if (intersection == null) {
                    intersection = new HashSet<>(ids);
                } else {
                    intersection.retainAll(ids);
                }
                if (intersection.isEmpty()) {
                    break;
                }
            }
            return intersection != null ? intersection : Collections.emptySet();
        }
    }
}