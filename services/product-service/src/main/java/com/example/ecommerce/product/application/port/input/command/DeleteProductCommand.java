package com.example.ecommerce.product.application.port.input.command;

import java.util.UUID;

public record DeleteProductCommand(UUID productId) {
    public DeleteProductCommand {
        if (productId == null)
            throw new IllegalArgumentException("Product ID is required");
    }
}
