# Feature Specification: Multi-Tenant E-Commerce Platform

**Feature Branch**: `main` / `domain-event-for-audit`
**Created**: 2026-01-10
**Status**: Implemented
**Input**: Multi-tenant e-commerce platform with RBAC, SSO, and Keycloak LDAP integration

## Overview

This specification describes the multi-tenant e-commerce platform PoC that demonstrates:
- Hexagonal Architecture (Ports & Adapters)
- Domain-Driven Design (DDD) with Aggregates and Value Objects
- CQRS (Command Query Responsibility Segregation)
- Multi-tenant data isolation
- Role-Based Access Control (RBAC) with Spring Security
- SSO/OAuth2/OIDC with Keycloak
- LDAP integration for user authentication

## Branch Strategy

| Branch | Audit Mechanism | Description |
|--------|-----------------|-------------|
| `main` | Spring AOP | Uses `@Auditable` annotation for automatic interception |
| `domain-event-for-audit` | Domain Events | Uses domain event publishing for fine-grained control |

> **IMPORTANT**: The audit mechanism difference between branches is an immutable design decision.

---

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Product Management (Priority: P1)

As a **product administrator**, I want to manage products within my tenant, so that I can maintain my product catalog independently from other tenants.

**Acceptance Scenarios**:

1. **Given** I am authenticated as ADMIN role, **When** I create a product, **Then** the product is created with my tenant ID and an audit log is recorded.

2. **Given** I am authenticated as ADMIN role, **When** I update a product in my tenant, **Then** the product is updated and a ProductUpdated domain event is published.

3. **Given** I am authenticated as ADMIN role, **When** I delete a product, **Then** the product status changes to DELETED (soft delete) and remains queryable for audit purposes.

4. **Given** I am authenticated as USER role, **When** I try to create a product, **Then** I receive a 403 Forbidden response.

---

### User Story 2 - Multi-Tenant Data Isolation (Priority: P1)

As a **tenant user**, I want to only see data belonging to my tenant, so that data privacy is maintained across the platform.

**Acceptance Scenarios**:

1. **Given** I belong to tenant-A, **When** I query products, **Then** I only see products belonging to tenant-A.

2. **Given** I am a system administrator (tenant=system), **When** I query products, **Then** I can see products from all tenants.

3. **Given** tenant context is not set, **When** I access any resource, **Then** the default tenant is used.

---

### User Story 3 - RBAC Authorization (Priority: P1)

As a **platform administrator**, I want role-based access control, so that users can only perform actions they are authorized for.

**Acceptance Scenarios**:

1. **Given** a user with ADMIN role, **When** accessing admin endpoints, **Then** access is granted.

2. **Given** a user with USER role, **When** accessing admin endpoints, **Then** access is denied with 403 status.

3. **Given** an unauthenticated request, **When** accessing protected endpoints, **Then** response is 401 Unauthorized.

4. **Given** a valid JWT token, **When** the token is expired, **Then** access is denied.

---

### User Story 4 - SSO with Keycloak (Priority: P2)

As a **user**, I want to authenticate via Keycloak, so that I can use single sign-on across services.

**Acceptance Scenarios**:

1. **Given** valid credentials in LDAP, **When** I authenticate via Keycloak, **Then** I receive JWT tokens with my roles and tenant information.

2. **Given** I am authenticated, **When** I access my profile, **Then** I see my username, email, roles, and groups from the JWT claims.

3. **Given** my LDAP group is mapped to Keycloak role, **When** I authenticate, **Then** my JWT contains the corresponding role.

---

### User Story 5 - Audit Logging (Priority: P2)

As an **auditor**, I want all important operations logged, so that I can review the audit trail for compliance.

**Acceptance Scenarios**:

1. **Given** a product is created, **When** the operation completes, **Then** an audit log is created with username, tenant, timestamp, and payload.

2. **Given** an operation fails, **When** the failure is captured, **Then** the audit log records the failure reason.

3. **Given** sensitive data in payload, **When** audit log is created, **Then** sensitive fields are masked (password, credit card, etc.).

---

### User Story 6 - mTLS Service Security (Priority: P2)

As a **platform operator**, I want service-to-service communication secured with mTLS, so that internal traffic is encrypted and mutually authenticated.

**Acceptance Scenarios**:

1. **Given** mTLS profile is enabled, **When** services communicate, **Then** connections use TLS 1.2/1.3 with mutual certificate verification.

2. **Given** cert-manager is deployed, **When** certificates are requested, **Then** all service certificates are signed by the internal CA and marked Ready.

3. **Given** a pod is running with mTLS, **When** health probes execute, **Then** they succeed via the HTTP management port (not HTTPS).

4. **Given** mTLS is enabled, **When** an unauthorized service attempts connection, **Then** the TLS handshake fails and connection is rejected.

**Verification Results (2026-01-11)**:

| Component | Status | Notes |
|-----------|:------:|-------|
| cert-manager | ✅ | v1.14.0 deployed |
| CA Issuer | ✅ | rbac-sso-ca-issuer Ready |
| Certificates | ✅ | All 4 certificates Ready |
| Pods | ✅ | All mTLS pods Running |
| Health Probes | ✅ | HTTP management port works |

---

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: The platform MUST support multi-tenant data isolation with tenant ID propagation via JWT claims.
- **FR-002**: The platform MUST enforce RBAC using Spring Security `@PreAuthorize` annotations.
- **FR-003**: The platform MUST integrate with Keycloak for OAuth2/OIDC authentication.
- **FR-004**: The platform MUST support LDAP user federation via Keycloak.
- **FR-005**: Product operations MUST publish domain events (ProductCreated, ProductUpdated, ProductDeleted, ProductPriceChanged).
- **FR-006**: All write operations MUST be audited with operator identity and timestamp.
- **FR-007**: The API Gateway MUST route requests to appropriate microservices based on path patterns.
- **FR-008**: The platform MUST follow hexagonal architecture with clear separation of domain, application, and infrastructure layers.

### Non-Functional Requirements

- **NFR-001**: API response time (P95) MUST be < 500ms.
- **NFR-002**: System MUST handle 100-500 concurrent users.
- **NFR-003**: Audit writes MUST not block business operations.
- **NFR-004**: Test coverage for domain layer MUST exceed 90%.

---

## Key Entities

### Product Aggregate (Product Service)

- **Product**: Aggregate root with ProductId, ProductCode, name, price (Money), category, status, tenantId
- **ProductId**: Value object wrapping UUID
- **ProductCode**: Value object with format validation (P followed by 6 digits)
- **Money**: Value object for monetary amounts with arithmetic operations
- **ProductStatus**: Enum (ACTIVE, INACTIVE, DELETED)

### User Context (User Service)

- **UserProfileView**: DTO containing username, email, firstName, lastName, tenantId, roles, groups

### Audit (Shared Library)

- **AuditLog**: Aggregate root for audit entries
- **AuditEventType**: Enum for event categorization
- **AuditResult**: Enum (SUCCESS, FAILURE)

---

## Success Criteria *(mandatory)*

| Criteria | Target | Measurement |
|----------|--------|-------------|
| Unit Test Count | > 200 | Gradle test report |
| Test Pass Rate | 100% | CI/CD pipeline |
| Architecture Compliance | 100% | ArchUnit tests |
| Cucumber Scenarios | All pass | BDD test runner |
| API Response Time (P95) | < 500ms | Actuator metrics |

---

## Implementation Status

### Completed Features

- [x] Hexagonal Architecture for all services
- [x] DDD with Aggregates, Value Objects, Domain Events
- [x] CQRS pattern with separate Command and Query services
- [x] Multi-tenant context propagation
- [x] RBAC with Spring Security @PreAuthorize
- [x] OAuth2/OIDC with Keycloak integration
- [x] LDAP user federation
- [x] Audit logging (Spring AOP in main, Domain Events in domain-event-for-audit)
- [x] Comprehensive unit tests (300+ tests)
- [x] Cucumber BDD tests (18 scenarios)
- [x] API Gateway with route configuration
- [x] mTLS East-West security with cert-manager

### Test Statistics

| Category | Count |
|----------|-------|
| Total Tests | 300 |
| Failures | 0 |
| Product Service Tests | ~100 |
| User Service Tests | ~20 |
| Gateway Service Tests | ~10 |
| Audit Lib Tests | ~150 |
| Cucumber Scenarios | 18 |

---

## Dependencies

- Keycloak 24.0+ for SSO
- OpenLDAP 1.5+ for user directory
- PostgreSQL 15+ (production) / H2 (development)
- Spring Boot 3.3.x
- Spring Cloud Gateway 4.1.x
- Java 17 (LTS)

---

## Out of Scope

- Order management and checkout flow
- Payment processing
- Inventory management (detailed)
- Real-time notifications
- Mobile application
