package com.example.ecommerce.product.application.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record ProductView(
    UUID id,
    String productCode,
    String name,
    BigDecimal price,
    String category,
    String description,
    String status,
    String tenantId,
    String createdBy,
    Instant createdAt,
    String updatedBy,
    Instant updatedAt
) {}
