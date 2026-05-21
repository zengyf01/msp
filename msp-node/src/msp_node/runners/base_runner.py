"""
基础运行器
最小化复用SecretFlow能力
"""

class BaseRunner:
    """运行器基类"""

    def __init__(self, config: dict):
        self.config = config
        self.initialized = False

    def initialize(self) -> None:
        """初始化运行器"""
        raise NotImplementedError

    def run(self, task_id: str, inputs: dict, params: dict) -> dict:
        """执行任务 - 自动处理初始化"""
        if not self.initialized:
            self.initialize()
        result = self._do_run(task_id, inputs, params)
        return {
            "task_id": task_id,
            "status": "completed",
            **result
        }

    def _do_run(self, task_id: str, inputs: dict, params: dict) -> dict:
        """子类实现核心执行逻辑"""
        raise NotImplementedError

    def health_check(self) -> bool:
        """健康检查"""
        return self.initialized

    def cleanup(self) -> None:
        """清理资源"""
        pass