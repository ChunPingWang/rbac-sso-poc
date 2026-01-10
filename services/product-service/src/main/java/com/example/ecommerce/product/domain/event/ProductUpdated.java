package com.example.ecommerce.product.domain.event;

import com.example.ecommerce.product.domain.model.valueobject.*;
import java.time.Instant;

public record ProductUpdated(
    ProductId productId,
    String name,
    Money price,
    String category,
    String updatedBy,
    Instant occurredAt
) implements DomainEvent {
}
