# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## 项目概述

密算平台(MSP)是一个隐私计算平台，为可信数据空间提供隐私计算能力，支持MPC、联邦学习、PSI三种计算类型。

## 常用命令

### 后端 (Java/Spring Boot)
```bash
cd msp-backend
mvn clean install              # 构建所有模块
mvn spring-boot:run            # 启动调度服务
mvn -pl msp-scheduler spring-boot:run  # 启动指定模块
```

### 前端 (Vue 3)
```bash
cd msp-frontend
npm install                    # 安装依赖
npm run dev                    # 开发模式 (localhost:3000, 代理到 localhost:8090)
npm run build                  # 生产构建
```

### Python计算节点
```bash
cd msp-node
pip install -r requirements.txt
python -m msp_node             # 启动节点
```

### Docker部署
```bash
cd msp-deploy/docker
docker-compose up -d          # 启动所有服务
docker-compose down            # 停止
```

## 技术栈

| 层级 | 技术 | 说明 |
|------|------|------|
| 后端 | Java 17, Spring Boot 3.2, Spring Cloud | 调度中心、API网关 |
| 前端 | Vue 3, Element Plus, TypeScript, Vite | Web控制台 |
| 计算 | SecretFlow (SPU/MPC) | 计算内核 |
| 编排 | Kuscia (K8s) | 任务编排、节点管理 |
| 数据库 | MySQL | 元数据存储 |
| 通信 | gRPC + TLS | 节点间通信 |

## 架构要点

### 模块划分
- **msp-gateway**: API网关，路由到下游服务
- **msp-scheduler**: 任务调度核心，管理任务生命周期
- **msp-node-manager**: 节点注册、心跳、状态管理
- **msp-kuscia**: Kuscia API客户端封装
- **msp-common**: 公共实体和工具类 (ApiResponse, ErrorCode, Task, Node等)
- **msp-starter**: 启动器模块

### Python节点结构 (msp-node)
- `runners/base_runner.py`: 运行器基类，定义了 initialize()、run()、_do_run()、health_check() 接口
- `runners/psi_runner.py`, `fl_runner.py`, `mpc_runner.py`: 各类任务运行器
- `adapters/secretflow_adapter.py`: SecretFlow适配器
- `transport/grpc_server.py`: gRPC通信服务

### 任务类型
- `TaskType`: PSI, FEDERATED_LEARNING, MPC
- `TaskStatus`: CREATED → PENDING → RUNNING → COMPLETED/FAILED
- `NodeStatus`: ONLINE, OFFLINE, BUSY

### 关键配置
- 前端 dev server 代理: `/api` → `http://localhost:8090` (vite.config.ts)
- Docker 服务间通信通过 docker-compose 网络
- Kuscia Master 连接 MySQL 获取配置

## 核心文件

- `ARCHITECTURE.md`: 完整架构文档
- `msp-backend/msp-scheduler/`: 任务调度核心
- `msp-backend/msp-node-manager/`: 节点管理
- `msp-node/src/msp_node/runners/`: Python运行器实现

## 镜像构建

```bash
cd msp-deploy/msp
docker-compose build --parallel # 并行构建所有镜像
docker-compose up -d                   # 启动所有容器
docker-compose up -d <service>         # 重启指定服务
docker-compose logs -f <service>       # 查看日志
```

## 调试经验

### 任务执行失败排查流程

**原则：追踪到第一个真正的错误，而不是被"跳过"等次要信息带偏**

1. **查看所有 ERROR 级别日志**
   ```bash
   docker-compose logs --tail=500 <service> 2>&1 | grep -i "error"
   ```

2. **检查完整执行链**
   - 组件状态矛盾时（如 read_table成功 + psi_tp跳过）→ 说明下游组件自己失败，不是上游问题
   - 不要只看 `INFO` 日志，要追踪 `ERROR` 日志

3. **多节点问题定位**
   ```bash
   docker logs node-a 2>&1 | grep -iE "(execute|error|fail)" | tail -50
   docker logs node-b 2>&1 | grep -iE "(execute|error|fail)" | tail -50
   docker logs node-c 2>&1 | grep -iE "(execute|error|fail)" | tail -50
   ```

4. **常见失败原因**
   - Python模块导入错误：`No module named 'xxx'` → 检查 requirements.txt 和 import 语句
   - DAG字段名不一致：`attrs` vs `config` → 前端用 `config`，后端可能只读 `attrs`
   - 数据源owner映射错误：`node-a` vs `node-hospital` → 保持节点ID命名一致
   - 缺少加密库导入：`PrivateFormat not defined` → 检查 `from xxx import yyy` 语句

5. **验证修复**
   - 修复后先验证导入/依赖，再重启容器
   - 单元测试验证：`docker exec node-a python -c "from module import Class; print('OK')"`

## 镜像仓库

私有镜像仓库用于推送 MSP 相关镜像，便于在 K8s 环境部署。

| 配置项 | 值 |
|--------|-----|
| 仓库地址 | `r.mayishangshu.cn:82` |
| 命名空间 | `/msp` |
| 账号 | `dos` |
| 密码 | `Test@666` |

### 推送镜像脚本

```bash
REGISTRY=r.mayishangshu.cn:82/msp

# 构建并推送所有镜像
cd msp-deploy/msp
docker-compose -f docker-compose.yml build --parallel

for svc in scheduler gateway node-manager frontend node-a node-b node-c kuscia; do
    docker tag docker-${svc}:latest ${REGISTRY}/${svc}:latest
    docker push ${REGISTRY}/${svc}:latest
done
```

### 登录镜像仓库

```bash
docker login r.mayishangshu.cn:82 -u dos -p Test@666
```

## docs文档



