package com.example.ecommerce.product.application.port.input.command;

import java.math.BigDecimal;

public record CreateProductCommand(
    String productCode,
    String name,
    BigDecimal price,
    String category,
    String description
) {
    public CreateProductCommand {
        if (name == null || name.isBlank())
            throw new IllegalArgumentException("Product name is required");
        if (price == null || price.compareTo(BigDecimal.ZERO) <= 0)
            throw new IllegalArgumentException("Price must be positive");
    }
}
