# Implementation Plan: Multi-Tenant E-Commerce Platform

**Branch**: `main` / `domain-event-for-audit` | **Date**: 2026-01-10 | **Spec**: [spec.md](./spec.md)
**Status**: Implemented

## Summary

Multi-tenant e-commerce platform PoC demonstrating enterprise-grade architecture patterns including Hexagonal Architecture, DDD, CQRS, RBAC, and SSO integration with Keycloak/LDAP.

## Technical Context

| Category | Technology | Version |
|----------|------------|---------|
| Language | Java | 17 (LTS) |
| Framework | Spring Boot | 3.3.x |
| Build Tool | Gradle | 8.5+ |
| Gateway | Spring Cloud Gateway | 4.1.x |
| Security | Spring Security + OAuth2 | 6.3.x |
| Database | H2 (Dev) / PostgreSQL (Prod) | - |
| SSO | Keycloak | 24.0 |
| LDAP | OpenLDAP | 1.5 |
| Container | Docker / Kubernetes | - |

## Constitution Check

| Principle | Status | Notes |
|-----------|--------|-------|
| I. Hexagonal Architecture | PASS | Ports & Adapters pattern in all services |
| II. Domain-Driven Design | PASS | Aggregates, Value Objects, Domain Events |
| III. SOLID Principles | PASS | SRP via CQRS, DIP via interfaces |
| IV. Test-Driven Development | PASS | 300+ tests, 18 BDD scenarios |
| V. Behavior-Driven Development | PASS | Cucumber with Chinese Gherkin |
| VI. Code Quality Standards | PASS | ArchUnit architecture tests |
| VII. Dependency Inversion | PASS | Domain layer has no framework dependencies |

## Project Structure

```text
rbac-sso-poc/
├── libs/
│   ├── audit-lib/           # Shared audit library
│   ├── common-lib/          # Common DTOs and exceptions
│   ├── security-lib/        # Security utilities
│   └── tenant-lib/          # Multi-tenant context
│
├── services/
│   ├── gateway-service/     # API Gateway (:8080)
│   ├── product-service/     # Product management (:8081)
│   └── user-service/        # User profile (:8082)
│
├── tests/
│   └── scenario-tests/      # Cucumber BDD tests
│
├── deploy/
│   ├── docker/              # Docker Compose configs
│   ├── k8s/                 # Kubernetes manifests
│   └── scripts/             # Deployment scripts
│
└── infra/
    ├── ldap/                # LDAP bootstrap
    └── keycloak/            # Realm configuration
```

## Microservice Architecture

### Gateway Service (Port 8080)

**Responsibility**: API routing, authentication, rate limiting

```
adapter/inbound/
└── config/
    ├── GatewaySecurityConfig.java    # OAuth2 resource server
    └── RouteConfig.java              # Dynamic routing
```

### Product Service (Port 8081)

**Responsibility**: Product lifecycle management

```
domain/
├── model/
│   ├── aggregate/Product.java         # Aggregate root
│   ├── valueobject/
│   │   ├── ProductId.java
│   │   ├── ProductCode.java
│   │   └── Money.java
│   └── event/
│       ├── ProductCreated.java
│       ├── ProductUpdated.java
│       └── ProductDeleted.java
└── repository/ProductRepository.java  # Output port

application/
├── service/
│   ├── ProductCommandService.java     # Write operations
│   └── ProductQueryService.java       # Read operations
└── dto/ProductView.java

adapter/
├── inbound/rest/
│   ├── ProductCommandController.java
│   └── ProductQueryController.java
└── outbound/persistence/
    └── JpaProductRepository.java
```

### User Service (Port 8082)

**Responsibility**: User profile from JWT claims

```
application/
├── service/UserProfileService.java
└── dto/UserProfileView.java

adapter/inbound/rest/
└── UserController.java
```

## Branch Differences

### Main Branch (Spring AOP Audit)

```java
// Usage: Add annotation to method
@Auditable(eventType = AuditEventType.CREATE_PRODUCT)
public UUID handle(CreateProductCommand cmd) {
    // Business logic - audit captured automatically via AOP
}
```

### Domain-Event Branch (Domain Event Audit)

```java
// Usage: Publish domain events
public UUID handle(CreateProductCommand cmd) {
    Product product = Product.create(...);
    eventPublisher.publish(product.pullDomainEvents());
    // Audit listener captures ProductCreated event
}
```

> **IMPORTANT**: This architectural difference is immutable by design.

## Test Strategy

| Layer | Test Type | Framework |
|-------|-----------|-----------|
| Domain | Unit Tests | JUnit 5, AssertJ |
| Application | Integration Tests | Spring Boot Test |
| Adapter | Contract Tests | Spring Cloud Contract |
| E2E | BDD Scenarios | Cucumber |
| Architecture | Compliance | ArchUnit |

## Deployment

### Local Development

```bash
# Start infrastructure
docker compose -f deploy/docker/docker-compose.infra.yml up -d

# Start services
./gradlew :services:gateway-service:bootRun
./gradlew :services:product-service:bootRun
./gradlew :services:user-service:bootRun
```

### Kubernetes

```bash
# Apply manifests
kubectl apply -k deploy/k8s/base/

# Blue-green deployment
./deploy/scripts/blue-green-deploy.sh v1.1.0 green
./deploy/scripts/blue-green-switch.sh green
```

## Implementation Status

All planned features have been implemented. See [tasks.md](./tasks.md) for detailed task completion status.
