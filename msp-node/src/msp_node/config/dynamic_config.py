"""
DynamicConfig - 动态配置管理类
支持动态参与方选择、Ray集群管理、数据源配置
"""

import os
import socket
import logging
from typing import Dict, List, Any, Optional

logger = logging.getLogger(__name__)


class DynamicConfig:
    """动态配置管理类

    从环境变量读取配置，支持：
    - SELF_PARTY / NODE_ID: 当前节点标识
    - PARTIES: 动态参与方列表（核心新功能）
    - RAY_HEAD_PARTY: Ray head 节点
    - SPU_*_HOST/PORT: 各节点 SPU 地址
    - DB_*_*: 各节点数据库配置
    """

    # 环境变量前缀
    SPU_PREFIX = "SPU_"
    DB_PREFIX = "DB_"

    def __init__(self):
        self._config: Dict[str, Any] = {}
        self._load_from_env()

    def _load_from_env(self) -> None:
        """从环境变量加载配置"""
        # 节点标识
        self._config['self_party'] = os.environ.get('SELF_PARTY', os.environ.get('NODE_ID', ''))
        self._config['node_name'] = os.environ.get('NODE_NAME', self._config['self_party'])

        # 动态 parties 列表（核心: 支持 PARTIES 环境变量）
        parties_str = os.environ.get('PARTIES', os.environ.get('ALL_PARTIES', ''))
        self._config['parties'] = [p.strip() for p in parties_str.split(',') if p.strip()]

        # Ray 集群配置
        self._config['ray_head_party'] = os.environ.get('RAY_HEAD_PARTY', '')
        if not self._config['ray_head_party'] and self._config['parties']:
            # 默认使用第一个节点作为 head
            self._config['ray_head_party'] = self._config['parties'][0]

        self._config['ray_head_address'] = os.environ.get('RAY_HEAD_ADDRESS', '')
        if not self._config['ray_head_address']:
            # 默认使用 head party 作为地址
            self._config['ray_head_address'] = f"{self._config['ray_head_party']}:6379"

        self._config['ray_head_port'] = os.environ.get('RAY_HEAD_PORT', '6379')
        self._config['ray_head_ip'] = self._get_container_ip()

        # SPU 配置
        self._config['spu_port'] = os.environ.get('SPU_PORT', '8000')
        self._config['spu_protocol'] = os.environ.get('SPU_PROTOCOL', 'SEMI2K')
        self._config['spu_field'] = os.environ.get('SPU_FIELD', 'FM64')
        self._config['spu_fxp_bits'] = int(os.environ.get('SPU_FXP_FRACTION_BITS', '18'))

        # PSI 配置
        self._config['psi_protocol'] = os.environ.get('PSI_PROTOCOL', 'KKRT')
        self._config['psi_receiver'] = os.environ.get('PSI_RECEIVER', '')
        self._config['psi_key_column'] = os.environ.get('PSI_KEY_COLUMN', 'id')

        # 数据源配置
        self._config['data_sources'] = self._parse_data_sources()

        # 工作目录
        self._config['work_dir'] = os.environ.get('WORK_DIR', '/tmp/secretflow')

        # 调试模式
        self._config['debug'] = os.environ.get('DEBUG_MODE', 'false').lower() == 'true'

    def _get_container_ip(self) -> str:
        """获取容器 IP"""
        try:
            s = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
            s.connect(('8.8.8.8', 80))
            ip = s.getsockname()[0]
            s.close()
            return ip
        except:
            return '127.0.0.1'

    def _parse_data_sources(self) -> Dict:
        """解析数据源配置"""
        sources = {}

        # 方式1：从环境变量读取 JSON
        sources_json = os.environ.get('DATA_SOURCES', '')
        if sources_json:
            import json
            try:
                sources = json.loads(sources_json)
            except:
                pass

        # 方式2：从环境变量读取每个节点的数据源
        for party in self._config['parties']:
            party_key = party.upper().replace('-', '_')
            ds_host = os.environ.get(f'DB_{party_key}_HOST')
            if ds_host:
                sources[party] = {
                    'host': ds_host,
                    'port': int(os.environ.get(f'DB_{party_key}_PORT', '3306')),
                    'user': os.environ.get(f'DB_{party_key}_USER', 'root'),
                    'password': os.environ.get(f'DB_{party_key}_PASSWORD', ''),
                    'database': os.environ.get(f'DB_{party_key}_DATABASE', ''),
                    'table': os.environ.get(f'DB_{party_key}_TABLE', 'data_table'),
                    'columns': os.environ.get(f'DB_{party_key}_COLUMNS', '*').split(',')
                }

        return sources

    def get_parties(self) -> List[str]:
        """获取参与方列表"""
        return self._config.get('parties', [])

    def get_self_party(self) -> str:
        """获取当前节点标识"""
        return self._config.get('self_party', '')

    def is_head_node(self) -> bool:
        """判断当前节点是否为 Head 节点"""
        parties = self.get_parties()
        if not parties:
            return True
        return self.get_self_party() == parties[0]

    def get_spu_config(self) -> dict:
        """获取 SPU 设备配置"""
        parties = self.get_parties()
        spu_port = self._config['spu_port']

        nodes = []
        for party in parties:
            address = self._get_party_address(party, 'spu')
            nodes.append({"party": party, "address": address})
            logger.info(f"SPU node: {party} -> {address}")

        return {
            "nodes": nodes,
            "runtime_config": {
                "protocol": self._config['spu_protocol'],
                "field": self._config['spu_field'],
                "fxp_fraction_bits": self._config['spu_fxp_bits'],
            }
        }

    def _get_party_address(self, party: str, service_type: str = 'ray') -> str:
        """获取参与方的服务地址"""
        party_key = party.upper().replace('-', '_')

        if service_type == 'ray':
            addr_env = f'RAY_{party_key}_ADDRESS'
            if addr_env in os.environ:
                return os.environ[addr_env]
            # 使用 Kubernetes DNS 或默认主机名
            return f"{party}:{self._config['ray_head_port']}"

        elif service_type == 'spu':
            # 首先检查完整的 SPU_{party}_ADDRESS 环境变量（如 SPU_NODE_RESEARCH=node-b:8000）
            addr_env = f'SPU_{party_key}'
            if addr_env in os.environ:
                return os.environ[addr_env]
            # 然后检查分离的 HOST/PORT 环境变量
            host_env = f'SPU_{party_key}_HOST'
            port_env = f'SPU_{party_key}_PORT'
            if host_env in os.environ:
                port = os.environ.get(port_env, self._config['spu_port'])
                return f"{os.environ[host_env]}:{port}"
            return f"{party}:{self._config['spu_port']}"

        return f"{party}:8000"

    def get_data_source(self, party: str) -> Optional[Dict]:
        """获取参与方的数据源配置"""
        return self._config['data_sources'].get(party)

    def get(self, key: str, default: Any = None) -> Any:
        """通用获取接口"""
        return self._config.get(key, default)

    def get_psi_config(self) -> Dict[str, Any]:
        """获取 PSI 配置"""
        return {
            'protocol': self._config.get('psi_protocol', 'kkrt'),
            'receiver': self._config.get('psi_receiver') or self.get_parties()[0] if self.get_parties() else '',
            'key_column': self._config.get('psi_key_column', 'id'),
        }

    def get_ray_head_address(self) -> str:
        """获取 Ray Head 地址"""
        return self._config.get('ray_head_address', '')

    def __repr__(self) -> str:
        return f"DynamicConfig(self_party={self.get_self_party()}, parties={self.get_parties()})"
