#!/bin/bash
# ==============================================================================
# integration-test.sh - Docker 整合測試腳本
#
# 說明：在 Docker 環境中執行整合測試，驗證所有服務正常運作
# 使用方式：./deploy/scripts/integration-test.sh
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
DOCKER_DIR="$PROJECT_ROOT/deploy/docker"

# 測試計數
TESTS_PASSED=0
TESTS_FAILED=0

# ==============================================================================
# 函數定義
# ==============================================================================

print_banner() {
    echo -e "${BLUE}"
    echo "╔═══════════════════════════════════════════════════════════════╗"
    echo "║         RBAC-SSO-POC Integration Test Suite                   ║"
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

# 測試 1: 服務健康檢查
test_service_health() {
    log_test "服務健康檢查"

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
            fail_test "$name 健康檢查失敗"
        fi
    done
}

# 測試 2: LDAP 連接
test_ldap_connection() {
    log_test "LDAP 連接測試"

    if docker exec rbac-openldap ldapsearch -x -H ldap://localhost -D "cn=admin,dc=ecommerce,dc=local" -w admin123 -b "dc=ecommerce,dc=local" >/dev/null 2>&1; then
        pass_test "LDAP 連接成功"
    else
        fail_test "LDAP 連接失敗"
    fi
}

# 測試 3: PostgreSQL 連接 (Keycloak DB)
test_postgres_connection() {
    log_test "PostgreSQL 連接測試"

    if docker exec rbac-postgres pg_isready -U keycloak >/dev/null 2>&1; then
        pass_test "PostgreSQL 連接成功"
    else
        fail_test "PostgreSQL 連接失敗"
    fi
}

# 測試 4: API 端點回應 (未認證應返回 401)
test_api_unauthorized() {
    log_test "API 未認證測試 (預期 401)"

    local response=$(curl -s -o /dev/null -w "%{http_code}" http://localhost:8081/api/products)
    if [ "$response" = "401" ]; then
        pass_test "Product API 未認證正確返回 401"
    else
        fail_test "Product API 未認證返回 $response (預期 401)"
    fi

    response=$(curl -s -o /dev/null -w "%{http_code}" http://localhost:8082/api/users/me)
    if [ "$response" = "401" ]; then
        pass_test "User API 未認證正確返回 401"
    else
        fail_test "User API 未認證返回 $response (預期 401)"
    fi
}

# 測試 5: Gateway 路由
test_gateway_routing() {
    log_test "Gateway 路由測試"

    # Gateway actuator 應該可以訪問
    local response=$(curl -s -o /dev/null -w "%{http_code}" http://localhost:8080/actuator/health)
    if [ "$response" = "200" ]; then
        pass_test "Gateway actuator 端點可訪問"
    else
        fail_test "Gateway actuator 端點返回 $response"
    fi
}

# 測試 6: Keycloak Admin Console
test_keycloak_admin() {
    log_test "Keycloak Admin Console 測試"

    local response=$(curl -s -o /dev/null -w "%{http_code}" http://localhost:8180/admin/master/console/)
    if [ "$response" = "200" ] || [ "$response" = "302" ]; then
        pass_test "Keycloak Admin Console 可訪問"
    else
        fail_test "Keycloak Admin Console 返回 $response"
    fi
}

# 測試 7: Docker 容器狀態
test_container_status() {
    log_test "Docker 容器狀態測試"

    local containers=(
        "rbac-openldap"
        "rbac-postgres"
        "rbac-keycloak"
        "rbac-gateway"
        "rbac-product-service"
        "rbac-user-service"
    )

    for container in "${containers[@]}"; do
        local status=$(docker inspect -f '{{.State.Status}}' "$container" 2>/dev/null)
        if [ "$status" = "running" ]; then
            pass_test "$container 運行中"
        else
            fail_test "$container 狀態: $status"
        fi
    done
}

# ==============================================================================
# 主程式
# ==============================================================================

main() {
    print_banner

    log_info "開始整合測試..."
    echo ""

    # 檢查 Docker 服務是否運行
    if ! docker info >/dev/null 2>&1; then
        log_error "Docker daemon 未運行"
        exit 1
    fi

    # 檢查服務是否已啟動
    local running_containers=$(docker ps --filter "name=rbac-" --format "{{.Names}}" | wc -l)
    if [ "$running_containers" -lt 6 ]; then
        log_warn "Docker 服務未完全啟動，正在等待..."
        wait_for_service "Gateway" "http://localhost:8080/actuator/health" 60
        wait_for_service "Product Service" "http://localhost:8081/actuator/health" 60
        wait_for_service "User Service" "http://localhost:8082/actuator/health" 60
    fi

    echo ""
    echo "=========================================="
    echo "執行測試用例"
    echo "=========================================="
    echo ""

    # 執行所有測試
    test_container_status
    echo ""
    test_service_health
    echo ""
    test_ldap_connection
    echo ""
    test_postgres_connection
    echo ""
    test_api_unauthorized
    echo ""
    test_gateway_routing
    echo ""
    test_keycloak_admin

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
        exit 1
    fi
}

main "$@"
