# PRD: 微服務電商平台 PoC

## 文件資訊

| 項目 | 內容 |
|------|------|
| 文件版本 | 3.2 |
| 建立日期 | 2025-01-10 |
| 最後更新 | 2026-01-10 |
| 專案代號 | ECOMMERCE-MSA-POC |
| 架構模式 | DDD + CQRS + Hexagonal Architecture |

---

## 1. 執行摘要

### 1.1 專案目標

建立採用領域驅動設計（DDD）的微服務電商平台 PoC，運用事件風暴進行領域建模，採用 CQRS 模式分離讀寫操作，並以六角形架構確保業務邏輯的獨立性與可測試性。

### 1.2 架構原則

| 原則 | 說明 |
|------|------|
| **Domain-Driven Design** | 以領域模型為核心，業務邏輯集中於領域層 |
| **Event Storming** | 透過事件風暴發現領域事件與聚合邊界 |
| **CQRS** | Command Query Responsibility Segregation，讀寫分離 |
| **Hexagonal Architecture** | 六角形架構，確保領域層獨立於技術實作 |
| **SOLID Principles** | 遵循單一職責、開放封閉、依賴反轉等原則 |

### 1.3 成功指標

| 指標 | 目標值 | 量測方式 |
|------|--------|----------|
| 領域模型覆蓋率 | 100% | 所有業務規則在 Domain 層 |
| 架構合規性 | 100% | ArchUnit 測試通過 |
| 測試覆蓋率 (Domain) | > 90% | 單元測試 |
| API 響應時間 (P95) | < 500ms | Gateway 監控 |

---

## 2. 事件風暴 (Event Storming)

### 2.1 事件風暴流程

```
┌─────────────────────────────────────────────────────────────────────────────────┐
│                           Event Storming Workshop                                │
├─────────────────────────────────────────────────────────────────────────────────┤
│                                                                                  │
│  Phase 1: 領域事件 (Domain Events) - 橘色便利貼                                 │
│  ───────────────────────────────────────────────                                │
│  「系統中發生了什麼重要的事？」                                                  │
│                                                                                  │
│  Phase 2: 命令 (Commands) - 藍色便利貼                                          │
│  ─────────────────────────────────────                                          │
│  「什麼動作觸發了這個事件？」                                                    │
│                                                                                  │
│  Phase 3: 聚合 (Aggregates) - 黃色便利貼                                        │
│  ────────────────────────────────────────                                       │
│  「哪個實體負責處理這個命令？」                                                  │
│                                                                                  │
│  Phase 4: 限界上下文 (Bounded Contexts) - 粉色邊框                              │
│  ─────────────────────────────────────────────────                              │
│  「這些聚合屬於哪個業務領域？」                                                  │
│                                                                                  │
│  Phase 5: 策略與讀取模型 (Policies & Read Models) - 紫色/綠色便利貼             │
│  ──────────────────────────────────────────────────────────────────             │
│  「當事件發生後，系統要自動做什麼？需要什麼樣的查詢視圖？」                       │
│                                                                                  │
└─────────────────────────────────────────────────────────────────────────────────┘
```

### 2.2 事件風暴結果 - User Context

```
┌─────────────────────────────────────────────────────────────────────────────────┐
│                              USER CONTEXT                                        │
│                          《限界上下文：使用者管理》                               │
├─────────────────────────────────────────────────────────────────────────────────┤
│                                                                                  │
│  Timeline ─────────────────────────────────────────────────────────────────────▶│
│                                                                                  │
│  ┌─────────────┐         ┌─────────────┐         ┌─────────────────────────┐   │
│  │   Actor     │         │   Command   │         │      Domain Event       │   │
│  │  (角色)     │         │   (藍色)    │         │        (橘色)           │   │
│  └─────────────┘         └─────────────┘         └─────────────────────────┘   │
│                                                                                  │
│  ┌─────────┐   觸發    ┌───────────────┐         ┌─────────────────────────┐   │
│  │ 訪客    │─────────▶│ RegisterUser  │         │                         │   │
│  └─────────┘           └───────┬───────┘         │                         │   │
│                                │                  │                         │   │
│                                ▼                  │                         │   │
│                    ┌───────────────────┐         │    UserRegistered       │   │
│                    │                   │────────▶│    {                    │   │
│                    │                   │         │      userId,            │   │
│                    │       User        │         │      username,          │   │
│                    │     Aggregate     │         │      role,              │   │
│                    │      (黃色)       │         │      occurredAt         │   │
│                    │                   │         │    }                    │   │
│                    │                   │         │                         │   │
│                    └───────────────────┘         └─────────────────────────┘   │
│                                                                                  │
│  ┌─────────┐   觸發    ┌───────────────┐         ┌─────────────────────────┐   │
│  │ 使用者  │─────────▶│ UpdateProfile │         │   UserProfileUpdated    │   │
│  └─────────┘           └───────┬───────┘         │   {                     │   │
│                                │                  │     userId,             │   │
│                                ▼                  │     changes: {          │   │
│                    ┌───────────────────┐         │       name, phone,      │   │
│                    │       User        │────────▶│       address           │   │
│                    │     Aggregate     │         │     },                  │   │
│                    └───────────────────┘         │     occurredAt          │   │
│                                                   │   }                     │   │
│                                                   └─────────────────────────┘   │
│                                                                                  │
│  ┌─────────┐   觸發    ┌───────────────┐         ┌─────────────────────────┐   │
│  │ 管理者  │─────────▶│  AssignRole   │         │      RoleAssigned       │   │
│  └─────────┘           └───────┬───────┘         │      {                  │   │
│                                │                  │        userId,          │   │
│                                ▼                  │        oldRole,         │   │
│                    ┌───────────────────┐         │        newRole,         │   │
│                    │       User        │────────▶│        assignedBy,      │   │
│                    │     Aggregate     │         │        occurredAt       │   │
│                    └───────────────────┘         │      }                  │   │
│                                                   └─────────────────────────┘   │
│                                                                                  │
│  Read Models (綠色 - Query Side):                                               │
│  ┌────────────────────┐    ┌────────────────────┐                              │
│  │   UserListView     │    │   UserDetailView   │                              │
│  │  • id              │    │  • id              │                              │
│  │  • username        │    │  • username        │                              │
│  │  • name            │    │  • name, phone     │                              │
│  │  • role            │    │  • address, email  │                              │
│  │  • status          │    │  • role, status    │                              │
│  └────────────────────┘    │  • timestamps      │                              │
│                            └────────────────────┘                              │
│                                                                                  │
└─────────────────────────────────────────────────────────────────────────────────┘
```

### 2.3 事件風暴結果 - Product Context

```
┌─────────────────────────────────────────────────────────────────────────────────┐
│                            PRODUCT CONTEXT                                       │
│                          《限界上下文：商品管理》                                 │
├─────────────────────────────────────────────────────────────────────────────────┤
│                                                                                  │
│  Timeline ─────────────────────────────────────────────────────────────────────▶│
│                                                                                  │
│  ═══════════════════════════════════════════════════════════════════════════   │
│  Product Aggregate Flow                                                         │
│  ═══════════════════════════════════════════════════════════════════════════   │
│                                                                                  │
│  ┌─────────┐   觸發    ┌───────────────┐         ┌─────────────────────────┐   │
│  │ 管理者  │─────────▶│CreateProduct  │         │    ProductCreated       │   │
│  └─────────┘           └───────┬───────┘         │    {                    │   │
│                                │                  │      productId,         │   │
│                                ▼                  │      productCode,       │   │
│                    ┌───────────────────┐         │      productName,       │   │
│                    │                   │────────▶│      price,             │   │
│                    │     Product       │         │      createdBy,         │   │
│                    │    Aggregate      │         │      occurredAt         │   │
│                    │                   │         │    }                    │   │
│                    └───────────────────┘         └───────────┬─────────────┘   │
│                                                               │                 │
│                                                               │ Policy (紫色)   │
│                                                               ▼                 │
│                                                   ┌─────────────────────────┐   │
│                                                   │  When ProductCreated:   │   │
│                                                   │  → Create Inventory     │   │
│                                                   │     with initial qty    │   │
│                                                   └─────────────────────────┘   │
│                                                                                  │
│  ┌─────────┐   觸發    ┌───────────────┐         ┌─────────────────────────┐   │
│  │ 管理者  │─────────▶│UpdateProduct  │         │    ProductUpdated       │   │
│  └─────────┘           └───────┬───────┘         │    {                    │   │
│                                │                  │      productId,         │   │
│                                ▼                  │      changes: {...},    │   │
│                    ┌───────────────────┐         │      updatedBy,         │   │
│                    │     Product       │────────▶│      occurredAt         │   │
│                    │    Aggregate      │         │    }                    │   │
│                    └───────────────────┘         └─────────────────────────┘   │
│                                                                                  │
│  ┌─────────┐   觸發    ┌───────────────┐         ┌─────────────────────────┐   │
│  │ 管理者  │─────────▶│DeleteProduct  │         │    ProductDeleted       │   │
│  └─────────┘           └───────┬───────┘         │    {                    │   │
│                                │                  │      productId,         │   │
│                                ▼                  │      deletedBy,         │   │
│                    ┌───────────────────┐         │      occurredAt         │   │
│                    │     Product       │────────▶│    }                    │   │
│                    │    Aggregate      │         └─────────────────────────┘   │
│                    └───────────────────┘                                        │
│                                                                                  │
│  ═══════════════════════════════════════════════════════════════════════════   │
│  Inventory Aggregate Flow (獨立聚合)                                            │
│  ═══════════════════════════════════════════════════════════════════════════   │
│                                                                                  │
│  ┌─────────┐   觸發    ┌───────────────┐         ┌─────────────────────────┐   │
│  │ 管理者  │─────────▶│ AdjustStock   │         │     StockAdjusted       │   │
│  └─────────┘           └───────┬───────┘         │     {                   │   │
│                                │                  │       inventoryId,      │   │
│                                ▼                  │       productId,        │   │
│                    ┌───────────────────┐         │       adjustment,       │   │
│                    │    Inventory      │────────▶│       newQuantity,      │   │
│                    │    Aggregate      │         │       reason,           │   │
│                    │                   │         │       adjustedBy        │   │
│                    │  - productId      │         │     }                   │   │
│                    │  - quantity       │         └───────────┬─────────────┘   │
│                    │  - reorderLevel   │                     │                 │
│                    └───────────────────┘                     │ Policy         │
│                                                               ▼                 │
│                                                   ┌─────────────────────────┐   │
│                                                   │  When qty <= reorder:   │   │
│                                                   │  → Emit LowStockWarning │   │
│                                                   └─────────────────────────┘   │
│                                                                                  │
│  Read Models (綠色 - Query Side):                                               │
│  ┌───────────────────┐  ┌───────────────────┐  ┌───────────────────┐          │
│  │  ProductListView  │  │ ProductDetailView │  │  InventoryView    │          │
│  │  • id             │  │ • id              │  │  • productId      │          │
│  │  • productCode    │  │ • productCode     │  │  • productCode    │          │
│  │  • productName    │  │ • productName     │  │  • quantity       │          │
│  │  • price          │  │ • price           │  │  • available      │          │
│  │  • inStock        │  │ • quantity        │  │  • reorderLevel   │          │
│  └───────────────────┘  │ • description     │  │  • lowStock       │          │
│                         │ • createdBy/At    │  └───────────────────┘          │
│                         │ • updatedBy/At    │                                  │
│                         └───────────────────┘                                  │
│                                                                                  │
└─────────────────────────────────────────────────────────────────────────────────┘
```

### 2.4 完整事件清單

#### User Context 領域事件

| Event | Trigger Command | Aggregate | 業務意義 |
|-------|-----------------|-----------|----------|
| `UserRegistered` | RegisterUser | User | 新使用者完成註冊 |
| `UserProfileUpdated` | UpdateUserProfile | User | 使用者更新個人資料 |
| `RoleAssigned` | AssignRole | User | 管理者變更使用者角色 |
| `UserDisabled` | DisableUser | User | 使用者帳號被停用 |
| `UserEnabled` | EnableUser | User | 使用者帳號被啟用 |

#### Product Context 領域事件

| Event | Trigger Command | Aggregate | 業務意義 |
|-------|-----------------|-----------|----------|
| `ProductCreated` | CreateProduct | Product | 新商品上架 |
| `ProductUpdated` | UpdateProduct | Product | 商品資訊更新 |
| `ProductDeleted` | DeleteProduct | Product | 商品下架（軟刪除） |
| `ProductPriceChanged` | ChangePrice | Product | 商品價格調整 |
| `StockAdjusted` | AdjustStock | Inventory | 庫存數量調整 |
| `LowStockWarning` | (系統觸發) | Inventory | 庫存低於安全水位 |

---

## 3. 限界上下文 (Bounded Contexts)

### 3.1 上下文地圖 (Context Map)

```
┌─────────────────────────────────────────────────────────────────────────────────┐
│                              Context Map                                         │
├─────────────────────────────────────────────────────────────────────────────────┤
│                                                                                  │
│                        ┌───────────────────────────┐                            │
│                        │     Identity Context      │                            │
│                        │       (Keycloak)          │                            │
│                        │                           │                            │
│                        │  • Authentication         │                            │
│                        │  • Authorization          │                            │
│                        │  • Token Management       │                            │
│                        └─────────────┬─────────────┘                            │
│                                      │                                          │
│                              [Conformist]                                       │
│                         (遵循 Keycloak 的模型)                                  │
│                                      │                                          │
│            ┌─────────────────────────┼─────────────────────────┐               │
│            │                         │                         │               │
│            ▼                         ▼                         ▼               │
│  ┌─────────────────────┐   ┌─────────────────────┐   ┌─────────────────────┐  │
│  │                     │   │                     │   │                     │  │
│  │    User Context     │   │  Product Context    │   │   Gateway Context   │  │
│  │   (user-service)    │   │ (product-service)   │   │     (gateway)       │  │
│  │                     │   │                     │   │                     │  │
│  │  ┌───────────────┐  │   │  ┌───────────────┐  │   │  • Routing          │  │
│  │  │ «aggregate»   │  │   │  │ «aggregate»   │  │   │  • Auth Filter      │  │
│  │  │    User       │  │   │  │   Product     │  │   │  • Rate Limiting    │  │
│  │  └───────────────┘  │   │  └───────────────┘  │   │  • Load Balancing   │  │
│  │                     │   │  ┌───────────────┐  │   │                     │  │
│  │  Domain Services:   │   │  │ «aggregate»   │  │   │                     │  │
│  │  • UserDomainSvc    │   │  │  Inventory    │  │   │                     │  │
│  │                     │   │  └───────────────┘  │   │                     │  │
│  │  Application Svc:   │   │                     │   │                     │  │
│  │  • UserCommandSvc   │   │  Domain Services:   │   │                     │  │
│  │  • UserQuerySvc     │   │  • ProductDomainSvc │   │                     │  │
│  │                     │   │  • PricingService   │   │                     │  │
│  └──────────┬──────────┘   │                     │   └─────────────────────┘  │
│             │              │  Application Svc:   │                             │
│             │              │  • ProductCmdSvc    │                             │
│             │              │  • ProductQuerySvc  │                             │
│             │              │  • InventoryCmdSvc  │                             │
│             │              └──────────┬──────────┘                             │
│             │                         │                                        │
│             │   [Published Language]  │                                        │
│             │      (Domain Events)    │                                        │
│             │                         │                                        │
│             └────────────┬────────────┘                                        │
│                          │                                                      │
│                          ▼                                                      │
│            ┌───────────────────────────────────┐                               │
│            │         Audit Context             │                               │
│            │        (Shared Kernel)            │                               │
│            │                                   │                               │
│            │  共用的稽核機制，所有 Context     │                               │
│            │  透過 AOP 自動記錄稽核日誌        │                               │
│            │                                   │                               │
│            │  • AuditLog                       │                               │
│            │  • AuditEvent                     │                               │
│            │  • @Auditable Annotation          │                               │
│            └───────────────────────────────────┘                               │
│                                                                                  │
│  ═══════════════════════════════════════════════════════════════════════════   │
│  Legend:                                                                        │
│  ─────────────────────────────────────────────────────────────────────────────  │
│  [Conformist]        : 下游完全遵循上游模型（不做轉換）                          │
│  [Published Language]: 透過領域事件進行跨 Context 通訊                          │
│  [Shared Kernel]     : 共享的核心模型（需謹慎變更）                             │
│  ═══════════════════════════════════════════════════════════════════════════   │
│                                                                                  │
└─────────────────────────────────────────────────────────────────────────────────┘
```

### 3.2 上下文職責

| Bounded Context | 微服務 | 核心職責 | 聚合 |
|-----------------|--------|----------|------|
| **User Context** | user-service | 使用者生命週期管理 | User |
| **Product Context** | product-service | 商品與庫存管理 | Product, Inventory |
| **Identity Context** | Keycloak | 身份認證與授權 | (外部系統) |
| **Gateway Context** | gateway | API 路由與安全 | - |
| **Audit Context** | audit-lib | 稽核日誌 (Shared Kernel) | AuditLog |

---

## 4. 領域模型 (Domain Model)

### 4.1 User Context 領域模型

```
┌─────────────────────────────────────────────────────────────────────────────────┐
│                         User Context - Domain Model                              │
├─────────────────────────────────────────────────────────────────────────────────┤
│                                                                                  │
│  ┌─────────────────────────────────────────────────────────────────────────┐   │
│  │                    《Aggregate Root》 User                               │   │
│  ├─────────────────────────────────────────────────────────────────────────┤   │
│  │                                                                          │   │
│  │  Identity:                                                               │   │
│  │  ─────────                                                               │   │
│  │  - id: UserId                        // Value Object - UUID Wrapper      │   │
│  │                                                                          │   │
│  │  Properties:                                                             │   │
│  │  ───────────                                                             │   │
│  │  - username: Username                // Value Object - 唯一識別          │   │
│  │  - password: HashedPassword          // Value Object - 加密儲存          │   │
│  │  - profile: UserProfile              // Value Object - 個人資料          │   │
│  │  - role: Role                        // Enumeration - USER/ADMIN         │   │
│  │  - status: UserStatus                // Enumeration - 帳號狀態           │   │
│  │  - auditInfo: AuditInfo              // Value Object - 建立/更新資訊     │   │
│  │                                                                          │   │
│  │  Behaviors (業務方法):                                                   │   │
│  │  ─────────────────────                                                   │   │
│  │  + static register(cmd): User        // Factory Method                   │   │
│  │  + updateProfile(cmd): void          // 更新個人資料                     │   │
│  │  + assignRole(role, by): void        // 指派角色                         │   │
│  │  + disable(reason, by): void         // 停用帳號                         │   │
│  │  + enable(by): void                  // 啟用帳號                         │   │
│  │                                                                          │   │
│  │  Domain Events:                                                          │   │
│  │  ───────────────                                                         │   │
│  │  - domainEvents: List<DomainEvent>   // 待發布的領域事件                 │   │
│  │  + pullDomainEvents(): List          // 取出並清空事件                   │   │
│  │                                                                          │   │
│  └─────────────────────────────────────────────────────────────────────────┘   │
│                                     │                                           │
│                                     │ contains                                  │
│                                     ▼                                           │
│  ┌─────────────────────────────────────────────────────────────────────────┐   │
│  │                   《Value Object》 UserProfile                           │   │
│  ├─────────────────────────────────────────────────────────────────────────┤   │
│  │  - name: PersonName                  // 姓名（2-50字元）                 │   │
│  │  - phone: PhoneNumber                // 電話（可選）                     │   │
│  │  - address: Address                  // 地址（可選，最大200字元）        │   │
│  │  - email: Email                      // Email（可選）                    │   │
│  │                                                                          │   │
│  │  + with*(newValue): UserProfile      // 產生新的 Profile（不可變）       │   │
│  └─────────────────────────────────────────────────────────────────────────┘   │
│                                                                                  │
│  Other Value Objects:                                                           │
│  ┌───────────────────┐ ┌───────────────────┐ ┌───────────────────┐             │
│  │  《VO》 UserId    │ │ 《VO》 Username   │ │《VO》HashedPassword│             │
│  ├───────────────────┤ ├───────────────────┤ ├───────────────────┤             │
│  │ - value: UUID     │ │ - value: String   │ │ - value: String   │             │
│  │                   │ │ 規則: 4-50字元    │ │ 規則: BCrypt      │             │
│  │ + generate()      │ │ 英數底線          │ │ + matches(raw)    │             │
│  │ + of(uuid)        │ │ + validate()      │ │ + of(raw)         │             │
│  └───────────────────┘ └───────────────────┘ └───────────────────┘             │
│                                                                                  │
│  Enumerations:                                                                  │
│  ┌───────────────────────────────┐ ┌───────────────────────────────┐           │
│  │      《Enum》 Role            │ │     《Enum》 UserStatus       │           │
│  ├───────────────────────────────┤ ├───────────────────────────────┤           │
│  │  USER   - 一般使用者          │ │  ACTIVE   - 正常              │           │
│  │  ADMIN  - 管理者              │ │  DISABLED - 停用              │           │
│  │                               │ │  PENDING  - 待驗證            │           │
│  └───────────────────────────────┘ └───────────────────────────────┘           │
│                                                                                  │
│  Domain Events:                                                                 │
│  ┌─────────────────────────────────────────────────────────────────────────┐   │
│  │  UserRegistered      { userId, username, role, occurredAt }             │   │
│  │  UserProfileUpdated  { userId, changes, occurredAt }                    │   │
│  │  RoleAssigned        { userId, oldRole, newRole, assignedBy }           │   │
│  │  UserDisabled        { userId, reason, disabledBy, occurredAt }         │   │
│  │  UserEnabled         { userId, enabledBy, occurredAt }                  │   │
│  └─────────────────────────────────────────────────────────────────────────┘   │
│                                                                                  │
└─────────────────────────────────────────────────────────────────────────────────┘
```

### 4.2 Product Context 領域模型

```
┌─────────────────────────────────────────────────────────────────────────────────┐
│                       Product Context - Domain Model                             │
├─────────────────────────────────────────────────────────────────────────────────┤
│                                                                                  │
│  ╔═════════════════════════════════════════════════════════════════════════╗   │
│  ║                   《Aggregate Root》 Product                             ║   │
│  ╠═════════════════════════════════════════════════════════════════════════╣   │
│  ║                                                                          ║   │
│  ║  Identity:                                                               ║   │
│  ║  - id: ProductId                     // Value Object - UUID              ║   │
│  ║                                                                          ║   │
│  ║  Properties:                                                             ║   │
│  ║  - productCode: ProductCode          // Value Object - P + 6位數字       ║   │
│  ║  - productName: ProductName          // Value Object - 2-100字元         ║   │
│  ║  - price: Money                      // Value Object - 金額              ║   │
│  ║  - description: Description          // Value Object - 可選，≤500字元    ║   │
│  ║  - status: ProductStatus             // Enum - ACTIVE/INACTIVE/DELETED   ║   │
│  ║  - auditInfo: AuditInfo              // Value Object                     ║   │
│  ║                                                                          ║   │
│  ║  Behaviors:                                                              ║   │
│  ║  + static create(cmd): Product       // Factory - 建立商品               ║   │
│  ║  + update(cmd): void                 // 更新商品資訊                     ║   │
│  ║  + changePrice(newPrice, by): void   // 變更價格（獨立追蹤）             ║   │
│  ║  + delete(by): void                  // 軟刪除                           ║   │
│  ║  + activate(): void                  // 上架                             ║   │
│  ║  + deactivate(): void                // 下架                             ║   │
│  ║                                                                          ║   │
│  ║  Invariants (不變量):                                                    ║   │
│  ║  • INV-P1: productCode 格式必須為 P + 6位數字                            ║   │
│  ║  • INV-P2: price 必須大於 0                                              ║   │
│  ║  • INV-P3: 已刪除的商品不能被更新                                        ║   │
│  ║  • INV-P4: productCode 建立後不可變更                                    ║   │
│  ║                                                                          ║   │
│  ╚═════════════════════════════════════════════════════════════════════════╝   │
│                                                                                  │
│  ╔═════════════════════════════════════════════════════════════════════════╗   │
│  ║                  《Aggregate Root》 Inventory                            ║   │
│  ║                     (獨立聚合，與 Product 分離)                          ║   │
│  ╠═════════════════════════════════════════════════════════════════════════╣   │
│  ║                                                                          ║   │
│  ║  Identity:                                                               ║   │
│  ║  - id: InventoryId                   // Value Object - UUID              ║   │
│  ║                                                                          ║   │
│  ║  Properties:                                                             ║   │
│  ║  - productId: ProductId              // Reference - 關聯商品             ║   │
│  ║  - quantity: Quantity                // Value Object - 庫存數量          ║   │
│  ║  - reservedQuantity: Quantity        // Value Object - 保留數量          ║   │
│  ║  - reorderLevel: Quantity            // Value Object - 安全庫存          ║   │
│  ║                                                                          ║   │
│  ║  Behaviors:                                                              ║   │
│  ║  + static create(productId, qty): Inventory                              ║   │
│  ║  + adjustStock(adjustment, reason, by): void                             ║   │
│  ║  + reserve(qty): void                // 預留庫存                         ║   │
│  ║  + release(qty): void                // 釋放預留                         ║   │
│  ║  + getAvailableQuantity(): Quantity  // 可用 = 總量 - 預留               ║   │
│  ║                                                                          ║   │
│  ║  Invariants:                                                             ║   │
│  ║  • INV-I1: quantity >= 0                                                 ║   │
│  ║  • INV-I2: reservedQuantity <= quantity                                  ║   │
│  ║  • INV-I3: 當 quantity <= reorderLevel 時發出 LowStockWarning            ║   │
│  ║                                                                          ║   │
│  ╚═════════════════════════════════════════════════════════════════════════╝   │
│                                                                                  │
│  Value Objects:                                                                 │
│  ┌───────────────────┐ ┌───────────────────┐ ┌───────────────────┐             │
│  │ 《VO》 Money      │ │《VO》 ProductCode │ │ 《VO》 Quantity   │             │
│  ├───────────────────┤ ├───────────────────┤ ├───────────────────┤             │
│  │ - amount: BigDec  │ │ - value: String   │ │ - value: int      │             │
│  │ - currency: TWD   │ │ Pattern: P\d{6}   │ │ 規則: >= 0        │             │
│  │                   │ │                   │ │                   │             │
│  │ + add(money)      │ │ + generate()      │ │ + add(qty)        │             │
│  │ + subtract(money) │ │ + of(code)        │ │ + subtract(qty)   │             │
│  │ + multiply(n)     │ │ + validate()      │ │ + isZero()        │             │
│  │ + isGreaterThan() │ │                   │ │ + isLessThan()    │             │
│  └───────────────────┘ └───────────────────┘ └───────────────────┘             │
│                                                                                  │
│  Domain Events:                                                                 │
│  ┌─────────────────────────────────────────────────────────────────────────┐   │
│  │  ProductCreated     { productId, productCode, name, price, createdBy }  │   │
│  │  ProductUpdated     { productId, changes, updatedBy, occurredAt }       │   │
│  │  ProductPriceChanged{ productId, oldPrice, newPrice, changedBy }        │   │
│  │  ProductDeleted     { productId, deletedBy, occurredAt }                │   │
│  │  StockAdjusted      { inventoryId, productId, adjustment, newQty }      │   │
│  │  LowStockWarning    { inventoryId, productId, currentQty, reorderLevel }│   │
│  └─────────────────────────────────────────────────────────────────────────┘   │
│                                                                                  │
└─────────────────────────────────────────────────────────────────────────────────┘
```

### 4.3 聚合設計原則

```
┌─────────────────────────────────────────────────────────────────────────────────┐
│                        Aggregate Design Principles                               │
├─────────────────────────────────────────────────────────────────────────────────┤
│                                                                                  │
│  1. 聚合邊界設計                                                                │
│  ───────────────                                                                │
│                                                                                  │
│  ┌─────────────────────────────────────────────────────────────────────────┐   │
│  │  為什麼 Product 和 Inventory 是分開的聚合？                              │   │
│  │                                                                          │   │
│  │  • 不同的變更頻率：庫存頻繁變動，商品資訊較少變更                        │   │
│  │  • 不同的一致性需求：庫存需要即時一致，商品可以最終一致                  │   │
│  │  • 獨立的業務規則：庫存有自己的不變量（如安全庫存）                      │   │
│  │  • 更好的並發性：分開後可以獨立鎖定，減少衝突                            │   │
│  │                                                                          │   │
│  │  關聯方式：Inventory 透過 ProductId (Value Object) 引用 Product          │   │
│  └─────────────────────────────────────────────────────────────────────────┘   │
│                                                                                  │
│  2. 不變量保護                                                                  │
│  ─────────────                                                                  │
│                                                                                  │
│  ┌─────────────────────────────────────────────────────────────────────────┐   │
│  │  聚合負責在任何操作前後維護不變量：                                      │   │
│  │                                                                          │   │
│  │  // Product Aggregate 範例                                               │   │
│  │  public void changePrice(Money newPrice, String changedBy) {             │   │
│  │      // 檢查不變量                                                       │   │
│  │      validateNotDeleted();                // INV-P3                      │   │
│  │      newPrice.validatePositive();         // INV-P2                      │   │
│  │                                                                          │   │
│  │      // 狀態變更                                                         │   │
│  │      Money oldPrice = this.price;                                        │   │
│  │      this.price = newPrice;                                              │   │
│  │      this.auditInfo = auditInfo.update(changedBy);                       │   │
│  │                                                                          │   │
│  │      // 發布領域事件                                                     │   │
│  │      registerEvent(new ProductPriceChanged(                              │   │
│  │          this.id, oldPrice, newPrice, changedBy                          │   │
│  │      ));                                                                  │   │
│  │  }                                                                       │   │
│  └─────────────────────────────────────────────────────────────────────────┘   │
│                                                                                  │
│  3. 領域事件發布                                                                │
│  ─────────────────                                                              │
│                                                                                  │
│  ┌─────────────────────────────────────────────────────────────────────────┐   │
│  │  事件在聚合內註冊，由 Application Service 負責發布：                     │   │
│  │                                                                          │   │
│  │  聚合內：                                                                │   │
│  │  protected void registerEvent(DomainEvent event) {                       │   │
│  │      this.domainEvents.add(event);                                       │   │
│  │  }                                                                       │   │
│  │                                                                          │   │
│  │  Application Service：                                                   │   │
│  │  public void handle(CreateProductCommand cmd) {                          │   │
│  │      Product product = Product.create(cmd);                              │   │
│  │      repository.save(product);                                           │   │
│  │      eventPublisher.publishAll(product.pullDomainEvents());              │   │
│  │  }                                                                       │   │
│  └─────────────────────────────────────────────────────────────────────────┘   │
│                                                                                  │
└─────────────────────────────────────────────────────────────────────────────────┘
```

---

## 5. CQRS 模型設計

### 5.1 CQRS 架構概覽

```
┌─────────────────────────────────────────────────────────────────────────────────┐
│                              CQRS Architecture                                   │
├─────────────────────────────────────────────────────────────────────────────────┤
│                                                                                  │
│                              ┌──────────────┐                                   │
│                              │    Client    │                                   │
│                              └──────┬───────┘                                   │
│                                     │                                           │
│                    ┌────────────────┴────────────────┐                          │
│                    │                                 │                          │
│            Commands (寫入)                    Queries (讀取)                    │
│                    │                                 │                          │
│                    ▼                                 ▼                          │
│  ╔═════════════════════════════════╗ ╔═════════════════════════════════╗       │
│  ║      Command Side               ║ ║        Query Side               ║       │
│  ║      (Write Model)              ║ ║        (Read Model)             ║       │
│  ╠═════════════════════════════════╣ ╠═════════════════════════════════╣       │
│  ║                                 ║ ║                                 ║       │
│  ║  ┌───────────────────────────┐  ║ ║  ┌───────────────────────────┐  ║       │
│  ║  │   Command Handler         │  ║ ║  │    Query Handler          │  ║       │
│  ║  │   (Application Service)   │  ║ ║  │    (Query Service)        │  ║       │
│  ║  │                           │  ║ ║  │                           │  ║       │
│  ║  │ • ProductCommandService   │  ║ ║  │ • ProductQueryService     │  ║       │
│  ║  │ • InventoryCommandService │  ║ ║  │ • InventoryQueryService   │  ║       │
│  ║  └─────────────┬─────────────┘  ║ ║  └─────────────┬─────────────┘  ║       │
│  ║                │                ║ ║                │                ║       │
│  ║                ▼                ║ ║                ▼                ║       │
│  ║  ┌───────────────────────────┐  ║ ║  ┌───────────────────────────┐  ║       │
│  ║  │   Domain Layer            │  ║ ║  │   Read Model (DTO/View)   │  ║       │
│  ║  │                           │  ║ ║  │                           │  ║       │
│  ║  │ • Aggregates              │  ║ ║  │ • ProductListView         │  ║       │
│  ║  │ • Domain Services         │  ║ ║  │ • ProductDetailView       │  ║       │
│  ║  │ • Domain Events           │  ║ ║  │ • InventoryView           │  ║       │
│  ║  │ • Business Rules          │  ║ ║  │                           │  ║       │
│  ║  └─────────────┬─────────────┘  ║ ║  └─────────────┬─────────────┘  ║       │
│  ║                │                ║ ║                │                ║       │
│  ║                ▼                ║ ║                ▼                ║       │
│  ║  ┌───────────────────────────┐  ║ ║  ┌───────────────────────────┐  ║       │
│  ║  │   Write Repository        │  ║ ║  │   Read Repository         │  ║       │
│  ║  │   (Aggregate Store)       │  ║ ║  │   (Optimized for Query)   │  ║       │
│  ║  │                           │  ║ ║  │                           │  ║       │
│  ║  │ • ProductRepository       │  ║ ║  │ • ProductReadRepository   │  ║       │
│  ║  │ • InventoryRepository     │  ║ ║  │ • Custom Query Methods    │  ║       │
│  ║  └─────────────┬─────────────┘  ║ ║  └─────────────┬─────────────┘  ║       │
│  ║                │                ║ ║                ▲                ║       │
│  ╚════════════════╪════════════════╝ ╚════════════════╪════════════════╝       │
│                   │                                   │                         │
│                   │        ┌─────────────────┐        │                         │
│                   │        │  Event Handler  │        │                         │
│                   └───────▶│                 │────────┘                         │
│                            │ Sync Read Model │                                  │
│                   Domain   │ from Events     │   Update                         │
│                   Events   └─────────────────┘   Read Model                     │
│                                                                                  │
│  ═══════════════════════════════════════════════════════════════════════════   │
│                                                                                  │
│                   ┌───────────────────────────────────────┐                     │
│                   │         Write Database                │                     │
│                   │      (Aggregate State)                │                     │
│                   │                                       │                     │
│                   │  products, inventories tables         │                     │
│                   │  (Normalized, Consistency-focused)    │                     │
│                   └───────────────────────────────────────┘                     │
│                                    │                                            │
│                            Domain Events                                        │
│                            (Async / Sync)                                       │
│                                    ▼                                            │
│                   ┌───────────────────────────────────────┐                     │
│                   │          Read Database                │                     │
│                   │       (Denormalized Views)            │                     │
│                   │                                       │                     │
│                   │  product_list_view, product_detail_   │                     │
│                   │  view, inventory_view tables          │                     │
│                   │  (Optimized for specific queries)     │                     │
│                   └───────────────────────────────────────┘                     │
│                                                                                  │
│  Note: 在 PoC 階段，Write/Read 可共用同一資料庫，                               │
│        透過不同的 Repository 介面區分職責。                                     │
│                                                                                  │
└─────────────────────────────────────────────────────────────────────────────────┘
```

### 5.2 Commands 定義

```
┌─────────────────────────────────────────────────────────────────────────────────┐
│                           Commands (Write Operations)                            │
├─────────────────────────────────────────────────────────────────────────────────┤
│                                                                                  │
│  User Context Commands:                                                         │
│  ══════════════════════                                                         │
│                                                                                  │
│  ┌─────────────────────────────────────────────────────────────────────────┐   │
│  │  UpdateUserProfileCommand                                                │   │
│  │  ────────────────────────                                                │   │
│  │  用途: 使用者更新自己的個人資料                                          │   │
│  │  觸發: PUT /api/users/me                                                 │   │
│  │  權限: USER, ADMIN                                                       │   │
│  │                                                                          │   │
│  │  {                                                                       │   │
│  │      userId: UUID           // 從 JWT Token 取得                         │   │
│  │      name: String?          // 選填，null 表示不更新                     │   │
│  │      phone: String?         // 選填                                      │   │
│  │      address: String?       // 選填                                      │   │
│  │  }                                                                       │   │
│  │                                                                          │   │
│  │  → 產生事件: UserProfileUpdated                                          │   │
│  └─────────────────────────────────────────────────────────────────────────┘   │
│                                                                                  │
│  ┌─────────────────────────────────────────────────────────────────────────┐   │
│  │  AssignRoleCommand                                                       │   │
│  │  ─────────────────                                                       │   │
│  │  用途: 管理者變更使用者角色                                              │   │
│  │  觸發: POST /api/users/{id}/role                                         │   │
│  │  權限: ADMIN only                                                        │   │
│  │                                                                          │   │
│  │  {                                                                       │   │
│  │      targetUserId: UUID     // 被變更角色的使用者                        │   │
│  │      newRole: Role          // USER | ADMIN                              │   │
│  │      assignedBy: String     // 執行者（從 JWT）                          │   │
│  │  }                                                                       │   │
│  │                                                                          │   │
│  │  → 產生事件: RoleAssigned                                                │   │
│  └─────────────────────────────────────────────────────────────────────────┘   │
│                                                                                  │
│  Product Context Commands:                                                      │
│  ═════════════════════════                                                      │
│                                                                                  │
│  ┌─────────────────────────────────────────────────────────────────────────┐   │
│  │  CreateProductCommand                                                    │   │
│  │  ────────────────────                                                    │   │
│  │  用途: 建立新商品                                                        │   │
│  │  觸發: POST /api/products                                                │   │
│  │  權限: ADMIN only                                                        │   │
│  │                                                                          │   │
│  │  {                                                                       │   │
│  │      productCode: String?   // 選填，null 則自動產生                     │   │
│  │      productName: String    // 必填，2-100 字元                          │   │
│  │      price: BigDecimal      // 必填，> 0                                 │   │
│  │      quantity: Integer      // 必填，>= 0，初始庫存                      │   │
│  │      description: String?   // 選填，<= 500 字元                         │   │
│  │      createdBy: String      // 從 JWT 取得                               │   │
│  │  }                                                                       │   │
│  │                                                                          │   │
│  │  → 產生事件: ProductCreated                                              │   │
│  │  → Policy: 自動建立對應的 Inventory                                      │   │
│  └─────────────────────────────────────────────────────────────────────────┘   │
│                                                                                  │
│  ┌─────────────────────────────────────────────────────────────────────────┐   │
│  │  UpdateProductCommand                                                    │   │
│  │  ────────────────────                                                    │   │
│  │  用途: 更新商品資訊（不含價格）                                          │   │
│  │  觸發: PUT /api/products/{id}                                            │   │
│  │  權限: ADMIN only                                                        │   │
│  │                                                                          │   │
│  │  {                                                                       │   │
│  │      productId: UUID        // 從 Path 取得                              │   │
│  │      productName: String?   // 選填                                      │   │
│  │      price: BigDecimal?     // 選填（如提供，會觸發 PriceChanged）       │   │
│  │      description: String?   // 選填                                      │   │
│  │      updatedBy: String      // 從 JWT 取得                               │   │
│  │  }                                                                       │   │
│  │                                                                          │   │
│  │  → 產生事件: ProductUpdated                                              │   │
│  │  → 若價格變更: ProductPriceChanged                                       │   │
│  └─────────────────────────────────────────────────────────────────────────┘   │
│                                                                                  │
│  ┌─────────────────────────────────────────────────────────────────────────┐   │
│  │  DeleteProductCommand                                                    │   │
│  │  ────────────────────                                                    │   │
│  │  用途: 刪除商品（軟刪除）                                                │   │
│  │  觸發: DELETE /api/products/{id}                                         │   │
│  │  權限: ADMIN only                                                        │   │
│  │                                                                          │   │
│  │  {                                                                       │   │
│  │      productId: UUID        // 從 Path 取得                              │   │
│  │      deletedBy: String      // 從 JWT 取得                               │   │
│  │  }                                                                       │   │
│  │                                                                          │   │
│  │  → 產生事件: ProductDeleted                                              │   │
│  └─────────────────────────────────────────────────────────────────────────┘   │
│                                                                                  │
│  ┌─────────────────────────────────────────────────────────────────────────┐   │
│  │  AdjustStockCommand                                                      │   │
│  │  ──────────────────                                                      │   │
│  │  用途: 調整商品庫存                                                      │   │
│  │  觸發: POST /api/products/{id}/stock                                     │   │
│  │  權限: ADMIN only                                                        │   │
│  │                                                                          │   │
│  │  {                                                                       │   │
│  │      productId: UUID        // 從 Path 取得                              │   │
│  │      adjustment: Integer    // 正數=增加，負數=減少                      │   │
│  │      reason: String         // 調整原因                                  │   │
│  │      adjustedBy: String     // 從 JWT 取得                               │   │
│  │  }                                                                       │   │
│  │                                                                          │   │
│  │  → 產生事件: StockAdjusted                                               │   │
│  │  → 若庫存 <= 安全水位: LowStockWarning                                   │   │
│  └─────────────────────────────────────────────────────────────────────────┘   │
│                                                                                  │
└─────────────────────────────────────────────────────────────────────────────────┘
```

### 5.3 Queries 定義

```
┌─────────────────────────────────────────────────────────────────────────────────┐
│                            Queries (Read Operations)                             │
├─────────────────────────────────────────────────────────────────────────────────┤
│                                                                                  │
│  User Context Queries:                                                          │
│  ═════════════════════                                                          │
│                                                                                  │
│  ┌─────────────────────────────────────────────────────────────────────────┐   │
│  │  GetCurrentUserQuery                                                     │   │
│  │  ───────────────────                                                     │   │
│  │  GET /api/users/me                                                       │   │
│  │  權限: USER, ADMIN                                                       │   │
│  │                                                                          │   │
│  │  Input: (從 JWT Token 自動取得 userId)                                   │   │
│  │  Output: UserDetailView                                                  │   │
│  └─────────────────────────────────────────────────────────────────────────┘   │
│                                                                                  │
│  ┌─────────────────────────────────────────────────────────────────────────┐   │
│  │  ListUsersQuery                                                          │   │
│  │  ──────────────                                                          │   │
│  │  GET /api/users?page=0&size=20&role=USER                                 │   │
│  │  權限: ADMIN only                                                        │   │
│  │                                                                          │   │
│  │  Input: { page: Int, size: Int, role: Role? }                            │   │
│  │  Output: PagedResult<UserListView>                                       │   │
│  └─────────────────────────────────────────────────────────────────────────┘   │
│                                                                                  │
│  ┌─────────────────────────────────────────────────────────────────────────┐   │
│  │  GetUserByIdQuery                                                        │   │
│  │  ────────────────                                                        │   │
│  │  GET /api/users/{id}                                                     │   │
│  │  權限: ADMIN only                                                        │   │
│  │                                                                          │   │
│  │  Input: { userId: UUID }                                                 │   │
│  │  Output: UserDetailView                                                  │   │
│  └─────────────────────────────────────────────────────────────────────────┘   │
│                                                                                  │
│  Product Context Queries:                                                       │
│  ════════════════════════                                                       │
│                                                                                  │
│  ┌─────────────────────────────────────────────────────────────────────────┐   │
│  │  ListProductsQuery                                                       │   │
│  │  ─────────────────                                                       │   │
│  │  GET /api/products?page=0&size=20&sort=price,asc                         │   │
│  │  權限: USER, ADMIN                                                       │   │
│  │                                                                          │   │
│  │  Input: {                                                                │   │
│  │      page: Int = 0                                                       │   │
│  │      size: Int = 20                                                      │   │
│  │      sortBy: String? = "createdAt"                                       │   │
│  │      sortDirection: SortDirection? = DESC                                │   │
│  │  }                                                                       │   │
│  │  Output: PagedResult<ProductListView>                                    │   │
│  └─────────────────────────────────────────────────────────────────────────┘   │
│                                                                                  │
│  ┌─────────────────────────────────────────────────────────────────────────┐   │
│  │  GetProductByIdQuery                                                     │   │
│  │  ───────────────────                                                     │   │
│  │  GET /api/products/{id}                                                  │   │
│  │  權限: USER, ADMIN                                                       │   │
│  │                                                                          │   │
│  │  Input: { productId: UUID }                                              │   │
│  │  Output: ProductDetailView (包含庫存資訊)                                │   │
│  └─────────────────────────────────────────────────────────────────────────┘   │
│                                                                                  │
│  ┌─────────────────────────────────────────────────────────────────────────┐   │
│  │  SearchProductsQuery                                                     │   │
│  │  ───────────────────                                                     │   │
│  │  GET /api/products/search?keyword=phone&minPrice=100&inStock=true        │   │
│  │  權限: USER, ADMIN                                                       │   │
│  │                                                                          │   │
│  │  Input: {                                                                │   │
│  │      keyword: String?       // 商品名稱模糊搜尋                          │   │
│  │      minPrice: BigDecimal?  // 最低價格                                  │   │
│  │      maxPrice: BigDecimal?  // 最高價格                                  │   │
│  │      inStock: Boolean?      // 是否有庫存                                │   │
│  │  }                                                                       │   │
│  │  Output: List<ProductListView>                                           │   │
│  └─────────────────────────────────────────────────────────────────────────┘   │
│                                                                                  │
│  ┌─────────────────────────────────────────────────────────────────────────┐   │
│  │  GetInventoryQuery                                                       │   │
│  │  ─────────────────                                                       │   │
│  │  GET /api/products/{id}/inventory                                        │   │
│  │  權限: ADMIN only                                                        │   │
│  │                                                                          │   │
│  │  Input: { productId: UUID }                                              │   │
│  │  Output: InventoryView                                                   │   │
│  └─────────────────────────────────────────────────────────────────────────┘   │
│                                                                                  │
└─────────────────────────────────────────────────────────────────────────────────┘
```

### 5.4 Read Models (Views)

```
┌─────────────────────────────────────────────────────────────────────────────────┐
│                              Read Models (Views)                                 │
├─────────────────────────────────────────────────────────────────────────────────┤
│                                                                                  │
│  User Read Models:                                                              │
│  ═════════════════                                                              │
│                                                                                  │
│  ┌─────────────────────────────┐    ┌─────────────────────────────┐            │
│  │      UserListView           │    │      UserDetailView         │            │
│  │      (列表用，精簡)         │    │      (詳情用，完整)         │            │
│  ├─────────────────────────────┤    ├─────────────────────────────┤            │
│  │  id: UUID                   │    │  id: UUID                   │            │
│  │  username: String           │    │  username: String           │            │
│  │  name: String               │    │  name: String               │            │
│  │  role: String               │    │  email: String              │            │
│  │  status: String             │    │  phone: String              │            │
│  │                             │    │  address: String            │            │
│  │  // 用於列表顯示            │    │  role: String               │            │
│  │  // 不包含敏感資訊          │    │  status: String             │            │
│  │                             │    │  createdAt: LocalDateTime   │            │
│  │                             │    │  updatedAt: LocalDateTime   │            │
│  │                             │    │                             │            │
│  │                             │    │  // 不包含 password         │            │
│  └─────────────────────────────┘    └─────────────────────────────┘            │
│                                                                                  │
│  Product Read Models:                                                           │
│  ════════════════════                                                           │
│                                                                                  │
│  ┌─────────────────────────────┐    ┌─────────────────────────────┐            │
│  │     ProductListView         │    │    ProductDetailView        │            │
│  │     (列表用，精簡)          │    │    (詳情用，完整)           │            │
│  ├─────────────────────────────┤    ├─────────────────────────────┤            │
│  │  id: UUID                   │    │  id: UUID                   │            │
│  │  productCode: String        │    │  productCode: String        │            │
│  │  productName: String        │    │  productName: String        │            │
│  │  price: BigDecimal          │    │  price: BigDecimal          │            │
│  │  quantity: Integer          │    │  quantity: Integer          │            │
│  │  inStock: Boolean           │    │  availableQuantity: Integer │            │
│  │                             │    │  description: String        │            │
│  │  // 前端列表顯示用          │    │  status: String             │            │
│  │  // inStock = qty > 0       │    │  createdBy: String          │            │
│  │                             │    │  createdAt: LocalDateTime   │            │
│  │                             │    │  updatedBy: String          │            │
│  │                             │    │  updatedAt: LocalDateTime   │            │
│  │                             │    │                             │            │
│  │                             │    │  // 包含庫存詳情            │            │
│  └─────────────────────────────┘    └─────────────────────────────┘            │
│                                                                                  │
│  ┌─────────────────────────────────────────────────────────────────────────┐   │
│  │                         InventoryView                                    │   │
│  │                         (庫存管理用)                                     │   │
│  ├─────────────────────────────────────────────────────────────────────────┤   │
│  │  productId: UUID                     // 關聯商品                         │   │
│  │  productCode: String                 // 冗餘，方便顯示                   │   │
│  │  productName: String                 // 冗餘，方便顯示                   │   │
│  │  quantity: Integer                   // 總庫存                           │   │
│  │  reservedQuantity: Integer           // 預留數量                         │   │
│  │  availableQuantity: Integer          // 可用 = 總量 - 預留               │   │
│  │  reorderLevel: Integer               // 安全庫存水位                     │   │
│  │  lowStock: Boolean                   // quantity <= reorderLevel         │   │
│  │  lastAdjustedAt: LocalDateTime       // 最後調整時間                     │   │
│  │  lastAdjustedBy: String              // 最後調整人                       │   │
│  └─────────────────────────────────────────────────────────────────────────┘   │
│                                                                                  │
│  Read Model 同步策略 (PoC 階段):                                                │
│  ═══════════════════════════════                                                │
│                                                                                  │
│  ┌─────────────────────────────────────────────────────────────────────────┐   │
│  │  Option 1: Synchronous (本 PoC 採用)                                     │   │
│  │  ───────────────────────────────────                                     │   │
│  │  • Write 和 Read 共用同一資料庫                                          │   │
│  │  • Query Service 直接查詢 Entity，轉換為 View                            │   │
│  │  • 優點：簡單、一致性高                                                  │   │
│  │  • 缺點：無法獨立擴展讀寫                                                │   │
│  │                                                                          │   │
│  │  Option 2: Event-Driven (未來擴展)                                       │   │
│  │  ────────────────────────────────                                        │   │
│  │  • Write 觸發 Domain Event                                               │   │
│  │  • Event Handler 非同步更新 Read Model                                   │   │
│  │  • Read Model 存在獨立的資料庫/快取                                      │   │
│  │  • 優點：可獨立擴展、查詢最佳化                                          │   │
│  │  • 缺點：最終一致性、複雜度高                                            │   │
│  └─────────────────────────────────────────────────────────────────────────┘   │
│                                                                                  │
└─────────────────────────────────────────────────────────────────────────────────┘
```

---

## 6. 權限控制 (RBAC)

### 6.1 角色定義

| 角色 | 代碼 | 說明 |
|------|------|------|
| 一般使用者 | `ROLE_USER` | 可查詢商品資訊，管理個人資料 |
| 管理者 | `ROLE_ADMIN` | 可管理商品（CRUD），管理使用者 |

### 6.2 Command / Query 權限矩陣

| Operation | Type | ROLE_USER | ROLE_ADMIN |
|-----------|------|:---------:|:----------:|
| **User Context** | | | |
| GetCurrentUserQuery | Query | ✓ | ✓ |
| UpdateUserProfileCommand | Command | ✓ (self) | ✓ (self) |
| ListUsersQuery | Query | ✗ | ✓ |
| GetUserByIdQuery | Query | ✗ | ✓ |
| AssignRoleCommand | Command | ✗ | ✓ |
| **Product Context** | | | |
| ListProductsQuery | Query | ✓ | ✓ |
| GetProductByIdQuery | Query | ✓ | ✓ |
| SearchProductsQuery | Query | ✓ | ✓ |
| GetInventoryQuery | Query | ✗ | ✓ |
| CreateProductCommand | Command | ✗ | ✓ |
| UpdateProductCommand | Command | ✗ | ✓ |
| DeleteProductCommand | Command | ✗ | ✓ |
| AdjustStockCommand | Command | ✗ | ✓ |

---

## 7. 稽核需求

### 7.1 需稽核的 Commands

| Command | 稽核事件 | 稽核內容 |
|---------|----------|----------|
| CreateProductCommand | PRODUCT_CREATED | productId, productCode, createdBy |
| UpdateProductCommand | PRODUCT_UPDATED | productId, changes, updatedBy |
| DeleteProductCommand | PRODUCT_DELETED | productId, deletedBy |
| AdjustStockCommand | STOCK_ADJUSTED | productId, adjustment, reason, adjustedBy |
| UpdateUserProfileCommand | USER_PROFILE_UPDATED | userId, changes |
| AssignRoleCommand | ROLE_ASSIGNED | userId, oldRole, newRole, assignedBy |

### 7.2 稽核日誌格式

```json
{
  "id": "uuid",
  "timestamp": "2025-01-10T12:00:00Z",
  "eventType": "PRODUCT_CREATED",
  "aggregateType": "Product",
  "aggregateId": "product-uuid",
  "username": "admin.user",
  "serviceName": "product-service",
  "action": "CreateProductCommand",
  "payload": {
    "productCode": "P000001",
    "productName": "iPhone 16",
    "price": 35900
  },
  "result": "SUCCESS",
  "clientIp": "192.168.1.100"
}
```

### 7.3 稽核資料完整性

| 需求 | 規範 |
|------|------|
| **儲存模式** | Append-Only（僅允許新增，禁止更新與刪除） |
| **資料不可變** | 已寫入的稽核日誌不可修改或刪除，確保稽核軌跡完整性 |
| **合規性** | 符合一般稽核合規要求，提供完整的操作證據鏈 |

### 7.4 稽核資料格式規範

| 項目 | 規範 |
|------|------|
| **Payload 大小上限** | 64 KB（超過時自動截斷，並標註截斷標記） |
| **截斷標記** | `"_truncated": true, "_originalSize": <bytes>` |
| **時間格式** | ISO 8601（UTC 時區，例：`2025-01-10T12:00:00Z`） |
| **ID 格式** | UUID v4 |

### 7.5 敏感資料遮蔽策略

採用**欄位層級遮蔽（Field-Level Masking）**，保留格式但隱藏實際值：

| 欄位類型 | 遮蔽範例 |
|----------|----------|
| 密碼 | `"password": "****"` |
| 信用卡號 | `"creditCard": "****-****-****-1234"` |
| 身分證字號 | `"idNumber": "A1234*****"` |
| 電話號碼 | `"phone": "0912-***-***"` |
| Email | `"email": "u***@example.com"` |

**設定方式**：透過 `@Auditable` 註解的 `maskFields` 屬性指定需遮蔽的欄位名稱。

### 7.6 效能需求

| 指標 | 目標值 |
|------|--------|
| **寫入吞吐量** | 100-500 events/second（所有微服務合計） |
| **擷取延遲** | < 50ms（正常負載下，稽核儲存可用時） |
| **查詢效能** | 5 秒內回應（百萬筆記錄範圍查詢） |
| **業務影響** | 稽核失敗不得阻擋業務操作（故障隔離） |

### 7.7 關聯追蹤 (Correlation ID)

稽核日誌支援透過 Correlation ID 關聯同一交易中的多個操作：

| 項目 | 規範 |
|------|------|
| **來源** | 從 MDC (Mapped Diagnostic Context) 自動擷取 |
| **欄位名稱** | `correlationId` |
| **格式** | UUID 或自訂 trace ID |
| **用途** | 關聯同一請求/交易中產生的多筆稽核記錄 |
| **預設值** | 若 MDC 中無 correlationId，則為 `null` |

**實作機制**：`AuditContextHolder` 會從 MDC 中擷取 `correlationId`（或 `traceId`）欄位，自動寫入稽核日誌。

### 7.8 動態設定重載

稽核設定支援動態重載，無需重啟服務：

| 項目 | 規範 |
|------|------|
| **機制** | `@ConfigurationProperties` + `@RefreshScope` |
| **觸發方式** | Spring Cloud Config 更新 或 `/actuator/refresh` 端點 |
| **可動態調整項目** | 遮蔽欄位清單、啟用/停用稽核、Payload 大小上限 |
| **不可動態調整** | 稽核儲存目標、資料庫連線 |

### 7.9 可觀測性需求

稽核函式庫須提供以下監控指標：

| 指標名稱 | 說明 |
|----------|------|
| `audit.events.total` | 稽核事件總數（Counter） |
| `audit.events.failed` | 稽核失敗次數（Counter） |
| `audit.capture.latency` | 擷取延遲（Histogram，毫秒） |
| `audit.queue.depth` | 處理佇列深度（Gauge） |
| `audit.storage.available` | 儲存可用狀態（Gauge，0/1） |

**健康檢查端點**：整合 Spring Boot Actuator，提供 `/actuator/health/audit` 端點。

---

## 8. 驗收標準

### 8.1 DDD 驗收

- [ ] 領域模型正確封裝業務邏輯
- [ ] 聚合邊界明確，不變量得到保護
- [ ] Value Objects 不可變且驗證自身
- [ ] Domain Events 在聚合狀態變更時發布
- [ ] 無 Anemic Domain Model（貧血模型）

### 8.2 CQRS 驗收

- [ ] Commands 與 Queries 完全分離
- [ ] Command Handler 只接受 Command，回傳 void 或 ID
- [ ] Query Handler 只接受 Query，回傳 Read Model
- [ ] Read Model 針對特定查詢場景設計

### 8.3 六角形架構驗收

- [ ] Domain 層無任何外部框架依賴
- [ ] Ports 定義清晰的介面
- [ ] Adapters 可替換
- [ ] 依賴方向：Adapter → Application → Domain

### 8.4 稽核機制驗收

- [ ] 稽核邏輯透過 AOP 實現，與業務邏輯完全分離
- [ ] 稽核函式庫可作為獨立依賴被不同微服務引用
- [ ] 所有標註 `@Auditable` 的操作自動產生稽核日誌
- [ ] 稽核儲存採用 Append-Only 模式，不允許更新或刪除
- [ ] Payload 超過 64 KB 時正確截斷並標註
- [ ] 敏感欄位正確遮蔽（保留格式，隱藏值）
- [ ] 稽核失敗不影響業務操作執行
- [ ] 監控指標正確暴露（事件計數、延遲、佇列深度）
- [ ] 寫入吞吐量達標（100-500 events/second）
- [ ] 擷取延遲 < 50ms（正常負載）
- [ ] 查詢效能達標（百萬筆記錄 5 秒內回應）
- [ ] 契約測試 (Contract Testing) 通過，確保 API 相容性

### 8.5 契約測試驗收 (Consumer-Driven Contract)

- [x] 使用 Spring Cloud Contract 定義 API 契約
- [x] Provider 端自動產生並執行契約測試
- [x] 產生 Stubs JAR 供 Consumer 端使用
- [x] 契約涵蓋所有公開 API 端點（查詢、錯誤處理）
- [ ] 契約測試整合至 CI/CD 流程

### 8.6 安全管控驗收

#### 南北向安全 (North-South)

- [x] OAuth2 Resource Server 配置 (JWT 驗證)
- [x] Keycloak Realm Role 提取與轉換
- [x] CORS 配置支援跨域請求
- [x] 端點授權 (@PreAuthorize) 保護稽核 API
- [x] 公開端點白名單 (Actuator health/info)
- [x] 可配置的安全屬性 (SecurityProperties)
- [ ] TLS/HTTPS 加密傳輸

#### 東西向安全 (East-West)

- [x] OAuth2 Client Credentials Flow 實作
- [x] ServiceTokenProvider Token 取得與快取
- [x] ServiceAuthInterceptor 請求攔截器
- [x] 預配置 RestTemplate (serviceRestTemplate)
- [ ] mTLS 雙向憑證驗證
- [ ] Kubernetes NetworkPolicy 網路隔離

---

## 9. 附錄

### 9.1 術語表

| 術語 | 說明 |
|------|------|
| **Aggregate** | 聚合，一組相關物件的集合，透過 Aggregate Root 存取 |
| **Aggregate Root** | 聚合根，聚合的入口點，負責維護不變量 |
| **Value Object** | 值物件，不可變，透過值判斷相等性，無唯一識別 |
| **Entity** | 實體，有唯一識別，可變 |
| **Domain Event** | 領域事件，記錄領域中發生的重要事實，過去式命名 |
| **Bounded Context** | 限界上下文，模型的語意邊界，同一個詞在不同 Context 可能有不同意義 |
| **Command** | 命令，表達「做某事」的意圖，可能改變狀態 |
| **Query** | 查詢，請求資訊，不改變狀態 |
| **CQRS** | 命令查詢職責分離，讀寫使用不同模型 |
| **Hexagonal Architecture** | 六角形架構，又稱 Ports & Adapters，核心與外部世界解耦 |

### 9.2 相關文件

- [TECH.md](./TECH.md) - 技術架構文件
- [INFRA.md](./INFRA.md) - 基礎設施文件
# PRD: 電子商務多租戶平台 - 產品需求文件

## 文件資訊

| 項目 | 內容 |
|------|------|
| 文件版本 | 1.0 |
| 建立日期 | 2026-01-10 |
| 專案代號 | ECOMMERCE-MULTITENANT-POC |
| 專案類型 | Monorepo (Gradle Multi-Module) |
| 狀態 | 待實作 |

---

## 1. 專案概述

### 1.1 專案目標

建立一個支援多租戶（Multi-Tenant）的電子商務平台 POC，使用 Monorepo 架構管理多個微服務，實現完整的 RBAC（角色型存取控制）機制。

### 1.2 核心功能

| 功能模組 | 說明 |
|----------|------|
| 商品管理 | 商品 CRUD、分類管理 |
| 使用者管理 | 帳號建立、角色指派 |
| 多租戶隔離 | 租戶資料隔離、獨立管理 |
| 權限控制 | RBAC 角色權限驗證 |

---

## 2. Monorepo 架構需求

### 2.1 強制要求

```
┌─────────────────────────────────────────────────────────────────────────┐
│  🚨 Monorepo 強制規範                                                    │
├─────────────────────────────────────────────────────────────────────────┤
│                                                                         │
│  1. 所有微服務必須位於 services/ 目錄下                                   │
│  2. 所有共用模組必須位於 libs/ 目錄下                                     │
│  3. 每個微服務/模組必須是 Gradle sub-module                              │
│  4. 禁止微服務之間直接依賴，必須透過 common-lib                           │
│  5. 每個微服務必須有獨立的 build.gradle.kts                              │
│  6. 根目錄 settings.gradle.kts 必須 include 所有模組                     │
│                                                                         │
└─────────────────────────────────────────────────────────────────────────┘
```

### 2.2 目錄結構規範

```
ecommerce-multitenant-poc/
├── settings.gradle.kts              # Gradle 根設定 (include 所有模組)
├── build.gradle.kts                 # 根 build 設定
├── gradle.properties                # Gradle 屬性
├── gradle/
│   └── wrapper/
│       └── gradle-wrapper.properties
│
├── libs/                            # 共用模組
│   ├── common-lib/                  # 共用 DTO、工具類、例外處理
│   │   ├── build.gradle.kts
│   │   └── src/main/java/
│   ├── security-lib/                # Spring Security 配置、JWT
│   │   ├── build.gradle.kts
│   │   └── src/main/java/
│   └── tenant-lib/                  # 多租戶核心機制
│       ├── build.gradle.kts
│       └── src/main/java/
│
├── services/                        # 微服務
│   ├── product-service/             # 商品服務
│   │   ├── build.gradle.kts
│   │   └── src/
│   │       ├── main/java/
│   │       └── test/java/
│   ├── user-service/                # 使用者服務
│   │   ├── build.gradle.kts
│   │   └── src/
│   │       ├── main/java/
│   │       └── test/java/
│   └── gateway-service/             # API Gateway
│       ├── build.gradle.kts
│       └── src/
│
├── infra/                           # 基礎設施配置
│   ├── docker-compose.yml
│   ├── keycloak/
│   │   └── realm-export.json
│   └── db/
│       └── init.sql
│
├── docs/                            # 文件
│   ├── PRD.md
│   ├── TECH.md
│   └── INFRA.md
│
└── tests/                           # 整合測試/情境測試
    └── scenario-tests/
        └── src/test/java/
```

### 2.3 Gradle Multi-Module 設定

**settings.gradle.kts (根目錄)**
```kotlin
rootProject.name = "ecommerce-multitenant-poc"

// 共用模組
include("libs:common-lib")
include("libs:security-lib")
include("libs:tenant-lib")

// 微服務
include("services:product-service")
include("services:user-service")
include("services:gateway-service")

// 整合測試
include("tests:scenario-tests")
```

---

## 3. 使用者角色與權限

### 3.1 角色定義

| 角色 | 代碼 | 說明 |
|------|------|------|
| 系統管理者 | ROLE_ADMIN | 完整系統權限 |
| 租戶管理者 | ROLE_TENANT_ADMIN | 租戶內完整權限 |
| 一般使用者 | ROLE_USER | 僅查詢權限 |

### 3.2 權限矩陣

| 功能 | ADMIN | TENANT_ADMIN | USER |
|------|:-----:|:------------:|:----:|
| 查詢商品 | ✅ | ✅ | ✅ |
| 新增商品 | ✅ | ✅ | ❌ |
| 修改商品 | ✅ | ✅ | ❌ |
| 刪除商品 | ✅ | ✅ | ❌ |
| 新增使用者 | ✅ | ✅ | ❌ |
| 管理租戶 | ✅ | ❌ | ❌ |

### 3.3 預設測試帳號

| Username | Password | Role | Tenant |
|----------|----------|------|--------|
| admin | admin123 | ROLE_ADMIN | system |
| tenant1.admin | tenant123 | ROLE_TENANT_ADMIN | tenant1 |
| tenant1.user | user123 | ROLE_USER | tenant1 |
| tenant2.admin | tenant123 | ROLE_TENANT_ADMIN | tenant2 |
| tenant2.user | user123 | ROLE_USER | tenant2 |

---

## 4. 預設商品資料

### 4.1 商品清單 (10 筆預設資料)

| ID | 商品名稱 | 分類 | 價格 | 庫存 | Tenant |
|----|----------|------|------|------|--------|
| 1 | iPhone 15 Pro | 手機 | 35,900 | 100 | tenant1 |
| 2 | MacBook Pro 14" | 筆電 | 59,900 | 50 | tenant1 |
| 3 | AirPods Pro 2 | 配件 | 7,490 | 200 | tenant1 |
| 4 | iPad Air | 平板 | 19,900 | 80 | tenant1 |
| 5 | Apple Watch S9 | 穿戴 | 12,900 | 150 | tenant1 |
| 6 | Samsung Galaxy S24 | 手機 | 27,900 | 120 | tenant2 |
| 7 | Galaxy Tab S9 | 平板 | 24,900 | 60 | tenant2 |
| 8 | Galaxy Buds3 | 配件 | 5,490 | 180 | tenant2 |
| 9 | Galaxy Watch 6 | 穿戴 | 9,990 | 100 | tenant2 |
| 10 | Galaxy Book3 | 筆電 | 42,900 | 40 | tenant2 |

---

## 5. 情境測試規格 (Gherkin)

### 5.1 商品管理情境

#### Feature: 商品查詢

```gherkin
Feature: 商品查詢功能
  作為平台使用者
  我希望能夠查詢商品
  以便了解可購買的商品資訊

  Background:
    Given 系統已初始化 10 筆預設商品
    And 存在預設的管理者帳號 "admin"

  Scenario: 管理者查詢所有商品
    Given 我以 "admin" 身份登入
    When 我發送 GET 請求到 "/api/products"
    Then 應該回傳 HTTP 200
    And 回應應包含 10 筆商品

  Scenario: 一般使用者查詢租戶商品
    Given 我以 "tenant1.user" 身份登入
    When 我發送 GET 請求到 "/api/products"
    Then 應該回傳 HTTP 200
    And 回應應只包含 tenant1 的 5 筆商品

  Scenario: 使用者查詢單一商品
    Given 我以 "tenant1.user" 身份登入
    When 我發送 GET 請求到 "/api/products/1"
    Then 應該回傳 HTTP 200
    And 回應應包含商品名稱 "iPhone 15 Pro"

  Scenario: 使用者無法查詢其他租戶商品
    Given 我以 "tenant1.user" 身份登入
    When 我發送 GET 請求到 "/api/products/6"
    Then 應該回傳 HTTP 403
```

#### Feature: 商品新增

```gherkin
Feature: 商品新增功能
  作為管理者
  我希望能夠新增商品
  以便擴充商品目錄

  Background:
    Given 存在預設的管理者帳號 "admin"
    And 存在預設的租戶管理者帳號 "tenant1.admin"
    And 存在預設的一般使用者帳號 "tenant1.user"

  Scenario: 管理者新增商品成功
    Given 我以 "admin" 身份登入
    When 我發送 POST 請求到 "/api/products" 包含以下資料:
      | name       | category | price  | stock |
      | 新商品     | 其他     | 9999   | 10    |
    Then 應該回傳 HTTP 201
    And 回應應包含新建立的商品 ID

  Scenario: 租戶管理者新增商品成功
    Given 我以 "tenant1.admin" 身份登入
    When 我發送 POST 請求到 "/api/products" 包含以下資料:
      | name       | category | price  | stock |
      | 租戶商品   | 電子     | 5000   | 20    |
    Then 應該回傳 HTTP 201
    And 商品應自動歸屬於 tenant1

  Scenario: 一般使用者無法新增商品
    Given 我以 "tenant1.user" 身份登入
    When 我發送 POST 請求到 "/api/products" 包含以下資料:
      | name       | category | price  | stock |
      | 非法商品   | 其他     | 1000   | 5     |
    Then 應該回傳 HTTP 403
    And 錯誤訊息應為 "Access Denied"
```

#### Feature: 商品修改

```gherkin
Feature: 商品修改功能
  作為管理者
  我希望能夠修改商品資訊
  以便維護商品資料正確性

  Background:
    Given 系統已初始化 10 筆預設商品
    And 存在預設的管理者帳號

  Scenario: 管理者修改商品成功
    Given 我以 "admin" 身份登入
    When 我發送 PUT 請求到 "/api/products/1" 包含以下資料:
      | price | 39900 |
    Then 應該回傳 HTTP 200
    And 商品 1 的價格應為 39900

  Scenario: 租戶管理者只能修改自己租戶的商品
    Given 我以 "tenant1.admin" 身份登入
    When 我發送 PUT 請求到 "/api/products/6" 包含以下資料:
      | price | 25000 |
    Then 應該回傳 HTTP 403

  Scenario: 一般使用者無法修改商品
    Given 我以 "tenant1.user" 身份登入
    When 我發送 PUT 請求到 "/api/products/1" 包含以下資料:
      | price | 30000 |
    Then 應該回傳 HTTP 403
```

#### Feature: 商品刪除

```gherkin
Feature: 商品刪除功能
  作為管理者
  我希望能夠刪除商品
  以便移除不需要的商品

  Background:
    Given 系統已初始化 10 筆預設商品

  Scenario: 管理者刪除商品成功
    Given 我以 "admin" 身份登入
    When 我發送 DELETE 請求到 "/api/products/1"
    Then 應該回傳 HTTP 204
    And 商品 1 應標記為已刪除

  Scenario: 租戶管理者只能刪除自己租戶的商品
    Given 我以 "tenant1.admin" 身份登入
    When 我發送 DELETE 請求到 "/api/products/6"
    Then 應該回傳 HTTP 403

  Scenario: 一般使用者無法刪除商品
    Given 我以 "tenant1.user" 身份登入
    When 我發送 DELETE 請求到 "/api/products/1"
    Then 應該回傳 HTTP 403
```

### 5.2 使用者管理情境

#### Feature: 使用者管理

```gherkin
Feature: 使用者管理功能
  作為管理者
  我希望能夠管理使用者
  以便控制系統存取權限

  Background:
    Given 存在預設的管理者帳號

  Scenario: 管理者新增一般使用者
    Given 我以 "admin" 身份登入
    When 我發送 POST 請求到 "/api/users" 包含以下資料:
      | username      | password  | role      | tenant  |
      | new.user      | pass123   | ROLE_USER | tenant1 |
    Then 應該回傳 HTTP 201
    And 新使用者應成功建立

  Scenario: 租戶管理者只能在自己租戶內新增使用者
    Given 我以 "tenant1.admin" 身份登入
    When 我發送 POST 請求到 "/api/users" 包含以下資料:
      | username      | password  | role      | tenant  |
      | tenant.user2  | pass123   | ROLE_USER | tenant1 |
    Then 應該回傳 HTTP 201
    And 新使用者應歸屬於 tenant1

  Scenario: 租戶管理者無法新增其他租戶使用者
    Given 我以 "tenant1.admin" 身份登入
    When 我發送 POST 請求到 "/api/users" 包含以下資料:
      | username      | password  | role      | tenant  |
      | hack.user     | pass123   | ROLE_USER | tenant2 |
    Then 應該回傳 HTTP 403

  Scenario: 一般使用者無法新增使用者
    Given 我以 "tenant1.user" 身份登入
    When 我發送 POST 請求到 "/api/users" 包含以下資料:
      | username      | password  | role      | tenant  |
      | illegal.user  | pass123   | ROLE_USER | tenant1 |
    Then 應該回傳 HTTP 403
```

### 5.3 多租戶隔離情境

#### Feature: 租戶資料隔離

```gherkin
Feature: 多租戶資料隔離
  作為平台
  我需要確保租戶資料完全隔離
  以保障資料安全性

  Background:
    Given 系統已初始化 10 筆預設商品
    And 商品 1-5 屬於 tenant1
    And 商品 6-10 屬於 tenant2

  Scenario: Tenant1 使用者只能看到 Tenant1 資料
    Given 我以 "tenant1.user" 身份登入
    When 我發送 GET 請求到 "/api/products"
    Then 應該回傳 HTTP 200
    And 回應應只包含 5 筆商品
    And 所有商品的 tenantId 應為 "tenant1"

  Scenario: Tenant2 使用者只能看到 Tenant2 資料
    Given 我以 "tenant2.user" 身份登入
    When 我發送 GET 請求到 "/api/products"
    Then 應該回傳 HTTP 200
    And 回應應只包含 5 筆商品
    And 所有商品的 tenantId 應為 "tenant2"

  Scenario: 系統管理者可以看到所有租戶資料
    Given 我以 "admin" 身份登入
    When 我發送 GET 請求到 "/api/products"
    Then 應該回傳 HTTP 200
    And 回應應包含 10 筆商品

  Scenario: 跨租戶存取應被拒絕
    Given 我以 "tenant1.user" 身份登入
    When 我嘗試直接存取 tenant2 的資料
    Then 應該回傳 HTTP 403
    And 存取記錄應記錄此安全事件
```

---

## 6. API 規格

### 6.1 商品服務 API

| Method | Endpoint | 說明 | 權限 |
|--------|----------|------|------|
| GET | /api/products | 查詢商品列表 | USER, ADMIN |
| GET | /api/products/{id} | 查詢單一商品 | USER, ADMIN |
| POST | /api/products | 新增商品 | ADMIN |
| PUT | /api/products/{id} | 修改商品 | ADMIN |
| DELETE | /api/products/{id} | 刪除商品 | ADMIN |

### 6.2 使用者服務 API

| Method | Endpoint | 說明 | 權限 |
|--------|----------|------|------|
| GET | /api/users | 查詢使用者列表 | ADMIN |
| GET | /api/users/{id} | 查詢單一使用者 | ADMIN |
| POST | /api/users | 新增使用者 | ADMIN |
| PUT | /api/users/{id} | 修改使用者 | ADMIN |
| DELETE | /api/users/{id} | 刪除使用者 | ADMIN |

---

## 7. 驗收標準

### 7.1 Phase 0: Monorepo 初始化

- [ ] 根目錄 settings.gradle.kts 正確 include 所有模組
- [ ] 根目錄 build.gradle.kts 設定完成
- [ ] 所有子模組可獨立編譯
- [ ] `./gradlew build` 全部通過

### 7.2 Phase 1: 基礎設施

- [ ] Docker Compose 可啟動所有服務
- [ ] Keycloak 初始化完成
- [ ] 預設帳號可登入
- [ ] 資料庫初始化完成

### 7.3 Phase 2: 微服務開發

- [ ] Product Service 完成 CRUD
- [ ] User Service 完成使用者管理
- [ ] Gateway Service 路由設定完成
- [ ] 多租戶隔離機制實作完成

### 7.4 Phase 3: 測試與驗收

- [ ] 單元測試覆蓋率 > 80%
- [ ] 所有情境測試通過
- [ ] 權限控制測試通過
- [ ] 多租戶隔離測試通過
- [ ] 10 筆預設商品正確初始化

---

## 8. AI 實作指引

### 8.1 強制執行順序

```
Phase 0 (Monorepo) → Phase 1 (INFRA) → Phase 2 (Services) → Phase 3 (Tests)
       │                    │                   │                  │
       ▼                    ▼                   ▼                  ▼
    驗證編譯            驗證容器            驗證 API           驗證測試
```

### 8.2 禁止事項

```
❌ 跳過 Phase 0 直接開發服務
❌ 未設定 Gradle multi-module 就開始編碼
❌ 微服務之間直接引用（必須透過 common-lib）
❌ 在未完成 INFRA 前開始微服務開發
❌ 跳過情境測試
❌ 未初始化預設資料就進行測試
```

---

## 附錄 A: 相關文件

- [TECH.md](./TECH.md) - 技術架構文件
- [INFRA.md](./INFRA.md) - 基礎設施文件

---

*— PRD 文件結束 —*
