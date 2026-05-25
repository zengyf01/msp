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
        """执行PSI (隐私集合求交)

        支持多种 PSI 协议:
        - ecdh: ECDH-PSI (默认，适用于小规模数据集)
        - kkrt: KKRT-PSI (适用于百万级数据)
        - bc22: BC22-PSI (适用于千万级数据)
        - unbalanced: 不平衡 PSI (适用于大小集合差异大的场景)
        """
        if not self.initialized:
            raise RuntimeError("SecretFlow not initialized")

        # 获取数据源
        data_source = inputs.get("data_source", {})
        key_column = params.get("key_column", "id")
        psi_type = params.get("psi_type", "ecdh")

        try:
            if psi_type == "ecdh":
                from secretflow.preprocessing import PSI
                psi = PSI(
                    self.spursu,
                    task_id=task_id,
                    key_column=key_column,
                    psi_type=psi_type,
                )
                result = psi.run(
                    data=data_source,
                    receiver=params.get("receiver_party", self.self_party),
                    sender=params.get("sender_party", ""),
                )

            elif psi_type == "kkrt":
                from secretflow.preprocessing.psi import KKRTPSI
                psi = KKRTPSI(
                    self.spursu,
                    task_id=task_id,
                    key_column=key_column,
                )
                result = psi.run(
                    data=data_source,
                    receiver=params.get("receiver_party", self.self_party),
                    sender=params.get("sender_party", ""),
                )

            elif psi_type == "bc22":
                from secretflow.preprocessing.psi import BC22PSI
                psi = BC22PSI(
                    self.spursu,
                    task_id=task_id,
                    key_column=key_column,
                )
                result = psi.run(
                    data=data_source,
                    receiver=params.get("receiver_party", self.self_party),
                    sender=params.get("sender_party", ""),
                    num_buckets=int(params.get("bucket_size", 1000)),
                )

            elif psi_type == "unbalanced":
                from secretflow.preprocessing.psi import UnbalancedPSI
                psi = UnbalancedPSI(
                    self.spursu,
                    task_id=task_id,
                    key_column=key_column,
                )
                result = psi.run(
                    data=data_source,
                    receiver=params.get("receiver_party", self.self_party),
                    sender=params.get("sender_party", ""),
                )

            else:
                raise ValueError(f"Unsupported psi_type: {psi_type}")

            return {
                "status": "ok",
                "matched_count": len(result) if result is not None else 0,
                "psi_type": psi_type,
            }

        except Exception as e:
            return {
                "status": "error",
                "error": str(e),
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

        import secretflow as sf

        # 获取输入数据
        data_inputs = inputs.get("data", {})
        data_a = data_inputs.get("data_a", [])
        data_b = data_inputs.get("data_b", [])

        # MPC计算类型
        mpc_type = params.get("mpc_type", "addition")  # addition, multiplication, comparison

        # 将数据转换为SPU设备上的数据
        # 使用 PYU 读取数据后移到 SPU
        pyu_a = self.pyu_devices.get(self.self_party)
        if pyu_a is None:
            pyu_a = PYU(self.self_party)

        # 在 PYU 上准备数据，然后移到 SPU
        def _prepare_data(values):
            import numpy as np
            return np.array(values)

        # 将输入数据放在 SPU 上
        # 通过 PYU 创建数据然后转到 SPU
        spu_data_a = sf.to(self.spursu, _prepare_data(data_a))
        spu_data_b = sf.to(self.spursu, _prepare_data(data_b))

        # 执行MPC计算
        if mpc_type == "addition":
            result = self.spursu.add(spu_data_a, spu_data_b)
        elif mpc_type == "multiplication":
            result = self.spursu.mul(spu_data_a, spu_data_b)
        elif mpc_type == "comparison":
            result = self.spursu.lt(spu_data_a, spu_data_b)
        else:
            raise ValueError(f"Unsupported MPC type: {mpc_type}")

        # 揭示结果获取明文值
        plain_result = sf.reveal(result)

        return {
            "status": "ok",
            "mpc_type": mpc_type,
            "result": plain_result.tolist() if hasattr(plain_result, 'tolist') else plain_result,
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

    def execute_vertical_fl(
        self,
        task_id: str,
        inputs: Dict[str, Any],
        params: Dict[str, str]
    ) -> Dict[str, Any]:
        """执行纵向联邦学习

        Args:
            task_id: 任务ID
            inputs: {
                "data_sources": {
                    "party_a": {"table": "...", "columns": [...]},
                    "party_b": {"table": "...", "columns": [...]}
                }
            }
            params: {
                "label_party": "party_a",
                "label_column": "default_flag",
                "model_type": "logistic_regression" | "secureboost",
                "num_trees": 10,        # for secureboost
                "max_depth": 6,          # for secureboost
                "epochs": 10,            # for logistic_regression
            }
        """
        if not self.initialized:
            raise RuntimeError("SecretFlow not initialized")

        model_type = params.get("model_type", "vertical_lr")

        if model_type in ("secureboost", "sgb"):
            return self.train_sgb(inputs, params)
        elif model_type in ("vertical_lr", "logistic_regression"):
            return self.train_vertical_lr(inputs, params)
        else:
            raise ValueError(f"Unsupported model_type: {model_type}")

    def train_sgb(
        self,
        inputs: Dict[str, Any],
        params: Dict[str, str]
    ) -> Dict[str, Any]:
        """训练 SecureBoost 模型

        基于 SecretFlow 的 SGB (SecureBoost) 实现
        """
        if not self.initialized:
            raise RuntimeError("SecretFlow not initialized")

        # 获取参数
        label_party = params.get("label_party")
        label_column = params.get("label_column", "label")
        num_trees = int(params.get("num_trees", 10))
        max_depth = int(params.get("max_depth", 6))
        learning_rate = float(params.get("learning_rate", 0.1))

        # 构建数据源
        data_sources = inputs.get("data_sources", {})

        # 检查 SGB 是否可用
        try:
            from secretflow.ml.boost import SGB
        except ImportError:
            return {
                "status": "error",
                "error": "SecureBoost (SGB) requires secretflow>=1.15.0. Please upgrade.",
                "model_type": "secureboost",
            }

        try:
            # 创建 SGB 模型
            sgb = SGB(
                device=self.spursu,
                num_boosting_rounds=num_trees,
                max_depth=max_depth,
                learning_rate=learning_rate,
            )

            # 训练
            model = sgb.train(
                data=data_sources,
                label=label_party,
                label_column=label_column,
            )

            # 评估 (如果可能)
            evaluation = {}
            try:
                if hasattr(model, 'evaluate'):
                    evaluation = model.evaluate(
                        data=data_sources,
                        label=label_party,
                        label_column=label_column,
                    )
            except Exception:
                pass  # 评估可能失败，但训练可能成功

            return {
                "status": "ok",
                "model_type": "secureboost",
                "auc": float(evaluation.get('auc', 0.0)),
                "num_trees": num_trees,
                "max_depth": max_depth,
            }
        except Exception as e:
            return {
                "status": "error",
                "error": str(e),
                "model_type": "secureboost",
            }

    def train_vertical_lr(
        self,
        inputs: Dict[str, Any],
        params: Dict[str, str]
    ) -> Dict[str, Any]:
        """训练纵向逻辑回归模型

        基于 SecretFlow 的垂直联邦学习实现
        """
        if not self.initialized:
            raise RuntimeError("SecretFlow not initialized")

        # 获取参数
        label_party = params.get("label_party")
        label_column = params.get("label_column", "label")
        epochs = int(params.get("epochs", 10))
        batch_size = int(params.get("batch_size", 64))

        # 构建数据源
        data_sources = inputs.get("data_sources", {})

        try:
            from secretflow.ml.nn import FLModel
        except ImportError:
            return {
                "status": "error",
                "error": "FLModel requires secretflow>=1.15.0. Please upgrade.",
                "model_type": "vertical_lr",
            }

        try:
            # 构建模型创建函数
            def create_model():
                import tensorflow as tf
                from tensorflow import keras

                # 简单模型
                model = keras.Sequential([
                    keras.layers.Dense(64, activation='relu', input_shape=(10,)),
                    keras.layers.Dense(32, activation='relu'),
                    keras.layers.Dense(1, activation='sigmoid')
                ])
                model.compile(optimizer='adam', loss='binary_crossentropy', metrics=['accuracy'])
                return model

            # 使用 FLModel 进行垂直联邦训练
            fl_model = FLModel(
                device=self.spursu,
                model_fn=create_model,
                aggregator_type='secure_aggregator',
            )

            # 训练
            history = fl_model.train(
                data=data_sources,
                label=label_party,
                label_column=label_column,
                epochs=epochs,
                batch_size=batch_size,
                validation_split=0.2,
            )

            # 评估
            evaluation = fl_model.evaluate(
                data=data_sources,
                label=label_party,
                label_column=label_column,
            )

            return {
                "status": "ok",
                "model_type": "vertical_lr",
                "accuracy": float(evaluation.get('accuracy', 0)),
                "loss": float(evaluation.get('loss', 0)),
                "epochs": epochs,
            }
        except Exception as e:
            return {
                "status": "error",
                "error": str(e),
                "model_type": "vertical_lr",
            }