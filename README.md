-- 密算平台

# 隐私计算平台 - 最小化变更架构

## 项目概述

密算平台(MSP)是一个开源的隐私计算平台，为可信数据空间提供隐私计算能力。

## 核心功能

- **MPC (多方安全计算)**: 安全多方计算协议
- **联邦学习**: 跨机构协同机器学习
- **PSI (差分求交)**: 隐私集合求交

## 技术架构

```
接入层 → 调度层 → 编排层 → 计算层 → 存储层
```

- **Java调度中心**: Spring Boot + Kuscia
- **Python计算节点**: SecretFlow SPU/SPU
- **前端**: Vue 3 + Element Plus

## 快速开始

### Docker部署

```bash
cd msp-deploy/docker
docker-compose up -d
```

### 项目结构

```
msp/
├── msp-backend/         # Java后端
├── msp-node/            # Python计算节点
├── msp-frontend/         # Vue前端
├── msp-deploy/          # 部署配置
└── docs/                 # 文档
```

## 参考资料

- [SecretFlow](https://github.com/secretflow/secretflow)
- [Kuscia](https://github.com/secretflow/kuscia)