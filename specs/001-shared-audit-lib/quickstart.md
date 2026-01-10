# Quickstart: Shared Audit Library

**Feature**: 001-shared-audit-lib | **Date**: 2026-01-10

## Overview

This guide helps you integrate the shared audit library into your microservice in under 30 minutes (per SC-002).

## Prerequisites

- Java 17+
- Spring Boot 3.3.x microservice
- Gradle build system

## Step 1: Add Dependency (2 minutes)

Add the audit library to your service's `build.gradle`:

```groovy
dependencies {
    implementation project(':libs:audit-lib')
}
```

## Step 2: Configure Application (3 minutes)

Add audit configuration to your `application.yml`:

```yaml
# application.yml
audit:
  enabled: true
  service-name: ${spring.application.name}
  payload:
    max-size: 65536  # 64 KB
  masking:
    default-fields:
      - password
      - secret
      - token
      - credential
```

## Step 3: Add Database Migration (5 minutes)

Create the audit_logs table. Add a Flyway migration:

```sql
-- V2__create_audit_logs_table.sql

CREATE TABLE audit_logs (
    id              UUID            PRIMARY KEY,
    timestamp       TIMESTAMP       NOT NULL,
    event_type      VARCHAR(100)    NOT NULL,
    aggregate_type  VARCHAR(100)    NOT NULL,
    aggregate_id    VARCHAR(255),
    username        VARCHAR(100)    NOT NULL DEFAULT 'ANONYMOUS',
    service_name    VARCHAR(100)    NOT NULL,
    action          VARCHAR(255),
    payload         TEXT,
    result          VARCHAR(20)     NOT NULL,
    error_message   TEXT,
    client_ip       VARCHAR(45),
    correlation_id  VARCHAR(100),
    payload_truncated BOOLEAN       NOT NULL DEFAULT FALSE,

    CONSTRAINT chk_result CHECK (result IN ('SUCCESS', 'FAILURE'))
);

-- Performance indexes
CREATE INDEX idx_audit_timestamp ON audit_logs(timestamp DESC);
CREATE INDEX idx_audit_username ON audit_logs(username, timestamp DESC);
CREATE INDEX idx_audit_aggregate ON audit_logs(aggregate_type, aggregate_id, timestamp DESC);
CREATE INDEX idx_audit_event_type ON audit_logs(event_type, timestamp DESC);
CREATE INDEX idx_audit_service ON audit_logs(service_name, timestamp DESC);
```

## Step 4: Enable Audit on Operations (5 minutes)

Add `@Auditable` annotation to methods you want to audit:

```java
import com.example.audit.annotation.Auditable;

@RestController
@RequestMapping("/api/products")
public class ProductCommandController {

    private final ProductCommandService commandService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN')")
    @Auditable(eventType = "PRODUCT_CREATED", resourceType = "Product")
    public ApiResponse<UUID> createProduct(@Valid @RequestBody CreateProductRequest req) {
        // Your existing business logic - NO CHANGES NEEDED
        var productId = commandService.handle(toCommand(req));
        return ApiResponse.success(productId.value(), "Product created");
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Auditable(eventType = "PRODUCT_UPDATED", resourceType = "Product")
    public ApiResponse<Void> updateProduct(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateProductRequest req) {
        commandService.handle(toCommand(id, req));
        return ApiResponse.success(null, "Product updated");
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('ADMIN')")
    @Auditable(eventType = "PRODUCT_DELETED", resourceType = "Product")
    public void deleteProduct(@PathVariable UUID id) {
        commandService.handle(new DeleteProductCommand(id));
    }
}
```

## Step 5: Handle Sensitive Data (3 minutes)

For operations with sensitive data, specify fields to mask:

```java
@PostMapping("/change-password")
@Auditable(
    eventType = "USER_PASSWORD_CHANGED",
    resourceType = "User",
    maskFields = {"oldPassword", "newPassword", "request.confirmPassword"}
)
public ApiResponse<Void> changePassword(@RequestBody ChangePasswordRequest request) {
    // Password fields will be masked as "****" in audit log
    userService.changePassword(request);
    return ApiResponse.success(null, "Password changed");
}
```

## Step 6: Verify Integration (5 minutes)

### Start your service and test:

```bash
# Create a product (adjust to your API)
curl -X POST http://localhost:8080/api/products \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{"productCode": "TEST-001", "productName": "Test Widget", "price": 99.99}'
```

### Check the audit log:

```sql
SELECT * FROM audit_logs ORDER BY timestamp DESC LIMIT 5;
```

Expected output:
```
id        | 550e8400-e29b-41d4-a716-446655440001
timestamp | 2026-01-10 08:30:00.123
event_type| PRODUCT_CREATED
aggregate_type | Product
aggregate_id   | prod-12345
username       | admin@example.com
service_name   | product-service
action         | createProduct
payload        | {"productCode":"TEST-001","productName":"Test Widget","price":99.99}
result         | SUCCESS
client_ip      | 192.168.1.100
```

### Check metrics:

```bash
curl http://localhost:8080/actuator/metrics/audit.events.total
```

## Complete Example

Here's a complete service class with auditing:

```java
package com.example.product.adapter.inbound.rest;

import com.example.audit.annotation.Auditable;
import com.example.common.dto.ApiResponse;
import com.example.product.application.port.input.command.*;
import com.example.product.application.service.ProductCommandService;
import com.example.security.util.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.UUID;

@RestController
@RequestMapping("/api/products")
@Tag(name = "Product Commands")
public class ProductCommandController {

    private final ProductCommandService commandService;

    public ProductCommandController(ProductCommandService commandService) {
        this.commandService = commandService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Create a new product")
    @Auditable(eventType = "PRODUCT_CREATED", resourceType = "Product")
    public ApiResponse<UUID> createProduct(@Valid @RequestBody CreateProductRequest req) {

        String user = SecurityUtils.getCurrentUsername().orElse("unknown");

        CreateProductCommand cmd = new CreateProductCommand(
            req.productCode(),
            req.productName(),
            req.price(),
            req.quantity(),
            req.description(),
            user
        );

        var productId = commandService.handle(cmd);
        return ApiResponse.success(productId.value(), "Product created");
    }

    @PutMapping("/{id}/price")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Update product price")
    @Auditable(eventType = "PRODUCT_PRICE_CHANGED", resourceType = "Product")
    public ApiResponse<Void> updatePrice(
            @PathVariable UUID id,
            @Valid @RequestBody UpdatePriceRequest req) {

        String user = SecurityUtils.getCurrentUsername().orElse("unknown");
        commandService.handle(new UpdatePriceCommand(id, req.newPrice(), user));

        return ApiResponse.success(null, "Price updated");
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Delete a product")
    @Auditable(eventType = "PRODUCT_DELETED", resourceType = "Product")
    public void deleteProduct(@PathVariable UUID id) {

        String user = SecurityUtils.getCurrentUsername().orElse("unknown");
        commandService.handle(new DeleteProductCommand(id, user));
    }
}
```

## Troubleshooting

### Audit logs not appearing

1. **Check if audit is enabled**:
   ```yaml
   audit:
     enabled: true  # Must be true
   ```

2. **Check if annotation is on a public method**:
   - Spring AOP requires methods to be `public`
   - Method must be called from outside the class (not self-invocation)

3. **Check database connection**:
   ```bash
   curl http://localhost:8080/actuator/health
   ```

### Username showing as ANONYMOUS

Ensure your security context is properly configured:
- JWT token must be present in request
- SecurityContextHolder must be populated before the audited method runs

### Payload truncated

If you see `"_truncated": true` in payload:
- Original payload exceeded 64KB limit
- Consider using `payloadExpression` to capture only essential fields

## Next Steps

- Read the [full annotation contract](./contracts/audit-annotation.md) for advanced usage
- Review the [API contract](./contracts/audit-api.yaml) for querying audit logs
- Check metrics at `/actuator/metrics` for `audit.*` metrics

## Time Summary

| Step | Time |
|------|------|
| Add dependency | 2 min |
| Configure application | 3 min |
| Database migration | 5 min |
| Enable on operations | 5 min |
| Handle sensitive data | 3 min |
| Verify integration | 5 min |
| **Total** | **~23 min** |

This is well within the 30-minute target (SC-002).
