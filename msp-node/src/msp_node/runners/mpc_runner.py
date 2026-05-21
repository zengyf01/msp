"""
MPC运行器
基于SecretFlow SPU实现真实的安全多方计算
"""

from typing import Dict, Any
from .base_runner import BaseRunner


class MPCRunner(BaseRunner):
    """MPC运行器 - 安全多方计算"""

    def __init__(self, config: dict):
        super().__init__(config)
        self.protocol = config.get("protocol", "semi2k")
        self.secretflow_adapter = None

    def initialize(self) -> None:
        """初始化SecretFlow SPU"""
        from .secretflow_adapter import SecretFlowAdapter

        # 使用配置初始化SecretFlow适配器
        self.secretflow_adapter = SecretFlowAdapter(self.config)
        self.secretflow_adapter.initialize()
        self.initialized = True

    def _do_run(self, task_id: str, inputs: dict, params: dict) -> dict:
        """执行MPC任务"""
        if self.secretflow_adapter is None:
            raise RuntimeError("SecretFlow adapter not initialized")

        try:
            # 使用SecretFlow执行MPC
            result = self.secretflow_adapter.execute_mpc(
                task_id=task_id,
                inputs=inputs,
                params={
                    **params,
                    "protocol": self.protocol,
                }
            )
            return result

        except Exception as e:
            return {
                "status": "error",
                "error": str(e),
                "protocol": self.protocol,
            }

    def cleanup(self) -> None:
        """清理资源"""
        if self.secretflow_adapter is not None:
            self.secretflow_adapter.cleanup()
        self.initialized = False