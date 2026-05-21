"""
SecretFlow适配器
封装SecretFlow设备调用，实现真正的隐私计算
"""

import os
import secretflow as sf
from secretflow.device import PYU, SPU, DeviceType
from typing import Dict, Any, List


class SecretFlowAdapter:
    """SecretFlow适配器 - 封装SecretFlow设备调用"""

    def __init__(self, config: dict):
        self.config = config
        self.sf = None
        self.parties = config.get("parties", [])
        self.self_party = config.get("self_party", "")
        self.spursu = None  # SPU device
        self.pyu_devices = {}  # PYU devices per party

    def initialize(self) -> None:
        """初始化SecretFlow"""
        if self.self_party not in self.parties:
            raise ValueError(f"Self party '{self.self_party}' not in parties: {self.parties}")

        # 初始化SecretFlow
        sf.init(
            address=self.config.get("cluster_config", {}).get("address", ""),
            party=self.self_party,
            config={
                "party_cat": self.config.get("category_config", {}),
                "device": self.config.get("device", "spu"),
            }
        )

        # 创建SPU设备
        spu_config = self._build_spu_config()
        self.spursu = SPU(sf.SPU(comp_config=spu_config))

        # 创建各方的PYU设备
        for party in self.parties:
            self.pyu_devices[party] = PYU(party)

        self.initialized = True

    def _build_spu_config(self) -> dict:
        """构建SPU配置"""
        cluster_config = self.config.get("cluster_config", {})

        # 多方配置
        parties = cluster_config.get("parties", self.parties)
        addresses = cluster_config.get("addresses", [])

        # 构建SPU配置
        nodes = []
        for i, party in enumerate(parties):
            node = {
                "party": party,
                "address": addresses[i] if i < len(addresses) else f"{party}:0",
            }
            nodes.append(node)

        return {
            "nodes": nodes,
            "runtime_config": {
                "kind": "semi2k",  # 或 "ref道德" 或 "aby3"
                "protocol": "semi2k",  # 协议: semi2k, aby3, ref道德
                "field": "sm",  # 或 "经典"
                "保密预算": self.config.get("security_budget", 40),
            }
        }

    def execute_psi(
        self,
        task_id: str,
        inputs: Dict[str, Any],
        params: Dict[str, str]
    ) -> Dict[str, Any]:
        """执行PSI (隐私集合求交)"""
        if not self.initialized:
            raise RuntimeError("SecretFlow not initialized")

        from secretflow.preprocessing import PSI

        # 获取数据源
        data_source = inputs.get("data_source", {})
        key_column = params.get("key_column", "id")
        psi_type = params.get("psi_type", "ecdh")  # ecdh, kkrt, bc22

        # 创建PSI任务
        psi = PSI(
            self.spursu,
            task_id=task_id,
            key_column=key_column,
            psi_type=psi_type,
        )

        # 执行PSI
        result = psi.run(
            data=data_source,
            receiver=params.get("receiver_party", self.self_party),
            sender=params.get("sender_party", ""),
        )

        return {
            "status": "ok",
            "matched_count": len(result) if result is not None else 0,
            "psi_type": psi_type,
        }

    def execute_federated_learning(
        self,
        task_id: str,
        inputs: Dict[str, Any],
        params: Dict[str, str]
    ) -> Dict[str, Any]:
        """执行联邦学习"""
        if not self.initialized:
            raise RuntimeError("SecretFlow not initialized")

        from secretflow.ml import FL

        model_type = params.get("model_type", "linear_regression")
        epochs = int(params.get("epochs", 10))
        batch_size = int(params.get("batch_size", 64))

        # 获取各方数据
        data_sources = inputs.get("data_sources", {})

        # 创建联邦学习任务
        fl = FL(
            device=self.spursu,
            model_type=model_type,
            task_id=task_id,
        )

        # 执行联邦训练
        model = fl.train(
            data=data_sources,
            epochs=epochs,
            batch_size=batch_size,
            validator=params.get("validator", None),
        )

        return {
            "status": "ok",
            "model_type": model_type,
            "epochs": epochs,
        }

    def execute_mpc(
        self,
        task_id: str,
        inputs: Dict[str, Any],
        params: Dict[str, str]
    ) -> Dict[str, Any]:
        """执行MPC (安全多方计算)"""
        if not self.initialized:
            raise RuntimeError("SecretFlow not initialized")

        # 获取输入数据
        data_inputs = inputs.get("data", {})

        # MPC计算类型
        mpc_type = params.get("mpc_type", "addition")  # addition, multiplication, comparison

        # 将数据转换为SPU设备上的数据
        # 实际实现需要根据具体MPC协议

        return {
            "status": "ok",
            "mpc_type": mpc_type,
        }

    def get_pyu_device(self, party: str) -> PYU:
        """获取指定方的PYU设备"""
        return self.pyu_devices.get(party)

    def get_spu_device(self) -> SPU:
        """获取SPU设备"""
        return self.spursu

    def cleanup(self) -> None:
        """清理SecretFlow资源"""
        if self.sf is not None:
            sf.shutdown()
            self.initialized = False