#!/bin/bash
# MSP K8s 部署脚本
# 使用方式: ./deploy-k8s.sh <镜像仓库地址>
#
# 示例:
#   ./deploy-k8s.sh registry.cn-hangzhou.aliyuncs.com/your-namespace

set -e

REGISTRY=${1:-""}
NAMESPACE="msp-system"

if [ -z "$REGISTRY" ]; then
    echo "用法: $0 <镜像仓库地址>"
    echo "示例: $0 registry.cn-hangzhou.aliyuncs.com/your-namespace"
    exit 1
fi

echo "==> 使用镜像仓库: $REGISTRY"

# 创建命名空间
echo "==> 创建命名空间..."
kubectl create namespace $NAMESPACE --dry-run=client -o yaml | kubectl apply -f -

# 部署 MySQL
echo "==> 部署 MySQL..."
kubectl apply -f mysql.yaml
kubectl wait --for=condition=ready pod -l app=mysql -n $NAMESPACE --timeout=120s

# 更新镜像地址并部署后端
echo "==> 部署后端服务..."
sed "s|image: msp/|image: ${REGISTRY}/msp/|g" backend.yaml | kubectl apply -f -

# 部署前端
echo "==> 部署前端..."
sed "s|image: msp/|image: ${REGISTRY}/msp/|g" frontend.yaml | kubectl apply -f -

echo ""
echo "==> 部署完成!"
echo ""
kubectl get pods -n $NAMESPACE
echo ""
echo "服务地址: http://<任意节点IP>:30000"