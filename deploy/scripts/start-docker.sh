#!/bin/bash
# ==============================================================================
# start-docker.sh - Docker Compose 完整環境啟動腳本
#
# 說明：所有服務都在 Docker 中運行
# 使用方式：./deploy/scripts/start-docker.sh [start|stop|status|logs|build]
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
LDIF_FILE="$PROJECT_ROOT/infra/ldap/bootstrap.ldif"

# ==============================================================================
# 函數定義
# ==============================================================================

print_banner() {
    echo -e "${BLUE}"
    echo "╔═══════════════════════════════════════════════════════════════╗"
    echo "║         RBAC-SSO-POC Docker Compose Environment               ║"
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

# 檢查必要工具
check_prerequisites() {
    log_info "檢查必要工具..."

    if ! command -v docker >/dev/null 2>&1; then
        log_error "Docker 未安裝"
        exit 1
    fi

    if ! docker info >/dev/null 2>&1; then
        log_error "Docker daemon 未運行"
        exit 1
    fi

    log_info "Docker 檢查通過"
}

# 建置 Docker images
build_images() {
    log_info "建置 Docker images..."

    cd "$PROJECT_ROOT"

    # 檢查服務目錄是否存在
    if [ ! -d "services" ]; then
        log_warn "services 目錄不存在，僅啟動基礎設施"
        return 1
    fi

    # 建置 Gradle 專案
    log_info "編譯 Java 專案..."
    ./gradlew build -x test || {
        log_warn "編譯失敗，僅啟動基礎設施"
        return 1
    }

    # 建置 Docker images
    cd "$DOCKER_DIR"
    docker compose build

    log_info "Docker images 建置完成"
    return 0
}

# 啟動服務
start_services() {
    local infra_only=${1:-false}

    cd "$DOCKER_DIR"

    if [ "$infra_only" = true ]; then
        log_info "啟動基礎設施服務..."
        docker compose -f docker-compose.infra.yml up -d
    else
        log_info "啟動所有服務..."
        docker compose up -d
    fi

    # 等待 OpenLDAP 就緒
    log_info "等待服務就緒..."
    echo -n "  OpenLDAP: "
    for i in {1..30}; do
        if docker exec rbac-openldap ldapsearch -x -H ldap://localhost -D "cn=admin,dc=ecommerce,dc=local" -w admin123 -b "dc=ecommerce,dc=local" >/dev/null 2>&1; then
            echo -e "${GREEN}Ready${NC}"
            break
        fi
        echo -n "."
        sleep 2
    done

    # 導入 LDAP 用戶
    if ! docker exec rbac-openldap ldapsearch -x -H ldap://localhost -D "cn=admin,dc=ecommerce,dc=local" -w admin123 -b "ou=users,dc=ecommerce,dc=local" uid 2>/dev/null | grep -q "uid: admin.user"; then
        log_info "導入 LDAP 測試用戶..."
        cat "$LDIF_FILE" | docker exec -i rbac-openldap ldapadd -x -D "cn=admin,dc=ecommerce,dc=local" -w admin123 2>/dev/null || true
    fi

    # 等待 Keycloak 就緒
    echo -n "  Keycloak: "
    for i in {1..60}; do
        if curl -sf http://localhost:8180/health/ready >/dev/null 2>&1; then
            echo -e "${GREEN}Ready${NC}"
            break
        fi
        echo -n "."
        sleep 3
    done

    if [ "$infra_only" = false ]; then
        # 等待應用服務
        for svc in gateway user-service product-service; do
            port=$(docker inspect "rbac-$svc" --format '{{range $p, $conf := .NetworkSettings.Ports}}{{(index $conf 0).HostPort}}{{end}}' 2>/dev/null || echo "")
            if [ -n "$port" ]; then
                echo -n "  $svc: "
                for i in {1..30}; do
                    if curl -sf "http://localhost:$port/actuator/health" >/dev/null 2>&1; then
                        echo -e "${GREEN}Ready${NC}"
                        break
                    fi
                    echo -n "."
                    sleep 2
                done
            fi
        done
    fi
}

# 停止服務
stop_services() {
    log_info "停止所有服務..."

    cd "$DOCKER_DIR"

    # 停止完整環境
    docker compose down 2>/dev/null || true

    # 停止基礎設施
    docker compose -f docker-compose.infra.yml down 2>/dev/null || true

    log_info "所有服務已停止"
}

# 顯示服務狀態
show_status() {
    echo -e "\n${BLUE}=== Docker 服務狀態 ===${NC}"
    docker ps --filter "name=rbac-" --format "table {{.Names}}\t{{.Status}}\t{{.Ports}}" 2>/dev/null || echo "無運行中的服務"

    echo -e "\n${BLUE}=== 存取資訊 ===${NC}"
    echo "Keycloak Admin:    http://localhost:8180 (admin / admin123)"
    echo "phpLDAPadmin:      http://localhost:8181"
    echo "Gateway:           http://localhost:8080"
    echo "User Service:      http://localhost:8081"
    echo "Product Service:   http://localhost:8082"
}

# 顯示日誌
show_logs() {
    local service=$1

    cd "$DOCKER_DIR"

    if [ -z "$service" ]; then
        docker compose logs -f
    else
        docker logs -f "rbac-$service"
    fi
}

# 清理資源
cleanup() {
    log_info "清理 Docker 資源..."

    cd "$DOCKER_DIR"

    docker compose down -v 2>/dev/null || true
    docker compose -f docker-compose.infra.yml down -v 2>/dev/null || true

    log_info "資源已清理"
}

# 顯示使用說明
show_usage() {
    echo "使用方式: $0 [command]"
    echo ""
    echo "Commands:"
    echo "  start       啟動所有服務 (預設)"
    echo "  start-infra 僅啟動基礎設施 (LDAP, Keycloak, PostgreSQL)"
    echo "  stop        停止所有服務"
    echo "  restart     重啟所有服務"
    echo "  status      顯示服務狀態"
    echo "  logs        顯示所有服務日誌"
    echo "  logs <svc>  顯示特定服務日誌"
    echo "  build       建置 Docker images"
    echo "  clean       停止並清理所有資源 (包含 volumes)"
    echo ""
    echo "Services: openldap, postgres, keycloak, phpldapadmin, gateway, user-service, product-service"
}

# ==============================================================================
# 主程式
# ==============================================================================

main() {
    print_banner

    case "${1:-start}" in
        start)
            check_prerequisites
            if build_images; then
                start_services false
            else
                start_services true
            fi
            echo ""
            show_status
            ;;
        start-infra)
            check_prerequisites
            start_services true
            echo ""
            show_status
            ;;
        stop)
            stop_services
            ;;
        restart)
            stop_services
            sleep 3
            check_prerequisites
            if build_images; then
                start_services false
            else
                start_services true
            fi
            show_status
            ;;
        status)
            show_status
            ;;
        logs)
            show_logs "$2"
            ;;
        build)
            check_prerequisites
            build_images
            ;;
        clean)
            cleanup
            ;;
        help|--help|-h)
            show_usage
            ;;
        *)
            log_error "未知命令: $1"
            show_usage
            exit 1
            ;;
    esac
}

main "$@"
