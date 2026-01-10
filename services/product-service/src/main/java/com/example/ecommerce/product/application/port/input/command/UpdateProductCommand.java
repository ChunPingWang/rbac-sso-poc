package com.example.ecommerce.product.application.port.input.command;

import java.math.BigDecimal;
import java.util.UUID;

public record UpdateProductCommand(
    UUID productId,
    String name,
    BigDecimal price,
    String category,
    String description
) {
    public UpdateProductCommand {
        if (productId == null)
            throw new IllegalArgumentException("Product ID is required");
    }
}
