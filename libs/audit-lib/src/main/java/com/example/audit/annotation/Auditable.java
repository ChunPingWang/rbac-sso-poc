package com.example.audit.annotation;

import java.lang.annotation.*;

/**
 * Marks a method for automatic audit logging via AOP.
 *
 * <p>When a method annotated with @Auditable is executed, an audit log entry
 * is automatically created capturing operation details, executor identity,
 * timestamp, and result.</p>
 *
 * <h2>Usage Example:</h2>
 * <pre>{@code
 * @Auditable(eventType = "PRODUCT_CREATED", resourceType = "Product")
 * public ProductId createProduct(CreateProductCommand command) {
 *     // Business logic - no audit code needed
 * }
 * }</pre>
 *
 * <h2>With Field Masking:</h2>
 * <pre>{@code
 * @Auditable(
 *     eventType = "USER_PASSWORD_CHANGED",
 *     resourceType = "User",
 *     maskFields = {"oldPassword", "newPassword"}
 * )
 * public void changePassword(ChangePasswordCommand command) {
 *     // Passwords will be masked as "****" in audit log
 * }
 * }</pre>
 *
 * @see com.example.audit.infrastructure.aspect.AuditAspect
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Auditable {

    /**
     * Event type identifier (required).
     * Should follow UPPER_SNAKE_CASE convention.
     *
     * <p>Examples: "PRODUCT_CREATED", "USER_PROFILE_UPDATED", "ROLE_ASSIGNED"</p>
     *
     * @return the event type
     */
    String eventType();

    /**
     * Resource/aggregate type being audited (required).
     * Should match the domain entity name.
     *
     * <p>Examples: "Product", "User", "Role"</p>
     *
     * @return the resource type
     */
    String resourceType();

    /**
     * Field names to mask in the audit payload (optional).
     * Supports nested paths using dot notation.
     *
     * <p>Examples: {"password", "creditCard", "user.ssn"}</p>
     *
     * @return array of field names to mask
     */
    String[] maskFields() default {};

    /**
     * SpEL expression for custom aggregate ID extraction (optional).
     * If not specified, attempts to extract from method arguments or return value.
     *
     * <p>Examples: "#result.id", "#args[0].userId", "#command.productId"</p>
     *
     * @return SpEL expression for aggregate ID
     */
    String aggregateIdExpression() default "";

    /**
     * SpEL expression for custom payload extraction (optional).
     * If not specified, serializes all method arguments.
     *
     * <p>Examples: "#args[0]", "{productId: #args[0], changes: #args[1]}"</p>
     *
     * @return SpEL expression for payload
     */
    String payloadExpression() default "";

    /**
     * Whether to capture the method return value in the payload (optional).
     * Default: false
     *
     * @return true to include result in payload
     */
    boolean includeResult() default false;
}
