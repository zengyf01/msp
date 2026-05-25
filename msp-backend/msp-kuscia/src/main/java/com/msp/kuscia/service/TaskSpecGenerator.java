package com.msp.kuscia.service;

import com.msp.common.core.DataSource;
import com.msp.common.core.TaskRequest;
import com.msp.common.core.TaskType;
import com.msp.kuscia.dto.KusciaTaskSpec;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 任务规格生成器 - 将MSP TaskRequest转换为Kuscia任务规格
 */
@Component
public class TaskSpecGenerator {

    private static final Map<TaskType, String> TASK_TYPE_MAP = Map.of(
        TaskType.PSI, "psi",
        TaskType.MPC, "mpc",
        TaskType.FEDERATED_LEARNING, "fl",
        TaskType.VERTICAL_FL, "vertical_fl"
    );

    private static final int DEFAULT_TIMEOUT_SECONDS = 3600;
    private static final int PSI_TIMEOUT_SECONDS = 600;

    /**
     * 生成Kuscia任务规格
     */
    public KusciaTaskSpec generate(String taskId, TaskRequest request) {
        KusciaTaskSpec spec = new KusciaTaskSpec();
        spec.setTaskId(taskId);
        spec.setName(request.getName() != null ? request.getName() : "msp-task-" + taskId);
        spec.setType(TASK_TYPE_MAP.getOrDefault(request.getType(), "mpc"));
        spec.setPriority("0");
        spec.setTimeoutSeconds(getTimeoutForTaskType(request.getType()));
        spec.setAnnotations(buildAnnotations(taskId, request));
        spec.setParties(buildParties(request.getParticipants()));
        spec.setInputs(buildInputs(request.getInputs()));
        spec.setParameters(request.getParameters());
        return spec;
    }

    private int getTimeoutForTaskType(TaskType type) {
        if (type == TaskType.PSI) {
            return PSI_TIMEOUT_SECONDS;
        }
        return DEFAULT_TIMEOUT_SECONDS;
    }

    private Map<String, String> buildAnnotations(String taskId, TaskRequest request) {
        Map<String, String> annotations = new HashMap<>();
        annotations.put("msp.task.id", taskId);
        annotations.put("msp.task.type", request.getType() != null ? request.getType().name() : "");
        annotations.put("msp.algorithm", request.getAlgorithm() != null ? request.getAlgorithm() : "");
        // 纵向联邦学习相关 annotations
        if (request.getLabelParty() != null) {
            annotations.put("msp.label.party", request.getLabelParty());
        }
        if (request.getLabelColumn() != null) {
            annotations.put("msp.label.column", request.getLabelColumn());
        }
        if (request.getModelType() != null) {
            annotations.put("msp.model.type", request.getModelType());
        }
        return annotations;
    }

    private List<KusciaTaskSpec.Party> buildParties(List<String> participants) {
        if (participants == null || participants.isEmpty()) {
            throw new IllegalArgumentException("Participants cannot be empty");
        }
        return participants.stream()
            .map(nodeName -> {
                KusciaTaskSpec.Party party = new KusciaTaskSpec.Party();
                party.setName(nodeName);
                party.setNodeID(nodeName);
                party.setCode(generateCodeForParty(nodeName));
                return party;
            })
            .collect(Collectors.toList());
    }

    private String generateCodeForParty(String nodeName) {
        return String.format("""
            import secretflow as sf
            import pandas as pd

            # Initialize SecretFlow party
            sf.init(address='kuscia-master:8083', party='%s', config={'debug': False})

            print(f'Party %s initialized')
            """, nodeName, nodeName);
    }

    private KusciaTaskSpec.TaskInput buildInputs(Map<String, DataSource> inputs) {
        if (inputs == null || inputs.isEmpty()) {
            return null;
        }

        KusciaTaskSpec.TaskInput taskInput = new KusciaTaskSpec.TaskInput();
        Map<String, Object> inputData = new HashMap<>();

        inputs.forEach((key, ds) -> {
            Map<String, Object> inputSpec = new HashMap<>();
            inputSpec.put("type", ds.getType() != null ? ds.getType().name() : "FILE");
            inputSpec.put("uri", buildDataUri(ds));
            if (ds.getColumns() != null) {
                inputSpec.put("columns", ds.getColumns());
            }
            inputData.put(key, inputSpec);
        });

        taskInput.setData(inputData);
        return taskInput;
    }

    private String buildDataUri(DataSource ds) {
        if (ds.getType() == DataSource.DataSourceType.MYSQL) {
            return String.format("mysql://%s:%d/%s/%s",
                ds.getHost(), ds.getPort(), ds.getDatabase(), ds.getTableName());
        } else if (ds.getType() == DataSource.DataSourceType.POSTGRESQL) {
            return String.format("postgresql://%s:%d/%s/%s",
                ds.getHost(), ds.getPort(), ds.getDatabase(), ds.getTableName());
        } else if (ds.getType() == DataSource.DataSourceType.FILE) {
            return String.format("file:///data/%s/%s", ds.getNodeId(), ds.getTableName());
        }
        return "file:///data/unknown";
    }
}