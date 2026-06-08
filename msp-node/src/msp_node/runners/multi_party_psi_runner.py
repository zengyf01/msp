"""
多方 PSI 运行器
支持 3 方及以上的 PSI 运算
"""

from typing import Dict, Any, List
from .base_runner import BaseRunner


class MultiPartyPSIRunner(BaseRunner):
    """多方 PSI 运行器 - 支持 3 方及以上"""

    def __init__(self, config: dict):
        super().__init__(config)
        self.secretflow_adapter = None
        self.parties = config.get("parties", [])

    def initialize(self) -> None:
        """初始化 SecretFlow 适配器"""
        from ..adapters.secretflow_adapter import SecretFlowAdapter

        self.secretflow_adapter = SecretFlowAdapter(self.config)
        self.secretflow_adapter.initialize()
        self.initialized = True

    def _do_run(self, task_id: str, inputs: dict, params: dict) -> dict:
        """执行多方 PSI"""
        if self.secretflow_adapter is None:
            raise RuntimeError("SecretFlow adapter not initialized")

        key_column = params.get("key_column", "id")
        psi_type = params.get("psi_type", "bc20")

        try:
            # 获取所有参与方的数据
            data_sources = inputs.get("data_sources", {})

            if len(self.parties) < 3:
                return {
                    "status": "error",
                    "error": "Multi-party PSI requires at least 3 parties"
                }

            # 根据 PSI 类型执行
            if psi_type == "bc20":
                return self._execute_bc20_psi(task_id, data_sources, key_column)
            elif psi_type == "ecdh_3party":
                return self._execute_ecdh_3party_psi(task_id, data_sources, key_column)
            else:
                return {
                    "status": "error",
                    "error": f"Unsupported psi_type for multi-party: {psi_type}"
                }

        except Exception as e:
            return {
                "status": "error",
                "error": str(e),
                "psi_type": psi_type
            }

    def _execute_bc20_psi(self, task_id: str, data_sources: dict, key_column: str) -> dict:
        """执行 BC20 多方 PSI"""
        try:
            from secretflow.preprocessing.psi import BC20PSI

            # 创建 BC20 PSI 实例
            psi = BC20PSI(
                self.secretflow_adapter.spursu,
                task_id=task_id,
                key_column=key_column,
            )

            # 执行多方 PSI
            result = psi.run(data=data_sources)

            return {
                "status": "ok",
                "psi_type": "bc20",
                "matched_count": len(result) if result is not None else 0,
                "num_parties": len(self.parties)
            }

        except ImportError:
            return {
                "status": "error",
                "error": "BC20PSI not available, please upgrade SecretFlow",
                "psi_type": "bc20"
            }
        except Exception as e:
            return {
                "status": "error",
                "error": str(e),
                "psi_type": "bc20"
            }

    def _execute_ecdh_3party_psi(self, task_id: str, data_sources: dict, key_column: str) -> dict:
        """执行 ECDH 三方 PSI"""
        try:
            from secretflow.preprocessing.psi import ECDH3PSI

            psi = ECDH3PSI(
                self.secretflow_adapter.spursu,
                task_id=task_id,
                key_column=key_column,
            )

            result = psi.run(data=data_sources)

            return {
                "status": "ok",
                "psi_type": "ecdh_3party",
                "matched_count": len(result) if result is not None else 0,
                "num_parties": 3
            }

        except ImportError:
            return {
                "status": "error",
                "error": "ECDH3PSI not available in current SecretFlow version",
                "psi_type": "ecdh_3party"
            }
        except Exception as e:
            return {
                "status": "error",
                "error": str(e),
                "psi_type": "ecdh_3party"
            }

    def cleanup(self) -> None:
        """清理资源"""
        if self.secretflow_adapter is not None:
            self.secretflow_adapter.cleanup()
        self.initialized = False
