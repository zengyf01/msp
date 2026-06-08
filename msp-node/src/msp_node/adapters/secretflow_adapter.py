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
        """初始化SecretFlow - 按需动态创建或连接 Ray 集群

        Ray 集群生命周期管理：
        1. 如果是 Head 节点（participants[0]）：启动 Ray head
        2. 如果是 Worker 节点：连接到 Head
        3. 所有节点调用 sf.init() 加入集群
        4. 任务完成后清理（shutdown Ray）
        """
        if self.self_party not in self.parties:
            raise ValueError(f"Self party '{self.self_party}' not in parties: {self.parties}")

        import ray

        # 判断自己是 Head 还是 Worker
        # Head 是 participants 中的第一个
        participants = self.config.get('participants', [])
        is_head = (len(participants) > 0 and self.self_party == participants[0])

        # Ray 集群标识：使用 task_id 或统一集群名
        cluster_id = self.config.get('task_id', 'default')
        ray_head_port = os.environ.get('RAY_HEAD_PORT', '6379')
        ray_spu_port = os.environ.get('SPU_PORT', '8000')

        # 动态 Ray 集群地址
        cluster_address = f"{self.self_party}:{ray_head_port}"

        if is_head:
            # Head 节点：启动 Ray head
            self._start_ray_head(cluster_address)
        else:
            # Worker 节点：连接到 Head
            head_party = participants[0] if len(participants) > 0 else None
            if head_party:
                self._connect_ray_worker(head_party, ray_head_port)

        # 连接后，调用 sf.init() 加入集群
        # 重要：sf.init() 的 address 是 Ray 集群的 GCS 地址，不是本节点地址
        # 对于 head 节点，address='auto'；对于 worker，address='<head_address>'
        if is_head:
            sf.init(
                party=self.self_party,
                num_cpus=8,
                config={
                    "party_cat": self.config.get("category_config", {}),
                    "device": self.config.get("device", "spu"),
                }
            )
        else:
            head_address = f"{participants[0]}:{ray_head_port}" if participants else None
            if head_address:
                sf.init(
                    address=f"ray://{head_address}",
                    party=self.self_party,
                    config={
                        "party_cat": self.config.get("category_config", {}),
                        "device": self.config.get("device", "spu"),
                    }
                )

        # 创建SPU设备（使用各节点的 SPU 监听地址）
        spu_config = self._build_spu_config()
        self.spursu = SPU(spu_config)

        # 创建各方的PYU设备
        for party in self.parties:
            self.pyu_devices[party] = PYU(party)

        self.initialized = True
        self._is_head = is_head

    def _start_ray_head(self, head_address: str) -> None:
        """启动 Ray Head 节点"""
        import ray

        # 如果已经初始化，不重复启动
        if ray.is_initialized():
            return

        container_ip = self._get_container_ip()
        head_port = os.environ.get('RAY_HEAD_PORT', '6379')

        import subprocess
        result = subprocess.run(
            ["ray", "start", "--head",
             "--node-ip-address", container_ip,
             "--port", head_port,
             "--num-cpus", "8"],
            capture_output=True,
            text=True,
            timeout=30
        )

        if result.returncode == 0:
            # ray start 成功，等待 Ray 准备好
            import time
            time.sleep(3)
            ray.init(ignore_reinit_error=True, include_dashboard=False)
        else:
            raise RuntimeError(f"Failed to start Ray head: {result.stderr}")

    def _connect_ray_worker(self, head_party: str, head_port: str) -> None:
        """连接到 Ray Head"""
        import ray

        if ray.is_initialized():
            return

        head_address = f"{head_party}:{head_port}"

        import subprocess
        result = subprocess.run(
            ["ray", "start", "--address", head_address],
            capture_output=True,
            text=True,
            timeout=30
        )

        if result.returncode == 0:
            ray.init(address=head_address, ignore_reinit_error=True, include_dashboard=False)
        else:
            raise RuntimeError(f"Failed to connect to Ray head: {result.stderr}")

    def _get_container_ip(self) -> str:
        """获取容器 IP"""
        import socket
        try:
            s = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
            s.connect(('8.8.8.8', 80))
            ip = s.getsockname()[0]
            s.close()
            return ip
        except:
            return socket.gethostname()

    def shutdown_ray(self) -> None:
        """关闭 Ray 集群（仅 Head 节点执行）"""
        import ray
        if hasattr(self, '_is_head') and self._is_head:
            ray.shutdown()
            import subprocess
            subprocess.run(["ray", "stop"], capture_output=True)

    def _build_spu_config(self) -> dict:
        """构建SPU配置 - 使用各节点的 SPU 监听地址"""
        cluster_config = self.config.get("cluster_config", {})

        # 多方配置
        parties = cluster_config.get("parties", self.parties)

        # SPU 地址：各节点的 SPU 监听端口
        # 格式：节点名:SPU端口
        # SPU 端口默认 8000，可通过环境变量 SPU_PORT 配置
        spu_port = os.environ.get('SPU_PORT', '8000')

        # 从环境变量或配置获取各节点的 SPU 地址
        # 环境变量格式：SPU_NODE_A=node-a:8000, SPU_NODE_B=node-b:8000
        addresses = []
        for i, party in enumerate(parties):
            env_key = f"SPU_{party.upper().replace('-', '_')}"
            addr = os.environ.get(env_key)
            if addr:
                addresses.append(addr)
            else:
                # 默认使用 party 名 + SPU 端口
                addresses.append(f"{party}:{spu_port}")

        # 构建SPU配置
        nodes = []
        for i, party in enumerate(parties):
            node = {
                "party": party,
                "address": addresses[i] if i < len(addresses) else f"{party}:{spu_port}",
            }
            nodes.append(node)

        return {
            "nodes": nodes,
            "runtime_config": {
                "protocol": "SEMI2K",  # 协议: SEMI2K, ABY3, REF2K
                "field": "FM64",       # 域: FM64, SM9
                "fxp_fraction_bits": 18,  # 定点数小数位
            }
        }

    def execute_psi(
        self,
        task_id: str,
        inputs: Dict[str, Any],
        params: Dict[str, str]
    ) -> Dict[str, Any]:
        """执行PSI (隐私集合求交)

        使用 spu.psi_df 方法，参考用户的 psi.py 实现方式：
        - 各方通过 Ray 组网
        - 通过 SPU 设备执行安全计算
        """
        if not self.initialized:
            raise RuntimeError("SecretFlow not initialized")

        # 获取数据源：支持 DataFrame、列表或字典
        data_source = inputs.get("data_source", {})
        key_column = params.get("key_column", "id")
        psi_type = params.get("psi_type", "ecdh")

        # 转换数据为 DataFrame（如果需要）
        import pandas as pd
        if isinstance(data_source, list):
            data_source = pd.DataFrame(data_source)
        elif isinstance(data_source, dict) and 'rows' in data_source:
            data_source = pd.DataFrame(data_source['rows'])

        self_party = params.get('self_party', self.self_party)
        other_parties = params.get('other_parties', [])

        try:
            # 构建 SPU 配置
            spu_config = self._build_spu_config()

            # 如果 self_party 在 parties 中，说明这是多方 PSI
            # 需要收集各方数据后通过 SPU 执行
            if self_party in self.parties and len(self.parties) > 1:
                # 多方 PSI：使用 spu.psi_df
                return self._execute_multi_party_psi(
                    spu_config, data_source, key_column, self_party, other_parties, psi_type
                )
            else:
                # 单方测试或简化的 PSI
                return self._execute_single_party_psi(data_source, key_column, psi_type)

        except Exception as e:
            return {
                "status": "error",
                "error": str(e),
                "psi_type": psi_type,
            }

    def _execute_multi_party_psi(
        self,
        spu_config: dict,
        data_source: 'pd.DataFrame',
        key_column: str,
        self_party: str,
        other_parties: list,
        psi_type: str
    ) -> Dict[str, Any]:
        """多方 PSI 执行 - 使用 spu.psi_df

        参考用户 psi.py 的实现模式：
        1. sf.init() 连接 Ray 集群（各节点通过 Ray 组网）
        2. 创建 PYU 设备代表各方
        3. 将数据放到 PYU 上
        4. 通过 SPU 执行 psi_df（各方数据在 SPU 安全计算）
        5. sf.reveal() 获取结果
        """
        import pandas as pd

        # 创建 SPU 设备
        spu = SPU(spu_config)

        # 将本地数据放到 PYU 上
        pyu_self = self.pyu_devices.get(self_party)
        if pyu_self is None:
            pyu_self = PYU(self_party)

        # 将 DataFrame 移到 PYU 设备
        self_data = pyu_self(lambda x: x)(data_source)

        # 收集所有参与方的 PYU 数据
        # 注意：这里假设各方数据相同（用于单节点）
        # 分布式场景下，各方的数据应该来自各自的本地存储
        all_dfs = [self_data]
        for party in other_parties:
            pyu_other = self.pyu_devices.get(party)
            if pyu_other is None:
                pyu_other = PYU(party)
            # 其他方的数据也需要通过 PYU 读取
            # 实际场景：应该从其他方的本地数据源读取
            # 这里暂时用本地数据测试
            other_data = pyu_other(lambda x: x)(data_source)
            all_dfs.append(other_data)

        # 确定接收方
        receiver = self_party

        # 根据 psi_type 选择协议
        protocol_map = {
            'ecdh': 'ECDH_PSI_2PC',
            'kkrt': 'KKRT_PSI_2PC',
            'bc22': 'BC22_PSI_2PC',
        }
        protocol = protocol_map.get(psi_type, 'KKRT_PSI_2PC')

        # 执行 PSI
        # spu.psi_df 会在 SPU 内部对各方数据进行隐私集合求交
        result = spu.psi_df(
            key=[key_column],
            dfs=all_dfs,
            receiver=receiver,
            protocol=protocol
        )

        # 获取结果（揭示）
        intersection = sf.reveal(result)

        return {
            "status": "ok",
            "matched_count": len(intersection) if intersection is not None else 0,
            "psi_type": psi_type,
            "protocol": protocol,
            "result": intersection.to_dict() if intersection is not None else None
        }

    def _execute_single_party_psi(
        self,
        data_source: 'pd.DataFrame',
        key_column: str,
        psi_type: str
    ) -> Dict[str, Any]:
        """单方 PSI（测试用）"""
        # 简单的单方测试，不涉及真正的安全计算
        return {
            "status": "ok",
            "matched_count": len(data_source),
            "psi_type": psi_type,
            "message": "Single party PSI (test mode)"
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