# Research: Kafka Search Event Consumer

**Branch**: `001-kafka-event-consumer` | **Date**: 2026-06-07

## Technology Stack Decisions

### Decision 1: Language & Runtime

- **Decision**: Kotlin 2.4.0 on JVM 21 (LTS)
- **Rationale**: Explicitly specified. Kotlin 2.4.0 brings K2 compiler stability, context
  parameters preview, and improved coroutine integration. JVM 21 provides virtual threads
  (Project Loom) relevant for I/O-bound Kafka consumption.
- **Alternatives considered**: Java 21 (more verbose, no data classes), Kotlin 1.x (older
  compiler, fewer language features).

### Decision 2: Framework

- **Decision**: Spring Boot 4.0.6
- **Rationale**: Explicitly specified. Spring Boot 4.x requires JVM 21+, aligns with Jakarta
  EE 11 (jakarta.* namespace), and ships Spring Framework 7.x which adds first-class virtual
  thread support and improved observability via Micrometer 1.14+.
- **Key change from Boot 3.x**: `javax.*` → `jakarta.*` namespace is complete; Hibernate 7.x
  is included (UUID-based IDs natively supported). Spring Kafka 4.x ships with Boot 4.
- **Alternatives considered**: Quarkus (smaller footprint but team unfamiliar), Micronaut
  (no explicit requirement for it).

### Decision 3: Kafka Client Strategy

- **Decision**: Spring Kafka (`spring-kafka`) with `@KafkaListener` and manual offset commit
  (`AckMode.MANUAL_IMMEDIATE`)
- **Rationale**: `spring-kafka` integrates cleanly with Spring Boot auto-configuration.
  Manual offset acknowledgement is mandatory (FR-010: offset not committed until DB write
  confirmed). `AckMode.MANUAL_IMMEDIATE` commits synchronously after the listener method
  returns, giving the tightest at-least-once guarantee.
- **Concurrency**: `ConcurrentKafkaListenerContainerFactory` with `concurrency` set to the
  number of topic partitions (configurable). Each partition processed by one thread.
- **Error handling**: `DefaultErrorHandler` with `ExponentialBackOffWithMaxRetries` for
  transient DB errors; `DeadLetterPublishingRecoverer` replaced by audit-log write (DLQ
  topic out of scope for v1 per spec assumption).
- **Alternatives considered**: Reactor Kafka (reactive, more complex, not required by spec),
  raw KafkaConsumer (too low-level, no Spring integration).

### Decision 4: Persistence

- **Decision**: Spring Data JPA with Hibernate 7.x on PostgreSQL 16
- **Rationale**: Explicitly specified. JPA provides the ORM layer; Hibernate 7 natively maps
  UUID primary keys without plugin. PostgreSQL-specific features used: `JSONB` columns for
  `search_criteria` and `search_result` (flexible schema, FR-011); `ON CONFLICT DO NOTHING`
  for idempotent inserts (FR-004).
- **Transaction strategy**: `@Transactional` on application service methods. Kafka listener
  calls application service in a transaction; offset committed only after transaction
  commits (enforced by listener AckMode).
- **Connection pool**: HikariCP (Spring Boot default). Pool size tuned to
  `listener.concurrency × 2` to avoid starvation.
- **Alternatives considered**: jOOQ (type-safe SQL, less ORM magic) — rejected because
  spec explicitly calls for Spring Data JPA.

### Decision 5: Database Migrations

- **Decision**: Flyway 11.x (included in Spring Boot 4)
- **Rationale**: Explicitly specified. Flyway runs at startup, applies versioned SQL
  migrations from `classpath:db/migration/`. Schema version tracked in
  `flyway_schema_history`. Migrations are idempotent SQL scripts only (no Java migrations).
- **Naming convention**: `V{version}__{description}.sql` (e.g., `V1__create_search_records.sql`).

### Decision 6: Idempotency Strategy

- **Decision**: Unique constraint on `search_records.event_id` (UUID). On duplicate insert,
  catch `DataIntegrityViolationException`, log as duplicate, write audit row with status
  `DUPLICATE`, and acknowledge offset without re-throwing.
- **Rationale**: Database-enforced uniqueness is the most reliable idempotency mechanism —
  it works even if the application crashes mid-processing and restarts. No distributed lock
  or Redis cache needed.
- **Alternatives considered**: Redis-based dedup cache (adds dependency, cache TTL
  introduces window for duplicates), application-level check-before-insert (TOCTOU race
  under concurrent consumers).

### Decision 7: Structured Logging

- **Decision**: SLF4J + Logback with Logstash encoder (`logstash-logback-encoder`) for JSON
  output in production profile. MDC carries `correlation_id` = `event_id` for every
  processing context.
- **Rationale**: Spring Boot ships Logback by default. Logstash encoder produces JSON lines
  consumable by ELK/Loki without additional parsing. MDC propagation through the call stack
  ensures every log line in a processing context carries the event ID.
- **Alternatives considered**: Log4j2 (similar capability, less common in Spring projects),
  OpenTelemetry logging (heavier, premature for v1).

### Decision 8: Metrics & Observability

- **Decision**: Micrometer 1.14+ (included in Spring Boot 4) with `MeterRegistry`.
  Prometheus scrape endpoint via `spring-boot-actuator` at `/actuator/prometheus`.
  Custom counters: `events.received`, `events.persisted`, `events.rejected`,
  `events.duplicate`. Timer: `events.processing.duration` (p50/p95/p99).
  Consumer lag exposed via `KafkaMetrics` binder.
- **Health probes**: Spring Boot Actuator `/actuator/health` (liveness) and
  `/actuator/health/readiness` (readiness). Custom `HealthIndicator` for Kafka connectivity.
- **Alternatives considered**: Dropwizard Metrics (legacy), manual metric exposition
  (fragile).

### Decision 9: Error Handling & RFC 7807

- **Decision**: `ProblemDetail` (Spring Framework 6+ native, available in Boot 4) returned
  from `@ControllerAdvice`. All REST error responses use `application/problem+json`
  content type with `type`, `title`, `status`, `detail`, `instance` fields.
- **Rationale**: Spring Boot 4 / Spring MVC 7 ships native `ProblemDetail` support aligned
  with RFC 7807. No additional library required.
- **Alternatives considered**: Zalando Problem library (Boot 3 era workaround, superseded).

### Decision 10: Schema Extensibility

- **Decision**: `search_criteria` and `search_result` stored as `JSONB` in PostgreSQL.
  Incoming event fields unknown to the domain model are captured in an `extra_fields JSONB`
  column on `search_records`. Jackson `@JsonAnyGetter`/`@JsonAnySetter` captures unknown
  fields at deserialization without failing.
- **Rationale**: Satisfies FR-011 (new optional fields require no code change). JSONB is
  queryable and indexable in PostgreSQL, supporting future analytics needs.

### Decision 11: Architecture Pattern

- **Decision**: Clean Onion Architecture (Hexagonal) as specified. Layers:
  - `domain/` — pure Kotlin, no Spring annotations, no JPA annotations
  - `application/` — use case interfaces (inbound ports) + implementations (application
    services). Spring `@Service` + `@Transactional` live here.
  - `adapter/inbound/event/` — `@KafkaListener` classes, translate Kafka messages to
    domain commands, call inbound ports
  - `adapter/inbound/rest/` — `@RestController` classes for health/metrics/audit REST APIs
  - `adapter/outbound/persistence/` — JPA entities + Spring Data repositories + mappers
- **Dependency rule**: Domain has zero outward dependencies. Application depends only on
  domain. Adapters depend on application ports.

### Decision 12: Testing Strategy

- **Decision**:
  - Unit: JUnit 5 + MockK for domain services and application services (mocked ports)
  - Integration: `@SpringBootTest` + Testcontainers (PostgreSQL + Kafka) for full pipeline
    tests. No mocks at infrastructure boundary per constitution Principle III.
  - Contract: JSON Schema validation tests for the incoming event payload.
  - Performance: k6 or Gatling load test script (separate, not part of unit/integration run).
- **Testcontainers modules**: `testcontainers-kafka` (Confluent platform image),
  `testcontainers-postgresql`.

## Resolved Clarifications

All technical choices were fully specified in user input. No ambiguities remain.
