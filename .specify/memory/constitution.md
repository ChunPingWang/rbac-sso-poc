<!--
================================================================================
SYNC IMPACT REPORT
================================================================================
Version Change: 0.0.0 → 1.0.0 (MAJOR - Initial constitution ratification)

Modified Principles: N/A (Initial creation)

Added Sections:
- Core Principles (7 principles)
  - I. Hexagonal Architecture
  - II. Domain-Driven Design (DDD)
  - III. SOLID Principles
  - IV. Test-Driven Development (TDD)
  - V. Behavior-Driven Development (BDD)
  - VI. Code Quality Standards
  - VII. Dependency Inversion & Framework Isolation
- Architecture Constraints
- Development Workflow
- Governance

Removed Sections: N/A (Initial creation)

Templates Requiring Updates:
- .specify/templates/plan-template.md: ✅ Compatible (Constitution Check section exists)
- .specify/templates/spec-template.md: ✅ Compatible (BDD scenarios supported)
- .specify/templates/tasks-template.md: ✅ Compatible (Test-first workflow supported)

Follow-up TODOs: None
================================================================================
-->

# RBAC-SSO-POC Constitution

## Core Principles

### I. Hexagonal Architecture (Ports & Adapters)

The system MUST be organized in concentric layers following the Hexagonal Architecture pattern:

- **Domain Layer (innermost)**: Contains business logic, domain entities, value objects, and domain services. This layer MUST have zero dependencies on frameworks or external libraries.
- **Application Layer**: Contains use cases and application services that orchestrate domain logic. This layer defines ports (interfaces) for external communication.
- **Infrastructure Layer (outermost)**: Contains adapters that implement ports, including frameworks, databases, external APIs, and UI components.

**Dependency Rule**: Dependencies MUST point inward only. Inner layers MUST NOT depend on outer layers. Outer layers depend on inner layers through well-defined ports (interfaces).

**Rationale**: This architecture ensures the domain logic remains framework-agnostic, testable in isolation, and adaptable to technology changes without affecting business rules.

### II. Domain-Driven Design (DDD)

All feature development MUST follow Domain-Driven Design principles:

- **Ubiquitous Language**: Code MUST use domain terminology consistently. Class names, method names, and variables MUST reflect the business domain vocabulary.
- **Bounded Contexts**: Each module MUST have clear boundaries with explicit context mapping to other modules.
- **Aggregates**: Related entities MUST be grouped into aggregates with a single aggregate root controlling access and ensuring invariants.
- **Value Objects**: Immutable domain concepts without identity MUST be implemented as value objects.
- **Domain Events**: Significant domain state changes MUST be expressed as domain events.
- **Repositories**: Data access MUST be abstracted through repository interfaces defined in the domain layer, implemented in infrastructure.

**Rationale**: DDD ensures the codebase accurately models the business domain, making it easier to understand, maintain, and evolve with changing requirements.

### III. SOLID Principles

All code MUST adhere to SOLID principles:

- **Single Responsibility Principle (SRP)**: Each class/module MUST have only one reason to change. Responsibilities MUST be clearly separated.
- **Open/Closed Principle (OCP)**: Code MUST be open for extension but closed for modification. New behavior SHOULD be added through composition or inheritance, not by modifying existing code.
- **Liskov Substitution Principle (LSP)**: Subtypes MUST be substitutable for their base types without altering program correctness.
- **Interface Segregation Principle (ISP)**: Clients MUST NOT be forced to depend on interfaces they do not use. Interfaces MUST be small and focused.
- **Dependency Inversion Principle (DIP)**: High-level modules MUST NOT depend on low-level modules. Both MUST depend on abstractions. Abstractions MUST NOT depend on details.

**Rationale**: SOLID principles produce code that is maintainable, extensible, and resistant to cascading changes.

### IV. Test-Driven Development (TDD) (NON-NEGOTIABLE)

All implementation MUST follow the TDD cycle:

1. **Red**: Write a failing test that defines desired behavior BEFORE writing implementation code.
2. **Green**: Write the minimum code necessary to make the test pass.
3. **Refactor**: Improve code structure while keeping tests green.

**Mandatory Requirements**:
- Tests MUST be written and verified to fail before any implementation begins.
- No production code may be written without a corresponding failing test.
- Test coverage MUST be maintained at a minimum of 80% for domain and application layers.
- Unit tests MUST test behavior, not implementation details.
- Tests MUST be fast, isolated, and repeatable.

**Rationale**: TDD ensures code is designed for testability, catches defects early, and provides living documentation of system behavior.

### V. Behavior-Driven Development (BDD)

User-facing features MUST be specified using BDD format:

- **Given-When-Then**: All acceptance criteria MUST be expressed as Given-When-Then scenarios.
- **Executable Specifications**: BDD scenarios SHOULD be automated as integration or acceptance tests where feasible.
- **Ubiquitous Language**: Scenarios MUST use domain language that stakeholders understand.

**Scenario Format**:
```gherkin
Given [initial context/precondition]
When [action/event occurs]
Then [expected outcome/postcondition]
```

**Rationale**: BDD bridges communication between technical and non-technical stakeholders, ensuring features meet actual user needs.

### VI. Code Quality Standards

All code MUST meet these quality standards:

- **Naming**: Names MUST be descriptive, intention-revealing, and consistent with domain language.
- **Functions/Methods**: MUST be small (< 20 lines preferred), do one thing, and operate at a single level of abstraction.
- **Classes**: MUST be cohesive with clear responsibilities. Favor composition over inheritance.
- **Comments**: Code SHOULD be self-documenting. Comments are reserved for explaining "why," not "what."
- **Error Handling**: Errors MUST be handled explicitly. Use domain-specific exceptions. Never swallow exceptions silently.
- **No Magic Numbers/Strings**: All literals MUST be named constants or configuration values.
- **DRY (Don't Repeat Yourself)**: Duplication MUST be eliminated through proper abstraction.
- **YAGNI (You Aren't Gonna Need It)**: Do NOT implement functionality until it is needed.

**Rationale**: High code quality reduces cognitive load, prevents bugs, and makes the codebase sustainable long-term.

### VII. Dependency Inversion & Framework Isolation

Frameworks and external dependencies MUST be isolated in the infrastructure layer:

- **Ports**: The application layer MUST define interfaces (ports) for all external interactions (databases, APIs, messaging, etc.).
- **Adapters**: The infrastructure layer MUST implement adapters that fulfill port contracts.
- **Framework Code**: All framework-specific code (web frameworks, ORM, etc.) MUST reside in infrastructure layer only.
- **Dependency Injection**: Dependencies MUST be injected through constructors or factory methods, never instantiated directly in domain/application code.
- **Anti-Corruption Layer**: When integrating with external systems, an anti-corruption layer MUST translate external models to domain models.

**Rationale**: Framework isolation allows the core business logic to remain stable regardless of framework upgrades, replacements, or technology migrations.

## Architecture Constraints

### Layer Structure

```
┌─────────────────────────────────────────────────────────────┐
│                    Infrastructure Layer                      │
│  (Frameworks, DB Adapters, API Controllers, External APIs)   │
│  ┌─────────────────────────────────────────────────────────┐│
│  │                  Application Layer                       ││
│  │         (Use Cases, Application Services, DTOs)          ││
│  │  ┌─────────────────────────────────────────────────────┐││
│  │  │                  Domain Layer                       │││
│  │  │  (Entities, Value Objects, Domain Services, Ports)  │││
│  │  └─────────────────────────────────────────────────────┘││
│  └─────────────────────────────────────────────────────────┘│
└─────────────────────────────────────────────────────────────┘
```

### Directory Structure Convention

```
src/
├── domain/              # Domain layer
│   ├── entities/        # Domain entities and aggregates
│   ├── value_objects/   # Value objects
│   ├── services/        # Domain services
│   ├── ports/           # Port interfaces (repositories, external services)
│   └── events/          # Domain events
├── application/         # Application layer
│   ├── use_cases/       # Application use cases
│   ├── services/        # Application services
│   └── dto/             # Data Transfer Objects
└── infrastructure/      # Infrastructure layer
    ├── adapters/        # Port implementations
    ├── persistence/     # Database implementations
    ├── web/             # Web framework controllers
    └── external/        # External API clients

tests/
├── unit/                # Unit tests (domain + application)
├── integration/         # Integration tests
└── contract/            # Contract tests
```

### Allowed Dependencies

| Layer          | May Depend On                    | MUST NOT Depend On          |
|----------------|----------------------------------|-----------------------------|
| Domain         | Standard library only            | Application, Infrastructure |
| Application    | Domain                           | Infrastructure              |
| Infrastructure | Application, Domain              | -                           |

## Development Workflow

### Feature Development Process

1. **Specification**: Define feature using BDD scenarios (Given-When-Then format).
2. **Domain Modeling**: Identify entities, value objects, and aggregates required.
3. **Port Definition**: Define interfaces for external dependencies in domain/application layer.
4. **TDD Implementation**:
   - Write failing unit tests for domain logic
   - Implement domain logic to pass tests
   - Write failing integration tests for use cases
   - Implement application services
   - Write failing contract tests for adapters
   - Implement infrastructure adapters
5. **Refactoring**: Improve code structure while maintaining green tests.
6. **Review**: Verify compliance with constitution principles.

### Code Review Checklist

All pull requests MUST verify:
- [ ] Tests written before implementation (TDD)
- [ ] BDD scenarios defined for user-facing features
- [ ] Hexagonal architecture layers respected
- [ ] No framework dependencies in domain/application layers
- [ ] SOLID principles followed
- [ ] DDD patterns applied appropriately
- [ ] Code quality standards met
- [ ] Test coverage maintained (>= 80% for domain/application)

## Governance

### Amendment Process

1. Propose amendment with clear rationale and impact analysis.
2. Document migration plan for existing code if applicable.
3. Obtain team consensus through documented review.
4. Update constitution version following semantic versioning.
5. Propagate changes to dependent templates and documentation.

### Versioning Policy

- **MAJOR**: Backward-incompatible changes to principles or removal of principles.
- **MINOR**: New principles added or existing principles materially expanded.
- **PATCH**: Clarifications, typos, or non-semantic refinements.

### Compliance Review

- All PRs MUST include constitution compliance verification.
- Quarterly architecture reviews SHOULD assess overall constitution adherence.
- Violations MUST be justified in the Complexity Tracking section of implementation plans.

**Version**: 1.0.0 | **Ratified**: 2026-01-10 | **Last Amended**: 2026-01-10
