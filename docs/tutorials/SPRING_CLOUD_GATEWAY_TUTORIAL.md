# Spring Cloud Gateway 教學

本文件提供 Spring Cloud Gateway 的完整教學，包含 API 路由設定、安全配置與 mTLS 整合。

## 目錄

- [1. Spring Cloud Gateway 概述](#1-spring-cloud-gateway-概述)
- [2. 基本路由配置](#2-基本路由配置)
- [3. 安全配置](#3-安全配置)
- [4. mTLS 配置](#4-mtls-配置)
- [5. 監控與管理端點](#5-監控與管理端點)
- [6. 進階配置](#6-進階配置)

---

## 1. Spring Cloud Gateway 概述

### 1.1 什麼是 Spring Cloud Gateway

Spring Cloud Gateway 是基於 Spring Framework 5、Project Reactor 和 Spring Boot 2/3 構建的 API 閘道，提供簡單而有效的路由管理方式。

### 1.2 核心概念

| 概念 | 說明 | 範例 |
|------|------|------|
| **Route** | 閘道的基本構建塊，由 ID、目標 URI、斷言和過濾器組成 | `product-service` 路由 |
| **Predicate** | 請求匹配條件，決定請求是否路由到目標 | `Path=/api/products/**` |
| **Filter** | 在請求發送前後修改請求或回應 | `StripPrefix=1` |

### 1.3 架構流程

```
┌─────────────────────────────────────────────────────────────────────────────────────────┐
│                           Spring Cloud Gateway 處理流程                                  │
│                                                                                         │
│   ┌───────────┐    ┌───────────────┐    ┌───────────────┐    ┌───────────────────────┐ │
│   │  Client   │───▶│  Gateway      │───▶│  Route        │───▶│  Global Filters       │ │
│   │  Request  │    │  Handler      │    │  Predicates   │    │  (認證、日誌、限流)    │ │
│   └───────────┘    │  Mapping      │    │  Matching     │    └───────────┬───────────┘ │
│                    └───────────────┘    └───────────────┘                │             │
│                                                                          ▼             │
│   ┌───────────┐    ┌───────────────┐    ┌───────────────┐    ┌───────────────────────┐ │
│   │  Client   │◀───│  Post         │◀───│  Downstream   │◀───│  Route Filters        │ │
│   │  Response │    │  Filters      │    │  Service      │    │  (路徑處理、Header)   │ │
│   └───────────┘    └───────────────┘    └───────────────┘    └───────────────────────┘ │
└─────────────────────────────────────────────────────────────────────────────────────────┘
```

---

## 2. 基本路由配置

### 2.1 YAML 配置方式

```yaml
# services/gateway-service/src/main/resources/application.yml

spring:
  application:
    name: gateway-service

  cloud:
    gateway:
      routes:
        # Product Service 路由
        - id: product-service
          uri: http://localhost:8081
          predicates:
            - Path=/api/products/**
          filters:
            - StripPrefix=0

        # User Service 路由
        - id: user-service
          uri: http://localhost:8082
          predicates:
            - Path=/api/users/**
          filters:
            - StripPrefix=0

server:
  port: 8080
```

### 2.2 路由配置詳解

#### Route ID
每個路由的唯一識別碼，用於日誌和監控。

```yaml
- id: product-service  # 建議使用服務名稱
```

#### URI (目標位址)
請求轉發的目標服務位址。

```yaml
# HTTP 協議
uri: http://localhost:8081

# HTTPS 協議 (mTLS)
uri: https://product-service.rbac-sso.svc.cluster.local:8081

# 負載均衡 (需要 Spring Cloud LoadBalancer)
uri: lb://product-service
```

#### Predicates (斷言)

| Predicate | 說明 | 範例 |
|-----------|------|------|
| Path | 路徑匹配 | `Path=/api/products/**` |
| Method | HTTP 方法匹配 | `Method=GET,POST` |
| Header | Header 匹配 | `Header=X-Request-Id, \d+` |
| Query | 查詢參數匹配 | `Query=category` |
| Host | 主機名匹配 | `Host=**.example.com` |
| Cookie | Cookie 匹配 | `Cookie=session,abc*` |
| After/Before | 時間條件 | `After=2025-01-01T00:00:00+08:00` |

```yaml
predicates:
  - Path=/api/products/**
  - Method=GET,POST,PUT,DELETE
  - Header=Authorization, Bearer.*
```

#### Filters (過濾器)

| Filter | 說明 | 範例 |
|--------|------|------|
| StripPrefix | 移除路徑前綴 | `StripPrefix=1` 將 `/api/products` 變為 `/products` |
| AddRequestHeader | 添加請求標頭 | `AddRequestHeader=X-Request-Source, gateway` |
| AddResponseHeader | 添加回應標頭 | `AddResponseHeader=X-Response-Time, %{now}` |
| RewritePath | 重寫路徑 | `RewritePath=/api/(?<segment>.*), /${segment}` |
| PrefixPath | 添加路徑前綴 | `PrefixPath=/v1` |
| RequestRateLimiter | 限流 | 需配置 Redis |
| CircuitBreaker | 斷路器 | 需 Resilience4j |

```yaml
filters:
  - StripPrefix=0
  - AddRequestHeader=X-Gateway-Source, spring-cloud-gateway
  - name: RequestRateLimiter
    args:
      redis-rate-limiter.replenishRate: 10
      redis-rate-limiter.burstCapacity: 20
```

### 2.3 K8s 環境路由配置

```yaml
# services/gateway-service/src/main/resources/application-k8s.yml

spring:
  cloud:
    gateway:
      routes:
        - id: product-service
          uri: http://product-service.rbac-sso.svc.cluster.local:8081
          predicates:
            - Path=/api/products/**
          filters:
            - StripPrefix=0

        - id: user-service
          uri: http://user-service.rbac-sso.svc.cluster.local:8082
          predicates:
            - Path=/api/users/**
          filters:
            - StripPrefix=0
```

---

## 3. 安全配置

### 3.1 OAuth2 Resource Server 配置

Gateway 作為 OAuth2 Resource Server，驗證所有傳入請求的 JWT Token。

```yaml
# application.yml
spring:
  security:
    oauth2:
      resourceserver:
        jwt:
          # Keycloak issuer URI
          issuer-uri: http://localhost:8180/realms/rbac-sso-realm
```

### 3.2 Security 配置類

```java
// SecurityConfig.java
@Configuration
@EnableWebFluxSecurity
public class SecurityConfig {

    @Bean
    public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {
        return http
            // 停用 CSRF (API Gateway 通常不需要)
            .csrf(ServerHttpSecurity.CsrfSpec::disable)

            // 設定授權規則
            .authorizeExchange(exchanges -> exchanges
                // 健康檢查端點公開
                .pathMatchers("/actuator/health/**").permitAll()
                .pathMatchers("/actuator/info").permitAll()

                // 管理端點需要 ADMIN 角色
                .pathMatchers("/api/admin/**").hasRole("ADMIN")

                // 其他 API 需要認證
                .pathMatchers("/api/**").authenticated()

                // 其他請求允許
                .anyExchange().permitAll()
            )

            // 啟用 OAuth2 Resource Server (JWT)
            .oauth2ResourceServer(oauth2 -> oauth2
                .jwt(jwt -> jwt
                    .jwtAuthenticationConverter(jwtAuthenticationConverter())
                )
            )

            .build();
    }

    @Bean
    public ReactiveJwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtGrantedAuthoritiesConverter grantedAuthoritiesConverter =
            new JwtGrantedAuthoritiesConverter();
        grantedAuthoritiesConverter.setAuthoritiesClaimName("roles");
        grantedAuthoritiesConverter.setAuthorityPrefix("ROLE_");

        ReactiveJwtAuthenticationConverter converter =
            new ReactiveJwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(
            new ReactiveJwtGrantedAuthoritiesConverterAdapter(grantedAuthoritiesConverter)
        );
        return converter;
    }
}
```

### 3.3 CORS 配置

```yaml
# application.yml
spring:
  cloud:
    gateway:
      globalcors:
        corsConfigurations:
          '[/**]':
            allowedOrigins:
              - "http://localhost:3000"
              - "http://localhost:8080"
              - "https://your-frontend.com"
            allowedMethods:
              - GET
              - POST
              - PUT
              - DELETE
              - OPTIONS
            allowedHeaders:
              - "*"
            exposedHeaders:
              - "X-Request-Id"
            allowCredentials: true
            maxAge: 3600
```

### 3.4 JWT Token 結構

Keycloak 簽發的 JWT Token 包含以下 Claims：

```json
{
  "exp": 1704067200,
  "iat": 1704063600,
  "iss": "http://localhost:8180/realms/rbac-sso-realm",
  "sub": "user-uuid-here",
  "preferred_username": "admin",
  "email": "admin@example.com",
  "tenant_id": "tenant-a",
  "roles": ["ADMIN", "TENANT_ADMIN"],
  "groups": ["/admins", "/tenant-a-admins"]
}
```

---

## 4. mTLS 配置

### 4.1 mTLS Profile 配置

啟用 mTLS 時，Gateway 使用獨立的 application-mtls.yml：

```yaml
# services/gateway-service/src/main/resources/application-mtls.yml

server:
  port: 8080
  ssl:
    enabled: true
    # 使用 PEM 格式憑證 (Spring Boot 3.1+)
    certificate: /etc/ssl/certs/tls.crt
    certificate-private-key: /etc/ssl/certs/tls.key
    # 客戶端憑證驗證 (對外可選)
    client-auth: want
    trust-certificate: /etc/ssl/certs/ca.crt
    # TLS 版本限制
    protocol: TLS
    enabled-protocols: TLSv1.3,TLSv1.2

spring:
  cloud:
    gateway:
      routes:
        # Product Service (使用 HTTPS)
        - id: product-service
          uri: https://product-service.rbac-sso.svc.cluster.local:8081
          predicates:
            - Path=/api/products/**
          filters:
            - StripPrefix=0

        # User Service (使用 HTTPS)
        - id: user-service
          uri: https://user-service.rbac-sso.svc.cluster.local:8082
          predicates:
            - Path=/api/users/**
          filters:
            - StripPrefix=0

# 健康檢查 (使用獨立的 HTTP 端口供 K8s 探針使用)
management:
  server:
    port: 8090  # 管理端口不啟用 SSL
  endpoints:
    web:
      exposure:
        include: health,info,gateway

logging:
  level:
    javax.net.ssl: DEBUG
    org.springframework.security: DEBUG
    reactor.netty: DEBUG
```

### 4.2 SSL 配置參數說明

| 參數 | 說明 | 建議值 |
|------|------|--------|
| `server.ssl.enabled` | 啟用 SSL | `true` |
| `server.ssl.certificate` | 服務憑證路徑 | `/etc/ssl/certs/tls.crt` |
| `server.ssl.certificate-private-key` | 私鑰路徑 | `/etc/ssl/certs/tls.key` |
| `server.ssl.client-auth` | 客戶端驗證模式 | `want` (外部可選) / `need` (內部強制) |
| `server.ssl.trust-certificate` | CA 憑證路徑 | `/etc/ssl/certs/ca.crt` |
| `server.ssl.enabled-protocols` | 啟用的 TLS 版本 | `TLSv1.3,TLSv1.2` |

### 4.3 client-auth 模式說明

| 模式 | 說明 | 適用場景 |
|------|------|----------|
| `none` | 不要求客戶端憑證 | 公開 API |
| `want` | 可選客戶端憑證 | Gateway 對外 |
| `need` | 強制客戶端憑證 | 內部服務間通訊 |

### 4.4 WebFlux SSL Context 配置

對於 Spring Cloud Gateway (WebFlux)，需要配置 Reactor Netty 的 SSL Context：

```java
// GatewayMtlsConfiguration.java
@Configuration
@ConditionalOnProperty(name = "server.ssl.enabled", havingValue = "true")
public class GatewayMtlsConfiguration {

    @Value("${server.ssl.certificate}")
    private String certificatePath;

    @Value("${server.ssl.certificate-private-key}")
    private String privateKeyPath;

    @Value("${server.ssl.trust-certificate}")
    private String caCertificatePath;

    @Bean
    public HttpClient httpClient() throws Exception {
        SslContext sslContext = SslContextBuilder
            .forClient()
            .keyManager(
                new File(certificatePath),
                new File(privateKeyPath)
            )
            .trustManager(new File(caCertificatePath))
            .protocols("TLSv1.3", "TLSv1.2")
            .build();

        return HttpClient.create()
            .secure(spec -> spec.sslContext(sslContext));
    }

    @Bean
    public WebClient.Builder webClientBuilder(HttpClient httpClient) {
        return WebClient.builder()
            .clientConnector(new ReactorClientHttpConnector(httpClient));
    }
}
```

---

## 5. 監控與管理端點

### 5.1 Actuator 配置

```yaml
# application.yml
management:
  endpoints:
    web:
      exposure:
        include: health,info,gateway,metrics,prometheus
  endpoint:
    health:
      show-details: when-authorized
      probes:
        enabled: true
    gateway:
      enabled: true
```

### 5.2 可用端點

| 端點 | 方法 | 說明 |
|------|------|------|
| `/actuator/health` | GET | 健康檢查 |
| `/actuator/health/liveness` | GET | K8s 存活探針 |
| `/actuator/health/readiness` | GET | K8s 就緒探針 |
| `/actuator/info` | GET | 應用程式資訊 |
| `/actuator/gateway/routes` | GET | 查看所有路由 |
| `/actuator/gateway/routes/{id}` | GET | 查看特定路由 |
| `/actuator/gateway/refresh` | POST | 重新載入路由 |
| `/actuator/gateway/globalfilters` | GET | 全域過濾器列表 |
| `/actuator/gateway/routefilters` | GET | 路由過濾器列表 |
| `/actuator/metrics` | GET | 指標資訊 |

### 5.3 查看路由範例

```bash
# 查看所有路由
curl http://localhost:8090/actuator/gateway/routes | jq

# 輸出範例
[
  {
    "route_id": "product-service",
    "route_object": {
      "predicate": "Paths: [/api/products/**]",
      "filters": ["StripPrefix=0"]
    },
    "uri": "http://product-service:8081",
    "order": 0
  },
  {
    "route_id": "user-service",
    "route_object": {
      "predicate": "Paths: [/api/users/**]",
      "filters": ["StripPrefix=0"]
    },
    "uri": "http://user-service:8082",
    "order": 0
  }
]
```

### 5.4 健康檢查回應

```json
// GET /actuator/health
{
  "status": "UP",
  "components": {
    "diskSpace": {
      "status": "UP",
      "details": {
        "total": 499963174912,
        "free": 234567890123
      }
    },
    "ping": {
      "status": "UP"
    },
    "ssl": {
      "status": "UP",
      "details": {
        "certificate": "valid",
        "expiry": "2026-01-15T00:00:00Z"
      }
    }
  }
}
```

---

## 6. 進階配置

### 6.1 Rate Limiting (限流)

需要 Redis 支援：

```yaml
spring:
  cloud:
    gateway:
      routes:
        - id: product-service
          uri: http://product-service:8081
          predicates:
            - Path=/api/products/**
          filters:
            - name: RequestRateLimiter
              args:
                redis-rate-limiter.replenishRate: 10   # 每秒補充 10 個 token
                redis-rate-limiter.burstCapacity: 20   # 最大容量 20 個 token
                key-resolver: "#{@userKeyResolver}"    # 使用 user 作為限流 key

  redis:
    host: redis
    port: 6379
```

```java
@Bean
public KeyResolver userKeyResolver() {
    return exchange -> Mono.just(
        exchange.getRequest().getHeaders()
            .getFirst("X-User-Id") != null
            ? exchange.getRequest().getHeaders().getFirst("X-User-Id")
            : "anonymous"
    );
}
```

### 6.2 Circuit Breaker (斷路器)

使用 Resilience4j：

```yaml
spring:
  cloud:
    gateway:
      routes:
        - id: product-service
          uri: http://product-service:8081
          predicates:
            - Path=/api/products/**
          filters:
            - name: CircuitBreaker
              args:
                name: productServiceCircuitBreaker
                fallbackUri: forward:/fallback/products

resilience4j:
  circuitbreaker:
    instances:
      productServiceCircuitBreaker:
        slidingWindowSize: 10
        failureRateThreshold: 50
        waitDurationInOpenState: 30s
        permittedNumberOfCallsInHalfOpenState: 5
```

### 6.3 Retry (重試)

```yaml
spring:
  cloud:
    gateway:
      routes:
        - id: product-service
          uri: http://product-service:8081
          predicates:
            - Path=/api/products/**
          filters:
            - name: Retry
              args:
                retries: 3
                statuses: BAD_GATEWAY,SERVICE_UNAVAILABLE
                methods: GET,POST
                backoff:
                  firstBackoff: 100ms
                  maxBackoff: 500ms
                  factor: 2
```

### 6.4 Request/Response 日誌

```java
@Component
public class LoggingFilter implements GlobalFilter, Ordered {

    private static final Logger log = LoggerFactory.getLogger(LoggingFilter.class);

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();

        log.info("Request: {} {} from {}",
            request.getMethod(),
            request.getPath(),
            request.getRemoteAddress()
        );

        return chain.filter(exchange)
            .then(Mono.fromRunnable(() -> {
                ServerHttpResponse response = exchange.getResponse();
                log.info("Response: {} for {} {}",
                    response.getStatusCode(),
                    request.getMethod(),
                    request.getPath()
                );
            }));
    }

    @Override
    public int getOrder() {
        return -1; // 最高優先級
    }
}
```

### 6.5 自訂 Global Filter

```java
@Component
public class TenantContextFilter implements GlobalFilter, Ordered {

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        return ReactiveSecurityContextHolder.getContext()
            .map(SecurityContext::getAuthentication)
            .filter(auth -> auth instanceof JwtAuthenticationToken)
            .cast(JwtAuthenticationToken.class)
            .map(jwt -> {
                String tenantId = jwt.getToken().getClaimAsString("tenant_id");
                // 將 tenant_id 加入 Header 傳遞給下游服務
                ServerHttpRequest mutatedRequest = exchange.getRequest()
                    .mutate()
                    .header("X-Tenant-Id", tenantId)
                    .build();
                return exchange.mutate().request(mutatedRequest).build();
            })
            .defaultIfEmpty(exchange)
            .flatMap(chain::filter);
    }

    @Override
    public int getOrder() {
        return 0;
    }
}
```

---

## 相關文件

- [專案架構與資安設計](../architecture/ARCHITECTURE_AND_SECURITY.md)
- [資安原理與配置](../security/SECURITY_PRINCIPLES_AND_CONFIGURATION.md)
- [Kubernetes 架構設計](../k8s/KUBERNETES_ARCHITECTURE.md)
