# 审计日志模块

## TC-AUDIT-001: 查询审计日志列表

**测试目标**: 验证审计日志分页查询

**请求**
```bash
GET /api/v1/msp/audit-logs
Authorization: Bearer <token>
```

**Query参数**:
- `page` (可选, 默认0)
- `size` (可选, 默认10)
- `action` (可选, 如: LOGIN, CREATE_TASK, UPDATE_NODE)
- `userId` (可选)

**预期响应**
```json
{
  "success": true,
  "code": "200",
  "message": "success",
  "data": {
    "content": [
      {
        "logId": "<日志ID>",
        "userId": "<用户ID>",
        "action": "LOGIN",
        "resourceType": "AUTH",
        "resourceId": null,
        "details": "用户登录成功",
        "ipAddress": "127.0.0.1",
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

## TC-AUDIT-002: 查询特定资源的审计日志

**测试目标**: 验证按资源类型和ID查询审计日志

**请求**
```bash
GET /api/v1/msp/audit-logs/resource/<resourceType>/<resourceId>
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
      "logId": "<日志ID>",
      "action": "UPDATE_TASK",
      "resourceType": "TASK",
      "resourceId": "<任务ID>",
      "details": "任务状态更新为RUNNING",
      "createTime": 1747891200000
    }
  ]
}
```

**执行结果**: ✅ 通过

---

## 汇总

| 用例ID | 名称 | 优先级 | 执行结果 |
|--------|------|--------|----------|
| TC-AUDIT-001 | 查询审计日志列表 | P0 | ✅ 通过 |
| TC-AUDIT-002 | 查询特定资源审计日志 | P0 | ✅ 通过 |
