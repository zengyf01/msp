"""
RayClusterManager - Ray 集群管理器
动态管理 Ray 集群，支持 head/worker 自动判断
"""

import os
import socket
import subprocess
import time
import logging
from typing import Optional

logger = logging.getLogger(__name__)


class RayClusterManager:
    """Ray 集群管理器

    负责 Ray 集群的初始化和管理：
    - Head 节点：启动 Ray head
    - Worker 节点：连接到 Head
    """

    def __init__(self, config):
        """初始化 Ray 集群管理器

        Args:
            config: DynamicConfig 实例或类似配置对象
        """
        self.config = config
        self._ray_initialized = False
        self._is_head = config.is_head_node()

    def initialize(self) -> None:
        """初始化 Ray 集群"""
        import ray

        # 如果已经初始化，跳过
        if ray.is_initialized():
            self._ray_initialized = True
            logger.info(f"Ray already initialized: {ray.get_runtime_context().gcs_address}")
            return

        if self._is_head:
            self._start_ray_head()
        else:
            self._connect_ray_worker()

        # 验证初始化成功
        self._ray_initialized = ray.is_initialized()
        if self._ray_initialized:
            logger.info(f"Ray cluster initialized: {ray.get_runtime_context().gcs_address}")
        else:
            raise RuntimeError("Failed to initialize Ray cluster")

    def _start_ray_head(self) -> None:
        """启动 Ray Head 节点"""
        import ray

        container_ip = self._get_container_ip()
        head_port = self.config.get('ray_head_port', '6379')
        num_cpus = os.environ.get('RAY_NUM_CPUS', '8')

        logger.info(f"Starting Ray head on {container_ip}:{head_port}")

        result = subprocess.run([
            "ray", "start", "--head",
            "--node-ip-address", container_ip,
            "--port", head_port,
            "--num-cpus", num_cpus,
            "--disable-usage-stats"  # 避免提示
        ], capture_output=True, text=True, timeout=120)

        if result.returncode == 0:
            logger.info("Ray head started successfully")
            time.sleep(3)
            ray.init(ignore_reinit_error=True, include_dashboard=False)
        else:
            error_msg = result.stderr[:500] if result.stderr else "Unknown error"
            logger.error(f"Failed to start Ray head: {error_msg}")
            raise RuntimeError(f"Failed to start Ray head: {error_msg}")

    def _connect_ray_worker(self) -> None:
        """连接到 Ray Head"""
        import ray

        head_address = self.config.get_ray_head_address()
        if not head_address:
            head_party = self.config.get('ray_head_party', '')
            head_port = self.config.get('ray_head_port', '6379')
            head_address = f"{head_party}:{head_port}"

        logger.info(f"Connecting to Ray head at {head_address}")

        result = subprocess.run([
            "ray", "start", "--address", head_address
        ], capture_output=True, text=True, timeout=60)

        if result.returncode == 0:
            logger.info("Ray worker started successfully")
            ray.init(address=head_address, ignore_reinit_error=True, include_dashboard=False)
        else:
            error_msg = result.stderr[:500] if result.stderr else "Unknown error"
            logger.warning(f"Failed to connect to Ray head: {error_msg}")
            # 回退到本地模式
            logger.info("Falling back to local Ray worker mode")
            ray.init(ignore_reinit_error=True, include_dashboard=False)

    def _get_container_ip(self) -> str:
        """获取容器 IP"""
        try:
            s = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
            s.connect(('8.8.8.8', 80))
            ip = s.getsockname()[0]
            s.close()
            return ip
        except:
            return socket.gethostname()

    def is_initialized(self) -> bool:
        """检查 Ray 是否已初始化"""
        return self._ray_initialized

    def is_head(self) -> bool:
        """检查当前节点是否为 Head"""
        return self._is_head

    def get_ray_address(self) -> Optional[str]:
        """获取 Ray 集群地址"""
        import ray
        if ray.is_initialized():
            return ray.get_runtime_context().gcs_address
        return None

    def shutdown(self) -> None:
        """关闭 Ray 集群（仅 Head 节点）"""
        import ray
        if self._is_head:
            try:
                ray.shutdown()
                subprocess.run(["ray", "stop"], capture_output=True, timeout=30)
                logger.info("Ray cluster shutdown")
            except Exception as e:
                logger.warning(f"Error shutting down Ray: {e}")
        else:
            try:
                ray.shutdown()
                logger.info("Ray worker disconnected")
            except Exception as e:
                logger.warning(f"Error disconnecting Ray: {e}")
