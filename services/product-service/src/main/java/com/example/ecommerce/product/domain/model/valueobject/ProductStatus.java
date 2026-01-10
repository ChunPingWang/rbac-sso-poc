package com.example.ecommerce.product.domain.model.valueobject;

/**
 * 商品狀態
 */
public enum ProductStatus {
    ACTIVE,     // 上架中
    INACTIVE,   // 下架中
    DELETED     // 已刪除（軟刪除）
}
