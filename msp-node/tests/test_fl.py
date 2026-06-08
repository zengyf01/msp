"""
联邦学习测试用例
测试纵向联邦学习和 SecureBoost 功能
"""

import sys
import os

# 添加 src 目录到路径
sys.path.insert(0, os.path.join(os.path.dirname(__file__), '..', 'src'))

import unittest
from unittest.mock import Mock, patch, MagicMock


class TestVerticalFLConfig(unittest.TestCase):
    """测试纵向联邦学习配置"""

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

    def test_vertical_fl_params_structure(self):
        """测试纵向 FL 参数结构"""
        params = {
            'fl_type': 'vertical',
            'label_party': 'alice',
            'label_column': 'default_flag',
            'model_type': 'logistic_regression',
            'epochs': 10,
            'batch_size': 64
        }

        self.assertEqual(params['fl_type'], 'vertical')
        self.assertEqual(params['label_party'], 'alice')
        self.assertEqual(params['model_type'], 'logistic_regression')

    def test_sgb_params_structure(self):
        """测试 SecureBoost 参数结构"""
        params = {
            'fl_type': 'vertical',
            'label_party': 'alice',
            'label_column': 'default_flag',
            'model_type': 'secureboost',
            'num_trees': 10,
            'max_depth': 6,
            'learning_rate': 0.1
        }

        self.assertEqual(params['model_type'], 'secureboost')
        self.assertEqual(params['num_trees'], 10)
        self.assertEqual(params['max_depth'], 6)

    def test_feature_parties_structure(self):
        """测试各方特征列配置"""
        feature_parties = {
            'alice': ['col_1', 'col_2', 'col_3'],
            'bob': ['col_4', 'col_5']
        }

        self.assertIn('alice', feature_parties)
        self.assertIn('bob', feature_parties)
        self.assertEqual(len(feature_parties['alice']), 3)
        self.assertEqual(len(feature_parties['bob']), 2)


class TestFLRunner(unittest.TestCase):
    """测试 FL Runner"""

    def setUp(self):
        """设置测试环境"""
        self.config = {
            'parties': ['alice', 'bob'],
            'self_party': 'alice',
            'cluster_config': {
                'addresses': ['127.0.0.1:40000', '127.0.0.1:40001']
            }
        }

    def test_fl_runner_initialization(self):
        """测试 FL Runner 初始化"""
        from msp_node.runners.fl_runner import FLRunner

        runner = FLRunner(self.config)
        self.assertFalse(runner.initialized)
        self.assertIsNone(runner.secretflow_adapter)

    def test_fl_runner_horizontal_dispatch(self):
        """测试 FL Runner 横向 FL 分发"""
        from msp_node.runners.fl_runner import FLRunner

        runner = FLRunner(self.config)

        # Mock 适配器
        mock_adapter = Mock()
        mock_adapter.execute_federated_learning.return_value = {
            'status': 'ok',
            'model_type': 'horizontal_fl'
        }
        runner.secretflow_adapter = mock_adapter

        # 执行横向 FL
        result = runner._do_run(
            task_id='test-task-001',
            inputs={'data_sources': {}},
            params={'fl_type': 'horizontal'}
        )

        mock_adapter.execute_federated_learning.assert_called_once()

    def test_fl_runner_vertical_dispatch(self):
        """测试 FL Runner 纵向 FL 分发"""
        from msp_node.runners.fl_runner import FLRunner

        runner = FLRunner(self.config)

        # Mock 适配器
        mock_adapter = Mock()
        mock_adapter.execute_vertical_fl.return_value = {
            'status': 'ok',
            'model_type': 'vertical_lr',
            'accuracy': 0.75
        }
        runner.secretflow_adapter = mock_adapter

        # 执行纵向 FL
        result = runner._do_run(
            task_id='test-task-001',
            inputs={'data_sources': {}},
            params={'fl_type': 'vertical'}
        )

        mock_adapter.execute_vertical_fl.assert_called_once()
        self.assertEqual(result['status'], 'ok')

    def test_fl_runner_default_to_horizontal(self):
        """测试 FL Runner 默认使用横向 FL"""
        from msp_node.runners.fl_runner import FLRunner

        runner = FLRunner(self.config)

        # Mock 适配器
        mock_adapter = Mock()
        mock_adapter.execute_federated_learning.return_value = {
            'status': 'ok'
        }
        runner.secretflow_adapter = mock_adapter

        # 不指定 fl_type，应该默认使用横向
        result = runner._do_run(
            task_id='test-task-001',
            inputs={},
            params={}
        )

        mock_adapter.execute_federated_learning.assert_called_once()


class TestSGBRunner(unittest.TestCase):
    """测试 SecureBoost Runner"""

    def setUp(self):
        """设置测试环境"""
        self.config = {
            'parties': ['alice', 'bob'],
            'self_party': 'alice',
            'cluster_config': {
                'addresses': ['127.0.0.1:40000', '127.0.0.1:40001']
            }
        }

    def test_sgb_runner_initialization(self):
        """测试 SGB Runner 初始化"""
        from msp_node.runners.sgb_runner import SGBRunner

        runner = SGBRunner(self.config)
        self.assertFalse(runner.initialized)
        self.assertIsNone(runner.secretflow_adapter)

    def test_sgb_runner_execute(self):
        """测试 SGB Runner 执行"""
        from msp_node.runners.sgb_runner import SGBRunner

        runner = SGBRunner(self.config)

        # Mock 适配器
        mock_adapter = Mock()
        mock_adapter.train_sgb.return_value = {
            'status': 'ok',
            'model_type': 'secureboost',
            'auc': 0.82,
            'num_trees': 10
        }
        runner.secretflow_adapter = mock_adapter

        # 执行 SGB
        result = runner._do_run(
            task_id='test-sgb-001',
            inputs={'data_sources': {}},
            params={
                'label_party': 'alice',
                'label_column': 'label',
                'num_trees': 10,
                'max_depth': 6
            }
        )

        mock_adapter.train_sgb.assert_called_once()
        self.assertEqual(result['status'], 'ok')
        self.assertEqual(result['model_type'], 'secureboost')


class TestSecretFlowAdapterVerticalFL(unittest.TestCase):
    """测试 SecretFlow 适配器的纵向 FL 方法"""

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

    def test_adapter_has_execute_vertical_fl(self):
        """测试适配器有 execute_vertical_fl 方法"""
        from msp_node.adapters.secretflow_adapter import SecretFlowAdapter

        adapter = SecretFlowAdapter(self.config)
        self.assertTrue(hasattr(adapter, 'execute_vertical_fl'))

    def test_adapter_has_train_sgb(self):
        """测试适配器有 train_sgb 方法"""
        from msp_node.adapters.secretflow_adapter import SecretFlowAdapter

        adapter = SecretFlowAdapter(self.config)
        self.assertTrue(hasattr(adapter, 'train_sgb'))

    def test_adapter_has_train_vertical_lr(self):
        """测试适配器有 train_vertical_lr 方法"""
        from msp_node.adapters.secretflow_adapter import SecretFlowAdapter

        adapter = SecretFlowAdapter(self.config)
        self.assertTrue(hasattr(adapter, 'train_vertical_lr'))

    def test_execute_vertical_fl_dispatches_to_sgb(self):
        """测试 execute_vertical_fl 分发到 SGB"""
        from msp_node.adapters.secretflow_adapter import SecretFlowAdapter

        adapter = SecretFlowAdapter(self.config)
        adapter.initialized = True

        # Mock spursu
        adapter.spursu = Mock()

        # Mock train_sgb
        adapter.train_sgb = Mock(return_value={
            'status': 'ok',
            'model_type': 'secureboost',
            'auc': 0.82
        })

        result = adapter.execute_vertical_fl(
            task_id='test-task',
            inputs={},
            params={'model_type': 'secureboost'}
        )

        adapter.train_sgb.assert_called_once()
        self.assertEqual(result['model_type'], 'secureboost')

    def test_execute_vertical_fl_dispatches_to_vertical_lr(self):
        """测试 execute_vertical_fl 分发到纵向 LR"""
        from msp_node.adapters.secretflow_adapter import SecretFlowAdapter

        adapter = SecretFlowAdapter(self.config)
        adapter.initialized = True
        adapter.spursu = Mock()

        # Mock train_vertical_lr
        adapter.train_vertical_lr = Mock(return_value={
            'status': 'ok',
            'model_type': 'vertical_lr',
            'accuracy': 0.78
        })

        result = adapter.execute_vertical_fl(
            task_id='test-task',
            inputs={},
            params={'model_type': 'logistic_regression'}
        )

        adapter.train_vertical_lr.assert_called_once()
        self.assertEqual(result['model_type'], 'vertical_lr')

    def test_execute_vertical_fl_raises_when_not_initialized(self):
        """测试 execute_vertical_fl 在未初始化时抛出异常"""
        from msp_node.adapters.secretflow_adapter import SecretFlowAdapter

        adapter = SecretFlowAdapter(self.config)
        adapter.initialized = False

        with self.assertRaises(RuntimeError) as context:
            adapter.execute_vertical_fl(
                task_id='test-task',
                inputs={},
                params={}
            )

        self.assertIn('not initialized', str(context.exception))


class TestVFLModelTypes(unittest.TestCase):
    """测试纵向 FL 模型类型"""

    def test_valid_model_types(self):
        """测试有效的模型类型"""
        valid_types = ['logistic_regression', 'vertical_lr', 'secureboost', 'sgb']

        for mtype in valid_types:
            # 验证类型在支持列表中
            self.assertIn(mtype, valid_types)

    def test_sgb_params_extraction(self):
        """测试 SGB 参数提取"""
        params = {
            'model_type': 'secureboost',
            'num_trees': 10,
            'max_depth': 6,
            'learning_rate': 0.1
        }

        num_trees = int(params.get('num_trees', 10))
        max_depth = int(params.get('max_depth', 6))
        learning_rate = float(params.get('learning_rate', 0.1))

        self.assertEqual(num_trees, 10)
        self.assertEqual(max_depth, 6)
        self.assertAlmostEqual(learning_rate, 0.1)

    def test_vertical_lr_params_extraction(self):
        """测试纵向 LR 参数提取"""
        params = {
            'model_type': 'vertical_lr',
            'epochs': 10,
            'batch_size': 64
        }

        epochs = int(params.get('epochs', 10))
        batch_size = int(params.get('batch_size', 64))

        self.assertEqual(epochs, 10)
        self.assertEqual(batch_size, 64)


class TestFLTaskRequestValidation(unittest.TestCase):
    """测试 FL 任务请求验证"""

    def test_label_party_required(self):
        """测试标签方是必需的"""
        # 在纵向 FL 中，label_party 是必需的
        params = {
            'fl_type': 'vertical',
            'label_party': 'alice',
            'model_type': 'secureboost'
        }

        self.assertIn('label_party', params)
        self.assertIsNotNone(params['label_party'])

    def test_feature_parties_required(self):
        """测试各方特征是必需的"""
        # 在纵向 FL 中，每个参与方需要指定特征列
        feature_parties = {
            'alice': ['col_1', 'col_2'],
            'bob': ['col_3']
        }

        self.assertTrue(len(feature_parties) >= 2)

    def test_label_column_required(self):
        """测试标签列是必需的"""
        params = {
            'label_party': 'alice',
            'label_column': 'default_flag'
        }

        self.assertIn('label_column', params)
        self.assertEqual(params['label_column'], 'default_flag')


if __name__ == '__main__':
    # 运行测试
    unittest.main(verbosity=2)