package com.example.ecommerce.product.adapter.inbound.rest.dto;

import jakarta.validation.constraints.DecimalMin;
import java.math.BigDecimal;

public record UpdateProductRequest(
    String name,

    @DecimalMin(value = "0.01", message = "Price must be positive")
    BigDecimal price,

    String category,

    String description
) {}
