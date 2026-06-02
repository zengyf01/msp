"""
gRPC服务
节点通信层 - 提供任务执行、健康检查等gRPC服务
"""

import grpc
from concurrent import futures
import logging
import threading
import signal
import sys
import os
import time
import requests
import json

# 导入msp_node模块
sys.path.insert(0, os.path.join(os.path.dirname(__file__), '..'))

from msp_node.protos import msp_node_pb2, msp_node_pb2_grpc
from msp_node.runners.psi_runner import PSIRunner
from msp_node.runners.fl_runner import FLRunner
from msp_node.runners.mpc_runner import MPCRunner
from msp_node.runners.base_runner import BaseRunner


logger = logging.getLogger(__name__)


class NodeManagerClient:
    """节点管理客户端 - 负责向NodeManager注册和发送心跳"""

    def __init__(self, node_manager_url: str):
        self.node_manager_url = node_manager_url.rstrip('/')
        self.node_id = os.environ.get('NODE_ID', 'node-a')
        self.node_name = os.environ.get('NODE_NAME', self.node_id)
        self.node_mode = os.environ.get('NODE_MODE', 'RAY')  # RAY / KUSCIA
        self.capabilities = os.environ.get('NODE_CAPABILITIES', 'PSI,FEDERATED_LEARNING,MPC')
        self.registered = False

    def _get_endpoint(self):
        """获取节点endpoint（内部docker网络地址）"""
        container_ip = get_container_ip()
        port = os.environ.get('GRPC_PORT', '50051')
        return f"{container_ip}:{port}"

    def _get_external_endpoint(self):
        """获取外部可访问的endpoint"""
        hostname = os.environ.get('HOSTNAME', 'localhost')
        port = os.environ.get('GRPC_PORT', '50051')
        # 映射到docker-compose暴露的端口
        port_mapping = {
            'node-a': '50051',
            'node-b': '50052',
            'node-c': '50053'
        }
        return f"localhost:{port_mapping.get(hostname, port)}"

    def register(self) -> bool:
        """向NodeManager注册节点"""
        try:
            endpoint = self._get_endpoint()
            external_endpoint = self._get_external_endpoint()

            payload = {
                "nodeId": self.node_id,
                "nodeName": self.node_name,
                "nodeMode": self.node_mode,
                "endpoint": endpoint,
                "externalEndpoint": external_endpoint,
                "capabilities": self.capabilities.split(','),
                "tags": [self.node_id]
            }

            url = f"{self.node_manager_url}/api/v1/msp/nodes/register"
            logger.info(f"Registering node {self.node_id} (mode={self.node_mode}) to {url}")

            response = requests.post(url, json=payload, timeout=10)
            if response.status_code == 200:
                result = response.json()
                logger.info(f"Node registered successfully: {result}")
                self.registered = True
                return True
            else:
                logger.warning(f"Node registration failed: {response.status_code} {response.text}")
                return False
        except Exception as e:
            logger.error(f"Node registration error: {e}")
            return False

    def heartbeat(self) -> bool:
        """发送心跳到NodeManager"""
        try:
            url = f"{self.node_manager_url}/api/v1/msp/nodes/{self.node_id}/heartbeat"
            response = requests.post(url, timeout=10)
            if response.status_code == 200:
                logger.debug(f"Heartbeat sent for node {self.node_id}")
                return True
            else:
                logger.warning(f"Heartbeat failed: {response.status_code}")
                return False
        except Exception as e:
            logger.error(f"Heartbeat error: {e}")
            return False


def start_heartbeat_service(node_manager_url: str, interval: int = 30):
    """启动心跳服务（后台线程）"""
    client = NodeManagerClient(node_manager_url)

    def heartbeat_loop():
        # 首次注册
        client.register()
        while True:
            time.sleep(interval)
            client.heartbeat()

    thread = threading.Thread(target=heartbeat_loop, daemon=True)
    thread.start()
    logger.info(f"Heartbeat service started (interval: {interval}s)")
    return thread


def get_container_ip():
    """获取容器在docker网络中的IP地址"""
    import socket
    try:
        s = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
        s.connect(('8.8.8.8', 80))
        ip = s.getsockname()[0]
        s.close()
        return ip
    except:
        hostname = socket.gethostname()
        return socket.gethostbyname(hostname)


def init_ray():
    """初始化Ray集群"""
    import ray
    import logging
    import os
    import subprocess
    import time

    logger = logging.getLogger(__name__)

    node_id = os.environ.get('NODE_ID', 'node-a')
    is_head = os.environ.get('RAY_HEAD', 'true').lower() == 'true'
    head_address = os.environ.get('RAY_HEAD_ADDRESS', 'node-a:6379')

    # 工作节点：使用ray start连接头节点
    if not is_head:
        logger.info(f"Connecting to Ray head at {head_address}...")
        try:
            result = subprocess.run(
                ["ray", "start", "--address", head_address],
                capture_output=True,
                text=True,
                timeout=30
            )
            if result.returncode == 0:
                logger.info("Ray worker started via ray start")
                # ray start成功，建立了到head的连接
                # 必须调用ray.init来连接到此Ray集群
                ray.init(address=head_address, ignore_reinit_error=True, include_dashboard=False)
                logger.info(f"Ray worker connected: {ray.get_runtime_context().gcs_address}")
            else:
                logger.warning(f"ray start failed: {result.stderr[:500]}")
                # ray start失败，回退到本地Ray（不启动head，只做worker）
                logger.info("Falling back to local Ray worker mode...")
                ray.init(ignore_reinit_error=True, include_dashboard=False)
                logger.info(f"Local Ray worker: {ray.get_runtime_context().gcs_address}")
        except Exception as e:
            logger.error(f"Worker failed to connect to Ray cluster: {e}")
            # 回退到本地Ray
            ray.init(ignore_reinit_error=True, include_dashboard=False)
            logger.info(f"Local Ray worker (fallback): {ray.get_runtime_context().gcs_address}")
        return  # 工作节点完成初始化，不执行下面的head节点代码

    # 头节点：使用实际IP启动Ray head，避免GCS和raylet地址不匹配
    container_ip = get_container_ip()
    logger.info(f"Starting Ray head node with container IP {container_ip}...")

    result = subprocess.run(
        ["ray", "start", "--head",
         "--node-ip-address", container_ip,
         "--port", "6379"],
        capture_output=True,
        text=True,
        timeout=30
    )
    if result.returncode == 0:
        logger.info("Ray head started successfully via ray start")
        logger.info(f"stdout: {result.stdout[:500] if result.stdout else 'none'}")
    else:
        logger.warning(f"ray start failed: {result.stderr[:500]}")

    time.sleep(5)

    try:
        # 检查Ray是否已经初始化（通过ray start）
        if ray.is_initialized():
            logger.info(f"Ray head already initialized: {ray.get_runtime_context().gcs_address}")
        else:
            ray.init(address='auto', ignore_reinit_error=True)
            logger.info(f"Ray head connected: {ray.get_runtime_context().gcs_address}")
    except Exception as e:
        logger.warning(f"ray.init failed: {e}")


class MSPNodeServicer(msp_node_pb2_grpc.MSPNodeServicer):
    """MSP节点gRPC服务实现"""

    def __init__(self, config: dict):
        self.config = config
        self.runners = {}
        self._init_runners()

    def _init_runners(self):
        """初始化运行器"""
        self.runners['psi'] = PSIRunner(self.config)
        self.runners['fl'] = FLRunner(self.config)
        self.runners['mpc'] = MPCRunner(self.config)

    def _get_runner(self, task_type: str) -> BaseRunner:
        """获取对应类型的运行器"""
        runner = self.runners.get(task_type.lower())
        if runner is None:
            raise ValueError(f"Unknown task type: {task_type}")
        return runner

    def ExecuteTask(self, request, context):
        """执行任务"""
        try:
            runner = self._get_runner(request.task_type)

            # 初始化运行器
            if not runner.initialized:
                runner.initialize()

            # 构建输入和参数
            inputs = {}
            if request.HasField('inputs'):
                inputs = dict(request.inputs)

            params = {}
            if request.HasField('parameters'):
                params = dict(request.parameters)

            # 执行任务
            result = runner.run(
                task_id=request.task_id,
                inputs=inputs,
                params=params
            )

            return msp_node_pb2.ExecuteTaskResponse(
                success=True,
                task_id=request.task_id,
                status=result.get("status", "completed"),
                result=str(result)
            )

        except Exception as e:
            logger.error(f"Task execution error: {e}")
            return msp_node_pb2.ExecuteTaskResponse(
                success=False,
                task_id=request.task_id,
                status="error",
                error=str(e)
            )

    def HealthCheck(self, request, context):
        """健康检查"""
        try:
            runner = self.runners.get('psi')
            healthy = runner is not None and runner.health_check()
            return msp_node_pb2.HealthCheckResponse(
                healthy=healthy,
                version="1.0.0"
            )
        except Exception as e:
            logger.error(f"Health check error: {e}")
            return msp_node_pb2.HealthCheckResponse(
                healthy=False,
                version="1.0.0"
            )


class GRPCServer:
    """gRPC服务器"""

    def __init__(self, port: int = 50051, config: dict = None):
        self.port = port
        self.config = config or {}
        self.server = None
        self.thread = None
        self.running = False

    def start(self) -> None:
        """启动服务器"""
        if self.running:
            logger.warning("Server already running")
            return

        # 创建服务器
        self.server = grpc.server(futures.ThreadPoolExecutor(max_workers=10))

        # 添加服务
        servicer = MSPNodeServicer(self.config)
        from msp_node.protos import msp_node_pb2_grpc
        msp_node_pb2_grpc.add_MSPNodeServicer_to_server(servicer, self.server)

        # 绑定端口
        self.server.add_insecure_port(f'[::]:{self.port}')

        # 启动
        self.server.start()
        self.running = True
        logger.info(f"gRPC server started on port {self.port}")

    def stop(self) -> None:
        """停止服务器"""
        if not self.running:
            return

        if self.server is not None:
            self.server.stop(grace=5)
            self.server = None

        self.running = False
        logger.info("gRPC server stopped")

    def wait_for_termination(self) -> None:
        """等待服务器终止"""
        if self.server is not None:
            self.server.wait_for_termination()

    def health_check(self) -> bool:
        """健康检查"""
        return self.running


def create_server(port: int = 50051, config: dict = None) -> GRPCServer:
    """创建gRPC服务器"""
    return GRPCServer(port=port, config=config)


if __name__ == '__main__':
    logging.basicConfig(
        level=logging.INFO,
        format='%(asctime)s - %(name)s - %(levelname)s - %(message)s'
    )

    # 初始化Ray集群
    init_ray()

    # 启动心跳服务（向NodeManager注册并定期发送心跳）
    node_manager_url = os.environ.get('NODE_MANAGER_URL', 'http://msp-node-manager:8082')
    start_heartbeat_service(node_manager_url, interval=30)

    server = create_server(port=int(os.environ.get('GRPC_PORT', 50051)))
    server.start()

    # 等待中断信号
    def signal_handler(sig, frame):
        logger.info("Shutting down server...")
        server.stop()
        sys.exit(0)

    signal.signal(signal.SIGINT, signal_handler)
    signal.signal(signal.SIGTERM, signal_handler)

    server.wait_for_termination()