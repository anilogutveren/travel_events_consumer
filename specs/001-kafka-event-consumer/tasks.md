---
description: "Task list for Kafka Search Event Consumer implementation"
---

# Tasks: Kafka Search Event Consumer

**Input**: Design documents from `specs/001-kafka-event-consumer/`

**Prerequisites**: plan.md ✅ | spec.md ✅ | data-model.md ✅ | contracts/ ✅ | research.md ✅

**Tests**: TDD approach — test tasks precede implementation tasks per Constitution Principle II.
All test tasks are marked with ⚠️ and MUST fail before the corresponding implementation begins.

**Organization**: Tasks grouped by user story to enable independent implementation and testing.

**Tech Stack**: Kotlin 2.4.0 · Spring Boot 4.0.6 · Spring Kafka · Spring Data JPA · PostgreSQL 16 · Flyway 11 · JUnit 5 · MockK · Testcontainers

---

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: User story label (US1, US2, US3)
- Every task includes an exact file path

---

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Project skeleton, build config, and shared cross-cutting infrastructure

- [x] T001 Initialise Gradle Kotlin DSL project with Spring Boot 4.0.6 initializr output — `build.gradle.kts`, `settings.gradle.kts`
- [x] T002 Add all required dependencies to `build.gradle.kts`: `spring-boot-starter-web`, `spring-boot-starter-data-jpa`, `spring-boot-starter-actuator`, `spring-kafka`, `flyway-core`, `flyway-database-postgresql`, `jackson-module-kotlin`, `logstash-logback-encoder`, `micrometer-registry-prometheus`, and test deps (`junit-5`, `mockk`, `testcontainers-kafka`, `testcontainers-postgresql`, `spring-boot-starter-test`)
- [x] T003 [P] Create root package structure: `src/main/kotlin/com/travel_events_consumer/` with sub-directories `domain/model/valueobjects/`, `domain/service/`, `domain/event/`, `application/port/inbound/`, `application/port/outbound/`, `application/service/`, `adapter/inbound/event/`, `adapter/inbound/rest/dto/`, `adapter/outbound/persistence/mapper/`
- [x] T004 [P] Create `src/main/resources/application.yml` with Spring Boot 4 configuration stubs: datasource (`${DB_URL}`, `${DB_USERNAME}`, `${DB_PASSWORD}`), Kafka bootstrap servers (`${KAFKA_BOOTSTRAP_SERVERS}`), consumer group id, topic list, Flyway enabled, Actuator endpoints exposed, Micrometer Prometheus registry enabled
- [x] T005 [P] Create `src/main/resources/application-local.yml` with local dev overrides pointing to `localhost:5432` and `localhost:9092`
- [x] T006 [P] Create `src/main/resources/logback-spring.xml` with Logstash JSON encoder for `production` profile and standard console encoder for `local`/`test` profiles; configure MDC fields `correlation_id`, `event_id`, `topic`, `partition`, `offset`
- [x] T007 [P] Create `docker-compose.yml` at repo root with `postgres:16-alpine` (port 5432, DB `travel_events`, user `travel_user`) and `confluentinc/cp-kafka:7.6.0` (port 9092, `KAFKA_AUTO_CREATE_TOPICS_ENABLE=true`, topic `search-events` auto-created)
- [x] T008 Create `src/main/kotlin/com/travel_events_consumer/TravelEventsConsumerApplication.kt` — Spring Boot main entry point

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Domain model, value objects, outbound ports, Flyway migrations, and JPA entities
that every user story depends on. No user story work begins until this phase is complete.

**⚠️ CRITICAL**: No user story work can begin until this phase is complete.

### Tests for Foundation ⚠️

> **Write these tests FIRST — they MUST FAIL before implementation**

- [x] T009 [P] Write unit tests for `EventId` value object (valid UUID accepts, invalid rejects, equality) in `src/test/kotlin/com/travel_events_consumer/domain/model/valueobjects/EventIdTest.kt`
- [x] T010 [P] Write unit tests for `UserId` and `SessionId` value objects in `src/test/kotlin/com/travel_events_consumer/domain/model/valueobjects/UserIdTest.kt` and `SessionIdTest.kt`
- [x] T011 [P] Write `SearchRecordMapperTest` verifying domain ↔ JPA entity round-trip and that `event_timestamp` is preserved with timezone offset in `src/test/kotlin/com/travel_events_consumer/adapter/outbound/persistence/mapper/SearchRecordMapperTest.kt`
- [x] T012 [P] Write `AuditLogMapperTest` verifying all `ProcessingStatus` enum values map correctly in `src/test/kotlin/com/travel_events_consumer/adapter/outbound/persistence/mapper/AuditLogMapperTest.kt`
- [x] T013 [P] Write Flyway migration integration test (Spring Boot Test + Testcontainers PostgreSQL) verifying V1 and V2 migrations apply cleanly and `flyway_schema_history` shows 2 successful rows in `src/test/kotlin/com/travel_events_consumer/FlywayMigrationTest.kt`

### Implementation

- [x] T014 [P] Create `ProcessingStatus.kt` enum: `PROCESSED`, `REJECTED`, `RETRY`, `DUPLICATE`, `DEAD_LETTERED` in `src/main/kotlin/com/travel_events_consumer/domain/model/ProcessingStatus.kt`
- [x] T015 [P] Create value objects `EventId.kt`, `UserId.kt`, `SessionId.kt` with inline value class pattern and UUID/String validation in `src/main/kotlin/com/travel_events_consumer/domain/model/valueobjects/`
- [x] T016 [P] Create `Destination.kt` data class (value object: `destinationId`, `destinationName`, `destinationType`, `region`, `country`) in `src/main/kotlin/com/travel_events_consumer/domain/model/Destination.kt`
- [x] T017 [P] Create `SearchResult.kt` data class (value object: `resultCode`, `resultSummary`, `resultCount`, `responseTimeMs`) in `src/main/kotlin/com/travel_events_consumer/domain/model/SearchResult.kt`
- [x] T018 Create `SearchRecord.kt` aggregate root data class (all fields per data-model.md; no JPA/Spring imports) in `src/main/kotlin/com/travel_events_consumer/domain/model/SearchRecord.kt`
- [x] T019 Create `EventAuditLog.kt` entity data class (all fields per data-model.md; no JPA/Spring imports) in `src/main/kotlin/com/travel_events_consumer/domain/model/EventAuditLog.kt`
- [x] T020 [P] Create domain events `SearchEventReceived.kt` and `EventProcessingFailed.kt` in `src/main/kotlin/com/travel_events_consumer/domain/event/`
- [x] T021 [P] Create outbound port interface `SearchRecordRepository.kt` (`save`, `existsByEventId`) in `src/main/kotlin/com/travel_events_consumer/application/port/outbound/SearchRecordRepository.kt`
- [x] T022 [P] Create outbound port interface `AuditLogRepository.kt` (`save`, `findByEventId`, `findAll` with filter+pageable, `findById`) in `src/main/kotlin/com/travel_events_consumer/application/port/outbound/AuditLogRepository.kt`
- [x] T023 Create `V1__create_search_records.sql` Flyway migration (full DDL from data-model.md including all indexes and unique constraint on `event_id`) in `src/main/resources/db/migration/V1__create_search_records.sql`
- [x] T024 Create `V2__create_event_audit_log.sql` Flyway migration (full DDL from data-model.md including all indexes) in `src/main/resources/db/migration/V2__create_event_audit_log.sql`
- [x] T025 Create `SearchRecordJpaEntity.kt` (`@Entity`, `@Table(name="search_records")`; all columns mapped per schema; `search_criteria`, `search_result` sub-fields, `extra_fields` as `@Column(columnDefinition="jsonb")`; `event_timestamp` as `OffsetDateTime` with `TIMESTAMPTZ`) in `src/main/kotlin/com/travel_events_consumer/adapter/outbound/persistence/SearchRecordJpaEntity.kt`
- [x] T026 Create `EventAuditLogJpaEntity.kt` (`@Entity`, `@Table(name="event_audit_log")`; all columns mapped per schema; `status` as `@Enumerated(EnumType.STRING)`) in `src/main/kotlin/com/travel_events_consumer/adapter/outbound/persistence/EventAuditLogJpaEntity.kt`
- [x] T027 [P] Create `SearchRecordJpaRepository.kt` extending `JpaRepository<SearchRecordJpaEntity, UUID>` with `existsByEventId(eventId: UUID): Boolean` in `src/main/kotlin/com/travel_events_consumer/adapter/outbound/persistence/SearchRecordJpaRepository.kt`
- [x] T028 [P] Create `AuditLogJpaRepository.kt` extending `JpaRepository<EventAuditLogJpaEntity, UUID>` with `findByEventId`, `findAllByStatusIn`, and `findAllByProcessedAtBetween` methods in `src/main/kotlin/com/travel_events_consumer/adapter/outbound/persistence/AuditLogJpaRepository.kt`
- [x] T029 Create `SearchRecordMapper.kt` (domain `SearchRecord` ↔ `SearchRecordJpaEntity`; uses Jackson `ObjectMapper` for JSONB field conversion; preserves `OffsetDateTime` exactly) in `src/main/kotlin/com/travel_events_consumer/adapter/outbound/persistence/mapper/SearchRecordMapper.kt`
- [x] T030 Create `AuditLogMapper.kt` (domain `EventAuditLog` ↔ `EventAuditLogJpaEntity`) in `src/main/kotlin/com/travel_events_consumer/adapter/outbound/persistence/mapper/AuditLogMapper.kt`
- [x] T031 Create `SearchRecordPersistenceAdapter.kt` (`@Component`, implements `SearchRecordRepository`, delegates to `SearchRecordJpaRepository` + `SearchRecordMapper`) in `src/main/kotlin/com/travel_events_consumer/adapter/outbound/persistence/SearchRecordPersistenceAdapter.kt`
- [x] T032 Create `AuditLogPersistenceAdapter.kt` (`@Component`, implements `AuditLogRepository`, delegates to `AuditLogJpaRepository` + `AuditLogMapper`, supports filter+pagination) in `src/main/kotlin/com/travel_events_consumer/adapter/outbound/persistence/AuditLogPersistenceAdapter.kt`

**Checkpoint**: Foundation complete — domain model, JPA adapters, Flyway migrations, and outbound ports all in place. User story phases can now begin.

---

## Phase 3: User Story 1 — Operator Ingests and Persists Search Events (Priority: P1) 🎯 MVP

**Goal**: Kafka listener consumes events, validates, persists with idempotency, maintains audit trail.

**Independent Test**: Publish a valid JSON event to `search-events` topic → verify record in `search_records` table within 5 s; publish duplicate → verify no second row and `DUPLICATE` audit row; publish malformed event → verify `REJECTED` audit row and consumer continues.

### Tests for User Story 1 ⚠️

> **Write these tests FIRST — they MUST FAIL before implementation**

- [x] T033 [P] [US1] Write `DuplicateDetectionPolicyTest` (unit, MockK): verifies policy returns `DUPLICATE` when `eventId` already exists, `NEW` when not in `src/test/kotlin/com/travel_events_consumer/domain/service/DuplicateDetectionPolicyTest.kt`
- [x] T034 [P] [US1] Write `SearchEventProcessorServiceTest` (unit, MockK): happy path persists record + PROCESSED audit + acks offset; duplicate path writes DUPLICATE audit + acks + no save call; malformed path writes REJECTED audit + acks; dead-letter path writes DEAD_LETTERED after max retries in `src/test/kotlin/com/travel_events_consumer/application/service/SearchEventProcessorServiceTest.kt`
- [x] T035 [P] [US1] Write `KafkaSearchEventListenerTest` (unit, MockK): valid JSON → calls use case; invalid JSON → REJECTED; missing required fields → REJECTED; verifies `Acknowledgment.acknowledge()` called in all paths in `src/test/kotlin/com/travel_events_consumer/adapter/inbound/event/KafkaSearchEventListenerTest.kt`
- [x] T036 [P] [US1] Write `SearchRecordPersistenceAdapterTest` (integration, Testcontainers PostgreSQL): save persists row; save with duplicate `event_id` throws `DataIntegrityViolationException`; `existsByEventId` returns true/false correctly in `src/test/kotlin/com/travel_events_consumer/adapter/outbound/persistence/SearchRecordPersistenceAdapterTest.kt`
- [x] T037 [P] [US1] Write `AuditLogPersistenceAdapterTest` (integration, Testcontainers PostgreSQL): save persists audit row; findByEventId returns correct rows in `src/test/kotlin/com/travel_events_consumer/adapter/outbound/persistence/AuditLogPersistenceAdapterTest.kt`
- [x] T038 [P] [US1] Write `KafkaEventSchemaContractTest` (contract): validates sample valid event from `contracts/kafka-event-schema-v1.json` passes JSON Schema; validates event with missing `destination` fails schema; validates event with unknown extra field passes (additionalProperties: true) in `src/test/kotlin/com/travel_events_consumer/contract/KafkaEventSchemaContractTest.kt`
- [x] T039 [P] [US1] Write `EventIngestionIntegrationTest` (full pipeline, Testcontainers Kafka + PostgreSQL): publish valid event → assert DB record within 5 s; assert PROCESSED audit row; assert original `event_timestamp` preserved in `src/test/kotlin/com/travel_events_consumer/integration/EventIngestionIntegrationTest.kt`
- [x] T040 [P] [US1] Write `DuplicateHandlingIntegrationTest` (full pipeline): publish same event twice → assert 1 DB record; assert 2 audit rows (PROCESSED + DUPLICATE) in `src/test/kotlin/com/travel_events_consumer/integration/DuplicateHandlingIntegrationTest.kt`
- [x] T041 [P] [US1] Write `MalformedEventIntegrationTest` (full pipeline): publish missing-`destination` event → assert 0 DB records; assert REJECTED audit row; publish valid event after → assert consumer still processing in `src/test/kotlin/com/travel_events_consumer/integration/MalformedEventIntegrationTest.kt`

### Implementation for User Story 1

- [x] T042 Create `DuplicateDetectionPolicy.kt` domain service (checks `SearchRecordRepository.existsByEventId`) in `src/main/kotlin/com/travel_events_consumer/domain/service/DuplicateDetectionPolicy.kt`
- [x] T043 Create inbound port `ProcessSearchEventUseCase.kt` interface (`processEvent(message: SearchEventMessage, acknowledgment: Acknowledgment)`) in `src/main/kotlin/com/travel_events_consumer/application/port/inbound/ProcessSearchEventUseCase.kt`
- [x] T044 Create `SearchEventMessage.kt` DTO data class (mirrors Kafka JSON structure: `eventId`, `eventVersion`, `eventTimestamp`, `user`, `destination`, `searchCriteria`, `searchResult`; uses `@JsonAnyGetter`/`@JsonAnySetter` for `extraFields`; nested data classes `UserMessage`, `DestinationMessage`, `SearchResultMessage`) in `src/main/kotlin/com/travel_events_consumer/adapter/inbound/event/SearchEventMessage.kt`
- [x] T045 Create `SearchEventProcessorService.kt` (`@Service`, `@Transactional`, implements `ProcessSearchEventUseCase`): validate → check duplicate → save `SearchRecord` → save `EventAuditLog(PROCESSED)` → ack offset; on `DataIntegrityViolationException` → save `EventAuditLog(DUPLICATE)` → ack; inject Micrometer `MeterRegistry` and increment `events.received`, `events.persisted`, `events.duplicate` counters; record `events.processing.duration` timer in `src/main/kotlin/com/travel_events_consumer/application/service/SearchEventProcessorService.kt`
- [x] T046 Create `KafkaSearchEventListener.kt` (`@Component`, `@KafkaListener`): deserialize JSON to `SearchEventMessage`; validate against JSON Schema (using `networknt/json-schema-validator`); call `ProcessSearchEventUseCase`; catch deserialization errors → save REJECTED audit directly → `acknowledge()` in `src/main/kotlin/com/travel_events_consumer/adapter/inbound/event/KafkaSearchEventListener.kt`
- [x] T047 Create `KafkaConsumerConfig.kt` (`@Configuration`): `ConcurrentKafkaListenerContainerFactory` with `AckMode.MANUAL_IMMEDIATE`; `DefaultErrorHandler` with `ExponentialBackOffWithMaxRetries(5)` for transient errors; `NotRetryableExceptions` list excludes `DataIntegrityViolationException`; dead-letter recovery writes `DEAD_LETTERED` audit row in `src/main/kotlin/com/travel_events_consumer/adapter/inbound/event/KafkaConsumerConfig.kt`
- [x] T048 Add MDC population to `KafkaSearchEventListener`: `MDC.put("correlation_id", eventId)`, `MDC.put("topic", topic)`, `MDC.put("partition", partition.toString())`, `MDC.put("offset", offset.toString())`; clear MDC in `finally` block

**Checkpoint**: User Story 1 fully functional — events ingested, persisted, deduplicated, audited, offset committed only after DB write.

---

## Phase 4: User Story 2 — Operator Monitors Event Processing Health (Priority: P2)

**Goal**: Health/readiness endpoints, Prometheus metrics, and consumer lag visibility.

**Independent Test**: Start service → `GET /actuator/health` returns 200; publish events → `/actuator/prometheus` shows correct counter values; stop Kafka → `/actuator/health/readiness` returns 503; restart → transitions back to 200.

### Tests for User Story 2 ⚠️

> **Write these tests FIRST — they MUST FAIL before implementation**

- [x] T049 [P] [US2] Write `KafkaHealthIndicatorTest` (unit, MockK): `UP` when Kafka admin client can list topics; `DOWN` when admin client throws exception in `src/test/kotlin/com/travel_events_consumer/adapter/inbound/rest/KafkaHealthIndicatorTest.kt`
- [x] T050 [P] [US2] Write `MetricsIntegrationTest` (Spring Boot Test + Testcontainers): publish 2 valid + 1 duplicate + 1 malformed event; scrape `/actuator/prometheus`; assert `events_received_total >= 4`, `events_persisted_total == 2`, `events_duplicate_total == 1`, `events_rejected_total == 1`; assert `events_processing_duration_seconds` histogram exists in `src/test/kotlin/com/travel_events_consumer/integration/MetricsIntegrationTest.kt`
- [x] T051 [P] [US2] Write `HealthEndpointIntegrationTest` (Spring Boot Test): liveness returns 200 with `{"status":"UP"}`; readiness returns 200 when DB + Kafka available; readiness returns 503 when custom `KafkaHealthIndicator` returns DOWN in `src/test/kotlin/com/travel_events_consumer/integration/HealthEndpointIntegrationTest.kt`

### Implementation for User Story 2

- [x] T052 Create `KafkaHealthIndicator.kt` (`@Component`, implements Spring Boot `HealthIndicator`): uses `KafkaAdmin` to list topics; returns `Health.up()` on success, `Health.down()` with exception detail on failure in `src/main/kotlin/com/travel_events_consumer/adapter/inbound/rest/KafkaHealthIndicator.kt`
- [x] T053 Expose Actuator endpoints in `application.yml`: `management.endpoints.web.exposure.include: health,prometheus`; `management.endpoint.health.show-details: always`; `management.health.readiness-state.enabled: true`; `management.health.liveness-state.enabled: true`
- [x] T054 Register Micrometer `KafkaMetrics` binder bean in `KafkaConsumerConfig.kt` to expose consumer group lag metrics (`kafka.consumer.fetch.manager.records.lag` → `consumer_lag`) to Prometheus
- [x] T055 [P] Verify `events.received`, `events.persisted`, `events.rejected`, `events.duplicate` counters and `events.processing.duration` timer are registered with `MeterRegistry` tags `{topic, consumer_group}` — update `SearchEventProcessorService.kt` if needed

**Checkpoint**: Health, readiness, and Prometheus metrics all working. Consumer lag visible. Both P1 and P2 user stories independently functional.

---

## Phase 5: User Story 3 — Operator Reviews Event Processing Audit Trail (Priority: P3)

**Goal**: REST API for querying audit log with filtering and pagination.

**Independent Test**: After publishing events (US1 established), `GET /api/v1/audit-log?status=REJECTED` returns paginated entries; `GET /api/v1/audit-log/{id}` returns single entry; `GET /api/v1/audit-log?event_id=<uuid>` filters correctly; invalid params return RFC 7807 400 response.

### Tests for User Story 3 ⚠️

> **Write these tests FIRST — they MUST FAIL before implementation**

- [x] T056 [P] [US3] Write `AuditLogQueryServiceTest` (unit, MockK): `queryAuditLog` delegates to repository with correct filter + pageable; `getById` returns entry or throws `NotFoundException` in `src/test/kotlin/com/travel_events_consumer/application/service/AuditLogQueryServiceTest.kt`
- [x] T057 [P] [US3] Write `AuditLogControllerTest` (`@WebMvcTest`): `GET /api/v1/audit-log` returns 200 with paginated body; `GET /api/v1/audit-log?status=INVALID` returns 400 `ProblemDetail`; `GET /api/v1/audit-log/{unknown-id}` returns 404 `ProblemDetail`; response body uses `snake_case` field names in `src/test/kotlin/com/travel_events_consumer/adapter/inbound/rest/AuditLogControllerTest.kt`
- [x] T058 [P] [US3] Write `AuditLogRestIntegrationTest` (Spring Boot Test + Testcontainers PostgreSQL): seed 3 audit rows; query by status; query by event_id; query by date range; verify pagination metadata correct in `src/test/kotlin/com/travel_events_consumer/integration/AuditLogRestIntegrationTest.kt`

### Implementation for User Story 3

- [x] T059 Create inbound port `QueryAuditLogUseCase.kt` interface (`queryAuditLog(filter: AuditLogFilter, pageable: Pageable): Page<EventAuditLog>` and `getById(id: UUID): EventAuditLog`) in `src/main/kotlin/com/travel_events_consumer/application/port/inbound/QueryAuditLogUseCase.kt`
- [x] T060 Create `AuditLogFilter.kt` data class (`eventId: String?`, `status: ProcessingStatus?`, `from: OffsetDateTime?`, `to: OffsetDateTime?`) in `src/main/kotlin/com/travel_events_consumer/application/service/AuditLogFilter.kt`
- [x] T061 Create `AuditLogQueryService.kt` (`@Service`, `@Transactional(readOnly = true)`, implements `QueryAuditLogUseCase`): delegates to `AuditLogRepository`; throws `AuditLogNotFoundException` when `getById` finds no entry in `src/main/kotlin/com/travel_events_consumer/application/service/AuditLogQueryService.kt`
- [x] T062 Create `AuditLogResponse.kt` DTO data class (snake_case JSON: `id`, `event_id`, `topic`, `partition_num`, `kafka_offset`, `status`, `error_message`, `raw_payload_ref`, `attempt_number`, `processed_at`) in `src/main/kotlin/com/travel_events_consumer/adapter/inbound/rest/dto/AuditLogResponse.kt`
- [x] T063 Create `AuditLogController.kt` (`@RestController`, `@RequestMapping("/api/v1/audit-log")`): `GET /` with `@RequestParam` (`event_id`, `status`, `from`, `to`, `@Min(0) page`, `@Max(100) @Min(1) size`); `GET /{id}`; maps domain page to `AuditLogResponse` DTOs in `src/main/kotlin/com/travel_events_consumer/adapter/inbound/rest/AuditLogController.kt`
- [x] T064 Create `GlobalExceptionHandler.kt` (`@ControllerAdvice`): maps `AuditLogNotFoundException` → `ProblemDetail(404)`; maps `MethodArgumentNotValidException` → `ProblemDetail(400)` with field-level errors; maps `ConstraintViolationException` → `ProblemDetail(400)`; maps `Exception` → `ProblemDetail(500)` without stack trace; all responses use `application/problem+json` in `src/main/kotlin/com/travel_events_consumer/adapter/inbound/rest/GlobalExceptionHandler.kt`
- [x] T065 Create `AuditLogNotFoundException.kt` domain exception in `src/main/kotlin/com/travel_events_consumer/domain/model/AuditLogNotFoundException.kt`
- [x] T066 Configure Jackson `ObjectMapper` bean: `WRITE_DATES_AS_TIMESTAMPS = false`, `SerializationFeature.INDENT_OUTPUT = false`, `PropertyNamingStrategies.SNAKE_CASE`, register `JavaTimeModule` and `KotlinModule` in `src/main/kotlin/com/travel_events_consumer/TravelEventsConsumerApplication.kt` or dedicated `JacksonConfig.kt`

**Checkpoint**: All three user stories independently functional and testable.

---

## Phase 6: Polish & Cross-Cutting Concerns

**Purpose**: Resilience hardening, schema evolution test, performance validation, documentation.

- [x] T067 Write `ResilienceIntegrationTest` (Testcontainers): stop PostgreSQL container mid-test; assert readiness returns 503; restart container; assert consumer resumes within 60 s and pending event is persisted in `src/test/kotlin/com/travel_events_consumer/integration/ResilienceIntegrationTest.kt`
- [x] T068 Write `SchemaEvolutionIntegrationTest` (Testcontainers Kafka + PostgreSQL): publish event with unknown top-level field `new_field_v2`; assert record persisted; assert `extra_fields` column contains `{"new_field_v2": "value"}`; assert status is PROCESSED (not REJECTED) in `src/test/kotlin/com/travel_events_consumer/integration/SchemaEvolutionIntegrationTest.kt`
- [x] T069 [P] Configure `application.yml` resilience settings: `spring.kafka.consumer.auto-offset-reset: earliest`; `spring.kafka.consumer.enable-auto-commit: false`; HikariCP `maximum-pool-size: 10`, `connection-timeout: 3000`, `keepalive-time: 30000`; Kafka `session.timeout.ms: 30000`, `max.poll.interval.ms: 300000`
- [x] T070 [P] Code cleanup: enforce cyclomatic complexity ≤ 10 across all classes (Constitution Principle I); remove any unused imports; verify all public interfaces named for intent without comments required
- [x] T071 [P] Verify structured JSON logging with `correlation_id` present on all processing paths: add integration test that captures log output and asserts `correlation_id` field non-null for both PROCESSED and REJECTED events in `src/test/kotlin/com/travel_events_consumer/integration/StructuredLoggingIntegrationTest.kt`
- [x] T072 [P] Verify RFC 7807 error envelope consistency: `AuditLogControllerTest` assertions confirm `content-type: application/problem+json`; all error responses have `type`, `title`, `status`, `detail` fields (Constitution Principle IV)
- [x] T073 [P] Generate Jacoco coverage report and verify ≥ 90 % line coverage on `src/main/kotlin/`: add `jacoco` plugin to `build.gradle.kts` with `jacocoTestCoverageVerification` task enforcing the threshold; configure to exclude generated and entry-point classes
- [x] T074 [P] Add `src/test/resources/application-test.yml` with Testcontainers datasource/Kafka auto-configuration, `spring.flyway.enabled: true`, `logging.level.root: WARN` (reduce noise), `management.endpoints.web.exposure.include: health,prometheus`
- [x] T075 Run all quickstart validation scenarios from `quickstart.md` manually; confirm all 11 scenarios pass; update `quickstart.md` with any discovered discrepancies
- [x] T076 [P] Documentation: update `README.md` with build instructions (`./gradlew build`), local run (`docker compose up -d && ./gradlew bootRun --args='--spring.profiles.active=local'`), test run (`./gradlew test`), and environment variable table

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: No dependencies — start immediately
- **Foundational (Phase 2)**: Depends on Phase 1 completion — **BLOCKS all user stories**
- **US1 (Phase 3)**: Depends on Phase 2 — test tasks T033–T041 first, then T042–T048
- **US2 (Phase 4)**: Depends on Phase 2 — can start in parallel with US1 after Phase 2
- **US3 (Phase 5)**: Depends on Phase 2 — can start after Phase 2; integrates with US1 data
- **Polish (Phase 6)**: Depends on all user story phases being complete

### User Story Dependencies

- **US1 (P1)**: Only depends on Foundation. No dependency on US2 or US3.
- **US2 (P2)**: Only depends on Foundation. Metrics built into US1 service, but US2 can be
  implemented independently (health indicator is standalone).
- **US3 (P3)**: Only depends on Foundation. The audit table is populated by US1, but the
  REST query layer can be built and tested with seed data independently.

### Within Each User Story

1. Tests MUST be written first and confirmed to FAIL (Constitution Principle II)
2. Implementation follows test-by-test (Red-Green-Refactor)
3. Commit test files before implementation files (verifiable in git history)

---

## Parallel Execution Examples

### Parallel tasks within Phase 2 (Foundation)

```
# All these can run simultaneously (different files):
T009 EventId value object test
T010 UserId/SessionId value object tests
T011 SearchRecordMapperTest
T012 AuditLogMapperTest
T013 Flyway migration test

# Then simultaneously:
T014 ProcessingStatus enum
T015 Value objects implementation
T016 Destination data class
T017 SearchResult data class
T023 V1 migration SQL
T024 V2 migration SQL
```

### Parallel tasks within Phase 3 (US1)

```
# Test tasks — all parallel (different files):
T033 DuplicateDetectionPolicyTest
T034 SearchEventProcessorServiceTest
T035 KafkaSearchEventListenerTest
T036 SearchRecordPersistenceAdapterTest
T037 AuditLogPersistenceAdapterTest
T038 KafkaEventSchemaContractTest

# Integration tests — all parallel:
T039 EventIngestionIntegrationTest
T040 DuplicateHandlingIntegrationTest
T041 MalformedEventIntegrationTest
```

---

## Implementation Strategy

### MVP First (User Story 1 Only — minimum viable pipeline)

1. Complete Phase 1: Setup
2. Complete Phase 2: Foundational (CRITICAL — blocks all stories)
3. Complete Phase 3: User Story 1 (T033–T048)
4. **STOP and VALIDATE**: Run full test suite + quickstart scenarios 1–4
5. Service is production-deployable: events ingested, persisted, deduplicated, audited

### Incremental Delivery

1. Setup + Foundation → Pipeline skeleton ready
2. + User Story 1 → Core ingestion working (deploy as MVP)
3. + User Story 2 → Monitoring operational (deploy to staging with full observability)
4. + User Story 3 → Audit query API live (complete feature)
5. + Polish → Hardened for production (resilience, coverage gate, schema evolution verified)

### Parallel Team Strategy

With 2 developers after Phase 2 completes:
- Developer A: User Story 1 (Kafka listener + ingestion pipeline)
- Developer B: User Story 2 (health + metrics) + User Story 3 (audit REST API)

---

## Notes

- All `[P]` tasks target different files — confirm no file conflicts before parallelising
- Test tasks within a story are all `[P]` — they can be written simultaneously by multiple developers
- Kafka `AckMode.MANUAL_IMMEDIATE` is non-negotiable (FR-010) — never switch to auto-commit
- JSONB columns require the PostgreSQL JDBC driver and `@Column(columnDefinition="jsonb")`; Jackson serialisation to/from `Map<String, Any>` via a custom `AttributeConverter`
- `OffsetDateTime` must be stored and retrieved with timezone; verify Hibernate `hibernate.jdbc.time_zone=UTC` is NOT set (would strip offset); use `spring.jpa.properties.hibernate.jdbc.time_zone` absent or set to a fixed zone only if all sources agree
- Commit after each task or logical group; tag each commit with the task ID (e.g. `feat: T042 add DuplicateDetectionPolicy`)
- Stop at each phase checkpoint to validate story independently before proceeding
