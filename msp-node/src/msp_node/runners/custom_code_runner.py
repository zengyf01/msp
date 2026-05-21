"""
自定义代码运行器
支持用户编写Python代码执行隐私计算任务
"""

import logging
from typing import Dict, Any
from .base_runner import BaseRunner

logger = logging.getLogger(__name__)


class CustomCodeRunner(BaseRunner):
    """自定义代码运行器"""

    def __init__(self, config: dict):
        super().__init__(config)
        self.secretflow_adapter = None

    def initialize(self) -> None:
        """初始化SecretFlow适配器"""
        from .secretflow_adapter import SecretFlowAdapter

        self.secretflow_adapter = SecretFlowAdapter(self.config)
        self.secretflow_adapter.initialize()
        self.initialized = True
        logger.info("CustomCodeRunner initialized")

    def _do_run(self, task_id: str, inputs: dict, params: dict) -> dict:
        """执行用户自定义代码"""
        if self.secretflow_adapter is None:
            raise RuntimeError("SecretFlow adapter not initialized")

        code = params.get("code")
        if not code:
            return {
                "status": "error",
                "error": "No code provided",
            }

        try:
            # 构建执行环境
            exec_globals = self._build_execution_globals()

            # 执行用户代码
            logger.info(f"Executing custom code for task {task_id}")
            exec(code, exec_globals)

            # 获取用户定义的run函数并执行
            if "run" in exec_globals and callable(exec_globals["run"]):
                result = exec_globals["run"]()
            else:
                result = {"status": "ok", "message": "Code executed successfully"}

            return {
                "status": "ok",
                "result": result,
            }

        except Exception as e:
            logger.error(f"Custom code execution failed: {e}")
            return {
                "status": "error",
                "error": str(e),
            }

    def _build_execution_globals(self) -> dict:
        """构建代码执行环境"""
        # 获取SecretFlow设备和适配器
        sf = self._get_secretflow_module()
        spu = None
        pyu = None

        if self.secretflow_adapter:
            spu = self.secretflow_adapter.spu_device
            pyu = self.secretflow_adapter.pyu_device

        return {
            # SecretFlow模块
            "sf": sf,
            # 设备
            "spu": spu,
            "pyu": pyu,
            # 输入数据
            "inputs": {},
            # 工具函数
            "print": print,
            "len": len,
            "range": range,
            "str": str,
            "int": int,
            "float": float,
            "list": list,
            "dict": dict,
            "tuple": tuple,
            "set": set,
            "bool": bool,
            "type": type,
            "isinstance": isinstance,
            "hasattr": hasattr,
            "getattr": getattr,
            "setattr": setattr,
        }

    def _get_secretflow_module(self):
        """获取SecretFlow模块"""
        try:
            import secretflow as sf
            return sf
        except ImportError:
            logger.warning("SecretFlow not installed, using mock")
            return self._create_mock_secretflow()

    def _create_mock_secretflow(self):
        """创建SecretFlow模拟对象"""

        class MockSF:
            class Device:
                pass

            class PYU:
                def __init__(self, party):
                    self.party = party

            class SPU:
                def __init__(self, config):
                    self.config = config

                def __getattr__(self, name):
                    return lambda *args, **kwargs: f"mock_{name}"

            @staticmethod
            def reveal(data):
                return "mock_revealed_data"

        return MockSF()

    def cleanup(self) -> None:
        """清理资源"""
        if self.secretflow_adapter is not None:
            self.secretflow_adapter.cleanup()
        self.initialized = False
        logger.info("CustomCodeRunner cleaned up")