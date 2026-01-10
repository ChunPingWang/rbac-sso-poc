package com.example.ecommerce.product.application.port.input.query;

import java.util.UUID;

public record GetProductByIdQuery(UUID productId) {
    public GetProductByIdQuery {
        if (productId == null)
            throw new IllegalArgumentException("Product ID is required");
    }
}
