# RBAC-SSO-POC Repository 分析報告

**檢查日期：** 2026-01-10  
**Repository：** https://github.com/ChunPingWang/rbac-sso-poc.git  
**分析目的：** 確認是否實作 KeyCloak LDAP 的 RBAC 與 SSO 功能

---

## 執行摘要

### 結論：❌ 尚未實作 KeyCloak LDAP 的 RBAC 與 SSO 功能

雖然專案名稱為 **"rbac-sso-poc"**，但目前兩個分支實際實作的是 **「Shared Audit Library (共用稽核函式庫)」**，而非 Keycloak LDAP 整合的 RBAC/SSO 功能。

---

## 分支資訊

| 分支 | 說明 | KeyCloak LDAP 實作 |
|------|------|:------------------:|
| `main` | 主分支，包含 AOP 與 Domain Event 雙機制 | ❌ 無 |
| `domain-event-for-audit` | Domain Event 稽核機制開發分支 | ❌ 無 |

---

## 目前專案實際包含的功能

### ✅ 已實作功能

#### 1. 稽核機制 (Audit Library)
- `@Auditable` AOP 註解式稽核
- `AuditEventPublisher` Domain Event 程式化稽核
- 稽核日誌自動記錄（操作者、時間戳記、Client IP、執行結果）
- 敏感資料遮罩功能
- Payload 大小限制與截斷處理

#### 2. 基礎架構
- Hexagonal Architecture (六角架構) 設計
- Spring Boot 3.3.x
- Spring Data JPA 3.3.x
- H2 (開發環境) / PostgreSQL (生產環境)
- Gradle 8.5 建置工具

#### 3. 可觀測性
- Micrometer metrics 整合
- Health Indicators
- Actuator endpoints

#### 4. 基礎安全性框架
- Spring Security OAuth2 基礎配置
- `SecurityAutoConfiguration.java`
- `ServiceAuthConfiguration.java`
- North-South 與 East-West 安全控制框架（僅框架，非完整實作）

---

## KeyCloak LDAP RBAC/SSO 功能檢查

### ❌ 缺少的功能

| 功能項目 | 狀態 | 說明 |
|----------|:----:|------|
| Keycloak Server 配置 | ❌ | 無 Keycloak Docker 或配置檔 |
| Keycloak Realm 設定 | ❌ | 無 Realm JSON 導入/導出檔 |
| LDAP User Federation | ❌ | 無 LDAP 與 Keycloak 整合配置 |
| OpenID Connect 整合 | ❌ | 無 OIDC 客戶端配置 |
| JWT Token 驗證 | ❌ | 無 Keycloak JWT 驗證邏輯 |
| Role Mapping (RBAC) | ❌ | 無角色映射實作 |
| SSO Session 管理 | ❌ | 無 SSO 會話管理 |
| `@PreAuthorize` 權限控制 | ❌ | 無方法級權限註解使用 |

---

## 專案結構分析

```
rbac-sso-poc/
├── libs/
│   └── audit-lib/                    # ← 主要實作內容：稽核函式庫
│       └── src/main/java/com/example/audit/
│           ├── annotation/           # @Auditable 註解
│           ├── domain/               # 領域模型
│           ├── application/          # 應用服務
│           └── infrastructure/       # 基礎設施
│               ├── aspect/           # AOP 切面
│               ├── event/            # Domain Event
│               ├── persistence/      # JPA Repository
│               ├── security/         # 基礎安全配置 (非 Keycloak)
│               └── metrics/          # Micrometer
├── infra/
│   └── ldap/                         # LDAP 目錄 (內容不明，可能為規劃中)
├── deploy/                           # 部署配置
├── specs/
│   └── 001-shared-audit-lib/         # 稽核函式庫規格文件
├── PRD.md                            # 產品需求文件
├── TECH.md                           # 技術文件
├── INFRA.md                          # 基礎設施文件
└── README.md                         # 專案說明
```

### 關鍵觀察

1. **`libs/audit-lib/`** 是專案的核心，完全聚焦於稽核功能
2. **`infra/ldap/`** 目錄存在，但從整體專案來看並未整合到應用程式中
3. **無 Keycloak 相關配置檔**（如 `realm-export.json`、`docker-compose.yml` 中的 keycloak 服務）
4. **`specs/` 目錄僅包含稽核函式庫規格**，無 SSO/RBAC 相關規格

---

## 技術堆疊對照

### 目前使用的技術

| 類別 | 技術 | 版本 |
|------|------|------|
| 語言 | Java | 17 (LTS) |
| 框架 | Spring Boot | 3.3.x |
| AOP | Spring AOP | 6.1.x |
| 事件 | Spring Events | 6.1.x |
| 資料存取 | Spring Data JPA | 3.3.x |
| 安全性 | Spring Security OAuth2 | 6.x |
| 監控 | Micrometer | 1.12.x |

### 實作 KeyCloak LDAP RBAC/SSO 需要新增的技術

| 類別 | 技術 | 說明 |
|------|------|------|
| Identity Provider | Keycloak | 21.x 或更新版本 |
| User Federation | OpenLDAP / Active Directory | LDAP 目錄服務 |
| 協定 | OpenID Connect / OAuth 2.0 | 身份驗證協定 |
| Spring 整合 | spring-boot-starter-oauth2-resource-server | JWT 驗證 |
| 容器化 | Docker Compose | Keycloak + LDAP 服務編排 |

---

# KeyCloak LDAP RBAC/SSO 實作建議與方案

## 實作架構概覽

```
┌─────────────────────────────────────────────────────────────────────┐
│                        整體架構圖                                    │
├─────────────────────────────────────────────────────────────────────┤
│                                                                     │
│  ┌──────────┐     ┌──────────────┐     ┌─────────────────────────┐ │
│  │  Client  │────▶│   Keycloak   │────▶│   OpenLDAP / AD         │ │
│  │  (SPA/   │     │   (IdP)      │     │   (User Federation)     │ │
│  │   App)   │◀────│              │     │                         │ │
│  └──────────┘     └──────────────┘     └─────────────────────────┘ │
│       │                  │                                         │
│       │ JWT Token        │ OIDC/OAuth2                             │
│       ▼                  ▼                                         │
│  ┌─────────────────────────────────────────────────────────────┐   │
│  │              Spring Boot Application                         │   │
│  │  ┌─────────────────┐  ┌─────────────────┐  ┌──────────────┐ │   │
│  │  │ Security Filter │  │ RBAC Controller │  │ Audit Lib    │ │   │
│  │  │ (JWT Validation)│  │ (@PreAuthorize) │  │ (已實作)      │ │   │
│  │  └─────────────────┘  └─────────────────┘  └──────────────┘ │   │
│  └─────────────────────────────────────────────────────────────┘   │
│                                                                     │
└─────────────────────────────────────────────────────────────────────┘
```

---

## 實作步驟總覽

| 階段 | 步驟 | 預估時間 |
|------|------|----------|
| Phase 1 | 基礎設施建置 (Docker Compose) | 2-3 天 |
| Phase 2 | Keycloak Realm 與 LDAP 配置 | 2-3 天 |
| Phase 3 | Spring Security 整合 | 3-4 天 |
| Phase 4 | RBAC 權限控制實作 | 2-3 天 |
| Phase 5 | 測試與驗證 | 2-3 天 |
| **總計** | | **11-16 天** |

---

## Phase 1: 基礎設施建置

### 1.1 建立專案目錄結構

```
rbac-sso-poc/
├── infra/
│   ├── keycloak/
│   │   ├── realm-export.json          # Keycloak Realm 配置
│   │   └── themes/                    # 自訂主題 (選用)
│   ├── ldap/
│   │   ├── bootstrap.ldif             # LDAP 初始資料
│   │   └── schema/                    # 自訂 Schema (選用)
│   └── docker-compose.yml             # 服務編排
```

### 1.2 Docker Compose 配置

建立 `infra/docker-compose.yml`：

```yaml
version: '3.8'

services:
  # ============================================
  # PostgreSQL - Keycloak 資料庫
  # ============================================
  postgres:
    image: postgres:15-alpine
    container_name: keycloak-postgres
    environment:
      POSTGRES_DB: keycloak
      POSTGRES_USER: keycloak
      POSTGRES_PASSWORD: keycloak_password
    volumes:
      - postgres_data:/var/lib/postgresql/data
    networks:
      - rbac-sso-network
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U keycloak"]
      interval: 10s
      timeout: 5s
      retries: 5

  # ============================================
  # OpenLDAP - 使用者目錄服務
  # ============================================
  openldap:
    image: osixia/openldap:1.5.0
    container_name: openldap
    environment:
      LDAP_ORGANISATION: "Example Corp"
      LDAP_DOMAIN: "example.com"
      LDAP_ADMIN_PASSWORD: "admin_password"
      LDAP_CONFIG_PASSWORD: "config_password"
      LDAP_READONLY_USER: "true"
      LDAP_READONLY_USER_USERNAME: "readonly"
      LDAP_READONLY_USER_PASSWORD: "readonly_password"
    ports:
      - "389:389"
      - "636:636"
    volumes:
      - ldap_data:/var/lib/ldap
      - ldap_config:/etc/ldap/slapd.d
      - ./ldap/bootstrap.ldif:/container/service/slapd/assets/config/bootstrap/ldif/custom/bootstrap.ldif
    networks:
      - rbac-sso-network
    healthcheck:
      test: ["CMD", "ldapsearch", "-x", "-H", "ldap://localhost", "-b", "dc=example,dc=com"]
      interval: 30s
      timeout: 10s
      retries: 3

  # ============================================
  # phpLDAPadmin - LDAP 管理介面
  # ============================================
  ldap-admin:
    image: osixia/phpldapadmin:0.9.0
    container_name: ldap-admin
    environment:
      PHPLDAPADMIN_LDAP_HOSTS: openldap
      PHPLDAPADMIN_HTTPS: "false"
    ports:
      - "8081:80"
    depends_on:
      - openldap
    networks:
      - rbac-sso-network

  # ============================================
  # Keycloak - Identity Provider
  # ============================================
  keycloak:
    image: quay.io/keycloak/keycloak:23.0
    container_name: keycloak
    command:
      - start-dev
      - --import-realm
    environment:
      KC_DB: postgres
      KC_DB_URL: jdbc:postgresql://postgres:5432/keycloak
      KC_DB_USERNAME: keycloak
      KC_DB_PASSWORD: keycloak_password
      KEYCLOAK_ADMIN: admin
      KEYCLOAK_ADMIN_PASSWORD: admin_password
      KC_HEALTH_ENABLED: "true"
      KC_METRICS_ENABLED: "true"
    ports:
      - "8080:8080"
    volumes:
      - ./keycloak/realm-export.json:/opt/keycloak/data/import/realm-export.json
    depends_on:
      postgres:
        condition: service_healthy
      openldap:
        condition: service_healthy
    networks:
      - rbac-sso-network
    healthcheck:
      test: ["CMD-SHELL", "exec 3<>/dev/tcp/127.0.0.1/8080;echo -e 'GET /health/ready HTTP/1.1\r\nhost: localhost\r\nConnection: close\r\n\r\n' >&3;if [ $? -eq 0 ]; then echo 'Healthcheck Successful';exit 0;else echo 'Healthcheck Failed';exit 1;fi;"]
      interval: 30s
      timeout: 10s
      retries: 5

networks:
  rbac-sso-network:
    driver: bridge

volumes:
  postgres_data:
  ldap_data:
  ldap_config:
```

### 1.3 LDAP 初始資料

建立 `infra/ldap/bootstrap.ldif`：

```ldif
# ============================================
# 組織單位 (Organizational Units)
# ============================================
dn: ou=users,dc=example,dc=com
objectClass: organizationalUnit
ou: users
description: All users

dn: ou=groups,dc=example,dc=com
objectClass: organizationalUnit
ou: groups
description: All groups

dn: ou=services,dc=example,dc=com
objectClass: organizationalUnit
ou: services
description: Service accounts

# ============================================
# 群組定義 (Groups)
# ============================================
dn: cn=admins,ou=groups,dc=example,dc=com
objectClass: groupOfNames
cn: admins
description: System Administrators
member: uid=admin,ou=users,dc=example,dc=com

dn: cn=developers,ou=groups,dc=example,dc=com
objectClass: groupOfNames
cn: developers
description: Development Team
member: uid=dev1,ou=users,dc=example,dc=com
member: uid=dev2,ou=users,dc=example,dc=com

dn: cn=viewers,ou=groups,dc=example,dc=com
objectClass: groupOfNames
cn: viewers
description: Read-only Users
member: uid=viewer1,ou=users,dc=example,dc=com

# ============================================
# 使用者定義 (Users)
# ============================================
dn: uid=admin,ou=users,dc=example,dc=com
objectClass: inetOrgPerson
objectClass: posixAccount
objectClass: shadowAccount
uid: admin
sn: Administrator
givenName: System
cn: System Administrator
displayName: System Administrator
mail: admin@example.com
uidNumber: 1000
gidNumber: 1000
homeDirectory: /home/admin
userPassword: {SSHA}encoded_password_here

dn: uid=dev1,ou=users,dc=example,dc=com
objectClass: inetOrgPerson
objectClass: posixAccount
objectClass: shadowAccount
uid: dev1
sn: Developer
givenName: John
cn: John Developer
displayName: John Developer
mail: dev1@example.com
uidNumber: 1001
gidNumber: 1001
homeDirectory: /home/dev1
userPassword: {SSHA}encoded_password_here

dn: uid=dev2,ou=users,dc=example,dc=com
objectClass: inetOrgPerson
objectClass: posixAccount
objectClass: shadowAccount
uid: dev2
sn: Developer
givenName: Jane
cn: Jane Developer
displayName: Jane Developer
mail: dev2@example.com
uidNumber: 1002
gidNumber: 1002
homeDirectory: /home/dev2
userPassword: {SSHA}encoded_password_here

dn: uid=viewer1,ou=users,dc=example,dc=com
objectClass: inetOrgPerson
objectClass: posixAccount
objectClass: shadowAccount
uid: viewer1
sn: Viewer
givenName: Bob
cn: Bob Viewer
displayName: Bob Viewer
mail: viewer1@example.com
uidNumber: 1003
gidNumber: 1003
homeDirectory: /home/viewer1
userPassword: {SSHA}encoded_password_here
```

### 1.4 啟動基礎設施

```bash
# 進入 infra 目錄
cd infra

# 啟動所有服務
docker-compose up -d

# 檢查服務狀態
docker-compose ps

# 查看日誌
docker-compose logs -f keycloak
```

---

## Phase 2: Keycloak Realm 與 LDAP 配置

### 2.1 Keycloak Realm 配置檔

建立 `infra/keycloak/realm-export.json`：

```json
{
  "realm": "rbac-sso-realm",
  "enabled": true,
  "displayName": "RBAC SSO POC",
  "displayNameHtml": "<strong>RBAC SSO POC</strong>",
  "sslRequired": "external",
  "registrationAllowed": false,
  "loginWithEmailAllowed": true,
  "duplicateEmailsAllowed": false,
  "resetPasswordAllowed": true,
  "editUsernameAllowed": false,
  "bruteForceProtected": true,
  "permanentLockout": false,
  "maxFailureWaitSeconds": 900,
  "minimumQuickLoginWaitSeconds": 60,
  "waitIncrementSeconds": 60,
  "quickLoginCheckMilliSeconds": 1000,
  "maxDeltaTimeSeconds": 43200,
  "failureFactor": 5,
  "accessTokenLifespan": 300,
  "accessTokenLifespanForImplicitFlow": 900,
  "ssoSessionIdleTimeout": 1800,
  "ssoSessionMaxLifespan": 36000,
  "offlineSessionIdleTimeout": 2592000,
  "accessCodeLifespan": 60,
  "accessCodeLifespanUserAction": 300,
  "accessCodeLifespanLogin": 1800,
  "actionTokenGeneratedByAdminLifespan": 43200,
  "actionTokenGeneratedByUserLifespan": 300,
  "roles": {
    "realm": [
      {
        "name": "ROLE_ADMIN",
        "description": "Administrator role with full access",
        "composite": false
      },
      {
        "name": "ROLE_DEVELOPER",
        "description": "Developer role with read-write access",
        "composite": false
      },
      {
        "name": "ROLE_VIEWER",
        "description": "Viewer role with read-only access",
        "composite": false
      }
    ],
    "client": {
      "rbac-sso-app": [
        {
          "name": "product:read",
          "description": "Can read products"
        },
        {
          "name": "product:write",
          "description": "Can create/update products"
        },
        {
          "name": "product:delete",
          "description": "Can delete products"
        },
        {
          "name": "user:read",
          "description": "Can read users"
        },
        {
          "name": "user:write",
          "description": "Can manage users"
        },
        {
          "name": "audit:read",
          "description": "Can read audit logs"
        }
      ]
    }
  },
  "defaultRoles": ["ROLE_VIEWER"],
  "clients": [
    {
      "clientId": "rbac-sso-app",
      "name": "RBAC SSO Application",
      "description": "Main application client",
      "enabled": true,
      "clientAuthenticatorType": "client-secret",
      "secret": "your-client-secret-here",
      "redirectUris": [
        "http://localhost:8082/*",
        "http://localhost:3000/*"
      ],
      "webOrigins": [
        "http://localhost:8082",
        "http://localhost:3000"
      ],
      "publicClient": false,
      "protocol": "openid-connect",
      "bearerOnly": false,
      "standardFlowEnabled": true,
      "implicitFlowEnabled": false,
      "directAccessGrantsEnabled": true,
      "serviceAccountsEnabled": true,
      "authorizationServicesEnabled": true,
      "fullScopeAllowed": true,
      "defaultClientScopes": [
        "web-origins",
        "acr",
        "roles",
        "profile",
        "email"
      ],
      "optionalClientScopes": [
        "address",
        "phone",
        "offline_access",
        "microprofile-jwt"
      ],
      "protocolMappers": [
        {
          "name": "realm-roles-mapper",
          "protocol": "openid-connect",
          "protocolMapper": "oidc-usermodel-realm-role-mapper",
          "consentRequired": false,
          "config": {
            "multivalued": "true",
            "userinfo.token.claim": "true",
            "id.token.claim": "true",
            "access.token.claim": "true",
            "claim.name": "realm_roles",
            "jsonType.label": "String"
          }
        },
        {
          "name": "client-roles-mapper",
          "protocol": "openid-connect",
          "protocolMapper": "oidc-usermodel-client-role-mapper",
          "consentRequired": false,
          "config": {
            "multivalued": "true",
            "userinfo.token.claim": "true",
            "id.token.claim": "true",
            "access.token.claim": "true",
            "claim.name": "resource_access.${client_id}.roles",
            "jsonType.label": "String"
          }
        },
        {
          "name": "groups-mapper",
          "protocol": "openid-connect",
          "protocolMapper": "oidc-group-membership-mapper",
          "consentRequired": false,
          "config": {
            "full.path": "false",
            "id.token.claim": "true",
            "access.token.claim": "true",
            "claim.name": "groups",
            "userinfo.token.claim": "true"
          }
        }
      ]
    },
    {
      "clientId": "rbac-sso-service",
      "name": "Service Account Client",
      "description": "For service-to-service communication",
      "enabled": true,
      "clientAuthenticatorType": "client-secret",
      "secret": "service-client-secret-here",
      "publicClient": false,
      "protocol": "openid-connect",
      "bearerOnly": false,
      "standardFlowEnabled": false,
      "implicitFlowEnabled": false,
      "directAccessGrantsEnabled": false,
      "serviceAccountsEnabled": true,
      "authorizationServicesEnabled": false
    }
  ],
  "components": {
    "org.keycloak.storage.UserStorageProvider": [
      {
        "name": "ldap-federation",
        "providerId": "ldap",
        "subComponents": {
          "org.keycloak.storage.ldap.mappers.LDAPStorageMapper": [
            {
              "name": "username",
              "providerId": "user-attribute-ldap-mapper",
              "subComponents": {},
              "config": {
                "ldap.attribute": ["uid"],
                "is.mandatory.in.ldap": ["true"],
                "always.read.value.from.ldap": ["false"],
                "read.only": ["true"],
                "user.model.attribute": ["username"]
              }
            },
            {
              "name": "email",
              "providerId": "user-attribute-ldap-mapper",
              "subComponents": {},
              "config": {
                "ldap.attribute": ["mail"],
                "is.mandatory.in.ldap": ["false"],
                "always.read.value.from.ldap": ["false"],
                "read.only": ["true"],
                "user.model.attribute": ["email"]
              }
            },
            {
              "name": "first name",
              "providerId": "user-attribute-ldap-mapper",
              "subComponents": {},
              "config": {
                "ldap.attribute": ["givenName"],
                "is.mandatory.in.ldap": ["true"],
                "always.read.value.from.ldap": ["true"],
                "read.only": ["true"],
                "user.model.attribute": ["firstName"]
              }
            },
            {
              "name": "last name",
              "providerId": "user-attribute-ldap-mapper",
              "subComponents": {},
              "config": {
                "ldap.attribute": ["sn"],
                "is.mandatory.in.ldap": ["true"],
                "always.read.value.from.ldap": ["true"],
                "read.only": ["true"],
                "user.model.attribute": ["lastName"]
              }
            },
            {
              "name": "groups",
              "providerId": "group-ldap-mapper",
              "subComponents": {},
              "config": {
                "groups.dn": ["ou=groups,dc=example,dc=com"],
                "group.name.ldap.attribute": ["cn"],
                "group.object.classes": ["groupOfNames"],
                "preserve.group.inheritance": ["false"],
                "membership.ldap.attribute": ["member"],
                "membership.attribute.type": ["DN"],
                "membership.user.ldap.attribute": ["uid"],
                "mode": ["READ_ONLY"],
                "user.roles.retrieve.strategy": ["LOAD_GROUPS_BY_MEMBER_ATTRIBUTE"],
                "drop.non.existing.groups.during.sync": ["false"]
              }
            }
          ]
        },
        "config": {
          "enabled": ["true"],
          "priority": ["0"],
          "fullSyncPeriod": ["-1"],
          "changedSyncPeriod": ["-1"],
          "cachePolicy": ["DEFAULT"],
          "editMode": ["READ_ONLY"],
          "importEnabled": ["true"],
          "syncRegistrations": ["false"],
          "vendor": ["other"],
          "usernameLDAPAttribute": ["uid"],
          "rdnLDAPAttribute": ["uid"],
          "uuidLDAPAttribute": ["entryUUID"],
          "userObjectClasses": ["inetOrgPerson, posixAccount"],
          "connectionUrl": ["ldap://openldap:389"],
          "usersDn": ["ou=users,dc=example,dc=com"],
          "authType": ["simple"],
          "bindDn": ["cn=admin,dc=example,dc=com"],
          "bindCredential": ["admin_password"],
          "searchScope": ["1"],
          "validatePasswordPolicy": ["false"],
          "trustEmail": ["false"],
          "useTruststoreSpi": ["ldapsOnly"],
          "connectionPooling": ["true"],
          "connectionPoolingAuthentication": ["simple"],
          "connectionPoolingDebug": ["off"],
          "connectionPoolingInitSize": ["1"],
          "connectionPoolingMaxSize": ["10"],
          "connectionPoolingPrefSize": ["5"],
          "connectionPoolingProtocol": ["plain"],
          "connectionPoolingTimeout": ["300000"],
          "connectionTimeout": ["30000"],
          "readTimeout": ["30000"],
          "pagination": ["true"],
          "batchSizeForSync": ["1000"],
          "allowKerberosAuthentication": ["false"],
          "useKerberosForPasswordAuthentication": ["false"]
        }
      }
    ]
  },
  "groups": [
    {
      "name": "admins",
      "realmRoles": ["ROLE_ADMIN"],
      "clientRoles": {
        "rbac-sso-app": ["product:read", "product:write", "product:delete", "user:read", "user:write", "audit:read"]
      }
    },
    {
      "name": "developers",
      "realmRoles": ["ROLE_DEVELOPER"],
      "clientRoles": {
        "rbac-sso-app": ["product:read", "product:write", "user:read", "audit:read"]
      }
    },
    {
      "name": "viewers",
      "realmRoles": ["ROLE_VIEWER"],
      "clientRoles": {
        "rbac-sso-app": ["product:read", "user:read"]
      }
    }
  ]
}
```

### 2.2 手動配置 LDAP User Federation (替代方案)

如果 JSON 導入有問題，可透過 Keycloak Admin Console 手動配置：

1. 登入 Keycloak Admin Console: `http://localhost:8080/admin`
2. 選擇或建立 Realm
3. 進入 **User Federation** → **Add Provider** → **ldap**
4. 填入以下配置：

| 設定項目 | 值 |
|----------|-----|
| Console Display Name | ldap-federation |
| Vendor | Other |
| Connection URL | ldap://openldap:389 |
| Users DN | ou=users,dc=example,dc=com |
| Bind DN | cn=admin,dc=example,dc=com |
| Bind Credential | admin_password |
| Edit Mode | READ_ONLY |
| Username LDAP attribute | uid |
| RDN LDAP attribute | uid |
| UUID LDAP attribute | entryUUID |
| User Object Classes | inetOrgPerson, posixAccount |

5. 點擊 **Save** 後，點擊 **Synchronize all users**

---

## Phase 3: Spring Security 整合

### 3.1 新增 Gradle 依賴

更新 `build.gradle`：

```groovy
plugins {
    id 'java'
    id 'org.springframework.boot' version '3.3.0'
    id 'io.spring.dependency-management' version '1.1.4'
}

dependencies {
    // Spring Boot Starters
    implementation 'org.springframework.boot:spring-boot-starter-web'
    implementation 'org.springframework.boot:spring-boot-starter-data-jpa'
    implementation 'org.springframework.boot:spring-boot-starter-validation'
    implementation 'org.springframework.boot:spring-boot-starter-actuator'
    
    // Spring Security OAuth2 Resource Server
    implementation 'org.springframework.boot:spring-boot-starter-oauth2-resource-server'
    implementation 'org.springframework.boot:spring-boot-starter-security'
    
    // JWT 處理
    implementation 'org.springframework.security:spring-security-oauth2-jose'
    
    // Keycloak Admin Client (選用，用於管理 API)
    implementation 'org.keycloak:keycloak-admin-client:23.0.0'
    
    // 現有的 Audit Library
    implementation project(':libs:audit-lib')
    
    // Database
    runtimeOnly 'com.h2database:h2'
    runtimeOnly 'org.postgresql:postgresql'
    
    // Lombok
    compileOnly 'org.projectlombok:lombok'
    annotationProcessor 'org.projectlombok:lombok'
    
    // Testing
    testImplementation 'org.springframework.boot:spring-boot-starter-test'
    testImplementation 'org.springframework.security:spring-security-test'
}
```

### 3.2 Application 配置

建立/更新 `application.yml`：

```yaml
server:
  port: 8082

spring:
  application:
    name: rbac-sso-poc
  
  # Database Configuration
  datasource:
    url: jdbc:h2:mem:rbacssodb;DB_CLOSE_DELAY=-1
    driver-class-name: org.h2.Driver
    username: sa
    password: 
  
  jpa:
    hibernate:
      ddl-auto: update
    show-sql: true
    properties:
      hibernate:
        format_sql: true
  
  # Spring Security OAuth2 Resource Server
  security:
    oauth2:
      resourceserver:
        jwt:
          issuer-uri: http://localhost:8080/realms/rbac-sso-realm
          jwk-set-uri: http://localhost:8080/realms/rbac-sso-realm/protocol/openid-connect/certs

# Keycloak Configuration (自訂)
keycloak:
  realm: rbac-sso-realm
  auth-server-url: http://localhost:8080
  resource: rbac-sso-app
  credentials:
    secret: your-client-secret-here
  use-resource-role-mappings: true

# Audit Library Configuration
audit:
  enabled: true
  service-name: ${spring.application.name}
  payload:
    max-size: 65536
  masking:
    default-fields:
      - password
      - secret
      - token
      - credential

# Actuator
management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics,prometheus
  endpoint:
    health:
      show-details: when_authorized

# Logging
logging:
  level:
    org.springframework.security: DEBUG
    com.example: DEBUG
```

### 3.3 Security Configuration 類別

建立 `src/main/java/com/example/rbac/config/SecurityConfig.java`：

```java
package com.example.rbac.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true, securedEnabled = true)
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            // Disable CSRF for stateless API
            .csrf(csrf -> csrf.disable())
            
            // Enable CORS
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            
            // Stateless session management
            .sessionManagement(session -> 
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            
            // Authorization rules
            .authorizeHttpRequests(authz -> authz
                // Public endpoints
                .requestMatchers("/actuator/health", "/actuator/info").permitAll()
                .requestMatchers("/api/public/**").permitAll()
                .requestMatchers("/swagger-ui/**", "/v3/api-docs/**").permitAll()
                
                // Admin endpoints
                .requestMatchers("/api/admin/**").hasRole("ADMIN")
                
                // Audit endpoints - require specific permission
                .requestMatchers("/api/v1/audit-logs/**").hasAuthority("audit:read")
                
                // All other endpoints require authentication
                .anyRequest().authenticated()
            )
            
            // OAuth2 Resource Server with JWT
            .oauth2ResourceServer(oauth2 -> oauth2
                .jwt(jwt -> jwt
                    .jwtAuthenticationConverter(jwtAuthenticationConverter())
                )
            );
        
        return http.build();
    }

    /**
     * 自訂 JWT Authentication Converter
     * 從 Keycloak JWT Token 中提取 roles 和 permissions
     */
    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(new KeycloakGrantedAuthoritiesConverter());
        return converter;
    }

    /**
     * Keycloak 專用的 GrantedAuthorities Converter
     * 處理 realm_roles 和 resource_access 中的角色
     */
    static class KeycloakGrantedAuthoritiesConverter 
            implements Converter<Jwt, Collection<GrantedAuthority>> {
        
        private static final String REALM_ROLES_CLAIM = "realm_access";
        private static final String RESOURCE_ACCESS_CLAIM = "resource_access";
        private static final String ROLES_KEY = "roles";
        private static final String CLIENT_ID = "rbac-sso-app";

        @Override
        public Collection<GrantedAuthority> convert(Jwt jwt) {
            // 提取 Realm Roles
            Stream<GrantedAuthority> realmRoles = extractRealmRoles(jwt);
            
            // 提取 Client Roles (permissions)
            Stream<GrantedAuthority> clientRoles = extractClientRoles(jwt);
            
            // 合併所有角色
            return Stream.concat(realmRoles, clientRoles)
                    .collect(Collectors.toSet());
        }

        @SuppressWarnings("unchecked")
        private Stream<GrantedAuthority> extractRealmRoles(Jwt jwt) {
            Map<String, Object> realmAccess = jwt.getClaim(REALM_ROLES_CLAIM);
            if (realmAccess == null) {
                return Stream.empty();
            }
            
            List<String> roles = (List<String>) realmAccess.get(ROLES_KEY);
            if (roles == null) {
                return Stream.empty();
            }
            
            return roles.stream()
                    .map(role -> new SimpleGrantedAuthority("ROLE_" + role.toUpperCase().replace("ROLE_", "")));
        }

        @SuppressWarnings("unchecked")
        private Stream<GrantedAuthority> extractClientRoles(Jwt jwt) {
            Map<String, Object> resourceAccess = jwt.getClaim(RESOURCE_ACCESS_CLAIM);
            if (resourceAccess == null) {
                return Stream.empty();
            }
            
            Map<String, Object> clientAccess = (Map<String, Object>) resourceAccess.get(CLIENT_ID);
            if (clientAccess == null) {
                return Stream.empty();
            }
            
            List<String> roles = (List<String>) clientAccess.get(ROLES_KEY);
            if (roles == null) {
                return Stream.empty();
            }
            
            // Client roles 作為 permissions (不加 ROLE_ prefix)
            return roles.stream()
                    .map(SimpleGrantedAuthority::new);
        }
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(List.of(
            "http://localhost:3000",
            "http://localhost:8082"
        ));
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);
        
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
```

### 3.4 當前使用者資訊服務

建立 `src/main/java/com/example/rbac/security/CurrentUserService.java`：

```java
package com.example.rbac.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class CurrentUserService {

    /**
     * 取得當前使用者名稱
     */
    public String getCurrentUsername() {
        return getJwt()
                .map(jwt -> jwt.getClaimAsString("preferred_username"))
                .orElse("anonymous");
    }

    /**
     * 取得當前使用者 Email
     */
    public Optional<String> getCurrentUserEmail() {
        return getJwt()
                .map(jwt -> jwt.getClaimAsString("email"));
    }

    /**
     * 取得當前使用者 ID (Subject)
     */
    public Optional<String> getCurrentUserId() {
        return getJwt()
                .map(Jwt::getSubject);
    }

    /**
     * 取得當前使用者所有角色
     */
    public Set<String> getCurrentUserRoles() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            return Set.of();
        }
        
        return authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .filter(auth -> auth.startsWith("ROLE_"))
                .collect(Collectors.toSet());
    }

    /**
     * 取得當前使用者所有權限 (不含 ROLE_ 前綴的)
     */
    public Set<String> getCurrentUserPermissions() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            return Set.of();
        }
        
        return authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .filter(auth -> !auth.startsWith("ROLE_"))
                .collect(Collectors.toSet());
    }

    /**
     * 檢查當前使用者是否有特定角色
     */
    public boolean hasRole(String role) {
        String normalizedRole = role.startsWith("ROLE_") ? role : "ROLE_" + role;
        return getCurrentUserRoles().contains(normalizedRole);
    }

    /**
     * 檢查當前使用者是否有特定權限
     */
    public boolean hasPermission(String permission) {
        return getCurrentUserPermissions().contains(permission);
    }

    /**
     * 取得原始 JWT Token
     */
    public Optional<Jwt> getJwt() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication instanceof JwtAuthenticationToken jwtAuth) {
            return Optional.of(jwtAuth.getToken());
        }
        return Optional.empty();
    }

    /**
     * 取得 JWT Token 字串
     */
    public Optional<String> getTokenValue() {
        return getJwt().map(Jwt::getTokenValue);
    }
}
```

---

## Phase 4: RBAC 權限控制實作

### 4.1 Product Controller 範例 (含 RBAC)

建立 `src/main/java/com/example/rbac/controller/ProductController.java`：

```java
package com.example.rbac.controller;

import com.example.audit.annotation.Auditable;
import com.example.rbac.dto.ApiResponse;
import com.example.rbac.dto.CreateProductRequest;
import com.example.rbac.dto.ProductDto;
import com.example.rbac.dto.UpdateProductRequest;
import com.example.rbac.service.ProductService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    /**
     * 查詢所有產品 - 需要 product:read 權限
     */
    @GetMapping
    @PreAuthorize("hasAuthority('product:read')")
    @Auditable(eventType = "PRODUCT_LIST_VIEWED", resourceType = "Product")
    public ResponseEntity<ApiResponse<List<ProductDto>>> getAllProducts() {
        List<ProductDto> products = productService.findAll();
        return ResponseEntity.ok(ApiResponse.success(products, "Products retrieved successfully"));
    }

    /**
     * 查詢單一產品 - 需要 product:read 權限
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('product:read')")
    @Auditable(eventType = "PRODUCT_VIEWED", resourceType = "Product")
    public ResponseEntity<ApiResponse<ProductDto>> getProduct(@PathVariable UUID id) {
        ProductDto product = productService.findById(id);
        return ResponseEntity.ok(ApiResponse.success(product, "Product retrieved successfully"));
    }

    /**
     * 建立產品 - 需要 product:write 權限
     */
    @PostMapping
    @PreAuthorize("hasAuthority('product:write')")
    @Auditable(eventType = "PRODUCT_CREATED", resourceType = "Product")
    public ResponseEntity<ApiResponse<ProductDto>> createProduct(
            @Valid @RequestBody CreateProductRequest request) {
        ProductDto product = productService.create(request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success(product, "Product created successfully"));
    }

    /**
     * 更新產品 - 需要 product:write 權限
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('product:write')")
    @Auditable(eventType = "PRODUCT_UPDATED", resourceType = "Product")
    public ResponseEntity<ApiResponse<ProductDto>> updateProduct(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateProductRequest request) {
        ProductDto product = productService.update(id, request);
        return ResponseEntity.ok(ApiResponse.success(product, "Product updated successfully"));
    }

    /**
     * 刪除產品 - 需要 product:delete 權限 (僅 Admin)
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('product:delete')")
    @Auditable(eventType = "PRODUCT_DELETED", resourceType = "Product")
    public ResponseEntity<ApiResponse<Void>> deleteProduct(@PathVariable UUID id) {
        productService.delete(id);
        return ResponseEntity.ok(ApiResponse.success(null, "Product deleted successfully"));
    }

    /**
     * 批次操作 - 需要 ADMIN 角色
     */
    @PostMapping("/batch-import")
    @PreAuthorize("hasRole('ADMIN')")
    @Auditable(eventType = "PRODUCT_BATCH_IMPORTED", resourceType = "Product")
    public ResponseEntity<ApiResponse<Integer>> batchImport(
            @RequestBody List<CreateProductRequest> requests) {
        int count = productService.batchImport(requests);
        return ResponseEntity.ok(ApiResponse.success(count, count + " products imported"));
    }
}
```

### 4.2 使用者管理 Controller 範例

建立 `src/main/java/com/example/rbac/controller/UserController.java`：

```java
package com.example.rbac.controller;

import com.example.audit.annotation.Auditable;
import com.example.rbac.dto.ApiResponse;
import com.example.rbac.dto.UserDto;
import com.example.rbac.security.CurrentUserService;
import com.example.rbac.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Set;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final CurrentUserService currentUserService;
    private final UserService userService;

    /**
     * 取得當前使用者資訊
     */
    @GetMapping("/me")
    @Auditable(eventType = "USER_PROFILE_VIEWED", resourceType = "User")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getCurrentUser(
            @AuthenticationPrincipal Jwt jwt) {
        Map<String, Object> userInfo = Map.of(
            "id", jwt.getSubject(),
            "username", currentUserService.getCurrentUsername(),
            "email", currentUserService.getCurrentUserEmail().orElse(""),
            "roles", currentUserService.getCurrentUserRoles(),
            "permissions", currentUserService.getCurrentUserPermissions()
        );
        return ResponseEntity.ok(ApiResponse.success(userInfo, "User info retrieved"));
    }

    /**
     * 取得所有使用者 - 需要 user:read 權限
     */
    @GetMapping
    @PreAuthorize("hasAuthority('user:read')")
    @Auditable(eventType = "USER_LIST_VIEWED", resourceType = "User")
    public ResponseEntity<ApiResponse<List<UserDto>>> getAllUsers() {
        List<UserDto> users = userService.findAll();
        return ResponseEntity.ok(ApiResponse.success(users, "Users retrieved successfully"));
    }

    /**
     * 管理使用者 - 需要 user:write 權限 (Admin only)
     */
    @PutMapping("/{userId}/roles")
    @PreAuthorize("hasAuthority('user:write')")
    @Auditable(eventType = "USER_ROLES_UPDATED", resourceType = "User", 
               maskFields = {"password", "token"})
    public ResponseEntity<ApiResponse<UserDto>> updateUserRoles(
            @PathVariable String userId,
            @RequestBody Set<String> roles) {
        UserDto user = userService.updateRoles(userId, roles);
        return ResponseEntity.ok(ApiResponse.success(user, "User roles updated"));
    }

    /**
     * 停用使用者 - 需要 ADMIN 角色
     */
    @PostMapping("/{userId}/disable")
    @PreAuthorize("hasRole('ADMIN')")
    @Auditable(eventType = "USER_DISABLED", resourceType = "User")
    public ResponseEntity<ApiResponse<Void>> disableUser(@PathVariable String userId) {
        userService.disable(userId);
        return ResponseEntity.ok(ApiResponse.success(null, "User disabled"));
    }
}
```

### 4.3 自訂權限評估器 (進階 RBAC)

建立 `src/main/java/com/example/rbac/security/CustomPermissionEvaluator.java`：

```java
package com.example.rbac.security;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.PermissionEvaluator;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import java.io.Serializable;

/**
 * 自訂權限評估器
 * 支援 @PreAuthorize("hasPermission(#entity, 'read')") 語法
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CustomPermissionEvaluator implements PermissionEvaluator {

    private final CurrentUserService currentUserService;

    @Override
    public boolean hasPermission(Authentication authentication, 
                                  Object targetDomainObject, 
                                  Object permission) {
        if (authentication == null || targetDomainObject == null || permission == null) {
            return false;
        }

        String targetType = targetDomainObject.getClass().getSimpleName().toLowerCase();
        String permissionString = permission.toString().toLowerCase();
        
        // 組合權限字串: product:read, user:write 等
        String requiredPermission = targetType + ":" + permissionString;
        
        boolean hasPermission = currentUserService.hasPermission(requiredPermission);
        
        log.debug("Permission check: user={}, permission={}, result={}",
                currentUserService.getCurrentUsername(), requiredPermission, hasPermission);
        
        return hasPermission;
    }

    @Override
    public boolean hasPermission(Authentication authentication, 
                                  Serializable targetId, 
                                  String targetType, 
                                  Object permission) {
        if (authentication == null || targetType == null || permission == null) {
            return false;
        }

        String requiredPermission = targetType.toLowerCase() + ":" + permission.toString().toLowerCase();
        
        boolean hasPermission = currentUserService.hasPermission(requiredPermission);
        
        log.debug("Permission check: user={}, targetId={}, permission={}, result={}",
                currentUserService.getCurrentUsername(), targetId, requiredPermission, hasPermission);
        
        return hasPermission;
    }
}
```

### 4.4 註冊自訂權限評估器

更新 `SecurityConfig.java` 加入：

```java
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true, securedEnabled = true)
public class SecurityConfig {

    // ... 其他配置 ...

    @Bean
    public MethodSecurityExpressionHandler methodSecurityExpressionHandler(
            CustomPermissionEvaluator permissionEvaluator) {
        DefaultMethodSecurityExpressionHandler handler = 
            new DefaultMethodSecurityExpressionHandler();
        handler.setPermissionEvaluator(permissionEvaluator);
        return handler;
    }
}
```

---

## Phase 5: 測試與驗證

### 5.1 取得 Access Token

```bash
# 使用 Password Grant 取得 Token (測試用)
curl -X POST "http://localhost:8080/realms/rbac-sso-realm/protocol/openid-connect/token" \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "grant_type=password" \
  -d "client_id=rbac-sso-app" \
  -d "client_secret=your-client-secret-here" \
  -d "username=admin" \
  -d "password=admin_password" \
  -d "scope=openid profile email"
```

### 5.2 測試 API 呼叫

```bash
# 設定 Token
export TOKEN="eyJhbGciOiJSUzI1NiIsInR5cCI6..."

# 測試取得當前使用者
curl -X GET "http://localhost:8082/api/v1/users/me" \
  -H "Authorization: Bearer $TOKEN"

# 測試取得產品列表
curl -X GET "http://localhost:8082/api/v1/products" \
  -H "Authorization: Bearer $TOKEN"

# 測試建立產品 (需要 product:write 權限)
curl -X POST "http://localhost:8082/api/v1/products" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "productCode": "SKU-001",
    "productName": "Test Product",
    "price": 99.99
  }'
```

### 5.3 Integration Test 範例

建立 `src/test/java/com/example/rbac/integration/SecurityIntegrationTest.java`：

```java
package com.example.rbac.integration;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class SecurityIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void whenUnauthenticated_thenUnauthorized() throws Exception {
        mockMvc.perform(get("/api/v1/products"))
               .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(authorities = {"product:read"})
    void whenHasProductReadAuthority_thenCanListProducts() throws Exception {
        mockMvc.perform(get("/api/v1/products"))
               .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(authorities = {"user:read"})
    void whenHasUserReadButNotProductRead_thenForbidden() throws Exception {
        mockMvc.perform(get("/api/v1/products"))
               .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = {"ADMIN"})
    void whenAdmin_thenCanAccessAdminEndpoints() throws Exception {
        mockMvc.perform(get("/api/admin/dashboard"))
               .andExpect(status().isOk());
    }
}
```

---

## 完整專案結構 (實作後)

```
rbac-sso-poc/
├── infra/
│   ├── docker-compose.yml                    # 新增
│   ├── keycloak/
│   │   └── realm-export.json                 # 新增
│   └── ldap/
│       └── bootstrap.ldif                    # 新增
├── libs/
│   └── audit-lib/                            # 既有
├── src/main/java/com/example/rbac/
│   ├── config/
│   │   └── SecurityConfig.java               # 新增
│   ├── controller/
│   │   ├── ProductController.java            # 新增
│   │   └── UserController.java               # 新增
│   ├── dto/
│   │   ├── ApiResponse.java                  # 新增
│   │   ├── ProductDto.java                   # 新增
│   │   └── UserDto.java                      # 新增
│   ├── security/
│   │   ├── CurrentUserService.java           # 新增
│   │   └── CustomPermissionEvaluator.java    # 新增
│   └── service/
│       ├── ProductService.java               # 新增
│       └── UserService.java                  # 新增
├── src/main/resources/
│   └── application.yml                       # 更新
├── src/test/java/com/example/rbac/
│   └── integration/
│       └── SecurityIntegrationTest.java      # 新增
├── build.gradle                              # 更新
└── README.md
```

---

## 權限對照表

| 角色 | Realm Role | Client Permissions | 可執行操作 |
|------|------------|-------------------|-----------|
| Admin | ROLE_ADMIN | product:read, product:write, product:delete, user:read, user:write, audit:read | 全部操作 |
| Developer | ROLE_DEVELOPER | product:read, product:write, user:read, audit:read | 產品 CRUD (無刪除)、查看使用者、查看稽核 |
| Viewer | ROLE_VIEWER | product:read, user:read | 僅查看 |

---

## 常見問題排除

### Q1: JWT Token 驗證失敗
```
確認事項：
1. Keycloak issuer-uri 是否正確
2. 網路是否可達 Keycloak jwk-set-uri
3. Token 是否過期
4. Client secret 是否正確
```

### Q2: LDAP 使用者無法同步
```
確認事項：
1. LDAP 連線 URL 是否正確
2. Bind DN 和密碼是否正確
3. Users DN 路徑是否正確
4. 執行 "Synchronize all users" 按鈕
```

### Q3: 權限檢查失敗
```
確認事項：
1. 檢查 JWT Token 中的 realm_access 和 resource_access
2. 確認 Protocol Mappers 配置正確
3. 使用 jwt.io 解析 Token 檢查 claims
4. 確認 Spring Security 日誌中的 authorities
```

---

## 總結

| 檢查項目 | 實作前 | 實作後 |
|----------|:------:|:------:|
| KeyCloak 整合 | ❌ | ✅ |
| LDAP User Federation | ❌ | ✅ |
| SSO 功能 | ❌ | ✅ |
| RBAC 權限控制 | ❌ | ✅ |
| JWT Token 驗證 | ❌ | ✅ |
| 方法級權限 (@PreAuthorize) | ❌ | ✅ |
| 稽核函式庫整合 | ✅ | ✅ |

---

*報告產生時間：2026-01-10*
