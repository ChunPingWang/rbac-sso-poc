# CONSTITUTION: 開發準則與架構規範

## 文件資訊

| 項目 | 內容 |
|------|------|
| 文件版本 | 1.0 |
| 建立日期 | 2025-01-10 |
| 專案代號 | SSO-RBAC-POC |
| 適用範圍 | 全專案開發團隊 |

---

## 1. 核心原則

本專案遵循以下核心開發原則，所有程式碼必須符合這些規範：

### 1.1 開發哲學

```
┌─────────────────────────────────────────────────────────────────┐
│                      開發準則金字塔                               │
├─────────────────────────────────────────────────────────────────┤
│                                                                  │
│                        ┌─────────┐                              │
│                        │  品質   │                              │
│                        │ Quality │                              │
│                       ─┴─────────┴─                             │
│                      ┌─────────────┐                            │
│                      │   可測試性   │                            │
│                      │ Testability │                            │
│                     ─┴─────────────┴─                           │
│                    ┌─────────────────┐                          │
│                    │   SOLID 原則    │                          │
│                    │   Principles    │                          │
│                   ─┴─────────────────┴─                         │
│                  ┌───────────────────────┐                      │
│                  │    六角形架構          │                      │
│                  │ Hexagonal Architecture │                      │
│                 ─┴───────────────────────┴─                     │
│                ┌───────────────────────────┐                    │
│                │     領域驅動設計 (DDD)      │                    │
│                │   Domain-Driven Design    │                    │
│               ─┴───────────────────────────┴─                   │
│                                                                  │
└─────────────────────────────────────────────────────────────────┘
```

---

## 2. 六角形架構 (Hexagonal Architecture)

### 2.1 架構概覽

六角形架構（又稱 Ports and Adapters）將應用程式分為三個主要區域：

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                           Infrastructure Layer (外圈)                        │
│   ┌─────────────┐  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐       │
│   │   Web/REST  │  │  Database   │  │  Keycloak   │  │    LDAP     │       │
│   │  Adapters   │  │  Adapters   │  │   Adapter   │  │   Adapter   │       │
│   └──────┬──────┘  └──────┬──────┘  └──────┬──────┘  └──────┬──────┘       │
│          │                │                │                │               │
│   ┌──────┴──────┐  ┌──────┴──────┐  ┌──────┴──────┐  ┌──────┴──────┐       │
│   │  Input Port │  │ Output Port │  │ Output Port │  │ Output Port │       │
│   │ (Driving)   │  │  (Driven)   │  │  (Driven)   │  │  (Driven)   │       │
│   └──────┬──────┘  └──────┬──────┘  └──────┬──────┘  └──────┬──────┘       │
│          │                │                │                │               │
│   ┌──────┴────────────────┴────────────────┴────────────────┴──────┐       │
│   │                                                                 │       │
│   │                    Application Layer (中圈)                     │       │
│   │   ┌─────────────────────────────────────────────────────────┐  │       │
│   │   │                  Use Cases / Services                    │  │       │
│   │   │  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐   │  │       │
│   │   │  │ UserUseCase  │  │ RoleUseCase  │  │ AuthUseCase  │   │  │       │
│   │   │  └──────────────┘  └──────────────┘  └──────────────┘   │  │       │
│   │   └─────────────────────────────────────────────────────────┘  │       │
│   │                              │                                  │       │
│   │   ┌──────────────────────────┴──────────────────────────┐      │       │
│   │   │                                                      │      │       │
│   │   │                  Domain Layer (內圈)                 │      │       │
│   │   │   ┌────────────┐  ┌────────────┐  ┌────────────┐    │      │       │
│   │   │   │  Entities  │  │   Value    │  │  Domain    │    │      │       │
│   │   │   │            │  │  Objects   │  │  Services  │    │      │       │
│   │   │   └────────────┘  └────────────┘  └────────────┘    │      │       │
│   │   │                                                      │      │       │
│   │   │   ┌────────────────────────────────────────────┐    │      │       │
│   │   │   │         Repository Interfaces (Ports)       │    │      │       │
│   │   │   └────────────────────────────────────────────┘    │      │       │
│   │   │                                                      │      │       │
│   │   └──────────────────────────────────────────────────────┘      │       │
│   │                                                                 │       │
│   └─────────────────────────────────────────────────────────────────┘       │
│                                                                              │
└──────────────────────────────────────────────────────────────────────────────┘
```

### 2.2 層級定義

#### 2.2.1 Domain Layer（領域層 - 內圈）

**職責**：包含核心業務邏輯，完全獨立於任何框架或技術。

**包含元素**：
- **Entities（實體）**：具有唯一識別的領域物件
- **Value Objects（值物件）**：不可變的、無識別的物件
- **Domain Services（領域服務）**：跨實體的業務邏輯
- **Repository Interfaces（倉儲介面）**：資料存取的抽象定義
- **Domain Events（領域事件）**：業務事件的定義

**禁止事項**：
- 不得依賴任何框架（Spring, JPA, etc.）
- 不得包含任何 I/O 操作
- 不得直接存取資料庫或外部服務

```java
// 正確範例：Domain Entity
package com.example.sso.domain.entity;

public class User {
    private final UserId id;
    private Username username;
    private Email email;
    private List<Role> roles;

    // 領域行為
    public void assignRole(Role role) {
        if (roles.contains(role)) {
            throw new DomainException("Role already assigned");
        }
        roles.add(role);
        // 可觸發領域事件
    }
}

// 正確範例：Repository Interface (Port)
package com.example.sso.domain.port.outbound;

public interface UserRepository {
    Optional<User> findById(UserId id);
    Optional<User> findByUsername(Username username);
    void save(User user);
}
```

#### 2.2.2 Application Layer（應用層 - 中圈）

**職責**：協調領域物件完成使用案例，管理交易邊界。

**包含元素**：
- **Use Cases / Application Services**：執行特定業務操作
- **Input Ports（輸入埠）**：定義應用層提供的服務介面
- **Output Ports（輸出埠）**：定義應用層需要的外部依賴介面
- **DTOs**：資料傳輸物件

**禁止事項**：
- 不得包含業務邏輯（應委託給 Domain Layer）
- 不得直接依賴 Infrastructure 具體實作

```java
// 正確範例：Input Port
package com.example.sso.application.port.inbound;

public interface AssignRoleUseCase {
    void assignRole(AssignRoleCommand command);
}

// 正確範例：Application Service 實作 Input Port
package com.example.sso.application.service;

@Service
@Transactional
public class RoleService implements AssignRoleUseCase {

    private final UserRepository userRepository;  // Output Port
    private final RoleRepository roleRepository;  // Output Port

    @Override
    public void assignRole(AssignRoleCommand command) {
        User user = userRepository.findById(command.getUserId())
            .orElseThrow(() -> new UserNotFoundException(command.getUserId()));

        Role role = roleRepository.findById(command.getRoleId())
            .orElseThrow(() -> new RoleNotFoundException(command.getRoleId()));

        user.assignRole(role);  // 委託領域邏輯

        userRepository.save(user);
    }
}
```

#### 2.2.3 Infrastructure Layer（基礎設施層 - 外圈）

**職責**：實作所有與外部世界的互動，包括框架整合。

**包含元素**：
- **Web Adapters（REST Controllers）**：處理 HTTP 請求
- **Persistence Adapters**：資料庫存取實作
- **External Service Adapters**：外部服務整合（Keycloak, LDAP）
- **Configuration**：Spring 配置、安全配置等

**規範**：
- 所有框架依賴只能存在於此層
- 必須透過實作 Port 介面來與內圈溝通

```java
// 正確範例：Persistence Adapter 實作 Output Port
package com.example.sso.infrastructure.persistence;

@Repository
public class JpaUserRepository implements UserRepository {

    private final UserJpaRepository jpaRepository;
    private final UserMapper mapper;

    @Override
    public Optional<User> findById(UserId id) {
        return jpaRepository.findById(id.getValue())
            .map(mapper::toDomain);
    }

    @Override
    public void save(User user) {
        UserEntity entity = mapper.toEntity(user);
        jpaRepository.save(entity);
    }
}

// 正確範例：Web Adapter 使用 Input Port
package com.example.sso.infrastructure.web;

@RestController
@RequestMapping("/api/admin")
public class RoleController {

    private final AssignRoleUseCase assignRoleUseCase;  // Input Port

    @PostMapping("/users/{userId}/roles")
    public ResponseEntity<Void> assignRole(
            @PathVariable Long userId,
            @RequestBody AssignRoleRequest request) {

        AssignRoleCommand command = new AssignRoleCommand(
            new UserId(userId),
            new RoleId(request.getRoleId())
        );

        assignRoleUseCase.assignRole(command);

        return ResponseEntity.ok().build();
    }
}
```

### 2.3 專案目錄結構

```
src/main/java/com/example/sso/
│
├── domain/                          # 領域層 (內圈) - 無框架依賴
│   ├── entity/                      # 領域實體
│   │   ├── User.java
│   │   ├── Role.java
│   │   └── Permission.java
│   │
│   ├── valueobject/                 # 值物件
│   │   ├── UserId.java
│   │   ├── Username.java
│   │   ├── Email.java
│   │   └── RoleId.java
│   │
│   ├── service/                     # 領域服務
│   │   └── AuthorizationDomainService.java
│   │
│   ├── event/                       # 領域事件
│   │   ├── UserCreatedEvent.java
│   │   └── RoleAssignedEvent.java
│   │
│   ├── exception/                   # 領域例外
│   │   ├── DomainException.java
│   │   └── InvalidRoleAssignmentException.java
│   │
│   └── port/                        # 埠口定義
│       └── outbound/                # 輸出埠 (領域層需要的外部服務)
│           ├── UserRepository.java
│           └── RoleRepository.java
│
├── application/                     # 應用層 (中圈)
│   ├── port/
│   │   └── inbound/                 # 輸入埠 (Use Case 介面)
│   │       ├── CreateUserUseCase.java
│   │       ├── AssignRoleUseCase.java
│   │       └── AuthenticateUserUseCase.java
│   │
│   ├── service/                     # 應用服務 (實作 Use Case)
│   │   ├── UserApplicationService.java
│   │   └── RoleApplicationService.java
│   │
│   └── dto/                         # 命令與查詢物件
│       ├── command/
│       │   ├── CreateUserCommand.java
│       │   └── AssignRoleCommand.java
│       └── query/
│           └── UserQueryResult.java
│
└── infrastructure/                  # 基礎設施層 (外圈) - 框架依賴
    ├── web/                         # Web Adapter
    │   ├── controller/
    │   │   ├── UserController.java
    │   │   ├── RoleController.java
    │   │   └── PublicController.java
    │   │
    │   ├── request/                 # HTTP 請求物件
    │   │   └── CreateUserRequest.java
    │   │
    │   ├── response/                # HTTP 回應物件
    │   │   └── UserResponse.java
    │   │
    │   └── exception/               # Web 例外處理
    │       └── GlobalExceptionHandler.java
    │
    ├── persistence/                 # 持久化 Adapter
    │   ├── repository/
    │   │   └── JpaUserRepository.java
    │   │
    │   ├── entity/                  # JPA Entity (非領域實體)
    │   │   └── UserEntity.java
    │   │
    │   └── mapper/
    │       └── UserMapper.java
    │
    ├── security/                    # 安全 Adapter
    │   ├── KeycloakAdapter.java
    │   └── JwtAuthenticationFilter.java
    │
    ├── external/                    # 外部服務 Adapter
    │   └── LdapAdapter.java
    │
    └── config/                      # Spring 配置
        ├── SecurityConfig.java
        ├── JpaConfig.java
        └── WebConfig.java
```

### 2.4 依賴規則

```
┌─────────────────────────────────────────────────────────────┐
│                       依賴方向圖                             │
├─────────────────────────────────────────────────────────────┤
│                                                              │
│   Infrastructure ──────────────▶ Application                │
│         │                              │                     │
│         │                              │                     │
│         │                              ▼                     │
│         └──────────────────────▶   Domain                   │
│                                                              │
│   ✓ 依賴只能由外圈指向內圈                                   │
│   ✗ 內圈不得依賴外圈                                         │
│                                                              │
└─────────────────────────────────────────────────────────────┘
```

**Maven 模組配置建議**（可選，大型專案適用）：

```xml
<!-- domain 模組 - 無任何框架依賴 -->
<module>sso-domain</module>

<!-- application 模組 - 只依賴 domain -->
<module>sso-application</module>

<!-- infrastructure 模組 - 依賴 application 和所有框架 -->
<module>sso-infrastructure</module>
```

---

## 3. SOLID 原則

### 3.1 Single Responsibility Principle (單一職責原則)

> **一個類別應該只有一個改變的理由**

```java
// 違反 SRP - 混合多種職責
public class UserService {
    public void createUser(User user) { /* ... */ }
    public void sendWelcomeEmail(User user) { /* ... */ }  // Email 職責
    public void generateUserReport(User user) { /* ... */ } // 報表職責
}

// 遵循 SRP - 分離職責
public class UserApplicationService {
    public void createUser(CreateUserCommand command) { /* ... */ }
}

public class EmailNotificationService {
    public void sendWelcomeEmail(User user) { /* ... */ }
}

public class UserReportService {
    public void generateUserReport(User user) { /* ... */ }
}
```

### 3.2 Open/Closed Principle (開放封閉原則)

> **對擴展開放，對修改封閉**

```java
// 違反 OCP - 每新增驗證規則需修改此類別
public class PasswordValidator {
    public boolean validate(String password) {
        if (password.length() < 8) return false;
        if (!password.matches(".*[A-Z].*")) return false;
        // 每次新增規則都要修改這裡
        return true;
    }
}

// 遵循 OCP - 透過抽象擴展
public interface PasswordRule {
    boolean validate(String password);
    String getErrorMessage();
}

public class MinLengthRule implements PasswordRule {
    private final int minLength;
    @Override public boolean validate(String password) {
        return password.length() >= minLength;
    }
}

public class UppercaseRule implements PasswordRule {
    @Override public boolean validate(String password) {
        return password.matches(".*[A-Z].*");
    }
}

public class PasswordValidator {
    private final List<PasswordRule> rules;

    public ValidationResult validate(String password) {
        return rules.stream()
            .filter(rule -> !rule.validate(password))
            .findFirst()
            .map(rule -> ValidationResult.failure(rule.getErrorMessage()))
            .orElse(ValidationResult.success());
    }
}
```

### 3.3 Liskov Substitution Principle (里氏替換原則)

> **子類別必須能夠替換其父類別**

```java
// 違反 LSP
public class Rectangle {
    protected int width, height;
    public void setWidth(int w) { width = w; }
    public void setHeight(int h) { height = h; }
    public int getArea() { return width * height; }
}

public class Square extends Rectangle {
    @Override
    public void setWidth(int w) {
        width = w;
        height = w;  // 改變了父類別的行為預期
    }
}

// 遵循 LSP - 使用組合或重新設計繼承階層
public interface Shape {
    int getArea();
}

public class Rectangle implements Shape {
    private final int width, height;
    public Rectangle(int width, int height) {
        this.width = width;
        this.height = height;
    }
    @Override public int getArea() { return width * height; }
}

public class Square implements Shape {
    private final int side;
    public Square(int side) { this.side = side; }
    @Override public int getArea() { return side * side; }
}
```

### 3.4 Interface Segregation Principle (介面隔離原則)

> **客戶端不應被迫依賴它不使用的介面**

```java
// 違反 ISP - 胖介面
public interface UserRepository {
    User findById(Long id);
    List<User> findAll();
    void save(User user);
    void delete(User user);
    List<User> findByRole(String role);
    List<User> searchByKeyword(String keyword);
    UserStatistics getStatistics();
}

// 遵循 ISP - 分離介面
public interface UserReadRepository {
    Optional<User> findById(UserId id);
    List<User> findAll();
}

public interface UserWriteRepository {
    void save(User user);
    void delete(User user);
}

public interface UserSearchRepository {
    List<User> findByRole(RoleId roleId);
    List<User> searchByKeyword(String keyword);
}

public interface UserStatisticsRepository {
    UserStatistics getStatistics();
}
```

### 3.5 Dependency Inversion Principle (依賴反轉原則)

> **高層模組不應依賴低層模組，兩者都應依賴抽象**

```java
// 違反 DIP - 直接依賴具體實作
public class UserService {
    private final JpaUserRepository repository;  // 直接依賴 JPA
    private final SmtpEmailSender emailSender;   // 直接依賴 SMTP
}

// 遵循 DIP - 依賴抽象
// Domain Layer 定義介面
public interface UserRepository {
    Optional<User> findById(UserId id);
    void save(User user);
}

public interface NotificationSender {
    void send(Notification notification);
}

// Application Layer 使用介面
public class UserApplicationService {
    private final UserRepository userRepository;      // 依賴抽象
    private final NotificationSender notificationSender;  // 依賴抽象

    public UserApplicationService(
            UserRepository userRepository,
            NotificationSender notificationSender) {
        this.userRepository = userRepository;
        this.notificationSender = notificationSender;
    }
}

// Infrastructure Layer 提供實作
@Repository
public class JpaUserRepository implements UserRepository { /* ... */ }

@Component
public class SmtpNotificationSender implements NotificationSender { /* ... */ }
```

---

## 4. 領域驅動設計 (DDD)

### 4.1 戰略設計

#### 4.1.1 Bounded Context（限界上下文）

本專案定義以下 Bounded Contexts：

```
┌─────────────────────────────────────────────────────────────────────┐
│                        Bounded Contexts                              │
├─────────────────────────────────────────────────────────────────────┤
│                                                                      │
│   ┌────────────────────┐      ┌────────────────────┐               │
│   │    Identity &      │      │    Authorization   │               │
│   │    Access Context  │◄────►│      Context       │               │
│   │                    │      │                    │               │
│   │ - User             │      │ - Role             │               │
│   │ - Credential       │      │ - Permission       │               │
│   │ - Authentication   │      │ - Policy           │               │
│   └────────────────────┘      └────────────────────┘               │
│            │                           │                            │
│            │                           │                            │
│            ▼                           ▼                            │
│   ┌────────────────────────────────────────────────┐               │
│   │                  Audit Context                  │               │
│   │                                                 │               │
│   │ - AuditEvent                                   │               │
│   │ - AuditTrail                                   │               │
│   └────────────────────────────────────────────────┘               │
│                                                                      │
└─────────────────────────────────────────────────────────────────────┘
```

#### 4.1.2 Ubiquitous Language（通用語言）

| 術語 | 定義 | Context |
|------|------|---------|
| User | 系統使用者，具有唯一識別與認證資訊 | Identity |
| Role | 角色，代表一組權限的集合 | Authorization |
| Permission | 權限，定義對特定資源的操作許可 | Authorization |
| Token | JWT 存取憑證，包含使用者身份與角色資訊 | Identity |
| Realm | Keycloak 的租戶隔離單位 | Identity |

### 4.2 戰術設計

#### 4.2.1 Aggregate（聚合）

```java
// User Aggregate Root
public class User {
    private UserId id;                    // Entity ID
    private Username username;            // Value Object
    private Email email;                  // Value Object
    private List<RoleAssignment> roleAssignments;  // Entity 集合
    private UserStatus status;            // Value Object

    // 所有修改必須透過 Aggregate Root
    public void assignRole(Role role, AssignedBy assignedBy) {
        validateCanAssignRole(role);
        roleAssignments.add(new RoleAssignment(role, assignedBy));
        // 發布領域事件
        DomainEvents.publish(new RoleAssignedEvent(this.id, role.getId()));
    }

    public void revokeRole(RoleId roleId) {
        roleAssignments.removeIf(ra -> ra.getRoleId().equals(roleId));
    }

    private void validateCanAssignRole(Role role) {
        if (roleAssignments.stream().anyMatch(ra -> ra.getRoleId().equals(role.getId()))) {
            throw new InvalidRoleAssignmentException("Role already assigned");
        }
    }
}
```

#### 4.2.2 Value Object（值物件）

```java
// Value Object - 不可變、無識別、值相等
public final class Email {
    private final String value;

    public Email(String value) {
        if (value == null || !value.matches("^[\\w.-]+@[\\w.-]+\\.[a-zA-Z]{2,}$")) {
            throw new InvalidEmailException(value);
        }
        this.value = value.toLowerCase();
    }

    public String getValue() {
        return value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Email email = (Email) o;
        return value.equals(email.value);
    }

    @Override
    public int hashCode() {
        return value.hashCode();
    }
}

// Value Object - Username
public final class Username {
    private static final int MIN_LENGTH = 3;
    private static final int MAX_LENGTH = 50;

    private final String value;

    public Username(String value) {
        if (value == null || value.length() < MIN_LENGTH || value.length() > MAX_LENGTH) {
            throw new InvalidUsernameException(value);
        }
        if (!value.matches("^[a-zA-Z0-9._-]+$")) {
            throw new InvalidUsernameException("Username contains invalid characters");
        }
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}
```

#### 4.2.3 Domain Event（領域事件）

```java
// 領域事件基底類別
public abstract class DomainEvent {
    private final Instant occurredOn;
    private final String eventId;

    protected DomainEvent() {
        this.occurredOn = Instant.now();
        this.eventId = UUID.randomUUID().toString();
    }

    public Instant getOccurredOn() { return occurredOn; }
    public String getEventId() { return eventId; }
}

// 具體領域事件
public class UserCreatedEvent extends DomainEvent {
    private final UserId userId;
    private final Username username;

    public UserCreatedEvent(UserId userId, Username username) {
        super();
        this.userId = userId;
        this.username = username;
    }
}

public class RoleAssignedEvent extends DomainEvent {
    private final UserId userId;
    private final RoleId roleId;

    public RoleAssignedEvent(UserId userId, RoleId roleId) {
        super();
        this.userId = userId;
        this.roleId = roleId;
    }
}
```

#### 4.2.4 Domain Service（領域服務）

```java
// 領域服務 - 跨聚合的業務邏輯
public class AuthorizationDomainService {

    public boolean hasPermission(User user, Permission requiredPermission) {
        return user.getRoleAssignments().stream()
            .flatMap(ra -> ra.getRole().getPermissions().stream())
            .anyMatch(p -> p.implies(requiredPermission));
    }

    public Set<Permission> getEffectivePermissions(User user) {
        return user.getRoleAssignments().stream()
            .flatMap(ra -> ra.getRole().getPermissions().stream())
            .collect(Collectors.toSet());
    }
}
```

---

## 5. 測試驅動開發 (TDD)

### 5.1 TDD 流程

```
┌─────────────────────────────────────────────────────────────────┐
│                        TDD 紅綠重構循環                          │
├─────────────────────────────────────────────────────────────────┤
│                                                                  │
│                      ┌─────────────┐                            │
│                      │   1. RED    │                            │
│                      │ 寫失敗測試  │                            │
│                      └──────┬──────┘                            │
│                             │                                    │
│            ┌────────────────┴────────────────┐                  │
│            │                                  │                  │
│            ▼                                  │                  │
│     ┌─────────────┐                          │                  │
│     │  2. GREEN   │                          │                  │
│     │ 寫最少代碼  │                          │                  │
│     │  讓測試通過  │                          │                  │
│     └──────┬──────┘                          │                  │
│            │                                  │                  │
│            ▼                                  │                  │
│     ┌─────────────┐                          │                  │
│     │ 3. REFACTOR │                          │                  │
│     │   重構程式   │──────────────────────────┘                  │
│     │  保持測試通過 │                                            │
│     └─────────────┘                                             │
│                                                                  │
└─────────────────────────────────────────────────────────────────┘
```

### 5.2 測試範例

```java
// 1. RED - 先寫測試
class UserTest {

    @Test
    void should_create_user_with_valid_data() {
        // Given
        Username username = new Username("john.doe");
        Email email = new Email("john@example.com");

        // When
        User user = User.create(username, email);

        // Then
        assertThat(user.getUsername()).isEqualTo(username);
        assertThat(user.getEmail()).isEqualTo(email);
        assertThat(user.getStatus()).isEqualTo(UserStatus.ACTIVE);
    }

    @Test
    void should_fail_when_creating_user_with_invalid_email() {
        // Given
        Username username = new Username("john.doe");

        // When & Then
        assertThatThrownBy(() -> new Email("invalid-email"))
            .isInstanceOf(InvalidEmailException.class);
    }

    @Test
    void should_assign_role_to_user() {
        // Given
        User user = createTestUser();
        Role adminRole = createAdminRole();
        AssignedBy assignedBy = new AssignedBy(new UserId(1L));

        // When
        user.assignRole(adminRole, assignedBy);

        // Then
        assertThat(user.hasRole(adminRole.getId())).isTrue();
    }

    @Test
    void should_reject_duplicate_role_assignment() {
        // Given
        User user = createTestUser();
        Role adminRole = createAdminRole();
        AssignedBy assignedBy = new AssignedBy(new UserId(1L));
        user.assignRole(adminRole, assignedBy);

        // When & Then
        assertThatThrownBy(() -> user.assignRole(adminRole, assignedBy))
            .isInstanceOf(InvalidRoleAssignmentException.class)
            .hasMessageContaining("already assigned");
    }
}

// 2. GREEN - 實作剛好讓測試通過的程式碼
// 3. REFACTOR - 重構，保持測試通過
```

### 5.3 測試金字塔

```
                    ┌─────────────┐
                    │     E2E     │  ← 少量、耗時
                    │   Tests     │
                   ─┴─────────────┴─
                  ┌─────────────────┐
                  │  Integration    │  ← 適量
                  │     Tests       │
                 ─┴─────────────────┴─
                ┌───────────────────────┐
                │      Unit Tests       │  ← 大量、快速
                │                       │
                └───────────────────────┘
```

---

## 6. 行為驅動開發 (BDD)

### 6.1 BDD 流程

```
┌─────────────────────────────────────────────────────────────────┐
│                        BDD 開發流程                              │
├─────────────────────────────────────────────────────────────────┤
│                                                                  │
│   1. Discovery    →    2. Formulation    →    3. Automation     │
│   (探索)               (格式化)               (自動化)           │
│                                                                  │
│   ┌───────────┐       ┌───────────┐       ┌───────────┐        │
│   │  Example  │       │  Given/   │       │ Automated │        │
│   │  Mapping  │  ──►  │  When/    │  ──►  │   Tests   │        │
│   │           │       │  Then     │       │           │        │
│   └───────────┘       └───────────┘       └───────────┘        │
│                                                                  │
└─────────────────────────────────────────────────────────────────┘
```

### 6.2 Gherkin 規格範例

```gherkin
# features/user_authentication.feature

Feature: User Authentication
  As a system user
  I want to authenticate using my LDAP credentials
  So that I can access protected resources

  Background:
    Given the following users exist in LDAP:
      | username    | email              | roles      |
      | admin.user  | admin@example.com  | ADMIN      |
      | normal.user | user@example.com   | USER       |

  Scenario: Successful login with valid credentials
    Given I am on the login page
    When I enter username "admin.user"
    And I enter password "correct-password"
    And I click the login button
    Then I should receive a valid JWT token
    And the token should contain role "ADMIN"

  Scenario: Failed login with invalid password
    Given I am on the login page
    When I enter username "admin.user"
    And I enter password "wrong-password"
    And I click the login button
    Then I should see an error message "Invalid credentials"
    And I should not receive a JWT token

  Scenario: Account lockout after multiple failed attempts
    Given I am on the login page
    And I have failed to login 4 times
    When I enter username "admin.user"
    And I enter password "wrong-password"
    And I click the login button
    Then my account should be locked for 30 minutes
    And I should see an error message "Account temporarily locked"

  Scenario Outline: Role-based access control
    Given I am logged in as "<user>"
    When I access the "<endpoint>" endpoint
    Then I should receive HTTP status <status>

    Examples:
      | user        | endpoint          | status |
      | admin.user  | /api/admin/users  | 200    |
      | normal.user | /api/admin/users  | 403    |
      | admin.user  | /api/user/profile | 200    |
      | normal.user | /api/user/profile | 200    |
```

### 6.3 Cucumber Step Definitions

```java
// src/test/java/com/example/sso/bdd/steps/AuthenticationSteps.java

@CucumberContextConfiguration
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class AuthenticationSteps {

    @Autowired
    private TestRestTemplate restTemplate;

    private ResponseEntity<?> lastResponse;
    private String currentToken;

    @Given("the following users exist in LDAP:")
    public void createUsersInLdap(DataTable dataTable) {
        List<Map<String, String>> users = dataTable.asMaps();
        users.forEach(this::createLdapUser);
    }

    @When("I enter username {string}")
    public void enterUsername(String username) {
        this.currentUsername = username;
    }

    @When("I enter password {string}")
    public void enterPassword(String password) {
        this.currentPassword = password;
    }

    @When("I click the login button")
    public void clickLogin() {
        LoginRequest request = new LoginRequest(currentUsername, currentPassword);
        lastResponse = restTemplate.postForEntity("/auth/login", request, TokenResponse.class);
    }

    @Then("I should receive a valid JWT token")
    public void shouldReceiveValidToken() {
        assertThat(lastResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        TokenResponse body = (TokenResponse) lastResponse.getBody();
        assertThat(body.getAccessToken()).isNotEmpty();
        currentToken = body.getAccessToken();
    }

    @Then("the token should contain role {string}")
    public void tokenShouldContainRole(String role) {
        Claims claims = Jwts.parser()
            .setSigningKey(publicKey)
            .parseClaimsJws(currentToken)
            .getBody();

        List<String> roles = extractRolesFromClaims(claims);
        assertThat(roles).contains(role);
    }
}
```

---

## 7. 測試標準

### 7.1 測試覆蓋率要求

| 測試類型 | 最低覆蓋率 | 目標覆蓋率 |
|----------|------------|------------|
| Domain Layer | 95% | 100% |
| Application Layer | 85% | 95% |
| Infrastructure Layer | 70% | 85% |
| 整體專案 | 80% | 90% |

### 7.2 測試分類與命名

```java
// 單元測試 - 測試 Domain 和 Application Layer
// 命名規則: should_{預期行為}_when_{條件}
class UserTest {
    @Test
    void should_create_active_user_when_valid_data_provided() { }

    @Test
    void should_throw_exception_when_email_is_invalid() { }
}

// 整合測試 - 測試 Infrastructure Layer
@SpringBootTest
@ActiveProfiles("test")
class UserRepositoryIntegrationTest {
    @Test
    void should_save_and_retrieve_user() { }
}

// API 測試 - 測試 REST Endpoints
@WebMvcTest(UserController.class)
class UserControllerTest {
    @Test
    void should_return_user_profile_when_authenticated() { }

    @Test
    void should_return_403_when_user_lacks_permission() { }
}

// BDD 測試 - 行為規格
@CucumberTest
class AuthenticationBddTest { }
```

### 7.3 測試資料管理

```java
// Test Fixtures - 建立測試物件的工廠
public class UserTestFixtures {

    public static User createDefaultUser() {
        return User.create(
            new Username("test.user"),
            new Email("test@example.com")
        );
    }

    public static User createAdminUser() {
        User user = createDefaultUser();
        user.assignRole(RoleTestFixtures.createAdminRole(), createSystemAssigner());
        return user;
    }

    public static UserBuilder aUser() {
        return new UserBuilder();
    }

    public static class UserBuilder {
        private Username username = new Username("test.user");
        private Email email = new Email("test@example.com");
        private List<Role> roles = new ArrayList<>();

        public UserBuilder withUsername(String username) {
            this.username = new Username(username);
            return this;
        }

        public UserBuilder withRole(Role role) {
            this.roles.add(role);
            return this;
        }

        public User build() {
            User user = User.create(username, email);
            roles.forEach(r -> user.assignRole(r, createSystemAssigner()));
            return user;
        }
    }
}
```

### 7.4 Mock 與 Stub 使用原則

```java
// 在 Application Layer 測試中 Mock Output Ports
class UserApplicationServiceTest {

    @Mock
    private UserRepository userRepository;  // Mock Output Port

    @Mock
    private NotificationSender notificationSender;  // Mock Output Port

    @InjectMocks
    private UserApplicationService userService;

    @Test
    void should_create_user_and_send_notification() {
        // Given
        CreateUserCommand command = new CreateUserCommand("john", "john@example.com");
        when(userRepository.findByUsername(any())).thenReturn(Optional.empty());

        // When
        userService.createUser(command);

        // Then
        verify(userRepository).save(any(User.class));
        verify(notificationSender).send(any(Notification.class));
    }
}

// Domain Layer 不應該需要 Mock - 純邏輯測試
class UserTest {
    @Test
    void should_assign_role() {
        // 不需要 Mock，直接測試領域邏輯
        User user = UserTestFixtures.createDefaultUser();
        Role role = RoleTestFixtures.createUserRole();

        user.assignRole(role, createAssigner());

        assertThat(user.hasRole(role.getId())).isTrue();
    }
}
```

---

## 8. 程式碼品質標準

### 8.1 靜態分析工具

| 工具 | 用途 | 品質門檻 |
|------|------|----------|
| SonarQube | 程式碼品質分析 | Quality Gate: Passed |
| Checkstyle | 程式碼風格檢查 | 0 violations |
| SpotBugs | Bug 偵測 | 0 High/Medium bugs |
| PMD | 程式碼問題偵測 | 0 Critical violations |
| JaCoCo | 測試覆蓋率 | ≥80% |

### 8.2 程式碼風格規範

```java
// 類別結構順序
public class User {
    // 1. 常數
    private static final int MAX_ROLES = 10;

    // 2. 靜態變數
    private static Logger logger = LoggerFactory.getLogger(User.class);

    // 3. 實例變數
    private final UserId id;
    private Username username;

    // 4. 建構子
    private User(UserId id, Username username) {
        this.id = id;
        this.username = username;
    }

    // 5. 靜態工廠方法
    public static User create(Username username, Email email) {
        return new User(UserId.generate(), username);
    }

    // 6. 公開方法
    public void assignRole(Role role) { }

    // 7. 私有方法
    private void validateRole(Role role) { }

    // 8. equals, hashCode, toString
    @Override
    public boolean equals(Object o) { }
}
```

### 8.3 命名規範

| 元素 | 命名規則 | 範例 |
|------|----------|------|
| 類別 | PascalCase | `UserApplicationService` |
| 介面 | PascalCase | `UserRepository` |
| 方法 | camelCase | `findByUsername()` |
| 常數 | SCREAMING_SNAKE_CASE | `MAX_LOGIN_ATTEMPTS` |
| 變數 | camelCase | `currentUser` |
| Package | lowercase | `com.example.sso.domain` |
| Test | should_*_when_* | `should_throw_when_invalid` |

### 8.4 禁止事項

- **禁止** 在 Domain Layer 使用 Spring 註解（@Autowired, @Component 等）
- **禁止** 在 Domain Layer 進行 I/O 操作
- **禁止** 使用 `null` 回傳值，改用 `Optional`
- **禁止** 在建構子中進行複雜邏輯
- **禁止** 使用過度巢狀的 if-else（最多 2 層）
- **禁止** 方法超過 20 行（應重構）
- **禁止** 類別超過 200 行（應拆分）
- **禁止** 捕獲 `Exception`，應捕獲具體例外

---

## 9. Git 提交規範

### 9.1 Commit Message 格式

```
<type>(<scope>): <subject>

<body>

<footer>
```

**Type**:
- `feat`: 新功能
- `fix`: Bug 修復
- `docs`: 文件更新
- `style`: 程式碼格式（不影響功能）
- `refactor`: 重構
- `test`: 測試相關
- `chore`: 建置/工具更新

**範例**:
```
feat(user): add role assignment functionality

- Implement User.assignRole() method
- Add RoleAssignedEvent domain event
- Add validation for duplicate role assignment

Closes #123
```

### 9.2 分支策略

```
main (production)
  │
  ├── develop (integration)
  │     │
  │     ├── feature/user-role-assignment
  │     │
  │     ├── feature/audit-logging
  │     │
  │     └── bugfix/invalid-token-handling
  │
  └── release/1.0.0
```

---

## 10. 審查檢查清單

### 10.1 Code Review Checklist

#### 架構層面
- [ ] 是否遵循六角形架構的依賴方向？
- [ ] Domain Layer 是否無框架依賴？
- [ ] 是否正確使用 Port & Adapter 模式？

#### SOLID 原則
- [ ] 每個類別是否只有單一職責？
- [ ] 是否透過抽象擴展而非修改？
- [ ] 是否符合介面隔離原則？
- [ ] 是否依賴抽象而非具體實作？

#### DDD
- [ ] Aggregate 邊界是否正確？
- [ ] Value Object 是否不可變？
- [ ] 領域邏輯是否在 Domain Layer？

#### 測試
- [ ] 是否有對應的單元測試？
- [ ] 測試覆蓋率是否達標？
- [ ] 測試命名是否清晰？

#### 程式碼品質
- [ ] 是否通過靜態分析檢查？
- [ ] 是否遵循命名規範？
- [ ] 是否有足夠的錯誤處理？

---

## 11. 附錄

### 11.1 相關文件

- [PRD.md](./PRD.md) - 產品需求文件
- [TECH.md](./TECH.md) - 技術架構文件
- [INFRA.md](./INFRA.md) - 基礎設施文件

### 11.2 參考資源

- [Hexagonal Architecture by Alistair Cockburn](https://alistair.cockburn.us/hexagonal-architecture/)
- [Domain-Driven Design by Eric Evans](https://domainlanguage.com/ddd/)
- [Clean Architecture by Robert C. Martin](https://blog.cleancoder.com/uncle-bob/2012/08/13/the-clean-architecture.html)
- [SOLID Principles](https://en.wikipedia.org/wiki/SOLID)
- [Test-Driven Development by Kent Beck](https://www.amazon.com/Test-Driven-Development-Kent-Beck/dp/0321146530)
- [BDD Introduction by Dan North](https://dannorth.net/introducing-bdd/)
