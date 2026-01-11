#!/bin/bash
# ==============================================================================
# k8s-mtls-deploy.sh - Kubernetes mTLS 部署腳本
#
# 說明：部署啟用 mTLS 的 RBAC-SSO-POC 到 Kind 環境
# 使用方式：./deploy/scripts/k8s-mtls-deploy.sh [options]
#
# Options:
#   --install-cert-manager  安裝 cert-manager
#   --build                 重新建置 Docker 映像
#   --delete                刪除現有部署
#   --verify                驗證 mTLS 連線
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

# 配置
CLUSTER_NAME="rbac-sso"
NAMESPACE="rbac-sso"
CERT_MANAGER_VERSION="v1.14.0"

# ==============================================================================
# 函數定義
# ==============================================================================

print_banner() {
    echo -e "${BLUE}"
    echo "╔═══════════════════════════════════════════════════════════════╗"
    echo "║       RBAC-SSO-POC mTLS Deployment Script                     ║"
    echo "║       Spring Boot + cert-manager Implementation               ║"
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

# 檢查先決條件
check_prerequisites() {
    log_step "檢查先決條件..."

    local missing=()

    command -v kubectl &> /dev/null || missing+=("kubectl")
    command -v kind &> /dev/null || missing+=("kind")
    command -v docker &> /dev/null || missing+=("docker")

    if [ ${#missing[@]} -ne 0 ]; then
        log_error "缺少必要工具: ${missing[*]}"
        exit 1
    fi

    log_info "所有先決條件已滿足"
}

# 安裝 cert-manager
install_cert_manager() {
    log_step "安裝 cert-manager ${CERT_MANAGER_VERSION}..."

    # 檢查是否已安裝
    if kubectl get namespace cert-manager &> /dev/null; then
        log_warn "cert-manager 已安裝，跳過..."
        return 0
    fi

    # 安裝 cert-manager
    kubectl apply -f https://github.com/cert-manager/cert-manager/releases/download/${CERT_MANAGER_VERSION}/cert-manager.yaml

    # 等待 cert-manager 就緒
    log_info "等待 cert-manager 就緒..."
    kubectl wait --for=condition=Available --timeout=300s deployment/cert-manager -n cert-manager
    kubectl wait --for=condition=Available --timeout=300s deployment/cert-manager-webhook -n cert-manager
    kubectl wait --for=condition=Available --timeout=300s deployment/cert-manager-cainjector -n cert-manager

    # 等待 webhook 完全就緒
    sleep 10

    log_info "cert-manager 安裝完成"
}

# 建立 CA Issuer 和憑證
setup_certificates() {
    log_step "設定 CA Issuer 和服務憑證..."

    # 確保 namespace 存在
    kubectl apply -f "$PROJECT_ROOT/deploy/k8s/base/namespace.yaml"

    # 建立 CA Issuer
    kubectl apply -f "$PROJECT_ROOT/deploy/k8s/security/cert-manager/ca-issuer.yaml"

    # 等待 CA 憑證簽發
    log_info "等待 CA 憑證簽發..."
    sleep 5

    # 檢查 CA Secret 是否存在
    for i in {1..30}; do
        if kubectl get secret rbac-sso-ca-secret -n cert-manager &> /dev/null; then
            log_info "CA 憑證已簽發"
            break
        fi
        echo -n "."
        sleep 2
    done

    # 複製 CA Secret 到應用 namespace
    log_info "複製 CA Secret 到 ${NAMESPACE} namespace..."
    kubectl get secret rbac-sso-ca-secret -n cert-manager -o yaml | \
        sed "s/namespace: cert-manager/namespace: ${NAMESPACE}/" | \
        kubectl apply -f -

    # 建立服務憑證
    kubectl apply -f "$PROJECT_ROOT/deploy/k8s/security/cert-manager/service-certificates.yaml"

    # 等待服務憑證簽發
    log_info "等待服務憑證簽發..."
    for cert in gateway-tls product-service-tls user-service-tls keycloak-tls; do
        for i in {1..30}; do
            status=$(kubectl get certificate $cert -n $NAMESPACE -o jsonpath='{.status.conditions[?(@.type=="Ready")].status}' 2>/dev/null || echo "False")
            if [ "$status" = "True" ]; then
                log_info "憑證 $cert 已就緒"
                break
            fi
            echo -n "."
            sleep 2
        done
    done
}

# 建置 Docker 映像
build_images() {
    log_step "建置 Docker 映像..."

    cd "$PROJECT_ROOT"

    # 建置 Gradle 專案
    log_info "建置 Gradle 專案..."
    ./gradlew clean build -x test

    # 建置 Docker 映像
    log_info "建置 Gateway 映像..."
    docker build -t rbac-gateway:latest -f services/gateway-service/Dockerfile .

    log_info "建置 Product Service 映像..."
    docker build -t rbac-product-service:latest -f services/product-service/Dockerfile .

    log_info "建置 User Service 映像..."
    docker build -t rbac-user-service:latest -f services/user-service/Dockerfile .

    # 載入映像到 Kind
    log_info "載入映像到 Kind cluster..."
    kind load docker-image rbac-gateway:latest --name "$CLUSTER_NAME"
    kind load docker-image rbac-product-service:latest --name "$CLUSTER_NAME"
    kind load docker-image rbac-user-service:latest --name "$CLUSTER_NAME"
}

# 部署基礎設施
deploy_infrastructure() {
    log_step "部署基礎設施服務..."

    kubectl apply -f "$PROJECT_ROOT/deploy/k8s/base/configmap.yaml"
    kubectl apply -f "$PROJECT_ROOT/deploy/k8s/base/secrets.yaml"
    kubectl apply -f "$PROJECT_ROOT/deploy/k8s/infrastructure/openldap.yaml"
    kubectl apply -f "$PROJECT_ROOT/deploy/k8s/infrastructure/postgres.yaml"
    kubectl apply -f "$PROJECT_ROOT/deploy/k8s/infrastructure/keycloak.yaml"

    # 等待基礎設施就緒
    log_info "等待基礎設施服務就緒..."
    kubectl wait --for=condition=Ready pod -l app=openldap -n $NAMESPACE --timeout=300s || true
    kubectl wait --for=condition=Ready pod -l app=postgres -n $NAMESPACE --timeout=300s || true
    kubectl wait --for=condition=Ready pod -l app=keycloak -n $NAMESPACE --timeout=300s || true
}

# 部署 mTLS 應用服務
deploy_mtls_services() {
    log_step "部署 mTLS 應用服務..."

    kubectl apply -f "$PROJECT_ROOT/deploy/k8s/services-mtls/gateway-mtls.yaml"
    kubectl apply -f "$PROJECT_ROOT/deploy/k8s/services-mtls/product-service-mtls.yaml"
    kubectl apply -f "$PROJECT_ROOT/deploy/k8s/services-mtls/user-service-mtls.yaml"

    # 等待應用服務就緒
    log_info "等待應用服務就緒..."
    kubectl wait --for=condition=Ready pod -l app=gateway -n $NAMESPACE --timeout=300s || true
    kubectl wait --for=condition=Ready pod -l app=product-service -n $NAMESPACE --timeout=300s || true
    kubectl wait --for=condition=Ready pod -l app=user-service -n $NAMESPACE --timeout=300s || true
}

# 驗證 mTLS 連線
verify_mtls() {
    log_step "驗證 mTLS 連線..."

    # 在 gateway pod 中測試 mTLS 連線
    GATEWAY_POD=$(kubectl get pod -l app=gateway -n $NAMESPACE -o jsonpath='{.items[0].metadata.name}')

    if [ -z "$GATEWAY_POD" ]; then
        log_error "找不到 Gateway Pod"
        return 1
    fi

    log_info "從 Gateway Pod 測試 mTLS 連線到 Product Service..."

    # 使用 curl 測試 mTLS 連線
    kubectl exec -n $NAMESPACE "$GATEWAY_POD" -- \
        curl -v --cacert /etc/ssl/certs/ca.crt \
             --cert /etc/ssl/certs/tls.crt \
             --key /etc/ssl/certs/tls.key \
             https://product-service:8081/actuator/health 2>&1 | grep -E "(SSL|TLS|HTTP)" || true

    log_info "mTLS 驗證完成"
}

# 顯示部署狀態
show_status() {
    log_step "部署狀態..."

    echo ""
    echo "=== Pods ==="
    kubectl get pods -n $NAMESPACE

    echo ""
    echo "=== Services ==="
    kubectl get services -n $NAMESPACE

    echo ""
    echo "=== Certificates ==="
    kubectl get certificates -n $NAMESPACE

    echo ""
    echo "=== Secrets (TLS) ==="
    kubectl get secrets -n $NAMESPACE | grep tls
}

# 刪除部署
delete_deployment() {
    log_step "刪除 mTLS 部署..."

    kubectl delete -f "$PROJECT_ROOT/deploy/k8s/services-mtls/" --ignore-not-found
    kubectl delete -f "$PROJECT_ROOT/deploy/k8s/security/cert-manager/service-certificates.yaml" --ignore-not-found
    kubectl delete -f "$PROJECT_ROOT/deploy/k8s/security/cert-manager/ca-issuer.yaml" --ignore-not-found

    log_info "mTLS 部署已刪除"
}

# 主程式
main() {
    print_banner

    local install_cm=false
    local build=false
    local delete=false
    local verify=false

    # 解析參數
    while [[ $# -gt 0 ]]; do
        case $1 in
            --install-cert-manager)
                install_cm=true
                shift
                ;;
            --build)
                build=true
                shift
                ;;
            --delete)
                delete=true
                shift
                ;;
            --verify)
                verify=true
                shift
                ;;
            *)
                log_error "未知參數: $1"
                exit 1
                ;;
        esac
    done

    check_prerequisites

    # 確保 Kind cluster 存在
    if ! kind get clusters | grep -q "^${CLUSTER_NAME}$"; then
        log_error "Kind cluster '${CLUSTER_NAME}' 不存在"
        log_info "請先執行: ./deploy/scripts/k8s-deploy.sh"
        exit 1
    fi

    kubectl config use-context "kind-${CLUSTER_NAME}"

    if [ "$delete" = true ]; then
        delete_deployment
        exit 0
    fi

    if [ "$install_cm" = true ]; then
        install_cert_manager
    fi

    # 檢查 cert-manager 是否已安裝
    if ! kubectl get namespace cert-manager &> /dev/null; then
        log_error "cert-manager 未安裝"
        log_info "請使用 --install-cert-manager 參數安裝"
        exit 1
    fi

    setup_certificates

    if [ "$build" = true ]; then
        build_images
    fi

    deploy_infrastructure
    deploy_mtls_services

    if [ "$verify" = true ]; then
        verify_mtls
    fi

    show_status

    echo ""
    log_info "mTLS 部署完成！"
    echo ""
    echo "=== 使用說明 ==="
    echo "驗證 mTLS: ./deploy/scripts/k8s-mtls-deploy.sh --verify"
    echo "查看憑證: kubectl get certificates -n $NAMESPACE"
    echo "查看 Pod 日誌: kubectl logs -n $NAMESPACE -l app=gateway"
}

main "$@"
