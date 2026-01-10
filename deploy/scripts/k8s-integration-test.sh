#!/bin/bash
# ==============================================================================
# k8s-integration-test.sh - Kubernetes 整合測試腳本
#
# 說明：在 Kind 環境中執行整合測試，驗證所有服務正常運作
# 使用方式：./deploy/scripts/k8s-integration-test.sh
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

# 測試計數
TESTS_PASSED=0
TESTS_FAILED=0

# Cluster 名稱
CLUSTER_NAME="rbac-sso"
NAMESPACE="rbac-sso"

# ==============================================================================
# 函數定義
# ==============================================================================

print_banner() {
    echo -e "${BLUE}"
    echo "╔═══════════════════════════════════════════════════════════════╗"
    echo "║     RBAC-SSO-POC Kubernetes Integration Test Suite            ║"
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

log_test() {
    echo -e "${BLUE}[TEST]${NC} $1"
}

pass_test() {
    echo -e "${GREEN}[PASS]${NC} $1"
    TESTS_PASSED=$((TESTS_PASSED + 1))
}

fail_test() {
    echo -e "${RED}[FAIL]${NC} $1"
    TESTS_FAILED=$((TESTS_FAILED + 1))
}

# 等待服務就緒
wait_for_service() {
    local name=$1
    local url=$2
    local max_attempts=${3:-30}

    echo -n "  等待 $name 就緒: "
    for i in $(seq 1 $max_attempts); do
        if curl -sf "$url" >/dev/null 2>&1; then
            echo -e "${GREEN}Ready${NC}"
            return 0
        fi
        echo -n "."
        sleep 2
    done
    echo -e "${RED}Timeout${NC}"
    return 1
}

# ==============================================================================
# 測試用例
# ==============================================================================

# 測試 1: Kind Cluster 存在
test_cluster_exists() {
    log_test "Kind Cluster 檢查"

    if kind get clusters | grep -q "^${CLUSTER_NAME}$"; then
        pass_test "Kind cluster '${CLUSTER_NAME}' 存在"
    else
        fail_test "Kind cluster '${CLUSTER_NAME}' 不存在"
        return 1
    fi
}

# 測試 2: Namespace 存在
test_namespace_exists() {
    log_test "Namespace 檢查"

    if kubectl get namespace "$NAMESPACE" >/dev/null 2>&1; then
        pass_test "Namespace '${NAMESPACE}' 存在"
    else
        fail_test "Namespace '${NAMESPACE}' 不存在"
    fi
}

# 測試 3: Pod 狀態
test_pod_status() {
    log_test "Pod 狀態檢查"

    local pods=("openldap" "postgres" "keycloak" "gateway" "product-service" "user-service")

    for pod in "${pods[@]}"; do
        local status=$(kubectl get pods -n "$NAMESPACE" -l app="$pod" -o jsonpath='{.items[0].status.phase}' 2>/dev/null)
        if [ "$status" = "Running" ]; then
            pass_test "$pod Pod 運行中"
        else
            fail_test "$pod Pod 狀態: $status"
        fi
    done
}

# 測試 4: Pod Ready 狀態
test_pod_ready() {
    log_test "Pod Ready 狀態檢查"

    local pods=("openldap" "postgres" "keycloak" "gateway" "product-service" "user-service")

    for pod in "${pods[@]}"; do
        local ready=$(kubectl get pods -n "$NAMESPACE" -l app="$pod" -o jsonpath='{.items[0].status.conditions[?(@.type=="Ready")].status}' 2>/dev/null)
        if [ "$ready" = "True" ]; then
            pass_test "$pod Pod 已就緒"
        else
            fail_test "$pod Pod 未就緒"
        fi
    done
}

# 測試 5: Service 存在
test_services_exist() {
    log_test "Service 存在檢查"

    local services=("openldap" "postgres" "keycloak" "gateway" "product-service" "user-service")

    for svc in "${services[@]}"; do
        if kubectl get service "$svc" -n "$NAMESPACE" >/dev/null 2>&1; then
            pass_test "Service '$svc' 存在"
        else
            fail_test "Service '$svc' 不存在"
        fi
    done
}

# 測試 6: NodePort Service 存在
test_nodeport_services() {
    log_test "NodePort Service 檢查"

    local services=("gateway-nodeport" "product-service-nodeport" "user-service-nodeport" "keycloak-nodeport")

    for svc in "${services[@]}"; do
        if kubectl get service "$svc" -n "$NAMESPACE" >/dev/null 2>&1; then
            pass_test "NodePort Service '$svc' 存在"
        else
            fail_test "NodePort Service '$svc' 不存在"
        fi
    done
}

# 測試 7: 服務健康檢查 (通過 localhost)
test_service_health() {
    log_test "服務健康檢查 (HTTP)"

    local services=(
        "Gateway:http://localhost:8080/actuator/health"
        "Product Service:http://localhost:8081/actuator/health"
        "User Service:http://localhost:8082/actuator/health"
        "Keycloak:http://localhost:8180/health/ready"
    )

    for service in "${services[@]}"; do
        local name="${service%%:*}"
        local url="${service#*:}"

        if curl -sf "$url" >/dev/null 2>&1; then
            pass_test "$name 健康檢查通過"
        else
            fail_test "$name 健康檢查失敗 ($url)"
        fi
    done
}

# 測試 8: API 端點回應 (未認證應返回 401)
test_api_unauthorized() {
    log_test "API 未認證測試 (預期 401)"

    local response=$(curl -s -o /dev/null -w "%{http_code}" http://localhost:8081/api/products 2>/dev/null)
    if [ "$response" = "401" ]; then
        pass_test "Product API 未認證正確返回 401"
    else
        fail_test "Product API 未認證返回 $response (預期 401)"
    fi

    response=$(curl -s -o /dev/null -w "%{http_code}" http://localhost:8082/api/users/me 2>/dev/null)
    if [ "$response" = "401" ]; then
        pass_test "User API 未認證正確返回 401"
    else
        fail_test "User API 未認證返回 $response (預期 401)"
    fi
}

# 測試 9: Keycloak Admin Console
test_keycloak_admin() {
    log_test "Keycloak Admin Console 測試"

    local response=$(curl -s -o /dev/null -w "%{http_code}" http://localhost:8180/admin/master/console/ 2>/dev/null)
    if [ "$response" = "200" ] || [ "$response" = "302" ]; then
        pass_test "Keycloak Admin Console 可訪問"
    else
        fail_test "Keycloak Admin Console 返回 $response"
    fi
}

# 測試 10: ConfigMap 和 Secret 存在
test_config_resources() {
    log_test "ConfigMap 和 Secret 檢查"

    if kubectl get configmap rbac-config -n "$NAMESPACE" >/dev/null 2>&1; then
        pass_test "ConfigMap 'rbac-config' 存在"
    else
        fail_test "ConfigMap 'rbac-config' 不存在"
    fi

    if kubectl get secret rbac-secrets -n "$NAMESPACE" >/dev/null 2>&1; then
        pass_test "Secret 'rbac-secrets' 存在"
    else
        fail_test "Secret 'rbac-secrets' 不存在"
    fi
}

# 測試 11: PVC 狀態
test_pvc_status() {
    log_test "PersistentVolumeClaim 狀態檢查"

    local pvcs=("ldap-data-pvc" "ldap-config-pvc" "postgres-data-pvc")

    for pvc in "${pvcs[@]}"; do
        local status=$(kubectl get pvc "$pvc" -n "$NAMESPACE" -o jsonpath='{.status.phase}' 2>/dev/null)
        if [ "$status" = "Bound" ]; then
            pass_test "PVC '$pvc' 已綁定"
        else
            fail_test "PVC '$pvc' 狀態: $status"
        fi
    done
}

# 測試 12: 內部 DNS 解析
test_internal_dns() {
    log_test "內部 DNS 解析測試"

    local services=("openldap" "postgres" "keycloak" "gateway" "product-service" "user-service")

    for svc in "${services[@]}"; do
        # 使用 gateway pod 來測試 DNS 解析
        local result=$(kubectl exec -n "$NAMESPACE" deploy/gateway -- nslookup "$svc.$NAMESPACE.svc.cluster.local" 2>/dev/null | grep -c "Address" || echo "0")
        if [ "$result" -gt 1 ]; then
            pass_test "DNS 解析 '$svc' 成功"
        else
            fail_test "DNS 解析 '$svc' 失敗"
        fi
    done
}

# ==============================================================================
# 主程式
# ==============================================================================

main() {
    print_banner

    log_info "開始 Kubernetes 整合測試..."
    echo ""

    # 檢查 kubectl 是否可用
    if ! command -v kubectl &> /dev/null; then
        log_error "kubectl 未安裝"
        exit 1
    fi

    # 設置 kubectl context
    kubectl config use-context "kind-${CLUSTER_NAME}" 2>/dev/null || {
        log_error "無法切換到 kind-${CLUSTER_NAME} context"
        exit 1
    }

    # 檢查 cluster 是否存在
    if ! test_cluster_exists; then
        log_error "請先執行 ./deploy/scripts/k8s-deploy.sh 部署服務"
        exit 1
    fi

    echo ""
    echo "=========================================="
    echo "執行測試用例"
    echo "=========================================="
    echo ""

    # 執行所有測試
    test_namespace_exists
    echo ""
    test_config_resources
    echo ""
    test_pvc_status
    echo ""
    test_pod_status
    echo ""
    test_pod_ready
    echo ""
    test_services_exist
    echo ""
    test_nodeport_services
    echo ""

    # 等待服務就緒後執行 HTTP 測試
    log_info "等待服務啟動完成..."
    sleep 5

    test_service_health
    echo ""
    test_api_unauthorized
    echo ""
    test_keycloak_admin

    # DNS 測試 (可選，需要 gateway pod 運行)
    # echo ""
    # test_internal_dns

    # 輸出測試結果
    echo ""
    echo "=========================================="
    echo "測試結果總結"
    echo "=========================================="
    echo -e "通過: ${GREEN}$TESTS_PASSED${NC}"
    echo -e "失敗: ${RED}$TESTS_FAILED${NC}"
    echo ""

    if [ $TESTS_FAILED -eq 0 ]; then
        echo -e "${GREEN}✓ 所有測試通過！${NC}"
        exit 0
    else
        echo -e "${RED}✗ 有 $TESTS_FAILED 個測試失敗${NC}"
        echo ""
        log_info "查看 Pod 狀態: kubectl get pods -n $NAMESPACE"
        log_info "查看 Pod 日誌: kubectl logs -n $NAMESPACE <pod-name>"
        exit 1
    fi
}

main "$@"
