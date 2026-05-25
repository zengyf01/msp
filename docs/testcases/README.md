# MSP 测试用例

密算平台 (MSP) 测试用例索引

## 基础信息

- **测试环境**: http://localhost:8090 (Gateway)
- **默认用户**: admin / admin123
- **API前缀**: /api/v1/msp
- **认证方式**: `Authorization: Bearer <token>` (除登录接口外)

## 模块索引

| 模块 | 文件 | 用例数 | 通过 | 待测试 |
|------|------|--------|------|--------|
| 用户认证 | [auth.md](./auth.md) | 6 | 6 | 0 |
| 任务管理 | [task.md](./task.md) | 6 | 6 | 0 |
| 节点管理 | [node.md](./node.md) | 5 | 5 | 0 |
| 数据源管理 | [datasource.md](./datasource.md) | 5 | 5 | 0 |
| 审计日志 | [audit.md](./audit.md) | 2 | 2 | 0 |
| MPC运算 | [mpc.md](./mpc.md) | 3 | 3 | 0 |
| PSI协议 | [psi.md](./psi.md) | 4 | 4 | 0 |
| 联邦学习 | [fl.md](./fl.md) | 2 | 0 | 2 |
| 系统管理 | [system.md](./system.md) | 9 | 6 | 3 |
| **合计** | | **42** | **37** | **5** |

## 错误码

| 错误码 | 说明 |
|--------|------|
| AUTH_FAILED | 认证失败（用户名/密码错误） |
| USER_DISABLED | 用户已被禁用 |
| USER_NOT_FOUND | 用户不存在 |
| TASK_NOT_FOUND | 任务不存在 |
| NODE_NOT_FOUND | 节点不存在 |
| DATA_SOURCE_NOT_FOUND | 数据源不存在 |
| UNAUTHORIZED | 未授权（缺少token） |
| FORBIDDEN | 禁止访问（权限不足） |
| INTERNAL_ERROR | 内部服务器错误 |

## 测试注意事项

1. **Token传递**: 除登录接口外，所有请求都需要通过 `Authorization: Bearer <token>` header 传递token
2. **时间戳**: 所有时间字段均为毫秒级Unix时间戳
3. **分页**: 列表接口默认使用分页，页面从0开始计数
4. **数据依赖**: 删除节点前需确保没有关联的数据源
5. **并发安全**: 不建议在同一数据上执行并发更新操作
