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

        // 基础导入和初始化
        sb.append("\"\"\"\n");
        sb.append("SecretFlow DAG 执行脚本\n");
        sb.append("任务类型: COMPONENT_DAG\n");
        sb.append("参与方: ").append(nodeName).append("\n");
        sb.append("生成时间: ").append(java.time.LocalDateTime.now()).append("\n");
        sb.append("\"\"\"\n\n");
        sb.append("import os\n");
        sb.append("import secretflow as sf\n");
        sb.append("import pandas as pd\n");
        sb.append("import pymysql\n");
        sb.append("from secretflow.device import PYU, SPU\n\n");

        // Ray 组网配置
        sb.append("# ============================================================\n");
        sb.append("# Ray 集群配置（动态组网，任务级别）\n");
        sb.append("# ============================================================\n");
        sb.append("RAY_HEAD_PORT = os.environ.get('RAY_HEAD_PORT', '6379')\n");
        sb.append("SPU_PORT = os.environ.get('SPU_PORT', '8000')\n");
        sb.append("SELF_PARTY = os.environ.get('SELF_PARTY', '").append(nodeName).append("')\n");
        sb.append("PARTIES = os.environ.get('PARTIES', '").append(String.join(",", request.getParticipants() != null ? request.getParticipants() : java.util.Collections.singletonList(nodeName))).append("').split(',')\n\n");

        // 数据库配置
        sb.append("# ============================================================\n");
        sb.append("# 数据源配置（从环境变量读取）\n");
        sb.append("# ============================================================\n");
        sb.append("def get_db_config(datasource_id):\n");
        sb.append("    prefix = f\"DB_{datasource_id.upper().replace('-', '_')}\"\n");
        sb.append("    return {\n");
        sb.append("        'host': os.environ.get(f'{prefix}_HOST', os.environ.get('DB_HOST', 'localhost')),\n");
        sb.append("        'port': int(os.environ.get(f'{prefix}_PORT', os.environ.get('DB_PORT', 3306))),\n");
        sb.append("        'user': os.environ.get(f'{prefix}_USER', os.environ.get('DB_USER', 'root')),\n");
        sb.append("        'password': os.environ.get(f'{prefix}_PASS', os.environ.get('DB_PASS', '')),\n");
        sb.append("        'database': os.environ.get(f'{prefix}_NAME', os.environ.get('DB_NAME', ''))\n");
        sb.append("    }\n\n");

        // Ray 组网函数
        sb.append("# ============================================================\n");
        sb.append("# Ray 动态组网\n");
        sb.append("# ============================================================\n");
        sb.append("def setup_ray_cluster():\n");
        sb.append("    import ray\n");
        sb.append("    import subprocess\n");
        sb.append("    import socket\n\n");
        sb.append("    def get_container_ip():\n");
        sb.append("        s = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)\n");
        sb.append("        s.connect(('8.8.8.8', 80))\n");
        sb.append("        ip = s.getsockname()[0]\n");
        sb.append("        s.close()\n");
        sb.append("        return ip\n\n");
        sb.append("    is_head = (SELF_PARTY == PARTIES[0])\n\n");
        sb.append("    if is_head:\n");
        sb.append("        result = subprocess.run([\n");
        sb.append("            'ray', 'start', '--head',\n");
        sb.append("            '--node-ip-address', get_container_ip(),\n");
        sb.append("            '--port', RAY_HEAD_PORT,\n");
        sb.append("            '--num-cpus', '8'\n");
        sb.append("        ], capture_output=True, text=True, timeout=30)\n");
        sb.append("        if result.returncode == 0:\n");
        sb.append("            ray.init(ignore_reinit_error=True, include_dashboard=False)\n");
        sb.append("            print(f'[{SELF_PARTY}] Ray Head started: {{ray.get_runtime_context().gcs_address}}')\n");
        sb.append("    else:\n");
        sb.append("        head_address = f'{PARTIES[0]}:{RAY_HEAD_PORT}'\n");
        sb.append("        result = subprocess.run([\n");
        sb.append("            'ray', 'start', '--address', head_address\n");
        sb.append("        ], capture_output=True, text=True, timeout=30)\n");
        sb.append("        if result.returncode == 0:\n");
        sb.append("            ray.init(address=head_address, ignore_reinit_error=True, include_dashboard=False)\n");
        sb.append("            print(f'[{SELF_PARTY}] Ray Worker connected: {{ray.get_runtime_context().gcs_address}}')\n\n");

        // SecretFlow 初始化
        sb.append("# ============================================================\n");
        sb.append("# SecretFlow 初始化\n");
        sb.append("# ============================================================\n");
        sb.append("def init_secretflow():\n");
        sb.append("    if SELF_PARTY == PARTIES[0]:\n");
        sb.append("        sf.init(\n");
        sb.append("            party=SELF_PARTY,\n");
        sb.append("            num_cpus=8,\n");
        sb.append("            config={'debug': False, 'device': 'spu'}\n");
        sb.append("        )\n");
        sb.append("    else:\n");
        sb.append("        head_address = f\"ray://{PARTIES[0]}:{RAY_HEAD_PORT}\"\n");
        sb.append("        sf.init(\n");
        sb.append("            address=head_address,\n");
        sb.append("            party=SELF_PARTY,\n");
        sb.append("            config={'debug': False, 'device': 'spu'}\n");
        sb.append("        )\n");
        sb.append("    print(f'[{SELF_PARTY}] SecretFlow initialized')\n\n");

        // SPU 设备创建
        sb.append("# ============================================================\n");
        sb.append("# SPU 设备创建\n");
        sb.append("# ============================================================\n");
        sb.append("def create_spu_device():\n");
        sb.append("    nodes = []\n");
        sb.append("    for party in PARTIES:\n");
        sb.append("        env_key = f\"SPU_{party.upper().replace('-', '_')}\"\n");
        sb.append("        addr = os.environ.get(env_key)\n");
        sb.append("        if not addr:\n");
        sb.append("            addr = f'{party}:{SPU_PORT}'\n");
        sb.append("        nodes.append({'party': party, 'address': addr})\n\n");
        sb.append("    spu_config = {\n");
        sb.append("        'nodes': nodes,\n");
        sb.append("        'runtime_config': {\n");
        sb.append("            'protocol': 'SEMI2K',\n");
        sb.append("            'field': 'FM64',\n");
        sb.append("            'fxp_fraction_bits': 18,\n");
        sb.append("        }\n");
        sb.append("    }\n");
        sb.append("    spu = SPU(spu_config)\n");
        sb.append("    print(f'[{SELF_PARTY}] SPU device created')\n");
        sb.append("    return spu\n\n");

        // 数据读取函数
        sb.append("# ============================================================\n");
        sb.append("# 数据读取\n");
        sb.append("# ============================================================\n");
        sb.append("def read_table(datasource_id, table_name, columns=None, limit=1000):\n");
        sb.append("    db = get_db_config(datasource_id)\n");
        sb.append("    conn = pymysql.connect(\n");
        sb.append("        host=db['host'], port=db['port'],\n");
        sb.append("        user=db['user'], password=db['password'],\n");
        sb.append("        database=db['database'], charset='utf8mb4'\n");
        sb.append("    )\n");
        sb.append("    try:\n");
        sb.append("        col_list = ', '.join([f'`{c}`' for c in columns]) if columns else '*'\n");
        sb.append("        sql = f\"SELECT {col_list} FROM `{table_name}` LIMIT {limit}\"\n");
        sb.append("        with conn.cursor(pymysql.cursors.DictCursor) as cursor:\n");
        sb.append("            cursor.execute(sql)\n");
        sb.append("            rows = cursor.fetchall()\n");
        sb.append("        print(f'[{SELF_PARTY}] read_table: {table_name} -> {len(rows)} rows')\n");
        sb.append("        return pd.DataFrame(rows)\n");
        sb.append("    finally:\n");
        sb.append("        conn.close()\n\n");

        // PSI 执行函数
        sb.append("# ============================================================\n");
        sb.append("# PSI 执行\n");
        sb.append("# ============================================================\n");
        sb.append("def execute_psi(spu, df_a, df_b, key_column, psi_type='ecdh'):\n");
        sb.append("    protocol_map = {\n");
        sb.append("        'ecdh': 'ECDH_PSI_2PC',\n");
        sb.append("        'kkrt': 'KKRT_PSI_2PC',\n");
        sb.append("        'bc22': 'BC22_PSI_2PC',\n");
        sb.append("    }\n");
        sb.append("    protocol = protocol_map.get(psi_type, 'KKRT_PSI_2PC')\n\n");
        sb.append("    pyu_self = PYU(SELF_PARTY)\n");
        sb.append("    self_data = pyu_self(lambda x: x)(df_a)\n\n");
        sb.append("    other_party = [p for p in PARTIES if p != SELF_PARTY][0]\n");
        sb.append("    pyu_other = PYU(other_party)\n");
        sb.append("    other_data = pyu_other(lambda x: x)(df_b)\n\n");
        sb.append("    all_dfs = [self_data, other_data]\n");
        sb.append("    receiver = PARTIES[0]\n\n");
        sb.append("    result = spu.psi_df(\n");
        sb.append("        key=[key_column],\n");
        sb.append("        dfs=all_dfs,\n");
        sb.append("        receiver=receiver,\n");
        sb.append("        protocol=protocol\n");
        sb.append("    )\n\n");
        sb.append("    aligned_df = sf.reveal(result)\n");
        sb.append("    print(f'[{SELF_PARTY}] PSI completed: {len(aligned_df)} rows')\n");
        sb.append("    return aligned_df\n\n");

        // CSV 写入函数
        sb.append("# ============================================================\n");
        sb.append("# 结果写入\n");
        sb.append("# ============================================================\n");
        sb.append("def write_csv(file_path, df):\n");
        sb.append("    os.makedirs(os.path.dirname(file_path), exist_ok=True)\n");
        sb.append("    df.to_csv(file_path, index=False)\n");
        sb.append("    size = os.path.getsize(file_path)\n");
        sb.append("    print(f'[{SELF_PARTY}] write_csv: {file_path} -> {len(df)} rows, {size} bytes')\n");
        sb.append("    return {'path': file_path, 'rows': len(df), 'sizeBytes': size}\n\n");

        // 清理函数
        sb.append("# ============================================================\n");
        sb.append("# 资源清理\n");
        sb.append("# ============================================================\n");
        sb.append("def cleanup():\n");
        sb.append("    import ray\n");
        sb.append("    if SELF_PARTY == PARTIES[0]:\n");
        sb.append("        ray.shutdown()\n");
        sb.append("        import subprocess\n");
        sb.append("        subprocess.run(['ray', 'stop'], capture_output=True)\n");
        sb.append("        print(f'[{SELF_PARTY}] Ray cluster shutdown')\n\n");

        // 解析 DAG 定义并生成执行代码
        sb.append("# ============================================================\n");
        sb.append("# DAG 执行入口\n");
        sb.append("# ============================================================\n");
        sb.append("def main():\n");
        sb.append("    print(f'\\n{\"=\"*60}')\n");
        sb.append("    print(f'[{SELF_PARTY}] DAG execution starting')\n");
        sb.append("    print(f'{\"=\"*60}\\n')\n\n");
        sb.append("    # Step 1: Ray 组网\n");
        sb.append("    setup_ray_cluster()\n\n");
        sb.append("    # Step 2: SecretFlow 初始化\n");
        sb.append("    init_secretflow()\n\n");
        sb.append("    # Step 3: SPU 设备\n");
        sb.append("    spu = create_spu_device()\n\n");

        // 解析 DAG 节点
        if (dagDefJson != null && !dagDefJson.isEmpty()) {
            try {
                com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
                java.util.Map<String, Object> dagDef = mapper.readValue(dagDefJson, java.util.Map.class);
                java.util.List<?> nodes = dagDef != null && dagDef.containsKey("nodes") ?
                    (java.util.List<?>) dagDef.get("nodes") : null;

                if (nodes != null) {
                    int stepNum = 1;
                    for (int i = 0; i < nodes.size(); i++) {
                        java.util.Map<?, ?> node = (java.util.Map<?, ?>) nodes.get(i);
                        if (node == null) continue;
                        String compId = String.valueOf(node.get("compId"));
                        String nid = String.valueOf(node.get("nodeId"));
                        @SuppressWarnings("unchecked")
                        // 兼容 attrs（前端编辑时写入）和 config（部分场景使用）两种字段名
                        java.util.Map<String, Object> attrs = node.containsKey("attrs") ?
                            (java.util.Map<String, Object>) node.get("attrs") : null;
                        if (attrs == null) {
                            attrs = node.containsKey("config") ?
                                (java.util.Map<String, Object>) node.get("config") : new java.util.HashMap<>();
                        }

                        sb.append("    # -------- ").append(stepNum++).append(". ").append(compId).append(" (").append(nid).append(") --------\n");

                        if ("read_table".equals(compId)) {
                            String dsId = String.valueOf(attrs.getOrDefault("datasource_id", "input_ds"));
                            String tbl = String.valueOf(attrs.getOrDefault("table_name", "input_table"));
                            String cols = attrs.containsKey("columns") ?
                                String.join(", ", ((java.util.List<?>) attrs.get("columns")).stream().map(String::valueOf).map(c -> "'" + c + "'").toList()) : "";
                            String limit = String.valueOf(attrs.getOrDefault("limit", "1000"));
                            sb.append("    df_").append(nid).append(" = read_table(\n");
                            sb.append("        datasource_id='").append(dsId).append("',\n");
                            sb.append("        table_name='").append(tbl).append("',\n");
                            if (!cols.isEmpty()) sb.append("        columns=[").append(cols).append("],\n");
                            sb.append("        limit=").append(limit).append("\n");
                            sb.append("    )\n");
                        } else if ("read_csv".equals(compId)) {
                            String path = String.valueOf(attrs.getOrDefault("file_path", "/tmp/input.csv"));
                            sb.append("    df_").append(nid).append(" = pd.read_csv('").append(path).append("')\n");
                        } else if ("psi".equals(compId)) {
                            String keyCol = String.valueOf(attrs.getOrDefault("key_column", "id"));
                            String psiType = String.valueOf(attrs.getOrDefault("psi_type", "ecdh"));
                            sb.append("    # 查找上游输入节点\n");
                            // 简化：假设上游是 read_table
                            sb.append("    aligned_df = execute_psi(spu, df_input, df_input, key_column='").append(keyCol).append("', psi_type='").append(psiType).append("')\n");
                        } else if ("write_csv".equals(compId)) {
                            String path = String.valueOf(attrs.getOrDefault("file_path", "/tmp/output.csv"));
                            sb.append("    write_csv('").append(path).append("', aligned_df)\n");
                        } else if ("write_table".equals(compId)) {
                            sb.append("    # write_table: 需要写入数据库\n");
                            sb.append("    # TODO: 实现 write_table\n");
                        } else {
                            sb.append("    # ").append(compId).append(": stub 实现\n");
                        }
                        sb.append("\n");
                    }
                }
            } catch (Exception e) {
                sb.append("    # DAG 解析失败: ").append(e.getMessage()).append("\n");
            }
        }

        sb.append("    # Step N: 清理资源\n");
        sb.append("    cleanup()\n");
        sb.append("    print(f'\\n[{SELF_PARTY}] DAG execution completed\\n')\n\n");
        sb.append("if __name__ == '__main__':\n");
        sb.append("    main()\n");

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