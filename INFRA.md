# INFRA: 基礎設施部署文件

## 文件資訊

| 項目 | 內容 |
|------|------|
| 文件版本 | 1.0 |
| 建立日期 | 2025-01-10 |
| 專案代號 | SSO-RBAC-POC |
| 適用範圍 | Infrastructure Components |

---

## 1. 基礎設施架構

### 1.1 架構總覽

```
┌────────────────────────────────────────────────────────────────────────────────┐
│                         Docker Compose Environment                              │
│                            Network: sso-network                                 │
├────────────────────────────────────────────────────────────────────────────────┤
│                                                                                 │
│  ┌─────────────────┐      ┌─────────────────┐      ┌─────────────────┐        │
│  │                 │      │                 │      │                 │        │
│  │    OpenLDAP     │◀────▶│    Keycloak     │◀────▶│   PostgreSQL    │        │
│  │                 │ LDAP │                 │ JDBC │                 │        │
│  │   Port: 389     │      │   Port: 8080    │      │   Port: 5432    │        │
│  │                 │      │                 │      │   (internal)    │        │
│  └─────────────────┘      └─────────────────┘      └─────────────────┘        │
│           │                        │                                           │
│           │                        │                                           │
│  ┌─────────────────┐      ┌─────────────────┐                                 │
│  │                 │      │                 │                                 │
│  │  phpLDAPadmin   │      │   Spring API    │                                 │
│  │                 │      │                 │                                 │
│  │   Port: 8081    │      │   Port: 9090    │                                 │
│  │                 │      │                 │                                 │
│  └─────────────────┘      └─────────────────┘                                 │
│                                                                                 │
└────────────────────────────────────────────────────────────────────────────────┘

External Access:
├── http://localhost:8080  → Keycloak Admin Console
├── http://localhost:8081  → phpLDAPadmin
├── http://localhost:9090  → Spring Boot API
└── ldap://localhost:389   → OpenLDAP
```

### 1.2 元件版本

| Component | Image | Version | Purpose |
|-----------|-------|---------|---------|
| OpenLDAP | osixia/openldap | 1.5.0 | User Directory |
| phpLDAPadmin | osixia/phpldapadmin | 0.9.0 | LDAP Admin UI |
| PostgreSQL | postgres | 15-alpine | Keycloak Database |
| Keycloak | quay.io/keycloak/keycloak | 24.0 | Identity Provider |
| Spring API | Custom Build | - | Application |

---

## 2. 目錄結構

```
sso-rbac-poc/
├── docker-compose.yml              # 主要編排檔案
├── .env                            # 環境變數
├── PRD.md                          # 業務需求文件
├── TECH.md                         # 技術架構文件
├── INFRA.md                        # 基礎設施文件（本文件）
│
├── ldap/                           # OpenLDAP 配置
│   ├── bootstrap.ldif              # 初始化資料
│   └── schema/                     # 自訂 Schema（如需）
│
├── keycloak/                       # Keycloak 配置
│   ├── realm-export.json           # Realm 匯出配置
│   └── themes/                     # 自訂主題（如需）
│
├── spring-api/                     # Spring Boot 應用
│   ├── src/
│   ├── Dockerfile
│   └── pom.xml
│
└── scripts/                        # 工具腳本
    ├── setup.sh                    # 環境初始化
    ├── cleanup.sh                  # 環境清理
    └── test-api.sh                 # API 測試
```

---

## 3. Docker Compose 配置

### 3.1 docker-compose.yml

```yaml
# docker-compose.yml
version: '3.8'

services:
  # ============================================================
  # OpenLDAP - User Directory Service
  # ============================================================
  openldap:
    image: osixia/openldap:1.5.0
    container_name: sso-openldap
    hostname: openldap
    restart: unless-stopped
    environment:
      # 組織設定
      LDAP_ORGANISATION: "Example Corporation"
      LDAP_DOMAIN: "example.com"
      LDAP_BASE_DN: "dc=example,dc=com"
      # 管理員密碼
      LDAP_ADMIN_PASSWORD: ${LDAP_ADMIN_PASSWORD:-admin123}
      LDAP_CONFIG_PASSWORD: ${LDAP_CONFIG_PASSWORD:-config123}
      # TLS 設定（PoC 停用）
      LDAP_TLS: "false"
      LDAP_TLS_VERIFY_CLIENT: "never"
      # 日誌
      LDAP_LOG_LEVEL: 256
    ports:
      - "${LDAP_PORT:-389}:389"
      - "${LDAPS_PORT:-636}:636"
    volumes:
      # 資料持久化
      - ldap_data:/var/lib/ldap
      - ldap_config:/etc/ldap/slapd.d
      # 初始化資料
      - ./ldap/bootstrap.ldif:/container/service/slapd/assets/config/bootstrap/ldif/custom/bootstrap.ldif
    networks:
      - sso-network
    healthcheck:
      test: ["CMD", "ldapsearch", "-x", "-H", "ldap://localhost", "-b", "dc=example,dc=com", "-D", "cn=admin,dc=example,dc=com", "-w", "${LDAP_ADMIN_PASSWORD:-admin123}"]
      interval: 30s
      timeout: 10s
      retries: 3
      start_period: 30s

  # ============================================================
  # phpLDAPadmin - LDAP Web Administration UI
  # ============================================================
  phpldapadmin:
    image: osixia/phpldapadmin:0.9.0
    container_name: sso-phpldapadmin
    restart: unless-stopped
    environment:
      PHPLDAPADMIN_LDAP_HOSTS: openldap
      PHPLDAPADMIN_HTTPS: "false"
    ports:
      - "${PHPLDAPADMIN_PORT:-8081}:80"
    depends_on:
      openldap:
        condition: service_healthy
    networks:
      - sso-network

  # ============================================================
  # PostgreSQL - Keycloak Database
  # ============================================================
  postgres:
    image: postgres:15-alpine
    container_name: sso-postgres
    restart: unless-stopped
    environment:
      POSTGRES_DB: ${POSTGRES_DB:-keycloak}
      POSTGRES_USER: ${POSTGRES_USER:-keycloak}
      POSTGRES_PASSWORD: ${POSTGRES_PASSWORD:-keycloak123}
    volumes:
      - postgres_data:/var/lib/postgresql/data
    networks:
      - sso-network
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U ${POSTGRES_USER:-keycloak} -d ${POSTGRES_DB:-keycloak}"]
      interval: 10s
      timeout: 5s
      retries: 5
      start_period: 10s

  # ============================================================
  # Keycloak - Identity Provider & SSO Server
  # ============================================================
  keycloak:
    image: quay.io/keycloak/keycloak:24.0
    container_name: sso-keycloak
    restart: unless-stopped
    environment:
      # 資料庫配置
      KC_DB: postgres
      KC_DB_URL: jdbc:postgresql://postgres:5432/${POSTGRES_DB:-keycloak}
      KC_DB_USERNAME: ${POSTGRES_USER:-keycloak}
      KC_DB_PASSWORD: ${POSTGRES_PASSWORD:-keycloak123}
      # 管理員帳號
      KEYCLOAK_ADMIN: ${KEYCLOAK_ADMIN:-admin}
      KEYCLOAK_ADMIN_PASSWORD: ${KEYCLOAK_ADMIN_PASSWORD:-admin123}
      # 主機名稱配置
      KC_HOSTNAME: ${KC_HOSTNAME:-localhost}
      KC_HOSTNAME_PORT: ${KEYCLOAK_PORT:-8080}
      KC_HOSTNAME_STRICT: "false"
      KC_HOSTNAME_STRICT_HTTPS: "false"
      # HTTP 配置（PoC 啟用 HTTP）
      KC_HTTP_ENABLED: "true"
      KC_PROXY: edge
      # 健康檢查
      KC_HEALTH_ENABLED: "true"
      KC_METRICS_ENABLED: "true"
    command: start-dev
    ports:
      - "${KEYCLOAK_PORT:-8080}:8080"
    depends_on:
      postgres:
        condition: service_healthy
      openldap:
        condition: service_healthy
    networks:
      - sso-network
    healthcheck:
      test: ["CMD-SHELL", "exec 3<>/dev/tcp/127.0.0.1/8080;echo -e 'GET /health/ready HTTP/1.1\r\nhost: localhost\r\nConnection: close\r\n\r\n' >&3;if [ $? -eq 0 ]; then echo 'Healthcheck Successful';exit 0;else echo 'Healthcheck Failed';exit 1;fi;"]
      interval: 30s
      timeout: 10s
      retries: 5
      start_period: 120s

  # ============================================================
  # Spring Boot API Application
  # ============================================================
  spring-api:
    build:
      context: ./spring-api
      dockerfile: Dockerfile
    container_name: sso-spring-api
    restart: unless-stopped
    environment:
      SPRING_PROFILES_ACTIVE: docker
      KEYCLOAK_ISSUER_URI: http://keycloak:8080/realms/demo
      KEYCLOAK_JWKS_URI: http://keycloak:8080/realms/demo/protocol/openid-connect/certs
      # JVM 配置
      JAVA_OPTS: "-Xms256m -Xmx512m"
    ports:
      - "${API_PORT:-9090}:9090"
    depends_on:
      keycloak:
        condition: service_healthy
    networks:
      - sso-network
    healthcheck:
      test: ["CMD", "wget", "--no-verbose", "--tries=1", "--spider", "http://localhost:9090/actuator/health"]
      interval: 30s
      timeout: 5s
      retries: 3
      start_period: 60s

# ============================================================
# Networks
# ============================================================
networks:
  sso-network:
    driver: bridge
    name: sso-network

# ============================================================
# Volumes
# ============================================================
volumes:
  ldap_data:
    name: sso-ldap-data
  ldap_config:
    name: sso-ldap-config
  postgres_data:
    name: sso-postgres-data
```

### 3.2 環境變數檔案 (.env)

```bash
# .env - Environment Variables

# ============ OpenLDAP ============
LDAP_ADMIN_PASSWORD=admin123
LDAP_CONFIG_PASSWORD=config123
LDAP_PORT=389
LDAPS_PORT=636

# ============ phpLDAPadmin ============
PHPLDAPADMIN_PORT=8081

# ============ PostgreSQL ============
POSTGRES_DB=keycloak
POSTGRES_USER=keycloak
POSTGRES_PASSWORD=keycloak123

# ============ Keycloak ============
KEYCLOAK_ADMIN=admin
KEYCLOAK_ADMIN_PASSWORD=admin123
KEYCLOAK_PORT=8080
KC_HOSTNAME=localhost

# ============ Spring API ============
API_PORT=9090
```

---

## 4. OpenLDAP 配置

### 4.1 目錄結構設計

```
dc=example,dc=com                    (Domain)
├── ou=users                         (Users OU)
│   ├── uid=admin.user              (Admin User)
│   ├── uid=normal.user             (Normal User)
│   └── uid=auditor.user            (Auditor User)
├── ou=groups                        (Groups OU)
│   ├── cn=admins                   (Admin Group)
│   ├── cn=users                    (User Group)
│   └── cn=auditors                 (Auditor Group)
└── ou=services                      (Service Accounts)
    └── cn=keycloak                 (Keycloak Bind Account)
```

### 4.2 bootstrap.ldif

```ldif
# ldap/bootstrap.ldif
# ============================================================
# OpenLDAP Bootstrap Data
# ============================================================

# ------------------------------------------------------------
# Organizational Units
# ------------------------------------------------------------
dn: ou=users,dc=example,dc=com
objectClass: organizationalUnit
ou: users
description: Container for user accounts

dn: ou=groups,dc=example,dc=com
objectClass: organizationalUnit
ou: groups
description: Container for groups

dn: ou=services,dc=example,dc=com
objectClass: organizationalUnit
ou: services
description: Container for service accounts

# ------------------------------------------------------------
# Service Account for Keycloak
# ------------------------------------------------------------
dn: cn=keycloak,ou=services,dc=example,dc=com
objectClass: organizationalRole
objectClass: simpleSecurityObject
cn: keycloak
description: Keycloak LDAP bind account
userPassword: keycloak123

# ------------------------------------------------------------
# User: admin.user (Administrator)
# ------------------------------------------------------------
dn: uid=admin.user,ou=users,dc=example,dc=com
objectClass: inetOrgPerson
objectClass: posixAccount
objectClass: shadowAccount
uid: admin.user
sn: User
givenName: Admin
cn: Admin User
displayName: Admin User
uidNumber: 10001
gidNumber: 10001
userPassword: admin123
loginShell: /bin/bash
homeDirectory: /home/admin.user
mail: admin@example.com
telephoneNumber: +886-2-1234-5678
title: System Administrator
employeeType: full-time

# ------------------------------------------------------------
# User: normal.user (Regular User)
# ------------------------------------------------------------
dn: uid=normal.user,ou=users,dc=example,dc=com
objectClass: inetOrgPerson
objectClass: posixAccount
objectClass: shadowAccount
uid: normal.user
sn: User
givenName: Normal
cn: Normal User
displayName: Normal User
uidNumber: 10002
gidNumber: 10002
userPassword: user123
loginShell: /bin/bash
homeDirectory: /home/normal.user
mail: user@example.com
telephoneNumber: +886-2-2345-6789
title: Staff
employeeType: full-time

# ------------------------------------------------------------
# User: auditor.user (Auditor)
# ------------------------------------------------------------
dn: uid=auditor.user,ou=users,dc=example,dc=com
objectClass: inetOrgPerson
objectClass: posixAccount
objectClass: shadowAccount
uid: auditor.user
sn: User
givenName: Auditor
cn: Auditor User
displayName: Auditor User
uidNumber: 10003
gidNumber: 10003
userPassword: auditor123
loginShell: /bin/bash
homeDirectory: /home/auditor.user
mail: auditor@example.com
telephoneNumber: +886-2-3456-7890
title: Internal Auditor
employeeType: full-time

# ------------------------------------------------------------
# User: test.user (Test Account)
# ------------------------------------------------------------
dn: uid=test.user,ou=users,dc=example,dc=com
objectClass: inetOrgPerson
objectClass: posixAccount
objectClass: shadowAccount
uid: test.user
sn: User
givenName: Test
cn: Test User
displayName: Test User
uidNumber: 10004
gidNumber: 10004
userPassword: test123
loginShell: /bin/bash
homeDirectory: /home/test.user
mail: test@example.com
employeeType: contractor

# ------------------------------------------------------------
# Group: admins
# ------------------------------------------------------------
dn: cn=admins,ou=groups,dc=example,dc=com
objectClass: groupOfNames
cn: admins
description: System Administrators
member: uid=admin.user,ou=users,dc=example,dc=com

# ------------------------------------------------------------
# Group: users
# ------------------------------------------------------------
dn: cn=users,ou=groups,dc=example,dc=com
objectClass: groupOfNames
cn: users
description: Regular Users
member: uid=admin.user,ou=users,dc=example,dc=com
member: uid=normal.user,ou=users,dc=example,dc=com
member: uid=test.user,ou=users,dc=example,dc=com

# ------------------------------------------------------------
# Group: auditors
# ------------------------------------------------------------
dn: cn=auditors,ou=groups,dc=example,dc=com
objectClass: groupOfNames
cn: auditors
description: Internal Auditors
member: uid=auditor.user,ou=users,dc=example,dc=com
```

### 4.3 LDAP 測試指令

```bash
# 測試 LDAP 連線
ldapsearch -x -H ldap://localhost:389 \
  -D "cn=admin,dc=example,dc=com" \
  -w admin123 \
  -b "dc=example,dc=com" \
  "(objectClass=*)"

# 搜尋所有使用者
ldapsearch -x -H ldap://localhost:389 \
  -D "cn=admin,dc=example,dc=com" \
  -w admin123 \
  -b "ou=users,dc=example,dc=com" \
  "(objectClass=inetOrgPerson)" \
  uid cn mail

# 搜尋所有群組
ldapsearch -x -H ldap://localhost:389 \
  -D "cn=admin,dc=example,dc=com" \
  -w admin123 \
  -b "ou=groups,dc=example,dc=com" \
  "(objectClass=groupOfNames)" \
  cn member

# 驗證使用者密碼
ldapwhoami -x -H ldap://localhost:389 \
  -D "uid=admin.user,ou=users,dc=example,dc=com" \
  -w admin123
```

---

## 5. Keycloak 配置

### 5.1 Realm 配置步驟

以下是手動配置 Keycloak Realm 的步驟（首次設置時執行）：

#### Step 1: 建立 Realm

1. 登入 Keycloak Admin Console: `http://localhost:8080`
2. 點擊左上角 "master" 下拉選單
3. 點擊 "Create Realm"
4. 輸入 Realm name: `demo`
5. 點擊 "Create"

#### Step 2: 配置 LDAP User Federation

1. 進入 `demo` Realm
2. 左側選單點擊 "User federation"
3. 點擊 "Add Ldap providers"
4. 填入以下配置：

```yaml
# LDAP Provider Settings
UI display name: OpenLDAP
Vendor: Other
Connection URL: ldap://openldap:389
Users DN: ou=users,dc=example,dc=com
Bind type: simple
Bind DN: cn=admin,dc=example,dc=com
Bind credential: admin123

# Sync Settings
Import users: ON
Sync registrations: OFF
Periodic full sync: ON
Full sync period: 604800  # 7 days
Periodic changed users sync: ON
Changed users sync period: 86400  # 1 day

# Kerberos Integration
Allow Kerberos authentication: OFF

# LDAP Searching and Updating
Edit mode: READ_ONLY
Username LDAP attribute: uid
RDN LDAP attribute: uid
UUID LDAP attribute: entryUUID
User object classes: inetOrgPerson, posixAccount
```

5. 點擊 "Test connection" 確認連線成功
6. 點擊 "Test authentication" 確認認證成功
7. 點擊 "Save"
8. 點擊 "Sync all users" 同步 LDAP 使用者

#### Step 3: 配置 LDAP Group Mapper

1. 在 LDAP Provider 頁面，點擊 "Mappers" 頁籤
2. 點擊 "Add mapper"
3. 填入以下配置：

```yaml
Name: ldap-group-mapper
Mapper type: group-ldap-mapper
LDAP Groups DN: ou=groups,dc=example,dc=com
Group Name LDAP Attribute: cn
Group Object Classes: groupOfNames
Membership LDAP Attribute: member
Membership Attribute Type: DN
Membership User LDAP Attribute: uid
Mode: READ_ONLY
User Groups Retrieve Strategy: LOAD_GROUPS_BY_MEMBER_ATTRIBUTE
Drop non-existing groups during sync: ON
```

4. 點擊 "Save"
5. 點擊 "Sync LDAP groups to Keycloak"

#### Step 4: 建立 Realm Roles

1. 左側選單點擊 "Realm roles"
2. 建立以下角色：

| Role Name | Description |
|-----------|-------------|
| ADMIN | System Administrator |
| USER | Regular User |
| AUDITOR | Internal Auditor |

#### Step 5: 配置 Group-Role Mapping

1. 左側選單點擊 "Groups"
2. 選擇 `admins` 群組
3. 點擊 "Role mapping" 頁籤
4. 點擊 "Assign role"
5. 勾選 "ADMIN" 和 "USER"
6. 對其他群組重複此步驟：
   - `users` → USER
   - `auditors` → AUDITOR, USER

#### Step 6: 建立 Client

1. 左側選單點擊 "Clients"
2. 點擊 "Create client"
3. 填入以下配置：

```yaml
# General Settings
Client type: OpenID Connect
Client ID: spring-api
Name: Spring Boot API
Description: Spring Boot API Client

# Capability config
Client authentication: ON
Authorization: OFF
Authentication flow:
  - Standard flow: ON
  - Direct access grants: ON  # 用於 Resource Owner Password Credentials
  - Service accounts roles: OFF

# Login settings
Root URL: http://localhost:9090
Home URL: http://localhost:9090
Valid redirect URIs: 
  - http://localhost:9090/*
  - http://localhost:3000/*  # 前端（如需）
Valid post logout redirect URIs: +
Web origins: +
```

4. 點擊 "Save"
5. 複製 Client Secret（Credentials 頁籤）

#### Step 7: Token 配置

1. 進入 `spring-api` Client
2. 點擊 "Advanced" 頁籤
3. 配置 Token 有效期：

```yaml
Access token lifespan: 15 minutes
Client Session Idle: 30 minutes
Client Session Max: 8 hours
```

### 5.2 Realm Export (realm-export.json)

```json
{
  "id": "demo",
  "realm": "demo",
  "displayName": "Demo Realm",
  "enabled": true,
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
  "roles": {
    "realm": [
      {
        "name": "ADMIN",
        "description": "System Administrator",
        "composite": false
      },
      {
        "name": "USER",
        "description": "Regular User",
        "composite": false
      },
      {
        "name": "AUDITOR",
        "description": "Internal Auditor",
        "composite": false
      }
    ]
  },
  "clients": [
    {
      "clientId": "spring-api",
      "name": "Spring Boot API",
      "enabled": true,
      "clientAuthenticatorType": "client-secret",
      "secret": "your-client-secret-here",
      "redirectUris": [
        "http://localhost:9090/*",
        "http://localhost:3000/*"
      ],
      "webOrigins": ["+"],
      "standardFlowEnabled": true,
      "directAccessGrantsEnabled": true,
      "serviceAccountsEnabled": false,
      "publicClient": false,
      "protocol": "openid-connect",
      "attributes": {
        "access.token.lifespan": "900",
        "client.session.idle.timeout": "1800",
        "client.session.max.lifespan": "28800"
      }
    }
  ],
  "components": {
    "org.keycloak.storage.UserStorageProvider": [
      {
        "name": "OpenLDAP",
        "providerId": "ldap",
        "config": {
          "vendor": ["other"],
          "connectionUrl": ["ldap://openldap:389"],
          "usersDn": ["ou=users,dc=example,dc=com"],
          "bindDn": ["cn=admin,dc=example,dc=com"],
          "bindCredential": ["admin123"],
          "authType": ["simple"],
          "editMode": ["READ_ONLY"],
          "usernameLDAPAttribute": ["uid"],
          "rdnLDAPAttribute": ["uid"],
          "uuidLDAPAttribute": ["entryUUID"],
          "userObjectClasses": ["inetOrgPerson, posixAccount"],
          "importEnabled": ["true"],
          "syncRegistrations": ["false"],
          "fullSyncPeriod": ["604800"],
          "changedSyncPeriod": ["86400"]
        }
      }
    ]
  }
}
```

---

## 6. 部署腳本

### 6.1 setup.sh - 環境初始化

```bash
#!/bin/bash
# scripts/setup.sh - Initialize SSO RBAC PoC Environment

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

echo -e "${GREEN}========================================${NC}"
echo -e "${GREEN}  SSO RBAC PoC - Environment Setup     ${NC}"
echo -e "${GREEN}========================================${NC}"

# Check prerequisites
echo -e "\n${YELLOW}[1/6] Checking prerequisites...${NC}"

if ! command -v docker &> /dev/null; then
    echo -e "${RED}Error: Docker is not installed${NC}"
    exit 1
fi

if ! command -v docker-compose &> /dev/null && ! docker compose version &> /dev/null; then
    echo -e "${RED}Error: Docker Compose is not installed${NC}"
    exit 1
fi

echo -e "${GREEN}✓ Prerequisites check passed${NC}"

# Create directories
echo -e "\n${YELLOW}[2/6] Creating directories...${NC}"
mkdir -p ldap keycloak spring-api scripts
echo -e "${GREEN}✓ Directories created${NC}"

# Create .env if not exists
echo -e "\n${YELLOW}[3/6] Creating environment file...${NC}"
if [ ! -f .env ]; then
    cat > .env << 'EOF'
# OpenLDAP
LDAP_ADMIN_PASSWORD=admin123
LDAP_CONFIG_PASSWORD=config123
LDAP_PORT=389
LDAPS_PORT=636

# phpLDAPadmin
PHPLDAPADMIN_PORT=8081

# PostgreSQL
POSTGRES_DB=keycloak
POSTGRES_USER=keycloak
POSTGRES_PASSWORD=keycloak123

# Keycloak
KEYCLOAK_ADMIN=admin
KEYCLOAK_ADMIN_PASSWORD=admin123
KEYCLOAK_PORT=8080
KC_HOSTNAME=localhost

# Spring API
API_PORT=9090
EOF
    echo -e "${GREEN}✓ .env file created${NC}"
else
    echo -e "${GREEN}✓ .env file already exists${NC}"
fi

# Pull Docker images
echo -e "\n${YELLOW}[4/6] Pulling Docker images...${NC}"
docker pull osixia/openldap:1.5.0
docker pull osixia/phpldapadmin:0.9.0
docker pull postgres:15-alpine
docker pull quay.io/keycloak/keycloak:24.0
echo -e "${GREEN}✓ Docker images pulled${NC}"

# Start services
echo -e "\n${YELLOW}[5/6] Starting services...${NC}"
docker compose up -d
echo -e "${GREEN}✓ Services starting...${NC}"

# Wait for services to be healthy
echo -e "\n${YELLOW}[6/6] Waiting for services to be ready...${NC}"
echo "This may take 2-3 minutes..."

# Wait for Keycloak
KEYCLOAK_READY=false
for i in {1..60}; do
    if curl -s http://localhost:8080/health/ready | grep -q "UP"; then
        KEYCLOAK_READY=true
        break
    fi
    echo -n "."
    sleep 5
done

if [ "$KEYCLOAK_READY" = true ]; then
    echo -e "\n${GREEN}✓ All services are ready!${NC}"
else
    echo -e "\n${YELLOW}⚠ Keycloak is still starting, please wait...${NC}"
fi

# Print access information
echo -e "\n${GREEN}========================================${NC}"
echo -e "${GREEN}  Setup Complete!                       ${NC}"
echo -e "${GREEN}========================================${NC}"
echo ""
echo -e "Access URLs:"
echo -e "  ${YELLOW}Keycloak:${NC}       http://localhost:8080"
echo -e "                  Username: admin"
echo -e "                  Password: admin123"
echo ""
echo -e "  ${YELLOW}phpLDAPadmin:${NC}   http://localhost:8081"
echo -e "                  Login DN: cn=admin,dc=example,dc=com"
echo -e "                  Password: admin123"
echo ""
echo -e "  ${YELLOW}Spring API:${NC}     http://localhost:9090"
echo -e "                  Swagger:  http://localhost:9090/swagger-ui.html"
echo ""
echo -e "${YELLOW}LDAP Test Users:${NC}"
echo -e "  admin.user / admin123    (ADMIN role)"
echo -e "  normal.user / user123    (USER role)"
echo -e "  auditor.user / auditor123 (AUDITOR role)"
echo ""
echo -e "${YELLOW}Next Steps:${NC}"
echo -e "  1. Configure Keycloak Realm (see INFRA.md Section 5)"
echo -e "  2. Test API endpoints with Postman or curl"
echo ""
```

### 6.2 cleanup.sh - 環境清理

```bash
#!/bin/bash
# scripts/cleanup.sh - Cleanup SSO RBAC PoC Environment

set -e

RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'

echo -e "${YELLOW}========================================${NC}"
echo -e "${YELLOW}  SSO RBAC PoC - Environment Cleanup   ${NC}"
echo -e "${YELLOW}========================================${NC}"

# Confirm cleanup
read -p "This will remove all containers and volumes. Continue? (y/N) " -n 1 -r
echo
if [[ ! $REPLY =~ ^[Yy]$ ]]; then
    echo -e "${RED}Cleanup cancelled${NC}"
    exit 1
fi

# Stop and remove containers
echo -e "\n${YELLOW}[1/3] Stopping containers...${NC}"
docker compose down
echo -e "${GREEN}✓ Containers stopped${NC}"

# Remove volumes
echo -e "\n${YELLOW}[2/3] Removing volumes...${NC}"
docker volume rm sso-ldap-data sso-ldap-config sso-postgres-data 2>/dev/null || true
echo -e "${GREEN}✓ Volumes removed${NC}"

# Remove network
echo -e "\n${YELLOW}[3/3] Removing network...${NC}"
docker network rm sso-network 2>/dev/null || true
echo -e "${GREEN}✓ Network removed${NC}"

echo -e "\n${GREEN}========================================${NC}"
echo -e "${GREEN}  Cleanup Complete!                     ${NC}"
echo -e "${GREEN}========================================${NC}"
```

### 6.3 test-api.sh - API 測試腳本

```bash
#!/bin/bash
# scripts/test-api.sh - Test API Endpoints

set -e

RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

# Configuration
KEYCLOAK_URL="http://localhost:8080"
API_URL="http://localhost:9090"
REALM="demo"
CLIENT_ID="spring-api"
CLIENT_SECRET="your-client-secret-here"  # 從 Keycloak 取得

echo -e "${GREEN}========================================${NC}"
echo -e "${GREEN}  SSO RBAC PoC - API Testing           ${NC}"
echo -e "${GREEN}========================================${NC}"

# Function to get access token
get_token() {
    local username=$1
    local password=$2
    
    curl -s -X POST "${KEYCLOAK_URL}/realms/${REALM}/protocol/openid-connect/token" \
        -H "Content-Type: application/x-www-form-urlencoded" \
        -d "client_id=${CLIENT_ID}" \
        -d "client_secret=${CLIENT_SECRET}" \
        -d "grant_type=password" \
        -d "username=${username}" \
        -d "password=${password}" \
        | jq -r '.access_token'
}

# Function to call API
call_api() {
    local method=$1
    local endpoint=$2
    local token=$3
    
    if [ -z "$token" ]; then
        curl -s -X ${method} "${API_URL}${endpoint}" \
            -H "Content-Type: application/json"
    else
        curl -s -X ${method} "${API_URL}${endpoint}" \
            -H "Content-Type: application/json" \
            -H "Authorization: Bearer ${token}"
    fi
}

# Test 1: Public Endpoints
echo -e "\n${BLUE}[Test 1] Public Endpoints (No Auth Required)${NC}"
echo -e "${YELLOW}GET /api/public/health${NC}"
call_api "GET" "/api/public/health" | jq .
echo -e "${YELLOW}GET /api/public/info${NC}"
call_api "GET" "/api/public/info" | jq .

# Test 2: Protected Endpoint without Token
echo -e "\n${BLUE}[Test 2] Protected Endpoint without Token${NC}"
echo -e "${YELLOW}GET /api/user/resources (expected: 401)${NC}"
call_api "GET" "/api/user/resources" | jq .

# Test 3: User Role Access
echo -e "\n${BLUE}[Test 3] User Role Access${NC}"
echo -e "${YELLOW}Getting token for normal.user...${NC}"
USER_TOKEN=$(get_token "normal.user" "user123")

if [ "$USER_TOKEN" != "null" ] && [ -n "$USER_TOKEN" ]; then
    echo -e "${GREEN}✓ Token obtained${NC}"
    
    echo -e "\n${YELLOW}GET /api/user/profile (expected: 200)${NC}"
    call_api "GET" "/api/user/profile" "$USER_TOKEN" | jq .
    
    echo -e "\n${YELLOW}GET /api/user/resources (expected: 200)${NC}"
    call_api "GET" "/api/user/resources" "$USER_TOKEN" | jq .
    
    echo -e "\n${YELLOW}GET /api/admin/users (expected: 403 - User cannot access admin)${NC}"
    call_api "GET" "/api/admin/users" "$USER_TOKEN" | jq .
else
    echo -e "${RED}✗ Failed to get token for normal.user${NC}"
fi

# Test 4: Admin Role Access
echo -e "\n${BLUE}[Test 4] Admin Role Access${NC}"
echo -e "${YELLOW}Getting token for admin.user...${NC}"
ADMIN_TOKEN=$(get_token "admin.user" "admin123")

if [ "$ADMIN_TOKEN" != "null" ] && [ -n "$ADMIN_TOKEN" ]; then
    echo -e "${GREEN}✓ Token obtained${NC}"
    
    echo -e "\n${YELLOW}GET /api/admin/users (expected: 200)${NC}"
    call_api "GET" "/api/admin/users" "$ADMIN_TOKEN" | jq .
    
    echo -e "\n${YELLOW}GET /api/admin/roles (expected: 200)${NC}"
    call_api "GET" "/api/admin/roles" "$ADMIN_TOKEN" | jq .
    
    echo -e "\n${YELLOW}GET /api/admin/settings (expected: 200)${NC}"
    call_api "GET" "/api/admin/settings" "$ADMIN_TOKEN" | jq .
else
    echo -e "${RED}✗ Failed to get token for admin.user${NC}"
fi

# Summary
echo -e "\n${GREEN}========================================${NC}"
echo -e "${GREEN}  Testing Complete!                     ${NC}"
echo -e "${GREEN}========================================${NC}"
```

---

## 7. 服務端點與存取資訊

### 7.1 存取資訊一覽

| Service | URL | Credentials |
|---------|-----|-------------|
| Keycloak Admin | http://localhost:8080 | admin / admin123 |
| phpLDAPadmin | http://localhost:8081 | cn=admin,dc=example,dc=com / admin123 |
| Spring API | http://localhost:9090 | (需 JWT Token) |
| Swagger UI | http://localhost:9090/swagger-ui.html | - |
| OpenLDAP | ldap://localhost:389 | cn=admin,dc=example,dc=com / admin123 |

### 7.2 測試帳號

| Username | Password | LDAP Groups | Keycloak Roles |
|----------|----------|-------------|----------------|
| admin.user | admin123 | admins, users | ADMIN, USER |
| normal.user | user123 | users | USER |
| auditor.user | auditor123 | auditors | AUDITOR, USER |
| test.user | test123 | users | USER |

### 7.3 Keycloak OAuth Endpoints

```
# Token Endpoint (取得 Access Token)
POST http://localhost:8080/realms/demo/protocol/openid-connect/token

# Authorization Endpoint (授權碼流程)
GET http://localhost:8080/realms/demo/protocol/openid-connect/auth

# UserInfo Endpoint
GET http://localhost:8080/realms/demo/protocol/openid-connect/userinfo

# JWKS Endpoint (公鑰)
GET http://localhost:8080/realms/demo/protocol/openid-connect/certs

# Logout Endpoint
POST http://localhost:8080/realms/demo/protocol/openid-connect/logout

# OpenID Configuration
GET http://localhost:8080/realms/demo/.well-known/openid-configuration
```

---

## 8. 取得 Token 範例

### 8.1 使用 curl 取得 Token

```bash
# Resource Owner Password Credentials Grant
curl -X POST "http://localhost:8080/realms/demo/protocol/openid-connect/token" \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "client_id=spring-api" \
  -d "client_secret=YOUR_CLIENT_SECRET" \
  -d "grant_type=password" \
  -d "username=admin.user" \
  -d "password=admin123"

# 回應範例
{
  "access_token": "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9...",
  "expires_in": 900,
  "refresh_expires_in": 1800,
  "refresh_token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "token_type": "Bearer",
  "not-before-policy": 0,
  "session_state": "abc123...",
  "scope": "openid profile email"
}
```

### 8.2 使用 Token 呼叫 API

```bash
# 設定 Token 變數
TOKEN="eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9..."

# 呼叫受保護 API
curl -X GET "http://localhost:9090/api/user/profile" \
  -H "Authorization: Bearer ${TOKEN}" \
  -H "Content-Type: application/json"

# 呼叫管理員 API
curl -X GET "http://localhost:9090/api/admin/users" \
  -H "Authorization: Bearer ${TOKEN}" \
  -H "Content-Type: application/json"
```

---

## 9. 故障排除

### 9.1 常見問題

#### OpenLDAP 無法啟動

```bash
# 檢查日誌
docker logs sso-openldap

# 常見原因：
# 1. bootstrap.ldif 語法錯誤
# 2. 權限問題
# 解決方案：刪除 volumes 重新啟動
docker compose down -v
docker compose up -d
```

#### Keycloak 無法連接 LDAP

```bash
# 確認 OpenLDAP 可存取
docker exec sso-keycloak \
  ldapsearch -x -H ldap://openldap:389 \
  -D "cn=admin,dc=example,dc=com" \
  -w admin123 \
  -b "dc=example,dc=com"

# 常見原因：
# 1. 網路問題 - 確認都在 sso-network
# 2. 認證資訊錯誤
# 3. OpenLDAP 尚未準備好
```

#### Spring API 啟動失敗

```bash
# 檢查日誌
docker logs sso-spring-api

# 常見原因：
# 1. Keycloak 尚未準備好
# 2. JWKS URI 配置錯誤
# 解決方案：等待 Keycloak 完全啟動後重啟
docker compose restart spring-api
```

#### Token 驗證失敗

```bash
# 確認 Token 有效性
# 將 Token 貼到 https://jwt.io 解析

# 常見原因：
# 1. Token 過期
# 2. Issuer 不匹配（localhost vs container name）
# 3. JWKS 快取問題

# 解決方案：確認 issuer-uri 配置正確
# application-docker.yml 中應使用容器名稱
```

### 9.2 日誌檢視

```bash
# 檢視所有服務日誌
docker compose logs -f

# 檢視特定服務日誌
docker compose logs -f keycloak
docker compose logs -f openldap
docker compose logs -f spring-api

# 檢視最近 100 行
docker compose logs --tail=100 keycloak
```

### 9.3 健康檢查

```bash
# OpenLDAP
docker exec sso-openldap ldapsearch -x -H ldap://localhost \
  -b "dc=example,dc=com" -D "cn=admin,dc=example,dc=com" -w admin123

# Keycloak
curl http://localhost:8080/health/ready

# Spring API
curl http://localhost:9090/actuator/health

# PostgreSQL
docker exec sso-postgres pg_isready -U keycloak
```

---

## 10. 附錄

### 10.1 相關文件

- [PRD.md](./PRD.md) - 業務需求文件
- [TECH.md](./TECH.md) - 技術架構文件
- [Keycloak Documentation](https://www.keycloak.org/documentation)
- [OpenLDAP Admin Guide](https://www.openldap.org/doc/admin26/)
- [Spring Security Reference](https://docs.spring.io/spring-security/reference/)

### 10.2 版本歷史

| 版本 | 日期 | 變更說明 |
|------|------|----------|
| 1.0 | 2025-01-10 | 初始版本 |
