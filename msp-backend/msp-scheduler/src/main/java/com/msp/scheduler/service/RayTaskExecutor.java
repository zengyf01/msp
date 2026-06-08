package com.msp.scheduler.service;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.msp.common.core.DataSource;
import com.msp.common.core.TaskRequest;
import com.msp.common.core.TaskStatus;
import com.msp.common.core.TaskType;
import com.msp.scheduler.repository.TaskRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

/**
 * Ray 模式任务执行器 - 通过 HTTP 调用 Python 计算节点
 */
@Component
public class RayTaskExecutor {

    private static final Logger log = LoggerFactory.getLogger(RayTaskExecutor.class);

    @Autowired
    private TaskRepository taskRepository;

    @Autowired
    private org.springframework.jdbc.core.JdbcTemplate jdbcTemplate;

    private final java.util.concurrent.ConcurrentHashMap<String, String> nodeNameCache = new java.util.concurrent.ConcurrentHashMap<>();

    @Value("${msp.node.http.port:50052}")
    private int nodeHttpPort;

    @Value("${msp.node.grpc.host:node-a}")
    private String defaultNodeHost;

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    // executeDag 的日志上下文，供 catch 块使用
    private List<Map<String, Object>> dagLogEntries;

    // 最后一个节点的响应，用于非 PSI 顺序分发
    private NodeExecuteResponse lastResponse;

    /**
     * 执行 PSI 任务
     */
    public String executePsi(String taskId, TaskRequest request) {
        log.info("执行 PSI 任务: {}", taskId);
        try {
            // 为每个参与方构建并发送 DAG 请求
            List<String> participants = request.getParticipants();
            if (participants == null || participants.isEmpty()) {
                throw new RuntimeException("PSI 任务需要指定参与方");
            }

            // 构建 PSI DAG 并发送给每个参与方
            String dagDefinition = buildPsiDagDefinition(taskId, request);

            Map<String, Object> aggregatedResult = new HashMap<>();
            aggregatedResult.put("task_id", taskId);
            aggregatedResult.put("type", "PSI");
            aggregatedResult.put("participants", participants);

            boolean anyRunning = false;
            boolean anyError = false;

            for (String participant : participants) {
                String nodeAddress = mapNodeNameToHost(participant) + ":" + getNodeHttpPort(participant);

                Map<String, Object> requestBody = new HashMap<>();
                requestBody.put("task_id", taskId);
                requestBody.put("dag_definition", dagDefinition);
                requestBody.put("self_party", participant);
                requestBody.put("participants", participants);
                requestBody.put("parameters", request.getParameters());
                requestBody.put("data_sources", request.getInputs());

                // 调用节点 HTTP 端点
                NodeExecuteResponse response = callNodeHttp(nodeAddress, requestBody);

                if (!response.isSuccess()) {
                    throw new RuntimeException("节点 " + resolveNodeName(participant) + " 执行失败: " + response.getError());
                }

                // 检查响应状态
                String responseStatus = response.getStatus();
                if ("running".equals(responseStatus)) {
                    anyRunning = true;
                    log.info("节点 {} PSI 执行中，等待其他参与方完成", participant);
                } else if ("error".equals(responseStatus) || "partial_failed".equals(responseStatus)) {
                    anyError = true;
                }

                // 汇总结果
                Map<String, Object> nodeResult = response.getResults();
                aggregatedResult.put("node_" + participant, nodeResult);
            }

            // 根据状态更新任务结果
            if (anyError) {
                aggregatedResult.put("status", "error");
                taskRepository.updateStatus(taskId, TaskStatus.FAILED);
            } else if (anyRunning) {
                // PSI 需要多方协调，仍在运行中
                aggregatedResult.put("status", "running");
                taskRepository.updateStatus(taskId, TaskStatus.RUNNING);
            } else {
                aggregatedResult.put("status", "ok");
                taskRepository.updateStatus(taskId, TaskStatus.COMPLETED);
            }

            String resultJson = objectMapper.writeValueAsString(aggregatedResult);
            taskRepository.updateResult(taskId, resultJson);
            return resultJson;

        } catch (Exception e) {
            log.error("PSI 任务执行失败: {}", e.getMessage());
            String errorResult = "{\"status\":\"error\",\"message\":\"" + e.getMessage() + "\"}";
            taskRepository.updateResult(taskId, errorResult);
            taskRepository.updateStatus(taskId, TaskStatus.FAILED);
            throw new RuntimeException("PSI 执行失败: " + e.getMessage(), e);
        }
    }

    /**
     * 执行 MPC 任务
     */
    public void executeMpc(String taskId, TaskRequest request) {
        log.info("执行 MPC 任务: {}", taskId);
        try {
            List<String> participants = request.getParticipants();
            if (participants == null || participants.isEmpty()) {
                throw new RuntimeException("MPC 任务需要指定参与方");
            }

            String dagDefinition = buildMpcDagDefinition(taskId, request);

            for (String participant : participants) {
                String nodeAddress = mapNodeNameToHost(participant) + ":" + getNodeHttpPort(participant);

                Map<String, Object> requestBody = new HashMap<>();
                requestBody.put("task_id", taskId);
                requestBody.put("dag_definition", dagDefinition);
                requestBody.put("self_party", participant);
                requestBody.put("participants", participants);
                requestBody.put("parameters", request.getParameters());
                requestBody.put("data_sources", request.getInputs());

                NodeExecuteResponse response = callNodeHttp(nodeAddress, requestBody);

                if (!response.isSuccess()) {
                    throw new RuntimeException("节点 " + resolveNodeName(participant) + " 执行失败: " + response.getError());
                }
            }

            taskRepository.updateResult(taskId, "{\"status\":\"ok\",\"type\":\"MPC\"}");

        } catch (Exception e) {
            log.error("MPC 任务执行失败: {}", e.getMessage());
            taskRepository.updateResult(taskId, "{\"status\":\"error\",\"message\":\"" + e.getMessage() + "\"}");
            throw new RuntimeException("MPC 执行失败: " + e.getMessage(), e);
        }
    }

    /**
     * 执行联邦学习任务
     */
    public void executeFederatedLearning(String taskId, TaskRequest request) {
        log.info("执行联邦学习任务: {}", taskId);
        try {
            List<String> participants = request.getParticipants();
            if (participants == null || participants.isEmpty()) {
                throw new RuntimeException("联邦学习任务需要指定参与方");
            }

            String dagDefinition = buildFlDagDefinition(taskId, request);

            for (String participant : participants) {
                String nodeAddress = mapNodeNameToHost(participant) + ":" + getNodeHttpPort(participant);

                Map<String, Object> requestBody = new HashMap<>();
                requestBody.put("task_id", taskId);
                requestBody.put("dag_definition", dagDefinition);
                requestBody.put("self_party", participant);
                requestBody.put("participants", participants);
                requestBody.put("parameters", request.getParameters());
                requestBody.put("data_sources", request.getInputs());

                NodeExecuteResponse response = callNodeHttp(nodeAddress, requestBody);

                if (!response.isSuccess()) {
                    throw new RuntimeException("节点 " + resolveNodeName(participant) + " 执行失败: " + response.getError());
                }
            }

            taskRepository.updateResult(taskId, "{\"status\":\"ok\",\"type\":\"FEDERATED_LEARNING\"}");

        } catch (Exception e) {
            log.error("联邦学习任务执行失败: {}", e.getMessage());
            taskRepository.updateResult(taskId, "{\"status\":\"error\",\"message\":\"" + e.getMessage() + "\"}");
            throw new RuntimeException("联邦学习执行失败: " + e.getMessage(), e);
        }
    }

    /**
     * 执行 DAG 任务
     */
    public void executeDag(String taskId, TaskRequest request) {
        log.info("执行 DAG 任务: {}", taskId);
        try {
            List<String> participants = request.getParticipants();
            String dagDefinition = request.getParameters() != null ?
                (String) request.getParameters().get("dag_definition") : null;

            if (dagDefinition == null) {
                throw new RuntimeException("DAG 任务需要 dag_definition 参数");
            }

            // 如果没有指定参与方，只发送给 head 节点
            if (participants == null || participants.isEmpty()) {
                participants = Collections.singletonList(defaultNodeHost);
            }

            // 日志和 DAG 解析变量，在 try 外声明以便 catch 块访问
            this.dagLogEntries = new ArrayList<>();
            List<Map<String, Object>> dagNodes = new ArrayList<>();
            List<List<String>> dagEdges = new ArrayList<>();
            long startTs = System.currentTimeMillis();

            try {
                Map<String, Object> dagDef = objectMapper.readValue(dagDefinition, Map.class);
                if (dagDef.get("nodes") instanceof List) {
                    for (Object n : (List<?>) dagDef.get("nodes")) {
                        if (n instanceof Map) dagNodes.add((Map<String, Object>) n);
                    }
                }
                if (dagDef.get("edges") instanceof List) {
                    for (Object e : (List<?>) dagDef.get("edges")) {
                        if (e instanceof Map) {
                            Map<?, ?> edge = (Map<?, ?>) e;
                            dagEdges.add(Arrays.asList(String.valueOf(edge.get("from")), String.valueOf(edge.get("to"))));
                        }
                    }
                }
            } catch (Exception ex) {
                log.warn("解析 dag_definition 失败: {}", ex.getMessage());
            }

            // 判断是否是 PSI 类型任务（需要同时分发到所有节点）
            boolean isPsiTask = dagNodes.stream()
                .anyMatch(n -> {
                    String compId = String.valueOf(n.getOrDefault("compId", n.getOrDefault("comp_id", "")));
                    return "psi".equals(compId) || "psi_tp".equals(compId) || "unbalance_psi".equals(compId);
                });

            if (isPsiTask) {
                // PSI 类型任务：先分发到所有节点，再收集结果
                executeDagForPsiParticipants(taskId, dagDefinition, participants, request);
            } else {
                // 非 PSI 类型任务：顺序分发
                executeDagSequential(taskId, dagDefinition, participants, request);
            }

            writeExecutionLog(taskId, this.dagLogEntries);

            // 检查是否有任何失败或仍在运行
            boolean anyFailed = this.dagLogEntries.stream()
                .anyMatch(e -> "失败".equals(e.get("stage")) || "部分失败".equals(e.get("stage")));
            boolean anyRunning = this.dagLogEntries.stream()
                .anyMatch(e -> "运行中".equals(e.get("stage")));
            if (anyFailed) {
                taskRepository.updateStatus(taskId, TaskStatus.FAILED);
                taskRepository.updateResult(taskId, "{\"status\":\"partial_failed\",\"message\":\"部分节点执行失败\"}");
            } else if (anyRunning) {
                taskRepository.updateStatus(taskId, TaskStatus.RUNNING);
                taskRepository.updateResult(taskId, "{\"status\":\"running\",\"message\":\"等待多方协调完成\"}");
            } else {
                taskRepository.updateStatus(taskId, TaskStatus.COMPLETED);
                try {
                    if (lastResponse != null) {
                        Map<String, Object> responseResults = lastResponse.getResults();
                        if (responseResults != null && !responseResults.isEmpty()) {
                            Map<String, Object> resultMap = new HashMap<>();
                            resultMap.put("status", "ok");
                            resultMap.put("type", "COMPONENT_DAG");
                            resultMap.put("nodeResults", responseResults);
                            taskRepository.updateResult(taskId, objectMapper.writeValueAsString(resultMap));
                        } else {
                            taskRepository.updateResult(taskId, "{\"status\":\"ok\",\"type\":\"COMPONENT_DAG\"}");
                        }
                    }
                } catch (Exception e) {
                    log.warn("写入结果失败: {}", e.getMessage());
                }
            }
        } catch (Exception e) {
            log.error("DAG 任务执行失败: {}", e.getMessage(), e);
            taskRepository.updateStatus(taskId, TaskStatus.FAILED);
            taskRepository.updateResult(taskId, "{\"status\":\"error\",\"message\":\"" + e.getMessage() + "\"}");
            throw e;
        }
    }

    /**
     * PSI 类型任务：同时分发到所有节点
     */
    private void executeDagForPsiParticipants(String taskId, String dagDefinition, List<String> participants, TaskRequest request) {
        log.info("执行 PSI DAG 任务（并行分发）: {}", taskId);

        // 1. 先同时分发 DAG 到所有节点
        Map<String, Future<NodeExecuteResponse>> responseFutures = new ConcurrentHashMap<>();
        Map<String, String> nodeAddresses = new ConcurrentHashMap<>();

        for (String participant : participants) {
            String participantName = resolveNodeName(participant);
            String nodeAddress = mapNodeNameToHost(participant) + ":" + getNodeHttpPort(participant);
            nodeAddresses.put(participant, nodeAddress);

            String customizedDagDefinition = buildCustomizedDagForParty(dagDefinition, participant, participants);
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("task_id", taskId);
            requestBody.put("dag_definition", customizedDagDefinition);
            requestBody.put("self_party", participant);
            requestBody.put("participants", participants);
            requestBody.put("parameters", request.getParameters());
            requestBody.put("data_sources", request.getInputs());

            long dispatchTs = System.currentTimeMillis();
            this.dagLogEntries.add(Map.of(
                "ts", dispatchTs,
                "stage", "分发",
                "level", "INFO",
                "nodeId", participant,
                "nodeName", participantName,
                "message", "【" + participantName + "】节点：分发 DAG 到 " + nodeAddress
            ));

            // 异步分发
            final String p = participant;
            responseFutures.put(p, CompletableFuture.supplyAsync(() ->
                callNodeHttp(nodeAddress, requestBody)
            ));
        }

        // 2. 收集所有节点结果
        for (String participant : participants) {
            String participantName = resolveNodeName(participant);
            try {
                NodeExecuteResponse response = responseFutures.get(participant).get(300, TimeUnit.SECONDS);
                lastResponse = response;

                if (!response.isSuccess()) {
                    this.dagLogEntries.add(Map.of(
                        "ts", System.currentTimeMillis(),
                        "stage", "失败",
                        "level", "ERROR",
                        "nodeId", participant,
                        "nodeName", participantName,
                        "error", response.getError(),
                        "message", "【" + participantName + "】节点：执行失败 -> " + response.getError()
                    ));
                } else {
                    // 检查组件执行结果
                    Map<String, Object> results = response.getResults();
                    boolean hasError = false;
                    boolean isRunning = false;
                    if (results != null) {
                        for (Map.Entry<String, Object> entry : results.entrySet()) {
                            String nodeId = entry.getKey();
                            Object nodeResult = entry.getValue();
                            Map<String, Object> nodeResultMap = null;
                            if (nodeResult instanceof Map) {
                                nodeResultMap = (Map<String, Object>) nodeResult;
                            }
                            String compId = nodeResultMap != null ? String.valueOf(nodeResultMap.get("component")) : "unknown";
                            String status = nodeResultMap != null ? String.valueOf(nodeResultMap.get("status")) : "unknown";
                            boolean nodeOk = "ok".equals(status);
                            boolean nodeSkipped = "skipped".equals(status);
                            boolean nodeRunning = "running".equals(status);
                            if (!nodeOk && !nodeSkipped && !nodeRunning) {
                                hasError = true;
                            }
                            this.dagLogEntries.add(Map.of(
                                "ts", System.currentTimeMillis(),
                                "stage", nodeOk ? "成功" : (nodeSkipped ? "跳过" : (nodeRunning ? "等待中" : "失败")),
                                "level", nodeOk || nodeRunning ? "INFO" : "ERROR",
                                "dagNodeId", nodeId,
                                "compId", compId,
                                "nodeId", participant,
                                "nodeName", participantName,
                                "message", nodeSkipped
                                    ? ("【" + participantName + "】节点：跳过组件 " + compId + "（上游失败）")
                                    : ("【" + participantName + "】节点：执行组件 " + compId + " -> " + status)
                            ));
                            if (nodeRunning) isRunning = true;
                        }
                    }
                    // 检查整体状态
                    String responseStatus = response.getStatus();
                    if ("partial_failed".equals(responseStatus) || "error".equals(responseStatus)) {
                        hasError = true;
                    }
                    if ("running".equals(responseStatus)) {
                        isRunning = true;
                    }
                    if (hasError) {
                        this.dagLogEntries.add(Map.of(
                            "ts", System.currentTimeMillis(),
                            "stage", "部分失败",
                            "level", "WARN",
                            "nodeId", participant,
                            "nodeName", participantName,
                            "message", "【" + participantName + "】节点：存在失败的组件"
                        ));
                    } else if (isRunning) {
                        this.dagLogEntries.add(Map.of(
                            "ts", System.currentTimeMillis(),
                            "stage", "运行中",
                            "level", "INFO",
                            "nodeId", participant,
                            "nodeName", participantName,
                            "message", "【" + participantName + "】节点：执行中，等待其他参与方完成"
                        ));
                    }
                }
            } catch (Exception e) {
                log.error("节点 {} 执行异常: {}", participant, e.getMessage());
                this.dagLogEntries.add(Map.of(
                    "ts", System.currentTimeMillis(),
                    "stage", "失败",
                    "level", "ERROR",
                    "nodeId", participant,
                    "nodeName", participantName,
                    "error", e.getMessage(),
                    "message", "【" + participantName + "】节点：执行异常 -> " + e.getMessage()
                ));
            }
        }
    }

    /**
     * 非 PSI 类型任务：顺序分发
     */
    private void executeDagSequential(String taskId, String dagDefinition, List<String> participants, TaskRequest request) {
        try {
            for (String participant : participants) {
                String participantName = resolveNodeName(participant);
                String nodeAddress = mapNodeNameToHost(participant) + ":" + getNodeHttpPort(participant);

                String customizedDagDefinition = buildCustomizedDagForParty(dagDefinition, participant, participants);
                Map<String, Object> requestBody = new HashMap<>();
                requestBody.put("task_id", taskId);
                requestBody.put("dag_definition", customizedDagDefinition);
                requestBody.put("self_party", participant);
                requestBody.put("participants", participants);
                requestBody.put("parameters", request.getParameters());
                requestBody.put("data_sources", request.getInputs());

                this.dagLogEntries.add(Map.of(
                    "ts", System.currentTimeMillis(),
                    "stage", "分发",
                    "level", "INFO",
                    "nodeId", participant,
                    "nodeName", participantName,
                    "message", "【" + participantName + "】节点：分发 DAG 到 " + nodeAddress
                ));

                NodeExecuteResponse response = callNodeHttp(nodeAddress, requestBody);
                lastResponse = response;

                if (!response.isSuccess()) {
                    long failTs = System.currentTimeMillis();
                    this.dagLogEntries.add(Map.of(
                        "ts", failTs,
                        "stage", "失败",
                        "level", "ERROR",
                        "nodeId", participant,
                        "nodeName", participantName,
                        "error", response.getError(),
                        "message", "【" + participantName + "】节点：执行失败 -> " + response.getError()
                    ));
                    writeExecutionLog(taskId, this.dagLogEntries);
                    throw new RuntimeException("【" + participantName + "】节点：执行失败 -> " + response.getError());
                }

                // 节点执行成功，写 NODE_END 日志
                Map<String, Object> results = response.getResults();
                boolean hasError = false;
                if (results != null) {
                    for (Map.Entry<String, Object> entry : results.entrySet()) {
                        String nodeId = entry.getKey();
                        Object nodeResult = entry.getValue();
                        Map<String, Object> nodeResultMap = null;
                        if (nodeResult instanceof Map) {
                            nodeResultMap = (Map<String, Object>) nodeResult;
                        }
                        String compId = nodeResultMap != null ? String.valueOf(nodeResultMap.get("component")) : "unknown";
                        String status = nodeResultMap != null ? String.valueOf(nodeResultMap.get("status")) : "unknown";
                        boolean nodeOk = "ok".equals(status);
                        // "skipped" 也是一种状态，不算错误（是上游失败导致的）
                        boolean nodeSkipped = "skipped".equals(status);
                        // "running" 是 PSI 多方协调的正常状态，不算错误
                        boolean nodeRunning = "running".equals(status);
                        if (!nodeOk && !nodeSkipped && !nodeRunning) {
                            hasError = true;
                        }
                        this.dagLogEntries.add(Map.of(
                            "ts", System.currentTimeMillis(),
                            "stage", nodeOk ? "成功" : (nodeSkipped ? "跳过" : (nodeRunning ? "等待中" : "失败")),
                            "level", nodeOk || nodeRunning ? "INFO" : "ERROR",
                            "dagNodeId", nodeId,
                            "compId", compId,
                            "nodeId", participant,
                            "nodeName", participantName,
                            "message", nodeSkipped
                                ? ("【" + participantName + "】节点：跳过组件 " + compId + "（上游失败）")
                                : ("【" + participantName + "】节点：执行组件 " + compId + " -> " + status)
                        ));
                    }
                }

                // 检查 Python 节点返回的整体状态
                String responseStatus = response.getStatus();
                if ("partial_failed".equals(responseStatus) || "error".equals(responseStatus)) {
                    hasError = true;
                }

                // PSI 'running' 状态表示等待其他参与方，不算完成也不算失败
                boolean isRunning = "running".equals(responseStatus);

                // 如果有任何节点失败，整体标记为部分失败
                if (hasError) {
                    this.dagLogEntries.add(Map.of(
                        "ts", System.currentTimeMillis(),
                        "stage", "部分失败",
                        "level", "WARN",
                        "nodeId", participant,
                        "nodeName", participantName,
                        "message", "【" + participantName + "】节点：存在失败的组件"
                    ));
                }

                // 如果是 'running' 状态，记录但不标记失败，继续等待
                if (isRunning) {
                    this.dagLogEntries.add(Map.of(
                        "ts", System.currentTimeMillis(),
                        "stage", "运行中",
                        "level", "INFO",
                        "nodeId", participant,
                        "nodeName", participantName,
                        "message", "【" + participantName + "】节点：执行中，等待其他参与方完成"
                    ));
                }
            }

            writeExecutionLog(taskId, this.dagLogEntries);

            // 检查是否有任何失败或仍在运行
            boolean anyFailed = this.dagLogEntries.stream()
                .anyMatch(e -> "失败".equals(e.get("stage")) || "部分失败".equals(e.get("stage")));
            boolean anyRunning = this.dagLogEntries.stream()
                .anyMatch(e -> "运行中".equals(e.get("stage")));
            if (anyFailed) {
                taskRepository.updateStatus(taskId, TaskStatus.FAILED);
                taskRepository.updateResult(taskId, "{\"status\":\"partial_failed\",\"message\":\"部分节点执行失败\"}");
            } else if (anyRunning) {
                taskRepository.updateStatus(taskId, TaskStatus.RUNNING);
                taskRepository.updateResult(taskId, "{\"status\":\"running\",\"message\":\"等待多方协调完成\"}");
            } else {
                taskRepository.updateStatus(taskId, TaskStatus.COMPLETED);
                try {
                    if (lastResponse != null) {
                        Map<String, Object> responseResults = lastResponse.getResults();
                        if (responseResults != null && !responseResults.isEmpty()) {
                            Map<String, Object> resultMap = new HashMap<>();
                            resultMap.put("status", "ok");
                            resultMap.put("type", "COMPONENT_DAG");
                            resultMap.put("nodeResults", responseResults);
                            taskRepository.updateResult(taskId, objectMapper.writeValueAsString(resultMap));
                        } else {
                            taskRepository.updateResult(taskId, "{\"status\":\"ok\",\"type\":\"COMPONENT_DAG\"}");
                        }
                    } else {
                        taskRepository.updateResult(taskId, "{\"status\":\"ok\",\"type\":\"COMPONENT_DAG\"}");
                    }
                } catch (Exception e) {
                    taskRepository.updateResult(taskId, "{\"status\":\"ok\",\"type\":\"COMPONENT_DAG\"}");
                }
            }

        } catch (Exception e) {
            log.error("DAG 任务执行失败: {}", e.getMessage());
            if (this.dagLogEntries == null) {
                this.dagLogEntries = new ArrayList<>();
            }
            this.dagLogEntries.add(Map.of(
                "ts", System.currentTimeMillis(),
                "stage", "失败",
                "level", "ERROR",
                "message", "任务执行失败: " + e.getMessage()
            ));
            writeExecutionLog(taskId, this.dagLogEntries);
            taskRepository.updateResult(taskId, "{\"status\":\"error\",\"message\":\"" + e.getMessage() + "\"}");
            taskRepository.updateStatus(taskId, TaskStatus.FAILED);
            throw new RuntimeException("DAG 执行失败: " + e.getMessage(), e);
        }
    }

    private void writeExecutionLog(String taskId, List<Map<String, Object>> logEntries) {
        try {
            String logJson = objectMapper.writeValueAsString(logEntries);
            taskRepository.updateExecutionLog(taskId, logJson);
        } catch (Exception e) {
            log.warn("写 execution_log 失败: {}", e.getMessage());
        }
    }

    /**
     * 通过 HTTP POST 调用 Python 节点
     */
    private NodeExecuteResponse callNodeHttp(String nodeAddress, Map<String, Object> requestBody) {
        try {
            String url = "http://" + nodeAddress + "/api/v1/execute/dag";
            log.info("调用节点 HTTP 端点: {}", url);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

            ResponseEntity<String> response = restTemplate.postForEntity(url, entity, String.class);

            if (response.getStatusCode().is2xxSuccessful()) {
                return objectMapper.readValue(response.getBody(), NodeExecuteResponse.class);
            } else {
                return new NodeExecuteResponse(false, null, null, null, "HTTP " + response.getStatusCode());
            }

        } catch (Exception e) {
            log.error("调用节点 {} 失败: {}", nodeAddress, e.getMessage());
            return new NodeExecuteResponse(false, null, null, null, e.getMessage());
        }
    }

    private String mapNodeNameToHost(String nodeId) {
        // 先尝试从数据库查 node_name，避免硬编码映射
        String resolved = resolveNodeName(nodeId);
        // 从中文名回退到 host：如果 resolved 是纯中文名，视为 nodeId 直接返回
        // 如果 resolved 和 nodeId 相同说明没查到，仍用 nodeId
        if (resolved == null || resolved.isEmpty()) return nodeId;
        // 最终按 resolved 名字做 host 映射（兼容旧的 switch 逻辑）
        switch (resolved.toLowerCase()) {
            case "node-a":
            case "医院":
            case "node-hospital":
                return "node-a";
            case "node-b":
            case "医疗研究所":
            case "node-research":
                return "node-b";
            case "node-c":
            case "保险公司":
            case "node-insurance":
                return "node-c";
            default:
                return resolved; // 直接用 resolved 名字作为 host
        }
    }

    /**
     * 获取节点的 HTTP 端口
     * Docker 映射：node-a:50052, node-b:50053, node-c:50054
     */
    private int getNodeHttpPort(String nodeId) {
        // Docker 端口映射：node-a:50052, node-b:50053, node-c:50054
        // HTTP 服务在不同主机端口上：50052->node-a, 50053->node-b, 50054->node-c
        String resolved = resolveNodeName(nodeId);
        if (resolved == null || resolved.isEmpty()) resolved = nodeId;
        switch (resolved.toLowerCase()) {
            case "node-a":
            case "医院":
            case "node-hospital":
                return 50052;
            case "node-b":
            case "医疗研究所":
            case "node-research":
                return 50053;
            case "node-c":
            case "保险公司":
            case "node-insurance":
                return 50054;
            default:
                return 50052; // 默认使用 node-a 的 HTTP 端口
        }
    }

    /**
     * 把节点的 ID 解析成 nodeName（查 msp_nodes 表，cache 后避免反复打 DB）。
     */
    private String resolveNodeName(String nodeId) {
        if (nodeId == null || nodeId.isEmpty()) return "";
        String cached = nodeNameCache.get(nodeId);
        if (cached != null) return cached;
        try {
            String name = jdbcTemplate.queryForObject(
                "SELECT node_name FROM msp_nodes WHERE node_id = ?",
                (rs, rowNum) -> rs.getString(1),
                nodeId
            );
            String resolved = name != null ? name : nodeId;
            nodeNameCache.put(nodeId, resolved);
            return resolved;
        } catch (Exception e) {
            log.debug("resolveNodeName miss for {}, fallback to nodeId", nodeId);
            nodeNameCache.put(nodeId, nodeId);
            return nodeId;
        }
    }

    /**
     * 构建 PSI DAG 定义
     */
    private String buildPsiDagDefinition(String taskId, TaskRequest request) {
        Map<String, Object> dag = new LinkedHashMap<>();
        dag.put("name", "psi_dag_" + taskId);
        dag.put("nodes", Arrays.asList(
            createNode("read_0", "read_table", "读取数据",
                Map.of("datasource_id", "input_ds", "table_name", "input_table", "limit", 1000)),
            createNode("psi_0", "psi", "PSI对齐",
                Map.of("key_column", "id", "psi_type", "ecdh")),
            createNode("write_0", "write_table", "写入结果",
                Map.of("output_datasource_id", "output_ds", "output_table", "psi_result"))
        ));
        dag.put("edges", Arrays.asList(
            Map.of("from", "read_0", "to", "psi_0"),
            Map.of("from", "psi_0", "to", "write_0")
        ));
        try {
            return objectMapper.writeValueAsString(dag);
        } catch (Exception e) {
            throw new RuntimeException("构建 PSI DAG 失败", e);
        }
    }

    /**
     * 构建 MPC DAG 定义
     */
    private String buildMpcDagDefinition(String taskId, TaskRequest request) {
        Map<String, Object> dag = new LinkedHashMap<>();
        dag.put("name", "mpc_dag_" + taskId);
        dag.put("nodes", Arrays.asList(
            createNode("read_a", "read_table", "读取数据A",
                Map.of("datasource_id", "input_ds_a", "table_name", "input_a", "limit", 1000)),
            createNode("read_b", "read_table", "读取数据B",
                Map.of("datasource_id", "input_ds_b", "table_name", "input_b", "limit", 1000)),
            createNode("mpc_0", "mpc", "MPC计算",
                Map.of("mpc_type", "addition")),
            createNode("write_0", "write_table", "写入结果",
                Map.of("output_datasource_id", "output_ds", "output_table", "mpc_result"))
        ));
        dag.put("edges", Arrays.asList(
            Map.of("from", "read_a", "to", "mpc_0"),
            Map.of("from", "read_b", "to", "mpc_0"),
            Map.of("from", "mpc_0", "to", "write_0")
        ));
        try {
            return objectMapper.writeValueAsString(dag);
        } catch (Exception e) {
            throw new RuntimeException("构建 MPC DAG 失败", e);
        }
    }

    /**
     * 构建联邦学习 DAG 定义
     */
    private String buildFlDagDefinition(String taskId, TaskRequest request) {
        Map<String, Object> dag = new LinkedHashMap<>();
        dag.put("name", "fl_dag_" + taskId);
        dag.put("nodes", Arrays.asList(
            createNode("read_0", "read_table", "读取特征",
                Map.of("datasource_id", "input_ds", "table_name", "features", "limit", 1000)),
            createNode("fl_0", "vertical_fl", "联邦训练",
                Map.of("model_type", "logistic_regression")),
            createNode("write_0", "write_table", "写入模型",
                Map.of("output_datasource_id", "output_ds", "output_table", "fl_model"))
        ));
        dag.put("edges", Arrays.asList(
            Map.of("from", "read_0", "to", "fl_0"),
            Map.of("from", "fl_0", "to", "write_0")
        ));
        try {
            return objectMapper.writeValueAsString(dag);
        } catch (Exception e) {
            throw new RuntimeException("构建 FL DAG 失败", e);
        }
    }

    private Map<String, Object> createNode(String nodeId, String compId, String label, Map<String, Object> attrs) {
        Map<String, Object> node = new LinkedHashMap<>();
        node.put("nodeId", nodeId);
        node.put("compId", compId);
        node.put("label", label);
        node.put("attrs", attrs);
        return node;
    }

    /**
     * 为指定节点生成定制化的 DAG 定义
     * 只包含该节点需要执行的组件，过滤掉不属于该节点的组件
     *
     * @param dagDefinition 原始 DAG 定义（JSON 字符串）
     * @param party 当前节点名称
     * @param allParties 所有参与节点列表
     * @return 定制化后的 DAG 定义（JSON 字符串）
     */
    private String buildCustomizedDagForParty(String dagDefinition, String party, List<String> allParties) {
        try {
            Map<String, Object> dagDef = objectMapper.readValue(dagDefinition, Map.class);
            List<Map<String, Object>> originalNodes = (List<Map<String, Object>>) dagDef.get("nodes");
            List<Map<String, Object>> originalEdges = (List<Map<String, Object>>) dagDef.get("edges");

            if (originalNodes == null || originalNodes.isEmpty()) {
                return dagDefinition;
            }

            boolean isHead = (allParties != null && !allParties.isEmpty() && party.equals(allParties.get(0)));

            // 过滤后的节点和边
            List<Map<String, Object>> filteredNodes = new ArrayList<>();
            Set<String> keptNodeIds = new HashSet<>();

            for (Map<String, Object> node : originalNodes) {
                String compId = String.valueOf(node.get("compId"));
                String nodeId = String.valueOf(node.get("nodeId"));
                // 同时支持 attrs 和 config两种字段名
                Map<String, Object> attrs = node.containsKey("attrs") ?
                    (Map<String, Object>) node.get("attrs") : new HashMap<>();
                if (attrs.isEmpty() && node.containsKey("config")) {
                    attrs = (Map<String, Object>) node.get("config");
                }

                boolean shouldKeep = false;

                if ("psi".equals(compId) || "psi_tp".equals(compId) || "unbalance_psi".equals(compId)) {
                    // PSI 组件：所有参与节点都需要执行
                    shouldKeep = true;
                } else if ("write_csv".equals(compId) || "write_table".equals(compId)) {
                    // 写入组件：只在 head 节点执行
                    shouldKeep = isHead;
                } else if ("read_table".equals(compId)) {
                    // 读取组件：根据 datasource_id 判断数据所有权
                    String datasourceId = String.valueOf(attrs.getOrDefault("datasource_id", ""));
                    String owner = getDatasourceOwner(datasourceId);
                    // 只有数据源属于当前节点时才执行
                    shouldKeep = party.equals(owner);
                } else {
                    // 其他组件：只在 head 节点执行
                    shouldKeep = isHead;
                }

                if (shouldKeep) {
                    filteredNodes.add(node);
                    keptNodeIds.add(nodeId);
                }
            }

            // 过滤边：只保留起点和终点都在 keptNodeIds 中的边
            List<Map<String, Object>> filteredEdges = new ArrayList<>();
            if (originalEdges != null) {
                for (Map<String, Object> edge : originalEdges) {
                    String from = String.valueOf(edge.get("from"));
                    String to = String.valueOf(edge.get("to"));
                    if (keptNodeIds.contains(from) && keptNodeIds.contains(to)) {
                        filteredEdges.add(edge);
                    }
                }
            }

            // 构建定制化的 DAG
            Map<String, Object> customizedDag = new LinkedHashMap<>();
            customizedDag.put("name", dagDef.get("name"));
            customizedDag.put("nodes", filteredNodes);
            customizedDag.put("edges", filteredEdges);

            log.info("[{}] DAG 定制化: 原始节点数={}, 定制后节点数={}, 边数={}",
                party, originalNodes.size(), filteredNodes.size(), filteredEdges.size());

            return objectMapper.writeValueAsString(customizedDag);

        } catch (Exception e) {
            log.error("生成定制化 DAG 失败: {}", e.getMessage());
            // 失败时返回原始 DAG
            return dagDefinition;
        }
    }

    /**
     * 根据 datasource_id 判断数据所有权属于哪个节点
     */
    private String getDatasourceOwner(String datasourceId) {
        if (datasourceId == null || datasourceId.isEmpty()) {
            return null;
        }
        // 从数据库查询数据源所属节点
        try {
            String nodeId = jdbcTemplate.queryForObject(
                "SELECT node_id FROM msp_datasources WHERE datasource_id = ?",
                (rs, rowNum) -> rs.getString("node_id"),
                datasourceId
            );
            if (nodeId != null) {
                return nodeId;
            }
        } catch (Exception e) {
            log.debug("查询 datasource owner 失败: {}", e.getMessage());
        }
        // 回退：根据 datasource_id 推算
        String lowerDsId = datasourceId.toLowerCase();
        if (lowerDsId.contains("hosp") || lowerDsId.contains("hospital")) {
            return "node-hospital";
        } else if (lowerDsId.contains("insurance")) {
            return "node-insurance";
        } else if (lowerDsId.contains("research")) {
            return "node-research";
        }
        return null;
    }

    /**
     * 节点执行响应
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class NodeExecuteResponse {
        private boolean success;
        private String taskId;
        private String status;
        private Map<String, Object> results;
        private String error;

        public NodeExecuteResponse() {}

        public NodeExecuteResponse(boolean success, String taskId, String status, Map<String, Object> results, String error) {
            this.success = success;
            this.taskId = taskId;
            this.status = status;
            this.results = results;
            this.error = error;
        }

        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }
        public String getTaskId() { return taskId; }
        public void setTaskId(String taskId) { this.taskId = taskId; }
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        public Map<String, Object> getResults() { return results; }
        public void setResults(Map<String, Object> results) { this.results = results; }
        public String getError() { return error; }
        public void setError(String error) { this.error = error; }
    }
}