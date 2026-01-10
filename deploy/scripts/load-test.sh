#!/bin/bash
# ==============================================================================
# load-test.sh - 簡易壓力測試腳本
#
# 說明：對 API 端點進行簡易壓力測試
# 使用方式：./deploy/scripts/load-test.sh [options]
#
# 依賴：curl (必須), ab/wrk/hey (可選，擇一)
# ==============================================================================

set -e

# 顏色定義
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
CYAN='\033[0;36m'
NC='\033[0m'

# 預設設定
KEYCLOAK_URL="${KEYCLOAK_URL:-http://localhost:8180}"
GATEWAY_URL="${GATEWAY_URL:-http://localhost:8080}"
CLIENT_ID="${CLIENT_ID:-ecommerce-api}"
CLIENT_SECRET="${CLIENT_SECRET:-}"
USERNAME="${USERNAME:-admin.user}"
PASSWORD="${PASSWORD:-admin123}"
REALM="${REALM:-ecommerce}"

# 壓測參數
CONCURRENT="${CONCURRENT:-10}"
REQUESTS="${REQUESTS:-100}"
DURATION="${DURATION:-30}"

# ==============================================================================
# 函數定義
# ==============================================================================

print_banner() {
    echo -e "${BLUE}"
    echo "╔═══════════════════════════════════════════════════════════════╗"
    echo "║              RBAC-SSO-POC Load Testing Tool                   ║"
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

log_result() {
    echo -e "${CYAN}[RESULT]${NC} $1"
}

# 檢查服務健康狀態
check_health() {
    log_info "檢查服務健康狀態..."

    local services=(
        "Keycloak:$KEYCLOAK_URL/health/ready"
        "Gateway:$GATEWAY_URL/actuator/health"
    )

    local all_healthy=true

    for svc in "${services[@]}"; do
        name="${svc%%:*}"
        url="${svc#*:}"

        if curl -sf "$url" >/dev/null 2>&1; then
            echo -e "  $name: ${GREEN}Healthy${NC}"
        else
            echo -e "  $name: ${RED}Unhealthy${NC}"
            all_healthy=false
        fi
    done

    if [ "$all_healthy" = false ]; then
        log_error "部分服務不健康，請先確保服務正常運行"
        exit 1
    fi
}

# 取得 OAuth2 Token
get_token() {
    log_info "取得 OAuth2 Access Token..."

    local token_url="$KEYCLOAK_URL/realms/$REALM/protocol/openid-connect/token"

    local response
    if [ -n "$CLIENT_SECRET" ]; then
        # Client Credentials Flow
        response=$(curl -s -X POST "$token_url" \
            -H "Content-Type: application/x-www-form-urlencoded" \
            -d "client_id=$CLIENT_ID" \
            -d "client_secret=$CLIENT_SECRET" \
            -d "grant_type=client_credentials")
    else
        # Resource Owner Password Flow
        response=$(curl -s -X POST "$token_url" \
            -H "Content-Type: application/x-www-form-urlencoded" \
            -d "client_id=$CLIENT_ID" \
            -d "username=$USERNAME" \
            -d "password=$PASSWORD" \
            -d "grant_type=password")
    fi

    ACCESS_TOKEN=$(echo "$response" | grep -o '"access_token":"[^"]*"' | cut -d'"' -f4)

    if [ -z "$ACCESS_TOKEN" ]; then
        log_warn "無法取得 Token，可能 Keycloak 尚未設定完成"
        log_warn "將使用無認證模式進行測試"
        return 1
    fi

    log_info "Token 取得成功 (長度: ${#ACCESS_TOKEN})"
    return 0
}

# 使用 curl 進行簡單壓測
load_test_curl() {
    local endpoint=$1
    local method=${2:-GET}
    local data=$3

    log_info "開始壓測 ($method $endpoint)"
    log_info "並發數: $CONCURRENT, 請求數: $REQUESTS"

    local start_time=$(date +%s.%N)
    local success=0
    local failed=0
    local total_time=0

    # 建立暫存目錄
    local tmp_dir=$(mktemp -d)
    trap "rm -rf $tmp_dir" EXIT

    # 執行並發請求
    for ((i=1; i<=REQUESTS; i++)); do
        (
            local req_start=$(date +%s.%N)

            local curl_opts="-s -o /dev/null -w %{http_code}:%{time_total}"
            if [ -n "$ACCESS_TOKEN" ]; then
                curl_opts="$curl_opts -H 'Authorization: Bearer $ACCESS_TOKEN'"
            fi

            local result
            if [ "$method" = "POST" ] && [ -n "$data" ]; then
                result=$(eval curl $curl_opts -X POST -H "'Content-Type: application/json'" -d "'$data'" "$GATEWAY_URL$endpoint")
            else
                result=$(eval curl $curl_opts "$GATEWAY_URL$endpoint")
            fi

            local http_code="${result%%:*}"
            local req_time="${result##*:}"

            echo "$http_code:$req_time" > "$tmp_dir/$i.result"
        ) &

        # 控制並發數
        if [ $((i % CONCURRENT)) -eq 0 ]; then
            wait
        fi
    done

    wait

    local end_time=$(date +%s.%N)

    # 統計結果
    for result_file in "$tmp_dir"/*.result; do
        if [ -f "$result_file" ]; then
            local result=$(cat "$result_file")
            local http_code="${result%%:*}"
            local req_time="${result##*:}"

            if [[ "$http_code" =~ ^2[0-9][0-9]$ ]]; then
                ((success++))
            else
                ((failed++))
            fi

            total_time=$(echo "$total_time + $req_time" | bc)
        fi
    done

    local elapsed=$(echo "$end_time - $start_time" | bc)
    local avg_time=$(echo "scale=3; $total_time / $REQUESTS" | bc)
    local rps=$(echo "scale=2; $REQUESTS / $elapsed" | bc)

    # 輸出結果
    echo ""
    echo -e "${BLUE}═══════════════════════════════════════════════════════════════${NC}"
    echo -e "${BLUE}                        測試結果                                ${NC}"
    echo -e "${BLUE}═══════════════════════════════════════════════════════════════${NC}"
    log_result "Endpoint:     $method $endpoint"
    log_result "總請求數:     $REQUESTS"
    log_result "成功:         $success ($(echo "scale=1; $success * 100 / $REQUESTS" | bc)%)"
    log_result "失敗:         $failed"
    log_result "總耗時:       ${elapsed}s"
    log_result "平均回應時間: ${avg_time}s"
    log_result "RPS:          $rps req/s"
    echo -e "${BLUE}═══════════════════════════════════════════════════════════════${NC}"
}

# 使用 hey 進行壓測 (如果有安裝)
load_test_hey() {
    local endpoint=$1
    local method=${2:-GET}

    if ! command -v hey >/dev/null 2>&1; then
        log_warn "hey 未安裝，使用 curl 進行測試"
        load_test_curl "$endpoint" "$method"
        return
    fi

    log_info "使用 hey 進行壓測"
    log_info "並發數: $CONCURRENT, 持續時間: ${DURATION}s"

    local hey_opts="-c $CONCURRENT -z ${DURATION}s"
    if [ -n "$ACCESS_TOKEN" ]; then
        hey_opts="$hey_opts -H 'Authorization: Bearer $ACCESS_TOKEN'"
    fi

    eval hey $hey_opts "$GATEWAY_URL$endpoint"
}

# 使用 ab 進行壓測 (如果有安裝)
load_test_ab() {
    local endpoint=$1

    if ! command -v ab >/dev/null 2>&1; then
        log_warn "ab (Apache Bench) 未安裝，使用 curl 進行測試"
        load_test_curl "$endpoint"
        return
    fi

    log_info "使用 Apache Bench 進行壓測"
    log_info "並發數: $CONCURRENT, 請求數: $REQUESTS"

    local ab_opts="-c $CONCURRENT -n $REQUESTS"
    if [ -n "$ACCESS_TOKEN" ]; then
        ab_opts="$ab_opts -H 'Authorization: Bearer $ACCESS_TOKEN'"
    fi

    eval ab $ab_opts "$GATEWAY_URL$endpoint"
}

# 執行預設測試套件
run_test_suite() {
    log_info "執行預設測試套件..."

    # 1. Health Check 測試
    echo -e "\n${YELLOW}[1/4] Health Check 測試${NC}"
    load_test_curl "/actuator/health" "GET"

    # 2. 產品列表查詢測試
    echo -e "\n${YELLOW}[2/4] 產品列表查詢測試${NC}"
    load_test_curl "/api/products" "GET"

    # 3. 使用者查詢測試
    echo -e "\n${YELLOW}[3/4] 使用者查詢測試${NC}"
    load_test_curl "/api/users" "GET"

    # 4. 認證端點測試
    echo -e "\n${YELLOW}[4/4] Token 端點測試${NC}"
    local token_requests=$((REQUESTS / 10))  # Token 請求數減少
    REQUESTS=$token_requests load_test_curl "" "POST" "" &
    # 直接對 Keycloak 測試
    local old_gateway=$GATEWAY_URL
    GATEWAY_URL=$KEYCLOAK_URL
    load_test_curl "/realms/$REALM/protocol/openid-connect/token" "POST" \
        "client_id=$CLIENT_ID&username=$USERNAME&password=$PASSWORD&grant_type=password"
    GATEWAY_URL=$old_gateway
}

# 顯示使用說明
show_usage() {
    echo "使用方式: $0 [options] [command]"
    echo ""
    echo "Commands:"
    echo "  test              執行預設測試套件 (預設)"
    echo "  health            測試 health endpoint"
    echo "  endpoint <path>   測試特定 endpoint"
    echo ""
    echo "Options:"
    echo "  -c, --concurrent N   並發連線數 (預設: $CONCURRENT)"
    echo "  -n, --requests N     總請求數 (預設: $REQUESTS)"
    echo "  -d, --duration N     測試持續時間秒數 (預設: $DURATION)"
    echo "  -g, --gateway URL    Gateway URL (預設: $GATEWAY_URL)"
    echo "  -k, --keycloak URL   Keycloak URL (預設: $KEYCLOAK_URL)"
    echo "  -u, --user NAME      測試用戶名 (預設: $USERNAME)"
    echo "  -p, --password PASS  測試密碼 (預設: $PASSWORD)"
    echo "  --tool [curl|hey|ab] 壓測工具 (預設: curl)"
    echo "  -h, --help           顯示說明"
    echo ""
    echo "環境變數:"
    echo "  KEYCLOAK_URL, GATEWAY_URL, CLIENT_ID, CLIENT_SECRET"
    echo "  USERNAME, PASSWORD, REALM"
    echo ""
    echo "範例:"
    echo "  $0 test                          # 執行預設測試"
    echo "  $0 -c 50 -n 1000 test            # 50 並發，1000 請求"
    echo "  $0 endpoint /api/products        # 測試特定端點"
    echo "  $0 --tool hey test               # 使用 hey 工具"
}

# ==============================================================================
# 主程式
# ==============================================================================

main() {
    local tool="curl"
    local command="test"
    local endpoint=""

    # 解析參數
    while [[ $# -gt 0 ]]; do
        case $1 in
            -c|--concurrent)
                CONCURRENT="$2"
                shift 2
                ;;
            -n|--requests)
                REQUESTS="$2"
                shift 2
                ;;
            -d|--duration)
                DURATION="$2"
                shift 2
                ;;
            -g|--gateway)
                GATEWAY_URL="$2"
                shift 2
                ;;
            -k|--keycloak)
                KEYCLOAK_URL="$2"
                shift 2
                ;;
            -u|--user)
                USERNAME="$2"
                shift 2
                ;;
            -p|--password)
                PASSWORD="$2"
                shift 2
                ;;
            --tool)
                tool="$2"
                shift 2
                ;;
            -h|--help)
                show_usage
                exit 0
                ;;
            test|health|endpoint)
                command="$1"
                if [ "$1" = "endpoint" ]; then
                    endpoint="$2"
                    shift
                fi
                shift
                ;;
            *)
                if [ -z "$endpoint" ] && [[ "$1" == /* ]]; then
                    endpoint="$1"
                    command="endpoint"
                fi
                shift
                ;;
        esac
    done

    print_banner

    echo -e "${BLUE}測試配置:${NC}"
    echo "  Gateway URL:  $GATEWAY_URL"
    echo "  Keycloak URL: $KEYCLOAK_URL"
    echo "  並發數:       $CONCURRENT"
    echo "  請求數:       $REQUESTS"
    echo "  工具:         $tool"
    echo ""

    # 檢查健康狀態
    check_health

    # 取得 Token
    get_token || true

    # 執行測試
    case $command in
        test)
            run_test_suite
            ;;
        health)
            load_test_curl "/actuator/health" "GET"
            ;;
        endpoint)
            if [ -z "$endpoint" ]; then
                log_error "請指定 endpoint"
                exit 1
            fi
            case $tool in
                hey)
                    load_test_hey "$endpoint"
                    ;;
                ab)
                    load_test_ab "$endpoint"
                    ;;
                *)
                    load_test_curl "$endpoint"
                    ;;
            esac
            ;;
        *)
            log_error "未知命令: $command"
            show_usage
            exit 1
            ;;
    esac

    echo ""
    log_info "測試完成"
}

main "$@"
