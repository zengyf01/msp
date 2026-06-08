#!/bin/bash
# MSP部署脚本
# 支持多种部署模式: docker-bridge, docker-host, kubernetes

set -e

COMPOSE_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$COMPOSE_DIR"

# 默认模式
DEFAULT_MODE="bridge"

# 颜色输出
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

usage() {
    echo "MSP 部署脚本"
    echo ""
    echo "用法: $0<命令> [选项]"
    echo ""
    echo "命令:"
    echo "  start<模式>   启动服务 (模式: bridge|host|kubernetes)"
    echo "  stop           停止所有服务"
    echo "  restart <模式> 重启服务"
    echo "  logs [服务]    查看日志"
    echo "  status         查看服务状态"
    echo "  build          构建镜像"
    echo "  clean 清理容器和数据"
    echo "  help 显示帮助"
    echo ""
    echo "部署模式:"
    echo "  bridge     (默认) Docker桥接网络模式 - 使用Docker网络服务名通信"
    echo "  host                  主机网络模式 - 使用localhost通信，适用于本地开发"
    echo "  kubernetes             K8s部署模式 - 使用Kuscia进行任务编排"
    echo ""
    echo "示例:"
    echo "  $0 start # 启动桥接模式 (默认)"
    echo "  $0 start host         # 启动主机网络模式"
    echo "  $0 start kubernetes # 启动K8s模式"
    echo "  $0 stop # 停止服务"
    echo "  $0 logs node-a       # 查看node-a日志"
    echo ""
}

# 获取compose文件
get_compose_file() {
    local mode=$1
    case $mode in
        host)
            echo "docker-compose.host.yml"
            ;;
        kubernetes|k8s)
            echo "docker-compose.kubernetes.yml"
            ;;
        *)
            echo "docker-compose.yml"
            ;;
    esac
}

cmd_start() {
    local mode=${1:-$DEFAULT_MODE}
    local compose_file=$(get_compose_file "$mode")

    echo -e "${GREEN}==>启动 MSP (模式: $mode)${NC}"
    echo " 使用配置: $compose_file"

    if [ ! -f "$compose_file" ]; then
        echo -e "${RED}错误: 配置文件 $compose_file 不存在${NC}"
        exit 1
    fi

    # 构建镜像
    echo -e "${YELLOW}==> 构建镜像...${NC}"
    docker-compose -f "$compose_file" build --parallel

    # 启动服务
    echo -e "${YELLOW}==> 启动服务...${NC}"
    docker-compose -f "$compose_file" up -d

    echo -e "${GREEN}==> 启动完成!${NC}"
    echo ""
    echo "服务地址:"
    echo "  - 前端:     http://localhost:3000"
    echo "  - API网关:  http://localhost:8092"
    echo "  - 调度服务: http://localhost:18081"
    echo "  - 节点管理: http://localhost:8082"
    echo ""
    echo "查看日志: $0 logs"
    echo "停止服务: $0 stop"
}

cmd_stop() {
    echo -e "${YELLOW}==> 停止所有 MSP 服务...${NC}"

    # 尝试所有可能的compose文件
    for compose_file in docker-compose.yml docker-compose.host.yml docker-compose.kubernetes.yml; do
        if [ -f "$compose_file" ]; then
            docker-compose -f "$compose_file" down2>/dev/null || true
        fi
    done

    echo -e "${GREEN}==>停止完成${NC}"
}

cmd_restart() {
    local mode=$1
    if [ -z "$mode" ]; then
        echo -e "${RED}错误: 请指定模式${NC}"
        echo "用法: $0 restart <模式>"
        exit 1
    fi
    cmd_stop
    cmd_start "$mode"
}

cmd_logs() {
    local service=${1:-}
    local compose_file=${2:-docker-compose.yml}

    if [ -n "$service" ]; then
        docker-compose -f "$compose_file" logs -f "$service"
    else
        docker-compose -f "$compose_file" logs -f
    fi
}

cmd_status() {
    echo -e "${GREEN}==> MSP 服务状态${NC}"
    echo ""

    # 尝试所有compose文件
    for compose_file in docker-compose.yml docker-compose.host.yml docker-compose.kubernetes.yml; do
        if [ -f "$compose_file" ]; then
            echo "配置: $compose_file"
            docker-compose -f "$compose_file" ps
            return
        fi
    done

    echo "未找到任何 compose 文件"
}

cmd_build() {
    local mode=${1:-$DEFAULT_MODE}
    local compose_file=$(get_compose_file "$mode")

    echo -e "${YELLOW}==> 构建镜像 ($mode)...${NC}"
    docker-compose -f "$compose_file" build --parallel
    echo -e "${GREEN}==> 构建完成${NC}"
}

cmd_clean() {
    echo -e "${YELLOW}==> 清理 MSP容器和数据...${NC}"

    for compose_file in docker-compose.yml docker-compose.host.yml docker-compose.kubernetes.yml; do
        if [ -f "$compose_file" ]; then
            docker-compose -f "$compose_file" down -v2>/dev/null || true
        fi
    done

    # 清理未使用的镜像
    docker image prune -f

    echo -e "${GREEN}==> 清理完成${NC}"
}

# 主命令处理
case "${1:-help}" in
    start)
        cmd_start "${2:-}"
        ;;
    stop)
        cmd_stop
        ;;
    restart)
        cmd_restart "${2:-}"
        ;;
    logs)
        cmd_logs "${2:-}" "${3:-}"
        ;;
    status)
        cmd_status
        ;;
    build)
        cmd_build "${2:-}"
        ;;
    clean)
        cmd_clean
        ;;
    help|--help|-h)
        usage
        ;;
    *)
        echo -e "${RED}错误: 未知命令 '$1'${NC}"
        usage
        exit 1
        ;;
esac