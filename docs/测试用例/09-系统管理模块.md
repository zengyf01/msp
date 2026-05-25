# 系统管理模块

## TC-SYS-001: 用户CRUD

**测试目标**: 验证用户创建、查看、更新、删除功能

**前置条件**: 已登录管理员账号

**测试步骤**:
1. 创建用户
```bash
POST /api/v1/msp/users
Content-Type: application/json
Authorization: Bearer <admin_token>

{
  "username": "testuser001",
  "password": "Test123456",
  "email": "test@example.com",
  "phone": "13800138000",
  "role": "USER"
}
```

2. 查询用户列表
```bash
GET /api/v1/msp/users
Authorization: Bearer <admin_token>
```

3. 查询单个用户详情
```bash
GET /api/v1/msp/users/<userId>
Authorization: Bearer <admin_token>
```

4. 更新用户信息
```bash
PUT /api/v1/msp/users/<userId>
Content-Type: application/json
Authorization: Bearer <admin_token>

{
  "email": "newemail@example.com",
  "phone": "13900139000"
}
```

5. 删除用户
```bash
DELETE /api/v1/msp/users/<userId>
Authorization: Bearer <admin_token>
```

**预期结果**:
- 用户创建成功，返回用户ID
- 用户列表包含新建用户
- 用户详情显示正确信息
- 用户更新成功，查询显示新信息
- 用户删除成功，列表中不再存在

**执行结果**: ✅ 通过

---

## TC-SYS-002: 用户启用禁用

**测试目标**: 验证用户启用/禁用功能，禁用后无法登录

**前置条件**: 存在已注册用户

**测试步骤**:
1. 禁用用户
```bash
POST /api/v1/msp/users/<userId>/disable
Authorization: Bearer <admin_token>
```

2. 尝试用禁用账号登录
```bash
POST /api/v1/msp/auth/login
Content-Type: application/json

{
  "username": "<disabled_username>",
  "password": "Test123456"
}
```

3. 重新启用用户
```bash
POST /api/v1/msp/users/<userId>/enable
Authorization: Bearer <admin_token>
```

4. 再次登录验证
```bash
POST /api/v1/msp/auth/login
Content-Type: application/json

{
  "username": "<enabled_username>",
  "password": "Test123456"
}
```

**预期结果**:
- 禁用操作返回成功
- 禁用用户登录返回 AUTH_FAILED 或 USER_DISABLED
- 启用操作返回成功
- 启用后用户可正常登录

**执行结果**: ✅ 通过

---

## TC-SYS-003: 密码重置

**测试目标**: 验证密码重置功能，重置后发送新密码

**前置条件**: 存在已注册用户

**测试步骤**:
1. 重置密码
```bash
POST /api/v1/msp/users/<userId>/reset-password
Authorization: Bearer <admin_token>
```

2. 使用新密码登录
```bash
POST /api/v1/msp/auth/login
Content-Type: application/json

{
  "username": "<username>",
  "password": "<new_password>"
}
```

**预期结果**:
- 密码重置请求成功
- 新密码可以正常登录

**执行结果**: ✅ 通过

---

## TC-SYS-004: 角色权限分配

**测试目标**: 验证为角色分配权限功能

**前置条件**: 已登录管理员账号，存在权限项

**测试步骤**:
1. 创建角色
```bash
POST /api/v1/msp/roles
Content-Type: application/json
Authorization: Bearer <admin_token>

{
  "roleCode": "DATA_ANALYST",
  "roleName": "数据分析师",
  "description": "负责数据分析的角色",
  "permissions": ["DATA_VIEW", "TASK_CREATE"]
}
```

2. 查询角色列表
```bash
GET /api/v1/msp/roles
Authorization: Bearer <admin_token>
```

3. 更新角色权限
```bash
PUT /api/v1/msp/roles/<roleId>
Content-Type: application/json
Authorization: Bearer <admin_token>

{
  "permissions": ["DATA_VIEW", "TASK_CREATE", "TASK_VIEW"]
}
```

4. 删除角色
```bash
DELETE /api/v1/msp/roles/<roleId>
Authorization: Bearer <admin_token>
```

**预期结果**:
- 角色创建成功
- 角色列表包含新建角色，权限正确
- 角色更新后权限正确更新
- 角色删除成功

**执行结果**: ⚠️ API正常，数据编码问题（init.sql）

---

## TC-SYS-005: 平台信息配置

**测试目标**: 验证平台信息配置修改功能

**前置条件**: 已登录管理员账号

**测试步骤**:
1. 获取当前平台信息
```bash
GET /api/v1/msp/system-config
Authorization: Bearer <admin_token>
```

2. 更新平台信息
```bash
PUT /api/v1/msp/system-config
Content-Type: application/json
Authorization: Bearer <admin_token>

{
  "platformName": "MSP生产环境",
  "platformDescription": "密算平台生产实例",
  "contactEmail": "support@example.com",
  "contactPhone": "400-123-4567"
}
```

3. 重新获取平台信息验证
```bash
GET /api/v1/msp/system-config
Authorization: Bearer <admin_token>
```

**预期结果**:
- 获取平台信息成功，显示当前配置
- 更新平台信息成功
- 再次获取显示新配置

**执行结果**: ✅ 代码已完成，需前端测试

---

## TC-SYS-006: 安全设置

**测试目标**: 验证JWT有效期、密码策略等安全设置

**前置条件**: 已登录管理员账号

**测试步骤**:
1. 获取安全设置
```bash
GET /api/v1/msp/system-config/security
Authorization: Bearer <admin_token>
```

2. 更新安全设置
```bash
PUT /api/v1/msp/system-config/security
Content-Type: application/json
Authorization: Bearer <admin_token>

{
  "jwtExpiration": 7200,
  "passwordMinLength": 8,
  "passwordRequireSpecialChar": true,
  "sessionTimeout": 3600
}
```

**预期结果**:
- 获取安全设置成功
- 更新安全设置成功，新设置生效

**执行结果**: ✅ 代码已完成，需前端测试

---

## TC-CODE-001: 自定义PSI

**测试目标**: 验证用户编写PSI代码执行功能

**前置条件**: 已启动模拟节点 alice 和 bob

**测试步骤**:
1. 创建自定义PSI任务
```bash
POST /api/v1/msp/tasks
Content-Type: application/json
Authorization: Bearer <token>

{
  "name": "自定义PSI任务",
  "type": "CUSTOM_CODE",
  "algorithm": "custom",
  "participants": ["alice", "bob"],
  "parameters": {
    "code": "def run():\n    from secretflow.preprocessing import PSI\n    psi = PSI(spu, key_column='user_id')\n    result = psi.run(data)\n    return {'status': 'ok', 'matched_count': len(result)}"
  },
  "description": "测试自定义PSI代码执行"
}
```

2. 查询任务状态
```bash
GET /api/v1/msp/tasks/<taskId>
Authorization: Bearer <token>
```

3. 获取任务结果
```bash
GET /api/v1/msp/tasks/<taskId>/result
Authorization: Bearer <token>
```

**预期结果**:
- 任务创建成功
- 任务执行完成后返回交集结果
- matched_count 正确

**执行结果**: ⏳ 待测试

---

## TC-CODE-002: 自定义FL

**测试目标**: 验证用户编写联邦学习代码执行功能

**前置条件**: 已启动模拟节点 alice 和 bob

**测试步骤**:
1. 创建自定义FL任务
```bash
POST /api/v1/msp/tasks
Content-Type: application/json
Authorization: Bearer <token>

{
  "name": "自定义联邦学习任务",
  "type": "CUSTOM_CODE",
  "algorithm": "custom",
  "participants": ["alice", "bob"],
  "parameters": {
    "code": "def run():\n    from secretflow.ml import FL\n    fl = FL(device=spu, model_type='linear_regression')\n    model = fl.train(data, epochs=10)\n    return {'status': 'ok', 'model_type': 'linear_regression'}"
  },
  "description": "测试自定义FL代码执行"
}
```

**预期结果**:
- 任务创建成功
- 模型训练成功
- 返回模型类型

**执行结果**: ⏳ 待测试

---

## 汇总

| 用例ID | 名称 | 优先级 | 执行结果 |
|--------|------|--------|----------|
| TC-SYS-001 | 用户CRUD | P0 | ✅ 通过 |
| TC-SYS-002 | 用户启用禁用 | P0 | ✅ 通过 |
| TC-SYS-003 | 密码重置 | P0 | ✅ 通过 |
| TC-SYS-004 | 角色权限分配 | P0 | ⚠️ API正常，数据编码问题 |
| TC-SYS-005 | 平台信息配置 | P0 | ✅ 代码完成，待前端测试 |
| TC-SYS-006 | 安全设置 | P0 | ✅ 代码完成，待前端测试 |
| TC-CODE-001 | 自定义PSI | P0 | ⏳ 待测试 |
| TC-CODE-002 | 自定义FL | P0 | ⏳ 待测试 |
