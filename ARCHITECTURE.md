# 密算平台 (MSP) - 技术架构文档

## 1. 项目概述

### 1.1 项目背景

密算平台是一个开源的隐私计算平台，旨在为可信数据空间提供隐私计算能力，解决"数据可用不可见、数据不出域"的核心问题。

### 1.2 核心目标

- **数据可用不可见**：在保护数据隐私的前提下实现多方数据协同计算
- **数据不出域**：数据始终保持在数据提供方本地，不流向第三方
- **跨机构协作**：支持2-5个参与方联合完成隐私计算任务

### 1.3 技术选型

| 组件 | 技术选型 | 说明 |
|------|---------|------|
| 后端 | Java (Spring Boot) | 调度中心、API服务 |
| 前端 | Vue 3 + Element Plus | Web控制台 |
| 计算内核 | SecretFlow (Python) | SPU/MPC/HEU设备 |
| 编排框架 | Kuscia (K8s) | 任务编排、节点管理 |
| 数据库 | MySQL / 达梦 | 元数据存储 |
| 通信协议 | gRPC + TLS | 节点间通信 |

---

## 2. 系统架构

### 2.1 整体架构图

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                              密算平台架构                                     │
├─────────────────────────────────────────────────────────────────────────────┤
│  接入层    │  Web Console(Vue)  │  OpenAPI Gateway  │  SDK(Java/Python)     │
├───────────┼─────────────────────────────────────────────────────────────────┤
│  调度层    │           Java调度中心 (Spring Boot)                           │
│           │  ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌──────────────────┐    │
│           │  │ 任务调度 │ │ 节点管理 │ │ 资源管理 │ │ 安全策略引擎    │    │
│           │  └──────────┘ └──────────┘ └──────────┘ └──────────────────┘    │
├───────────┼─────────────────────────────────────────────────────────────────┤
│  编排层    │                    Kuscia (K8s Task Orchestrator)              │
│           │  ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌──────────────────┐    │
│           │  │ Kuscia   │ │ 中控服务 │ │ Registry │ │ SecretFlow       │    │
│           │  │ Agent    │ │          │ │          │ │ (计算内核)       │    │
│           │  └──────────┘ └──────────┘ └──────────┘ └──────────────────┘    │
├───────────┼─────────────────────────────────────────────────────────────────┤
│  计算层    │   SecretFlow 设备抽象                                            │
│           │  ┌────┐ ┌────┐ ┌────┐ ┌────┐ ┌────┐ ┌────────────────────┐     │
│           │  │ PYU│ │ SPU│ │ HEU│ │TEEU│ │Link│ │ DataRunner         │     │
│           │  └────┘ └────┘ └────┘ └────┘ └────┘ └────────────────────┘     │
├───────────┼─────────────────────────────────────────────────────────────────┤
│  存储层    │         MySQL (元数据) │ PostgreSQL │ 对象存储 │ Kafka          │
└───────────┴─────────────────────────────────────────────────────────────────┘
```

### 2.2 模块层次

| 层次 | 模块 | 职责 |
|------|------|------|
| **接入层** | msp-gateway | API网关、鉴权、路由 |
| | msp-console | Web控制台 (Vue) |
| | msp-sdk | Java/Python SDK |
| **调度层** | msp-scheduler | 任务生命周期管理 |
| | msp-node-manager | 节点注册、心跳、状态 |
| | msp-resource-manager | 计算资源分配 |
| | msp-strategy-engine | 安全策略评估 |
| **编排层** | kuscia-master | K8s任务编排主节点 |
| | kuscia-agent | 计算节点代理 |
| **计算层** | PYU | Python计算单元 |
| | SPU | 安全处理单元 (MPC) |
| | HEU | 同态加密单元 |
| | TEEU | 可信执行环境单元 |
| **存储层** | MySQL/达梦 | 元数据存储 |
| | 对象存储 | 结果文件存储 |

---

## 3. 核心模块设计

### 3.1 Java调度中心 (msp-scheduler)

**路径**: `msp-backend/msp-scheduler/`

```
msp-scheduler/
├── src/main/java/com/msp/scheduler/
│   ├── controller/
│   │   └── TaskController.java          # 任务REST接口
│   ├── service/
│   │   ├── TaskScheduler.java           # 任务调度核心
│   │   ├── TaskStateMachine.java        # 任务状态机
│   │   └── KusciaClient.java            # Kuscia API客户端
│   ├── domain/
│   │   ├── Task.java                    # 任务实体
│   │   ├── TaskStep.java                # 任务步骤
│   │   └── ExecutionContext.java        # 执行上下文
│   └── config/
│       └── SchedulerConfig.java         # 调度配置
└── resources/
    └── application.yml
```

**核心接口**:

```java
public interface TaskScheduler {
    // 提交任务
    String submitTask(TaskRequest request);

    // 查询任务状态
    TaskStatus queryStatus(String taskId);

    // 取消任务
    boolean cancelTask(String taskId);
}
```

### 3.2 节点管理服务 (msp-node-manager)

**路径**: `msp-backend/msp-node-manager/`

```
msp-node-manager/
├── src/main/java/com/msp/node/
│   ├── controller/
│   │   └── NodeController.java          # 节点REST接口
│   ├── service/
│   │   ├── NodeRegistry.java            # 节点注册
│   │   ├── HeartbeatMonitor.java        # 心跳监控
│   │   └── NodeStatusManager.java      # 节点状态管理
│   ├── domain/
│   │   ├── Node.java                    # 节点实体
│   │   ├── NodeCapability.java          # 节点能力
│   │   └── NodeRoute.java               # 节点路由
│   └── adapter/
│       └── KusciaNodeAdapter.java       # Kuscia节点适配
└── resources/
    └── application.yml
```

### 3.3 Kuscia集成层 (msp-kuscia)

**路径**: `msp-backend/msp-kuscia/`

```
msp-kuscia/
├── src/main/java/com/msp/kuscia/
│   ├── client/
│   │   ├── KusciaApiClient.java         # Kuscia API客户端
│   │   ├── JobClient.java               # 任务客户端
│   │   └── DomainClient.java            # 域客户端
│   ├── config/
│   │   └── KusciaConfig.java            # Kuscia配置
│   └── proto/
│       └── kuscia.proto                 # Kuscia协议定义
└── resources/
    └── kuscia.yaml
```

### 3.4 Python计算节点 (msp-node)

**路径**: `msp-node/`

```
msp-node/
├── src/
│   ├── msp_node/
│   │   ├── runners/
│   │   │   ├── __init__.py
│   │   │   ├── base_runner.py           # 基础运行器
│   │   │   ├── psi_runner.py            # PSI运行器
│   │   │   ├── fl_runner.py             # 联邦学习运行器
│   │   │   └── mpc_runner.py            # MPC运行器
│   │   ├── adapters/
│   │   │   └── secretflow_adapter.py    # SecretFlow适配器
│   │   ├── transport/
│   │   │   └── grpc_server.py           # gRPC服务
│   │   └── entry.py                     # 入口点
│   └── requirements.txt
└── Dockerfile
```

---

## 4. 数据模型

### 4.1 核心实体

```java
// 任务实体
public class Task {
    String taskId;
    TaskType type;                    // PSI, FEDERATED_LEARNING, MPC
    TaskStatus status;                // CREATED, PENDING, RUNNING, COMPLETED, FAILED
    List<String> participants;
    AlgorithmSpec algorithm;
    Map<String, DataSource> inputs;
    Map<String, String> parameters;
    Long createTime;
    Long updateTime;
}

// 节点实体
public class Node {
    String nodeId;
    String nodeName;
    NodeStatus status;                // ONLINE, OFFLINE, BUSY
    NodeCapability capability;        // 支持的设备类型
    String endpoint;                  // gRPC地址
    List<String> tags;
}

// 数据源实体
public class DataSource {
    String dataSourceId;
    DataSourceType type;              // MYSQL, POSTGRESQL, API, FILE
    String host;
    Integer port;
    String database;
    String tableName;
    List<String> columns;
}
```

### 4.2 任务状态机

```
CREATED → PENDING → RUNNING → COMPLETED
                   ↓
                FAILED
                   ↓
              CANCELLED
```

---

## 5. API 设计

### 5.1 API 路径规范

```
/api/v1/msp
├── /tasks              # 任务管理
├── /nodes              # 节点管理
├── /data               # 数据管理
├── /algorithms         # 算法管理
├── /results            # 结果管理
└── /auth               # 认证授权
```

### 5.2 核心接口

#### 任务接口

```yaml
# 创建任务
POST /api/v1/msp/tasks
Request:
{
  "name": "PSI匹配任务",
  "type": "PSI",
  "participants": ["node-A", "node-B"],
  "inputs": [
    {
      "nodeId": "node-A",
      "dataSourceId": "ds-001",
      "column": "phone"
    }
  ],
  "parameters": {
    "psi_type": "ecdh"
  }
}

# 查询任务状态
GET /api/v1/msp/tasks/{taskId}

# 取消任务
DELETE /api/v1/msp/tasks/{taskId}
```

#### 节点接口

```yaml
# 注册节点
POST /api/v1/msp/nodes/register
{
  "nodeId": "node-A",
  "nodeName": "数据中心A",
  "capabilities": ["PYU", "SPU"],
  "endpoint": "grpc://192.168.1.100:8080"
}

# 查询节点列表
GET /api/v1/msp/nodes?status=ONLINE

# 心跳上报
POST /api/v1/msp/nodes/{nodeId}/heartbeat
```

---

## 6. 部署架构

### 6.1 Docker本地部署

```
msp-docker/
├── docker-compose.yml
├── postgres/
│   └── init.sql
├── kuscia/
│   └── master.yaml
├── scheduler/
│   └── Dockerfile
└── node/
    └── Dockerfile
```

### 6.2 K8s云端部署

```
msp-k8s/
├── namespace.yaml
├── configmap.yaml
├── secret.yaml
├── postgres.yaml
├── kuscia/
│   ├── kuscia-master.yaml
│   └── kuscia-agent.yaml
├── scheduler/
│   └── deployment.yaml
└── frontend/
    └── web.yaml
```

### 6.3 节点部署模式

| 节点类型 | 部署方式 | 资源需求 |
|---------|---------|---------|
| 轻量节点 (Lite) | Docker | 2核4G |
| 标准节点 (Agent) | Docker/K8s | 4核8G |
| 全量节点 (Full) | K8s | 8核16G+GPU |

---

## 7. 安全设计

### 7.1 安全措施

| 安全措施 | 实现方式 |
|---------|---------|
| 节点身份 | mTLS + 证书双向认证 |
| 数据传输 | TLS 1.3 端到端加密 |
| 数据存储 | 敏感数据加密存储 |
| 任务隔离 | Cgroup/K8s 资源隔离 |
| 审计日志 | 不可篡改的操作记录 |
| 访问控制 | RBAC 角色权限模型 |

### 7.2 国产化支持

- 密码算法：国密SM2/SM3/SM4
- 操作系统：麒麟、统信
- CPU：鲲鹏、飞腾、海光
- 数据库：达梦、GaussDB

---

## 8. 项目结构

```
msp/
├── msp-backend/                 # Java后端
│   ├── pom.xml
│   ├── msp-gateway/              # API网关
│   ├── msp-scheduler/            # 任务调度服务
│   ├── msp-node-manager/         # 节点管理服务
│   ├── msp-kuscia/               # Kuscia集成
│   ├── msp-common/               # 公共模块
│   └── msp-starter/              # 启动器
│
├── msp-node/                     # Python计算节点
│   ├── src/
│   │   └── msp_node/
│   │       ├── runners/          # 运行器
│   │       ├── adapters/         # 适配器
│   │       └── transport/        # 通信
│   ├── requirements.txt
│   └── Dockerfile
│
├── msp-frontend/                  # Vue前端
│   ├── src/
│   │   ├── views/               # 页面
│   │   ├── api/                 # API调用
│   │   └── components/          # 组件
│   └── package.json
│
├── msp-deploy/                   # 部署配置
│   ├── docker/
│   ├── kubernetes/
│   └── scripts/
│
└── docs/                         # 文档
    ├── architecture/
    └── api/
```

---

## 9. 开发阶段规划

### 阶段一：基础设施（1-2周）

- [ ] 搭建Java后端项目骨架
- [ ] 搭建Vue前端项目骨架
- [ ] 集成MySQL数据库
- [ ] 部署Kuscia体验版
- [ ] 验证SPU PSI功能
- [ ] 制定API规范

### 阶段二：核心模块（3-5周）

- [ ] 调度服务开发
- [ ] 节点服务开发
- [ ] 数据服务开发
- [ ] PSI算法集成
- [ ] 前端页面开发

### 阶段三：系统集成（6-7周）

- [ ] API网关开发
- [ ] 权限系统开发
- [ ] 审计日志开发
- [ ] 端到端测试

### 阶段四：生产准备（8-10周）

- [ ] K8s部署适配
- [ ] 高可用设计
- [ ] 性能优化
- [ ] 安全加固

---

## 10. 关键设计决策

| 决策点 | 选择 | 理由 |
|--------|------|------|
| 调度模式 | 中心化Java调度 | 复用Java生态，与Kuscia解耦 |
| 节点通信 | gRPC | 高性能、双向流、支持多语言 |
| 任务描述 | JSON over REST | 前端友好，调试方便 |
| 容器编排 | Kuscia (K8s) | 降低SecretFlow部署复杂度 |
| 计算内核 | SecretFlow SPU | 成熟开源，国产化兼容 |

---

## 11. 参考资料

- [SecretFlow GitHub](https://github.com/secretflow/secretflow)
- [Kuscia GitHub](https://github.com/secretflow/kuscia)
- [SecretFlow官方文档](https://www.secretflow.org.cn/)