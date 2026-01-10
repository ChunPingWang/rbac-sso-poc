#!/bin/bash
# ==============================================================================
# start-k8s.sh - Kubernetes 環境部署腳本
#
# 說明：所有服務都在 Kubernetes 中運行 (使用 Kind)
# 使用方式：./deploy/scripts/start-k8s.sh [start|stop|status|logs]
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

# Kind cluster 名稱
CLUSTER_NAME="rbac-sso-poc"
NAMESPACE="rbac-sso"

# ==============================================================================
# 函數定義
# ==============================================================================

print_banner() {
    echo -e "${BLUE}"
    echo "╔═══════════════════════════════════════════════════════════════╗"
    echo "║           RBAC-SSO-POC Kubernetes Environment                 ║"
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
    command -v kind >/dev/null 2>&1 || missing+=("kind")
    command -v kubectl >/dev/null 2>&1 || missing+=("kubectl")

    if [ ${#missing[@]} -ne 0 ]; then
        log_error "缺少必要工具: ${missing[*]}"
        exit 1
    fi

    log_info "工具檢查通過"
}

# 建立 Kind Cluster
create_cluster() {
    if kind get clusters 2>/dev/null | grep -q "^${CLUSTER_NAME}$"; then
        log_info "Kind cluster '$CLUSTER_NAME' 已存在"
        kubectl cluster-info --context "kind-$CLUSTER_NAME" >/dev/null 2>&1 || {
            log_warn "Cluster context 無效，重新建立..."
            kind delete cluster --name "$CLUSTER_NAME"
        }
    fi

    if ! kind get clusters 2>/dev/null | grep -q "^${CLUSTER_NAME}$"; then
        log_info "建立 Kind cluster: $CLUSTER_NAME..."

        # 建立 Kind config
        cat > /tmp/kind-config.yaml <<EOF
kind: Cluster
apiVersion: kind.x-k8s.io/v1alpha4
name: $CLUSTER_NAME
nodes:
  - role: control-plane
    kubeadmConfigPatches:
      - |
        kind: InitConfiguration
        nodeRegistration:
          kubeletExtraArgs:
            node-labels: "ingress-ready=true"
    extraPortMappings:
      - containerPort: 80
        hostPort: 80
        protocol: TCP
      - containerPort: 443
        hostPort: 443
        protocol: TCP
      - containerPort: 30080
        hostPort: 8080
        protocol: TCP
      - containerPort: 30180
        hostPort: 8180
        protocol: TCP
      - containerPort: 30389
        hostPort: 30389
        protocol: TCP
EOF

        kind create cluster --config /tmp/kind-config.yaml
        rm -f /tmp/kind-config.yaml

        log_info "Kind cluster 建立完成"
    fi

    # 設定 kubectl context
    kubectl config use-context "kind-$CLUSTER_NAME"
}

# 建立 namespace
create_namespace() {
    log_info "建立 namespace: $NAMESPACE..."

    kubectl create namespace "$NAMESPACE" 2>/dev/null || true
    kubectl config set-context --current --namespace="$NAMESPACE"
}

# 部署基礎設施
deploy_infrastructure() {
    log_info "部署基礎設施..."

    # 建立 K8s 資源目錄
    mkdir -p "$K8S_DIR/base"

    # PostgreSQL
    log_info "部署 PostgreSQL..."
    cat <<EOF | kubectl apply -f -
apiVersion: v1
kind: PersistentVolumeClaim
metadata:
  name: postgres-pvc
  namespace: $NAMESPACE
spec:
  accessModes:
    - ReadWriteOnce
  resources:
    requests:
      storage: 1Gi
---
apiVersion: apps/v1
kind: Deployment
metadata:
  name: postgres
  namespace: $NAMESPACE
spec:
  replicas: 1
  selector:
    matchLabels:
      app: postgres
  template:
    metadata:
      labels:
        app: postgres
    spec:
      containers:
        - name: postgres
          image: postgres:15-alpine
          ports:
            - containerPort: 5432
          env:
            - name: POSTGRES_DB
              value: keycloak
            - name: POSTGRES_USER
              value: keycloak
            - name: POSTGRES_PASSWORD
              value: keycloak123
          volumeMounts:
            - name: postgres-storage
              mountPath: /var/lib/postgresql/data
      volumes:
        - name: postgres-storage
          persistentVolumeClaim:
            claimName: postgres-pvc
---
apiVersion: v1
kind: Service
metadata:
  name: postgres
  namespace: $NAMESPACE
spec:
  selector:
    app: postgres
  ports:
    - port: 5432
      targetPort: 5432
EOF

    # OpenLDAP
    log_info "部署 OpenLDAP..."
    cat <<EOF | kubectl apply -f -
apiVersion: apps/v1
kind: Deployment
metadata:
  name: openldap
  namespace: $NAMESPACE
spec:
  replicas: 1
  selector:
    matchLabels:
      app: openldap
  template:
    metadata:
      labels:
        app: openldap
    spec:
      containers:
        - name: openldap
          image: osixia/openldap:1.5.0
          ports:
            - containerPort: 389
            - containerPort: 636
          env:
            - name: LDAP_ORGANISATION
              value: "E-Commerce Corp"
            - name: LDAP_DOMAIN
              value: "ecommerce.local"
            - name: LDAP_BASE_DN
              value: "dc=ecommerce,dc=local"
            - name: LDAP_ADMIN_PASSWORD
              value: admin123
            - name: LDAP_TLS
              value: "false"
---
apiVersion: v1
kind: Service
metadata:
  name: openldap
  namespace: $NAMESPACE
spec:
  selector:
    app: openldap
  ports:
    - name: ldap
      port: 389
      targetPort: 389
    - name: ldaps
      port: 636
      targetPort: 636
  type: NodePort
EOF

    # Keycloak
    log_info "部署 Keycloak..."
    cat <<EOF | kubectl apply -f -
apiVersion: apps/v1
kind: Deployment
metadata:
  name: keycloak
  namespace: $NAMESPACE
spec:
  replicas: 1
  selector:
    matchLabels:
      app: keycloak
  template:
    metadata:
      labels:
        app: keycloak
    spec:
      containers:
        - name: keycloak
          image: quay.io/keycloak/keycloak:24.0
          args: ["start-dev"]
          ports:
            - containerPort: 8080
          env:
            - name: KC_DB
              value: postgres
            - name: KC_DB_URL
              value: jdbc:postgresql://postgres:5432/keycloak
            - name: KC_DB_USERNAME
              value: keycloak
            - name: KC_DB_PASSWORD
              value: keycloak123
            - name: KEYCLOAK_ADMIN
              value: admin
            - name: KEYCLOAK_ADMIN_PASSWORD
              value: admin123
            - name: KC_HTTP_ENABLED
              value: "true"
            - name: KC_HEALTH_ENABLED
              value: "true"
          readinessProbe:
            httpGet:
              path: /health/ready
              port: 8080
            initialDelaySeconds: 60
            periodSeconds: 10
---
apiVersion: v1
kind: Service
metadata:
  name: keycloak
  namespace: $NAMESPACE
spec:
  selector:
    app: keycloak
  ports:
    - port: 8080
      targetPort: 8080
      nodePort: 30180
  type: NodePort
EOF

    # 等待部署就緒
    log_info "等待部署就緒..."

    kubectl wait --for=condition=available deployment/postgres -n "$NAMESPACE" --timeout=120s || true
    kubectl wait --for=condition=available deployment/openldap -n "$NAMESPACE" --timeout=120s || true
    kubectl wait --for=condition=available deployment/keycloak -n "$NAMESPACE" --timeout=300s || true

    # 導入 LDAP 用戶
    log_info "導入 LDAP 測試用戶..."
    sleep 10

    LDAP_POD=$(kubectl get pods -n "$NAMESPACE" -l app=openldap -o jsonpath='{.items[0].metadata.name}')
    if [ -n "$LDAP_POD" ]; then
        cat "$PROJECT_ROOT/infra/ldap/bootstrap.ldif" | kubectl exec -i "$LDAP_POD" -n "$NAMESPACE" -- ldapadd -x -D "cn=admin,dc=ecommerce,dc=local" -w admin123 2>/dev/null || true
    fi
}

# 停止服務
stop_services() {
    log_info "停止 Kubernetes 服務..."

    kubectl delete namespace "$NAMESPACE" 2>/dev/null || true

    log_info "服務已停止"
}

# 刪除 cluster
delete_cluster() {
    log_info "刪除 Kind cluster..."

    kind delete cluster --name "$CLUSTER_NAME" 2>/dev/null || true

    log_info "Cluster 已刪除"
}

# 顯示服務狀態
show_status() {
    echo -e "\n${BLUE}=== Kind Cluster 狀態 ===${NC}"
    kind get clusters 2>/dev/null || echo "無 Kind clusters"

    echo -e "\n${BLUE}=== Namespace: $NAMESPACE ===${NC}"
    kubectl get all -n "$NAMESPACE" 2>/dev/null || echo "Namespace 不存在或無資源"

    echo -e "\n${BLUE}=== 存取資訊 ===${NC}"
    KEYCLOAK_PORT=$(kubectl get svc keycloak -n "$NAMESPACE" -o jsonpath='{.spec.ports[0].nodePort}' 2>/dev/null || echo "N/A")
    echo "Keycloak Admin:    http://localhost:$KEYCLOAK_PORT (admin / admin123)"
    echo ""
    echo "使用 kubectl port-forward 存取其他服務:"
    echo "  kubectl port-forward svc/openldap 389:389 -n $NAMESPACE"
    echo "  kubectl port-forward svc/postgres 5432:5432 -n $NAMESPACE"
}

# 顯示日誌
show_logs() {
    local service=$1

    if [ -z "$service" ]; then
        log_info "可用服務: postgres, openldap, keycloak"
        return
    fi

    kubectl logs -f -l app="$service" -n "$NAMESPACE"
}

# Port forward
port_forward() {
    log_info "設定 port forwarding..."

    # 背景執行 port-forward
    kubectl port-forward svc/keycloak 8180:8080 -n "$NAMESPACE" &
    kubectl port-forward svc/openldap 389:389 -n "$NAMESPACE" &
    kubectl port-forward svc/postgres 5432:5432 -n "$NAMESPACE" &

    log_info "Port forwarding 已啟動"
    log_info "Keycloak: http://localhost:8180"
    log_info "OpenLDAP: localhost:389"
    log_info "PostgreSQL: localhost:5432"
    log_info ""
    log_info "按 Ctrl+C 停止"

    wait
}

# 顯示使用說明
show_usage() {
    echo "使用方式: $0 [command]"
    echo ""
    echo "Commands:"
    echo "  start           建立 cluster 並部署所有服務 (預設)"
    echo "  stop            停止所有服務 (保留 cluster)"
    echo "  delete          刪除 Kind cluster"
    echo "  status          顯示服務狀態"
    echo "  logs <service>  顯示服務日誌"
    echo "  port-forward    啟動 port forwarding"
    echo ""
    echo "Services: postgres, openldap, keycloak"
}

# ==============================================================================
# 主程式
# ==============================================================================

main() {
    print_banner

    case "${1:-start}" in
        start)
            check_prerequisites
            create_cluster
            create_namespace
            deploy_infrastructure
            echo ""
            show_status
            ;;
        stop)
            stop_services
            ;;
        delete)
            stop_services
            delete_cluster
            ;;
        status)
            show_status
            ;;
        logs)
            show_logs "$2"
            ;;
        port-forward|pf)
            port_forward
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
