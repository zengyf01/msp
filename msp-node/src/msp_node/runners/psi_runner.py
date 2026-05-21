"""
PSI运行器
基于SecretFlow SPU实现真实的隐私集合求交
"""

from typing import Dict, Any
from .base_runner import BaseRunner


class PSIRunner(BaseRunner):
    """PSI运行器 - 隐私集合求交"""

    def __init__(self, config: dict):
        super().__init__(config)
        self.psi_type = config.get("psi_type", "ecdh")
        self.secretflow_adapter = None

    def initialize(self) -> None:
        """初始化SecretFlow SPU"""
        from .secretflow_adapter import SecretFlowAdapter

        # 使用配置初始化SecretFlow适配器
        self.secretflow_adapter = SecretFlowAdapter(self.config)
        self.secretflow_adapter.initialize()
        self.initialized = True

    def _do_run(self, task_id: str, inputs: dict, params: dict) -> dict:
        """执行PSI任务"""
        if self.secretflow_adapter is None:
            raise RuntimeError("SecretFlow adapter not initialized")

        try:
            # 使用SecretFlow执行PSI
            result = self.secretflow_adapter.execute_psi(
                task_id=task_id,
                inputs=inputs,
                params={
                    **params,
                    "psi_type": self.psi_type,
                }
            )
            return result

        except Exception as e:
            return {
                "status": "error",
                "error": str(e),
                "psi_type": self.psi_type,
            }

    def cleanup(self) -> None:
        """清理资源"""
        if self.secretflow_adapter is not None:
            self.secretflow_adapter.cleanup()
        self.initialized = False