"""
SecureBoost 运行器
专门用于 SGB (SecureBoost) 模型的训练和预测
"""

from typing import Dict, Any
from .base_runner import BaseRunner


class SGBRunner(BaseRunner):
    """SecureBoost 运行器"""

    def __init__(self, config: dict):
        super().__init__(config)
        self.secretflow_adapter = None

    def initialize(self) -> None:
        """初始化SecretFlow适配器"""
        from .secretflow_adapter import SecretFlowAdapter

        self.secretflow_adapter = SecretFlowAdapter(self.config)
        self.secretflow_adapter.initialize()
        self.initialized = True

    def _do_run(self, task_id: str, inputs: dict, params: dict) -> dict:
        """执行SecureBoost训练"""
        if self.secretflow_adapter is None:
            raise RuntimeError("SecretFlow adapter not initialized")

        try:
            # 调用 adapter 的 train_sgb
            result = self.secretflow_adapter.train_sgb(inputs, params)
            return result

        except Exception as e:
            return {
                "status": "error",
                "error": str(e),
                "model_type": "secureboost",
            }

    def predict(self, model_path: str, data: dict) -> dict:
        """使用训练好的 SGB 模型进行预测"""
        # 预留接口 - 未来版本实现
        return {
            "status": "error",
            "error": "SGB predict not yet implemented",
        }

    def cleanup(self) -> None:
        """清理资源"""
        if self.secretflow_adapter is not None:
            self.secretflow_adapter.cleanup()
        self.initialized = False