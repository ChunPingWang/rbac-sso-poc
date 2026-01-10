package com.example.ecommerce.product.domain.event;

import com.example.ecommerce.product.domain.model.valueobject.*;
import java.time.Instant;

public record ProductDeleted(
    ProductId productId,
    String deletedBy,
    Instant occurredAt
) implements DomainEvent {
}
