"""
DAG 执行器
用于执行组件组成的 DAG 工作流
"""

from typing import Dict, Any, List
from .base_runner import BaseRunner


class DAGExecutor(BaseRunner):
    """DAG 执行器"""

    def __init__(self, config: dict):
        super().__init__(config)
        self.secretflow_adapter = None
        self._component_registry = {}

    def initialize(self) -> None:
        """初始化 SecretFlow 适配器"""
        from ..adapters.secretflow_adapter import SecretFlowAdapter

        self.secretflow_adapter = SecretFlowAdapter(self.config)
        self.secretflow_adapter.initialize()
        self._register_components()
        self.initialized = True

    def _register_components(self) -> None:
        """注册所有可用的组件"""
        self._component_registry = {
            # 数据输入
            "read_table": self._execute_read_table,

            # 数据对齐
            "psi": self._execute_psi,
            "psi_tp": self._execute_psi_tp,
            "unbalance_psi": self._execute_unbalance_psi,

            # 预处理
            "binning": self._execute_binning,
            "vert_binning": self._execute_vert_binning,
            "woe_binning": self._execute_woe_binning,
            "sample": self._execute_sample,

            # 线性模型
            "ss_glm_train": self._execute_ss_glm_train,
            "ss_glm_predict": self._execute_ss_glm_predict,

            # 树模型
            "sgb_train": self._execute_sgb_train,
            "sgb_predict": self._execute_sgb_predict,

            # 统计分析
            "biclassification_eval": self._execute_biclassification_eval,
            "regression_eval": self._execute_regression_eval,

            # 数据输出
            "write_table": self._execute_write_table,
        }

    def run(self, task_id: str, dag_def: dict, params: dict) -> dict:
        """执行 DAG 工作流"""
        if not self.initialized:
            self.initialize()

        # 验证 DAG 定义
        if "nodes" not in dag_def or "edges" not in dag_def:
            raise ValueError("Invalid DAG definition: missing 'nodes' or 'edges'")

        # 解析并执行 DAG
        execution_plan = self._plan_execution(dag_def)
        results = {}
        node_results = {}

        for node in execution_plan:
            result = self._execute_node(node, node_results)
            node_results[node["node_id"]] = result

        return {
            "task_id": task_id,
            "status": "completed",
            "results": node_results
        }

    def _do_run(self, task_id: str, inputs: dict, params: dict) -> dict:
        """执行入口 - 支持兼容旧接口"""
        dag_def = params.get("dag", {})
        return self.run(task_id, dag_def, params)

    def _plan_execution(self, dag_def: dict) -> List[Dict[str, Any]]:
        """
        生成拓扑排序的执行计划
        使用 Kahn 算法
        """
        nodes = {n["node_id"]: n for n in dag_def["nodes"]}
        edges = dag_def.get("edges", [])

        # 构建入度表和邻接表
        in_degree = {node_id: 0 for node_id in nodes}
        adjacency = {node_id: [] for node_id in nodes}

        for edge in edges:
            from_node = edge.get("from")
            to_node = edge.get("to")
            if from_node in nodes and to_node in nodes:
                adjacency[from_node].append(to_node)
                in_degree[to_node] += 1

        # Kahn 算法
        queue = [n for n in nodes if in_degree[n] == 0]
        execution_order = []

        while queue:
            current = queue.pop(0)
            execution_order.append(nodes[current])

            for neighbor in adjacency[current]:
                in_degree[neighbor] -= 1
                if in_degree[neighbor] == 0:
                    queue.append(neighbor)

        # 检查是否有环
        if len(execution_order) != len(nodes):
            raise ValueError("DAG contains cycle")

        return execution_order

    def _execute_node(self, node: dict, upstream_results: dict) -> dict:
        """执行单个节点"""
        node_id = node.get("node_id")
        comp_id = node.get("comp_id")

        if comp_id not in self._component_registry:
            return {
                "status": "error",
                "error": f"Unknown component: {comp_id}"
            }

        # 获取上游输出作为输入
        inputs = self._build_inputs(node, upstream_results)

        # 获取节点参数
        attrs = node.get("attrs", {})

        # 调用组件
        handler = self._component_registry[comp_id]
        return handler(inputs, attrs)

    def _build_inputs(self, node: dict, upstream_results: dict) -> dict:
        """构建节点输入"""
        inputs = {}
        input_bindings = node.get("inputs", [])

        for binding in input_bindings:
            input_name = binding.get("input_name")
            source_node_id = binding.get("source_node_id")
            output_name = binding.get("output_name")

            if source_node_id in upstream_results:
                source_result = upstream_results[source_node_id]
                if output_name in source_result:
                    inputs[input_name] = source_result[output_name]

        return inputs

    # 组件执行方法
    def _execute_read_table(self, inputs: dict, attrs: dict) -> dict:
        """读取数据表"""
        return {
            "status": "ok",
            "component": "read_table",
            "data": inputs.get("data")
        }

    def _execute_psi(self, inputs: dict, attrs: dict) -> dict:
        """PSI 组件"""
        key_column = attrs.get("key_column", "id")
        psi_type = attrs.get("psi_type", "ecdh")

        return {
            "status": "ok",
            "component": "psi",
            "key_column": key_column,
            "psi_type": psi_type
        }

    def _execute_psi_tp(self, inputs: dict, attrs: dict) -> dict:
        """三方 PSI 组件"""
        key_column = attrs.get("key_column", "id")
        return {
            "status": "ok",
            "component": "psi_tp",
            "key_column": key_column
        }

    def _execute_unbalance_psi(self, inputs: dict, attrs: dict) -> dict:
        """不平衡 PSI 组件"""
        return {
            "status": "ok",
            "component": "unbalance_psi"
        }

    def _execute_binning(self, inputs: dict, attrs: dict) -> dict:
        """分箱组件"""
        return {
            "status": "ok",
            "component": "binning"
        }

    def _execute_vert_binning(self, inputs: dict, attrs: dict) -> dict:
        """纵向分箱组件"""
        return {
            "status": "ok",
            "component": "vert_binning"
        }

    def _execute_woe_binning(self, inputs: dict, attrs: dict) -> dict:
        """WOE 分箱组件"""
        return {
            "status": "ok",
            "component": "woe_binning"
        }

    def _execute_sample(self, inputs: dict, attrs: dict) -> dict:
        """采样组件"""
        return {
            "status": "ok",
            "component": "sample"
        }

    def _execute_ss_glm_train(self, inputs: dict, attrs: dict) -> dict:
        """SS-GLM 训练组件"""
        return {
            "status": "ok",
            "component": "ss_glm_train"
        }

    def _execute_ss_glm_predict(self, inputs: dict, attrs: dict) -> dict:
        """SS-GLM 预测组件"""
        return {
            "status": "ok",
            "component": "ss_glm_predict"
        }

    def _execute_sgb_train(self, inputs: dict, attrs: dict) -> dict:
        """SGB 训练组件"""
        return {
            "status": "ok",
            "component": "sgb_train"
        }

    def _execute_sgb_predict(self, inputs: dict, attrs: dict) -> dict:
        """SGB 预测组件"""
        return {
            "status": "ok",
            "component": "sgb_predict"
        }

    def _execute_biclassification_eval(self, inputs: dict, attrs: dict) -> dict:
        """二分类评估组件"""
        return {
            "status": "ok",
            "component": "biclassification_eval"
        }

    def _execute_regression_eval(self, inputs: dict, attrs: dict) -> dict:
        """回归评估组件"""
        return {
            "status": "ok",
            "component": "regression_eval"
        }

    def _execute_write_table(self, inputs: dict, attrs: dict) -> dict:
        """写入数据表组件"""
        return {
            "status": "ok",
            "component": "write_table"
        }

    def cleanup(self) -> None:
        """清理资源"""
        if self.secretflow_adapter is not None:
            self.secretflow_adapter.cleanup()
        self.initialized = False
