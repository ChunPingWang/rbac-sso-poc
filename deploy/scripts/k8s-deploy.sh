#!/bin/bash
# ==============================================================================
# k8s-deploy.sh - Kubernetes 部署腳本
#
# 說明：在 Kind 環境中部署所有服務
# 使用方式：./deploy/scripts/k8s-deploy.sh [--build] [--delete]
# ==============================================================================

set -e

# 顏色定義
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

# 專案根目錄
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"
K8S_DIR="$PROJECT_ROOT/deploy/k8s"

# 參數
BUILD_IMAGES=false
DELETE_CLUSTER=false
CLUSTER_NAME="rbac-sso"

# ==============================================================================
# 函數定義
# ==============================================================================

print_banner() {
    echo -e "${BLUE}"
    echo "╔═══════════════════════════════════════════════════════════════╗"
    echo "║         RBAC-SSO-POC Kubernetes Deployment                    ║"
    echo "╚═══════════════════════════════════════════════════════════════╝"
    echo -e "${NC}"
}

log_info() {
    echo -e "${GREEN}[INFO]${NC} $1"
}

log_warn() {
    echo -e "${YELLOW}[WARN]${NC} $1"
}

log_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

log_step() {
    echo -e "${BLUE}[STEP]${NC} $1"
}

# 檢查必要工具
check_prerequisites() {
    log_step "檢查必要工具..."

    if ! command -v kind &> /dev/null; then
        log_error "kind 未安裝，請先安裝 kind"
        exit 1
    fi

    if ! command -v kubectl &> /dev/null; then
        log_error "kubectl 未安裝，請先安裝 kubectl"
        exit 1
    fi

    if ! command -v docker &> /dev/null; then
        log_error "docker 未安裝，請先安裝 docker"
        exit 1
    fi

    log_info "所有必要工具已安裝"
}

# 建立 Kind cluster
create_cluster() {
    log_step "建立 Kind cluster..."

    if kind get clusters | grep -q "^${CLUSTER_NAME}$"; then
        log_warn "Cluster ${CLUSTER_NAME} 已存在"
        return 0
    fi

    kind create cluster --config "$K8S_DIR/kind-config.yaml" --name "$CLUSTER_NAME"
    log_info "Kind cluster 建立完成"
}

# 刪除 Kind cluster
delete_cluster() {
    log_step "刪除 Kind cluster..."

    if kind get clusters | grep -q "^${CLUSTER_NAME}$"; then
        kind delete cluster --name "$CLUSTER_NAME"
        log_info "Kind cluster 已刪除"
    else
        log_warn "Cluster ${CLUSTER_NAME} 不存在"
    fi
}

# 建立 Docker images
build_images() {
    log_step "建立 Docker images..."

    cd "$PROJECT_ROOT"

    # 建立 JAR 檔案
    log_info "編譯專案..."
    ./gradlew clean build -x test

    # 建立 Docker images
    log_info "建立 Gateway image..."
    docker build -t rbac-gateway:latest -f deploy/docker/Dockerfile.gateway .

    log_info "建立 Product Service image..."
    docker build -t rbac-product-service:latest -f deploy/docker/Dockerfile.product-service .

    log_info "建立 User Service image..."
    docker build -t rbac-user-service:latest -f deploy/docker/Dockerfile.user-service .

    log_info "Docker images 建立完成"
}

# 載入 images 到 Kind cluster
load_images() {
    log_step "載入 images 到 Kind cluster..."

    kind load docker-image rbac-gateway:latest --name "$CLUSTER_NAME"
    kind load docker-image rbac-product-service:latest --name "$CLUSTER_NAME"
    kind load docker-image rbac-user-service:latest --name "$CLUSTER_NAME"

    log_info "Images 載入完成"
}

# 部署到 Kubernetes
deploy_to_k8s() {
    log_step "部署到 Kubernetes..."

    # 設置 kubectl context
    kubectl config use-context "kind-${CLUSTER_NAME}"

    # 部署 base 配置
    log_info "部署 Namespace 和 ConfigMap..."
    kubectl apply -f "$K8S_DIR/base/namespace.yaml"
    kubectl apply -f "$K8S_DIR/base/configmap.yaml"
    kubectl apply -f "$K8S_DIR/base/secrets.yaml"

    # 部署基礎設施
    log_info "部署 OpenLDAP..."
    kubectl apply -f "$K8S_DIR/infrastructure/openldap.yaml"

    log_info "部署 PostgreSQL..."
    kubectl apply -f "$K8S_DIR/infrastructure/postgres.yaml"

    # 等待 PostgreSQL 就緒
    log_info "等待 PostgreSQL 就緒..."
    kubectl wait --for=condition=ready pod -l app=postgres -n rbac-sso --timeout=120s || true

    log_info "部署 Keycloak..."
    kubectl apply -f "$K8S_DIR/infrastructure/keycloak.yaml"

    # 等待 Keycloak 就緒
    log_info "等待 Keycloak 就緒 (可能需要 2-3 分鐘)..."
    kubectl wait --for=condition=ready pod -l app=keycloak -n rbac-sso --timeout=300s || true

    # 部署應用服務
    log_info "部署 Gateway..."
    kubectl apply -f "$K8S_DIR/services/gateway.yaml"

    log_info "部署 Product Service..."
    kubectl apply -f "$K8S_DIR/services/product-service.yaml"

    log_info "部署 User Service..."
    kubectl apply -f "$K8S_DIR/services/user-service.yaml"

    # 部署 NodePort 服務
    log_info "部署 NodePort 服務..."
    kubectl apply -f "$K8S_DIR/services/nodeport-services.yaml"

    log_info "部署完成"
}

# 等待所有服務就緒
wait_for_services() {
    log_step "等待所有服務就緒..."

    local services=("openldap" "postgres" "keycloak" "gateway" "product-service" "user-service")

    for service in "${services[@]}"; do
        log_info "等待 $service..."
        kubectl wait --for=condition=ready pod -l app="$service" -n rbac-sso --timeout=180s || {
            log_warn "$service 尚未就緒，請稍後再試"
        }
    done

    log_info "服務狀態:"
    kubectl get pods -n rbac-sso
}

# 顯示服務狀態
show_status() {
    log_step "服務狀態"
    echo ""
    kubectl get all -n rbac-sso
    echo ""
    log_info "存取服務:"
    echo "  - Gateway:         http://localhost:8080"
    echo "  - Product Service: http://localhost:8081"
    echo "  - User Service:    http://localhost:8082"
    echo "  - Keycloak:        http://localhost:8180"
}

# 解析參數
parse_args() {
    while [[ $# -gt 0 ]]; do
        case $1 in
            --build)
                BUILD_IMAGES=true
                shift
                ;;
            --delete)
                DELETE_CLUSTER=true
                shift
                ;;
            *)
                log_error "未知參數: $1"
                echo "使用方式: $0 [--build] [--delete]"
                exit 1
                ;;
        esac
    done
}

# ==============================================================================
# 主程式
# ==============================================================================

main() {
    parse_args "$@"
    print_banner

    check_prerequisites

    if [ "$DELETE_CLUSTER" = true ]; then
        delete_cluster
        exit 0
    fi

    create_cluster

    if [ "$BUILD_IMAGES" = true ]; then
        build_images
    fi

    load_images
    deploy_to_k8s
    wait_for_services
    show_status

    echo ""
    log_info "部署完成！執行整合測試: ./deploy/scripts/k8s-integration-test.sh"
}

main "$@"
