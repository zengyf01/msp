# Kubernetes部署清单

## 目录结构

```
kubernetes/
├── namespace.yaml    # 命名空间
├── mysql.yaml        # MySQL数据库
├── backend.yaml      # 后端服务 (scheduler, gateway, node-manager)
├── frontend.yaml     # 前端服务 + Ingress
└── README.md         # 部署说明
```

## 快速部署

```bash
# 1. 创建命名空间
kubectl apply -f namespace.yaml

# 2. 部署MySQL
kubectl apply -f mysql.yaml

# 3. 部署后端服务
kubectl apply -f backend.yaml

# 4. 部署前端
kubectl apply -f frontend.yaml

# 5. 检查状态
kubectl get pods -n msp-system
```

## 服务访问

- 前端: http://localhost:30000 (NodePort方式)
- 或通过Ingress: http://msp.example.com (需要配置hosts)

## 注意

- 实际部署前需要构建对应镜像并推送到镜像仓库
- 默认用户名: admin / admin123
- 生产环境请修改默认密码