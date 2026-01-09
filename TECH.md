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

## 6. 架構測試 (ArchUnit)

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
