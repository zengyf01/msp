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

# 导入msp_node模块
import os
sys.path.insert(0, os.path.join(os.path.dirname(__file__), '..'))

from msp_node.protos import msp_node_pb2, msp_node_pb2_grpc
from msp_node.runners.psi_runner import PSIRunner
from msp_node.runners.fl_runner import FLRunner
from msp_node.runners.mpc_runner import MPCRunner
from msp_node.runners.base_runner import BaseRunner


logger = logging.getLogger(__name__)


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

    server = create_server(port=50051)
    server.start()

    # 等待中断信号
    def signal_handler(sig, frame):
        logger.info("Shutting down server...")
        server.stop()
        sys.exit(0)

    signal.signal(signal.SIGINT, signal_handler)
    signal.signal(signal.SIGTERM, signal_handler)

    server.wait_for_termination()