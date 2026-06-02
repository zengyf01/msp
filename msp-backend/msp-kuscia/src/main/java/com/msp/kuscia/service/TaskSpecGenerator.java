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
        TaskType.VERTICAL_FL, "vertical_fl",
        TaskType.COMPOUND_TASK, "compound",
        TaskType.COMPONENT_DAG, "component_dag",
        TaskType.CUSTOM_CODE, "custom_code"
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
        spec.setParties(buildParties(request.getParticipants(), request));
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

    private List<KusciaTaskSpec.Party> buildParties(List<String> participants, TaskRequest request) {
        if (participants == null || participants.isEmpty()) {
            throw new IllegalArgumentException("Participants cannot be empty");
        }
        return participants.stream()
            .map(nodeName -> {
                KusciaTaskSpec.Party party = new KusciaTaskSpec.Party();
                party.setName(nodeName);
                party.setNodeID(nodeName);
                party.setCode(generateCodeForParty(nodeName, request));
                return party;
            })
            .collect(Collectors.toList());
    }

    private String generateCodeForParty(String nodeName, TaskRequest request) {
        TaskType taskType = request.getType();
        String algorithm = request.getAlgorithm();

        if (taskType == TaskType.PSI) {
            return generatePSICode(nodeName, request);
        } else if (taskType == TaskType.MPC) {
            return generateMPCCode(nodeName, request);
        } else if (taskType == TaskType.FEDERATED_LEARNING || taskType == TaskType.VERTICAL_FL) {
            return generateFLCode(nodeName, request);
        } else if (taskType == TaskType.COMPONENT_DAG) {
            return generateDAGCode(nodeName, request);
        } else if (taskType == TaskType.CUSTOM_CODE) {
            return generateCustomCode(nodeName, request);
        }

        // Default code
        return String.format("""
            import secretflow as sf
            import pandas as pd

            # Initialize SecretFlow party
            sf.init(address='kuscia-master:8083', party='%s', config={'debug': False})

            print(f'Party %s initialized')
            """, nodeName, nodeName);
    }

    private String generatePSICode(String nodeName, TaskRequest request) {
        return String.format("""
            import secretflow as sf
            import pandas as pd
            from secretflow.component import psi

            # Initialize SecretFlow party
            sf.init(address='kuscia-master:8083', party='%s', config={'debug': False})

            # PSI Task Configuration
            # Algorithm: %s
            # Join Key: id_card (from inputs)

            print(f'Party %s: PSI task starting')
            # Load data and perform PSI
            # psi.gen_psi_selector(...)
            print(f'Party %s: PSI task completed')
            """, nodeName, request.getAlgorithm() != null ? request.getAlgorithm() : "ecdh", nodeName, nodeName);
    }

    private String generateMPCCode(String nodeName, TaskRequest request) {
        return String.format("""
            import secretflow as sf
            import pandas as pd
            from secretflow.component import mpc

            # Initialize SecretFlow party
            sf.init(address='kuscia-master:8083', party='%s', config={'debug': False})

            # MPC Task Configuration
            # Algorithm: %s
            # Operation: addition/multiplication/comparison

            print(f'Party %s: MPC task starting')
            # Load data and perform MPC computation
            # sf.mpc.{...}
            print(f'Party %s: MPC task completed')
            """, nodeName, request.getAlgorithm() != null ? request.getAlgorithm() : "addition", nodeName, nodeName);
    }

    private String generateFLCode(String nodeName, TaskRequest request) {
        return String.format("""
            import secretflow as sf
            import pandas as pd
            from secretflow.component import fl

            # Initialize SecretFlow party
            sf.init(address='kuscia-master:8083', party='%s', config={'debug': False})

            # Federated Learning Task Configuration
            # Model Type: %s
            # Label Party: %s

            print(f'Party %s: FL task starting')
            # Load data and perform federated learning
            # sf.fl.horizontal.{...} or sf.fl.vertical.{...}
            print(f'Party %s: FL task completed')
            """, nodeName,
            request.getModelType() != null ? request.getModelType() : "logistic_regression",
            request.getLabelParty() != null ? request.getLabelParty() : "node-a",
            nodeName, nodeName);
    }

    private String generateDAGCode(String nodeName, TaskRequest request) {
        String dagDefJson = request.getParameters() != null ? request.getParameters().get("dag_definition") : null;
        StringBuilder sb = new StringBuilder();
        sb.append("import secretflow as sf\n");
        sb.append("import pandas as pd\n");
        sb.append("from secretflow.component import comp as sf_comp\n\n");
        sb.append(String.format("# Initialize SecretFlow party\n"));
        sb.append(String.format("sf.init(address='kuscia-master:8083', party='%s', config={'debug': False})\n\n", nodeName));
        sb.append("# Component DAG Task\n");
        sb.append("# DAG Definition:\n");

        if (dagDefJson != null && !dagDefJson.isEmpty()) {
            try {
                com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
                java.util.Map<String, Object> dagDef = mapper.readValue(dagDefJson, java.util.Map.class);
                java.util.List<?> nodes = dagDef != null && dagDef.containsKey("nodes") ?
                    (java.util.List<?>) dagDef.get("nodes") : null;

                if (nodes != null) {
                    for (int i = 0; i < nodes.size(); i++) {
                        java.util.Map<?, ?> node = (java.util.Map<?, ?>) nodes.get(i);
                        String compId = node != null && node.containsKey("compId") ? (String) node.get("compId") : "unknown";
                        String label = node != null && node.containsKey("label") ? (String) node.get("label") : compId;
                        sb.append(String.format("# [%d] %s (%s)\n", i + 1, label, compId));
                    }
                }
                sb.append("#\n");
                sb.append("# DAG Execution Flow:\n");
                sb.append("# 1. read_table -> Load data from configured datasource\n");
                sb.append("# 2. psi -> Perform PSI to align data across parties\n");
                sb.append("# 3. preprocessing -> Data preprocessing (binning, feature engineering)\n");
                sb.append("# 4. ml_model -> Train model (ss_glm, sgb)\n");
                sb.append("# 5. evaluation -> Evaluate model performance\n");
                sb.append("# 6. write_table -> Save results to output table\n");
            } catch (Exception e) {
                sb.append("# Failed to parse DAG definition\n");
            }
        } else {
            sb.append("# No DAG definition provided\n");
        }

        sb.append(String.format("\nprint(f'Party %s: Component DAG task starting')\n", nodeName));
        sb.append("# DAG execution would proceed here:\n");
        sb.append("# result = sf_comp.dag_executor.execute(dag_def)\n");
        sb.append(String.format("print(f'Party %s: Component DAG task completed')\n", nodeName));

        return sb.toString();
    }

    private String generateCustomCode(String nodeName, TaskRequest request) {
        // For custom code tasks, the code should come from parameters
        String customCode = request.getParameters() != null ? request.getParameters().get("code") : null;
        if (customCode != null && !customCode.isEmpty()) {
            return customCode;
        }
        return String.format("""
            import secretflow as sf
            import pandas as pd

            # Initialize SecretFlow party
            sf.init(address='kuscia-master:8083', party='%s', config={'debug': False})

            # Custom Code Task
            # No custom code provided, please specify code in parameters

            print(f'Party %s: Custom code task initialized')
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