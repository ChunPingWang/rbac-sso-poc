#!/bin/bash
# ==============================================================================
# start-local.sh - 本地開發環境啟動腳本
#
# 說明：Keycloak/LDAP/PostgreSQL 使用 Docker，Java 應用直接在 OS 上啟動
# 使用方式：./deploy/scripts/start-local.sh [start|stop|status|logs]
# ==============================================================================

set -e

# 顏色定義
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# 專案根目錄
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"
DOCKER_DIR="$PROJECT_ROOT/deploy/docker"
LDIF_FILE="$PROJECT_ROOT/infra/ldap/bootstrap.ldif"

# PID 檔案目錄
PID_DIR="$PROJECT_ROOT/.pids"
mkdir -p "$PID_DIR"

# ==============================================================================
# 函數定義
# ==============================================================================

print_banner() {
    echo -e "${BLUE}"
    echo "╔═══════════════════════════════════════════════════════════════╗"
    echo "║           RBAC-SSO-POC Local Development Environment          ║"
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

    local missing=()

    command -v docker >/dev/null 2>&1 || missing+=("docker")
    command -v java >/dev/null 2>&1 || missing+=("java")

    if [ ${#missing[@]} -ne 0 ]; then
        log_error "缺少必要工具: ${missing[*]}"
        exit 1
    fi

    # 檢查 Java 版本
    java_version=$(java -version 2>&1 | head -n 1 | cut -d'"' -f2 | cut -d'.' -f1)
    if [ "$java_version" -lt 17 ]; then
        log_error "需要 Java 17 或更高版本，當前版本: $java_version"
        exit 1
    fi

    log_info "工具檢查通過 (Java $java_version)"
}

# 啟動 Docker 基礎設施
start_docker_infra() {
    log_info "啟動 Docker 基礎設施 (OpenLDAP, PostgreSQL, Keycloak)..."

    cd "$DOCKER_DIR"
    docker compose -f docker-compose.infra.yml up -d

    # 等待服務就緒
    log_info "等待服務就緒..."

    # 等待 OpenLDAP
    echo -n "  OpenLDAP: "
    for i in {1..30}; do
        if docker exec rbac-openldap ldapsearch -x -H ldap://localhost -D "cn=admin,dc=ecommerce,dc=local" -w admin123 -b "dc=ecommerce,dc=local" >/dev/null 2>&1; then
            echo -e "${GREEN}Ready${NC}"
            break
        fi
        echo -n "."
        sleep 2
    done

    # 導入 LDAP 用戶 (如果尚未導入)
    if ! docker exec rbac-openldap ldapsearch -x -H ldap://localhost -D "cn=admin,dc=ecommerce,dc=local" -w admin123 -b "ou=users,dc=ecommerce,dc=local" uid 2>/dev/null | grep -q "uid: admin.user"; then
        log_info "導入 LDAP 測試用戶..."
        cat "$LDIF_FILE" | docker exec -i rbac-openldap ldapadd -x -D "cn=admin,dc=ecommerce,dc=local" -w admin123 2>/dev/null || true
    fi

    # 等待 PostgreSQL
    echo -n "  PostgreSQL: "
    for i in {1..30}; do
        if docker exec rbac-postgres pg_isready -U keycloak >/dev/null 2>&1; then
            echo -e "${GREEN}Ready${NC}"
            break
        fi
        echo -n "."
        sleep 2
    done

    # 等待 Keycloak
    echo -n "  Keycloak: "
    for i in {1..60}; do
        if curl -sf http://localhost:8180/health/ready >/dev/null 2>&1; then
            echo -e "${GREEN}Ready${NC}"
            break
        fi
        echo -n "."
        sleep 3
    done
}

# 啟動 Java 應用
start_java_apps() {
    log_info "啟動 Java 應用..."

    cd "$PROJECT_ROOT"

    # 檢查是否有 Gradle wrapper
    if [ ! -f "./gradlew" ]; then
        log_warn "找不到 gradlew，嘗試使用系統 gradle"
        GRADLE_CMD="gradle"
    else
        GRADLE_CMD="./gradlew"
        chmod +x ./gradlew
    fi

    # 檢查服務目錄是否存在
    if [ ! -d "libs/audit-lib" ]; then
        log_warn "audit-lib 尚未建立，跳過 Java 應用啟動"
        log_info "請先完成 audit-lib 實作後再啟動 Java 應用"
        return
    fi

    # 編譯專案
    log_info "編譯專案..."
    $GRADLE_CMD build -x test || {
        log_warn "編譯失敗，可能尚未建立服務模組"
        return
    }

    # 啟動各服務 (背景執行)
    local services=("gateway:8080" "user-service:8081" "product-service:8082")

    for svc_port in "${services[@]}"; do
        svc="${svc_port%%:*}"
        port="${svc_port##*:}"

        if [ -d "services/$svc" ]; then
            log_info "啟動 $svc (port: $port)..."

            nohup $GRADLE_CMD :services:$svc:bootRun \
                --args="--spring.profiles.active=local" \
                > "$PROJECT_ROOT/logs/$svc.log" 2>&1 &

            echo $! > "$PID_DIR/$svc.pid"
            log_info "$svc 已啟動 (PID: $!)"
        fi
    done
}

# 停止所有服務
stop_all() {
    log_info "停止所有服務..."

    # 停止 Java 應用
    for pid_file in "$PID_DIR"/*.pid; do
        if [ -f "$pid_file" ]; then
            pid=$(cat "$pid_file")
            svc=$(basename "$pid_file" .pid)

            if kill -0 "$pid" 2>/dev/null; then
                log_info "停止 $svc (PID: $pid)..."
                kill "$pid" 2>/dev/null || true
            fi
            rm -f "$pid_file"
        fi
    done

    # 停止 Docker 服務
    log_info "停止 Docker 基礎設施..."
    cd "$DOCKER_DIR"
    docker compose -f docker-compose.infra.yml down

    log_info "所有服務已停止"
}

# 顯示服務狀態
show_status() {
    echo -e "\n${BLUE}=== Docker 服務狀態 ===${NC}"
    docker ps --filter "name=rbac-" --format "table {{.Names}}\t{{.Status}}\t{{.Ports}}" 2>/dev/null || echo "無運行中的 Docker 服務"

    echo -e "\n${BLUE}=== Java 應用狀態 ===${NC}"
    for pid_file in "$PID_DIR"/*.pid; do
        if [ -f "$pid_file" ]; then
            pid=$(cat "$pid_file")
            svc=$(basename "$pid_file" .pid)

            if kill -0 "$pid" 2>/dev/null; then
                echo -e "$svc: ${GREEN}Running${NC} (PID: $pid)"
            else
                echo -e "$svc: ${RED}Stopped${NC}"
            fi
        fi
    done

    if [ ! -f "$PID_DIR"/*.pid ] 2>/dev/null; then
        echo "無運行中的 Java 應用"
    fi

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

    if [ -z "$service" ]; then
        log_info "顯示 Docker 日誌 (按 Ctrl+C 退出)..."
        docker compose -f "$DOCKER_DIR/docker-compose.infra.yml" logs -f
    else
        case $service in
            keycloak|openldap|postgres|phpldapadmin)
                docker logs -f "rbac-$service"
                ;;
            gateway|user-service|product-service)
                if [ -f "$PROJECT_ROOT/logs/$service.log" ]; then
                    tail -f "$PROJECT_ROOT/logs/$service.log"
                else
                    log_error "找不到 $service 日誌檔案"
                fi
                ;;
            *)
                log_error "未知服務: $service"
                ;;
        esac
    fi
}

# 顯示使用說明
show_usage() {
    echo "使用方式: $0 [command]"
    echo ""
    echo "Commands:"
    echo "  start     啟動所有服務 (預設)"
    echo "  stop      停止所有服務"
    echo "  restart   重啟所有服務"
    echo "  status    顯示服務狀態"
    echo "  logs      顯示 Docker 日誌"
    echo "  logs <service>  顯示特定服務日誌"
    echo ""
    echo "Services: keycloak, openldap, postgres, gateway, user-service, product-service"
}

# ==============================================================================
# 主程式
# ==============================================================================

main() {
    print_banner

    case "${1:-start}" in
        start)
            check_prerequisites
            start_docker_infra
            start_java_apps
            echo ""
            show_status
            ;;
        stop)
            stop_all
            ;;
        restart)
            stop_all
            sleep 3
            check_prerequisites
            start_docker_infra
            start_java_apps
            show_status
            ;;
        status)
            show_status
            ;;
        logs)
            show_logs "$2"
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

# 建立日誌目錄
mkdir -p "$PROJECT_ROOT/logs"

main "$@"
