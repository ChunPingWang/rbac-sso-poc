package com.example.ecommerce.product.domain.model.aggregate;

import com.example.ecommerce.product.domain.event.*;
import com.example.ecommerce.product.domain.model.valueobject.*;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Product Aggregate Root
 * 維護不變量，封裝業務邏輯，產生領域事件
 */
public class Product {

    private final ProductId id;
    private final ProductCode productCode;  // 建立後不可變
    private String name;
    private Money price;
    private String description;
    private String category;
    private ProductStatus status;
    private String tenantId;
    private String createdBy;
    private Instant createdAt;
    private String updatedBy;
    private Instant updatedAt;

    private final List<DomainEvent> domainEvents = new ArrayList<>();

    // ===== Factory Method =====
    public static Product create(ProductCode code, String name, Money price,
                                  String category, String description,
                                  String tenantId, String createdBy) {
        price.validatePositive();  // 不變量驗證

        Product product = new Product(
            ProductId.generate(),
            code,
            name,
            price,
            category,
            description,
            tenantId,
            createdBy
        );

        product.registerEvent(new ProductCreated(
            product.id, code, name, price, category, createdBy, Instant.now()
        ));

        return product;
    }

    // Private constructor
    private Product(ProductId id, ProductCode code, String name, Money price,
                    String category, String description, String tenantId, String createdBy) {
        this.id = id;
        this.productCode = code;
        this.name = name;
        this.price = price;
        this.category = category;
        this.description = description;
        this.tenantId = tenantId;
        this.status = ProductStatus.ACTIVE;
        this.createdBy = createdBy;
        this.createdAt = Instant.now();
        this.updatedBy = createdBy;
        this.updatedAt = Instant.now();
    }

    // For reconstruction from persistence
    public Product(ProductId id, ProductCode code, String name, Money price,
                   String category, String description, String tenantId,
                   ProductStatus status, String createdBy, Instant createdAt,
                   String updatedBy, Instant updatedAt) {
        this.id = id;
        this.productCode = code;
        this.name = name;
        this.price = price;
        this.category = category;
        this.description = description;
        this.tenantId = tenantId;
        this.status = status;
        this.createdBy = createdBy;
        this.createdAt = createdAt;
        this.updatedBy = updatedBy;
        this.updatedAt = updatedAt;
    }

    // ===== Business Methods =====

    public void update(String name, Money price, String category,
                       String description, String updatedBy) {
        validateNotDeleted();
        if (price != null) {
            price.validatePositive();
        }

        if (name != null) this.name = name;
        if (price != null) this.price = price;
        if (category != null) this.category = category;
        if (description != null) this.description = description;
        this.updatedBy = updatedBy;
        this.updatedAt = Instant.now();

        registerEvent(new ProductUpdated(this.id, name, price, category, updatedBy, Instant.now()));
    }

    public void changePrice(Money newPrice, String changedBy) {
        validateNotDeleted();
        newPrice.validatePositive();

        Money oldPrice = this.price;
        this.price = newPrice;
        this.updatedBy = changedBy;
        this.updatedAt = Instant.now();

        registerEvent(new ProductPriceChanged(id, oldPrice, newPrice, changedBy, Instant.now()));
    }

    public void delete(String deletedBy) {
        validateNotDeleted();
        this.status = ProductStatus.DELETED;
        this.updatedBy = deletedBy;
        this.updatedAt = Instant.now();

        registerEvent(new ProductDeleted(id, deletedBy, Instant.now()));
    }

    public void activate() {
        if (status == ProductStatus.DELETED) {
            throw new IllegalStateException("Cannot activate deleted product");
        }
        this.status = ProductStatus.ACTIVE;
    }

    public void deactivate() {
        validateNotDeleted();
        this.status = ProductStatus.INACTIVE;
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

    // ===== Getters (No Setters - maintain encapsulation) =====
    public ProductId getId() { return id; }
    public ProductCode getProductCode() { return productCode; }
    public String getName() { return name; }
    public Money getPrice() { return price; }
    public String getDescription() { return description; }
    public String getCategory() { return category; }
    public ProductStatus getStatus() { return status; }
    public String getTenantId() { return tenantId; }
    public String getCreatedBy() { return createdBy; }
    public Instant getCreatedAt() { return createdAt; }
    public String getUpdatedBy() { return updatedBy; }
    public Instant getUpdatedAt() { return updatedAt; }
    public boolean isActive() { return status == ProductStatus.ACTIVE; }
}
