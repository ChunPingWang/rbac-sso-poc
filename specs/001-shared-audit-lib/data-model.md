# Data Model: Shared Audit Library

**Feature**: 001-shared-audit-lib | **Date**: 2026-01-10

## 1. Domain Entities

### 1.1 AuditLog (Aggregate Root)

The primary entity representing a single audit trail entry.

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                               AuditLog                                       │
│                          (Aggregate Root)                                    │
├─────────────────────────────────────────────────────────────────────────────┤
│ + id: AuditLogId                  # Unique identifier (UUID)                │
│ + timestamp: Instant              # When the event occurred (UTC)           │
│ + eventType: AuditEventType       # Type of audit event (value object)      │
│ + aggregateType: String           # Entity type (e.g., "Product")           │
│ + aggregateId: String             # Entity ID being audited                 │
│ + username: String                # Executor identity                       │
│ + serviceName: String             # Originating microservice                │
│ + action: String                  # Method/operation name                   │
│ + payload: String                 # JSON serialized payload (max 64KB)      │
│ + result: AuditResult             # SUCCESS or FAILURE                      │
│ + errorMessage: String?           # Error details if failed (nullable)      │
│ + clientIp: String                # Client IP address                       │
│ + correlationId: String?          # Links related operations (nullable)     │
│ + payloadTruncated: boolean       # True if payload was truncated           │
├─────────────────────────────────────────────────────────────────────────────┤
│ «invariants»                                                                 │
│ - id must not be null                                                        │
│ - timestamp must not be null                                                 │
│ - eventType must not be null                                                 │
│ - aggregateType must not be blank                                            │
│ - username must not be blank (default: "ANONYMOUS")                         │
│ - serviceName must not be blank                                              │
│ - result must not be null                                                    │
│ - payload size must not exceed 64KB                                          │
│ - once created, all fields are immutable (append-only)                       │
└─────────────────────────────────────────────────────────────────────────────┘
```

**Java Implementation**:

```java
public final class AuditLog {

    private final AuditLogId id;
    private final Instant timestamp;
    private final AuditEventType eventType;
    private final String aggregateType;
    private final String aggregateId;
    private final String username;
    private final String serviceName;
    private final String action;
    private final String payload;
    private final AuditResult result;
    private final String errorMessage;
    private final String clientIp;
    private final String correlationId;
    private final boolean payloadTruncated;

    // Private constructor - use builder
    private AuditLog(Builder builder) {
        // Validation
        Objects.requireNonNull(builder.id, "id must not be null");
        Objects.requireNonNull(builder.timestamp, "timestamp must not be null");
        Objects.requireNonNull(builder.eventType, "eventType must not be null");
        requireNotBlank(builder.aggregateType, "aggregateType");
        requireNotBlank(builder.username, "username");
        requireNotBlank(builder.serviceName, "serviceName");
        Objects.requireNonNull(builder.result, "result must not be null");

        this.id = builder.id;
        this.timestamp = builder.timestamp;
        this.eventType = builder.eventType;
        this.aggregateType = builder.aggregateType;
        this.aggregateId = builder.aggregateId;
        this.username = builder.username;
        this.serviceName = builder.serviceName;
        this.action = builder.action;
        this.payload = builder.payload;
        this.result = builder.result;
        this.errorMessage = builder.errorMessage;
        this.clientIp = builder.clientIp;
        this.correlationId = builder.correlationId;
        this.payloadTruncated = builder.payloadTruncated;
    }

    public static Builder builder() {
        return new Builder();
    }

    // Getters only - no setters (immutable)
}
```

## 2. Value Objects

### 2.1 AuditLogId

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                              AuditLogId                                      │
│                           (Value Object)                                     │
├─────────────────────────────────────────────────────────────────────────────┤
│ + value: UUID                                                                │
├─────────────────────────────────────────────────────────────────────────────┤
│ + of(UUID): AuditLogId           # Factory method                           │
│ + generate(): AuditLogId         # Generate new random ID                   │
│ + toString(): String             # Returns UUID string                       │
│ + equals(Object): boolean        # Value equality                            │
│ + hashCode(): int                # Based on UUID value                       │
└─────────────────────────────────────────────────────────────────────────────┘
```

### 2.2 AuditEventType

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                            AuditEventType                                    │
│                           (Value Object)                                     │
├─────────────────────────────────────────────────────────────────────────────┤
│ + value: String                  # Event type name (e.g., "PRODUCT_CREATED")│
├─────────────────────────────────────────────────────────────────────────────┤
│ «invariants»                                                                 │
│ - value must not be blank                                                    │
│ - value should follow UPPER_SNAKE_CASE convention                            │
├─────────────────────────────────────────────────────────────────────────────┤
│ + of(String): AuditEventType     # Factory method with validation           │
│ + equals(Object): boolean        # Value equality                            │
│ + hashCode(): int                # Based on value                            │
└─────────────────────────────────────────────────────────────────────────────┘
```

### 2.3 AuditResult (Enum)

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                             AuditResult                                      │
│                              (Enum)                                          │
├─────────────────────────────────────────────────────────────────────────────┤
│ SUCCESS                          # Operation completed successfully          │
│ FAILURE                          # Operation failed with error               │
└─────────────────────────────────────────────────────────────────────────────┘
```

## 3. Port Interfaces

### 3.1 AuditLogRepository (Output Port)

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                         «interface»                                          │
│                       AuditLogRepository                                     │
│                        (Output Port)                                         │
├─────────────────────────────────────────────────────────────────────────────┤
│ «append-only semantics - no update/delete methods»                          │
├─────────────────────────────────────────────────────────────────────────────┤
│ + save(AuditLog): AuditLog                                                  │
│ + findById(AuditLogId): Optional<AuditLog>                                  │
│ + findByUsername(String, Pageable): Page<AuditLog>                          │
│ + findByAggregateTypeAndAggregateId(String, String, Pageable): Page         │
│ + findByEventType(AuditEventType, Pageable): Page<AuditLog>                 │
│ + findByTimestampBetween(Instant, Instant, Pageable): Page<AuditLog>        │
│ + findByServiceName(String, Pageable): Page<AuditLog>                       │
│ + findByCorrelationId(String): List<AuditLog>                               │
└─────────────────────────────────────────────────────────────────────────────┘
```

**Note**: This interface intentionally omits `update()`, `delete()`, and `deleteAll()` methods to enforce append-only semantics per FR-011.

## 4. Database Schema

### 4.1 Table: audit_logs

```sql
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

    -- Constraints
    CONSTRAINT chk_result CHECK (result IN ('SUCCESS', 'FAILURE')),
    CONSTRAINT chk_payload_size CHECK (LENGTH(payload) <= 65536)
);

-- Indexes for query performance (SC-008: <5 seconds for 1M records)
CREATE INDEX idx_audit_timestamp ON audit_logs(timestamp DESC);
CREATE INDEX idx_audit_username ON audit_logs(username, timestamp DESC);
CREATE INDEX idx_audit_aggregate ON audit_logs(aggregate_type, aggregate_id, timestamp DESC);
CREATE INDEX idx_audit_event_type ON audit_logs(event_type, timestamp DESC);
CREATE INDEX idx_audit_service ON audit_logs(service_name, timestamp DESC);
CREATE INDEX idx_audit_correlation ON audit_logs(correlation_id) WHERE correlation_id IS NOT NULL;

-- Optional: Prevent updates/deletes at database level
-- REVOKE UPDATE, DELETE ON audit_logs FROM app_user;
```

### 4.2 JPA Entity Mapping

```java
@Entity
@Table(name = "audit_logs")
@Immutable  // Hibernate: prevents HQL/JPQL updates
public class AuditLogJpaEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "timestamp", nullable = false, updatable = false)
    private Instant timestamp;

    @Column(name = "event_type", nullable = false, length = 100, updatable = false)
    private String eventType;

    @Column(name = "aggregate_type", nullable = false, length = 100, updatable = false)
    private String aggregateType;

    @Column(name = "aggregate_id", length = 255, updatable = false)
    private String aggregateId;

    @Column(name = "username", nullable = false, length = 100, updatable = false)
    private String username;

    @Column(name = "service_name", nullable = false, length = 100, updatable = false)
    private String serviceName;

    @Column(name = "action", length = 255, updatable = false)
    private String action;

    @Column(name = "payload", columnDefinition = "TEXT", updatable = false)
    private String payload;

    @Enumerated(EnumType.STRING)
    @Column(name = "result", nullable = false, length = 20, updatable = false)
    private AuditResult result;

    @Column(name = "error_message", columnDefinition = "TEXT", updatable = false)
    private String errorMessage;

    @Column(name = "client_ip", length = 45, updatable = false)
    private String clientIp;

    @Column(name = "correlation_id", length = 100, updatable = false)
    private String correlationId;

    @Column(name = "payload_truncated", nullable = false, updatable = false)
    private boolean payloadTruncated;

    // Protected no-arg constructor for JPA
    protected AuditLogJpaEntity() {}

    // All-args constructor for mapping from domain
}
```

## 5. Entity Relationships

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                          Entity Relationships                                │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                              │
│   ┌─────────────────┐                                                       │
│   │    AuditLog     │ ──────────────────────────────────────────────────►   │
│   │  (Standalone)   │       References external aggregates via:             │
│   └────────┬────────┘       - aggregateType (string)                        │
│            │                - aggregateId (string)                          │
│            │                                                                │
│            │ correlationId                                                  │
│            │ (optional)                                                     │
│            ▼                                                                │
│   ┌─────────────────┐                                                       │
│   │    AuditLog     │  Multiple AuditLogs can share same correlationId      │
│   │   (Related)     │  to link operations in the same transaction          │
│   └─────────────────┘                                                       │
│                                                                              │
│   Note: AuditLog is a standalone aggregate with no direct entity            │
│   relationships. It references other aggregates by ID only.                 │
│                                                                              │
└─────────────────────────────────────────────────────────────────────────────┘
```

## 6. Data Flow

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                           Audit Data Flow                                    │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                              │
│   Business Operation                                                        │
│          │                                                                  │
│          ▼                                                                  │
│   ┌─────────────────┐     ┌─────────────────┐     ┌─────────────────┐      │
│   │  @Auditable     │────▶│  AuditAspect    │────▶│ PayloadProcessor│      │
│   │  Annotation     │     │  (Intercept)    │     │ (Serialize/Mask)│      │
│   └─────────────────┘     └────────┬────────┘     └────────┬────────┘      │
│                                    │                       │                │
│                                    │  Extract context      │ Processed      │
│                                    ▼                       │ payload        │
│                           ┌─────────────────┐              │                │
│                           │AuditContextHolder│             │                │
│                           │ - username      │              │                │
│                           │ - clientIp      │              │                │
│                           │ - correlationId │              │                │
│                           └────────┬────────┘              │                │
│                                    │                       │                │
│                                    ▼                       ▼                │
│                           ┌─────────────────────────────────────┐          │
│                           │           AuditLog.builder()        │          │
│                           │        (Domain Entity Creation)     │          │
│                           └────────────────┬────────────────────┘          │
│                                            │                                │
│                                            ▼                                │
│                           ┌─────────────────────────────────────┐          │
│                           │      AuditLogRepository.save()      │          │
│                           │         (Output Port)               │          │
│                           └────────────────┬────────────────────┘          │
│                                            │                                │
│                                            ▼                                │
│                           ┌─────────────────────────────────────┐          │
│                           │    JpaAuditLogRepository            │          │
│                           │      (Adapter Implementation)       │          │
│                           └────────────────┬────────────────────┘          │
│                                            │                                │
│                                            ▼                                │
│                           ┌─────────────────────────────────────┐          │
│                           │        audit_logs table             │          │
│                           │        (PostgreSQL/H2)              │          │
│                           └─────────────────────────────────────┘          │
│                                                                              │
└─────────────────────────────────────────────────────────────────────────────┘
```

## 7. Sample Data

### 7.1 Successful Operation Audit

```json
{
  "id": "550e8400-e29b-41d4-a716-446655440001",
  "timestamp": "2026-01-10T08:30:00.123Z",
  "eventType": "PRODUCT_CREATED",
  "aggregateType": "Product",
  "aggregateId": "prod-12345",
  "username": "admin@example.com",
  "serviceName": "product-service",
  "action": "createProduct",
  "payload": "{\"productCode\":\"SKU-001\",\"productName\":\"Widget\",\"price\":99.99}",
  "result": "SUCCESS",
  "errorMessage": null,
  "clientIp": "192.168.1.100",
  "correlationId": "corr-abc-123",
  "payloadTruncated": false
}
```

### 7.2 Failed Operation Audit

```json
{
  "id": "550e8400-e29b-41d4-a716-446655440002",
  "timestamp": "2026-01-10T08:31:00.456Z",
  "eventType": "PRODUCT_UPDATED",
  "aggregateType": "Product",
  "aggregateId": "prod-99999",
  "username": "user@example.com",
  "serviceName": "product-service",
  "action": "updateProduct",
  "payload": "{\"productId\":\"prod-99999\",\"newPrice\":150.00}",
  "result": "FAILURE",
  "errorMessage": "ProductNotFoundException: Product with id prod-99999 not found",
  "clientIp": "192.168.1.101",
  "correlationId": "corr-def-456",
  "payloadTruncated": false
}
```

### 7.3 Masked Payload Audit

```json
{
  "id": "550e8400-e29b-41d4-a716-446655440003",
  "timestamp": "2026-01-10T08:32:00.789Z",
  "eventType": "USER_PASSWORD_CHANGED",
  "aggregateType": "User",
  "aggregateId": "user-67890",
  "username": "user@example.com",
  "serviceName": "user-service",
  "action": "changePassword",
  "payload": "{\"userId\":\"user-67890\",\"oldPassword\":\"****\",\"newPassword\":\"****\"}",
  "result": "SUCCESS",
  "errorMessage": null,
  "clientIp": "192.168.1.102",
  "correlationId": null,
  "payloadTruncated": false
}
```

### 7.4 Truncated Payload Audit

```json
{
  "id": "550e8400-e29b-41d4-a716-446655440004",
  "timestamp": "2026-01-10T08:33:00.012Z",
  "eventType": "BULK_IMPORT",
  "aggregateType": "Product",
  "aggregateId": null,
  "username": "system",
  "serviceName": "product-service",
  "action": "bulkImportProducts",
  "payload": "{\"_truncated\":true,\"_originalSize\":128000,\"_summary\":{\"recordCount\":500,\"batchId\":\"batch-123\"}}",
  "result": "SUCCESS",
  "errorMessage": null,
  "clientIp": "10.0.0.50",
  "correlationId": "corr-bulk-789",
  "payloadTruncated": true
}
```

## 8. Query Patterns

### 8.1 Primary Query Use Cases

| Use Case | Query Method | Index Used |
|----------|--------------|------------|
| Audit by user | `findByUsername(username, pageable)` | `idx_audit_username` |
| Audit by entity | `findByAggregateTypeAndAggregateId(type, id, pageable)` | `idx_audit_aggregate` |
| Audit by time range | `findByTimestampBetween(start, end, pageable)` | `idx_audit_timestamp` |
| Audit by event type | `findByEventType(type, pageable)` | `idx_audit_event_type` |
| Audit by service | `findByServiceName(service, pageable)` | `idx_audit_service` |
| Related operations | `findByCorrelationId(correlationId)` | `idx_audit_correlation` |

### 8.2 Performance Targets (from SC-008)

- Query response time: <5 seconds
- Data volume: up to 1 million records
- Pagination: 100 records per page default
