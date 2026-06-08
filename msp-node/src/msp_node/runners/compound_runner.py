"""
复合任务运行器
协调 PSI+FL+MPC 三阶段执行
"""

from typing import Dict, Any
from .base_runner import BaseRunner


class CompoundRunner(BaseRunner):
    """复合任务运行器 - 协调 PSI+FL+MPC 三阶段"""

    def __init__(self, config: dict):
        super().__init__(config)
        self.psi_runner = None
        self.fl_runner = None
        self.mpc_runner = None

    def initialize(self) -> None:
        """初始化各个阶段的运行器"""
        from .psi_runner import PSIRunner
        from .fl_runner import FLRunner
        from .mpc_runner import MPCRunner

        self.psi_runner = PSIRunner(self.config)
        self.fl_runner = FLRunner(self.config)
        self.mpc_runner = MPCRunner(self.config)

        self.psi_runner.initialize()
        self.fl_runner.initialize()
        self.mpc_runner.initialize()

        self.initialized = True

    def _do_run(self, task_id: str, inputs: dict, params: dict) -> dict:
        """执行复合任务"""
        stages = params.get("stages", [])

        if not stages:
            return {
                "status": "error",
                "error": "No stages defined in compound task"
            }

        results = {}

        for idx, stage in enumerate(stages):
            stage_type = stage.get("type")
            stage_params = stage.get("params", {})
            stage_order = stage.get("order", idx)

            try:
                if stage_type == "PSI":
                    result = self._execute_psi_stage(task_id, inputs, stage_params)
                elif stage_type == "FL":
                    result = self._execute_fl_stage(task_id, inputs, stage_params)
                elif stage_type == "MPC":
                    result = self._execute_mpc_stage(task_id, inputs, stage_params)
                else:
                    result = {
                        "status": "error",
                        "error": f"Unknown stage type: {stage_type}"
                    }

                results[stage_order] = {
                    "stage": stage_type,
                    "status": "completed",
                    **result
                }

            except Exception as e:
                results[stage_order] = {
                    "stage": stage_type,
                    "status": "error",
                    "error": str(e)
                }

        return {
            "status": "completed",
            "stages": results
        }

    def _execute_psi_stage(self, task_id: str, inputs: dict, params: dict) -> dict:
        """执行 PSI 阶段"""
        return self.psi_runner._do_run(task_id, inputs, params)

    def _execute_fl_stage(self, task_id: str, inputs: dict, params: dict) -> dict:
        """执行 FL 阶段"""
        return self.fl_runner._do_run(task_id, inputs, params)

    def _execute_mpc_stage(self, task_id: str, inputs: dict, params: dict) -> dict:
        """执行 MPC 阶段"""
        return self.mpc_runner._do_run(task_id, inputs, params)

    def cleanup(self) -> None:
        """清理资源"""
        if self.psi_runner:
            self.psi_runner.cleanup()
        if self.fl_runner:
            self.fl_runner.cleanup()
        if self.mpc_runner:
            self.mpc_runner.cleanup()
        self.initialized = False
