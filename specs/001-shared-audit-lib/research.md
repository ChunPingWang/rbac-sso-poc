# Research: Shared Audit Library

**Feature**: 001-shared-audit-lib | **Date**: 2026-01-10

## 1. Technology Research

### 1.1 Spring AOP for Cross-Cutting Concerns

**Purpose**: Implement audit logging as a cross-cutting concern without polluting business logic.

**Key Findings**:

| Aspect | Details |
|--------|---------|
| **Framework** | Spring AOP 6.1.x (proxy-based) |
| **Annotation** | Custom `@Auditable` with retention at RUNTIME |
| **Pointcut** | `@Around("@annotation(auditable)")` |
| **Ordering** | `@Order(Ordered.LOWEST_PRECEDENCE - 10)` - execute after transaction commit |

**Proxy Behavior**:
- Spring AOP uses JDK dynamic proxies (for interfaces) or CGLIB (for classes)
- Self-invocation within the same class bypasses the proxy - must call from external class
- `@EnableAspectJAutoProxy(proxyTargetClass = true)` ensures CGLIB proxies

**Code Pattern**:
```java
@Aspect
@Component
@Order(Ordered.LOWEST_PRECEDENCE - 10)
public class AuditAspect {

    @Around("@annotation(auditable)")
    public Object audit(ProceedingJoinPoint pjp, Auditable auditable) throws Throwable {
        // Capture before execution
        Object result = pjp.proceed();  // Execute actual method
        // Capture after execution (success)
        return result;
    }
}
```

**Constraints**:
- Aspect must not throw exceptions that block business execution
- Use try-finally to ensure audit capture even on exception
- Audit failures logged locally, not propagated

### 1.2 Append-Only Storage Pattern

**Purpose**: Ensure audit integrity by preventing updates/deletions.

**Implementation Options**:

| Option | Pros | Cons | Chosen |
|--------|------|------|--------|
| Database triggers | Enforced at DB level | Requires DBA, DB-specific | No |
| JPA lifecycle callbacks | Simple, portable | Can be bypassed | No |
| Repository interface design | Clean architecture, testable | Requires discipline | ✅ Yes |
| PostgreSQL RULE | Strong enforcement | PostgreSQL-specific | Optional |

**Chosen Approach**: Repository Interface Design
- `AuditLogRepository` interface only exposes `save()` and `find*()` methods
- No `update()`, `delete()`, or `deleteAll()` methods exposed
- JPA entity uses `@Immutable` (Hibernate) for additional safety
- Database constraint (optional): `REVOKE UPDATE, DELETE ON audit_logs`

**Entity Pattern**:
```java
@Entity
@Table(name = "audit_logs")
@Immutable  // Hibernate-specific: prevents HQL updates
public class AuditLogEntity {
    @Id
    private UUID id;

    // All fields are final-like (no setters)
    // Constructor sets all values
}
```

### 1.3 Payload Processing

**Purpose**: Handle serialization, size limits, and sensitive data masking.

**Size Limit**: 64 KB maximum (from spec clarification)

**Truncation Strategy**:
```java
if (serializedPayload.length() > MAX_SIZE) {
    return Map.of(
        "_truncated", true,
        "_originalSize", serializedPayload.length(),
        "_summary", extractKeyFields(originalPayload)
    );
}
```

**Masking Strategy**: Partial visibility (format preserved, value hidden)

| Field Type | Example Input | Masked Output |
|------------|---------------|---------------|
| Password | `secret123` | `****` |
| Credit Card | `4111-1111-1111-1234` | `****-****-****-1234` |
| Email | `user@example.com` | `u***@example.com` |
| Phone | `+1-555-123-4567` | `+1-555-***-****` |

**Implementation**: FieldMasker interface with strategy pattern
```java
public interface FieldMasker {
    boolean supports(String fieldName);
    String mask(Object value);
}
```

### 1.4 Context Propagation

**Purpose**: Capture username and client IP from request context.

**Context Sources**:

| Data | Source | Spring Mechanism |
|------|--------|------------------|
| Username | JWT/OAuth2 token | `SecurityContextHolder` |
| Client IP | HTTP request | `RequestContextHolder` + `X-Forwarded-For` |
| Correlation ID | MDC | `MDC.get("correlationId")` |
| Service Name | Application property | `@Value("${spring.application.name}")` |

**Thread Safety Consideration**:
- `SecurityContextHolder` uses `ThreadLocal` by default
- For async processing, use `DelegatingSecurityContextExecutor`
- `AuditContextHolder` wraps these sources with null-safe access

### 1.5 Metrics and Health

**Purpose**: Expose operational metrics per FR-012.

**Micrometer Metrics**:

| Metric Name | Type | Description |
|-------------|------|-------------|
| `audit.events.total` | Counter | Total audit events captured |
| `audit.events.failed` | Counter | Failed audit captures |
| `audit.capture.latency` | Timer | Time to capture and store audit |
| `audit.queue.depth` | Gauge | Pending async audit events |

**Health Indicator**:
```java
@Component
public class AuditHealthIndicator implements HealthIndicator {

    @Override
    public Health health() {
        // Check if audit repository is accessible
        // Check if failure rate is acceptable
        return Health.up()
            .withDetail("failureRate", calculateFailureRate())
            .withDetail("queueDepth", getQueueDepth())
            .build();
    }
}
```

## 2. Existing Codebase Patterns

### 2.1 Hexagonal Architecture Alignment

From TECH.md, the codebase follows:
- **Domain Layer**: Pure Java, no framework dependencies
- **Application Layer**: Use cases, depends only on domain
- **Infrastructure/Adapter Layer**: Spring, JPA, external integrations

**Audit Library Alignment**:
```
libs/audit-lib/
├── domain/           # AuditLog entity, AuditLogRepository interface
├── application/      # AuditQueryService (queries only)
└── infrastructure/   # AuditAspect, JPA adapter, config
```

### 2.2 Existing Annotation Patterns

From TECH.md Section 5.6, controllers already use:
- `@PreAuthorize` for authorization
- `@Operation` for OpenAPI docs
- `@Valid` for validation

**Audit annotation fits naturally**:
```java
@PostMapping
@PreAuthorize("hasRole('ADMIN')")
@Auditable(eventType = "PRODUCT_CREATED", resourceType = "Product")
public ApiResponse<UUID> createProduct(@Valid @RequestBody CreateProductRequest req) {
```

### 2.3 Error Handling Pattern

From constitution (Principle VI):
> "Errors MUST be handled explicitly. Use domain-specific exceptions. Never swallow exceptions silently."

**Audit Exception Handling**:
- Audit failures are NOT domain exceptions
- They are infrastructure concerns
- Logged via SLF4J, metrics incremented
- Never thrown to caller

## 3. Integration Points

### 3.1 With Microservices

Each microservice adds:
```groovy
// build.gradle
dependencies {
    implementation project(':libs:audit-lib')
}
```

Auto-configuration enables via:
```yaml
# application.yml
audit:
  enabled: true
  service-name: ${spring.application.name}
```

### 3.2 With Security Context

```java
@Component
public class AuditContextHolder {

    public Optional<String> getCurrentUsername() {
        return Optional.ofNullable(SecurityContextHolder.getContext())
            .map(SecurityContext::getAuthentication)
            .filter(Authentication::isAuthenticated)
            .map(Authentication::getName);
    }
}
```

### 3.3 With Database

Shared audit table options:
1. **Per-service table**: Each service has own `audit_logs` table
2. **Central audit database**: Separate database/schema for all audits
3. **Event streaming**: Kafka topic → dedicated audit service

**Chosen for POC**: Per-service table (simplest, no additional infra)

## 4. Risk Assessment

| Risk | Likelihood | Impact | Mitigation |
|------|------------|--------|------------|
| Audit failure blocks business | Low | High | Try-finally pattern, no exception propagation |
| Performance degradation | Medium | Medium | Async processing, <50ms target |
| Payload too large | Medium | Low | 64KB truncation with marker |
| Self-invocation bypasses audit | Medium | Medium | Document limitation, use external calls |
| Security context unavailable | Low | Low | Default to "ANONYMOUS" with flag |

## 5. Decisions Made

| Decision | Rationale |
|----------|-----------|
| Spring AOP over AspectJ | Spring AOP sufficient, no compile-time weaving needed |
| Repository-level append-only | Portable, testable, aligns with hexagonal architecture |
| Per-service audit table | Simpler for POC, can migrate to central later |
| Sync capture by default | Simpler initial implementation, async optional |
| Jackson for serialization | Already in Spring Boot, handles circular refs |

## 6. Open Questions (Resolved)

| Question | Resolution | Source |
|----------|------------|--------|
| Audit log immutability? | Append-only storage | Spec clarification |
| Max payload size? | 64 KB | Spec clarification |
| Masking strategy? | Partial visibility | Spec clarification |
| Expected throughput? | 100-500 events/sec | Spec clarification |
| Self-monitoring? | Micrometer metrics + health indicator | Spec clarification |

## 7. References

- [Spring AOP Documentation](https://docs.spring.io/spring-framework/reference/core/aop.html)
- [Micrometer Documentation](https://micrometer.io/docs)
- [TECH.md Section 9](../../../TECH.md#9-共用稽核函式庫-audit-lib) - Audit library architecture
- [Constitution](../../.specify/memory/constitution.md) - Development principles
