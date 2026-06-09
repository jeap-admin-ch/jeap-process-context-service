# AGENTS.md

This file provides guidance to coding agents when working with code in this repository.

## Project Overview

jEAP Process Context Service is an event-driven process observation library for microservices. It tracks distributed
business processes without central orchestration using JSON-based process templates (choreography pattern), Kafka event
streaming, and PostgreSQL persistence. Downstream projects create their own service instances by depending on
`jeap-process-context-service-instance` and implementing plugins via the plugin API.

It is based on Java 25 and Spring Boot 4.

## Build Commands

```bash
# Build entire project
./mvnw clean install

# Run tests
./mvnw clean test

# Build specific module
./mvnw clean install -pl jeap-process-context-scs

# Run tests for a single module
./mvnw test -pl jeap-process-context-repository-jpa

# Run single test class
./mvnw test -pl jeap-process-context-domain -Dtest=ProcessInstanceViewServiceTest

# Run single test method
./mvnw test -pl jeap-process-context-domain -Dtest=ProcessInstanceViewServiceTest#testSomething

# Skip tests
./mvnw clean install -DskipTests

# Build multiple specific modules (useful when stubs change)
./mvnw install -pl jeap-process-context-domain-test,jeap-process-context-repository-jpa -DskipTests
```

### Frontend (Angular)

```bash
cd jeap-process-context-ui

npm start                     # Development server at http://localhost:4200/
npm run build                 # Production build
npm run cypress:run           # Run Cypress component tests
npx cypress open --port 7000  # Interactive Cypress testing
```

## Architecture

**Hexagonal Architecture (Ports & Adapters)**:

- `jeap-process-context-domain/` - Core business logic, no Spring dependencies. Contains ProcessInstance,
  ProcessTemplate, Message entities and port interfaces (repositories, Transactions, MetricsListener).
- `jeap-process-context-domain-test/` - Test utilities with stub implementations (ProcessInstanceStubs,
  ProcessTemplateStubs, ProcessContextRepositoryFacadeStub)
- `jeap-process-context-plugin-api/` - Plugin interfaces for event extraction, correlation, filtering, and process
  completion conditions
- `jeap-process-context-message/` - Event builders for outbound events (e.g., ProcessSnapshotCreatedEventBuilder)
- `jeap-process-context-adapter-kafka/` - Kafka consumer with manual acknowledgment, message filtering, and dynamic
  listener creation per message type
- `jeap-process-context-adapter-rest-api/` - REST endpoints with `@TransactionalReadReplica` for reads and
  `@PreAuthorize` role-based security
- `jeap-process-context-adapter-micrometer/` - Prometheus metrics via `@Timed` annotations
- `jeap-process-context-adapter-objectstorage/` - Optional S3/MinIO adapter for snapshot storage (AWS SDK v2)
- `jeap-process-context-repository-jpa/` - JPA persistence with Flyway migrations and Caffeine caching
- `jeap-process-context-repository-template-json/` - JSON template storage with hash-based versioning
- `jeap-process-context-scs/` - Spring Boot application entry point (`@EnableAsync`)
- `jeap-process-context-service-instance/` - POM aggregator module; inheritance base for downstream projects to create
  their own instances with plugins
- `jeap-process-context-ui/` - Angular 20 frontend with Material Design and Oblique UI library

**Core Flow**: External microservices publish domain events to Kafka -> Process Context Service correlates events to
process definitions -> State updated in PostgreSQL -> UI visualizes process state.

## Key Domain Patterns

- **Pre-generated UUIDs**: Entities use `Generators.timeBasedEpochGenerator()` for IDs set at construction time. This
  means JPA's `isNew()` returns false, so `save()` calls `merge()` not `persist()`. **Always use the return value
  of `save()`** — `@PrePersist` callbacks fire on the managed copy, not the original object.
- **Transient template fields**: `ProcessInstance` and `TaskInstance` have `@Transient` fields (ProcessTemplate,
  TaskType) that are rehydrated after loading from persistence via `onAfterLoadFromPersistentState()` /
  `setTaskTypeFromTemplate()`.
- **Base entity classes**: `ImmutableDomainEntity` (sets createdAt via `@PrePersist`) and `MutableDomainEntity` (adds
  modifiedAt via `@PreUpdate` and `@Version` for optimistic locking).
- **Transaction boundaries**: Domain layer uses a `Transactions` port abstraction (
  `transactions.withinNewTransaction()`). REST reads use `@TransactionalReadReplica`.
- **No cascade relationships**: `@OneToMany(cascade=ALL)` fields have been removed. JPQL uses cross-entity joins (
  `FROM ProcessInstance p, ProcessData d WHERE d.processInstance = p`) instead of navigation joins. Save order matters:
  persist parent before child entities.

## Plugin API

Downstream projects implement these interfaces to customize behavior:

- `MessageCorrelationProvider<M>` - Maps messages to process instances via origin process IDs
- `ProcessInstantiationCondition<M>` - Controls whether a message creates a new process instance
- `ProcessCompletionCondition` - Determines when a process is complete (default:
  `AllTasksInFinalStateProcessCompletionCondition`)
- `MessageFilter<M>` - Filters messages before processing
- `PayloadExtractor<E>` / `ReferenceExtractor<E>` - Extract MessageData from event payloads/references

## Testing

- **Unit tests** (`*Test.java`): JUnit 5, Mockito, AssertJ. Domain tests have no Spring dependencies.
- **JPA tests**: `@DataJpaTest` with `@ContextConfiguration(classes = JpaAdapterConfig.class)`. Uses in PostgreSQL
  mode (`jdbc:h2:mem:testdb;DATABASE_TO_UPPER=FALSE;MODE=PostgreSQL`). Mock `ProcessTemplateRepository` and
  `ProcessContextFactory` with `@MockitoBean`.
- **Integration tests** (`*IT.java`): `@SpringBootTest` with TestContainers for PostgreSQL, Kafka, MinIO. Kafka tests
  extend `KafkaAdapterIntegrationTestBase` with `@ActiveProfiles("local")`.
- **Frontend tests**: Cypress component tests (`npm run cypress:run`), Jest for unit tests.
- **Test stubs**: `jeap-process-context-domain-test/` module. When stubs change, rebuild this module before running
  dependent tests.

## Database

- PostgreSQL for production, H2 (PostgreSQL mode) for tests
- Flyway migrations in `jeap-process-context-repository-jpa/src/main/resources/db/migration/`
    - `common/` - Cross-database migrations
    - `postgres/` - PostgreSQL-specific migrations
    - `h2/` - H2-specific migrations for test compatibility
- Migration naming: `V1_0_N__description-with-hyphens.sql` (e.g., `V1_0_45__create-pending-message-table.sql`)

## Local Development

Start dependencies via Docker Compose in the `docker/` directory (PostgreSQL on port 5555, Kafka on 9092, Schema
Registry on 7781). Run Spring Boot app with profiles `local,local-npm-ui`. Access UI at http://localhost:4200/.

## Versioning and Commits

- Commit Message: Use the JIRA ID from the branch name as a prefix (if available), do not use conventional commit
  messages. Example: `JIRA-1234 Implement feature X`.
- Semantic Versioning; all changes documented in [CHANGELOG.md](./CHANGELOG.md) (Keep a Changelog format).
- `setPomVersions.sh` updates the version across all module POMs.
- When working on a feature branch, increase the version to `x.y.z-SNAPSHOT` in the POMs.
- When bumping the version, also update the changelog, and update version/date in `publiccode.yml`.
- Changelog: Add a new section for the updated version, add a "### Changed" section beneath it, describe the changes on
  the feature branch, and set today's date for the new version
- When the version on a feature branch has not yet been bumped compared to master, ask the user if a major, minor or
  patch version bump should be performed, and update the version accordingly.
