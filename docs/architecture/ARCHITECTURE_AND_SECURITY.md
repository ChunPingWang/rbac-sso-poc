# 專案架構與資安設計文件

本文件說明 RBAC-SSO-POC 多租戶電子商務平台的整體架構與資安設計。

## 目錄

- [1. 系統架構概述](#1-系統架構概述)
- [2. 服務層級架構](#2-服務層級架構)
- [3. 安全架構設計](#3-安全架構設計)
- [4. 資料流與通訊安全](#4-資料流與通訊安全)
- [5. 憑證管理架構](#5-憑證管理架構)

---

## 1. 系統架構概述

### 1.1 整體架構圖

```
                                    ┌─────────────────────────────────────────────────────────────┐
                                    │                      Client Layer                            │
                                    │                                                             │
                                    │    ┌──────────────┐          ┌──────────────┐              │
                                    │    │  Web Browser │          │  Mobile App  │              │
                                    │    └──────┬───────┘          └──────┬───────┘              │
                                    │           │                         │                       │
                                    └───────────┼─────────────────────────┼───────────────────────┘
                                                │ HTTPS (TLS 1.3)         │
                                    ┌───────────▼─────────────────────────▼───────────────────────┐
                                    │                   API Gateway Layer                          │
   ┌──────────────────────┐         │  ┌─────────────────────────────────────────────────────┐   │
   │                      │         │  │              Spring Cloud Gateway                     │   │
   │      Keycloak        │◄────────┼──┤              (Port 8080)                             │   │
   │    (OAuth2/OIDC)     │ OAuth2  │  │                                                       │   │
   │     Port 8180        │         │  │  - JWT Token 驗證                                    │   │
   │                      │         │  │  - 路由轉發                                          │   │
   │  ┌───────────────┐   │         │  │  - CORS 處理                                         │   │
   │  │  LDAP Server  │   │         │  │  - Rate Limiting (可選)                              │   │
   │  │   (Port 389)  │   │         │  └────────────────────┬────────────────────────────────┘   │
   │  └───────────────┘   │         │                       │ mTLS                               │
   └──────────────────────┘         └───────────────────────┼─────────────────────────────────────┘
                                                            │
                                    ┌───────────────────────▼─────────────────────────────────────┐
                                    │                 Microservices Layer                          │
                                    │                                                             │
                                    │   ┌───────────────────────┐   ┌───────────────────────┐    │
                                    │   │    Product Service    │   │     User Service      │    │
                                    │   │      (Port 8081)      │   │     (Port 8082)       │    │
                                    │   │                       │   │                       │    │
                                    │   │  - Hexagonal Arch     │◄──┤  - JWT Profile        │    │
                                    │   │  - DDD / CQRS         │   │  - Tenant Context     │    │
                                    │   │  - Domain Events      │   │                       │    │
                                    │   │  - 稽核日誌            │   │                       │    │
                                    │   └───────────┬───────────┘   └───────────┬───────────┘    │
                                    │               │                           │                 │
                                    └───────────────┼───────────────────────────┼─────────────────┘
                                                    │                           │
                                    ┌───────────────▼───────────────────────────▼─────────────────┐
                                    │                     Data Layer                               │
                                    │   ┌───────────────────────┐   ┌───────────────────────┐    │
                                    │   │     PostgreSQL/H2     │   │     Audit Logs DB     │    │
                                    │   │    (Products Data)    │   │   (稽核記錄儲存)       │    │
                                    │   └───────────────────────┘   └───────────────────────┘    │
                                    └─────────────────────────────────────────────────────────────┘
```

### 1.2 核心設計原則

| 原則 | 說明 | 實作方式 |
|------|------|----------|
| **縱深防禦** | 多層安全控制，單一防線失效不影響整體 | 南北向 + 東西向安全機制 |
| **最小權限** | 服務和用戶僅獲得完成任務所需的最小權限 | RBAC + 細粒度權限控制 |
| **零信任** | 不預設信任任何請求，始終驗證 | mTLS + JWT 雙重驗證 |
| **多租戶隔離** | 租戶資料嚴格隔離 | TenantContext + 查詢過濾 |
| **可稽核性** | 所有重要操作皆可追蹤 | AOP / Domain Event 稽核 |

---

## 2. 服務層級架構

### 2.1 微服務清單

| 服務名稱 | 端口 | 管理端口 | 職責 | 技術棧 |
|----------|------|----------|------|--------|
| Gateway Service | 8080 | 8090 | API 閘道、路由、認證 | Spring Cloud Gateway (WebFlux) |
| Product Service | 8081 | 8091 | 商品管理 CRUD | Spring Boot (Servlet) |
| User Service | 8082 | 8092 | 用戶資訊查詢 | Spring Boot (Servlet) |
| Keycloak | 8180 | - | OAuth2/OIDC 認證 | Keycloak 24.x |

### 2.2 共用函式庫 (Shared Libraries)

```
libs/
├── common-lib/      # 共用 DTO、Exception、ApiResponse
├── security-lib/    # OAuth2 Resource Server 配置
├── tenant-lib/      # TenantContext、TenantFilter
└── audit-lib/       # 稽核日誌 Domain + Infrastructure
    ├── domain/
    │   ├── AuditLog.java
    │   ├── AuditLogId.java
    │   ├── AuditEventType.java
    │   └── AuditResult.java
    ├── application/
    │   └── AuditQueryService.java
    └── infrastructure/
        ├── JpaAuditLogRepository.java
        ├── AuditableAspect.java (AOP)
        └── MtlsWebClientConfiguration.java
```

### 2.3 Hexagonal Architecture

Product Service 採用六角架構 (Ports & Adapters)：

```
┌──────────────────────────────────────────────────────────────────────────────────────────┐
│                                     ADAPTERS                                             │
│  ┌────────────────────────────────────┐    ┌────────────────────────────────────────┐   │
│  │          Inbound Adapters          │    │          Outbound Adapters              │   │
│  │  ┌──────────────────────────────┐  │    │  ┌──────────────────────────────────┐  │   │
│  │  │  ProductCommandController    │  │    │  │     JpaProductRepository         │  │   │
│  │  │  ProductQueryController      │  │    │  │     SpringDataProductRepository  │  │   │
│  │  └──────────────────────────────┘  │    │  └──────────────────────────────────┘  │   │
│  └─────────────────┬──────────────────┘    └───────────────────┬────────────────────┘   │
│                    │                                           ▲                         │
├────────────────────┼───────────────────────────────────────────┼─────────────────────────┤
│                    ▼                                           │                         │
│  ┌─────────────────────────────────────────────────────────────┴───────────────────────┐ │
│  │                              APPLICATION LAYER                                       │ │
│  │                                                                                      │ │
│  │   ProductCommandService                    ProductQueryService                       │ │
│  │   - handle(CreateProductCommand)           - handle(GetProductByIdQuery)            │ │
│  │   - handle(UpdateProductCommand)           - handle(ListProductsQuery)              │ │
│  │   - handle(DeleteProductCommand)                                                     │ │
│  │                                                                                      │ │
│  └──────────────────────────────────────┬───────────────────────────────────────────────┘ │
│                                         │                                                │
├─────────────────────────────────────────┼────────────────────────────────────────────────┤
│                                         ▼                                                │
│  ┌──────────────────────────────────────────────────────────────────────────────────────┐ │
│  │                                DOMAIN LAYER                                          │ │
│  │                                                                                      │ │
│  │   Product (Aggregate Root)         Value Objects           Domain Events             │ │
│  │   - create()                       - ProductId             - ProductCreated          │ │
│  │   - update()                       - ProductCode           - ProductUpdated          │ │
│  │   - delete()                       - Money                 - ProductDeleted          │ │
│  │   - changePrice()                  - ProductStatus         - ProductPriceChanged     │ │
│  │                                                                                      │ │
│  │   ProductRepository (Port/Interface)                                                 │ │
│  └──────────────────────────────────────────────────────────────────────────────────────┘ │
└──────────────────────────────────────────────────────────────────────────────────────────┘
```

---

## 3. 安全架構設計

### 3.1 安全層級劃分

```
┌─────────────────────────────────────────────────────────────────────────────────────────┐
│                               Security Architecture                                      │
│                                                                                         │
│  ┌───────────────────────────────────────────────────────────────────────────────────┐  │
│  │                            南北向安全 (North-South)                                │  │
│  │                                                                                   │  │
│  │   ┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐             │  │
│  │   │  OAuth2/OIDC    │    │   JWT Token     │    │     RBAC        │             │  │
│  │   │  認證流程        │───▶│   驗證機制       │───▶│   權限控制       │             │  │
│  │   │  (Keycloak)     │    │  (Spring Sec)   │    │ (@PreAuthorize) │             │  │
│  │   └─────────────────┘    └─────────────────┘    └─────────────────┘             │  │
│  │                                                                                   │  │
│  │   適用範圍：Client → API Gateway → Microservices                                  │  │
│  └───────────────────────────────────────────────────────────────────────────────────┘  │
│                                                                                         │
│  ┌───────────────────────────────────────────────────────────────────────────────────┐  │
│  │                            東西向安全 (East-West)                                  │  │
│  │                                                                                   │  │
│  │   ┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐             │  │
│  │   │  mTLS 雙向認證   │    │   cert-manager  │    │   TLS 1.3/1.2   │             │  │
│  │   │  服務身份驗證    │───▶│   憑證自動管理   │───▶│   傳輸加密       │             │  │
│  │   │  (Spring SSL)   │    │  (K8s Native)   │    │  (HTTPS Only)   │             │  │
│  │   └─────────────────┘    └─────────────────┘    └─────────────────┘             │  │
│  │                                                                                   │  │
│  │   適用範圍：Gateway ↔ Product Service ↔ User Service                              │  │
│  └───────────────────────────────────────────────────────────────────────────────────┘  │
│                                                                                         │
│  ┌───────────────────────────────────────────────────────────────────────────────────┐  │
│  │                            資料層安全 (Data Layer)                                 │  │
│  │                                                                                   │  │
│  │   ┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐             │  │
│  │   │   多租戶隔離     │    │    稽核日誌      │    │   敏感資料保護   │             │  │
│  │   │  TenantContext  │    │  AOP/DomainEvent│    │   (未來擴充)     │             │  │
│  │   │   查詢過濾       │    │   完整追蹤       │    │   加密 / 脫敏    │             │  │
│  │   └─────────────────┘    └─────────────────┘    └─────────────────┘             │  │
│  └───────────────────────────────────────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────────────────────────────────┘
```

### 3.2 認證授權流程

```
┌──────────┐     ┌──────────┐     ┌──────────┐     ┌──────────┐     ┌──────────┐
│  User    │     │  Browser │     │  Gateway │     │ Keycloak │     │ Service  │
└────┬─────┘     └────┬─────┘     └────┬─────┘     └────┬─────┘     └────┬─────┘
     │                │                │                │                │
     │  1. Access     │                │                │                │
     │  Protected     │                │                │                │
     │  Resource      │                │                │                │
     │───────────────▶│                │                │                │
     │                │  2. Request    │                │                │
     │                │  without Token │                │                │
     │                │───────────────▶│                │                │
     │                │                │                │                │
     │                │  3. 302 Redirect to Keycloak    │                │
     │                │◀───────────────│                │                │
     │                │                │                │                │
     │                │  4. Login Page │                │                │
     │                │───────────────────────────────▶│                │
     │                │                │                │                │
     │  5. Username/  │                │                │                │
     │  Password      │                │                │                │
     │───────────────▶│                │                │                │
     │                │  6. LDAP Bind Verify            │                │
     │                │───────────────────────────────▶│                │
     │                │                │                │                │
     │                │  7. JWT Token (roles, tenant_id)│                │
     │                │◀───────────────────────────────│                │
     │                │                │                │                │
     │                │  8. Request with Bearer Token  │                │
     │                │───────────────▶│                │                │
     │                │                │  9. Forward   │                │
     │                │                │  with JWT     │                │
     │                │                │──────────────────────────────▶│
     │                │                │                │                │
     │                │                │                │  10. Validate │
     │                │                │                │  JWT + RBAC   │
     │                │                │                │◀──────────────│
     │                │                │                │                │
     │                │  11. Response  │                │                │
     │                │◀───────────────│◀──────────────────────────────│
     │                │                │                │                │
```

### 3.3 RBAC 權限矩陣

| 角色 | 商品查詢 | 商品建立 | 商品更新 | 商品刪除 | 用戶查詢 | 用戶管理 |
|------|:--------:|:--------:|:--------:|:--------:|:--------:|:--------:|
| ADMIN | Yes | Yes | Yes | Yes | Yes | Yes |
| TENANT_ADMIN | Yes | Yes | Yes | No | Yes | No |
| USER | Yes | No | No | No | Yes | No |
| VIEWER | Yes | No | No | No | No | No |

### 3.4 多租戶隔離機制

```java
// TenantFilter - 從 JWT 擷取租戶資訊
@Component
public class TenantFilter extends OncePerRequestFilter {
    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) {
        String tenantId = extractTenantFromJwt(request);
        TenantContext.setCurrentTenant(tenantId);
        try {
            chain.doFilter(request, response);
        } finally {
            TenantContext.clear();
        }
    }
}

// 查詢時自動過濾租戶資料
public List<Product> findProducts() {
    String currentTenant = TenantContext.getCurrentTenant();
    if ("system".equals(currentTenant)) {
        return repository.findAll();  // 系統管理員可見所有
    }
    return repository.findByTenantId(currentTenant);  // 只能看自己租戶
}
```

---

## 4. 資料流與通訊安全

### 4.1 通訊協議配置

| 路徑 | 協議 | 加密 | 認證方式 |
|------|------|------|----------|
| Client → Gateway | HTTPS | TLS 1.3/1.2 | JWT Bearer Token |
| Gateway → Services | HTTPS | mTLS | Client Certificate + JWT |
| Service → Service | HTTPS | mTLS | Client Certificate |
| Service → Database | TCP | 可選加密 | Username/Password |
| Service → Keycloak | HTTPS | TLS | OAuth2 |

### 4.2 mTLS 握手流程

```
┌──────────────┐                              ┌──────────────┐
│   Gateway    │                              │   Product    │
│   Service    │                              │   Service    │
└──────┬───────┘                              └──────┬───────┘
       │                                             │
       │  1. ClientHello (TLS 1.3)                  │
       │────────────────────────────────────────────▶│
       │                                             │
       │  2. ServerHello + Server Certificate       │
       │◀────────────────────────────────────────────│
       │                                             │
       │  3. CertificateRequest                     │
       │◀────────────────────────────────────────────│
       │                                             │
       │  4. Client Certificate (gateway-tls-secret) │
       │────────────────────────────────────────────▶│
       │                                             │
       │  5. CertificateVerify                      │
       │────────────────────────────────────────────▶│
       │                                             │
       │         ┌─────────────────────┐            │
       │         │ 雙方驗證對方憑證是否  │            │
       │         │ 由同一 CA 簽發       │            │
       │         └─────────────────────┘            │
       │                                             │
       │  6. Encrypted Application Data (HTTPS)     │
       │◀───────────────────────────────────────────▶│
       │                                             │
```

### 4.3 憑證信任鏈

```
                    ┌─────────────────────────────────────────┐
                    │        Self-Signed Root CA              │
                    │        (rbac-sso-ca)                    │
                    │                                         │
                    │  CN: rbac-sso-ca                        │
                    │  O: RBAC-SSO-POC                        │
                    │  OU: Platform Security                  │
                    │  Validity: 10 years                     │
                    │  Algorithm: ECDSA P-256                 │
                    └─────────────────┬───────────────────────┘
                                      │
                                      │ Signs
                                      │
        ┌─────────────────────────────┼─────────────────────────────┐
        │                             │                             │
        ▼                             ▼                             ▼
┌───────────────────┐    ┌───────────────────┐    ┌───────────────────┐
│  gateway-tls      │    │ product-service   │    │  user-service     │
│                   │    │ -tls              │    │  -tls             │
│ CN: gateway       │    │ CN: product-svc   │    │ CN: user-svc      │
│ SAN:              │    │ SAN:              │    │ SAN:              │
│ - gateway         │    │ - product-service │    │ - user-service    │
│ - gateway.rbac-   │    │ - product-service │    │ - user-service.   │
│   sso.svc.cluster │    │   .rbac-sso.svc.  │    │   rbac-sso.svc.   │
│   .local          │    │   cluster.local   │    │   cluster.local   │
│ Validity: 1 year  │    │ Validity: 1 year  │    │ Validity: 1 year  │
│ renewBefore: 30d  │    │ renewBefore: 30d  │    │ renewBefore: 30d  │
└───────────────────┘    └───────────────────┘    └───────────────────┘
```

---

## 5. 憑證管理架構

### 5.1 cert-manager 工作流程

```
┌─────────────────────────────────────────────────────────────────────────────────────────┐
│                              cert-manager Workflow                                       │
│                                                                                         │
│   1. Certificate Resource 建立                                                          │
│   ┌──────────────────────────────────────────────────────────────────────────────────┐  │
│   │ apiVersion: cert-manager.io/v1                                                   │  │
│   │ kind: Certificate                                                                │  │
│   │ metadata:                                                                        │  │
│   │   name: product-service-tls                                                      │  │
│   │ spec:                                                                            │  │
│   │   secretName: product-service-tls-secret                                         │  │
│   │   issuerRef:                                                                     │  │
│   │     name: rbac-sso-ca-issuer                                                     │  │
│   └──────────────────────────────────────────────────────────────────────────────────┘  │
│                                      │                                                  │
│                                      ▼                                                  │
│   2. cert-manager Controller 處理                                                       │
│   ┌──────────────────────────────────────────────────────────────────────────────────┐  │
│   │  - 讀取 Certificate spec                                                         │  │
│   │  - 向 ClusterIssuer (rbac-sso-ca-issuer) 申請簽發                                 │  │
│   │  - CA Issuer 使用 rbac-sso-ca-secret 中的 CA 私鑰簽發                              │  │
│   └──────────────────────────────────────────────────────────────────────────────────┘  │
│                                      │                                                  │
│                                      ▼                                                  │
│   3. Secret 自動建立                                                                    │
│   ┌──────────────────────────────────────────────────────────────────────────────────┐  │
│   │  Secret: product-service-tls-secret                                              │  │
│   │  Data:                                                                           │  │
│   │    tls.crt: <signed certificate>                                                 │  │
│   │    tls.key: <private key>                                                        │  │
│   │    ca.crt:  <CA certificate>                                                     │  │
│   └──────────────────────────────────────────────────────────────────────────────────┘  │
│                                      │                                                  │
│                                      ▼                                                  │
│   4. Pod 掛載憑證                                                                       │
│   ┌──────────────────────────────────────────────────────────────────────────────────┐  │
│   │  volumes:                                                                        │  │
│   │    - name: tls-certs                                                             │  │
│   │      secret:                                                                     │  │
│   │        secretName: product-service-tls-secret                                    │  │
│   │  volumeMounts:                                                                   │  │
│   │    - name: tls-certs                                                             │  │
│   │      mountPath: /etc/ssl/certs                                                   │  │
│   │      readOnly: true                                                              │  │
│   └──────────────────────────────────────────────────────────────────────────────────┘  │
│                                                                                         │
│   5. 自動更新 (renewBefore: 720h = 30 days)                                             │
│   ┌──────────────────────────────────────────────────────────────────────────────────┐  │
│   │  - cert-manager 監控憑證到期時間                                                  │  │
│   │  - 到期前 30 天自動重新簽發                                                        │  │
│   │  - Secret 內容更新                                                               │  │
│   │  - Pod 需重啟以載入新憑證 (可配置 annotation checksum)                             │  │
│   └──────────────────────────────────────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────────────────────────────────┘
```

### 5.2 憑證檔案結構

```
/etc/ssl/certs/
├── tls.crt      # 服務憑證 (PEM 格式)
│                # -----BEGIN CERTIFICATE-----
│                # ...服務公開金鑰憑證...
│                # -----END CERTIFICATE-----
│
├── tls.key      # 服務私鑰 (PEM 格式)
│                # -----BEGIN EC PRIVATE KEY-----
│                # ...ECDSA P-256 私鑰...
│                # -----END EC PRIVATE KEY-----
│
└── ca.crt       # CA 憑證 (PEM 格式)
                 # -----BEGIN CERTIFICATE-----
                 # ...CA 公開金鑰憑證，用於驗證其他服務...
                 # -----END CERTIFICATE-----
```

### 5.3 安全控管狀態總覽

| 層級 | 控制項 | 狀態 | 說明 |
|------|--------|:----:|------|
| **南北向** | OAuth2/OIDC | Done | Keycloak 24.x 整合 |
| | JWT 驗證 | Done | Spring Security 6.x |
| | RBAC 權限控制 | Done | @PreAuthorize + SpEL |
| | CORS 配置 | Done | 白名單控制 |
| **東西向** | mTLS | Done | Spring Boot SSL Bundle |
| | 憑證管理 | Done | cert-manager 自動化 |
| | 憑證自動更新 | Done | renewBefore: 30d |
| **資料層** | 多租戶隔離 | Done | TenantContext |
| | 稽核日誌 | Done | AOP / Domain Event |
| | 資料加密 | Planned | DB-level encryption |

---

## 相關文件

- [Spring Cloud Gateway 教學](../tutorials/SPRING_CLOUD_GATEWAY_TUTORIAL.md)
- [資安原理與配置](../security/SECURITY_PRINCIPLES_AND_CONFIGURATION.md)
- [Kubernetes 架構設計](../k8s/KUBERNETES_ARCHITECTURE.md)
