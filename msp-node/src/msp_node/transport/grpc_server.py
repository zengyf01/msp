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
from flask import Flask, request, jsonify

# 导入msp_node模块
sys.path.insert(0, os.path.join(os.path.dirname(__file__), '..'))

from msp_node.protos import msp_node_pb2, msp_node_pb2_grpc
from msp_node.runners.psi_runner import PSIRunner
from msp_node.runners.fl_runner import FLRunner
from msp_node.runners.mpc_runner import MPCRunner
from msp_node.runners.base_runner import BaseRunner


logger = logging.getLogger(__name__)


def _patch_secretflow_sfl():
    """修复 SecretFlow 的 sfl 模块缺失问题

    SecretFlow 1.14.0b0 在尝试使用 SIMULATION 或 RAY_PRODUCTION 模式时，
    会尝试导入 sfl.distributed 模块，但该模块不存在。
    此 patch 将这些模式映射到 DebugStrategy，使其可以在没有 sfl 的情况下工作。
    """
    try:
        from secretflow.distributed import op_context, op_strategy, const

        original_init_strategy = op_context.SFOpContext._init_strategy

        def patched_init_strategy(self):
            if self._mode not in self._strategies:
                import logging as _log
                try:
                    import importlib
                    importlib.import_module('sfl.distributed')
                except Exception as e:
                    _log.error(f'import sfl.distributed fail.{e}')

                if self._mode not in self._strategies:
                    from secretflow.distributed.op_strategy import DebugStrategy
                    from secretflow.distributed.const import DISTRIBUTION_MODE
                    if self._mode in (DISTRIBUTION_MODE.SIMULATION, DISTRIBUTION_MODE.RAY_PRODUCTION):
                        _log.info(f'Adding {self._mode} as DebugStrategy fallback')
                        self._strategies[self._mode] = DebugStrategy

            return original_init_strategy(self)

        op_context.SFOpContext._init_strategy = patched_init_strategy
        logger.info("SecretFlow sfl patch applied successfully")
    except Exception as e:
        logger.warning(f"Failed to apply SecretFlow sfl patch: {e}")


# 在模块加载时应用 patch
_patch_secretflow_sfl()

# Flask HTTP 服务
http_app = Flask(__name__)

# 全局 Ray/SecretFlow 初始化状态（在主线程初始化，供 HTTP 请求复用）
_ray_initialized = False
_sf_initialized = False
_ray_cluster_manager = None
_dynamic_config = None

# ECDH PSI 全局状态（用于多方密钥交换）
_ecdh_psi_state = {
    'public_keys': {}, # {party: public_key_hex}
    'encrypted_ids': {},     # {party: [encrypted_id1, ...]}
    'lock': threading.Lock()
}


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
    """初始化Ray集群（使用 RayClusterManager）"""
    from msp_node.config import DynamicConfig
    from msp_node.cluster import RayClusterManager

    logger.info("Initializing Ray cluster via RayClusterManager")

    try:
        config = DynamicConfig()
        cluster_manager = RayClusterManager(config)
        cluster_manager.initialize()

        # 保存全局引用
        global _ray_cluster_manager, _dynamic_config
        _ray_cluster_manager = cluster_manager
        _dynamic_config = config

        logger.info(f"Ray cluster initialized: is_head={cluster_manager.is_head()}")
    except Exception as e:
        logger.error(f"Failed to initialize Ray cluster: {e}")
        raise


def _init_secretflow_at_startup():
    """在主线程初始化 SecretFlow - 在 Flask 启动前调用

    这在容器启动时（main thread）执行，不是在 HTTP 请求处理线程中。
    这样 sf.init() 不会触发 'Dont use fed api outside main thread' 错误。
    """
    global _ray_initialized, _sf_initialized

    import ray
    if not ray.is_initialized():
        logger.warning("Ray not initialized, skipping SecretFlow init")
        return

    import secretflow as sf
    from secretflow.device import PYU, SPU

    node_id = os.environ.get('NODE_ID', 'node-a')
    participants = [node_id]  # 初始化时只有自己，后续任务会扩展

    spu_port = os.environ.get('SPU_PORT', '8000')

    # 构建 SPU 配置
    spu_config = {
        "nodes": [
            {"party": node_id, "address": f"{node_id}:{spu_port}"}
        ],
        "runtime_config": {
            "protocol": "SEMI2K",
            "field": "FM64",
            "fxp_fraction_bits": 18,
        }
    }

    # 检查是否已经初始化过
    try:
        # 尝试获取已初始化的 sf 状态
        sf.info()
        logger.info("SecretFlow already initialized")
        _sf_initialized = True
        return
    except Exception:
        pass

    try:
        # 获取所有参与方（从环境变量或默认列表）
        all_parties = os.environ.get('ALL_PARTIES', 'node-hospital,node-insurance').split(',')
        all_parties = [p.strip() for p in all_parties]

        # 初始化 SecretFlow - simulation 模式
        # 传入 parties 而不传 cluster_config，使用 simulation 模式
        sf.init(
            party=node_id,
            num_cpus=8,
            parties=all_parties,  # simulation 模式需要 parties
            config={
                "party_cat": {},
                "device": "spu",
            }
            # 注意：不传 cluster_config，使用 simulation 模式，避免触发 sfl.distributed 导入
        )

        # 构建 SPU 配置
        spu_port = os.environ.get('SPU_PORT', '8000')
        spu_nodes = []
        for party in all_parties:
            env_key = f"SPU_{party.upper().replace('-', '_')}"
            spu_addr = os.environ.get(env_key, f"{party}:{spu_port}")
            spu_nodes.append({"party": party, "address": spu_addr})

        spu_config = {
            "nodes": spu_nodes,
            "runtime_config": {
                "protocol": "SEMI2K",
                "field": "FM64",
                "fxp_fraction_bits": 18,
            }
        }

        # 注意：不在这里创建 SPU 设备！因为 SPU 需要连接所有参与方的端口，
        # 但其他节点的 SPU 端口可能还未就绪。在 simulation 模式下，
        # SPU 会在 dag_executor 运行时按需创建。
        # global _http_spu, _http_pyu_devices
        # _http_spu = SPU(spu_config)
        # _http_pyu_devices = {p: PYU(p) for p in all_parties}

        _sf_initialized = True
        logger.info(f"SecretFlow initialized at startup for party={node_id}")
    except Exception as e:
        logger.warning(f"SecretFlow init at startup failed: {e}")
        _sf_initialized = False


# Flask HTTP 服务（全局共享配置）
_http_config = {}
_http_dag_executor = None


def _get_dag_executor():
    """获取或初始化 DAG 执行器"""
    global _http_dag_executor, _http_config
    if _http_dag_executor is None:
        from msp_node.runners.dag_executor import DAGExecutor
        _http_dag_executor = DAGExecutor(_http_config)
        _http_dag_executor.initialize()
    return _http_dag_executor


@http_app.route('/api/v1/execute/dag', methods=['POST'])
def execute_dag():
    """HTTP DAG 执行端点"""
    try:
        req = request.get_json()
        task_id = req.get('task_id')
        dag_def_json = req.get('dag_definition')

        if not task_id or not dag_def_json:
            return jsonify({'success': False, 'error': 'task_id and dag_definition are required'}), 400

        dag_def = json.loads(dag_def_json)

        # 执行 DAG
        dag_executor = _get_dag_executor()
        params = {
            'data_sources': req.get('data_sources', {}),
            'parameters': req.get('parameters', {}),
            'self_party': req.get('self_party'),
            'participants': req.get('participants', [])
        }
        result = dag_executor.run(task_id=task_id, dag_def=dag_def, params=params)

        return jsonify({
            'success': True,
            'task_id': task_id,
            'status': result.get('status', 'completed'),
            'results': result.get('results', {})
        })
    except Exception as e:
        logger.error(f"DAG execution error: {e}")
        return jsonify({'success': False, 'error': str(e)}), 500


@http_app.route('/api/v1/execute/component', methods=['POST'])
def execute_component():
    """HTTP 单组件执行端点"""
    try:
        req = request.get_json()
        task_id = req.get('task_id')
        component_type = req.get('component_type')
        attrs = req.get('attrs', {})
        inputs = req.get('inputs', {})

        if not task_id or not component_type:
            return jsonify({'success': False, 'error': 'task_id and component_type are required'}), 400

        dag_executor = _get_dag_executor()
        handler = dag_executor._component_registry.get(component_type)
        if handler is None:
            return jsonify({'success': False, 'error': f'Unknown component: {component_type}'}), 404

        result = handler(inputs, attrs)
        return jsonify({
            'success': True,
            'task_id': task_id,
            'component': component_type,
            'result': result
        })
    except Exception as e:
        logger.error(f"Component execution error: {e}")
        return jsonify({'success': False, 'error': str(e)}), 500


@http_app.route('/health', methods=['GET'])
def health():
    """健康检查端点"""
    return jsonify({'status': 'healthy', 'service': 'msp-node-http'})


@http_app.route('/api/v1/psi/submit_public_key', methods=['POST'])
def psi_submit_public_key():
    """接收本节点公钥，返回所有参与方的公钥（用于 ECDH 密钥交换）

    请求体:
    {
        "task_id": "xxx",
        "party": "node-hospital",
        "public_key": "04...",
        "participants": ["node-hospital", "node-insurance"]
    }

    响应:
    {
        "success": true,
        "public_keys": {
            "node-hospital": "04...",
            "node-insurance": "04..."
        }
    }
    """
    try:
        req = request.get_json()
        task_id = req.get('task_id')
        party = req.get('party')
        public_key = req.get('public_key')
        participants = req.get('participants', [])

        if not task_id or not party or not public_key:
            return jsonify({'success': False, 'error': 'task_id, party, public_key required'}), 400

        with _ecdh_psi_state['lock']:
            # 存储公钥
            _ecdh_psi_state['public_keys'][party] = public_key

            # 检查是否收集完所有公钥
            all_received = all(p in _ecdh_psi_state['public_keys'] for p in participants)

            if all_received:
                # 返回所有公钥
                result = {
                    'success': True,
                    'public_keys': dict(_ecdh_psi_state['public_keys'])
                }
                # 清理状态（可选，保留到任务完成）
                logger.info(f"ECDHSI key exchange complete for task {task_id}")
            else:
                # 还需等待其他节点
                result = {
                    'success': True,
                    'public_keys': None,
                    'message': f'Waiting for {len(participants) - len(_ecdh_psi_state["public_keys"])} more parties'
                }

        return jsonify(result)

    except Exception as e:
        logger.error(f"PSI public key submission error: {e}")
        return jsonify({'success': False, 'error': str(e)}), 500


@http_app.route('/api/v1/psi/submit_encrypted_ids', methods=['POST'])
def psi_submit_encrypted_ids():
    """接收本节点加密后的 ID，返回交集结果

    请求体:
    {
        "task_id": "xxx",
        "party": "node-hospital",
        "encrypted_ids": ["hash1", "hash2", ...],
        "participants": ["node-hospital", "node-insurance"]
    }

    响应:
    {
        "success": true,
        "intersection": ["hash1", ...]
    }
    """
    try:
        req = request.get_json()
        task_id = req.get('task_id')
        party = req.get('party')
        encrypted_ids = req.get('encrypted_ids', [])
        participants = req.get('participants', [])

        if not task_id or not party or not encrypted_ids:
            return jsonify({'success': False, 'error': 'task_id, party, encrypted_ids required'}), 400

        with _ecdh_psi_state['lock']:
            # 存储加密 ID
            _ecdh_psi_state['encrypted_ids'][party] = encrypted_ids

            # 检查是否收集完所有加密 ID
            all_received = all(p in _ecdh_psi_state['encrypted_ids'] for p in participants)

            if all_received:
                # 计算交集
                all_encrypted = []
                for p in participants:
                    all_encrypted.extend(_ecdh_psi_state['encrypted_ids'].get(p, []))

                # 使用 Counter找出现次数等于参与方数量的加密 ID（所有节点都有）
                from collections import Counter
                counter = Counter(all_encrypted)
                intersection = [eid for eid, count in counter.items() if count >= len(participants)]

                result = {
                    'success': True,
                    'intersection': intersection,
                    'stats': {
                        'total_encrypted': len(all_encrypted),
                        'intersection_size': len(intersection),
                        'parties': len(participants)
                    }
                }

                # 清理状态
                logger.info(f"ECDHSI compute complete for task {task_id}: {len(intersection)} intersections")

                # 保留结果但不清理（等待其他请求获取）
            else:
                # 还需等待其他节点
                result = {
                    'success': True,
                    'intersection': None,
                    'message': f'Waiting for {len(participants) - len(_ecdh_psi_state["encrypted_ids"])} more parties'
                }

        return jsonify(result)

    except Exception as e:
        logger.error(f"PSI encrypted IDs submission error: {e}")
        return jsonify({'success': False, 'error': str(e)}), 500


@http_app.route('/api/v1/psi/get_intersection/<task_id>', methods=['GET'])
def psi_get_intersection(task_id):
    """获取指定任务的交集结果"""
    with _ecdh_psi_state['lock']:
        if not _ecdh_psi_state['encrypted_ids']:
            return jsonify({'success': False, 'error': 'No intersection data available'}), 404

        # 重新计算交集并返回
        all_encrypted = []
        for ids in _ecdh_psi_state['encrypted_ids'].values():
            all_encrypted.extend(ids)

        from collections import Counter
        counter = Counter(all_encrypted)
        participants_count = len(_ecdh_psi_state['encrypted_ids'])
        intersection = [eid for eid, count in counter.items() if count >= participants_count]

        return jsonify({
            'success': True,
            'task_id': task_id,
            'intersection': intersection,
            'size': len(intersection)
        })


@http_app.route('/api/v1/psi/reset', methods=['POST'])
def psi_reset():
    """重置 ECDH PSI 状态（用于清理）"""
    with _ecdh_psi_state['lock']:
        _ecdh_psi_state['public_keys'].clear()
        _ecdh_psi_state['encrypted_ids'].clear()

    return jsonify({'success': True, 'message': 'ECDHSI state reset'})


class MSPNodeServicer(msp_node_pb2_grpc.MSPNodeServicer):
    """MSP节点gRPC服务实现"""

    def __init__(self, config: dict):
        self.config = config
        self.runners = {}
        self.dag_executor = None
        self._init_runners()
        self._init_dag_executor()

    def _init_runners(self):
        """初始化运行器"""
        self.runners['psi'] = PSIRunner(self.config)
        self.runners['fl'] = FLRunner(self.config)
        self.runners['mpc'] = MPCRunner(self.config)

    def _init_dag_executor(self):
        """初始化DAG执行器"""
        from msp_node.runners.dag_executor import DAGExecutor
        self.dag_executor = DAGExecutor(self.config)
        self.dag_executor.initialize()

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
    global _http_config
    _http_config = config or {}
    return GRPCServer(port=port, config=config)


def start_http_server(port: int = 50052):
    """启动 Flask HTTP 服务器（在单独线程中）"""
    global http_app
    http_port = int(os.environ.get('HTTP_PORT', port))
    logger.info(f"Starting HTTP server on port {http_port}")
    http_app.run(host='0.0.0.0', port=http_port, debug=False, threaded=True)


if __name__ == '__main__':
    logging.basicConfig(
        level=logging.INFO,
        format='%(asctime)s - %(name)s - %(levelname)s - %(message)s'
    )

    # =============================================
    # 在主线程初始化 Ray 集群（容器启动时执行一次）
    # =============================================
    node_id = os.environ.get('NODE_ID', 'node-a')
    is_head = os.environ.get('RAY_HEAD', 'true').lower() == 'true'

    logger.info(f"Initializing Ray at startup: node_id={node_id}, is_head={is_head}")

    # 调用 init_ray() 建立 Ray 集群（head 启动或 worker 连接）
    init_ray()

    # Head 节点：在主线程初始化 SecretFlow（避免 FedAPI 线程错误）
    if is_head:
        _init_secretflow_at_startup()
    else:
        # Worker 节点也初始化 SF（连接到 head 后）
        time.sleep(2)  # 等待连接稳定
        _init_secretflow_at_startup()

    # =============================================
    # 启动心跳服务（向NodeManager注册并定期发送心跳）
    # =============================================
    node_manager_url = os.environ.get('NODE_MANAGER_URL', 'http://msp-node-manager:8082')
    heartbeat_interval = int(os.environ.get('HEARTBEAT_INTERVAL', '3'))
    start_heartbeat_service(node_manager_url, interval=heartbeat_interval)

    # 启动 Flask HTTP 服务器（在单独线程中）
    http_port = int(os.environ.get('HTTP_PORT', 50052))
    http_thread = threading.Thread(target=start_http_server, args=(http_port,), daemon=True)
    http_thread.start()
    logger.info(f"Flask HTTP server started in background on port {http_port}")

    # 启动 gRPC 服务器
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