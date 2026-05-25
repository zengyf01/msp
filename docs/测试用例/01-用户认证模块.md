# 用户认证模块

## TC-AUTH-001: 登录

**测试目标**: 验证用户登录功能

**请求**
```bash
POST /api/v1/msp/auth/login
Content-Type: application/json

{
  "username": "admin",
  "password": "admin123"
}
```

**预期响应**
```json
{
  "success": true,
  "code": "200",
  "message": "success",
  "data": {
    "token": "eyJhbGci...",
    "userId": "admin-default-id",
    "username": "admin"
  }
}
```

**执行结果**: ✅ 通过

---

## TC-AUTH-002: 登出

**测试目标**: 验证用户登出功能

**请求**
```bash
POST /api/v1/msp/auth/logout
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

## TC-AUTH-003: 获取当前用户

**测试目标**: 验证获取当前登录用户信息

**请求**
```bash
GET /api/v1/msp/auth/current
Authorization: Bearer <token>
```

**预期响应**
```json
{
  "success": true,
  "code": "200",
  "message": "success",
  "data": {
    "userId": "admin-default-id",
    "username": "admin",
    "role": "ADMIN",
    "enabled": true
  }
}
```

**执行结果**: ✅ 通过

---

## TC-AUTH-004: 创建用户

**测试目标**: 验证用户注册功能

**请求**
```bash
POST /api/v1/msp/auth/register
Content-Type: application/json
Authorization: Bearer <admin_token>

{
  "username": "testuser",
  "password": "test123",
  "role": "USER"
}
```

**预期响应**
```json
{
  "success": true,
  "code": "200",
  "message": "success",
  "data": "<新用户ID>"
}
```

**执行结果**: ✅ 通过

---

## TC-AUTH-005: 登录失败 - 用户不存在

**测试目标**: 验证用户不存在时的错误处理

**请求**
```bash
POST /api/v1/msp/auth/login
Content-Type: application/json

{
  "username": "nonexistent",
  "password": "any"
}
```

**预期响应**
```json
{
  "success": false,
  "code": "AUTH_FAILED",
  "message": "用户名或密码错误"
}
```

**执行结果**: ✅ 通过

---

## TC-AUTH-006: 登录失败 - 密码错误

**测试目标**: 验证密码错误时的错误处理

**请求**
```bash
POST /api/v1/msp/auth/login
Content-Type: application/json

{
  "username": "admin",
  "password": "wrongpassword"
}
```

**预期响应**
```json
{
  "success": false,
  "code": "AUTH_FAILED",
  "message": "用户名或密码错误"
}
```

**执行结果**: ✅ 通过

---

## 汇总

| 用例ID | 名称 | 优先级 | 执行结果 |
|--------|------|--------|----------|
| TC-AUTH-001 | 登录 | P0 | ✅ 通过 |
| TC-AUTH-002 | 登出 | P0 | ✅ 通过 |
| TC-AUTH-003 | 获取当前用户 | P0 | ✅ 通过 |
| TC-AUTH-004 | 创建用户 | P0 | ✅ 通过 |
| TC-AUTH-005 | 登录失败-用户不存在 | P0 | ✅ 通过 |
| TC-AUTH-006 | 登录失败-密码错误 | P0 | ✅ 通过 |
