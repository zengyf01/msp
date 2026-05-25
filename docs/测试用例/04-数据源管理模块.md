# 数据源管理模块

## TC-DS-001: 创建数据源

**测试目标**: 验证数据源创建功能

**请求**
```bash
POST /api/v1/msp/datasources
Content-Type: application/json
Authorization: Bearer <token>

{
  "nodeId": "<节点ID>",
  "name": "MySQL测试数据源",
  "type": "MYSQL",
  "host": "192.168.1.100",
  "port": 3306,
  "database": "test_db",
  "tableName": "user_data",
  "columns": ["id", "name", "email"]
}
```

**预期响应**
```json
{
  "success": true,
  "code": "200",
  "message": "success",
  "data": "<数据源ID>"
}
```

**执行结果**: ✅ 通过

---

## TC-DS-002: 查询数据源列表

**测试目标**: 验证数据源列表查询

**请求**
```bash
GET /api/v1/msp/datasources
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
      "dataSourceId": "<数据源ID>",
      "nodeId": "<节点ID>",
      "name": "MySQL测试数据源",
      "type": "MYSQL",
      "host": "192.168.1.100",
      "port": 3306,
      "database": "test_db",
      "tableName": "user_data",
      "columns": ["id", "name", "email"],
      "createTime": 1747891200000
    }
  ]
}
```

**执行结果**: ✅ 通过

---

## TC-DS-003: 查询数据源详情

**测试目标**: 验证数据源详情查询

**请求**
```bash
GET /api/v1/msp/datasources/<dataSourceId>
Authorization: Bearer <token>
```

**预期响应**
```json
{
  "success": true,
  "code": "200",
  "message": "success",
  "data": {
    "dataSourceId": "<数据源ID>",
    "nodeId": "<节点ID>",
    "name": "MySQL测试数据源",
    "type": "MYSQL",
    "host": "192.168.1.100",
    "port": 3306,
    "database": "test_db",
    "tableName": "user_data",
    "columns": ["id", "name", "email"],
    "createTime": 1747891200000,
    "updateTime": 1747891200000
  }
}
```

**执行结果**: ✅ 通过

---

## TC-DS-004: 更新数据源

**测试目标**: 验证数据源更新

**请求**
```bash
PUT /api/v1/msp/datasources/<dataSourceId>
Content-Type: application/json
Authorization: Bearer <token>

{
  "name": "MySQL测试数据源(已更新)",
  "host": "192.168.1.101",
  "port": 3307
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

## TC-DS-005: 删除数据源

**测试目标**: 验证数据源删除

**请求**
```bash
DELETE /api/v1/msp/datasources/<dataSourceId>
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
| TC-DS-001 | 创建数据源 | P0 | ✅ 通过 |
| TC-DS-002 | 查询数据源列表 | P0 | ✅ 通过 |
| TC-DS-003 | 查询数据源详情 | P0 | ✅ 通过 |
| TC-DS-004 | 更新数据源 | P0 | ✅ 通过 |
| TC-DS-005 | 删除数据源 | P0 | ✅ 通过 |
