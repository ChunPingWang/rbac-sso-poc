# TECH: 應用程式技術架構文件

## 文件資訊

| 項目 | 內容 |
|------|------|
| 文件版本 | 1.0 |
| 建立日期 | 2025-01-10 |
| 專案代號 | SSO-RBAC-POC |
| 適用範圍 | Spring Boot API Application |

---

## 1. 架構概覽

### 1.1 系統架構圖

```
┌─────────────────────────────────────────────────────────────────────────────────┐
│                           System Architecture                                    │
├─────────────────────────────────────────────────────────────────────────────────┤
│                                                                                  │
│    ┌─────────────┐                                                              │
│    │   Client    │                                                              │
│    │ (Browser/   │                                                              │
│    │  Mobile)    │                                                              │
│    └──────┬──────┘                                                              │
│           │                                                                      │
│           │ HTTPS                                                               │
│           ▼                                                                      │
│    ┌─────────────────────────────────────────────────────────────────────┐      │
│    │                    Spring Boot Application                          │      │
│    ├─────────────────────────────────────────────────────────────────────┤      │
│    │  ┌─────────────────────────────────────────────────────────────┐   │      │
│    │  │                   Presentation Layer                         │   │      │
│    │  │  ┌─────────────┐ ┌─────────────┐ ┌─────────────────────┐    │   │      │
│    │  │  │ Public      │ │ User        │ │ Admin               │    │   │      │
│    │  │  │ Controller  │ │ Controller  │ │ Controller          │    │   │      │
│    │  │  └─────────────┘ └─────────────┘ └─────────────────────┘    │   │      │
│    │  └─────────────────────────────────────────────────────────────┘   │      │
│    │                              │                                      │      │
│    │  ┌─────────────────────────────────────────────────────────────┐   │      │
│    │  │                    Security Layer                            │   │      │
│    │  │  ┌──────────────┐ ┌──────────────┐ ┌────────────────────┐   │   │      │
│    │  │  │ JWT Filter   │ │ Role Voter   │ │ Method Security    │   │   │      │
│    │  │  │              │ │              │ │ @PreAuthorize      │   │   │      │
│    │  │  └──────────────┘ └──────────────┘ └────────────────────┘   │   │      │
│    │  └─────────────────────────────────────────────────────────────┘   │      │
│    │                              │                                      │      │
│    │  ┌─────────────────────────────────────────────────────────────┐   │      │
│    │  │                    Business Layer                            │   │      │
│    │  │  ┌──────────────┐ ┌──────────────┐ ┌────────────────────┐   │   │      │
│    │  │  │ User Service │ │ Role Service │ │ Resource Service   │   │   │      │
│    │  │  └──────────────┘ └──────────────┘ └────────────────────┘   │   │      │
│    │  └─────────────────────────────────────────────────────────────┘   │      │
│    └─────────────────────────────────────────────────────────────────────┘      │
│                              │                                                   │
│                              │ JWKS                                             │
│                              ▼                                                   │
│                       ┌─────────────┐                                           │
│                       │  Keycloak   │                                           │
│                       │    (IdP)    │                                           │
│                       └─────────────┘                                           │
│                                                                                  │
└─────────────────────────────────────────────────────────────────────────────────┘
```

### 1.2 技術棧

| Layer | Technology | Version |
|-------|------------|---------|
| Runtime | Java | 21 (LTS) |
| Framework | Spring Boot | 3.3.x |
| Security | Spring Security | 6.3.x |
| OAuth2 | Spring OAuth2 Resource Server | 6.3.x |
| Build Tool | Maven | 3.9.x |
| Container | Docker | 24.x |
| API Doc | SpringDoc OpenAPI | 2.5.x |

---

## 2. 專案結構

### 2.1 目錄結構

```
spring-api/
├── src/
│   ├── main/
│   │   ├── java/com/example/sso/
│   │   │   ├── SsoApiApplication.java           # 應用程式入口
│   │   │   ├── config/
│   │   │   │   ├── SecurityConfig.java          # Spring Security 配置
│   │   │   │   ├── OpenApiConfig.java           # Swagger/OpenAPI 配置
│   │   │   │   └── CorsConfig.java              # CORS 配置
│   │   │   ├── controller/
│   │   │   │   ├── PublicController.java        # 公開 API
│   │   │   │   ├── UserController.java          # 使用者 API
│   │   │   │   └── AdminController.java         # 管理員 API
│   │   │   ├── service/
│   │   │   │   ├── UserService.java             # 使用者服務
│   │   │   │   └── AuditService.java            # 稽核服務
│   │   │   ├── dto/
│   │   │   │   ├── UserProfileDto.java          # 使用者資料 DTO
│   │   │   │   ├── ResourceDto.java             # 資源 DTO
│   │   │   │   └── ApiResponse.java             # 統一回應格式
│   │   │   ├── security/
│   │   │   │   ├── KeycloakRoleConverter.java   # Keycloak 角色轉換器
│   │   │   │   └── JwtAuditFilter.java          # JWT 稽核過濾器
│   │   │   └── exception/
│   │   │       ├── GlobalExceptionHandler.java  # 全域例外處理
│   │   │       └── ApiException.java            # 自訂例外
│   │   └── resources/
│   │       ├── application.yml                  # 主配置檔
│   │       ├── application-local.yml            # 本地開發配置
│   │       └── application-docker.yml           # Docker 環境配置
│   └── test/
│       └── java/com/example/sso/
│           ├── controller/
│           │   └── UserControllerTest.java
│           └── security/
│               └── SecurityConfigTest.java
├── Dockerfile
├── pom.xml
└── README.md
```

### 2.2 Maven 依賴 (pom.xml)

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 
         https://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>
    
    <parent>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-parent</artifactId>
        <version>3.3.5</version>
        <relativePath/>
    </parent>
    
    <groupId>com.example</groupId>
    <artifactId>sso-api</artifactId>
    <version>1.0.0-SNAPSHOT</version>
    <name>SSO RBAC PoC API</name>
    <description>Spring Boot API with Keycloak SSO and RBAC</description>
    
    <properties>
        <java.version>21</java.version>
    </properties>
    
    <dependencies>
        <!-- Spring Boot Web -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>
        
        <!-- Spring Security OAuth2 Resource Server -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-oauth2-resource-server</artifactId>
        </dependency>
        
        <!-- Spring Boot Actuator (Health Check) -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-actuator</artifactId>
        </dependency>
        
        <!-- Spring Boot Validation -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-validation</artifactId>
        </dependency>
        
        <!-- OpenAPI / Swagger -->
        <dependency>
            <groupId>org.springdoc</groupId>
            <artifactId>springdoc-openapi-starter-webmvc-ui</artifactId>
            <version>2.5.0</version>
        </dependency>
        
        <!-- Lombok (Optional) -->
        <dependency>
            <groupId>org.projectlombok</groupId>
            <artifactId>lombok</artifactId>
            <optional>true</optional>
        </dependency>
        
        <!-- Test Dependencies -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-test</artifactId>
            <scope>test</scope>
        </dependency>
        <dependency>
            <groupId>org.springframework.security</groupId>
            <artifactId>spring-security-test</artifactId>
            <scope>test</scope>
        </dependency>
    </dependencies>
    
    <build>
        <plugins>
            <plugin>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-maven-plugin</artifactId>
                <configuration>
                    <excludes>
                        <exclude>
                            <groupId>org.projectlombok</groupId>
                            <artifactId>lombok</artifactId>
                        </exclude>
                    </excludes>
                </configuration>
            </plugin>
        </plugins>
    </build>
</project>
```

---

## 3. 安全架構

### 3.1 認證流程

```
┌─────────┐                  ┌──────────┐                  ┌───────────┐
│ Client  │                  │ Keycloak │                  │ Spring API│
└────┬────┘                  └────┬─────┘                  └─────┬─────┘
     │                            │                              │
     │  1. Login (user/pass)      │                              │
     │───────────────────────────▶│                              │
     │                            │                              │
     │  2. JWT Tokens             │                              │
     │◀───────────────────────────│                              │
     │                            │                              │
     │  3. API Request            │                              │
     │  Authorization: Bearer xxx │                              │
     │───────────────────────────────────────────────────────────▶
     │                            │                              │
     │                            │  4. Fetch JWKS (cached)      │
     │                            │◀─────────────────────────────│
     │                            │                              │
     │                            │  5. Return Public Keys       │
     │                            │─────────────────────────────▶│
     │                            │                              │
     │                            │      6. Validate JWT         │
     │                            │      7. Extract Roles        │
     │                            │      8. Check Authorization  │
     │                            │                              │
     │  9. API Response           │                              │
     │◀──────────────────────────────────────────────────────────│
```

### 3.2 Spring Security 配置

```java
// src/main/java/com/example/sso/config/SecurityConfig.java
package com.example.sso.config;

import com.example.sso.security.KeycloakRoleConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
public class SecurityConfig {

    @Value("${spring.security.oauth2.resourceserver.jwt.jwk-set-uri}")
    private String jwkSetUri;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            // 停用 CSRF（Stateless API 不需要）
            .csrf(csrf -> csrf.disable())
            
            // Stateless Session
            .sessionManagement(session -> 
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            
            // 授權規則
            .authorizeHttpRequests(auth -> auth
                // 公開端點
                .requestMatchers(
                    "/api/public/**",
                    "/actuator/health",
                    "/actuator/info",
                    "/swagger-ui/**",
                    "/v3/api-docs/**"
                ).permitAll()
                
                // 管理員端點
                .requestMatchers("/api/admin/**").hasRole("ADMIN")
                
                // 使用者端點
                .requestMatchers("/api/user/**").hasAnyRole("USER", "ADMIN")
                
                // 其他請求需認證
                .anyRequest().authenticated()
            )
            
            // OAuth2 Resource Server 配置
            .oauth2ResourceServer(oauth2 -> oauth2
                .jwt(jwt -> jwt
                    .jwtAuthenticationConverter(jwtAuthenticationConverter())
                )
            );
        
        return http.build();
    }

    @Bean
    public JwtDecoder jwtDecoder() {
        return NimbusJwtDecoder.withJwkSetUri(jwkSetUri).build();
    }

    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(new KeycloakRoleConverter());
        return converter;
    }
}
```

### 3.3 Keycloak 角色轉換器

```java
// src/main/java/com/example/sso/security/KeycloakRoleConverter.java
package com.example.sso.security;

import org.springframework.core.convert.converter.Converter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class KeycloakRoleConverter implements Converter<Jwt, Collection<GrantedAuthority>> {

    private static final String REALM_ACCESS_CLAIM = "realm_access";
    private static final String RESOURCE_ACCESS_CLAIM = "resource_access";
    private static final String ROLES_KEY = "roles";
    private static final String ROLE_PREFIX = "ROLE_";

    @Override
    public Collection<GrantedAuthority> convert(Jwt jwt) {
        // 合併 Realm Roles 和 Resource Roles
        Set<GrantedAuthority> authorities = Stream.concat(
            extractRealmRoles(jwt).stream(),
            extractResourceRoles(jwt).stream()
        ).collect(Collectors.toSet());
        
        return authorities;
    }

    /**
     * 從 realm_access.roles 提取角色
     */
    @SuppressWarnings("unchecked")
    private Collection<GrantedAuthority> extractRealmRoles(Jwt jwt) {
        Map<String, Object> realmAccess = jwt.getClaimAsMap(REALM_ACCESS_CLAIM);
        
        if (realmAccess == null || !realmAccess.containsKey(ROLES_KEY)) {
            return Collections.emptyList();
        }
        
        List<String> roles = (List<String>) realmAccess.get(ROLES_KEY);
        return roles.stream()
            .filter(role -> !isDefaultRole(role))
            .map(role -> new SimpleGrantedAuthority(ROLE_PREFIX + role.toUpperCase()))
            .collect(Collectors.toList());
    }

    /**
     * 從 resource_access 提取 Client Roles（可選）
     */
    @SuppressWarnings("unchecked")
    private Collection<GrantedAuthority> extractResourceRoles(Jwt jwt) {
        Map<String, Object> resourceAccess = jwt.getClaimAsMap(RESOURCE_ACCESS_CLAIM);
        
        if (resourceAccess == null) {
            return Collections.emptyList();
        }
        
        List<GrantedAuthority> authorities = new ArrayList<>();
        
        resourceAccess.forEach((clientId, clientRoles) -> {
            if (clientRoles instanceof Map) {
                Map<String, Object> clientRolesMap = (Map<String, Object>) clientRoles;
                Object roles = clientRolesMap.get(ROLES_KEY);
                
                if (roles instanceof List) {
                    ((List<String>) roles).forEach(role -> 
                        authorities.add(new SimpleGrantedAuthority(
                            ROLE_PREFIX + clientId.toUpperCase() + "_" + role.toUpperCase()
                        ))
                    );
                }
            }
        });
        
        return authorities;
    }

    /**
     * 過濾 Keycloak 預設角色
     */
    private boolean isDefaultRole(String role) {
        return role.startsWith("default-roles-") || 
               role.equals("offline_access") || 
               role.equals("uma_authorization");
    }
}
```

---

## 4. API 設計

### 4.1 Controller 實作

```java
// src/main/java/com/example/sso/controller/PublicController.java
package com.example.sso.controller;

import com.example.sso.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.Map;

@RestController
@RequestMapping("/api/public")
@Tag(name = "Public", description = "公開 API，不需要認證")
public class PublicController {

    @GetMapping("/health")
    @Operation(summary = "健康檢查", description = "回傳服務健康狀態")
    public ApiResponse<Map<String, String>> health() {
        return ApiResponse.success(Map.of(
            "status", "UP",
            "timestamp", Instant.now().toString()
        ));
    }

    @GetMapping("/info")
    @Operation(summary = "系統資訊", description = "回傳系統基本資訊")
    public ApiResponse<Map<String, String>> info() {
        return ApiResponse.success(Map.of(
            "application", "SSO RBAC PoC API",
            "version", "1.0.0",
            "description", "Spring Boot API with Keycloak SSO"
        ));
    }
}
```

```java
// src/main/java/com/example/sso/controller/UserController.java
package com.example.sso.controller;

import com.example.sso.dto.ApiResponse;
import com.example.sso.dto.ResourceDto;
import com.example.sso.dto.UserProfileDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/user")
@Tag(name = "User", description = "使用者 API，需要 USER 或 ADMIN 角色")
@SecurityRequirement(name = "bearer-jwt")
public class UserController {

    @GetMapping("/profile")
    @Operation(summary = "取得個人資料", description = "從 JWT Token 取得使用者資訊")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ApiResponse<UserProfileDto> getProfile(@AuthenticationPrincipal Jwt jwt) {
        UserProfileDto profile = UserProfileDto.builder()
            .username(jwt.getClaimAsString("preferred_username"))
            .email(jwt.getClaimAsString("email"))
            .fullName(jwt.getClaimAsString("name"))
            .givenName(jwt.getClaimAsString("given_name"))
            .familyName(jwt.getClaimAsString("family_name"))
            .roles(extractRoles(jwt))
            .tokenIssuedAt(jwt.getIssuedAt())
            .tokenExpiresAt(jwt.getExpiresAt())
            .build();
        
        return ApiResponse.success(profile);
    }

    @GetMapping("/resources")
    @Operation(summary = "取得資源列表", description = "取得使用者可存取的資源")
    public ApiResponse<List<ResourceDto>> getResources() {
        List<ResourceDto> resources = List.of(
            new ResourceDto("RES-001", "Sales Report Q4", "document", "read"),
            new ResourceDto("RES-002", "Marketing Dashboard", "dashboard", "read"),
            new ResourceDto("RES-003", "Customer Database", "database", "read")
        );
        return ApiResponse.success(resources);
    }

    @SuppressWarnings("unchecked")
    private List<String> extractRoles(Jwt jwt) {
        Map<String, Object> realmAccess = jwt.getClaimAsMap("realm_access");
        if (realmAccess != null && realmAccess.containsKey("roles")) {
            return ((List<String>) realmAccess.get("roles")).stream()
                .filter(role -> !role.startsWith("default-roles-"))
                .toList();
        }
        return List.of();
    }
}
```

```java
// src/main/java/com/example/sso/controller/AdminController.java
package com.example.sso.controller;

import com.example.sso.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
@Tag(name = "Admin", description = "管理員 API，需要 ADMIN 角色")
@SecurityRequirement(name = "bearer-jwt")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    @GetMapping("/users")
    @Operation(summary = "取得使用者列表", description = "列出所有系統使用者")
    public ApiResponse<List<Map<String, String>>> listUsers() {
        List<Map<String, String>> users = List.of(
            Map.of("id", "1", "username", "admin.user", "email", "admin@example.com", "role", "ADMIN"),
            Map.of("id", "2", "username", "normal.user", "email", "user@example.com", "role", "USER"),
            Map.of("id", "3", "username", "auditor.user", "email", "auditor@example.com", "role", "AUDITOR")
        );
        return ApiResponse.success(users);
    }

    @GetMapping("/roles")
    @Operation(summary = "取得角色列表", description = "列出所有可用角色")
    public ApiResponse<List<Map<String, Object>>> listRoles() {
        List<Map<String, Object>> roles = List.of(
            Map.of("name", "ADMIN", "description", "系統管理員", "permissions", List.of("*")),
            Map.of("name", "USER", "description", "一般使用者", "permissions", List.of("read:profile", "read:resources")),
            Map.of("name", "AUDITOR", "description", "稽核人員", "permissions", List.of("read:logs", "read:reports"))
        );
        return ApiResponse.success(roles);
    }

    @GetMapping("/settings")
    @Operation(summary = "取得系統設定", description = "取得系統配置資訊")
    public ApiResponse<Map<String, Object>> getSettings() {
        return ApiResponse.success(Map.of(
            "maxConcurrentUsers", 100,
            "sessionTimeout", 3600,
            "passwordPolicy", Map.of(
                "minLength", 8,
                "requireUppercase", true,
                "requireNumbers", true,
                "requireSpecialChars", true
            ),
            "auditEnabled", true,
            "features", List.of("SSO", "RBAC", "MFA", "AUDIT_LOG")
        ));
    }

    @GetMapping("/audit-logs")
    @Operation(summary = "取得稽核日誌", description = "查詢系統稽核記錄")
    public ApiResponse<List<Map<String, String>>> getAuditLogs(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        List<Map<String, String>> logs = List.of(
            Map.of("timestamp", "2025-01-10T10:30:00Z", "user", "admin.user", 
                   "action", "LOGIN", "result", "SUCCESS", "ip", "192.168.1.100"),
            Map.of("timestamp", "2025-01-10T10:31:00Z", "user", "admin.user", 
                   "action", "VIEW_USERS", "result", "SUCCESS", "ip", "192.168.1.100"),
            Map.of("timestamp", "2025-01-10T10:35:00Z", "user", "normal.user", 
                   "action", "LOGIN", "result", "SUCCESS", "ip", "192.168.1.101")
        );
        return ApiResponse.success(logs);
    }
}
```

### 4.2 DTO 類別

```java
// src/main/java/com/example/sso/dto/ApiResponse.java
package com.example.sso.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {
    private boolean success;
    private String message;
    private T data;
    private String error;
    private String timestamp;

    public static <T> ApiResponse<T> success(T data) {
        return ApiResponse.<T>builder()
            .success(true)
            .data(data)
            .timestamp(Instant.now().toString())
            .build();
    }

    public static <T> ApiResponse<T> success(T data, String message) {
        return ApiResponse.<T>builder()
            .success(true)
            .message(message)
            .data(data)
            .timestamp(Instant.now().toString())
            .build();
    }

    public static <T> ApiResponse<T> error(String error) {
        return ApiResponse.<T>builder()
            .success(false)
            .error(error)
            .timestamp(Instant.now().toString())
            .build();
    }

    public static <T> ApiResponse<T> error(String error, String message) {
        return ApiResponse.<T>builder()
            .success(false)
            .error(error)
            .message(message)
            .timestamp(Instant.now().toString())
            .build();
    }
}
```

```java
// src/main/java/com/example/sso/dto/UserProfileDto.java
package com.example.sso.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserProfileDto {
    private String username;
    private String email;
    private String fullName;
    private String givenName;
    private String familyName;
    private List<String> roles;
    private Instant tokenIssuedAt;
    private Instant tokenExpiresAt;
}
```

```java
// src/main/java/com/example/sso/dto/ResourceDto.java
package com.example.sso.dto;

public record ResourceDto(
    String id,
    String name,
    String type,
    String permission
) {}
```

---

## 5. 配置檔案

### 5.1 application.yml

```yaml
# src/main/resources/application.yml
server:
  port: 9090
  servlet:
    context-path: /

spring:
  application:
    name: sso-rbac-api

  security:
    oauth2:
      resourceserver:
        jwt:
          # Keycloak JWKS 端點
          jwk-set-uri: ${KEYCLOAK_JWKS_URI:http://localhost:8080/realms/demo/protocol/openid-connect/certs}
          # Issuer 驗證
          issuer-uri: ${KEYCLOAK_ISSUER_URI:http://localhost:8080/realms/demo}

# Actuator 配置
management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics
  endpoint:
    health:
      show-details: when_authorized
  info:
    env:
      enabled: true

# OpenAPI 配置
springdoc:
  api-docs:
    path: /v3/api-docs
  swagger-ui:
    path: /swagger-ui.html
    operations-sorter: method
    tags-sorter: alpha

# 日誌配置
logging:
  level:
    root: INFO
    com.example.sso: DEBUG
    org.springframework.security: DEBUG
  pattern:
    console: "%d{yyyy-MM-dd HH:mm:ss} [%thread] %-5level %logger{36} - %msg%n"
```

### 5.2 application-docker.yml

```yaml
# src/main/resources/application-docker.yml
spring:
  security:
    oauth2:
      resourceserver:
        jwt:
          jwk-set-uri: http://keycloak:8080/realms/demo/protocol/openid-connect/certs
          issuer-uri: http://keycloak:8080/realms/demo

logging:
  level:
    root: INFO
    com.example.sso: INFO
    org.springframework.security: WARN
```

---

## 6. 例外處理

```java
// src/main/java/com/example/sso/exception/GlobalExceptionHandler.java
package com.example.sso.exception;

import com.example.sso.dto.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ApiResponse<Void>> handleAuthenticationException(AuthenticationException ex) {
        log.warn("Authentication failed: {}", ex.getMessage());
        return ResponseEntity
            .status(HttpStatus.UNAUTHORIZED)
            .body(ApiResponse.error("UNAUTHORIZED", "認證失敗，請提供有效的 Token"));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponse<Void>> handleAccessDeniedException(AccessDeniedException ex) {
        log.warn("Access denied: {}", ex.getMessage());
        return ResponseEntity
            .status(HttpStatus.FORBIDDEN)
            .body(ApiResponse.error("FORBIDDEN", "權限不足，無法存取此資源"));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleGenericException(Exception ex) {
        log.error("Unexpected error: ", ex);
        return ResponseEntity
            .status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(ApiResponse.error("INTERNAL_ERROR", "系統發生錯誤，請稍後再試"));
    }
}
```

---

## 7. Dockerfile

```dockerfile
# Dockerfile
FROM eclipse-temurin:21-jdk-alpine AS builder

WORKDIR /app

# 複製 Maven 配置
COPY pom.xml .
COPY mvnw .
COPY .mvn .mvn

# 下載依賴（快取層）
RUN ./mvnw dependency:go-offline -B

# 複製原始碼並建置
COPY src src
RUN ./mvnw package -DskipTests -B

# 執行階段
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# 建立非 root 使用者
RUN addgroup -g 1001 appgroup && \
    adduser -u 1001 -G appgroup -D appuser

# 複製執行檔
COPY --from=builder /app/target/*.jar app.jar

# 設定權限
RUN chown -R appuser:appgroup /app
USER appuser

# Health Check
HEALTHCHECK --interval=30s --timeout=3s --start-period=60s --retries=3 \
    CMD wget --no-verbose --tries=1 --spider http://localhost:9090/actuator/health || exit 1

EXPOSE 9090

ENTRYPOINT ["java", "-jar", "app.jar"]
```

---

## 8. 測試策略

### 8.1 單元測試範例

```java
// src/test/java/com/example/sso/controller/UserControllerTest.java
package com.example.sso.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void publicEndpoint_shouldBeAccessibleWithoutAuth() throws Exception {
        mockMvc.perform(get("/api/public/health"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void userEndpoint_withoutAuth_shouldReturn401() throws Exception {
        mockMvc.perform(get("/api/user/resources"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "USER")
    void userEndpoint_withUserRole_shouldReturn200() throws Exception {
        mockMvc.perform(get("/api/user/resources"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @WithMockUser(roles = "USER")
    void adminEndpoint_withUserRole_shouldReturn403() throws Exception {
        mockMvc.perform(get("/api/admin/users"))
            .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void adminEndpoint_withAdminRole_shouldReturn200() throws Exception {
        mockMvc.perform(get("/api/admin/users"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true));
    }
}
```

---

## 9. API 文件

### 9.1 Swagger/OpenAPI 配置

```java
// src/main/java/com/example/sso/config/OpenApiConfig.java
package com.example.sso.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
            .info(new Info()
                .title("SSO RBAC PoC API")
                .version("1.0.0")
                .description("Spring Boot API with Keycloak SSO and Role-Based Access Control")
                .contact(new Contact()
                    .name("Architecture Team")
                    .email("arch@example.com")))
            .components(new Components()
                .addSecuritySchemes("bearer-jwt", new SecurityScheme()
                    .type(SecurityScheme.Type.HTTP)
                    .scheme("bearer")
                    .bearerFormat("JWT")
                    .description("輸入從 Keycloak 取得的 JWT Access Token")));
    }
}
```

### 9.2 API 端點總覽

| Method | Endpoint | 說明 | 權限 |
|--------|----------|------|------|
| GET | `/api/public/health` | 健康檢查 | 公開 |
| GET | `/api/public/info` | 系統資訊 | 公開 |
| GET | `/api/user/profile` | 個人資料 | USER, ADMIN |
| GET | `/api/user/resources` | 資源列表 | USER, ADMIN |
| GET | `/api/admin/users` | 使用者列表 | ADMIN |
| GET | `/api/admin/roles` | 角色列表 | ADMIN |
| GET | `/api/admin/settings` | 系統設定 | ADMIN |
| GET | `/api/admin/audit-logs` | 稽核日誌 | ADMIN |

---

## 10. 部署檢查清單

### 10.1 開發環境

- [ ] JDK 21 已安裝
- [ ] Maven 3.9+ 已安裝
- [ ] IDE 已配置 Lombok 支援
- [ ] Keycloak 服務可存取

### 10.2 Docker 環境

- [ ] Docker 24+ 已安裝
- [ ] Docker Compose v2 已安裝
- [ ] 網路配置正確（sso-network）
- [ ] 環境變數已設定

### 10.3 安全檢查

- [ ] JWT 簽章驗證正常
- [ ] RBAC 權限控制正確
- [ ] 敏感端點已保護
- [ ] CORS 配置適當
