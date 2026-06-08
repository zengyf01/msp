"""
MultiPartyPSIExecutor - 多方 PSI 执行器
整合 spu.psi_df 实现，支持 ECDH/KKRT/BC22 协议
结果只在接收方保存
"""

import logging
from typing import Dict, Any, Optional

logger = logging.getLogger(__name__)


class MultiPartyPSIExecutor:
    """多方 PSI 执行器

    使用 SecretFlow SPU 执行多方 PSI，支持：
    - ECDH_PSI_2PC
    - KKRT_PSI_2PC
    - BC22_PSI_2PC
    """

    PROTOCOL_MAP = {
        'ecdh': 'ECDH_PSI_2PC',
        'kkrt': 'KKRT_PSI_2PC',
        'bc22': 'BC22_PSI_2PC',
    }

    def __init__(self, config):
        """初始化多方 PSI 执行器

        Args:
            config: DynamicConfig 实例
        """
        self.config = config
        self.spu = None
        self._pyu_devices = {}

    def initialize_spu(self, parties: list = None) -> None:
        """初始化 SPU 设备

        Args:
            parties: 可选的参与方列表，用于任务级别的动态参与方选择
                   如果不指定，则使用 config 中的默认参与方
        """
        import secretflow as sf

        if parties:
            spu_config = self._build_spu_config(parties)
        else:
            spu_config = self.config.get_spu_config()
        self.spu = sf.SPU(spu_config)
        logger.info(f"SPU device created with {len(spu_config['nodes'])} nodes")

    def _build_spu_config(self, parties: list) -> dict:
        """根据指定的参与方列表构建 SPU 配置"""
        spu_port = self.config._config.get('spu_port', '8000')

        nodes = []
        for party in parties:
            address = self.config._get_party_address(party, 'spu')
            nodes.append({"party": party, "address": address})
            logger.info(f"SPU node (task-specific): {party} -> {address}")

        runtime_config = {
            "protocol": self.config._config.get('spu_protocol', 'SEMI2K'),
            "field": self.config._config.get('spu_field', 'FM64'),
            "fxp_fraction_bits": self.config._config.get('spu_fxp_bits', 18),
        }

        return {
            "nodes": nodes,
            "runtime_config": runtime_config
        }

    def _get_or_create_pyu(self, party: str):
        """获取或创建 PYU 设备"""
        import secretflow as sf
        if party not in self._pyu_devices:
            self._pyu_devices[party] = sf.PYU(party)
        return self._pyu_devices[party]

    def execute(
        self,
        data_df,
        key_column: str,
        receiver: Optional[str] = None,
        psi_type: str = 'kkrt',
        parties: list = None
    ):
        """执行多方 PSI

        Args:
            data_df: 本地数据 DataFrame
            key_column: 求交密钥列
            receiver: 结果接收方节点ID，默认使用配置的 receiver
            psi_type: PSI 协议类型 (ecdh/kkrt/bc22)
            parties: 任务参与方列表，用于动态参与方选择

        Returns:
            交集结果 DataFrame (仅 receiver 能看到)
        """
        import secretflow as sf
        import pandas as pd

        if self.spu is None:
            self.initialize_spu(parties)

        # 获取本节点
        self_party = self.config.get_self_party()

        # 获取接收方
        if not receiver:
            psi_config = self.config.get_psi_config()
            receiver = psi_config.get('receiver')
            if not receiver:
                # 默认使用 head 节点作为 receiver
                parties = self.config.get_parties()
                receiver = parties[0] if parties else self_party

        # 获取协议
        protocol = self.PROTOCOL_MAP.get(psi_type.lower(), 'KKRT_PSI_2PC')

        logger.info(f"Executing PSI: key_column={key_column}, protocol={protocol}, receiver={receiver}, self_party={self_party}")

        # 将数据放到 PYU 上
        pyu_self = self._get_or_create_pyu(self_party)
        self_data = pyu_self(lambda x: x)(data_df)

        # 执行 PSI - SPU 内部自动协调各方完成计算
        result = self.spu.psi_df(
            key=[key_column],
            dfs=[self_data],
            receiver=receiver,
            protocol=protocol
        )

        # 揭示结果（仅 receiver 能获取明文）
        intersection = sf.reveal(result)

        matched_count = len(intersection) if intersection is not None else 0
        logger.info(f"PSI completed: matched={matched_count}")

        return intersection

    def execute_with_stats(
        self,
        data_df,
        key_column: str,
        receiver: Optional[str] = None,
        psi_type: str = 'kkrt'
    ) -> tuple:
        """执行 PSI 并返回统计信息

        Returns:
            (intersection_df, stats_dict)
        """
        import pandas as pd

        # 记录原始数据大小
        original_size = len(data_df)

        # 执行 PSI
        intersection = self.execute(data_df, key_column, receiver, psi_type)

        # 构建统计信息
        stats = {
            'protocol': psi_type,
            'key_column': key_column,
            'receiver': receiver or self.config.get_parties()[0],
            'original_size': original_size,
            'intersection_size': len(intersection) if intersection is not None else 0,
        }

        return intersection, stats

    @staticmethod
    def get_matched_count(intersection_df) -> int:
        """获取交集数量"""
        if intersection_df is None:
            return 0
        import pandas as pd
        if isinstance(intersection_df, pd.DataFrame):
            return len(intersection_df)
        return len(intersection_df)
