# 資安原理與配置管理

本文件說明 RBAC-SSO-POC 專案的資安原理、設計理念與配置管理方式。

## 目錄

- [1. 資安設計原則](#1-資安設計原則)
- [2. OAuth2/OIDC 認證機制](#2-oauth2oidc-認證機制)
- [3. mTLS 雙向認證](#3-mtls-雙向認證)
- [4. RBAC 權限控制](#4-rbac-權限控制)
- [5. 多租戶安全隔離](#5-多租戶安全隔離)
- [6. 稽核日誌機制](#6-稽核日誌機制)
- [7. 安全配置管理](#7-安全配置管理)

---

## 1. 資安設計原則

### 1.1 縱深防禦 (Defense in Depth)

```
┌───────────────────────────────────────────────────────────────────────────────────────┐
│                               縱深防禦架構                                              │
│                                                                                       │
│  Layer 1: 網路邊界 ─────────────────────────────────────────────────────────────────   │
│  ┌─────────────────────────────────────────────────────────────────────────────────┐  │
│  │  - Kubernetes Network Policy                                                    │  │
│  │  - Ingress Controller (HTTPS Only)                                              │  │
│  │  - DDoS Protection (Cloud Provider)                                             │  │
│  └─────────────────────────────────────────────────────────────────────────────────┘  │
│                                         │                                             │
│  Layer 2: 應用程式閘道 ────────────────────────────────────────────────────────────   │
│  ┌─────────────────────────────────────────────────────────────────────────────────┐  │
│  │  - Spring Cloud Gateway                                                         │  │
│  │  - JWT Token 驗證                                                               │  │
│  │  - Rate Limiting                                                                │  │
│  │  - CORS 控制                                                                    │  │
│  └─────────────────────────────────────────────────────────────────────────────────┘  │
│                                         │                                             │
│  Layer 3: 服務間通訊 ──────────────────────────────────────────────────────────────   │
│  ┌─────────────────────────────────────────────────────────────────────────────────┐  │
│  │  - mTLS 雙向認證                                                                │  │
│  │  - Service Mesh (可選: Istio)                                                   │  │
│  │  - Network Policies                                                             │  │
│  └─────────────────────────────────────────────────────────────────────────────────┘  │
│                                         │                                             │
│  Layer 4: 應用程式層 ──────────────────────────────────────────────────────────────   │
│  ┌─────────────────────────────────────────────────────────────────────────────────┐  │
│  │  - RBAC 權限控制                                                                │  │
│  │  - 多租戶資料隔離                                                               │  │
│  │  - 輸入驗證                                                                     │  │
│  │  - 稽核日誌                                                                     │  │
│  └─────────────────────────────────────────────────────────────────────────────────┘  │
│                                         │                                             │
│  Layer 5: 資料層 ──────────────────────────────────────────────────────────────────   │
│  ┌─────────────────────────────────────────────────────────────────────────────────┐  │
│  │  - Database Access Control                                                      │  │
│  │  - Encryption at Rest (可選)                                                    │  │
│  │  - Backup & Recovery                                                            │  │
│  └─────────────────────────────────────────────────────────────────────────────────┘  │
└───────────────────────────────────────────────────────────────────────────────────────┘
```

### 1.2 零信任原則 (Zero Trust)

| 原則 | 實作方式 | 說明 |
|------|----------|------|
| **永不信任** | mTLS + JWT | 所有請求都需驗證 |
| **最小權限** | RBAC | 僅授予完成任務所需權限 |
| **假設被攻破** | 稽核日誌 | 記錄所有操作以便追蹤 |
| **明確驗證** | 多因素驗證 | 身份 + 設備 + 行為 |
| **限制爆炸半徑** | 多租戶隔離 | 單一租戶洩漏不影響其他 |

### 1.3 安全開發生命週期 (SDL)

```
┌─────────────────────────────────────────────────────────────────────────────────────────┐
│                           Secure Development Lifecycle                                   │
│                                                                                         │
│    ┌──────────┐    ┌──────────┐    ┌──────────┐    ┌──────────┐    ┌──────────┐       │
│    │  需求    │───▶│  設計    │───▶│  開發    │───▶│  測試    │───▶│  部署    │       │
│    │  分析    │    │  審查    │    │  實作    │    │  驗證    │    │  監控    │       │
│    └────┬─────┘    └────┬─────┘    └────┬─────┘    └────┬─────┘    └────┬─────┘       │
│         │               │               │               │               │              │
│    ┌────▼─────┐    ┌────▼─────┐    ┌────▼─────┐    ┌────▼─────┐    ┌────▼─────┐       │
│    │ 威脅模型 │    │ 架構審查 │    │ 程式碼   │    │ 滲透測試 │    │ 安全監控 │       │
│    │ 分析     │    │ 安全設計 │    │ 審查     │    │ 漏洞掃描 │    │ 事件回應 │       │
│    └──────────┘    └──────────┘    └──────────┘    └──────────┘    └──────────┘       │
│                                                                                         │
└─────────────────────────────────────────────────────────────────────────────────────────┘
```

---

## 2. OAuth2/OIDC 認證機制

### 2.1 OAuth2 流程概述

```
┌────────────────────────────────────────────────────────────────────────────────────────┐
│                         OAuth2 Authorization Code Flow                                  │
│                                                                                        │
│  ┌──────────┐                                           ┌──────────────────┐           │
│  │          │  1. Authorization Request                 │                  │           │
│  │  Client  │──────────────────────────────────────────▶│    Keycloak      │           │
│  │          │                                           │  (Auth Server)   │           │
│  │          │  2. User Authentication                   │                  │           │
│  │          │◀──────────────────────────────────────────│                  │           │
│  │          │                                           └────────┬─────────┘           │
│  │          │                                                    │                     │
│  │          │  3. Authorization Code                             │                     │
│  │          │◀───────────────────────────────────────────────────┘                     │
│  │          │                                                                          │
│  │          │  4. Token Request (code + client_secret)                                 │
│  │          │────────────────────────────────────────────────────▶                     │
│  │          │                                           ┌──────────────────┐           │
│  │          │  5. Access Token + Refresh Token          │                  │           │
│  │          │◀──────────────────────────────────────────│    Keycloak      │           │
│  │          │                                           │                  │           │
│  │          │  6. API Request (Bearer Token)            └──────────────────┘           │
│  │          │────────────────────────────────────────────────────▶                     │
│  │          │                                           ┌──────────────────┐           │
│  │          │  7. Response                              │  Resource Server │           │
│  │          │◀──────────────────────────────────────────│  (Microservices) │           │
│  └──────────┘                                           └──────────────────┘           │
└────────────────────────────────────────────────────────────────────────────────────────┘
```

### 2.2 JWT Token 結構

```
┌────────────────────────────────────────────────────────────────────────────────────────┐
│                              JWT Token 結構                                             │
│                                                                                        │
│  Header (Base64)                                                                       │
│  ┌────────────────────────────────────────────────────────────────────────────────┐   │
│  │  {                                                                              │   │
│  │    "alg": "RS256",           // 簽章演算法                                       │   │
│  │    "typ": "JWT",             // Token 類型                                       │   │
│  │    "kid": "key-id-123"       // Key ID (用於驗證)                                │   │
│  │  }                                                                              │   │
│  └────────────────────────────────────────────────────────────────────────────────┘   │
│                                         .                                              │
│  Payload (Base64)                                                                      │
│  ┌────────────────────────────────────────────────────────────────────────────────┐   │
│  │  {                                                                              │   │
│  │    "iss": "http://keycloak:8180/realms/rbac-sso",  // 簽發者                    │   │
│  │    "sub": "user-uuid-12345",                       // Subject (用戶 ID)         │   │
│  │    "aud": "gateway-client",                        // Audience                   │   │
│  │    "exp": 1704067200,                              // 過期時間                   │   │
│  │    "iat": 1704063600,                              // 簽發時間                   │   │
│  │    "preferred_username": "admin",                  // 用戶名                     │   │
│  │    "email": "admin@example.com",                   // Email                      │   │
│  │    "tenant_id": "tenant-a",                        // 租戶 ID (自定義)           │   │
│  │    "roles": ["ADMIN", "TENANT_ADMIN"],             // 角色 (自定義)              │   │
│  │    "groups": ["/admins", "/tenant-a"]              // 群組 (自定義)              │   │
│  │  }                                                                              │   │
│  └────────────────────────────────────────────────────────────────────────────────┘   │
│                                         .                                              │
│  Signature (RS256)                                                                     │
│  ┌────────────────────────────────────────────────────────────────────────────────┐   │
│  │  RSASHA256(                                                                     │   │
│  │    base64UrlEncode(header) + "." + base64UrlEncode(payload),                   │   │
│  │    privateKey                                                                   │   │
│  │  )                                                                              │   │
│  └────────────────────────────────────────────────────────────────────────────────┘   │
└────────────────────────────────────────────────────────────────────────────────────────┘
```

### 2.3 Spring Security 配置

```java
// SecurityConfig.java (Servlet-based 服務)
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        return http
            .csrf(csrf -> csrf.disable())
            .sessionManagement(session ->
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            )
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/actuator/health/**").permitAll()
                .requestMatchers("/api/admin/**").hasRole("ADMIN")
                .requestMatchers("/api/**").authenticated()
                .anyRequest().permitAll()
            )
            .oauth2ResourceServer(oauth2 -> oauth2
                .jwt(jwt -> jwt
                    .jwtAuthenticationConverter(jwtAuthenticationConverter())
                )
            )
            .build();
    }

    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtGrantedAuthoritiesConverter converter = new JwtGrantedAuthoritiesConverter();
        converter.setAuthoritiesClaimName("roles");
        converter.setAuthorityPrefix("ROLE_");

        JwtAuthenticationConverter jwtConverter = new JwtAuthenticationConverter();
        jwtConverter.setJwtGrantedAuthoritiesConverter(converter);
        return jwtConverter;
    }
}
```

### 2.4 Keycloak 整合配置

```yaml
# application.yml
spring:
  security:
    oauth2:
      resourceserver:
        jwt:
          issuer-uri: ${KEYCLOAK_ISSUER_URI:http://localhost:8180/realms/rbac-sso-realm}
          jwk-set-uri: ${KEYCLOAK_ISSUER_URI:http://localhost:8180/realms/rbac-sso-realm}/protocol/openid-connect/certs
```

---

## 3. mTLS 雙向認證

### 3.1 mTLS 原理

```
┌────────────────────────────────────────────────────────────────────────────────────────┐
│                              mTLS vs TLS 比較                                           │
│                                                                                        │
│  傳統 TLS (單向)                          mTLS (雙向)                                   │
│  ┌─────────────────────┐                 ┌─────────────────────┐                      │
│  │     Client          │                 │     Client          │                      │
│  │                     │                 │  ┌───────────────┐  │                      │
│  │  信任 Server 憑證    │                 │  │ Client Cert   │  │                      │
│  │  (驗證 Server 身份)  │                 │  │ + Private Key │  │                      │
│  │                     │                 │  └───────────────┘  │                      │
│  └─────────┬───────────┘                 └─────────┬───────────┘                      │
│            │                                       │                                   │
│            │ Server 憑證                           │ Server 憑證 + Client 憑證          │
│            ▼                                       ▼                                   │
│  ┌─────────────────────┐                 ┌─────────────────────┐                      │
│  │     Server          │                 │     Server          │                      │
│  │  ┌───────────────┐  │                 │  ┌───────────────┐  │                      │
│  │  │ Server Cert   │  │                 │  │ Server Cert   │  │                      │
│  │  │ + Private Key │  │                 │  │ + Private Key │  │                      │
│  │  └───────────────┘  │                 │  │ + CA Cert     │  │ ◀── 驗證 Client 身份  │
│  │                     │                 │  └───────────────┘  │                      │
│  └─────────────────────┘                 └─────────────────────┘                      │
│                                                                                        │
│  安全等級: ★★☆                            安全等級: ★★★                              │
│  - 只驗證 Server                          - 雙方互相驗證                                │
│  - Client 身份未驗證                       - 防止未授權服務呼叫                          │
└────────────────────────────────────────────────────────────────────────────────────────┘
```

### 3.2 憑證層級結構

```
                    Root CA (Self-Signed)
                    ┌─────────────────────┐
                    │  rbac-sso-ca        │
                    │                     │
                    │  Algorithm: ECDSA   │
                    │  Validity: 10 years │
                    └─────────┬───────────┘
                              │
              ┌───────────────┼───────────────┐
              │               │               │
              ▼               ▼               ▼
        ┌───────────┐   ┌───────────┐   ┌───────────┐
        │ Gateway   │   │ Product   │   │ User      │
        │ Service   │   │ Service   │   │ Service   │
        │           │   │           │   │           │
        │ client-   │   │ client-   │   │ client-   │
        │ auth:want │   │ auth:need │   │ auth:need │
        └───────────┘   └───────────┘   └───────────┘
             │               │               │
             └───────────────┼───────────────┘
                             │
                    ┌────────▼────────┐
                    │  Trust Chain    │
                    │                 │
                    │ 所有服務信任同   │
                    │ 一個 CA 簽發的   │
                    │ 憑證             │
                    └─────────────────┘
```

### 3.3 Spring Boot SSL 配置

```yaml
# application-mtls.yml
server:
  port: 8081
  ssl:
    enabled: true
    # PEM 格式憑證 (Spring Boot 3.1+)
    certificate: /etc/ssl/certs/tls.crt
    certificate-private-key: /etc/ssl/certs/tls.key
    # 客戶端憑證驗證
    client-auth: need  # none | want | need
    trust-certificate: /etc/ssl/certs/ca.crt
    # TLS 協議
    protocol: TLS
    enabled-protocols: TLSv1.3,TLSv1.2
```

### 3.4 client-auth 模式詳解

| 模式 | 行為 | 使用場景 |
|------|------|----------|
| `none` | 不要求客戶端憑證 | 公開 API、Web 前端 |
| `want` | 如果提供則驗證，不提供也允許 | Gateway 對外、向下相容 |
| `need` | 強制要求客戶端憑證 | 內部服務間通訊 |

### 3.5 cert-manager 憑證資源

```yaml
# service-certificates.yaml
apiVersion: cert-manager.io/v1
kind: Certificate
metadata:
  name: product-service-tls
  namespace: rbac-sso
spec:
  secretName: product-service-tls-secret
  duration: 8760h     # 1 年
  renewBefore: 720h   # 30 天前自動更新
  commonName: product-service
  privateKey:
    algorithm: ECDSA
    size: 256
  usages:
    - server auth     # 作為 Server
    - client auth     # 作為 Client (呼叫其他服務時)
  dnsNames:
    - product-service
    - product-service.rbac-sso.svc.cluster.local
  issuerRef:
    name: rbac-sso-ca-issuer
    kind: ClusterIssuer
```

---

## 4. RBAC 權限控制

### 4.1 角色層級設計

```
┌────────────────────────────────────────────────────────────────────────────────────────┐
│                              RBAC 角色層級                                              │
│                                                                                        │
│                        ┌─────────────────┐                                             │
│                        │     ADMIN       │                                             │
│                        │ (系統管理員)     │                                             │
│                        │                 │                                             │
│                        │ - 所有權限      │                                             │
│                        │ - 跨租戶管理    │                                             │
│                        └────────┬────────┘                                             │
│                                 │                                                      │
│                    ┌────────────┴────────────┐                                        │
│                    │                         │                                        │
│           ┌────────▼────────┐      ┌────────▼────────┐                               │
│           │  TENANT_ADMIN   │      │     OPERATOR    │                               │
│           │ (租戶管理員)     │      │   (維運人員)     │                               │
│           │                 │      │                 │                               │
│           │ - 租戶內管理    │      │ - 監控權限      │                               │
│           │ - 商品 CRUD     │      │ - 日誌查看      │                               │
│           └────────┬────────┘      └─────────────────┘                               │
│                    │                                                                  │
│           ┌────────┴────────┐                                                        │
│           │                 │                                                        │
│  ┌────────▼────────┐  ┌────▼────────────┐                                            │
│  │      USER       │  │     VIEWER      │                                            │
│  │  (一般使用者)    │  │   (唯讀使用者)   │                                            │
│  │                 │  │                 │                                            │
│  │ - 商品查看      │  │ - 商品查看      │                                            │
│  │ - 個人資訊      │  │ (僅限自己租戶)   │                                            │
│  └─────────────────┘  └─────────────────┘                                            │
└────────────────────────────────────────────────────────────────────────────────────────┘
```

### 4.2 權限矩陣

| 端點 | ADMIN | TENANT_ADMIN | USER | VIEWER |
|------|:-----:|:------------:|:----:|:------:|
| `GET /api/products` | Yes | Yes (租戶內) | Yes (租戶內) | Yes (租戶內) |
| `GET /api/products/{id}` | Yes | Yes (租戶內) | Yes (租戶內) | Yes (租戶內) |
| `POST /api/products` | Yes | Yes | No | No |
| `PUT /api/products/{id}` | Yes | Yes (租戶內) | No | No |
| `DELETE /api/products/{id}` | Yes | No | No | No |
| `GET /api/users/me` | Yes | Yes | Yes | Yes |
| `GET /api/admin/users` | Yes | No | No | No |
| `GET /actuator/health` | Yes | Yes | Yes | Yes |

### 4.3 Spring Security 方法級權限

```java
// ProductCommandController.java
@RestController
@RequestMapping("/api/products")
public class ProductCommandController {

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'TENANT_ADMIN')")
    public ResponseEntity<ApiResponse<UUID>> createProduct(
            @RequestBody CreateProductRequest request) {
        // 建立商品邏輯
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'TENANT_ADMIN')")
    public ResponseEntity<ApiResponse<Void>> updateProduct(
            @PathVariable UUID id,
            @RequestBody UpdateProductRequest request) {
        // 更新商品邏輯
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")  // 只有 ADMIN 可刪除
    public ResponseEntity<Void> deleteProduct(@PathVariable UUID id) {
        // 刪除商品邏輯
    }
}
```

### 4.4 SpEL 表達式範例

| 表達式 | 說明 |
|--------|------|
| `hasRole('ADMIN')` | 具有 ADMIN 角色 |
| `hasAnyRole('ADMIN', 'TENANT_ADMIN')` | 具有任一角色 |
| `hasAuthority('SCOPE_read')` | 具有特定權限 |
| `@authService.canAccess(#id)` | 呼叫自定義服務方法 |
| `#request.tenantId == authentication.principal.tenantId` | 租戶驗證 |

---

## 5. 多租戶安全隔離

### 5.1 隔離機制流程

```
┌────────────────────────────────────────────────────────────────────────────────────────┐
│                              多租戶資料隔離流程                                          │
│                                                                                        │
│  1. 請求進入                                                                           │
│  ┌──────────────────────────────────────────────────────────────────────────────────┐ │
│  │  GET /api/products                                                               │ │
│  │  Authorization: Bearer eyJ... (JWT with tenant_id: tenant-a)                     │ │
│  └──────────────────────────────────────────────────────────────────────────────────┘ │
│                                         │                                              │
│  2. TenantFilter 擷取租戶                                                              │
│  ┌──────────────────────────────────────▼───────────────────────────────────────────┐ │
│  │  TenantFilter.doFilter() {                                                       │ │
│  │      String tenantId = extractFromJwt(request);  // tenant-a                     │ │
│  │      TenantContext.setCurrentTenant(tenantId);   // ThreadLocal 儲存             │ │
│  │      try {                                                                       │ │
│  │          chain.doFilter(request, response);                                      │ │
│  │      } finally {                                                                 │ │
│  │          TenantContext.clear();                                                  │ │
│  │      }                                                                           │ │
│  │  }                                                                               │ │
│  └──────────────────────────────────────────────────────────────────────────────────┘ │
│                                         │                                              │
│  3. Service 層查詢                                                                     │
│  ┌──────────────────────────────────────▼───────────────────────────────────────────┐ │
│  │  ProductQueryService.findAll() {                                                 │ │
│  │      String tenant = TenantContext.getCurrentTenant();                           │ │
│  │      if ("system".equals(tenant)) {                                              │ │
│  │          return repository.findAll();        // ADMIN 看全部                     │ │
│  │      }                                                                           │ │
│  │      return repository.findByTenantId(tenant);  // 其他只看自己租戶               │ │
│  │  }                                                                               │ │
│  └──────────────────────────────────────────────────────────────────────────────────┘ │
│                                         │                                              │
│  4. Repository 執行 SQL                                                                │
│  ┌──────────────────────────────────────▼───────────────────────────────────────────┐ │
│  │  SELECT * FROM products WHERE tenant_id = 'tenant-a' AND status != 'DELETED'    │ │
│  └──────────────────────────────────────────────────────────────────────────────────┘ │
└────────────────────────────────────────────────────────────────────────────────────────┘
```

### 5.2 TenantContext 實作

```java
// TenantContext.java
public final class TenantContext {
    private static final ThreadLocal<String> CURRENT_TENANT = new ThreadLocal<>();

    public static void setCurrentTenant(String tenantId) {
        CURRENT_TENANT.set(tenantId);
    }

    public static String getCurrentTenant() {
        return CURRENT_TENANT.get();
    }

    public static void clear() {
        CURRENT_TENANT.remove();
    }

    public static boolean isSystemTenant() {
        return "system".equals(getCurrentTenant());
    }
}

// TenantFilter.java
@Component
public class TenantFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain)
            throws ServletException, IOException {
        try {
            String tenantId = extractTenantFromJwt(request);
            if (tenantId != null) {
                TenantContext.setCurrentTenant(tenantId);
            }
            chain.doFilter(request, response);
        } finally {
            TenantContext.clear();
        }
    }

    private String extractTenantFromJwt(HttpServletRequest request) {
        // 從 SecurityContext 取得 JWT claims
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth instanceof JwtAuthenticationToken jwtAuth) {
            return jwtAuth.getToken().getClaimAsString("tenant_id");
        }
        return null;
    }
}
```

### 5.3 跨租戶存取防護

```java
// ProductQueryService.java
@Service
public class ProductQueryService {

    public ProductView getById(UUID id) {
        Product product = repository.findById(ProductId.of(id))
            .orElseThrow(() -> new ProductNotFoundException(id));

        // 租戶驗證
        String currentTenant = TenantContext.getCurrentTenant();
        if (!TenantContext.isSystemTenant() &&
            !product.getTenantId().equals(currentTenant)) {
            // 非系統管理員嘗試存取其他租戶資料
            throw new ProductNotFoundException(id);  // 回傳 404 而非 403
        }

        return toView(product);
    }
}
```

---

## 6. 稽核日誌機制

### 6.1 稽核架構

```
┌────────────────────────────────────────────────────────────────────────────────────────┐
│                              稽核日誌架構                                               │
│                                                                                        │
│  方式一：Spring AOP (main 分支)                                                         │
│  ┌──────────────────────────────────────────────────────────────────────────────────┐ │
│  │                                                                                  │ │
│  │   @Auditable(eventType = CREATE_PRODUCT)                                        │ │
│  │   public UUID handle(CreateProductCommand cmd) {                                │ │
│  │       // 業務邏輯                                                                │ │
│  │   }                                                                              │ │
│  │                                                                                  │ │
│  │   AuditableAspect 自動攔截:                                                      │ │
│  │   - 記錄方法參數                                                                 │ │
│  │   - 記錄執行結果                                                                 │ │
│  │   - 記錄執行時間                                                                 │ │
│  │   - 記錄錯誤訊息 (如果失敗)                                                       │ │
│  │                                                                                  │ │
│  └──────────────────────────────────────────────────────────────────────────────────┘ │
│                                                                                        │
│  方式二：Domain Event (domain-event-for-audit 分支)                                     │
│  ┌──────────────────────────────────────────────────────────────────────────────────┐ │
│  │                                                                                  │ │
│  │   Product product = Product.create(...);                                        │ │
│  │   // 自動註冊 ProductCreated 事件                                                 │ │
│  │                                                                                  │ │
│  │   eventPublisher.publish(product.pullDomainEvents());                           │ │
│  │   // AuditDomainEventListener 監聽並記錄                                          │ │
│  │                                                                                  │ │
│  └──────────────────────────────────────────────────────────────────────────────────┘ │
└────────────────────────────────────────────────────────────────────────────────────────┘
```

### 6.2 AuditLog Entity

```java
// AuditLog.java (Domain)
@Entity
@Table(name = "audit_logs")
public class AuditLog {

    @Id
    private UUID id;

    @Column(nullable = false)
    private Instant timestamp;

    @Column(name = "event_type", nullable = false)
    private String eventType;

    @Column(name = "aggregate_type")
    private String aggregateType;

    @Column(name = "aggregate_id")
    private String aggregateId;

    @Column(nullable = false)
    private String username;

    @Column(name = "service_name")
    private String serviceName;

    @Column(nullable = false)
    private String action;

    @Column(columnDefinition = "TEXT")
    private String payload;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private AuditResult result;  // SUCCESS, FAILURE

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "client_ip")
    private String clientIp;

    @Column(name = "correlation_id")
    private String correlationId;

    @Column(name = "payload_truncated")
    private boolean payloadTruncated;
}
```

### 6.3 @Auditable 註解使用

```java
// AOP 方式 (main 分支)
@Service
public class ProductCommandService {

    @Auditable(eventType = AuditEventType.CREATE_PRODUCT)
    public UUID handle(CreateProductCommand cmd) {
        Product product = Product.create(
            ProductCode.of(cmd.productCode()),
            cmd.name(),
            Money.of(cmd.price()),
            cmd.category(),
            cmd.description(),
            TenantContext.getCurrentTenant(),
            SecurityUtils.getCurrentUsername()
        );
        return repository.save(product).getId().getValue();
    }
}
```

### 6.4 稽核日誌查詢 API

```
GET /api/audit/logs?username=admin&page=0&size=20
GET /api/audit/logs?eventType=CREATE_PRODUCT&page=0&size=20
GET /api/audit/logs?from=2025-01-01T00:00:00Z&to=2025-01-31T23:59:59Z
GET /api/audit/logs/{correlationId}
```

---

## 7. 安全配置管理

### 7.1 配置檔案結構

```
services/
├── gateway-service/
│   └── src/main/resources/
│       ├── application.yml         # 預設配置
│       ├── application-dev.yml     # 開發環境
│       ├── application-k8s.yml     # K8s 環境
│       └── application-mtls.yml    # mTLS 配置
│
├── product-service/
│   └── src/main/resources/
│       ├── application.yml
│       ├── application-dev.yml
│       ├── application-k8s.yml
│       └── application-mtls.yml
│
└── user-service/
    └── src/main/resources/
        ├── application.yml
        ├── application-dev.yml
        ├── application-k8s.yml
        └── application-mtls.yml
```

### 7.2 環境變數管理

| 變數名稱 | 說明 | 預設值 |
|----------|------|--------|
| `SPRING_PROFILES_ACTIVE` | 啟用的 Profile | `dev` |
| `SERVER_PORT` | 服務端口 | `8080/8081/8082` |
| `KEYCLOAK_ISSUER_URI` | Keycloak Issuer | `http://localhost:8180/...` |
| `MTLS_ENABLED` | 是否啟用 mTLS | `false` |
| `MTLS_CERTIFICATE` | 憑證路徑 | `/etc/ssl/certs/tls.crt` |
| `MTLS_PRIVATE_KEY` | 私鑰路徑 | `/etc/ssl/certs/tls.key` |
| `MTLS_CA_CERTIFICATE` | CA 憑證路徑 | `/etc/ssl/certs/ca.crt` |

### 7.3 K8s ConfigMap 配置

```yaml
# deploy/k8s/base/configmap.yaml
apiVersion: v1
kind: ConfigMap
metadata:
  name: rbac-config
  namespace: rbac-sso
data:
  KEYCLOAK_ISSUER_URI: "http://keycloak.rbac-sso.svc.cluster.local:8180/realms/rbac-sso-realm"
  SPRING_PROFILES_ACTIVE: "k8s"
```

### 7.4 Secret 管理

```yaml
# deploy/k8s/base/secret.yaml
apiVersion: v1
kind: Secret
metadata:
  name: rbac-secrets
  namespace: rbac-sso
type: Opaque
data:
  # Base64 encoded
  KEYCLOAK_CLIENT_SECRET: c2VjcmV0MTIz
```

### 7.5 安全最佳實踐清單

| 項目 | 建議 | 狀態 |
|------|------|:----:|
| 敏感資料不寫死在程式碼 | 使用環境變數或 Secret | Done |
| 憑證不提交到 Git | 使用 cert-manager 動態簽發 | Done |
| 預設拒絕所有請求 | 白名單方式開放 | Done |
| 啟用 TLS 1.3 | 停用 TLS 1.0/1.1 | Done |
| JWT Token 驗證 | 驗證簽章、過期時間、Issuer | Done |
| 稽核所有敏感操作 | @Auditable / Domain Event | Done |
| 錯誤訊息不洩漏資訊 | 統一錯誤回應格式 | Done |
| 定期更新憑證 | cert-manager renewBefore | Done |

---

## 相關文件

- [專案架構與資安設計](../architecture/ARCHITECTURE_AND_SECURITY.md)
- [Spring Cloud Gateway 教學](../tutorials/SPRING_CLOUD_GATEWAY_TUTORIAL.md)
- [Kubernetes 架構設計](../k8s/KUBERNETES_ARCHITECTURE.md)
