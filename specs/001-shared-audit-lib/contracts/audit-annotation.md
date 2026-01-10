# Contract: @Auditable Annotation

**Feature**: 001-shared-audit-lib | **Date**: 2026-01-10

## Overview

The `@Auditable` annotation marks methods for automatic audit logging via AOP. This document defines the contract for how the annotation should be used and what behavior it guarantees.

## Annotation Definition

```java
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Auditable {

    /**
     * Event type identifier (required).
     * Should follow UPPER_SNAKE_CASE convention.
     * Examples: "PRODUCT_CREATED", "USER_PROFILE_UPDATED", "ROLE_ASSIGNED"
     */
    String eventType();

    /**
     * Resource/aggregate type being audited (required).
     * Should match the domain entity name.
     * Examples: "Product", "User", "Role"
     */
    String resourceType();

    /**
     * Field names to mask in the audit payload (optional).
     * Supports nested paths using dot notation.
     * Examples: {"password", "creditCard", "user.ssn"}
     */
    String[] maskFields() default {};

    /**
     * SpEL expression for custom aggregate ID extraction (optional).
     * If not specified, attempts to extract from method arguments or return value.
     * Examples: "#result.id", "#args[0].userId", "#command.productId"
     */
    String aggregateIdExpression() default "";

    /**
     * SpEL expression for custom payload extraction (optional).
     * If not specified, serializes all method arguments.
     * Examples: "#args[0]", "{productId: #args[0], changes: #args[1]}"
     */
    String payloadExpression() default "";

    /**
     * Whether to capture the method return value in the payload (optional).
     * Default: false
     */
    boolean includeResult() default false;
}
```

## Usage Examples

### Basic Usage

```java
@Auditable(eventType = "PRODUCT_CREATED", resourceType = "Product")
public ProductId createProduct(CreateProductCommand command) {
    // Business logic
}
```

### With Field Masking

```java
@Auditable(
    eventType = "USER_PASSWORD_CHANGED",
    resourceType = "User",
    maskFields = {"oldPassword", "newPassword"}
)
public void changePassword(ChangePasswordCommand command) {
    // Business logic
}
```

### With Custom Aggregate ID

```java
@Auditable(
    eventType = "ORDER_UPDATED",
    resourceType = "Order",
    aggregateIdExpression = "#command.orderId.toString()"
)
public void updateOrder(UpdateOrderCommand command) {
    // Business logic
}
```

### With Custom Payload

```java
@Auditable(
    eventType = "BULK_IMPORT",
    resourceType = "Product",
    payloadExpression = "{batchId: #args[0].batchId, count: #args[0].items.size()}"
)
public ImportResult bulkImport(BulkImportCommand command) {
    // Business logic
}
```

### Including Return Value

```java
@Auditable(
    eventType = "PRODUCT_CREATED",
    resourceType = "Product",
    includeResult = true
)
public ProductDetailView createProduct(CreateProductCommand command) {
    // Business logic
    return productDetailView;  // Included in audit payload
}
```

## Behavioral Contract

### Guarantees

| Behavior | Guarantee |
|----------|-----------|
| **Audit Capture** | Audit log is captured after method execution (success or failure) |
| **Business Isolation** | Audit failure NEVER causes business operation to fail |
| **Context Capture** | Username, client IP, timestamp are automatically captured |
| **Payload Processing** | Arguments are JSON-serialized with masking applied |
| **Size Limit** | Payload is truncated at 64KB with truncation marker |
| **Transaction** | Audit capture runs after business transaction commits |

### Edge Case Handling

| Scenario | Behavior |
|----------|----------|
| Method throws exception | Audit captured with `result=FAILURE` and error message |
| Username unavailable | Default to `"ANONYMOUS"` with flag |
| Client IP unavailable | Default to `"unknown"` |
| Circular reference in payload | Handled gracefully (reference markers) |
| Null method arguments | Serialized as `null` in payload |
| Void return type | No return value captured (regardless of `includeResult`) |

### SpEL Expression Context

Available variables in SpEL expressions:

| Variable | Type | Description |
|----------|------|-------------|
| `#args` | `Object[]` | Method arguments array |
| `#arg0`, `#arg1`, ... | `Object` | Individual arguments by index |
| `#result` | `Object` | Method return value (only in success case) |
| `#method` | `Method` | Reflected method object |
| `#target` | `Object` | Target object instance |

### Execution Order

```
1. Method invocation starts
2. Business logic executes
3. Transaction commits (if applicable)
4. AuditAspect captures audit (in finally block)
5. Audit saved to repository
6. Metrics updated
7. Method returns to caller
```

## Integration Requirements

### Dependency

```groovy
dependencies {
    implementation project(':libs:audit-lib')
}
```

### Configuration

```yaml
audit:
  enabled: true
  service-name: ${spring.application.name}
```

### Placement

The `@Auditable` annotation SHOULD be placed on:

- Application service methods (use case handlers)
- Controller methods (for REST API audit)

The annotation SHOULD NOT be placed on:

- Domain layer methods (violates hexagonal architecture)
- Private methods (proxy won't intercept)
- Methods called internally from same class (self-invocation bypass)

## Metrics Exposed

When `@Auditable` methods execute, the following metrics are updated:

| Metric | Type | Description |
|--------|------|-------------|
| `audit.events.total{eventType,result}` | Counter | Total audited events |
| `audit.events.failed` | Counter | Failed audit captures |
| `audit.capture.latency` | Timer | Time to capture and store |

## Validation Rules

The annotation processor validates:

1. `eventType` is not blank
2. `resourceType` is not blank
3. `maskFields` entries are valid field paths
4. SpEL expressions are syntactically valid (at startup)
