# 任务管理模块

## TC-TASK-001: 创建任务

**测试目标**: 验证任务创建功能

**请求**
```bash
POST /api/v1/msp/tasks
Content-Type: application/json
Authorization: Bearer <token>

{
  "name": "PSI任务测试",
  "type": "PSI",
  "algorithm": "RSA",
  "description": "测试隐私求交任务",
  "participants": ["node1", "node2"],
  "inputs": {
    "dataSourceId": "<数据源ID>",
    "columns": ["id", "name"]
  },
  "parameters": {
    "matchType": "INTERSECTION"
  }
}
```

**预期响应**
```json
{
  "success": true,
  "code": "200",
  "message": "success",
  "data": "<任务ID>"
}
```

**执行结果**: ✅ 通过

---

## TC-TASK-002: 查询任务列表

**测试目标**: 验证任务列表查询（分页）

**请求**
```bash
GET /api/v1/msp/tasks
Authorization: Bearer <token>
```

**Query参数**:
- `page` (可选, 默认0)
- `size` (可选, 默认10)
- `status` (可选, 如: CREATED, PENDING, RUNNING, COMPLETED, FAILED)

**预期响应**
```json
{
  "success": true,
  "code": "200",
  "message": "success",
  "data": {
    "content": [
      {
        "taskId": "<任务ID>",
        "name": "PSI任务测试",
        "type": "PSI",
        "algorithm": "RSA",
        "status": "CREATED",
        "createTime": 1747891200000
      }
    ],
    "totalElements": 1,
    "totalPages": 1,
    "currentPage": 0
  }
}
```

**执行结果**: ✅ 通过

---

## TC-TASK-003: 查询任务详情

**测试目标**: 验证任务详情查询

**请求**
```bash
GET /api/v1/msp/tasks/<taskId>
Authorization: Bearer <token>
```

**预期响应**
```json
{
  "success": true,
  "code": "200",
  "message": "success",
  "data": {
    "taskId": "<任务ID>",
    "name": "PSI任务测试",
    "type": "PSI",
    "algorithm": "RSA",
    "status": "CREATED",
    "participants": ["node1", "node2"],
    "inputs": {},
    "parameters": {},
    "description": "测试隐私求交任务",
    "createTime": 1747891200000,
    "updateTime": 1747891200000
  }
}
```

**执行结果**: ✅ 通过

---

## TC-TASK-004: 更新任务状态

**测试目标**: 验证任务状态更新

**请求**
```bash
PUT /api/v1/msp/tasks/<taskId>/status
Content-Type: application/json
Authorization: Bearer <token>

{
  "status": "RUNNING"
}
```

**预期响应**
```json
{
  "success": true,
  "code": "200",
  "message": "success"
}
```

**执行结果**: ✅ 通过

---

## TC-TASK-005: 删除任务

**测试目标**: 验证任务删除

**请求**
```bash
DELETE /api/v1/msp/tasks/<taskId>
Authorization: Bearer <token>
```

**预期响应**
```json
{
  "success": true,
  "code": "200",
  "message": "success"
}
```

**执行结果**: ✅ 通过

---

## TC-TASK-006: 按状态查询任务

**测试目标**: 验证按状态过滤查询任务

**请求**
```bash
GET /api/v1/msp/tasks/status/RUNNING
Authorization: Bearer <token>
```

**预期响应**
```json
{
  "success": true,
  "code": "200",
  "message": "success",
  "data": [
    {
      "taskId": "<任务ID>",
      "name": "运行中的任务",
      "type": "PSI",
      "status": "RUNNING"
    }
  ]
}
```

**执行结果**: ✅ 通过

---

## TC-TASK-007: 集成测试 - 完整任务流程

**测试目标**: 验证完整任务生命周期

```
1. 管理员登录
   POST /api/v1/msp/auth/login
   -> 获取token

2. 创建节点
   POST /api/v1/msp/nodes
   -> 获取nodeId

3. 创建数据源
   POST /api/v1/msp/datasources
   -> 获取dataSourceId

4. 创建任务
   POST /api/v1/msp/tasks
   -> 获取taskId

5. 查询任务列表确认
   GET /api/v1/msp/tasks

6. 更新任务状态为RUNNING
   PUT /api/v1/msp/tasks/<taskId>/status
   Body: {"status": "RUNNING"}

7. 查询任务详情
   GET /api/v1/msp/tasks/<taskId>

8. 更新任务状态为COMPLETED
   PUT /api/v1/msp/tasks/<taskId>/status
   Body: {"status": "COMPLETED"}

9. 查询审计日志确认操作记录
   GET /api/v1/msp/audit-logs?userId=<userId>
```

**执行结果**: ✅ 通过

---

## 汇总

| 用例ID | 名称 | 优先级 | 执行结果 |
|--------|------|--------|----------|
| TC-TASK-001 | 创建任务 | P0 | ✅ 通过 |
| TC-TASK-002 | 查询任务列表 | P0 | ✅ 通过 |
| TC-TASK-003 | 查询任务详情 | P0 | ✅ 通过 |
| TC-TASK-004 | 更新任务状态 | P0 | ✅ 通过 |
| TC-TASK-005 | 删除任务 | P0 | ✅ 通过 |
| TC-TASK-006 | 按状态查询任务 | P0 | ✅ 通过 |
| TC-TASK-007 | 完整任务流程 | P0 | ✅ 通过 |
