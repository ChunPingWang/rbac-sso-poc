# Tasks: Shared Audit Library

**Input**: Design documents from `/specs/001-shared-audit-lib/`
**Prerequisites**: plan.md (required), spec.md (required), research.md, data-model.md, contracts/

**Tests**: Required per Constitution Principle IV (TDD is NON-NEGOTIABLE).

**Organization**: Tasks are grouped by user story to enable independent implementation and testing of each story.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: Which user story this task belongs to (e.g., US1, US2, US3, US4)
- Include exact file paths in descriptions

## Path Conventions

Based on plan.md structure:
- **Library root**: `libs/audit-lib/`
- **Main source**: `libs/audit-lib/src/main/java/com/example/audit/`
- **Test source**: `libs/audit-lib/src/test/java/com/example/audit/`

---

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Project initialization and Gradle module structure

- [ ] T001 Create `libs/audit-lib/` directory structure per plan.md
- [ ] T002 Create `libs/audit-lib/build.gradle` with Spring Boot starter-aop, starter-data-jpa, micrometer-core dependencies
- [ ] T003 [P] Create `libs/audit-lib/src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports` for auto-configuration
- [ ] T004 [P] Update root `settings.gradle` to include `libs:audit-lib` module

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Core infrastructure that MUST be complete before ANY user story can be implemented

**⚠️ CRITICAL**: No user story work can begin until this phase is complete

- [ ] T005 Create `AuditResult` enum in `libs/audit-lib/src/main/java/com/example/audit/domain/model/AuditResult.java`
- [ ] T006 [P] Create `AuditLogId` value object in `libs/audit-lib/src/main/java/com/example/audit/domain/model/AuditLogId.java`
- [ ] T007 [P] Create `AuditEventType` value object in `libs/audit-lib/src/main/java/com/example/audit/domain/model/AuditEventType.java`
- [ ] T008 Create `AuditLog` domain entity (aggregate root) with builder in `libs/audit-lib/src/main/java/com/example/audit/domain/model/AuditLog.java`
- [ ] T009 Create `AuditLogRepository` interface (output port, append-only) in `libs/audit-lib/src/main/java/com/example/audit/domain/port/AuditLogRepository.java`

**Checkpoint**: Foundation ready - domain model complete, user story implementation can now begin

---

## Phase 3: User Story 1 - Enable Audit on Business Operations (Priority: P1) 🎯 MVP

**Goal**: Implement AOP-based audit capture with `@Auditable` annotation that automatically logs operations without modifying business logic

**Independent Test**: Mark a single test method with `@Auditable`, execute it, and verify an audit log is automatically captured with correct details

### Tests for User Story 1 ⚠️

> **NOTE: Write these tests FIRST, ensure they FAIL before implementation**

- [ ] T010 [P] [US1] Unit test for AuditLog builder validation in `libs/audit-lib/src/test/java/com/example/audit/unit/domain/AuditLogTest.java`
- [ ] T011 [P] [US1] Unit test for PayloadProcessor serialization/truncation in `libs/audit-lib/src/test/java/com/example/audit/unit/processor/PayloadProcessorTest.java`
- [ ] T012 [P] [US1] Unit test for AuditAspect success/failure capture in `libs/audit-lib/src/test/java/com/example/audit/unit/aspect/AuditAspectTest.java`
- [ ] T013 [US1] Integration test for end-to-end audit capture in `libs/audit-lib/src/test/java/com/example/audit/integration/AuditIntegrationTest.java`

### Implementation for User Story 1

- [ ] T014 [P] [US1] Create `@Auditable` annotation in `libs/audit-lib/src/main/java/com/example/audit/annotation/Auditable.java`
- [ ] T015 [P] [US1] Create `AuditContextHolder` for username/IP/correlationId extraction (from SecurityContext, RequestContext, MDC) in `libs/audit-lib/src/main/java/com/example/audit/infrastructure/context/AuditContextHolder.java`
- [ ] T016 [P] [US1] Create `PayloadProcessor` with Jackson serialization in `libs/audit-lib/src/main/java/com/example/audit/infrastructure/processor/PayloadProcessor.java`
- [ ] T017 [US1] Create `AuditAspect` with `@Around` advice in `libs/audit-lib/src/main/java/com/example/audit/infrastructure/aspect/AuditAspect.java`
- [ ] T018 [US1] Implement try-finally pattern in AuditAspect to ensure business operations never fail due to audit errors
- [ ] T019 [US1] Add 64KB payload truncation logic with `_truncated` marker in PayloadProcessor
- [ ] T020 [US1] Add circular reference handling in PayloadProcessor using Jackson's `SerializationFeature.FAIL_ON_SELF_REFERENCES`

**Checkpoint**: At this point, User Story 1 should be fully functional - `@Auditable` annotation captures audit logs automatically

---

## Phase 4: User Story 2 - Integrate Shared Audit Library into Microservice (Priority: P2)

**Goal**: Package the library as a reusable dependency with auto-configuration that microservices can include with minimal setup

**Independent Test**: Add `implementation project(':libs:audit-lib')` to a test microservice, start it, and verify audit mechanism activates automatically

### Tests for User Story 2 ⚠️

- [ ] T021 [P] [US2] Contract test for AuditLogRepository in `libs/audit-lib/src/test/java/com/example/audit/contract/AuditLogRepositoryContractTest.java`
- [ ] T022 [P] [US2] Integration test for auto-configuration activation in `libs/audit-lib/src/test/java/com/example/audit/integration/AutoConfigurationTest.java`

### Implementation for User Story 2

- [ ] T023 [P] [US2] Create `AuditLogJpaEntity` with `@Immutable` in `libs/audit-lib/src/main/java/com/example/audit/infrastructure/persistence/entity/AuditLogJpaEntity.java`
- [ ] T024 [P] [US2] Create entity mapper between domain and JPA in `libs/audit-lib/src/main/java/com/example/audit/infrastructure/persistence/mapper/AuditLogMapper.java`
- [ ] T025 [US2] Create `JpaAuditLogRepository` adapter (append-only) in `libs/audit-lib/src/main/java/com/example/audit/infrastructure/persistence/JpaAuditLogRepository.java`
- [ ] T026 [US2] Create `SpringDataAuditLogRepository` interface in `libs/audit-lib/src/main/java/com/example/audit/infrastructure/persistence/SpringDataAuditLogRepository.java`
- [ ] T027 [US2] Create `AuditAutoConfiguration` with `@ConditionalOnProperty` in `libs/audit-lib/src/main/java/com/example/audit/infrastructure/config/AuditAutoConfiguration.java`
- [ ] T028 [US2] Create `AuditProperties` for `audit.*` configuration binding in `libs/audit-lib/src/main/java/com/example/audit/infrastructure/config/AuditProperties.java`
- [ ] T029 [US2] Create Flyway migration `V1__create_audit_logs_table.sql` in `libs/audit-lib/src/main/resources/db/migration/`
- [ ] T030 [P] [US2] Create `AuditMetrics` with Micrometer counters/timers in `libs/audit-lib/src/main/java/com/example/audit/infrastructure/metrics/AuditMetrics.java`
- [ ] T031 [P] [US2] Create `AuditHealthIndicator` in `libs/audit-lib/src/main/java/com/example/audit/infrastructure/health/AuditHealthIndicator.java`

**Checkpoint**: At this point, User Story 2 should be complete - any microservice can add the library and get auditing with minimal config

---

## Phase 5: User Story 3 - Query Audit Trail for Compliance (Priority: P3)

**Goal**: Provide query capabilities for auditors to search audit logs by user, time range, service, or operation type

**Independent Test**: Insert sample audit logs and query by username/time range/event type, verify correct results returned

### Tests for User Story 3 ⚠️

- [ ] T032 [P] [US3] Unit test for AuditQueryService query methods in `libs/audit-lib/src/test/java/com/example/audit/unit/application/AuditQueryServiceTest.java`
- [ ] T033 [US3] Integration test for query API endpoints in `libs/audit-lib/src/test/java/com/example/audit/integration/AuditQueryApiTest.java`

### Implementation for User Story 3

- [ ] T034 [P] [US3] Create `AuditLogView` DTO in `libs/audit-lib/src/main/java/com/example/audit/application/dto/AuditLogView.java`
- [ ] T035 [P] [US3] Create `PagedResponse` wrapper in `libs/audit-lib/src/main/java/com/example/audit/application/dto/PagedResponse.java`
- [ ] T036 [US3] Create `AuditQueryService` with paginated query methods in `libs/audit-lib/src/main/java/com/example/audit/application/service/AuditQueryService.java`
- [ ] T037 [US3] Create `AuditQueryController` REST endpoint per contracts/audit-api.yaml in `libs/audit-lib/src/main/java/com/example/audit/infrastructure/web/AuditQueryController.java`
- [ ] T038 [US3] Add database indexes for query performance in Flyway migration `V2__add_audit_indexes.sql`

**Checkpoint**: At this point, User Story 3 should be complete - auditors can query audit logs via REST API

---

## Phase 6: User Story 4 - Configure Audit Behavior Per Service (Priority: P4)

**Goal**: Enable per-service configuration for audit behavior including field masking and operation exclusion

**Independent Test**: Configure masking rules, execute audited operation with sensitive data, verify fields are masked in the log

### Tests for User Story 4 ⚠️

- [ ] T039 [P] [US4] Unit test for FieldMasker strategies in `libs/audit-lib/src/test/java/com/example/audit/unit/processor/FieldMaskerTest.java`
- [ ] T040 [US4] Integration test for configuration-driven masking in `libs/audit-lib/src/test/java/com/example/audit/integration/MaskingConfigurationTest.java`

### Implementation for User Story 4

- [ ] T041 [P] [US4] Create `FieldMasker` interface in `libs/audit-lib/src/main/java/com/example/audit/infrastructure/processor/FieldMasker.java`
- [ ] T042 [P] [US4] Create `PasswordFieldMasker` in `libs/audit-lib/src/main/java/com/example/audit/infrastructure/processor/maskers/PasswordFieldMasker.java`
- [ ] T043 [P] [US4] Create `CreditCardFieldMasker` with partial visibility in `libs/audit-lib/src/main/java/com/example/audit/infrastructure/processor/maskers/CreditCardFieldMasker.java`
- [ ] T044 [P] [US4] Create `EmailFieldMasker` with partial visibility in `libs/audit-lib/src/main/java/com/example/audit/infrastructure/processor/maskers/EmailFieldMasker.java`
- [ ] T045 [US4] Add `maskFields` support to `@Auditable` annotation and PayloadProcessor
- [ ] T046 [US4] Add `audit.masking.default-fields` configuration to AuditProperties
- [ ] T047 [US4] Implement dynamic configuration reload via `@ConfigurationProperties` + `@RefreshScope` (requires Spring Cloud Config or `/actuator/refresh` endpoint)

**Checkpoint**: All user stories should now be independently functional

---

## Phase 7: Polish & Cross-Cutting Concerns

**Purpose**: Improvements that affect multiple user stories

- [ ] T048 [P] Add ArchUnit test for hexagonal architecture compliance in `libs/audit-lib/src/test/java/com/example/audit/architecture/ArchitectureTest.java`
- [ ] T049 [P] Add JavaDoc documentation to all public APIs
- [ ] T050 Code cleanup and unused import removal
- [ ] T051 Run `./gradlew :libs:audit-lib:test` and verify all tests pass
- [ ] T052 Run quickstart.md validation - verify a sample microservice can integrate in <30 minutes

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: No dependencies - can start immediately
- **Foundational (Phase 2)**: Depends on Setup completion - BLOCKS all user stories
- **User Stories (Phase 3-6)**: All depend on Foundational phase completion
  - User stories can then proceed in parallel (if staffed)
  - Or sequentially in priority order (P1 → P2 → P3 → P4)
- **Polish (Phase 7)**: Depends on all desired user stories being complete

### User Story Dependencies

- **User Story 1 (P1)**: Can start after Foundational (Phase 2) - No dependencies on other stories
- **User Story 2 (P2)**: Can start after Foundational (Phase 2) - Integrates with US1 AuditAspect but independently testable
- **User Story 3 (P3)**: Can start after Foundational (Phase 2) - Uses repository from US2 but independently testable with mock
- **User Story 4 (P4)**: Can start after Foundational (Phase 2) - Extends PayloadProcessor from US1 but independently testable

### Within Each User Story

- Tests MUST be written and FAIL before implementation (TDD per Constitution)
- Value objects before entities
- Entities before repositories
- Services before endpoints
- Core implementation before integration
- Story complete before moving to next priority

### Parallel Opportunities

Within each phase, tasks marked [P] can run in parallel:

- **Phase 1**: T003, T004 parallel
- **Phase 2**: T006, T007 parallel (after T005)
- **Phase 3**: T010, T011, T012 parallel (tests); T014, T015, T016 parallel (implementation)
- **Phase 4**: T021, T022 parallel (tests); T023, T024, T030, T031 parallel
- **Phase 5**: T032, T033 parallel (tests); T034, T035 parallel
- **Phase 6**: T039, T040 parallel (tests); T041, T042, T043, T044 parallel
- **Phase 7**: T048, T049 parallel

---

## Parallel Example: User Story 1

```bash
# Launch all tests for User Story 1 together (TDD - write first, verify fail):
Task: T010 "Unit test for AuditLog builder validation"
Task: T011 "Unit test for PayloadProcessor serialization/truncation"
Task: T012 "Unit test for AuditAspect success/failure capture"

# After tests fail, launch parallel implementation tasks:
Task: T014 "Create @Auditable annotation"
Task: T015 "Create AuditContextHolder"
Task: T016 "Create PayloadProcessor"

# Sequential tasks (dependencies exist):
Task: T017 "Create AuditAspect" (depends on T014, T015, T016)
Task: T018 "Implement try-finally pattern" (depends on T017)
Task: T019 "Add 64KB truncation logic" (depends on T016)
Task: T020 "Add circular reference handling" (depends on T016)

# Run tests again - should now pass
Task: T013 "Integration test for end-to-end audit capture"
```

---

## Implementation Strategy

### MVP First (User Story 1 Only)

1. Complete Phase 1: Setup
2. Complete Phase 2: Foundational (CRITICAL - blocks all stories)
3. Complete Phase 3: User Story 1
4. **STOP and VALIDATE**: Test `@Auditable` annotation captures audit logs
5. MVP ready - basic audit capture works

### Incremental Delivery

1. Complete Setup + Foundational → Foundation ready
2. Add User Story 1 → Test independently → **MVP ready** (basic audit)
3. Add User Story 2 → Test independently → **Reusable library** (microservice integration)
4. Add User Story 3 → Test independently → **Queryable** (compliance queries)
5. Add User Story 4 → Test independently → **Configurable** (field masking)
6. Each story adds value without breaking previous stories

### Parallel Team Strategy

With multiple developers:

1. Team completes Setup + Foundational together
2. Once Foundational is done:
   - Developer A: User Story 1 (core audit capture)
   - Developer B: User Story 2 (packaging/config) - can use mocks for AuditAspect
   - Developer C: User Story 3 (query API) - can use mocks for repository
3. Stories complete and integrate independently
4. Developer D joins for User Story 4 (masking)

---

## Task Summary

| Phase | User Story | Task Count | Parallel Tasks |
|-------|------------|------------|----------------|
| Phase 1 | Setup | 4 | 2 |
| Phase 2 | Foundational | 5 | 2 |
| Phase 3 | US1 (P1) MVP | 11 | 6 |
| Phase 4 | US2 (P2) | 11 | 4 |
| Phase 5 | US3 (P3) | 7 | 3 |
| Phase 6 | US4 (P4) | 9 | 5 |
| Phase 7 | Polish | 5 | 2 |
| **Total** | | **52** | **24** |

---

## Notes

- [P] tasks = different files, no dependencies
- [Story] label maps task to specific user story for traceability
- Each user story should be independently completable and testable
- **TDD is mandatory** per Constitution Principle IV - verify tests fail before implementing
- Commit after each task or logical group
- Stop at any checkpoint to validate story independently
- All audit failures must NOT block business operations (FR-005)
- Append-only semantics enforced at repository level (FR-011)
