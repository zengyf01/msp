# 联邦学习模块

## TC-FL-001: 两方纵向 LR

**测试目标**: 验证纵向逻辑回归训练收敛，准确率 > 70%

**前置条件**: 已启动模拟节点 alice 和 bob，SecretFlow >= 1.15.0

**测试步骤**:
1. 创建纵向联邦学习任务，选择逻辑回归模型
2. 指定标签提供方 (alice) 和特征提供方 (bob)
3. 执行训练

**请求**
```bash
POST /api/v1/msp/tasks
Content-Type: application/json
Authorization: Bearer <token>

{
  "name": "纵向LR训练任务",
  "type": "VERTICAL_FL",
  "algorithm": "vertical_lr",
  "participants": ["alice", "bob"],
  "labelParty": "alice",
  "labelColumn": "default_flag",
  "modelType": "logistic_regression",
  "featureParties": {
    "alice": ["col_1", "col_2"],
    "bob": ["col_3", "col_4"]
  },
  "parameters": {
    "fl_type": "vertical",
    "epochs": "10",
    "batch_size": "64"
  },
  "description": "测试纵向逻辑回归"
}
```

**预期结果**:
- 任务创建成功
- 训练完成，准确率 > 70%

**执行结果**: ⏳ 待集成测试 (代码已完成，需 SecretFlow >= 1.15.0 环境)

---

## TC-FL-002: 两方 SecureBoost

**测试目标**: 验证 SecureBoost 训练完成，AUC > 0.75

**前置条件**: 已启动模拟节点 alice 和 bob，SecretFlow >= 1.15.0

**测试步骤**:
1. 创建纵向联邦学习任务，选择 SecureBoost 模型
2. 指定标签提供方、特征提供方和 SGB 参数
3. 执行训练

**请求**
```bash
POST /api/v1/msp/tasks
Content-Type: application/json
Authorization: Bearer <token>

{
  "name": "SecureBoost训练任务",
  "type": "VERTICAL_FL",
  "algorithm": "secureboost",
  "participants": ["alice", "bob"],
  "labelParty": "alice",
  "labelColumn": "default_flag",
  "modelType": "secureboost",
  "featureParties": {
    "alice": ["col_1", "col_2"],
    "bob": ["col_3", "col_4"]
  },
  "parameters": {
    "fl_type": "vertical",
    "num_trees": "10",
    "max_depth": "6",
    "learning_rate": "0.1"
  },
  "description": "测试SecureBoost"
}
```

**预期结果**:
- 任务创建成功
- 训练完成，AUC > 0.75

**执行结果**: ⏳ 待集成测试 (代码已完成，需 SecretFlow >= 1.15.0 环境)

---

## 联邦学习模型对比

| 模型 | 类型 | 适用场景 | 特点 |
|------|------|----------|------|
| Logistic Regression | 纵向联邦 | 二分类 | 可解释性强，训练快 |
| SecureBoost | 纵向联邦 | 复杂非线性 | AUC高，特征选择 |

---

## 汇总

| 用例ID | 名称 | 优先级 | 执行结果 |
|--------|------|--------|----------|
| TC-FL-001 | 两方纵向 LR | P0 | ⏳ 待集成测试 |
| TC-FL-002 | 两方 SecureBoost | P0 | ⏳ 待集成测试 |
