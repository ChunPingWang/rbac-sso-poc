package com.example.ecommerce.product.domain.event;

import com.example.ecommerce.product.domain.model.valueobject.*;
import java.time.Instant;

public record ProductPriceChanged(
    ProductId productId,
    Money oldPrice,
    Money newPrice,
    String changedBy,
    Instant occurredAt
) implements DomainEvent {
}
