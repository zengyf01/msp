"""
联邦学习运行器
基于SecretFlow FL框架实现联邦学习
"""

from typing import Dict, Any
from .base_runner import BaseRunner


class FLRunner(BaseRunner):
    """联邦学习运行器"""

    def __init__(self, config: dict):
        super().__init__(config)
        self.model_type = config.get("model_type", "logistic_regression")
        self.secretflow_adapter = None

    def initialize(self) -> None:
        """初始化SecretFlow FL"""
        from .secretflow_adapter import SecretFlowAdapter

        # 使用配置初始化SecretFlow适配器
        self.secretflow_adapter = SecretFlowAdapter(self.config)
        self.secretflow_adapter.initialize()
        self.initialized = True

    def _do_run(self, task_id: str, inputs: dict, params: dict) -> dict:
        """执行联邦学习任务 - 根据 task type 分发"""
        if self.secretflow_adapter is None:
            raise RuntimeError("SecretFlow adapter not initialized")

        try:
            # 获取任务类型参数
            fl_type = params.get("fl_type", "horizontal")  # horizontal / vertical

            if fl_type == "vertical":
                # 纵向联邦学习
                result = self.secretflow_adapter.execute_vertical_fl(
                    task_id=task_id,
                    inputs=inputs,
                    params=params
                )
            else:
                # 横向联邦学习 (默认)
                result = self.secretflow_adapter.execute_federated_learning(
                    task_id=task_id,
                    inputs=inputs,
                    params=params
                )
            return result

        except Exception as e:
            return {
                "status": "error",
                "error": str(e),
            }

    def cleanup(self) -> None:
        """清理资源"""
        if self.secretflow_adapter is not None:
            self.secretflow_adapter.cleanup()
        self.initialized = False