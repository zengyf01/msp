# MPC运算模块

## TC-MPC-001: 两方加法

**测试目标**: 验证MPC加法运算返回正确结果 (100 + 200 = 300)

**前置条件**: 已启动模拟节点 alice 和 bob，SecretFlow >= 1.14.0

**测试步骤**:
1. 准备测试数据
```python
# 节点A数据
data_a = [100, 200, 300]

# 节点B数据
data_b = [50, 100, 150]
```

2. 执行MPC加法
```bash
POST /api/v1/msp/tasks
Content-Type: application/json
Authorization: Bearer <token>

{
  "name": "MPC加法测试",
  "type": "MPC",
  "algorithm": "semi2k",
  "participants": ["alice", "bob"],
  "parameters": {
    "mpc_type": "addition"
  },
  "inputs": {
    "data_a": [100, 200, 300],
    "data_b": [50, 100, 150]
  }
}
```

3. 验证结果
```bash
GET /api/v1/msp/tasks/<taskId>/result
Authorization: Bearer <token>
```

**预期结果**:
```json
{
  "status": "ok",
  "mpc_type": "addition",
  "result": [150, 300, 450]
}
```

**执行结果**: ✅ 通过 (容器内测试)

---

## TC-MPC-002: 两方乘法

**测试目标**: 验证MPC乘法运算返回正确结果 (3 × 4 = 12)

**前置条件**: 已启动模拟节点 alice 和 bob

**测试步骤**:
1. 执行MPC乘法
```bash
POST /api/v1/msp/tasks
Content-Type: application/json
Authorization: Bearer <token>

{
  "name": "MPC乘法测试",
  "type": "MPC",
  "algorithm": "semi2k",
  "participants": ["alice", "bob"],
  "parameters": {
    "mpc_type": "multiplication"
  },
  "inputs": {
    "data_a": [3],
    "data_b": [4]
  }
}
```

**预期结果**:
```json
{
  "status": "ok",
  "mpc_type": "multiplication",
  "result": [12]
}
```

**执行结果**: ✅ 通过 (容器内测试)

---

## TC-MPC-003: 两方比较

**测试目标**: 验证MPC比较运算返回正确结果 (5 < 10 = true)

**前置条件**: 已启动模拟节点 alice 和 bob

**测试步骤**:
1. 执行MPC比较
```bash
POST /api/v1/msp/tasks
Content-Type: application/json
Authorization: Bearer <token>

{
  "name": "MPC比较测试",
  "type": "MPC",
  "algorithm": "semi2k",
  "participants": ["alice", "bob"],
  "parameters": {
    "mpc_type": "comparison"
  },
  "inputs": {
    "data_a": [5, 10, 15],
    "data_b": [10, 10, 20]
  }
}
```

**预期结果**:
```json
{
  "status": "ok",
  "mpc_type": "comparison",
  "result": [true, false, true]
}
```

**执行结果**: ✅ 通过 (容器内测试)

---

## 汇总

| 用例ID | 名称 | 优先级 | 执行结果 |
|--------|------|--------|----------|
| TC-MPC-001 | 两方加法 (100+200=300) | P0 | ✅ 通过 |
| TC-MPC-002 | 两方乘法 (3×4=12) | P0 | ✅ 通过 |
| TC-MPC-003 | 两方比较 (5<10=true) | P0 | ✅ 通过 |
