# MSP 部署模式操作指南

## 部署模式概览

| 模式 | 配置文件 | 网络模式 | bRPC 支持 | 推荐场景 |
|------|----------|----------|-----------|----------|
| docker-bridge | docker-compose.yml | Docker 桥接 | :x: | macOS/Windows 开发 |
| docker-host | docker-compose.host.yml | Host (localhost) | :white_check_mark: | Linux 服务器开发/测试 |
| kubernetes | docker-compose.kubernetes.yml | 桥接 + Kuscia | :white_check_mark: | 生产环境/K8s 集群 |

## 快速开始

### 方式一：使用部署脚本（推荐）

```bash
cd /Users/zyf/Documents/project/msp/msp-deploy/msp

# 启动（默认桥接模式）
./deploy.sh start

# 启动主机网络模式（Linux）
./deploy.sh start host

# 启动 Kubernetes 模式
./deploy.sh start kubernetes

# 停止服务
./deploy.sh stop

# 查看日志
./deploy.sh logs node-a

# 查看状态
./deploy.sh status

# 清理
./deploy.sh clean
```

### 方式二：直接使用 docker-compose

```bash
cd /Users/zyf/Documents/project/msp/msp-deploy/msp

# 桥接模式（默认）
docker-compose up -d

# 主机网络模式
docker-compose -f docker-compose.host.yml up -d

# Kubernetes 模式
docker-compose -f docker-compose.kubernetes.yml up -d
```

---

## 模式一：Docker 桥接模式（默认）

**适用环境**：macOS / Windows Docker Desktop

### 特点
- 使用 Docker 桥接网络，服务间通过服务名通信
- 各节点 SPU 端口需映射到不同主机端口避免冲突
- **不支持 bRPC**：PSI 计算会回退到 Test PSI 模式

### 端口映射

| 服务 | 容器端口 | 映射端口 |
|------|----------|----------|
| MySQL (MSP) | 3306 | 13306 |
| Scheduler | 8081 | 18081 |
| Gateway | 8090 | 8092 |
| Node Manager | 8082 | 8082 |
| Frontend | 3000 | 3000 |
| Node A (gRPC) | 50051 | 50051 |
| Node A (SPU) | 8000 | 8000 |
| Node B (gRPC) | 50051 | 50053 |
| Node B (SPU) | 8000 | 8001 |
| Node C (gRPC) | 50051 | 50054 |
| Node C (SPU) | 8000 | 8002 |
| Node A DB | 3306 | 3307 |
| Node B DB | 3306 | 3308 |
| Node C DB | 3306 | 3310 |

### 服务地址
- 前端：http://localhost:3000
- API 网关：http://localhost:8092
- 调度服务：http://localhost:18081
- 节点管理：http://localhost:8082

---

## 模式二：Docker 主机网络模式

**适用环境**：Linux 服务器（**不支持 macOS/Windows**）

### 特点
- 使用 `network_mode: host`，容器直接使用主机网络
- SPU 通信直接通过 localhost，无需端口映射
- **完全支持 bRPC**：可执行真正的跨节点 PSI 计算

### 端口占用

| 服务 | 端口 |
|------|------|
| MySQL (MSP) | 3306 |
| Scheduler | 8081 |
| Gateway | 8090 |
| Node Manager | 8082 |
| Frontend | 3000 |
| Node A SPU | 8000 |
| Node B SPU | 8001 |
| Node C SPU | 8002 |
| Node A DB | 3307 |
| Node B DB | 3308 |
| Node C DB | 3310 |

### 重要提示
- 此模式使用 localhost 通信，与桥接模式不同
- 各节点数据库使用不同端口避免冲突
- **仅限 Linux 服务器使用**

---

## 模式三：Kubernetes 模式

**适用环境**：K8s 集群或 Docker Compose + Kuscia

### 特点
- 使用 Kuscia 进行任务编排
- 适用于生产环境
- **完全支持 bRPC**：通过 Kuscia/SFU 内部网络通信

### 服务器前置条件

#### 方式 A：Docker Compose 部署（本次使用）

仅需安装 Docker 和 Docker Compose：

```bash
# Docker >= 20.10
docker --version

# Docker Compose >= 2.0
docker-compose --version
```

#### 方式 B：真实 K8s 集群部署

需在 K8s 集群中部署以下组件：

| 组件 | 版本要求 | 说明 |
|------|----------|------|
| Kubernetes | >= 1.24 | K8s 控制面 |
| Docker/Containerd | 最新稳定版 | 容器运行时 |
| kubectl | >= 1.24 | K8s CLI 工具 |
| Helm | >= 3.0 | 包管理器（可选） |

### 核心组件
- **kuscia-master**：K8s 编排核心，管理节点和任务
- 各节点使用 `NODE_MODE=KUSCIA` 接入 Kuscia

### Kuscia Token 配置
各节点的 `KUSCIA_NODE_TOKEN` 需与 Kuscia Master 注册时的 token 一致。当前配置使用预设 token，**正式环境请使用安全的 token 生成方式**。

### 服务地址
- Kuscia Master API：http://localhost:8080
- Kuscia Master GRPC：localhost:8081
- Kuscia Master HTTP：localhost:8083

### 启动与停止

```bash
# 启动
docker-compose -f docker-compose.kubernetes.yml up -d

# 查看日志
docker-compose -f docker-compose.kubernetes.yml logs -f kuscia-master

# 停止
docker-compose -f docker-compose.kubernetes.yml down
```

---

## 常见问题

### Q: 为什么 PSI 计算结果不对？
检查是否使用了 Test PSI。查看日志关键词 `fallback to test PSI`。

**解决方案**：
- 使用 Linux 服务器部署 `docker-host` 模式
- 或使用 `kubernetes` 模式

### Q: node-b/node-c 无法连接 node-a 的 SPU？
检查 `docker-compose.yml` 中的 `SPU_NODE_*` 环境变量配置，确保端口与实际映射端口一致（node-b 用 8001，node-c 用 8002）。

### Q: macOS/Windows 上如何完整测试 PSI？
只能测试流程，无法真正执行跨节点 PSI。建议：
1. 使用 Test PSI 验证流程
2. 在 Linux 服务器上部署完整测试

---

## 环境变量

| 变量 | 默认值 | 说明 |
|------|--------|------|
| DB_PASSWORD | msp123456 | MSP 数据库密码 |
| DB_ROOT_PASSWORD | root123456 | MySQL root 密码 |
| DEPLOY_MODE | docker-bridge | 部署模式 |

---

## 清理环境

```bash
# 停止并删除容器
docker-compose -f docker-compose.yml down
docker-compose -f docker-compose.host.yml down
docker-compose -f docker-compose.kubernetes.yml down

# 删除数据卷（彻底清理）
docker-compose -f docker-compose.yml down -v
```