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
    每次只有有修改代码的任务完成后都需要构建镜像和启动容器
    每次构建镜像只需要构建有修改的镜像

## docs文档
    每个会话每次问答完成后都需要更新文档
    相同属性、作用的文档只能有一份
    将已经完成的迭代的测试用例，以模块分类加入到测试用例文档中



