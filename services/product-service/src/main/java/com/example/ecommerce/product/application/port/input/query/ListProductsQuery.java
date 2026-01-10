package com.example.ecommerce.product.application.port.input.query;

public record ListProductsQuery(
    int page,
    int size,
    String category,
    String sortBy,
    String sortDirection
) {
    public ListProductsQuery {
        if (page < 0) page = 0;
        if (size <= 0 || size > 100) size = 20;
        if (sortBy == null || sortBy.isBlank()) sortBy = "createdAt";
        if (sortDirection == null) sortDirection = "DESC";
    }

    public static ListProductsQuery defaultQuery() {
        return new ListProductsQuery(0, 20, null, "createdAt", "DESC");
    }
}
