# TECH: 技術架構文件

## 文件資訊

| 項目 | 內容 |
|------|------|
| 文件版本 | 3.2 |
| 建立日期 | 2025-01-10 |
| 最後更新 | 2026-01-10 |
| 專案代號 | ECOMMERCE-MSA-POC |
| 架構模式 | Hexagonal Architecture + CQRS + DDD |

---

## 1. 技術棧總覽

| Category | Technology | Version |
|----------|------------|---------|
| Language | Java | 17 (LTS) |
| Framework | Spring Boot | 3.3.x |
| Build Tool | Gradle | 8.5+ |
| Gateway | Spring Cloud Gateway | 4.1.x |
| Security | Spring Security + OAuth2 | 6.3.x |
| API Doc | SpringDoc OpenAPI | 2.5.x |
| Database | H2 (Dev) / PostgreSQL (Prod) | - |
| ORM | Spring Data JPA | 3.3.x |
| AOP | Spring AOP | 6.1.x |
| Audit | audit-lib (Shared) | 1.0.x |
| Metrics | Micrometer | 1.12.x |
| Architecture Test | ArchUnit | 1.2.x |
| Contract Test | Spring Cloud Contract | 4.1.x |
| Container | Docker | 24.x |
| Orchestration | Kubernetes | 1.28+ |

---

## 2. 六角形架構 (Hexagonal Architecture)

### 2.1 架構概念

```
┌─────────────────────────────────────────────────────────────────────────────────┐
│                     Hexagonal Architecture (Ports & Adapters)                    │
├─────────────────────────────────────────────────────────────────────────────────┤
│                                                                                  │
│  核心原則：                                                                      │
│  ═════════                                                                       │
│  • 業務邏輯（Domain）位於架構中心，不依賴任何外部技術                            │
│  • 透過 Ports（介面）定義與外部世界的互動契約                                    │
│  • Adapters 實作 Ports，處理具體的技術細節                                       │
│  • 依賴方向：外層 → 內層（Adapters → Application → Domain）                     │
│                                                                                  │
│                          ┌─────────────────┐                                    │
│                          │    Driving      │                                    │
│                          │   Adapters      │                                    │
│                          │  (Left Side)    │                                    │
│                          │                 │                                    │
│                          │ • REST API      │                                    │
│                          │ • GraphQL       │                                    │
│                          │ • Message Queue │                                    │
│                          └────────┬────────┘                                    │
│                                   │                                             │
│                                   │ calls                                       │
│                                   ▼                                             │
│                          ┌─────────────────┐                                    │
│                          │  Input Ports    │                                    │
│                          │  (Use Cases)    │                                    │
│                          │                 │                                    │
│                          │ • CommandHandler│                                    │
│                          │ • QueryHandler  │                                    │
│                          └────────┬────────┘                                    │
│                                   │                                             │
│          ┌────────────────────────┼────────────────────────┐                   │
│          │                        │                        │                   │
│          │     ╔══════════════════╧══════════════════╗     │                   │
│          │     ║         APPLICATION LAYER           ║     │                   │
│          │     ║  • Command Services                 ║     │                   │
│          │     ║  • Query Services                   ║     │                   │
│          │     ║  • DTOs / Commands / Queries        ║     │                   │
│          │     ╚══════════════════╤══════════════════╝     │                   │
│          │                        │                        │                   │
│          │     ╔══════════════════╧══════════════════╗     │                   │
│          │     ║           DOMAIN LAYER              ║     │                   │
│          │     ║         (Pure Business)             ║     │                   │
│          │     ║                                     ║     │                   │
│          │     ║  • Aggregates & Entities            ║     │                   │
│          │     ║  • Value Objects                    ║     │                   │
│          │     ║  • Domain Services                  ║     │                   │
│          │     ║  • Domain Events                    ║     │                   │
│          │     ║  • Repository Interfaces (Ports)    ║     │                   │
│          │     ║                                     ║     │                   │
│          │     ║  ⚠️ NO external dependencies!       ║     │                   │
│          │     ╚══════════════════╤══════════════════╝     │                   │
│          │                        │                        │                   │
│          └────────────────────────┼────────────────────────┘                   │
│                                   │                                             │
│                                   │ defines                                     │
│                                   ▼                                             │
│                          ┌─────────────────┐                                    │
│                          │  Output Ports   │                                    │
│                          │  (Interfaces)   │                                    │
│                          │                 │                                    │
│                          │ • Repository    │                                    │
│                          │ • EventPublisher│                                    │
│                          └────────┬────────┘                                    │
│                                   │                                             │
│                                   │ implemented by                              │
│                                   ▼                                             │
│                          ┌─────────────────┐                                    │
│                          │    Driven       │                                    │
│                          │   Adapters      │                                    │
│                          │  (Right Side)   │                                    │
│                          │                 │                                    │
│                          │ • JPA Repo      │                                    │
│                          │ • Kafka         │                                    │
│                          │ • HTTP Client   │                                    │
│                          └─────────────────┘                                    │
│                                                                                  │
└─────────────────────────────────────────────────────────────────────────────────┘
```

### 2.2 分層依賴規則

```
┌─────────────────────────────────────────────────────────────────────────────────┐
│                           Layer Dependencies                                     │
├─────────────────────────────────────────────────────────────────────────────────┤
│                                                                                  │
│  ┌─────────────────────────────────────────────────────────────────────────┐   │
│  │  ADAPTER LAYER (Infrastructure)                                          │   │
│  │  ─────────────────────────────────                                       │   │
│  │  Driving Adapters:                  Driven Adapters:                     │   │
│  │  • REST Controllers                 • JPA Repositories                  │   │
│  │  • Message Listeners                • Event Publishers                  │   │
│  │                                                                          │   │
│  │  可依賴：Application, Domain, 技術框架 (Spring, JPA...)                  │   │
│  └─────────────────────────────────────────────────────────────────────────┘   │
│                                     │                                           │
│                                     │ depends on                                │
│                                     ▼                                           │
│  ┌─────────────────────────────────────────────────────────────────────────┐   │
│  │  APPLICATION LAYER                                                       │   │
│  │  ─────────────────                                                       │   │
│  │  • Command Handlers, Query Handlers                                      │   │
│  │  • Application Services                                                  │   │
│  │  • DTOs, Commands, Queries                                               │   │
│  │                                                                          │   │
│  │  可依賴：Domain Layer                                                    │   │
│  │  ⚠️ 不可依賴：Adapter Layer                                              │   │
│  └─────────────────────────────────────────────────────────────────────────┘   │
│                                     │                                           │
│                                     │ depends on                                │
│                                     ▼                                           │
│  ┌─────────────────────────────────────────────────────────────────────────┐   │
│  │  DOMAIN LAYER                                                            │   │
│  │  ────────────                                                            │   │
│  │  • Aggregates, Entities, Value Objects                                   │   │
│  │  • Domain Services, Domain Events                                        │   │
│  │  • Repository Interfaces (Output Ports)                                  │   │
│  │                                                                          │   │
│  │  ⚠️ 絕對不可依賴：任何其他層、任何框架（Spring, JPA, Jackson...）       │   │
│  │  ⚠️ 只能使用：Java 標準庫                                                │   │
│  └─────────────────────────────────────────────────────────────────────────┘   │
│                                                                                  │
└─────────────────────────────────────────────────────────────────────────────────┘
```

---

## 3. SOLID 原則應用

### 3.1 SRP - 單一職責原則

```java
// ❌ 違反 SRP：一個類別做太多事
class ProductService {
    public Product createProduct(...) { }      // 寫入
    public List<Product> findAll() { }         // 讀取
    public void adjustStock(...) { }           // 庫存
    public Report generateReport() { }         // 報表
}

// ✅ 遵循 SRP：CQRS 分離
class ProductCommandService {                  // 只處理寫入
    public ProductId handle(CreateProductCommand cmd) { }
    public void handle(UpdateProductCommand cmd) { }
}

class ProductQueryService {                    // 只處理讀取
    public ProductDetailView handle(GetProductByIdQuery q) { }
    public PagedResult<ProductListView> handle(ListProductsQuery q) { }
}

class InventoryCommandService {                // 獨立的庫存服務
    public void handle(AdjustStockCommand cmd) { }
}
```

### 3.2 OCP - 開放封閉原則

```java
// ✅ 對擴展開放，對修改封閉
// Domain Event Handler 介面
public interface DomainEventHandler<T extends DomainEvent> {
    void handle(T event);
}

// 新增功能只需新增類別
@Component
class CreateInventoryOnProductCreated implements DomainEventHandler<ProductCreated> {
    public void handle(ProductCreated event) { /* 建立庫存 */ }
}

@Component
class NotifyAdminOnProductCreated implements DomainEventHandler<ProductCreated> {
    public void handle(ProductCreated event) { /* 通知管理員 */ }
}
```

### 3.3 LSP - 里氏替換原則

```java
// Domain Layer 定義介面
public interface ProductRepository {
    Optional<Product> findById(ProductId id);
    Product save(Product product);
}

// Adapter Layer 實作 - 兩種實作可互相替換
@Repository
class JpaProductRepository implements ProductRepository { /* JPA 實作 */ }

class InMemoryProductRepository implements ProductRepository { /* 測試用 */ }
```

### 3.4 ISP - 介面隔離原則

```java
// ✅ 分離 Command 和 Query 介面
interface ProductRepository {              // Write 用
    Optional<Product> findById(ProductId id);
    Product save(Product product);
}

interface ProductReadRepository {          // Query 用
    Optional<ProductDetailView> findDetailById(UUID id);
    Page<ProductListView> findAllActive(Pageable pageable);
}
```

### 3.5 DIP - 依賴反轉原則

```java
// Domain Layer 定義抽象（介面）
public interface ProductRepository {
    Product save(Product product);
}

// Application Layer 依賴抽象
@Service
class ProductCommandService {
    private final ProductRepository repository;  // 依賴介面，不是具體類別
}

// Adapter Layer 實作抽象
@Repository
class JpaProductRepository implements ProductRepository {
    // 依賴方向反轉：Adapter → Domain
}
```

---

## 4. 專案結構

### 4.1 微服務內部結構（六角形架構）

```
services/product-service/
├── build.gradle
├── Dockerfile
└── src/main/java/com/example/product/
    │
    ├── ProductServiceApplication.java
    │
    │   ╔═══════════════════════════════════════════════════════╗
    │   ║                    DOMAIN LAYER                       ║
    │   ║               (無任何外部依賴)                        ║
    │   ╚═══════════════════════════════════════════════════════╝
    │
    ├── domain/
    │   ├── model/
    │   │   ├── aggregate/
    │   │   │   ├── Product.java            # Product 聚合根
    │   │   │   └── Inventory.java          # Inventory 聚合根
    │   │   └── valueobject/
    │   │       ├── ProductId.java
    │   │       ├── ProductCode.java
    │   │       ├── Money.java
    │   │       ├── Quantity.java
    │   │       └── AuditInfo.java
    │   │
    │   ├── event/                          # 領域事件
    │   │   ├── DomainEvent.java
    │   │   ├── ProductCreated.java
    │   │   ├── ProductUpdated.java
    │   │   └── StockAdjusted.java
    │   │
    │   ├── repository/                     # Repository 介面 (Output Ports)
    │   │   ├── ProductRepository.java
    │   │   └── InventoryRepository.java
    │   │
    │   └── exception/
    │       └── ProductNotFoundException.java
    │
    │   ╔═══════════════════════════════════════════════════════╗
    │   ║                  APPLICATION LAYER                    ║
    │   ╚═══════════════════════════════════════════════════════╝
    │
    ├── application/
    │   ├── port/
    │   │   ├── input/
    │   │   │   ├── command/
    │   │   │   │   ├── CreateProductCommand.java
    │   │   │   │   ├── UpdateProductCommand.java
    │   │   │   │   └── AdjustStockCommand.java
    │   │   │   └── query/
    │   │   │       ├── GetProductByIdQuery.java
    │   │   │       └── ListProductsQuery.java
    │   │   │
    │   │   └── output/
    │   │       ├── ProductReadRepository.java
    │   │       └── DomainEventPublisher.java
    │   │
    │   ├── service/
    │   │   ├── ProductCommandService.java
    │   │   └── ProductQueryService.java
    │   │
    │   └── dto/
    │       ├── ProductDetailView.java
    │       └── ProductListView.java
    │
    │   ╔═══════════════════════════════════════════════════════╗
    │   ║                   ADAPTER LAYER                       ║
    │   ╚═══════════════════════════════════════════════════════╝
    │
    └── adapter/
        ├── inbound/
        │   └── rest/
        │       ├── ProductCommandController.java
        │       ├── ProductQueryController.java
        │       └── dto/
        │           ├── CreateProductRequest.java
        │           └── UpdateProductRequest.java
        │
        ├── outbound/
        │   └── persistence/
        │       ├── JpaProductRepository.java
        │       ├── JpaProductReadRepository.java
        │       └── entity/
        │           ├── ProductJpaEntity.java
        │           └── mapper/
        │               └── ProductMapper.java
        │
        └── config/
            ├── SecurityConfig.java
            └── OpenApiConfig.java
```

---

## 5. 核心程式碼實作

### 5.1 Domain Layer - Value Object

```java
// domain/model/valueobject/Money.java
package com.example.product.domain.model.valueobject;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * 金額 Value Object - 不可變，自我驗證
 */
public final class Money {
    
    private final BigDecimal amount;
    
    private Money(BigDecimal amount) {
        this.amount = Objects.requireNonNull(amount).setScale(2);
    }
    
    public static Money of(BigDecimal amount) {
        return new Money(amount);
    }
    
    public static Money zero() {
        return new Money(BigDecimal.ZERO);
    }
    
    public Money add(Money other) {
        return new Money(this.amount.add(other.amount));
    }
    
    public Money subtract(Money other) {
        return new Money(this.amount.subtract(other.amount));
    }
    
    public boolean isPositive() {
        return amount.compareTo(BigDecimal.ZERO) > 0;
    }
    
    public void validatePositive() {
        if (!isPositive()) {
            throw new IllegalArgumentException("Price must be positive");
        }
    }
    
    public BigDecimal amount() { return amount; }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Money money)) return false;
        return amount.compareTo(money.amount) == 0;
    }
    
    @Override
    public int hashCode() { return Objects.hash(amount); }
}
```

### 5.2 Domain Layer - Aggregate Root

```java
// domain/model/aggregate/Product.java
package com.example.product.domain.model.aggregate;

import com.example.product.domain.event.*;
import com.example.product.domain.model.valueobject.*;
import java.time.Instant;
import java.util.*;

/**
 * Product Aggregate Root
 * - 維護不變量
 * - 封裝業務邏輯
 * - 產生領域事件
 */
public class Product {
    
    private final ProductId id;
    private final ProductCode productCode;  // 建立後不可變
    private ProductName productName;
    private Money price;
    private Description description;
    private ProductStatus status;
    private AuditInfo auditInfo;
    
    private final List<DomainEvent> domainEvents = new ArrayList<>();
    
    // ===== Factory Method =====
    public static Product create(ProductCode code, ProductName name,
                                  Money price, Description desc, String createdBy) {
        price.validatePositive();  // 不變量驗證
        
        Product product = new Product(ProductId.generate(), code, name, 
                                       price, desc, createdBy);
        
        product.registerEvent(new ProductCreated(
            product.id, code, name.value(), price, createdBy, Instant.now()
        ));
        
        return product;
    }
    
    private Product(ProductId id, ProductCode code, ProductName name,
                    Money price, Description desc, String createdBy) {
        this.id = id;
        this.productCode = code;
        this.productName = name;
        this.price = price;
        this.description = desc;
        this.status = ProductStatus.ACTIVE;
        this.auditInfo = AuditInfo.create(createdBy);
    }
    
    // ===== Business Methods =====
    
    public void changePrice(Money newPrice, String changedBy) {
        validateNotDeleted();
        newPrice.validatePositive();
        
        Money oldPrice = this.price;
        this.price = newPrice;
        this.auditInfo = auditInfo.update(changedBy);
        
        registerEvent(new ProductPriceChanged(id, oldPrice, newPrice, changedBy));
    }
    
    public void delete(String deletedBy) {
        validateNotDeleted();
        this.status = ProductStatus.DELETED;
        this.auditInfo = auditInfo.update(deletedBy);
        registerEvent(new ProductDeleted(id, deletedBy, Instant.now()));
    }
    
    private void validateNotDeleted() {
        if (status == ProductStatus.DELETED) {
            throw new IllegalStateException("Cannot modify deleted product");
        }
    }
    
    // ===== Domain Events =====
    
    protected void registerEvent(DomainEvent event) {
        domainEvents.add(event);
    }
    
    public List<DomainEvent> pullDomainEvents() {
        List<DomainEvent> events = new ArrayList<>(domainEvents);
        domainEvents.clear();
        return events;
    }
    
    // ===== Getters (No Setters) =====
    public ProductId id() { return id; }
    public ProductCode productCode() { return productCode; }
    public ProductName productName() { return productName; }
    public Money price() { return price; }
    public ProductStatus status() { return status; }
}
```

### 5.3 Domain Layer - Repository Interface

```java
// domain/repository/ProductRepository.java
package com.example.product.domain.repository;

import com.example.product.domain.model.aggregate.Product;
import com.example.product.domain.model.valueobject.*;
import java.util.Optional;

/**
 * Product Repository Interface (Output Port)
 * 定義在 Domain Layer，由 Adapter Layer 實作
 */
public interface ProductRepository {
    Optional<Product> findById(ProductId id);
    Product save(Product product);
    void delete(ProductId id);
    boolean existsByProductCode(ProductCode code);
}
```

### 5.4 Application Layer - Command & Query

```java
// application/port/input/command/CreateProductCommand.java
package com.example.product.application.port.input.command;

import java.math.BigDecimal;

public record CreateProductCommand(
    String productCode,
    String productName,
    BigDecimal price,
    Integer quantity,
    String description,
    String createdBy
) {
    public CreateProductCommand {
        if (productName == null || productName.isBlank())
            throw new IllegalArgumentException("Product name required");
        if (price == null || price.compareTo(BigDecimal.ZERO) <= 0)
            throw new IllegalArgumentException("Price must be positive");
    }
}

// application/port/input/query/ListProductsQuery.java
package com.example.product.application.port.input.query;

public record ListProductsQuery(
    int page,
    int size,
    String sortBy,
    String sortDirection
) {
    public static ListProductsQuery defaultQuery() {
        return new ListProductsQuery(0, 20, "createdAt", "DESC");
    }
}
```

### 5.5 Application Layer - Services

```java
// application/service/ProductCommandService.java
package com.example.product.application.service;

import com.example.product.application.port.input.command.*;
import com.example.product.application.port.output.DomainEventPublisher;
import com.example.product.domain.model.aggregate.Product;
import com.example.product.domain.model.valueobject.*;
import com.example.product.domain.repository.ProductRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class ProductCommandService {
    
    private final ProductRepository repository;
    private final DomainEventPublisher eventPublisher;
    
    public ProductCommandService(ProductRepository repository,
                                  DomainEventPublisher eventPublisher) {
        this.repository = repository;
        this.eventPublisher = eventPublisher;
    }
    
    public ProductId handle(CreateProductCommand cmd) {
        ProductCode code = cmd.productCode() != null 
            ? ProductCode.of(cmd.productCode())
            : ProductCode.generate();
        
        if (repository.existsByProductCode(code)) {
            throw new DuplicateProductCodeException(code);
        }
        
        Product product = Product.create(
            code,
            ProductName.of(cmd.productName()),
            Money.of(cmd.price()),
            Description.of(cmd.description()),
            cmd.createdBy()
        );
        
        repository.save(product);
        eventPublisher.publishAll(product.pullDomainEvents());
        
        return product.id();
    }
}

// application/service/ProductQueryService.java
package com.example.product.application.service;

import com.example.product.application.dto.*;
import com.example.product.application.port.input.query.*;
import com.example.product.application.port.output.ProductReadRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class ProductQueryService {
    
    private final ProductReadRepository readRepository;
    
    public ProductQueryService(ProductReadRepository readRepository) {
        this.readRepository = readRepository;
    }
    
    public ProductDetailView handle(GetProductByIdQuery query) {
        return readRepository.findDetailById(query.productId())
            .orElseThrow(() -> new ProductNotFoundException(query.productId()));
    }
    
    public PageResponse<ProductListView> handle(ListProductsQuery query) {
        return readRepository.findAllActive(query.page(), query.size());
    }
}
```

### 5.6 Adapter Layer - REST Controller

```java
// adapter/inbound/rest/ProductCommandController.java
package com.example.product.adapter.inbound.rest;

import com.example.product.adapter.inbound.rest.dto.*;
import com.example.product.application.port.input.command.*;
import com.example.product.application.service.ProductCommandService;
import com.example.audit.annotation.Auditable;
import com.example.common.dto.ApiResponse;
import com.example.security.util.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.UUID;

@RestController
@RequestMapping("/api/products")
@Tag(name = "Product Commands")
public class ProductCommandController {
    
    private final ProductCommandService commandService;
    
    public ProductCommandController(ProductCommandService commandService) {
        this.commandService = commandService;
    }
    
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "建立商品")
    @Auditable(eventType = "PRODUCT_CREATED", resourceType = "Product")
    public ApiResponse<UUID> createProduct(@Valid @RequestBody CreateProductRequest req) {
        
        String user = SecurityUtils.getCurrentUsername().orElse("unknown");
        
        CreateProductCommand cmd = new CreateProductCommand(
            req.productCode(), req.productName(), req.price(),
            req.quantity(), req.description(), user
        );
        
        var productId = commandService.handle(cmd);
        return ApiResponse.success(productId.value(), "Product created");
    }
    
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('ADMIN')")
    @Auditable(eventType = "PRODUCT_DELETED", resourceType = "Product")
    public void deleteProduct(@PathVariable UUID id) {
        String user = SecurityUtils.getCurrentUsername().orElse("unknown");
        commandService.handle(new DeleteProductCommand(id, user));
    }
}
```

### 5.7 Adapter Layer - JPA Repository

```java
// adapter/outbound/persistence/JpaProductRepository.java
package com.example.product.adapter.outbound.persistence;

import com.example.product.adapter.outbound.persistence.entity.*;
import com.example.product.adapter.outbound.persistence.mapper.ProductMapper;
import com.example.product.domain.model.aggregate.Product;
import com.example.product.domain.model.valueobject.*;
import com.example.product.domain.repository.ProductRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public class JpaProductRepository implements ProductRepository {
    
    private final SpringDataProductRepository jpaRepo;
    private final ProductMapper mapper;
    
    public JpaProductRepository(SpringDataProductRepository jpaRepo,
                                 ProductMapper mapper) {
        this.jpaRepo = jpaRepo;
        this.mapper = mapper;
    }
    
    @Override
    public Optional<Product> findById(ProductId id) {
        return jpaRepo.findById(id.value()).map(mapper::toDomain);
    }
    
    @Override
    public Product save(Product product) {
        ProductJpaEntity entity = mapper.toEntity(product);
        ProductJpaEntity saved = jpaRepo.save(entity);
        return mapper.toDomain(saved);
    }
    
    @Override
    public boolean existsByProductCode(ProductCode code) {
        return jpaRepo.existsByProductCode(code.value());
    }
    
    @Override
    public void delete(ProductId id) {
        jpaRepo.deleteById(id.value());
    }
}
```

---

## 6. 安全架構

### 6.1 安全架構總覽

```
┌─────────────────────────────────────────────────────────────────────────────────┐
│                           Security Architecture                                  │
├─────────────────────────────────────────────────────────────────────────────────┤
│                                                                                  │
│  ┌────────────────────────────────────────────────────────────────────────────┐ │
│  │                    NORTH-SOUTH (外部 → 內部)                               │ │
│  │                                                                             │ │
│  │   Client ──► Gateway ──► OAuth2 Resource Server ──► @PreAuthorize ──► API  │ │
│  │              (CORS)       (JWT Validation)          (Role Check)            │ │
│  │                                                                             │ │
│  │   Token Flow:                                                               │ │
│  │   1. Client 向 Keycloak 取得 JWT Token                                      │ │
│  │   2. Client 在 Authorization Header 附帶 Bearer Token                       │ │
│  │   3. Resource Server 驗證 Token 簽章與有效期                                │ │
│  │   4. 從 Token 中提取 Realm Roles → Spring Security Authorities              │ │
│  │   5. @PreAuthorize 檢查角色權限                                             │ │
│  └────────────────────────────────────────────────────────────────────────────┘ │
│                                                                                  │
│  ┌────────────────────────────────────────────────────────────────────────────┐ │
│  │                    EAST-WEST (服務 → 服務)                                 │ │
│  │                                                                             │ │
│  │   Service A ──► ServiceTokenProvider ──► OAuth2 Client Credentials ──►     │ │
│  │                 (Get Token)              (Token Endpoint)                   │ │
│  │                                                                             │ │
│  │              ──► ServiceAuthInterceptor ──► Service B                       │ │
│  │                  (Add Bearer Token)                                         │ │
│  │                                                                             │ │
│  │   Token Caching:                                                            │ │
│  │   • 自動快取 Token，過期前 60 秒自動刷新                                     │ │
│  │   • Thread-safe 設計，支援高併發                                            │ │
│  └────────────────────────────────────────────────────────────────────────────┘ │
│                                                                                  │
└─────────────────────────────────────────────────────────────────────────────────┘
```

### 6.2 安全配置

#### SecurityProperties 配置

```yaml
# application.yml
audit:
  security:
    enabled: true
    issuer-uri: http://localhost:8180/realms/ecommerce
    # jwk-set-uri: http://localhost:8180/realms/ecommerce/protocol/openid-connect/certs

    # CORS 配置
    cors:
      enabled: true
      allowed-origins:
        - http://localhost:3000
        - http://localhost:8080
      allowed-methods:
        - GET
        - POST
        - PUT
        - DELETE
      allowed-headers:
        - Authorization
        - Content-Type
        - X-Correlation-ID
      allow-credentials: true
      max-age: 3600

    # 公開端點（不需認證）
    public-paths:
      - /actuator/health
      - /actuator/health/**
      - /actuator/info

    # 可存取稽核日誌的角色
    audit-roles:
      - ADMIN
      - AUDITOR

# 服務間認證 (East-West)
  service-auth:
    enabled: true
    client-id: my-service-client
    client-secret: ${SERVICE_CLIENT_SECRET}
```

#### SecurityAutoConfiguration

```java
// 自動配置 OAuth2 Resource Server
@AutoConfiguration
@ConditionalOnProperty(name = "audit.security.enabled", havingValue = "true")
public class SecurityAutoConfiguration {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .sessionManagement(session ->
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/actuator/**").permitAll()
                .requestMatchers("/api/v1/audit-logs/**").authenticated()
                .anyRequest().authenticated()
            )
            .oauth2ResourceServer(oauth2 -> oauth2
                .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter()))
            );
        return http.build();
    }

    @Bean
    public JwtDecoder jwtDecoder() {
        return JwtDecoders.fromIssuerLocation(issuerUri);
    }
}
```

#### Keycloak Role Mapping

```java
// 從 Keycloak JWT 提取角色
class KeycloakRealmRoleConverter implements Converter<Jwt, Collection<GrantedAuthority>> {

    @Override
    public Collection<GrantedAuthority> convert(Jwt jwt) {
        // JWT 結構:
        // {
        //   "realm_access": { "roles": ["ADMIN", "USER"] },
        //   "resource_access": { "my-client": { "roles": ["manage"] } }
        // }

        Map<String, Object> realmAccess = jwt.getClaimAsMap("realm_access");
        List<String> roles = (List<String>) realmAccess.get("roles");

        return roles.stream()
            .map(role -> new SimpleGrantedAuthority("ROLE_" + role))
            .collect(toList());
    }
}
```

### 6.3 端點授權

```java
// AuditQueryController.java
@RestController
@RequestMapping("/api/v1/audit-logs")
@PreAuthorize("hasAnyRole('ADMIN', 'AUDITOR') or hasAuthority('SCOPE_audit:read')")
public class AuditQueryController {

    @GetMapping
    public ResponseEntity<PagedResponse<AuditLogView>> queryAuditLogs(...) {
        // 需要 ADMIN、AUDITOR 角色，或 audit:read scope
    }

    @GetMapping("/{id}")
    public ResponseEntity<AuditLogView> getAuditLogById(@PathVariable UUID id) {
        // 同上
    }
}
```

### 6.4 服務間認證 (East-West)

```java
// 使用預配置的 RestTemplate
@Service
public class ProductService {

    private final RestTemplate serviceRestTemplate; // 自動注入

    public ProductService(RestTemplate serviceRestTemplate) {
        this.serviceRestTemplate = serviceRestTemplate;
    }

    public Product getProduct(UUID id) {
        // 請求自動附帶 Bearer Token
        return serviceRestTemplate.getForObject(
            "http://product-service/api/products/" + id,
            Product.class
        );
    }
}

// 或手動使用 ServiceTokenProvider
@Service
public class ManualAuthService {

    private final ServiceTokenProvider tokenProvider;
    private final RestTemplate restTemplate;

    public void callService() {
        String token = tokenProvider.getToken()
            .orElseThrow(() -> new SecurityException("No token"));

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);

        restTemplate.exchange(url, GET, new HttpEntity<>(headers), Response.class);
    }
}
```

---

## 7. 架構測試 (ArchUnit)

```java
// test/architecture/ArchitectureTest.java
package com.example.product.architecture;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.*;
import static com.tngtech.archunit.library.Architectures.layeredArchitecture;

class ArchitectureTest {
    
    private static JavaClasses classes;
    
    @BeforeAll
    static void setup() {
        classes = new ClassFileImporter()
            .importPackages("com.example.product");
    }
    
    @Test
    void domainShouldNotDependOnOuterLayers() {
        noClasses()
            .that().resideInAPackage("..domain..")
            .should().dependOnClassesThat()
            .resideInAnyPackage(
                "..application..",
                "..adapter..",
                "org.springframework..",
                "jakarta.persistence.."
            )
            .check(classes);
    }
    
    @Test
    void applicationShouldNotDependOnAdapter() {
        noClasses()
            .that().resideInAPackage("..application..")
            .should().dependOnClassesThat()
            .resideInAPackage("..adapter..")
            .check(classes);
    }
    
    @Test
    void layeredArchitectureIsRespected() {
        layeredArchitecture()
            .consideringAllDependencies()
            .layer("Domain").definedBy("..domain..")
            .layer("Application").definedBy("..application..")
            .layer("Adapter").definedBy("..adapter..")
            .whereLayer("Domain").mayNotAccessAnyLayer()
            .whereLayer("Application").mayOnlyAccessLayers("Domain")
            .whereLayer("Adapter").mayOnlyAccessLayers("Domain", "Application")
            .check(classes);
    }
    
    @Test
    void valueObjectsShouldBeFinal() {
        classes()
            .that().resideInAPackage("..domain.model.valueobject..")
            .should().haveModifier(JavaModifier.FINAL)
            .check(classes);
    }
}
```

### 6.2 契約測試 (Spring Cloud Contract)

採用 Consumer-Driven Contract (CDC) 模式，確保 API 相容性：

```
┌─────────────────────────────────────────────────────────────────────────────────┐
│                     Spring Cloud Contract Workflow                               │
├─────────────────────────────────────────────────────────────────────────────────┤
│                                                                                  │
│  Provider (audit-lib)                    Consumer (微服務)                       │
│  ══════════════════                      ═══════════════                         │
│                                                                                  │
│  1. 定義契約 (Groovy DSL)                                                        │
│     src/test/resources/contracts/                                                │
│     └── auditquery/                                                              │
│         ├── shouldReturnAuditLogById.groovy                                      │
│         ├── shouldReturnAuditLogsByUsername.groovy                               │
│         └── ...                                                                  │
│                                                                                  │
│  2. 執行 contractTest                    3. 使用 Stubs JAR                       │
│     ./gradlew contractTest                  testImplementation                   │
│         │                                   'com.example:audit-lib:stubs'        │
│         ▼                                       │                                │
│     ┌─────────────────┐                         ▼                                │
│     │ 自動產生測試    │                   ┌─────────────────┐                    │
│     │ AuditqueryTest  │                   │ WireMock Stub   │                    │
│     │ (6 tests)       │                   │ Server          │                    │
│     └─────────────────┘                   └─────────────────┘                    │
│                                                                                  │
│  4. 發布 Stubs JAR                                                               │
│     ./gradlew publishStubsPublicationToMavenLocal                                │
│                                                                                  │
└─────────────────────────────────────────────────────────────────────────────────┘
```

#### 契約定義範例

```groovy
// src/test/resources/contracts/auditquery/shouldReturnAuditLogById.groovy
Contract.make {
    name "should return audit log by ID"
    description "Returns a single audit log entry by its unique ID"

    request {
        method GET()
        url "/api/v1/audit-logs/550e8400-e29b-41d4-a716-446655440000"
        headers {
            contentType applicationJson()
        }
    }

    response {
        status OK()
        headers {
            contentType applicationJson()
        }
        body([
            id: "550e8400-e29b-41d4-a716-446655440000",
            eventType: "PRODUCT_CREATED",
            aggregateType: "Product",
            username: "admin@example.com",
            result: "SUCCESS"
        ])
        bodyMatchers {
            jsonPath('$.id', byRegex('[a-f0-9-]{36}'))
            jsonPath('$.timestamp', byRegex('[0-9]{4}-[0-9]{2}-[0-9]{2}T.*'))
        }
    }
}
```

#### BaseContractTest

```java
// src/test/java/com/example/audit/contract/BaseContractTest.java
public abstract class BaseContractTest {

    private AuditQueryService queryService;

    @BeforeEach
    public void setup() {
        queryService = mock(AuditQueryService.class);
        setupMockResponses();

        // 配置 Jackson ObjectMapper
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        AuditQueryController controller = new AuditQueryController(queryService);
        RestAssuredMockMvc.standaloneSetup(
            MockMvcBuilders.standaloneSetup(controller)
                .setMessageConverters(new MappingJackson2HttpMessageConverter(objectMapper))
        );
    }

    private void setupMockResponses() {
        // Mock 回傳值設定...
    }
}
```

#### Gradle 配置

```groovy
// libs/audit-lib/build.gradle
plugins {
    id 'org.springframework.cloud.contract' version '4.1.4'
    id 'maven-publish'
}

dependencyManagement {
    imports {
        mavenBom "org.springframework.cloud:spring-cloud-dependencies:2023.0.3"
    }
}

dependencies {
    testImplementation 'org.springframework.cloud:spring-cloud-starter-contract-verifier'
    testImplementation 'org.springframework.cloud:spring-cloud-contract-wiremock'
}

contracts {
    testFramework = TestFramework.JUNIT5
    baseClassForTests = 'com.example.audit.contract.BaseContractTest'
    contractsDslDir = file("src/test/resources/contracts")
}

publishing {
    publications {
        stubs(MavenPublication) {
            artifact verifierStubsJar
            artifactId = "${project.name}"
            version = "${project.version}-stubs"
        }
    }
}
```

---

## 7. Gradle 配置

### 7.1 Root build.gradle

```groovy
// build.gradle (Root)
plugins {
    id 'java'
    id 'org.springframework.boot' version '3.3.5' apply false
    id 'io.spring.dependency-management' version '1.1.6' apply false
}

allprojects {
    group = 'com.example'
    version = '1.0.0-SNAPSHOT'
    repositories { mavenCentral() }
}

subprojects {
    apply plugin: 'java'
    
    java {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    
    ext {
        springCloudVersion = '2023.0.3'
        springDocVersion = '2.5.0'
    }
}

configure(subprojects.findAll { it.path.startsWith(':services') }) {
    apply plugin: 'org.springframework.boot'
    apply plugin: 'io.spring.dependency-management'
    
    dependencyManagement {
        imports {
            mavenBom "org.springframework.cloud:spring-cloud-dependencies:${springCloudVersion}"
        }
    }
}
```

### 7.2 Product Service build.gradle

```groovy
// services/product-service/build.gradle
dependencies {
    // Spring Boot
    implementation 'org.springframework.boot:spring-boot-starter-web'
    implementation 'org.springframework.boot:spring-boot-starter-data-jpa'
    implementation 'org.springframework.boot:spring-boot-starter-validation'
    implementation 'org.springframework.boot:spring-boot-starter-oauth2-resource-server'
    implementation 'org.springframework.boot:spring-boot-starter-actuator'
    
    // API Documentation
    implementation "org.springdoc:springdoc-openapi-starter-webmvc-ui:${springDocVersion}"
    
    // Shared Libraries
    implementation project(':libs:common-lib')
    implementation project(':libs:audit-lib')
    implementation project(':libs:security-lib')
    
    // Database
    runtimeOnly 'com.h2database:h2'
    runtimeOnly 'org.postgresql:postgresql'
    
    // Testing
    testImplementation 'org.springframework.boot:spring-boot-starter-test'
    testImplementation 'org.springframework.security:spring-security-test'
    testImplementation 'com.tngtech.archunit:archunit-junit5:1.2.1'
}
```

---

## 8. CQRS 資料流

```
┌─────────────────────────────────────────────────────────────────────────────────┐
│                              CQRS Data Flow                                      │
├─────────────────────────────────────────────────────────────────────────────────┤
│                                                                                  │
│  COMMAND FLOW (Write):                                                          │
│  ════════════════════                                                           │
│                                                                                  │
│  HTTP Request                                                                   │
│       │                                                                         │
│       ▼                                                                         │
│  ┌─────────────────────┐                                                       │
│  │ CommandController   │  ← Adapter (Inbound)                                  │
│  │ • Validate Request  │                                                       │
│  │ • Create Command    │                                                       │
│  └──────────┬──────────┘                                                       │
│             │                                                                   │
│             ▼                                                                   │
│  ┌─────────────────────┐                                                       │
│  │ CommandService      │  ← Application                                        │
│  │ • Load Aggregate    │                                                       │
│  │ • Execute Command   │                                                       │
│  │ • Save Aggregate    │                                                       │
│  │ • Publish Events    │                                                       │
│  └──────────┬──────────┘                                                       │
│             │                                                                   │
│             ▼                                                                   │
│  ┌─────────────────────┐                                                       │
│  │ Aggregate           │  ← Domain                                             │
│  │ • Validate Rules    │                                                       │
│  │ • Update State      │                                                       │
│  │ • Register Events   │                                                       │
│  └──────────┬──────────┘                                                       │
│             │                                                                   │
│             ▼                                                                   │
│  ┌─────────────────────┐                                                       │
│  │ JpaRepository       │  ← Adapter (Outbound)                                 │
│  │ • Map to Entity     │                                                       │
│  │ • Persist           │                                                       │
│  └─────────────────────┘                                                       │
│                                                                                  │
│                                                                                  │
│  QUERY FLOW (Read):                                                             │
│  ══════════════════                                                             │
│                                                                                  │
│  HTTP Request                                                                   │
│       │                                                                         │
│       ▼                                                                         │
│  ┌─────────────────────┐                                                       │
│  │ QueryController     │  ← Adapter (Inbound)                                  │
│  │ • Create Query      │                                                       │
│  └──────────┬──────────┘                                                       │
│             │                                                                   │
│             ▼                                                                   │
│  ┌─────────────────────┐                                                       │
│  │ QueryService        │  ← Application                                        │
│  │ • Execute Query     │     (不經過 Domain)                                   │
│  └──────────┬──────────┘                                                       │
│             │                                                                   │
│             ▼                                                                   │
│  ┌─────────────────────┐                                                       │
│  │ ReadRepository      │  ← Adapter (Outbound)                                 │
│  │ • Query Database    │                                                       │
│  │ • Return View/DTO   │                                                       │
│  └─────────────────────┘                                                       │
│                                                                                  │
└─────────────────────────────────────────────────────────────────────────────────┘
```

---

## 9. 共用稽核函式庫 (audit-lib)

### 9.1 架構設計

稽核函式庫採用 AOP（Aspect-Oriented Programming）機制，與業務邏輯完全分離：

```
┌─────────────────────────────────────────────────────────────────────────────────┐
│                          Audit Library Architecture                              │
├─────────────────────────────────────────────────────────────────────────────────┤
│                                                                                  │
│  ┌─────────────────────────────────────────────────────────────────────────┐   │
│  │                        Microservice (user-service, product-service)      │   │
│  │                                                                          │   │
│  │   ┌───────────────────┐      ┌───────────────────┐                      │   │
│  │   │  Business Logic   │      │  @Auditable       │                      │   │
│  │   │  (Application     │ ────▶│  Annotation       │                      │   │
│  │   │   Service)        │      │                   │                      │   │
│  │   └───────────────────┘      └─────────┬─────────┘                      │   │
│  │                                         │                                │   │
│  └─────────────────────────────────────────┼────────────────────────────────┘   │
│                                            │                                     │
│  ┌─────────────────────────────────────────┼────────────────────────────────┐   │
│  │                          audit-lib (Shared Library)                       │   │
│  │                                         │                                │   │
│  │                                         ▼                                │   │
│  │   ┌─────────────────────────────────────────────────────────────────┐   │   │
│  │   │                      AuditAspect (AOP)                          │   │   │
│  │   │  • @Around("@annotation(Auditable)")                            │   │   │
│  │   │  • Extract method context                                       │   │   │
│  │   │  • Build AuditLog entity                                        │   │   │
│  │   │  • Handle success/failure                                       │   │   │
│  │   └────────────────────────────────┬────────────────────────────────┘   │   │
│  │                                    │                                    │   │
│  │                 ┌──────────────────┴──────────────────┐                 │   │
│  │                 │                                     │                 │   │
│  │                 ▼                                     ▼                 │   │
│  │   ┌─────────────────────────────┐   ┌─────────────────────────────┐   │   │
│  │   │    PayloadProcessor        │   │    AuditLogRepository       │   │   │
│  │   │  • Serialize payload       │   │    (Output Port)            │   │   │
│  │   │  • Truncate if > 64KB      │   │  • save(AuditLog)           │   │   │
│  │   │  • Mask sensitive fields   │   │  • Append-only semantics    │   │   │
│  │   │  • Handle circular refs    │   └──────────────┬──────────────┘   │   │
│  │   └─────────────────────────────┘                  │                  │   │
│  │                                                    │                  │   │
│  │   ┌─────────────────────────────┐                  │                  │   │
│  │   │    AuditMetrics             │                  │                  │   │
│  │   │  • audit.events.total       │                  │                  │   │
│  │   │  • audit.events.failed      │                  │                  │   │
│  │   │  • audit.capture.latency    │                  │                  │   │
│  │   │  • audit.queue.depth        │                  │                  │   │
│  │   └─────────────────────────────┘                  │                  │   │
│  │                                                    │                  │   │
│  └────────────────────────────────────────────────────┼──────────────────┘   │
│                                                       │                       │
│  ┌────────────────────────────────────────────────────┼──────────────────┐   │
│  │                     Infrastructure (Adapter)        │                  │   │
│  │                                                     ▼                  │   │
│  │   ┌─────────────────────────────────────────────────────────────────┐│   │
│  │   │  JpaAuditLogRepository (Append-Only Implementation)             ││   │
│  │   │  • INSERT only (no UPDATE/DELETE methods exposed)               ││   │
│  │   │  • Database constraint: no update/delete triggers               ││   │
│  │   └─────────────────────────────────────────────────────────────────┘│   │
│  │                                                                       │   │
│  └───────────────────────────────────────────────────────────────────────┘   │
│                                                                                  │
└─────────────────────────────────────────────────────────────────────────────────┘
```

### 9.2 核心元件

#### 9.2.1 @Auditable 註解

```java
// libs/audit-lib/src/main/java/com/example/audit/annotation/Auditable.java
package com.example.audit.annotation;

import java.lang.annotation.*;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Auditable {

    /** 稽核事件類型 (e.g., PRODUCT_CREATED) */
    String eventType();

    /** 資源類型 (e.g., Product, User) */
    String resourceType();

    /** 需遮蔽的欄位名稱 */
    String[] maskFields() default {};

    /** 自訂 Payload 擷取器（SpEL 表達式） */
    String payloadExpression() default "";
}
```

#### 9.2.2 AuditAspect

```java
// libs/audit-lib/src/main/java/com/example/audit/aspect/AuditAspect.java
package com.example.audit.aspect;

@Aspect
@Component
@Order(Ordered.LOWEST_PRECEDENCE - 10)  // 在交易提交後執行
public class AuditAspect {

    private final AuditLogRepository repository;
    private final PayloadProcessor payloadProcessor;
    private final AuditMetrics metrics;
    private final AuditContextHolder contextHolder;

    @Around("@annotation(auditable)")
    public Object audit(ProceedingJoinPoint pjp, Auditable auditable) throws Throwable {
        long startTime = System.currentTimeMillis();
        Object result = null;
        Throwable error = null;

        try {
            result = pjp.proceed();
            return result;
        } catch (Throwable t) {
            error = t;
            throw t;
        } finally {
            try {
                captureAuditLog(pjp, auditable, result, error, startTime);
            } catch (Exception e) {
                // 稽核失敗不影響業務操作
                metrics.incrementFailed();
                log.error("Audit capture failed", e);
            }
        }
    }

    private void captureAuditLog(ProceedingJoinPoint pjp, Auditable auditable,
                                  Object result, Throwable error, long startTime) {

        AuditLog log = AuditLog.builder()
            .id(UUID.randomUUID())
            .timestamp(Instant.now())
            .eventType(auditable.eventType())
            .aggregateType(auditable.resourceType())
            .aggregateId(extractAggregateId(pjp, result))
            .username(contextHolder.getCurrentUsername().orElse("ANONYMOUS"))
            .serviceName(contextHolder.getServiceName())
            .action(pjp.getSignature().getName())
            .payload(payloadProcessor.process(pjp.getArgs(), auditable.maskFields()))
            .result(error == null ? "SUCCESS" : "FAILURE")
            .errorMessage(error != null ? error.getMessage() : null)
            .clientIp(contextHolder.getClientIp().orElse("unknown"))
            .correlationId(contextHolder.getCorrelationId().orElse(null))  // 從 MDC 擷取
            .build();

        repository.save(log);

        long latency = System.currentTimeMillis() - startTime;
        metrics.recordLatency(latency);
        metrics.incrementTotal();
    }
}
```

#### 9.2.3 AuditContextHolder

```java
// libs/audit-lib/src/main/java/com/example/audit/context/AuditContextHolder.java
package com.example.audit.context;

import org.slf4j.MDC;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import java.util.Optional;

/**
 * 稽核上下文持有者 - 從各種來源擷取稽核所需資訊
 */
@Component
public class AuditContextHolder {

    private final AuditProperties properties;

    /** 從 SecurityContext 取得當前使用者名稱 */
    public Optional<String> getCurrentUsername() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated()) {
            return Optional.of(auth.getName());
        }
        return Optional.of("ANONYMOUS");
    }

    /** 從 RequestContext 取得 Client IP */
    public Optional<String> getClientIp() {
        var attrs = RequestContextHolder.getRequestAttributes();
        if (attrs instanceof ServletRequestAttributes servletAttrs) {
            var request = servletAttrs.getRequest();
            String ip = request.getHeader("X-Forwarded-For");
            return Optional.ofNullable(ip != null ? ip.split(",")[0].trim()
                                                  : request.getRemoteAddr());
        }
        return Optional.empty();
    }

    /**
     * 從 MDC 取得 Correlation ID
     * 支援多種常見 key: correlationId, traceId, X-Correlation-ID
     */
    public Optional<String> getCorrelationId() {
        String correlationId = MDC.get("correlationId");
        if (correlationId == null) {
            correlationId = MDC.get("traceId");
        }
        if (correlationId == null) {
            correlationId = MDC.get("X-Correlation-ID");
        }
        return Optional.ofNullable(correlationId);
    }

    public String getServiceName() {
        return properties.getServiceName();
    }
}
```

#### 9.2.4 PayloadProcessor

```java
// libs/audit-lib/src/main/java/com/example/audit/processor/PayloadProcessor.java
package com.example.audit.processor;

@Component
public class PayloadProcessor {

    private static final int MAX_PAYLOAD_SIZE = 64 * 1024;  // 64 KB
    private final ObjectMapper objectMapper;
    private final List<FieldMasker> maskers;

    public String process(Object[] args, String[] maskFields) {
        try {
            Map<String, Object> payload = buildPayloadMap(args);

            // 遮蔽敏感欄位
            for (String field : maskFields) {
                maskField(payload, field);
            }

            String json = objectMapper.writeValueAsString(payload);

            // 截斷超大 Payload
            if (json.length() > MAX_PAYLOAD_SIZE) {
                return truncatePayload(json, payload);
            }

            return json;
        } catch (Exception e) {
            return "{\"_error\": \"payload serialization failed\"}";
        }
    }

    private void maskField(Map<String, Object> payload, String fieldPath) {
        // 支援巢狀路徑：user.password, payment.card.number
        String[] parts = fieldPath.split("\\.");
        maskFieldRecursive(payload, parts, 0);
    }

    private String truncatePayload(String json, Map<String, Object> original) {
        Map<String, Object> truncated = new LinkedHashMap<>();
        truncated.put("_truncated", true);
        truncated.put("_originalSize", json.length());
        truncated.put("_summary", extractSummary(original));
        return objectMapper.writeValueAsString(truncated);
    }
}
```

### 9.3 函式庫整合

#### 9.3.1 Gradle 依賴

```groovy
// services/product-service/build.gradle
dependencies {
    implementation project(':libs:audit-lib')
}

// libs/audit-lib/build.gradle
dependencies {
    implementation 'org.springframework.boot:spring-boot-starter-aop'
    implementation 'org.springframework.boot:spring-boot-starter-data-jpa'
    implementation 'org.springframework.boot:spring-boot-starter-actuator'
    implementation 'io.micrometer:micrometer-core'

    compileOnly 'org.projectlombok:lombok'
    annotationProcessor 'org.projectlombok:lombok'
}
```

#### 9.3.2 AuditProperties（動態設定）

```java
// libs/audit-lib/src/main/java/com/example/audit/config/AuditProperties.java
package com.example.audit.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import java.util.List;

/**
 * 稽核設定屬性 - 支援動態重載
 *
 * 使用 @RefreshScope 搭配 @ConfigurationProperties
 * 透過 /actuator/refresh 端點或 Spring Cloud Config 觸發重載
 */
@RefreshScope
@ConfigurationProperties(prefix = "audit")
public class AuditProperties {

    /** 是否啟用稽核 */
    private boolean enabled = true;

    /** 服務名稱（用於識別稽核來源） */
    private String serviceName;

    /** Payload 設定 */
    private Payload payload = new Payload();

    /** 遮蔽設定 */
    private Masking masking = new Masking();

    public static class Payload {
        /** Payload 最大大小（bytes），預設 64 KB */
        private int maxSize = 65536;
        // getters/setters
    }

    public static class Masking {
        /** 預設遮蔽欄位清單 */
        private List<String> defaultFields = List.of("password", "secret", "token");
        // getters/setters
    }

    // getters/setters
}
```

#### 9.3.3 自動配置

```java
// libs/audit-lib/src/main/java/com/example/audit/config/AuditAutoConfiguration.java
package com.example.audit.config;

@Configuration
@EnableAspectJAutoProxy
@EnableConfigurationProperties(AuditProperties.class)
@ConditionalOnProperty(name = "audit.enabled", havingValue = "true", matchIfMissing = true)
public class AuditAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public AuditAspect auditAspect(AuditLogRepository repository,
                                    PayloadProcessor processor,
                                    AuditMetrics metrics,
                                    AuditContextHolder contextHolder) {
        return new AuditAspect(repository, processor, metrics, contextHolder);
    }

    @Bean
    @ConditionalOnMissingBean
    public AuditContextHolder auditContextHolder(AuditProperties properties) {
        return new AuditContextHolder(properties);
    }

    @Bean
    @ConditionalOnMissingBean
    public AuditMetrics auditMetrics(MeterRegistry registry) {
        return new AuditMetrics(registry);
    }

    @Bean
    public HealthIndicator auditHealthIndicator(AuditLogRepository repository) {
        return new AuditHealthIndicator(repository);
    }
}
```

#### 9.3.4 應用程式配置

```yaml
# application.yml
audit:
  enabled: true
  service-name: ${spring.application.name}
  payload:
    max-size: 65536  # 64 KB
  async:
    enabled: true
    queue-capacity: 1000
  masking:
    default-fields:
      - password
      - secret
      - token
```

### 9.4 效能設計

| 設計決策 | 說明 |
|----------|------|
| **非同步處理** | 稽核日誌寫入採用非同步佇列，不阻塞業務執行緒 |
| **批次寫入** | 支援批次寫入以提高吞吐量（可配置） |
| **故障隔離** | 稽核失敗只記錄錯誤日誌，不拋出異常 |
| **連線池** | 使用獨立的資料庫連線池，避免影響業務查詢 |
| **指標暴露** | 透過 Micrometer 暴露監控指標至 Prometheus |

### 9.5 專案結構

```
libs/audit-lib/
├── build.gradle
└── src/main/java/com/example/audit/
    ├── annotation/
    │   └── Auditable.java
    ├── aspect/
    │   └── AuditAspect.java
    ├── config/
    │   ├── AuditAutoConfiguration.java
    │   └── AuditProperties.java           # @RefreshScope 支援動態重載
    ├── context/
    │   └── AuditContextHolder.java        # 從 SecurityContext/MDC 擷取上下文
    ├── domain/
    │   └── AuditLog.java
    ├── metrics/
    │   └── AuditMetrics.java
    ├── processor/
    │   ├── PayloadProcessor.java
    │   └── FieldMasker.java
    ├── repository/
    │   └── AuditLogRepository.java        # Output Port (Interface)
    └── health/
        └── AuditHealthIndicator.java
```

---

## 10. 附錄

### 10.1 相關文件

- [PRD.md](./PRD.md) - 業務需求與領域建模
- [INFRA.md](./INFRA.md) - 基礎設施與部署

### 10.2 參考資料

- [Hexagonal Architecture by Alistair Cockburn](https://alistair.cockburn.us/hexagonal-architecture/)
- [Domain-Driven Design by Eric Evans](https://www.domainlanguage.com/ddd/)
- [CQRS by Martin Fowler](https://martinfowler.com/bliki/CQRS.html)
# TECH: 電子商務多租戶平台 - 技術架構文件

## 文件資訊

| 項目 | 內容 |
|------|------|
| 文件版本 | 1.0 |
| 建立日期 | 2026-01-10 |
| 專案代號 | ECOMMERCE-MULTITENANT-POC |
| 適用範圍 | Application Architecture |

---

## 1. 技術棧總覽

### 1.1 核心技術

| Category | Technology | Version |
|----------|------------|---------|
| Language | Java | 21 (LTS) |
| Framework | Spring Boot | 3.3.x |
| Build Tool | Gradle (Kotlin DSL) | 8.5+ |
| Gateway | Spring Cloud Gateway | 4.1.x |
| Security | Spring Security + OAuth2 | 6.3.x |
| Identity | Keycloak | 24.x |
| API Doc | SpringDoc OpenAPI | 2.5.x |
| Database | PostgreSQL | 16.x |
| ORM | Spring Data JPA | 3.3.x |
| Testing | JUnit 5 + Cucumber | 7.x |
| Container | Docker | 24.x |

### 1.2 模組依賴關係

```
┌─────────────────────────────────────────────────────────────────────┐
│                         gateway-service                             │
│                    (Spring Cloud Gateway)                           │
└─────────────────────────────┬───────────────────────────────────────┘
                              │
            ┌─────────────────┼─────────────────┐
            │                 │                 │
            ▼                 ▼                 ▼
┌───────────────────┐ ┌───────────────────┐ ┌───────────────────┐
│  product-service  │ │   user-service    │ │  (future svc)     │
└─────────┬─────────┘ └─────────┬─────────┘ └───────────────────┘
          │                     │
          └──────────┬──────────┘
                     │
          ┌──────────┼──────────┐
          │          │          │
          ▼          ▼          ▼
┌──────────────┐ ┌──────────────┐ ┌──────────────┐
│  common-lib  │ │ security-lib │ │  tenant-lib  │
└──────────────┘ └──────────────┘ └──────────────┘
```

---

## 2. Gradle Multi-Module 設定

### 2.1 根目錄 settings.gradle.kts

```kotlin
rootProject.name = "ecommerce-multitenant-poc"

// ===== 共用模組 =====
include("libs:common-lib")
include("libs:security-lib")
include("libs:tenant-lib")

// ===== 微服務 =====
include("services:product-service")
include("services:user-service")
include("services:gateway-service")

// ===== 測試模組 =====
include("tests:scenario-tests")

// ===== Dependency Version Catalog =====
dependencyResolutionManagement {
    versionCatalogs {
        create("libs") {
            // Versions
            version("spring-boot", "3.3.0")
            version("spring-cloud", "2023.0.2")
            version("lombok", "1.18.32")
            version("mapstruct", "1.5.5.Final")
            version("springdoc", "2.5.0")
            version("cucumber", "7.18.0")
            version("testcontainers", "1.19.8")
            
            // Spring Boot Starters
            library("spring-boot-starter-web", "org.springframework.boot", "spring-boot-starter-web").versionRef("spring-boot")
            library("spring-boot-starter-data-jpa", "org.springframework.boot", "spring-boot-starter-data-jpa").versionRef("spring-boot")
            library("spring-boot-starter-security", "org.springframework.boot", "spring-boot-starter-security").versionRef("spring-boot")
            library("spring-boot-starter-oauth2-resource-server", "org.springframework.boot", "spring-boot-starter-oauth2-resource-server").versionRef("spring-boot")
            library("spring-boot-starter-validation", "org.springframework.boot", "spring-boot-starter-validation").versionRef("spring-boot")
            library("spring-boot-starter-test", "org.springframework.boot", "spring-boot-starter-test").versionRef("spring-boot")
            
            // Spring Cloud
            library("spring-cloud-starter-gateway", "org.springframework.cloud", "spring-cloud-starter-gateway").versionRef("spring-cloud")
            
            // Database
            library("postgresql", "org.postgresql", "postgresql").version("42.7.3")
            library("h2", "com.h2database", "h2").version("2.2.224")
            
            // Tools
            library("lombok", "org.projectlombok", "lombok").versionRef("lombok")
            library("mapstruct", "org.mapstruct", "mapstruct").versionRef("mapstruct")
            library("mapstruct-processor", "org.mapstruct", "mapstruct-processor").versionRef("mapstruct")
            
            // API Docs
            library("springdoc-openapi-starter", "org.springdoc", "springdoc-openapi-starter-webmvc-ui").versionRef("springdoc")
            
            // Testing
            library("cucumber-java", "io.cucumber", "cucumber-java").versionRef("cucumber")
            library("cucumber-spring", "io.cucumber", "cucumber-spring").versionRef("cucumber")
            library("cucumber-junit-platform", "io.cucumber", "cucumber-junit-platform-engine").versionRef("cucumber")
            library("testcontainers-postgresql", "org.testcontainers", "postgresql").versionRef("testcontainers")
            library("testcontainers-junit", "org.testcontainers", "junit-jupiter").versionRef("testcontainers")
        }
    }
}
```

### 2.2 根目錄 build.gradle.kts

```kotlin
plugins {
    java
    id("org.springframework.boot") version "3.3.0" apply false
    id("io.spring.dependency-management") version "1.1.5" apply false
    id("jacoco")
}

allprojects {
    group = "com.example.ecommerce"
    version = "1.0.0-SNAPSHOT"

    repositories {
        mavenCentral()
    }
}

subprojects {
    apply(plugin = "java")
    apply(plugin = "jacoco")
    
    java {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }

    tasks.withType<JavaCompile> {
        options.encoding = "UTF-8"
        options.compilerArgs.add("-parameters")
    }

    tasks.withType<Test> {
        useJUnitPlatform()
    }
    
    tasks.jacocoTestReport {
        reports {
            xml.required.set(true)
            html.required.set(true)
        }
    }
}

// 根專案測試覆蓋率彙總
tasks.register<JacocoReport>("jacocoRootReport") {
    dependsOn(subprojects.map { it.tasks.named("test") })
    
    additionalSourceDirs.setFrom(subprojects.map { it.sourceSets.main.get().allSource.srcDirs })
    sourceDirectories.setFrom(subprojects.map { it.sourceSets.main.get().allSource.srcDirs })
    classDirectories.setFrom(subprojects.map { it.sourceSets.main.get().output })
    executionData.setFrom(subprojects.mapNotNull { 
        it.tasks.findByName("test")?.let { task ->
            (task as Test).extensions.getByType(JacocoTaskExtension::class).destinationFile
        }
    })
    
    reports {
        xml.required.set(true)
        html.required.set(true)
    }
}
```

### 2.3 gradle.properties

```properties
# Gradle
org.gradle.parallel=true
org.gradle.caching=true
org.gradle.daemon=true
org.gradle.jvmargs=-Xmx2g -XX:+HeapDumpOnOutOfMemoryError

# Project
projectVersion=1.0.0-SNAPSHOT
javaVersion=21
```

---

## 3. 共用模組 (libs/)

### 3.1 common-lib/build.gradle.kts

```kotlin
plugins {
    `java-library`
}

dependencies {
    api(libs.lombok)
    annotationProcessor(libs.lombok)
    
    api("jakarta.validation:jakarta.validation-api:3.0.2")
    api("com.fasterxml.jackson.core:jackson-annotations:2.17.1")
    
    testImplementation(libs.spring.boot.starter.test)
}
```

### 3.2 common-lib 核心類別

**BaseEntity.java**
```java
package com.example.ecommerce.common.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

@Getter
@Setter
@MappedSuperclass
public abstract class BaseEntity {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "tenant_id", nullable = false)
    private String tenantId;
    
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    @Column(name = "created_by")
    private String createdBy;
    
    @Column(name = "updated_by")
    private String updatedBy;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
```

**ApiResponse.java**
```java
package com.example.ecommerce.common.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApiResponse<T> {
    private boolean success;
    private String message;
    private T data;
    private String errorCode;
    
    public static <T> ApiResponse<T> success(T data) {
        return ApiResponse.<T>builder()
                .success(true)
                .data(data)
                .build();
    }
    
    public static <T> ApiResponse<T> error(String message, String errorCode) {
        return ApiResponse.<T>builder()
                .success(false)
                .message(message)
                .errorCode(errorCode)
                .build();
    }
}
```

### 3.3 security-lib/build.gradle.kts

```kotlin
plugins {
    `java-library`
    id("org.springframework.boot")
    id("io.spring.dependency-management")
}

tasks.bootJar { enabled = false }
tasks.jar { enabled = true }

dependencies {
    api(project(":libs:common-lib"))
    api(libs.spring.boot.starter.security)
    api(libs.spring.boot.starter.oauth2.resource.server)
    
    compileOnly(libs.lombok)
    annotationProcessor(libs.lombok)
    
    testImplementation(libs.spring.boot.starter.test)
}
```

**SecurityConfig.java**
```java
package com.example.ecommerce.security.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .sessionManagement(session -> 
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/actuator/**", "/swagger-ui/**", "/v3/api-docs/**").permitAll()
                .requestMatchers("/api/**").authenticated()
                .anyRequest().authenticated())
            .oauth2ResourceServer(oauth2 -> 
                oauth2.jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter())));
        
        return http.build();
    }
    
    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(new KeycloakRoleConverter());
        return converter;
    }
}
```

**KeycloakRoleConverter.java**
```java
package com.example.ecommerce.security.converter;

import org.springframework.core.convert.converter.Converter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.*;
import java.util.stream.Collectors;

public class KeycloakRoleConverter implements Converter<Jwt, Collection<GrantedAuthority>> {

    @Override
    public Collection<GrantedAuthority> convert(Jwt jwt) {
        Map<String, Object> realmAccess = jwt.getClaimAsMap("realm_access");
        
        if (realmAccess == null || realmAccess.isEmpty()) {
            return Collections.emptyList();
        }
        
        @SuppressWarnings("unchecked")
        List<String> roles = (List<String>) realmAccess.get("roles");
        
        return roles.stream()
                .map(role -> new SimpleGrantedAuthority("ROLE_" + role.toUpperCase()))
                .collect(Collectors.toList());
    }
}
```

### 3.4 tenant-lib/build.gradle.kts

```kotlin
plugins {
    `java-library`
    id("org.springframework.boot")
    id("io.spring.dependency-management")
}

tasks.bootJar { enabled = false }
tasks.jar { enabled = true }

dependencies {
    api(project(":libs:common-lib"))
    api(project(":libs:security-lib"))
    api(libs.spring.boot.starter.data.jpa)
    
    compileOnly(libs.lombok)
    annotationProcessor(libs.lombok)
    
    testImplementation(libs.spring.boot.starter.test)
}
```

**TenantContext.java**
```java
package com.example.ecommerce.tenant.context;

public class TenantContext {
    
    private static final ThreadLocal<String> CURRENT_TENANT = new ThreadLocal<>();
    
    public static String getCurrentTenant() {
        return CURRENT_TENANT.get();
    }
    
    public static void setCurrentTenant(String tenantId) {
        CURRENT_TENANT.set(tenantId);
    }
    
    public static void clear() {
        CURRENT_TENANT.remove();
    }
}
```

**TenantFilter.java**
```java
package com.example.ecommerce.tenant.filter;

import com.example.ecommerce.tenant.context.TenantContext;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class TenantFilter implements Filter {

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        try {
            String tenantId = extractTenantFromToken();
            TenantContext.setCurrentTenant(tenantId);
            chain.doFilter(request, response);
        } finally {
            TenantContext.clear();
        }
    }
    
    private String extractTenantFromToken() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        
        if (authentication != null && authentication.getPrincipal() instanceof Jwt jwt) {
            // 從 JWT claims 中提取 tenant_id
            String tenantId = jwt.getClaimAsString("tenant_id");
            if (tenantId != null) {
                return tenantId;
            }
            
            // 系統管理者可以存取所有租戶
            if (hasRole(jwt, "ADMIN")) {
                return "system";
            }
        }
        
        return "unknown";
    }
    
    private boolean hasRole(Jwt jwt, String role) {
        Map<String, Object> realmAccess = jwt.getClaimAsMap("realm_access");
        if (realmAccess != null) {
            @SuppressWarnings("unchecked")
            List<String> roles = (List<String>) realmAccess.get("roles");
            return roles != null && roles.contains(role);
        }
        return false;
    }
}
```

**TenantAwareRepository.java**
```java
package com.example.ecommerce.tenant.repository;

import com.example.ecommerce.tenant.context.TenantContext;
import jakarta.persistence.EntityManager;
import org.hibernate.Session;
import org.springframework.data.jpa.repository.support.JpaEntityInformation;
import org.springframework.data.jpa.repository.support.SimpleJpaRepository;

import java.io.Serializable;

public class TenantAwareRepository<T, ID extends Serializable> 
        extends SimpleJpaRepository<T, ID> {

    private final EntityManager entityManager;

    public TenantAwareRepository(JpaEntityInformation<T, ?> entityInformation,
                                  EntityManager entityManager) {
        super(entityInformation, entityManager);
        this.entityManager = entityManager;
    }

    @Override
    protected T getReferenceById(ID id) {
        enableTenantFilter();
        return super.getReferenceById(id);
    }

    private void enableTenantFilter() {
        String currentTenant = TenantContext.getCurrentTenant();
        if (currentTenant != null && !"system".equals(currentTenant)) {
            Session session = entityManager.unwrap(Session.class);
            session.enableFilter("tenantFilter")
                   .setParameter("tenantId", currentTenant);
        }
    }
}
```

---

## 4. 微服務模組 (services/)

### 4.1 product-service/build.gradle.kts

```kotlin
plugins {
    id("org.springframework.boot")
    id("io.spring.dependency-management")
}

dependencies {
    implementation(project(":libs:common-lib"))
    implementation(project(":libs:security-lib"))
    implementation(project(":libs:tenant-lib"))
    
    implementation(libs.spring.boot.starter.web)
    implementation(libs.spring.boot.starter.data.jpa)
    implementation(libs.spring.boot.starter.validation)
    implementation(libs.springdoc.openapi.starter)
    
    runtimeOnly(libs.postgresql)
    runtimeOnly(libs.h2)
    
    compileOnly(libs.lombok)
    annotationProcessor(libs.lombok)
    annotationProcessor(libs.mapstruct.processor)
    
    testImplementation(libs.spring.boot.starter.test)
    testImplementation(libs.testcontainers.postgresql)
    testImplementation(libs.testcontainers.junit)
}
```

### 4.2 Product Domain

**Product.java**
```java
package com.example.ecommerce.product.domain;

import com.example.ecommerce.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Filter;
import org.hibernate.annotations.FilterDef;
import org.hibernate.annotations.ParamDef;

import java.math.BigDecimal;

@Entity
@Table(name = "products")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FilterDef(name = "tenantFilter", parameters = @ParamDef(name = "tenantId", type = String.class))
@Filter(name = "tenantFilter", condition = "tenant_id = :tenantId")
public class Product extends BaseEntity {

    @Column(nullable = false)
    private String name;
    
    @Column(nullable = false)
    private String category;
    
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal price;
    
    @Column(nullable = false)
    private Integer stock;
    
    @Column(length = 1000)
    private String description;
    
    @Column(nullable = false)
    @Builder.Default
    private Boolean active = true;
}
```

**ProductRepository.java**
```java
package com.example.ecommerce.product.repository;

import com.example.ecommerce.product.domain.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {
    
    List<Product> findByTenantIdAndActiveTrue(String tenantId);
    
    Optional<Product> findByIdAndTenantId(Long id, String tenantId);
    
    @Query("SELECT p FROM Product p WHERE p.active = true")
    List<Product> findAllActive();
    
    List<Product> findByCategoryAndTenantId(String category, String tenantId);
}
```

**ProductService.java**
```java
package com.example.ecommerce.product.service;

import com.example.ecommerce.product.domain.Product;
import com.example.ecommerce.product.dto.*;
import com.example.ecommerce.product.repository.ProductRepository;
import com.example.ecommerce.tenant.context.TenantContext;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProductService {

    private final ProductRepository productRepository;
    private final ProductMapper productMapper;

    public List<ProductResponse> findAll() {
        String tenantId = TenantContext.getCurrentTenant();
        
        List<Product> products;
        if ("system".equals(tenantId)) {
            // 系統管理者可以看到所有商品
            products = productRepository.findAllActive();
        } else {
            // 一般使用者只能看到自己租戶的商品
            products = productRepository.findByTenantIdAndActiveTrue(tenantId);
        }
        
        return productMapper.toResponseList(products);
    }

    public ProductResponse findById(Long id) {
        String tenantId = TenantContext.getCurrentTenant();
        
        Product product;
        if ("system".equals(tenantId)) {
            product = productRepository.findById(id)
                    .orElseThrow(() -> new ResourceNotFoundException("Product not found"));
        } else {
            product = productRepository.findByIdAndTenantId(id, tenantId)
                    .orElseThrow(() -> new AccessDeniedException("Access denied to this product"));
        }
        
        return productMapper.toResponse(product);
    }

    @Transactional
    public ProductResponse create(CreateProductRequest request) {
        String tenantId = TenantContext.getCurrentTenant();
        
        Product product = productMapper.toEntity(request);
        product.setTenantId(tenantId);
        
        Product saved = productRepository.save(product);
        return productMapper.toResponse(saved);
    }

    @Transactional
    public ProductResponse update(Long id, UpdateProductRequest request) {
        String tenantId = TenantContext.getCurrentTenant();
        
        Product product = productRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new AccessDeniedException("Access denied to this product"));
        
        productMapper.updateEntity(request, product);
        
        Product saved = productRepository.save(product);
        return productMapper.toResponse(saved);
    }

    @Transactional
    public void delete(Long id) {
        String tenantId = TenantContext.getCurrentTenant();
        
        Product product = productRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new AccessDeniedException("Access denied to this product"));
        
        // 軟刪除
        product.setActive(false);
        productRepository.save(product);
    }
}
```

**ProductController.java**
```java
package com.example.ecommerce.product.controller;

import com.example.ecommerce.common.dto.ApiResponse;
import com.example.ecommerce.product.dto.*;
import com.example.ecommerce.product.service.ProductService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
@Tag(name = "Product", description = "商品管理 API")
public class ProductController {

    private final ProductService productService;

    @GetMapping
    @PreAuthorize("hasAnyRole('USER', 'TENANT_ADMIN', 'ADMIN')")
    @Operation(summary = "查詢商品列表")
    public ResponseEntity<ApiResponse<List<ProductResponse>>> findAll() {
        List<ProductResponse> products = productService.findAll();
        return ResponseEntity.ok(ApiResponse.success(products));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('USER', 'TENANT_ADMIN', 'ADMIN')")
    @Operation(summary = "查詢單一商品")
    public ResponseEntity<ApiResponse<ProductResponse>> findById(@PathVariable Long id) {
        ProductResponse product = productService.findById(id);
        return ResponseEntity.ok(ApiResponse.success(product));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('TENANT_ADMIN', 'ADMIN')")
    @Operation(summary = "新增商品")
    public ResponseEntity<ApiResponse<ProductResponse>> create(
            @Valid @RequestBody CreateProductRequest request) {
        ProductResponse product = productService.create(request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success(product));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('TENANT_ADMIN', 'ADMIN')")
    @Operation(summary = "修改商品")
    public ResponseEntity<ApiResponse<ProductResponse>> update(
            @PathVariable Long id,
            @Valid @RequestBody UpdateProductRequest request) {
        ProductResponse product = productService.update(id, request);
        return ResponseEntity.ok(ApiResponse.success(product));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('TENANT_ADMIN', 'ADMIN')")
    @Operation(summary = "刪除商品")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        productService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
```

### 4.3 user-service/build.gradle.kts

```kotlin
plugins {
    id("org.springframework.boot")
    id("io.spring.dependency-management")
}

dependencies {
    implementation(project(":libs:common-lib"))
    implementation(project(":libs:security-lib"))
    implementation(project(":libs:tenant-lib"))
    
    implementation(libs.spring.boot.starter.web)
    implementation(libs.spring.boot.starter.data.jpa)
    implementation(libs.spring.boot.starter.validation)
    implementation(libs.springdoc.openapi.starter)
    
    // Keycloak Admin Client
    implementation("org.keycloak:keycloak-admin-client:24.0.4")
    
    runtimeOnly(libs.postgresql)
    runtimeOnly(libs.h2)
    
    compileOnly(libs.lombok)
    annotationProcessor(libs.lombok)
    
    testImplementation(libs.spring.boot.starter.test)
}
```

### 4.4 gateway-service/build.gradle.kts

```kotlin
plugins {
    id("org.springframework.boot")
    id("io.spring.dependency-management")
}

dependencies {
    implementation(project(":libs:common-lib"))
    
    implementation("org.springframework.cloud:spring-cloud-starter-gateway")
    implementation(libs.spring.boot.starter.oauth2.resource.server)
    
    testImplementation(libs.spring.boot.starter.test)
}

dependencyManagement {
    imports {
        mavenBom("org.springframework.cloud:spring-cloud-dependencies:2023.0.2")
    }
}
```

**application.yml (gateway-service)**
```yaml
server:
  port: 8080

spring:
  application:
    name: gateway-service
  cloud:
    gateway:
      routes:
        - id: product-service
          uri: lb://product-service
          predicates:
            - Path=/api/products/**
        - id: user-service
          uri: lb://user-service
          predicates:
            - Path=/api/users/**
      default-filters:
        - DedupeResponseHeader=Access-Control-Allow-Origin Access-Control-Allow-Credentials
  security:
    oauth2:
      resourceserver:
        jwt:
          issuer-uri: http://localhost:8180/realms/ecommerce
```

---

## 5. 情境測試模組 (tests/)

### 5.1 scenario-tests/build.gradle.kts

```kotlin
plugins {
    id("org.springframework.boot")
    id("io.spring.dependency-management")
}

dependencies {
    testImplementation(project(":libs:common-lib"))
    
    testImplementation(libs.spring.boot.starter.test)
    testImplementation(libs.spring.boot.starter.web)
    
    // Cucumber
    testImplementation(libs.cucumber.java)
    testImplementation(libs.cucumber.spring)
    testImplementation(libs.cucumber.junit.platform)
    
    // Testcontainers
    testImplementation(libs.testcontainers.postgresql)
    testImplementation(libs.testcontainers.junit)
    testImplementation("org.testcontainers:keycloak:1.19.8")
    
    // REST Assured
    testImplementation("io.rest-assured:rest-assured:5.4.0")
}

tasks.test {
    useJUnitPlatform()
    systemProperty("cucumber.junit-platform.naming-strategy", "long")
}
```

### 5.2 Cucumber 測試架構

**CucumberSpringConfiguration.java**
```java
package com.example.ecommerce.tests.config;

import io.cucumber.spring.CucumberContextConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.KeycloakContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@CucumberContextConfiguration
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
public class CucumberSpringConfiguration {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16")
            .withDatabaseName("ecommerce_test")
            .withUsername("test")
            .withPassword("test");

    @Container
    static KeycloakContainer keycloak = new KeycloakContainer()
            .withRealmImportFile("keycloak/realm-export.json");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.security.oauth2.resourceserver.jwt.issuer-uri",
                () -> keycloak.getAuthServerUrl() + "/realms/ecommerce");
    }
}
```

**ProductSteps.java**
```java
package com.example.ecommerce.tests.steps;

import io.cucumber.java.en.*;
import io.restassured.RestAssured;
import io.restassured.response.Response;
import org.springframework.boot.test.web.server.LocalServerPort;

import static io.restassured.RestAssured.*;
import static org.hamcrest.Matchers.*;

public class ProductSteps {

    @LocalServerPort
    private int port;
    
    private String accessToken;
    private Response response;

    @Given("系統已初始化 {int} 筆預設商品")
    public void initProducts(int count) {
        RestAssured.baseURI = "http://localhost:" + port;
        // 預設商品已透過 SQL 初始化
    }

    @Given("我以 {string} 身份登入")
    public void loginAs(String username) {
        accessToken = getAccessToken(username);
    }

    @When("我發送 GET 請求到 {string}")
    public void sendGetRequest(String path) {
        response = given()
                .header("Authorization", "Bearer " + accessToken)
                .when()
                .get(path);
    }

    @When("我發送 POST 請求到 {string} 包含以下資料:")
    public void sendPostRequest(String path, Map<String, String> data) {
        response = given()
                .header("Authorization", "Bearer " + accessToken)
                .contentType("application/json")
                .body(data)
                .when()
                .post(path);
    }

    @Then("應該回傳 HTTP {int}")
    public void verifyStatusCode(int statusCode) {
        response.then().statusCode(statusCode);
    }

    @Then("回應應包含 {int} 筆商品")
    public void verifyProductCount(int count) {
        response.then()
                .body("data", hasSize(count));
    }

    private String getAccessToken(String username) {
        // 從 Keycloak 取得 access token
        return given()
                .contentType("application/x-www-form-urlencoded")
                .formParam("client_id", "ecommerce-client")
                .formParam("grant_type", "password")
                .formParam("username", username)
                .formParam("password", getPasswordForUser(username))
                .when()
                .post(keycloakTokenUrl)
                .then()
                .extract()
                .path("access_token");
    }
}
```

---

## 6. 驗證命令

### 6.1 Phase 0: Monorepo 初始化驗證

```bash
# 1. 驗證 Gradle 設定
./gradlew projects

# 預期輸出:
# Root project 'ecommerce-multitenant-poc'
# +--- Project ':libs:common-lib'
# +--- Project ':libs:security-lib'
# +--- Project ':libs:tenant-lib'
# +--- Project ':services:product-service'
# +--- Project ':services:user-service'
# +--- Project ':services:gateway-service'
# \--- Project ':tests:scenario-tests'

# 2. 驗證編譯
./gradlew build

# 3. 驗證模組依賴
./gradlew :services:product-service:dependencies --configuration compileClasspath
```

### 6.2 Phase 2: 微服務驗證

```bash
# 1. 啟動服務
./gradlew :services:product-service:bootRun

# 2. 健康檢查
curl http://localhost:8081/actuator/health

# 3. Swagger UI
open http://localhost:8081/swagger-ui.html
```

### 6.3 Phase 3: 測試驗證

```bash
# 1. 執行所有測試
./gradlew test

# 2. 執行情境測試
./gradlew :tests:scenario-tests:test

# 3. 產生測試覆蓋率報告
./gradlew jacocoRootReport
open build/reports/jacoco/jacocoRootReport/html/index.html
```

---

## 7. 實作狀態

### 7.1 分支與稽核機制

本專案採用兩個分支策略，唯一差異在於稽核日誌實作機制：

| 分支 | 稽核機制 | 技術實作 | 適用場景 |
|------|----------|----------|----------|
| `main` | Spring AOP | `@Auditable` + `AuditAspect` | 快速整合、橫切關注點分離 |
| `domain-event-for-audit` | Domain Events | `DomainEvent` + `EventListener` | 細粒度控制、事件驅動架構 |

> **設計原則**: 兩個分支的稽核機制差異是架構層級的不可變決策。

### 7.2 Spring AOP 稽核 (main 分支)

```java
// 使用方式
@Auditable(eventType = AuditEventType.CREATE_PRODUCT)
public UUID handle(CreateProductCommand cmd) {
    // 業務邏輯 - 稽核透過 AOP 自動攔截
}
```

### 7.3 Domain Event 稽核 (domain-event-for-audit 分支)

```java
// 使用方式
public UUID handle(CreateProductCommand cmd) {
    Product product = Product.create(...);
    eventPublisher.publish(product.pullDomainEvents());
    // ProductCreated 事件由 AuditDomainEventListener 捕獲
}
```

### 7.4 測試覆蓋率

| 模組 | 指令覆蓋率 | 分支覆蓋率 | 狀態 |
|------|------------|------------|------|
| product-service | 96% | 75% | ✅ |
| user-service | 96% | N/A | ✅ |
| gateway-service | 92% | N/A | ✅ |
| audit-lib | 67% | N/A | ⚠️ |
| 整體 | 80%+ | - | ✅ 達標 |

### 7.5 功能完成度

| 功能 | 狀態 | 說明 |
|------|------|------|
| Hexagonal Architecture | ✅ | 所有服務採用 Ports & Adapters |
| DDD Domain Model | ✅ | Aggregates, Value Objects, Domain Events |
| CQRS Pattern | ✅ | 分離 Command/Query Services |
| Multi-tenant | ✅ | TenantContext + JWT tenant_id claim |
| RBAC | ✅ | @PreAuthorize + Keycloak Roles |
| OAuth2/OIDC | ✅ | Keycloak Integration |
| LDAP Federation | ✅ | OpenLDAP + Keycloak User Federation |
| Audit Logging | ✅ | 雙機制 (AOP / Domain Events) |
| API Gateway | ✅ | Spring Cloud Gateway |
| BDD Tests | ✅ | Cucumber + Chinese Gherkin |
| Architecture Tests | ✅ | ArchUnit |

---

## 附錄 A: 相關文件

- [PRD.md](./PRD.md) - 產品需求文件
- [INFRA.md](./INFRA.md) - 基礎設施文件
- [specs/001-shared-audit-lib](./specs/001-shared-audit-lib/) - 稽核函式庫規格
- [specs/002-multi-tenant-ecommerce](./specs/002-multi-tenant-ecommerce/) - 多租戶電商平台規格

---

*— TECH 文件結束 —*
