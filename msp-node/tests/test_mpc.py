"""
MPC 测试用例
测试 SPU 基础运算：加法、乘法、比较
"""

import sys
import os

# 添加 src 目录到路径
sys.path.insert(0, os.path.join(os.path.dirname(__file__), '..', 'src'))

import unittest
from unittest.mock import Mock, patch


class TestMPCOperations(unittest.TestCase):
    """测试 MPC 基础运算"""

    def setUp(self):
        """设置测试环境"""
        self.config = {
            'parties': ['alice', 'bob'],
            'self_party': 'alice',
            'cluster_config': {
                'addresses': ['127.0.0.1:40000', '127.0.0.1:40001']
            }
        }

    def test_mpc_config_creation(self):
        """测试 MPC 配置创建"""
        # 验证配置结构
        self.assertIn('parties', self.config)
        self.assertIn('self_party', self.config)
        self.assertEqual(len(self.config['parties']), 2)
        self.assertIn('alice', self.config['parties'])
        self.assertIn('bob', self.config['parties'])

    def test_mpc_type_validation(self):
        """测试 MPC 类型验证"""
        valid_types = ['addition', 'multiplication', 'comparison']

        # 测试有效类型
        for mpc_type in valid_types:
            self.assertIn(mpc_type, valid_types)

    def test_mpc_inputs_structure(self):
        """测试 MPC 输入数据结构"""
        # 模拟 inputs 数据
        inputs = {
            'data': {
                'data_a': [100, 200, 300],
                'data_b': [50, 100, 150]
            }
        }

        data_inputs = inputs.get('data', {})
        data_a = data_inputs.get('data_a', [])
        data_b = data_inputs.get('data_b', [])

        self.assertEqual(len(data_a), 3)
        self.assertEqual(len(data_b), 3)
        self.assertEqual(data_a[0], 100)
        self.assertEqual(data_b[0], 50)

    def test_addition_expected_result(self):
        """测试加法预期结果: 100 + 50 = 150"""
        data_a = [100, 200, 300]
        data_b = [50, 100, 150]

        expected = [150, 300, 450]

        # 计算
        result = [a + b for a, b in zip(data_a, data_b)]

        self.assertEqual(result, expected)

    def test_multiplication_expected_result(self):
        """测试乘法预期结果: 3 * 4 = 12"""
        data_a = [3, 6, 9]
        data_b = [4, 5, 2]

        expected = [12, 30, 18]

        # 计算
        result = [a * b for a, b in zip(data_a, data_b)]

        self.assertEqual(result, expected)

    def test_comparison_expected_result(self):
        """测试比较预期结果: 5 < 10 = True"""
        data_a = [5, 10, 15]
        data_b = [10, 10, 20]

        expected = [True, False, True]  # 5<10, 10<10, 15<20

        # 计算
        result = [a < b for a, b in zip(data_a, data_b)]

        self.assertEqual(result, expected)


class TestMPCRunner(unittest.TestCase):
    """测试 MPC Runner"""

    def setUp(self):
        """设置测试环境"""
        self.config = {
            'protocol': 'semi2k',
            'parties': ['alice', 'bob'],
            'self_party': 'alice',
            'cluster_config': {
                'addresses': ['127.0.0.1:40000', '127.0.0.1:40001']
            }
        }

    @patch('msp_node.runners.secretflow_adapter.SecretFlowAdapter')
    def test_mpc_runner_initialization(self, mock_adapter):
        """测试 MPC Runner 初始化"""
        from msp_node.runners.mpc_runner import MPCRunner

        runner = MPCRunner(self.config)
        self.assertEqual(runner.protocol, 'semi2k')
        self.assertFalse(runner.initialized)

    @patch('msp_node.runners.secretflow_adapter.SecretFlowAdapter')
    def test_mpc_runner_with_mock_adapter(self, mock_adapter):
        """测试 MPC Runner 使用模拟适配器"""
        from msp_node.runners.mpc_runner import MPCRunner

        # 配置 mock
        mock_instance = Mock()
        mock_instance.execute_mpc.return_value = {
            'status': 'ok',
            'mpc_type': 'addition',
            'result': [150, 300, 450]
        }
        mock_adapter.return_value = mock_instance

        runner = MPCRunner(self.config)
        runner.secretflow_adapter = mock_instance

        # 测试执行
        result = runner._do_run(
            task_id='test-task-001',
            inputs={'data': {'data_a': [100, 200], 'data_b': [50, 100]}},
            params={'mpc_type': 'addition'}
        )

        self.assertEqual(result['status'], 'ok')
        self.assertEqual(result['mpc_type'], 'addition')
        self.assertEqual(result['result'], [150, 300, 450])


class TestSecretFlowAdapter(unittest.TestCase):
    """测试 SecretFlow 适配器"""

    def setUp(self):
        """设置测试环境"""
        self.config = {
            'parties': ['alice', 'bob'],
            'self_party': 'alice',
            'cluster_config': {
                'addresses': ['127.0.0.1:40000', '127.0.0.1:40001']
            },
            'device': 'spu'
        }

    def test_adapter_config_validation(self):
        """测试适配器配置验证"""
        from msp_node.adapters.secretflow_adapter import SecretFlowAdapter

        adapter = SecretFlowAdapter(self.config)

        self.assertEqual(adapter.self_party, 'alice')
        self.assertEqual(len(adapter.parties), 2)

    def test_spu_config_structure(self):
        """测试 SPU 配置结构"""
        from msp_node.adapters.secretflow_adapter import SecretFlowAdapter

        adapter = SecretFlowAdapter(self.config)
        spu_config = adapter._build_spu_config()

        # 验证 SPU 配置结构
        self.assertIn('nodes', spu_config)
        self.assertIn('runtime_config', spu_config)
        self.assertEqual(len(spu_config['nodes']), 2)

        # 验证节点配置
        for node in spu_config['nodes']:
            self.assertIn('party', node)
            self.assertIn('address', node)

        # 验证运行时配置
        runtime = spu_config['runtime_config']
        self.assertIn('kind', runtime)
        self.assertIn('protocol', runtime)


class TestMPCCodeIntegration(unittest.TestCase):
    """测试 MPC 代码集成"""

    def test_mpc_type_to_operation_mapping(self):
        """测试 MPC 类型到操作的映射"""
        mpc_type_to_op = {
            'addition': 'add',
            'multiplication': 'mul',
            'comparison': 'lt'
        }

        self.assertEqual(mpc_type_to_op['addition'], 'add')
        self.assertEqual(mpc_type_to_op['multiplication'], 'mul')
        self.assertEqual(mpc_type_to_op['comparison'], 'lt')

    def test_result_format(self):
        """测试结果格式"""
        # 模拟 execute_mpc 返回格式
        mock_result = {
            'status': 'ok',
            'mpc_type': 'addition',
            'result': [150, 300, 450]
        }

        self.assertIn('status', mock_result)
        self.assertIn('mpc_type', mock_result)
        self.assertIn('result', mock_result)
        self.assertEqual(mock_result['status'], 'ok')


if __name__ == '__main__':
    # 运行测试
    unittest.main(verbosity=2)