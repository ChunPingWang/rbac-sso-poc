# Kubernetes 架構設計與管理

本文件說明 RBAC-SSO-POC 專案在 Kubernetes 環境的架構設計、部署管理與安全配置。

## 目錄

- [1. K8s 架構概述](#1-k8s-架構概述)
- [2. 資源配置](#2-資源配置)
- [3. 網路架構](#3-網路架構)
- [4. 憑證管理 (cert-manager)](#4-憑證管理-cert-manager)
- [5. 部署管理](#5-部署管理)
- [6. 監控與日誌](#6-監控與日誌)
- [7. 故障排除](#7-故障排除)

---

## 1. K8s 架構概述

### 1.1 整體架構圖

```
┌────────────────────────────────────────────────────────────────────────────────────────┐
│                              Kubernetes Cluster (Kind)                                  │
│                                                                                        │
│  ┌──────────────────────────────────────────────────────────────────────────────────┐ │
│  │  Namespace: rbac-sso                                                              │ │
│  │                                                                                   │ │
│  │  ┌─────────────────────────────────────────────────────────────────────────────┐ │ │
│  │  │  Ingress / NodePort                                                         │ │ │
│  │  │  ┌───────────────┐  ┌───────────────┐  ┌───────────────┐                   │ │ │
│  │  │  │ :30080        │  │ :30180        │  │ :30389        │                   │ │ │
│  │  │  │ Gateway       │  │ Keycloak      │  │ LDAP          │                   │ │ │
│  │  │  └───────┬───────┘  └───────┬───────┘  └───────┬───────┘                   │ │ │
│  │  └──────────┼──────────────────┼──────────────────┼────────────────────────────┘ │ │
│  │             │                  │                  │                              │ │
│  │  ┌──────────▼──────────────────▼──────────────────▼────────────────────────────┐ │ │
│  │  │  Services (ClusterIP)                                                       │ │ │
│  │  │  ┌───────────────┐  ┌───────────────┐  ┌───────────────┐  ┌─────────────┐  │ │ │
│  │  │  │ gateway       │  │ keycloak      │  │ openldap      │  │ postgresql  │  │ │ │
│  │  │  │ :8080         │  │ :8180         │  │ :389/636      │  │ :5432       │  │ │ │
│  │  │  └───────┬───────┘  └───────┬───────┘  └───────┬───────┘  └──────┬──────┘  │ │ │
│  │  └──────────┼──────────────────┼──────────────────┼─────────────────┼──────────┘ │ │
│  │             │                  │                  │                 │            │ │
│  │  ┌──────────▼──────────────────▼──────────────────▼─────────────────▼──────────┐ │ │
│  │  │  Deployments / StatefulSets                                                 │ │ │
│  │  │                                                                             │ │ │
│  │  │  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐  ┌─────────────────────┐ │ │ │
│  │  │  │ Gateway     │  │ Product     │  │ User        │  │ Keycloak            │ │ │ │
│  │  │  │ Pod         │  │ Service Pod │  │ Service Pod │  │ Pod                 │ │ │ │
│  │  │  │             │  │             │  │             │  │                     │ │ │ │
│  │  │  │ ┌─────────┐ │  │ ┌─────────┐ │  │ ┌─────────┐ │  │ ┌─────────────────┐ │ │ │ │
│  │  │  │ │Container│ │  │ │Container│ │  │ │Container│ │  │ │ Container       │ │ │ │ │
│  │  │  │ │:8080    │ │  │ │:8081    │ │  │ │:8082    │ │  │ │ :8180           │ │ │ │ │
│  │  │  │ │:8090    │ │  │ │:8091    │ │  │ │:8092    │ │  │ │                 │ │ │ │ │
│  │  │  │ │(mgmt)   │ │  │ │(mgmt)   │ │  │ │(mgmt)   │ │  │ └─────────────────┘ │ │ │ │
│  │  │  │ └─────────┘ │  │ └─────────┘ │  │ └─────────┘ │  │                     │ │ │ │
│  │  │  └─────────────┘  └─────────────┘  └─────────────┘  └─────────────────────┘ │ │ │
│  │  │                                                                             │ │ │
│  │  │  ┌─────────────┐  ┌─────────────┐                                          │ │ │
│  │  │  │ OpenLDAP    │  │ PostgreSQL  │                                          │ │ │
│  │  │  │ Pod         │  │ Pod         │                                          │ │ │
│  │  │  │ (StatefulSet)│ │ (StatefulSet)│                                         │ │ │
│  │  │  └─────────────┘  └─────────────┘                                          │ │ │
│  │  └─────────────────────────────────────────────────────────────────────────────┘ │ │
│  │                                                                                   │ │
│  │  ┌─────────────────────────────────────────────────────────────────────────────┐ │ │
│  │  │  Storage (PersistentVolumeClaims)                                           │ │ │
│  │  │  ┌───────────────┐  ┌───────────────┐  ┌───────────────┐                   │ │ │
│  │  │  │ keycloak-pvc  │  │ postgres-pvc  │  │ ldap-pvc      │                   │ │ │
│  │  │  │ 1Gi           │  │ 1Gi           │  │ 256Mi         │                   │ │ │
│  │  │  └───────────────┘  └───────────────┘  └───────────────┘                   │ │ │
│  │  └─────────────────────────────────────────────────────────────────────────────┘ │ │
│  └───────────────────────────────────────────────────────────────────────────────────┘ │
│                                                                                        │
│  ┌──────────────────────────────────────────────────────────────────────────────────┐ │
│  │  Namespace: cert-manager                                                          │ │
│  │  ┌─────────────────┐  ┌─────────────────┐  ┌─────────────────┐                   │ │
│  │  │ cert-manager    │  │ cert-manager    │  │ cert-manager    │                   │ │
│  │  │ controller      │  │ webhook         │  │ cainjector      │                   │ │
│  │  └─────────────────┘  └─────────────────┘  └─────────────────┘                   │ │
│  └──────────────────────────────────────────────────────────────────────────────────┘ │
└────────────────────────────────────────────────────────────────────────────────────────┘
```

### 1.2 資源清單

| 資源類型 | 名稱 | Namespace | 說明 |
|----------|------|-----------|------|
| Namespace | rbac-sso | - | 應用程式命名空間 |
| Namespace | cert-manager | - | 憑證管理命名空間 |
| Deployment | gateway | rbac-sso | API 閘道 |
| Deployment | product-service | rbac-sso | 商品服務 |
| Deployment | user-service | rbac-sso | 使用者服務 |
| Deployment | keycloak | rbac-sso | 認證服務 |
| StatefulSet | postgresql | rbac-sso | Keycloak 資料庫 |
| StatefulSet | openldap | rbac-sso | LDAP 使用者目錄 |
| Service | gateway | rbac-sso | ClusterIP + NodePort |
| Service | product-service | rbac-sso | ClusterIP |
| Service | user-service | rbac-sso | ClusterIP |
| Service | keycloak | rbac-sso | ClusterIP + NodePort |
| ConfigMap | rbac-config | rbac-sso | 應用程式配置 |
| Secret | rbac-secrets | rbac-sso | 敏感資料 |
| PVC | keycloak-pvc | rbac-sso | Keycloak 儲存 |
| PVC | postgres-pvc | rbac-sso | PostgreSQL 儲存 |
| PVC | ldap-pvc | rbac-sso | LDAP 儲存 |

---

## 2. 資源配置

### 2.1 Deployment 配置範例

```yaml
# deploy/k8s/services/product-service.yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: product-service
  namespace: rbac-sso
  labels:
    app: product-service
    tier: application
spec:
  replicas: 1
  selector:
    matchLabels:
      app: product-service
  template:
    metadata:
      labels:
        app: product-service
        tier: application
    spec:
      serviceAccountName: product-service
      containers:
        - name: product-service
          image: rbac-product-service:latest
          imagePullPolicy: IfNotPresent
          ports:
            - containerPort: 8081
              name: http
            - containerPort: 8091
              name: management
          env:
            - name: SPRING_PROFILES_ACTIVE
              value: "k8s"
            - name: SERVER_PORT
              value: "8081"
            - name: KEYCLOAK_ISSUER_URI
              valueFrom:
                configMapKeyRef:
                  name: rbac-config
                  key: KEYCLOAK_ISSUER_URI
          # 資源限制
          resources:
            requests:
              memory: "256Mi"
              cpu: "100m"
            limits:
              memory: "512Mi"
              cpu: "500m"
          # 健康檢查
          livenessProbe:
            httpGet:
              path: /actuator/health/liveness
              port: 8091
            initialDelaySeconds: 90
            periodSeconds: 10
            failureThreshold: 3
          readinessProbe:
            httpGet:
              path: /actuator/health/readiness
              port: 8091
            initialDelaySeconds: 45
            periodSeconds: 5
            failureThreshold: 3
```

### 2.2 Service 配置

```yaml
# ClusterIP Service (內部通訊)
apiVersion: v1
kind: Service
metadata:
  name: product-service
  namespace: rbac-sso
spec:
  type: ClusterIP
  ports:
    - port: 8081
      targetPort: 8081
      name: http
  selector:
    app: product-service

---
# NodePort Service (外部存取)
apiVersion: v1
kind: Service
metadata:
  name: gateway-nodeport
  namespace: rbac-sso
spec:
  type: NodePort
  ports:
    - port: 8080
      targetPort: 8080
      nodePort: 30080
      name: http
  selector:
    app: gateway
```

### 2.3 ConfigMap 配置

```yaml
apiVersion: v1
kind: ConfigMap
metadata:
  name: rbac-config
  namespace: rbac-sso
data:
  KEYCLOAK_ISSUER_URI: "http://keycloak.rbac-sso.svc.cluster.local:8180/realms/rbac-sso-realm"
  SPRING_PROFILES_ACTIVE: "k8s"
  LOG_LEVEL: "INFO"
```

### 2.4 Secret 配置

```yaml
apiVersion: v1
kind: Secret
metadata:
  name: rbac-secrets
  namespace: rbac-sso
type: Opaque
stringData:
  KEYCLOAK_ADMIN_PASSWORD: admin
  POSTGRES_PASSWORD: postgres
  LDAP_ADMIN_PASSWORD: admin
```

---

## 3. 網路架構

### 3.1 服務發現

```
┌────────────────────────────────────────────────────────────────────────────────────────┐
│                              K8s DNS 服務發現                                           │
│                                                                                        │
│  ┌──────────────────────────────────────────────────────────────────────────────────┐ │
│  │  ClusterIP Service DNS 名稱                                                       │ │
│  │                                                                                   │ │
│  │  服務名稱               │ DNS 名稱                                                │ │
│  │  ──────────────────────┼─────────────────────────────────────────────────────── │ │
│  │  gateway               │ gateway.rbac-sso.svc.cluster.local                     │ │
│  │  product-service       │ product-service.rbac-sso.svc.cluster.local             │ │
│  │  user-service          │ user-service.rbac-sso.svc.cluster.local                │ │
│  │  keycloak              │ keycloak.rbac-sso.svc.cluster.local                    │ │
│  │  postgresql            │ postgresql.rbac-sso.svc.cluster.local                  │ │
│  │  openldap              │ openldap.rbac-sso.svc.cluster.local                    │ │
│  └──────────────────────────────────────────────────────────────────────────────────┘ │
│                                                                                        │
│  使用方式 (application-k8s.yml):                                                        │
│  ┌──────────────────────────────────────────────────────────────────────────────────┐ │
│  │  spring:                                                                          │ │
│  │    cloud:                                                                         │ │
│  │      gateway:                                                                     │ │
│  │        routes:                                                                    │ │
│  │          - id: product-service                                                    │ │
│  │            uri: http://product-service.rbac-sso.svc.cluster.local:8081           │ │
│  │            predicates:                                                            │ │
│  │              - Path=/api/products/**                                              │ │
│  └──────────────────────────────────────────────────────────────────────────────────┘ │
└────────────────────────────────────────────────────────────────────────────────────────┘
```

### 3.2 端口映射

| 服務 | 內部端口 | NodePort | 說明 |
|------|----------|----------|------|
| Gateway | 8080 | 30080 | API 閘道 |
| Gateway (mgmt) | 8090 | - | 管理端口 |
| Product Service | 8081 | - | 商品服務 |
| Product Service (mgmt) | 8091 | - | 管理端口 |
| User Service | 8082 | - | 使用者服務 |
| User Service (mgmt) | 8092 | - | 管理端口 |
| Keycloak | 8180 | 30180 | 認證服務 |
| PostgreSQL | 5432 | - | 資料庫 |
| OpenLDAP | 389 | 30389 | LDAP |
| OpenLDAP (SSL) | 636 | - | LDAPS |

### 3.3 Network Policy (可選)

```yaml
# 只允許 Gateway 存取後端服務
apiVersion: networking.k8s.io/v1
kind: NetworkPolicy
metadata:
  name: product-service-policy
  namespace: rbac-sso
spec:
  podSelector:
    matchLabels:
      app: product-service
  policyTypes:
    - Ingress
  ingress:
    - from:
        - podSelector:
            matchLabels:
              app: gateway
      ports:
        - protocol: TCP
          port: 8081
    # 允許 Prometheus 抓取 metrics
    - from:
        - namespaceSelector:
            matchLabels:
              name: monitoring
      ports:
        - protocol: TCP
          port: 8091
```

---

## 4. 憑證管理 (cert-manager)

### 4.1 cert-manager 架構

```
┌────────────────────────────────────────────────────────────────────────────────────────┐
│                              cert-manager 架構                                          │
│                                                                                        │
│  1. Issuer 層級                                                                        │
│  ┌──────────────────────────────────────────────────────────────────────────────────┐ │
│  │                                                                                   │ │
│  │  ┌─────────────────────┐           ┌─────────────────────┐                       │ │
│  │  │  SelfSigned         │──────────▶│  rbac-sso-ca        │                       │ │
│  │  │  ClusterIssuer      │  簽發     │  (CA Certificate)   │                       │ │
│  │  │  (selfsigned-issuer)│           │  在 cert-manager NS │                       │ │
│  │  └─────────────────────┘           └──────────┬──────────┘                       │ │
│  │                                               │                                   │ │
│  │                                               │ 建立                              │ │
│  │                                               ▼                                   │ │
│  │                                    ┌─────────────────────┐                       │ │
│  │                                    │  CA ClusterIssuer   │                       │ │
│  │                                    │  (rbac-sso-ca-      │                       │ │
│  │                                    │   issuer)           │                       │ │
│  │                                    └──────────┬──────────┘                       │ │
│  └───────────────────────────────────────────────┼───────────────────────────────────┘ │
│                                                  │                                     │
│  2. Certificate 層級                              │ 簽發                                │
│  ┌───────────────────────────────────────────────▼───────────────────────────────────┐ │
│  │                                                                                   │ │
│  │  ┌─────────────────────┐  ┌─────────────────────┐  ┌─────────────────────┐       │ │
│  │  │ gateway-tls         │  │ product-service-tls │  │ user-service-tls    │       │ │
│  │  │ Certificate         │  │ Certificate         │  │ Certificate         │       │ │
│  │  └──────────┬──────────┘  └──────────┬──────────┘  └──────────┬──────────┘       │ │
│  │             │                        │                        │                   │ │
│  │             ▼                        ▼                        ▼                   │ │
│  │  ┌─────────────────────┐  ┌─────────────────────┐  ┌─────────────────────┐       │ │
│  │  │ gateway-tls-secret  │  │ product-service-    │  │ user-service-       │       │ │
│  │  │ (K8s Secret)        │  │ tls-secret          │  │ tls-secret          │       │ │
│  │  │                     │  │ (K8s Secret)        │  │ (K8s Secret)        │       │ │
│  │  │ - tls.crt           │  │ - tls.crt           │  │ - tls.crt           │       │ │
│  │  │ - tls.key           │  │ - tls.key           │  │ - tls.key           │       │ │
│  │  │ - ca.crt            │  │ - ca.crt            │  │ - ca.crt            │       │ │
│  │  └─────────────────────┘  └─────────────────────┘  └─────────────────────┘       │ │
│  └───────────────────────────────────────────────────────────────────────────────────┘ │
│                                                                                        │
│  3. Pod 掛載                                                                           │
│  ┌───────────────────────────────────────────────────────────────────────────────────┐ │
│  │                                                                                   │ │
│  │  Pod (product-service)                                                            │ │
│  │  ┌─────────────────────────────────────────────────────────────────────────────┐ │ │
│  │  │  volumeMounts:                                                               │ │ │
│  │  │    - name: tls-certs                                                         │ │ │
│  │  │      mountPath: /etc/ssl/certs                                               │ │ │
│  │  │      readOnly: true                                                          │ │ │
│  │  │                                                                              │ │ │
│  │  │  /etc/ssl/certs/                                                             │ │ │
│  │  │  ├── tls.crt  ← 服務憑證                                                     │ │ │
│  │  │  ├── tls.key  ← 私鑰                                                         │ │ │
│  │  │  └── ca.crt   ← CA 憑證 (驗證其他服務)                                        │ │ │
│  │  └─────────────────────────────────────────────────────────────────────────────┘ │ │
│  └───────────────────────────────────────────────────────────────────────────────────┘ │
└────────────────────────────────────────────────────────────────────────────────────────┘
```

### 4.2 CA Issuer 配置

```yaml
# deploy/k8s/security/cert-manager/ca-issuer.yaml

# 1. Self-Signed Issuer
apiVersion: cert-manager.io/v1
kind: ClusterIssuer
metadata:
  name: selfsigned-issuer
spec:
  selfSigned: {}

---
# 2. CA Certificate
apiVersion: cert-manager.io/v1
kind: Certificate
metadata:
  name: rbac-sso-ca
  namespace: cert-manager
spec:
  isCA: true
  commonName: rbac-sso-ca
  secretName: rbac-sso-ca-secret
  duration: 87600h  # 10 years
  renewBefore: 720h # 30 days
  privateKey:
    algorithm: ECDSA
    size: 256
  issuerRef:
    name: selfsigned-issuer
    kind: ClusterIssuer
  subject:
    organizations:
      - RBAC-SSO-POC
    organizationalUnits:
      - Platform Security

---
# 3. CA Issuer
apiVersion: cert-manager.io/v1
kind: ClusterIssuer
metadata:
  name: rbac-sso-ca-issuer
spec:
  ca:
    secretName: rbac-sso-ca-secret
```

### 4.3 Service Certificate 配置

```yaml
# deploy/k8s/security/cert-manager/service-certificates.yaml

apiVersion: cert-manager.io/v1
kind: Certificate
metadata:
  name: product-service-tls
  namespace: rbac-sso
spec:
  secretName: product-service-tls-secret
  duration: 8760h    # 1 year
  renewBefore: 720h  # 30 days before expiry
  commonName: product-service
  privateKey:
    algorithm: ECDSA
    size: 256
  usages:
    - server auth
    - client auth
  dnsNames:
    - product-service
    - product-service.rbac-sso
    - product-service.rbac-sso.svc
    - product-service.rbac-sso.svc.cluster.local
  issuerRef:
    name: rbac-sso-ca-issuer
    kind: ClusterIssuer
```

### 4.4 查看憑證狀態

```bash
# 查看所有憑證
kubectl get certificates -n rbac-sso

# 輸出範例
NAME                   READY   SECRET                        AGE
gateway-tls            True    gateway-tls-secret            1h
product-service-tls    True    product-service-tls-secret    1h
user-service-tls       True    user-service-tls-secret       1h

# 查看憑證詳細資訊
kubectl describe certificate product-service-tls -n rbac-sso

# 查看 Secret 內容
kubectl get secret product-service-tls-secret -n rbac-sso -o yaml
```

---

## 5. 部署管理

### 5.1 部署腳本

```bash
#!/bin/bash
# deploy/scripts/k8s-deploy.sh

# 建立 Kind cluster
kind create cluster --name rbac-sso-cluster --config deploy/k8s/kind/kind-config.yaml

# 載入 Docker images
kind load docker-image rbac-gateway:latest --name rbac-sso-cluster
kind load docker-image rbac-product-service:latest --name rbac-sso-cluster
kind load docker-image rbac-user-service:latest --name rbac-sso-cluster

# 建立 namespace
kubectl apply -f deploy/k8s/base/namespace.yaml

# 部署基礎設施
kubectl apply -f deploy/k8s/infrastructure/

# 等待基礎設施就緒
kubectl wait --for=condition=ready pod -l app=keycloak -n rbac-sso --timeout=300s

# 部署應用程式服務
kubectl apply -f deploy/k8s/services/
```

### 5.2 mTLS 部署腳本

```bash
#!/bin/bash
# deploy/scripts/k8s-mtls-deploy.sh

# 安裝 cert-manager
kubectl apply -f https://github.com/cert-manager/cert-manager/releases/download/v1.13.0/cert-manager.yaml

# 等待 cert-manager 就緒
kubectl wait --for=condition=ready pod -l app.kubernetes.io/component=controller -n cert-manager --timeout=120s

# 部署 CA Issuer
kubectl apply -f deploy/k8s/security/cert-manager/ca-issuer.yaml

# 等待 CA Certificate 就緒
sleep 10

# 部署服務憑證
kubectl apply -f deploy/k8s/security/cert-manager/service-certificates.yaml

# 等待所有憑證簽發完成
kubectl wait --for=condition=ready certificate --all -n rbac-sso --timeout=60s

# 部署 mTLS 服務
kubectl apply -f deploy/k8s/services-mtls/
```

### 5.3 滾動更新

```bash
# 更新 image
kubectl set image deployment/product-service \
  product-service=rbac-product-service:v2.0.0 \
  -n rbac-sso

# 查看更新狀態
kubectl rollout status deployment/product-service -n rbac-sso

# 回滾
kubectl rollout undo deployment/product-service -n rbac-sso

# 查看歷史版本
kubectl rollout history deployment/product-service -n rbac-sso
```

### 5.4 擴縮容

```bash
# 手動擴縮容
kubectl scale deployment product-service --replicas=3 -n rbac-sso

# HPA (Horizontal Pod Autoscaler)
kubectl autoscale deployment product-service \
  --min=2 --max=10 --cpu-percent=80 \
  -n rbac-sso
```

---

## 6. 監控與日誌

### 6.1 健康檢查端點

```yaml
# 每個服務都有獨立的管理端口
livenessProbe:
  httpGet:
    path: /actuator/health/liveness
    port: 8091  # 管理端口，不受 mTLS 影響
    scheme: HTTP
  initialDelaySeconds: 90
  periodSeconds: 10
  failureThreshold: 3

readinessProbe:
  httpGet:
    path: /actuator/health/readiness
    port: 8091
    scheme: HTTP
  initialDelaySeconds: 45
  periodSeconds: 5
  failureThreshold: 3
```

### 6.2 查看日誌

```bash
# 查看 Pod 日誌
kubectl logs -f deployment/product-service -n rbac-sso

# 查看所有 container 日誌
kubectl logs -f deployment/product-service -n rbac-sso --all-containers

# 查看之前的 container 日誌 (crash 後)
kubectl logs deployment/product-service -n rbac-sso --previous

# 使用 stern 查看多 Pod 日誌
stern product-service -n rbac-sso
```

### 6.3 Metrics 端點

```bash
# 查看服務 metrics
kubectl port-forward svc/product-service 8091:8091 -n rbac-sso

curl http://localhost:8091/actuator/prometheus
```

### 6.4 整合測試

```bash
# 執行整合測試
./deploy/scripts/k8s-integration-test.sh

# 測試項目包含:
# - Cluster 存在檢查
# - Namespace 檢查
# - ConfigMap/Secret 檢查
# - PVC 狀態檢查
# - Pod 運行狀態
# - Service 狀態
# - 健康檢查端點
# - API 認證測試
```

---

## 7. 故障排除

### 7.1 常見問題

#### Pod CrashLoopBackOff

```bash
# 查看 Pod 事件
kubectl describe pod <pod-name> -n rbac-sso

# 查看日誌
kubectl logs <pod-name> -n rbac-sso --previous

# 常見原因:
# - OOM (記憶體不足) - 增加 memory limits
# - 健康檢查失敗 - 調整 initialDelaySeconds
# - 設定錯誤 - 檢查 ConfigMap/Secret
```

#### ImagePullBackOff

```bash
# 確認 image 已載入到 Kind
kind load docker-image <image-name> --name rbac-sso-cluster

# 或檢查 imagePullPolicy
# imagePullPolicy: IfNotPresent (for local images)
```

#### Service 無法連線

```bash
# 檢查 Service 端點
kubectl get endpoints <service-name> -n rbac-sso

# 測試 Pod 間連線
kubectl exec -it <pod-name> -n rbac-sso -- \
  curl http://product-service:8081/actuator/health

# 檢查 DNS 解析
kubectl exec -it <pod-name> -n rbac-sso -- \
  nslookup product-service.rbac-sso.svc.cluster.local
```

#### mTLS 連線問題

```bash
# 檢查憑證是否簽發成功
kubectl get certificates -n rbac-sso

# 檢查 Secret 內容
kubectl get secret product-service-tls-secret -n rbac-sso -o yaml

# 查看 SSL 相關日誌
kubectl logs deployment/product-service -n rbac-sso | grep -i ssl

# 驗證憑證
kubectl exec -it deployment/product-service -n rbac-sso -- \
  openssl x509 -in /etc/ssl/certs/tls.crt -text -noout
```

### 7.2 除錯指令速查

```bash
# 查看所有資源狀態
kubectl get all -n rbac-sso

# 查看 Pod 詳細資訊
kubectl describe pod <pod-name> -n rbac-sso

# 進入 Pod 除錯
kubectl exec -it <pod-name> -n rbac-sso -- /bin/sh

# 查看事件 (依時間排序)
kubectl get events -n rbac-sso --sort-by='.lastTimestamp'

# 查看資源使用量
kubectl top pods -n rbac-sso

# 查看節點狀態
kubectl get nodes -o wide
```

### 7.3 重啟服務

```bash
# 重啟 Deployment
kubectl rollout restart deployment/product-service -n rbac-sso

# 刪除 Pod (會自動重建)
kubectl delete pod <pod-name> -n rbac-sso

# 重新部署
kubectl delete -f deploy/k8s/services/product-service.yaml
kubectl apply -f deploy/k8s/services/product-service.yaml
```

---

## 相關文件

- [專案架構與資安設計](../architecture/ARCHITECTURE_AND_SECURITY.md)
- [Spring Cloud Gateway 教學](../tutorials/SPRING_CLOUD_GATEWAY_TUTORIAL.md)
- [資安原理與配置](../security/SECURITY_PRINCIPLES_AND_CONFIGURATION.md)
