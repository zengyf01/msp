# 节点管理模块

## TC-NODE-001: 创建节点

**测试目标**: 验证节点注册功能

**请求**
```bash
POST /api/v1/msp/nodes
Content-Type: application/json
Authorization: Bearer <token>

{
  "nodeName": "测试节点1",
  "endpoint": "http://node1:8080",
  "capabilities": ["PSI", "MPC"],
  "tags": ["测试", "生产"]
}
```

**预期响应**
```json
{
  "success": true,
  "code": "200",
  "message": "success",
  "data": "<节点ID>"
}
```

**执行结果**: ✅ 通过

---

## TC-NODE-002: 查询节点列表

**测试目标**: 验证节点列表查询

**请求**
```bash
GET /api/v1/msp/nodes
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
      "nodeId": "<节点ID>",
      "nodeName": "测试节点1",
      "status": "ONLINE",
      "endpoint": "http://node1:8080",
      "capabilities": ["PSI", "MPC"],
      "tags": ["测试", "生产"],
      "createTime": 1747891200000
    }
  ]
}
```

**执行结果**: ✅ 通过

---

## TC-NODE-003: 查询节点详情

**测试目标**: 验证节点详情查询

**请求**
```bash
GET /api/v1/msp/nodes/<nodeId>
Authorization: Bearer <token>
```

**预期响应**
```json
{
  "success": true,
  "code": "200",
  "message": "success",
  "data": {
    "nodeId": "<节点ID>",
    "nodeName": "测试节点1",
    "status": "ONLINE",
    "endpoint": "http://node1:8080",
    "capabilities": ["PSI", "MPC"],
    "tags": ["测试", "生产"],
    "createTime": 1747891200000,
    "updateTime": 1747891200000
  }
}
```

**执行结果**: ✅ 通过

---

## TC-NODE-004: 更新节点状态

**测试目标**: 验证节点状态更新

**请求**
```bash
PUT /api/v1/msp/nodes/<nodeId>/status
Content-Type: application/json
Authorization: Bearer <token>

{
  "status": "OFFLINE"
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

## TC-NODE-005: 删除节点

**测试目标**: 验证节点删除

**请求**
```bash
DELETE /api/v1/msp/nodes/<nodeId>
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

## 汇总

| 用例ID | 名称 | 优先级 | 执行结果 |
|--------|------|--------|----------|
| TC-NODE-001 | 创建节点 | P0 | ✅ 通过 |
| TC-NODE-002 | 查询节点列表 | P0 | ✅ 通过 |
| TC-NODE-003 | 查询节点详情 | P0 | ✅ 通过 |
| TC-NODE-004 | 更新节点状态 | P0 | ✅ 通过 |
| TC-NODE-005 | 删除节点 | P0 | ✅ 通过 |
