package com.example.ecommerce.product.domain.event;

import com.example.ecommerce.product.domain.model.valueobject.*;
import java.time.Instant;

public record ProductCreated(
    ProductId productId,
    ProductCode productCode,
    String name,
    Money price,
    String category,
    String createdBy,
    Instant occurredAt
) implements DomainEvent {
}
