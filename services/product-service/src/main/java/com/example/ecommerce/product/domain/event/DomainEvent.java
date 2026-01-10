package com.example.ecommerce.product.domain.event;

import java.time.Instant;

/**
 * 領域事件基礎介面
 */
public interface DomainEvent {
    Instant occurredAt();
}
