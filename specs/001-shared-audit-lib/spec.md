# Feature Specification: Shared Audit Library

**Feature Branch**: `001-shared-audit-lib`
**Created**: 2026-01-10
**Status**: Draft
**Input**: User description: "建立共用稽核函式庫，採用 AOP 機制與業務邏輯分離，可供不同微服務引用"

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Enable Audit on Business Operations (Priority: P1)

As a **microservice developer**, I want to enable audit logging on any business operation by simply marking it, so that audit concerns are completely separated from my business logic and I can focus on implementing features.

**Why this priority**: This is the core value proposition - separating audit from business logic. Without this, developers must manually write audit code in every operation, leading to code duplication and inconsistent audit trails.

**Independent Test**: Can be fully tested by marking a single operation as auditable and verifying that the audit log is automatically captured without modifying any business logic code.

**Acceptance Scenarios**:

1. **Given** a business operation is marked as auditable, **When** the operation executes successfully, **Then** an audit log entry is automatically created with operation details, executor identity, timestamp, and result.

2. **Given** a business operation is marked as auditable, **When** the operation fails with an error, **Then** an audit log entry is automatically created with the failure details and error information.

3. **Given** a business operation is NOT marked as auditable, **When** the operation executes, **Then** no audit log entry is created.

---

### User Story 2 - Integrate Shared Audit Library into Microservice (Priority: P2)

As a **microservice developer**, I want to add the shared audit library as a dependency to my service, so that I can immediately use audit capabilities without reimplementing the audit mechanism.

**Why this priority**: Reusability is essential for a multi-microservice architecture. Each service should be able to adopt auditing with minimal configuration effort.

**Independent Test**: Can be fully tested by adding the library to a new microservice and configuring it to connect to an audit storage, then verifying audit logs are captured.

**Acceptance Scenarios**:

1. **Given** a new microservice project, **When** the shared audit library is added as a dependency, **Then** the service can use all audit marking capabilities after providing minimal configuration.

2. **Given** the audit library is integrated, **When** the service starts, **Then** the audit mechanism activates automatically without explicit initialization code in business logic.

3. **Given** multiple microservices use the same audit library, **When** each service captures audit logs, **Then** all logs follow the same standardized format for consistent analysis.

---

### User Story 3 - Query Audit Trail for Compliance (Priority: P3)

As an **auditor or compliance officer**, I want to query the audit trail by various criteria, so that I can investigate specific operations, users, or time periods for compliance and security reviews.

**Why this priority**: The audit data must be queryable to provide value. However, this depends on audit logs being captured first (P1) and stored consistently (P2).

**Independent Test**: Can be fully tested by querying audit logs filtered by date range, user, service name, or operation type, and verifying correct results are returned.

**Acceptance Scenarios**:

1. **Given** audit logs exist in the system, **When** a query is made for a specific user, **Then** all audit entries for that user are returned in chronological order.

2. **Given** audit logs exist from multiple services, **When** a query is made for a specific time range, **Then** all audit entries within that range are returned regardless of originating service.

3. **Given** audit logs exist, **When** a query is made for a specific operation type, **Then** all audit entries matching that operation type are returned.

---

### User Story 4 - Configure Audit Behavior Per Service (Priority: P4)

As a **microservice developer**, I want to configure which operations are audited and what information is captured, so that I can control audit verbosity and exclude sensitive data from audit logs.

**Why this priority**: Configuration flexibility is important but secondary to core functionality. Services may have different compliance requirements or performance constraints.

**Independent Test**: Can be fully tested by configuring audit rules for a service and verifying that only configured operations are audited with the specified information.

**Acceptance Scenarios**:

1. **Given** audit configuration specifies certain operations to exclude, **When** those operations execute, **Then** no audit log is created for them.

2. **Given** audit configuration specifies fields to mask, **When** an audit log is created, **Then** sensitive fields are masked with partial visibility (e.g., `"password": "****"`, `"creditCard": "****-****-****-1234"`) preserving format while hiding actual values.

3. **Given** audit configuration is changed, **When** subsequent operations execute, **Then** the new configuration is applied without requiring service restart.

---

### Edge Cases

- What happens when the audit storage is unavailable? The business operation MUST complete successfully; audit failure MUST NOT block business operations. Audit failures should be logged locally and optionally retried.
- What happens when audit payload exceeds size limits? The payload MUST be truncated at 64 KB maximum with a marker indicating truncation occurred.
- What happens when the executor identity cannot be determined? A default "SYSTEM" or "ANONYMOUS" identity MUST be used with a flag indicating identity was not available.
- What happens when multiple operations occur in the same transaction? Each operation MUST have its own audit entry with a correlation ID linking related operations.
- What happens when circular references exist in audit payload? Circular references MUST be detected and handled gracefully (e.g., replaced with reference markers).

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: The audit library MUST automatically capture audit information when a business operation is marked as auditable, without requiring explicit audit code in the business logic.
- **FR-002**: The audit library MUST capture the following information for each audited operation: unique audit ID, timestamp, operation type/name, aggregate type, aggregate ID, executor identity (username), originating service name, operation payload, operation result (success/failure), and client IP address.
- **FR-003**: The audit library MUST be packagable as a standalone, reusable component that different microservices can include as a dependency.
- **FR-004**: The audit library MUST provide a declarative mechanism to mark operations as auditable (e.g., annotation, attribute, or decorator pattern).
- **FR-005**: The audit library MUST NOT cause business operations to fail when audit capture fails; audit failures MUST be isolated from business logic execution.
- **FR-006**: The audit library MUST support configuration of audit behavior including: which operations to audit, which fields to capture or mask (using partial-visibility masking that preserves format), and audit storage destination.
- **FR-007**: The audit library MUST produce audit logs in a standardized, structured format suitable for querying and analysis.
- **FR-008**: The audit library MUST capture audit information for both successful and failed operations, including failure reason for failed operations.
- **FR-009**: The audit library MUST support correlation of related audit entries (e.g., operations within the same business transaction) via correlation identifiers.
- **FR-010**: The audit library MUST preserve the separation between audit infrastructure and domain/application layers as per hexagonal architecture principles.
- **FR-011**: The audit library MUST enforce append-only semantics for audit logs; updates and deletions of existing audit entries MUST NOT be permitted to ensure audit integrity.
- **FR-012**: The audit library MUST expose metrics and health indicators including: audit failure count, capture latency, and processing queue depth to enable operational monitoring.

### Key Entities

- **AuditLog**: Represents a single audit trail entry. Contains audit ID, timestamp, event type, aggregate information, executor identity, service name, action name, payload, result status, and client IP.
- **AuditEvent**: Represents the type of auditable event (e.g., PRODUCT_CREATED, USER_PROFILE_UPDATED, ROLE_ASSIGNED). Used for categorization and filtering.
- **AuditConfiguration**: Represents service-specific audit settings including which operations to audit, fields to mask, and storage configuration.
- **AuditableOperation**: Represents the marking/metadata applied to a business operation indicating it should be audited, including the event type and custom payload specifications.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: Developers can enable auditing on a new business operation within 5 minutes by adding a single marker/declaration, without writing any audit-specific code.
- **SC-002**: A new microservice can integrate the audit library and have functional audit logging within 30 minutes of starting integration.
- **SC-003**: 100% of audited operations produce correctly formatted audit logs that pass schema validation.
- **SC-004**: Audit capture adds less than 50ms latency to business operations under normal load (audit storage available).
- **SC-005**: Business operation success rate remains at 100% even when audit storage is unavailable (audit failures do not block business operations).
- **SC-006**: Audit logs from all integrated microservices can be queried together using the same query interface and produce consistent results.
- **SC-007**: Zero audit-related code exists in domain layer classes; all audit logic resides in infrastructure layer.
- **SC-008**: Auditors can retrieve relevant audit entries for any given user, time period, or operation type within 5 seconds for queries spanning up to 1 million audit records.
- **SC-009**: The audit system MUST sustain 100-500 audit events per second write throughput across all integrated microservices without degradation.

## Clarifications

### Session 2026-01-10

- Q: Should audit logs be tamper-evident/immutable? → A: Append-only storage (no updates or deletes permitted)
- Q: Maximum audit payload size limit? → A: 64 KB maximum
- Q: Masking strategy for sensitive fields? → A: Field-level masking with partial visibility (show format, mask value)
- Q: Expected audit write volume? → A: Medium volume: 100-500 events/second
- Q: How to monitor the audit library itself? → A: Expose metrics and health indicators (failure count, latency, queue depth)

## Assumptions

- The platform uses a common authentication mechanism that provides executor identity (username) through a standard context (e.g., security context, JWT claims).
- Each microservice has access to a shared or federated audit storage mechanism.
- The audit storage solution supports structured queries on the defined audit log fields.
- Services operate in a request-response pattern where client IP and user context are available during operation execution.
- The existing hexagonal architecture constitution (Principle VII: Dependency Inversion & Framework Isolation) governs how the audit library integrates with services.

## Dependencies

- Authentication/authorization system must provide current user identity.
- Network infrastructure must provide client IP information.
- A central or federated audit storage solution must be available.
- Constitution principles (especially hexagonal architecture and SOLID) must be followed in implementation.

## Out of Scope

- Real-time audit alerting or notification systems.
- Audit log archival and retention policies (infrastructure concern).
- Specific audit storage technology selection (to be determined during planning).
- User interface for audit log visualization (separate feature).
- Audit log encryption at rest (infrastructure security concern).
