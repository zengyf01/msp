# 帮助团队快速上手密算平台

## 本地开发环境设置

1. **前置要求**
   - JDK 17+
   - Node.js 18+
   - Docker & Docker Compose
   - Python 3.10+ (用于计算节点)

2. **后端启动**
   ```bash
   cd msp-backend
   mvn spring-boot:run
   ```

3. **前端启动**
   ```bash
   cd msp-frontend
   npm install
   npm run dev
   ```

4. **Docker快速部署**
   ```bash
   cd msp-deploy/docker
   docker-compose up -d
   ```

## 技术栈

- 后端: Java 17, Spring Boot 3.2, Spring Cloud
- 前端: Vue 3, Element Plus, TypeScript
- 计算: SecretFlow, SPU, Kuscia
- 数据库: PostgreSQL, SQLite

## 关键文件

- `ARCHITECTURE.md` - 完整架构文档
- `msp-backend/` - Java后端源码
- `msp-node/` - Python计算节点
- `msp-frontend/` - Vue前端源码