package com.example.ecommerce.product.domain.model.valueobject;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;
import java.util.regex.Pattern;

/**
 * Product Code Value Object
 * 格式：P + 6位數字，例如 P000001
 */
public final class ProductCode {

    private static final Pattern PATTERN = Pattern.compile("^P\\d{6}$");
    private static final AtomicLong COUNTER = new AtomicLong(System.currentTimeMillis() % 1000000);

    private final String value;

    private ProductCode(String value) {
        Objects.requireNonNull(value, "ProductCode cannot be null");
        if (!PATTERN.matcher(value).matches()) {
            throw new IllegalArgumentException(
                "ProductCode must match pattern P + 6 digits (e.g., P000001), got: " + value
            );
        }
        this.value = value;
    }

    public static ProductCode of(String value) {
        return new ProductCode(value.toUpperCase());
    }

    public static ProductCode generate() {
        long next = COUNTER.incrementAndGet() % 1000000;
        return new ProductCode(String.format("P%06d", next));
    }

    public String value() {
        return value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ProductCode that)) return false;
        return value.equals(that.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(value);
    }

    @Override
    public String toString() {
        return value;
    }
}
