"""
ECDH-based PSI Implementation
隐私集合求交 - 不依赖 SecretFlow SPU
使用 ECDH (Elliptic Curve Diffie-Hellman) 密钥交换实现
"""

import os
import logging
import hashlib
import json
import requests
from typing import Dict, Any, List, Tuple, Optional
from cryptography.hazmat.primitives import hashes
from cryptography.hazmat.primitives.asymmetric import ec
from cryptography.hazmat.primitives.serialization import (
    Encoding,
    PublicFormat,
    PrivateFormat,
    load_der_private_key,
    load_der_public_key,
    NoEncryption,
)
from cryptography.hazmat.primitives.kdf.hkdf import HKDF
from cryptography.hazmat.backends import default_backend

logger = logging.getLogger(__name__)


class ECDHPSI:
    """ECDH-based PSI 实现

    安全属性：
    - 可用不可见：数据在本地加密后传输，只返回交集结果
    - 数据不出域：原始数据不离开本节点，只传输加密后的数据
    """

    def __init__(self, self_party: str, participants: List[str]):
        self.self_party = self_party
        self.participants = participants
        self._private_key: Optional[bytes] = None
        self._public_key: Optional[bytes] = None
        self._shared_secrets: Dict[str, bytes] = {}

    def generate_key_pair(self) -> Tuple[bytes, bytes]:
        """生成本节点的 ECDH 密钥对

        Returns:
            (private_key_bytes, public_key_bytes)
        """
        private_key = ec.generate_private_key(ec.SECP256R1(), default_backend())
        public_key = private_key.public_key()

        private_bytes = private_key.private_bytes(
            Encoding.DER,
            format=PrivateFormat.PKCS8,
            encryption_algorithm=NoEncryption()
        )
        public_bytes = public_key.public_bytes(
            Encoding.DER,
            PublicFormat.SubjectPublicKeyInfo
        )

        self._private_key = private_bytes
        self._public_key = public_bytes

        return private_bytes, public_bytes

    def get_public_key_hex(self) -> str:
        """获取公钥的十六进制字符串"""
        if self._public_key is None:
            self.generate_key_pair()
        return self._public_key.hex()

    def compute_shared_secrets(self, peer_public_keys: Dict[str, str]) -> None:
        """计算与所有参与方的共享密钥

        Args:
            peer_public_keys: {party_id: public_key_hex}
        """
        if self._private_key is None:
            raise RuntimeError("Private key not generated. Call generate_key_pair() first.")

        for peer_party, pubkey_hex in peer_public_keys.items():
            if peer_party == self.self_party:
                continue

            peer_public_key = bytes.fromhex(pubkey_hex)
            shared_key = self._compute_shared_secret(peer_party, peer_public_key)
            self._shared_secrets[peer_party] = shared_key

        logger.info(f"Computed shared secrets with {len(self._shared_secrets)} parties")

    def _compute_shared_secret(self, peer_party: str, peer_public_key: bytes) -> bytes:
        """计算与对方节点的共享密钥"""
        private_key = load_der_private_key(
            self._private_key, password=None, backend=default_backend()
        )
        peer_public = load_der_public_key(peer_public_key, backend=default_backend())

        shared_key = private_key.exchange(ec.ECDH(), peer_public)

        # 使用 HKDF 派生固定长度的密钥
        hkdf = HKDF(
            algorithm=hashes.SHA256(),
            length=32,
            salt=self.self_party.encode(),
            info=peer_party.encode(),
            backend=default_backend()
        )
        derived_key = hkdf.derive(shared_key)

        return derived_key

    def encrypt_ids(self, ids: List[str], shared_secret: bytes) -> List[str]:
        """使用共享密钥加密 ID 列表（确定性加密）

        Args:
            ids: ID 列表
            shared_secret: 共享密钥

        Returns:
            加密后的 ID 列表 (hex encoded)
        """
        encrypted = []
        for id_val in ids:
            # 使用 HMAC-SHA256 进行确定性加密（相同输入产生相同输出）
            hmac = hashlib.hmac.new(shared_secret, id_val.encode(), hashlib.sha256)
            encrypted.append(hmac.hexdigest())
        return encrypted

    def compute_intersection(
        self,
        my_encrypted_ids: List[str],
        all_encrypted_ids: Dict[str, List[str]]
    ) -> List[str]:
        """计算多方加密 ID 的交集

        Args:
            my_encrypted_ids: 本节点加密后的 ID
            all_encrypted_ids: 所有参与方加密后的 ID {party: [encrypted_ids]}

        Returns:
            交集结果（加密 ID 列表）
        """
        # 合并所有参与方的加密 ID
        all_encrypted: Set[str] = set()
        for party, ids in all_encrypted_ids.items():
            if party == self.self_party:
                continue
            all_encrypted.update(ids)

        # 计算与本节点加密 ID 的交集
        my_set = set(my_encrypted_ids)
        intersection = list(my_set & all_encrypted)

        logger.info(f"Intersection size: {len(intersection)} (my_ids={len(my_encrypted_ids)})")
        return intersection

    def get_aggregate_key(self) -> bytes:
        """获取聚合密钥（用于加密本方数据）

        所有共享密钥的哈希，保证所有节点使用相同密钥加密
        """
        if not self._shared_secrets:
            raise RuntimeError("No shared secrets computed")

        # 对所有共享密钥排序后合并哈希
        sorted_keys = sorted(self._shared_secrets.keys())
        aggregate = b""
        for party in sorted_keys:
            aggregate += self._shared_secrets[party]

        return hashlib.sha256(aggregate).digest()


class ECDHPSIClient:
    """ECD PSI 客户端 - 各节点使用

    负责：
    1. 生成密钥对
    2. 与协调器交换公钥
    3. 加密本地数据
    4. 发送加密数据并接收交集结果
    """

    def __init__(self, self_party: str, scheduler_url: str):
        self.self_party = self_party
        self.scheduler_url = scheduler_url.rstrip('/')
        self.psi = ECDHPSI(self_party, [])

    def execute_psi(
        self,
        local_ids: List[str],
        participants: List[str],
        key_column: str = 'id'
    ) -> Dict[str, Any]:
        """执行 ECDH PSI

        Args:
            local_ids: 本地 ID 列表
            participants: 参与方列表
            key_column: 用于 PSI 的列名

        Returns:
            PSI 结果
        """
        try:
            logger.info(f"Executing ECDH PSI: self_party={self_party}, participants={participants}, ids={len(local_ids)}")

            # 更新 PSI 实例的参与方列表
            self.psi = ECDHPSI(self.self_party, participants)

            # 1. 生成本节点密钥对
            private_key, public_key = self.psi.generate_key_pair()
            public_key_hex = public_key.hex()

            # 2.发送公钥到调度器，接收所有参与方的公钥
            key_exchange_payload = {
                'taskId': f"psi_{self.self_party}",
                'party': self.self_party,
                'publicKey': public_key_hex,
                'participants': participants
            }

            response = requests.post(
                f"{self.scheduler_url}/api/v1/psi/key_exchange",
                json=key_exchange_payload,
                timeout=30
            )

            if response.status_code != 200:
                raise RuntimeError(f"Key exchange failed: {response.status_code} {response.text}")

            all_public_keys = response.json().get('publicKeys', {})

            # 3. 计算共享密钥
            self.psi.compute_shared_secrets(all_public_keys)

            # 4. 使用聚合密钥加密本地数据
            aggregate_key = self.psi.get_aggregate_key()
            encrypted_ids = self.psi.encrypt_ids(local_ids, aggregate_key)

            # 5. 发送加密数据到调度器，接收交集结果
            psi_payload = {
                'taskId': f"psi_{self.self_party}",
                'party': self.self_party,
                'encryptedIds': encrypted_ids,
                'participants': participants
            }

            response = requests.post(
                f"{self.scheduler_url}/api/v1/psi/submit_encrypted_ids",
                json=psi_payload,
                timeout=60
            )

            if response.status_code != 200:
                raise RuntimeError(f"PSI compute failed: {response.status_code} {response.text}")

            result = response.json()
            intersection = result.get('intersection', [])

            # 6. 揭示交集（将加密 ID 转换回原始 ID）
            # 由于使用确定性加密，可以直接映射
            id_mapping = {eid: orig for orig, eid in zip(local_ids, encrypted_ids)}
            original_ids = [id_mapping.get(eid, eid) for eid in intersection]

            logger.info(f"ECDH PSI completed: matched={len(original_ids)}")

            return {
                'status': 'ok',
                'component': 'psi',
                'psi_type': 'ecdh',
                'method': 'ECDH',
                'self_party': self.self_party,
                'matched_count': len(original_ids),
                'data': [{key_column: oid} for oid in original_ids]
            }

        except Exception as e:
            logger.error(f"ECDH PSI error: {e}")
            return {
                'status': 'error',
                'error': str(e),
                'component': 'psi'
            }


def execute_ecdh_psi(
    local_ids: List[str],
    self_party: str,
    participants: List[str],
    scheduler_url: str,
    key_column: str = 'id'
) -> Dict[str, Any]:
    """执行 ECDH PSI（便捷函数）

    Args:
        local_ids: 本地 ID 列表
        self_party: 本节点 ID
        participants: 参与方列表
        scheduler_url: 调度器 URL
        key_column: 用于 PSI 的列名

    Returns:
        PSI 结果
    """
    client = ECDHPSIClient(self_party, scheduler_url)
    return client.execute_psi(local_ids, participants, key_column)