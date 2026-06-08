# AGENTS.md

## Project at a glance
- Kotlin 2.4 + Spring Boot 4 service that consumes Kafka `search-events` messages and persists to PostgreSQL (`README.md`, `build.gradle.kts`).
- Main flow is Kafka-first; REST is operational/audit query surface, not business command handling (`src/main/kotlin/com/travel_events_consumer/adapter/inbound/rest`).
- Architecture follows ports/adapters: domain + application ports/services + inbound/outbound adapters (`src/main/kotlin/com/travel_events_consumer/{domain,application,adapter}`).

## Big-picture data flow
- `KafkaSearchEventListener` validates raw JSON against `contracts/search-event-v1.json`, maps to `SearchEventMessage`, sets MDC context, then calls `ProcessSearchEventUseCase`.
- `SearchEventProcessorService` handles idempotency and persistence, writes `EventAuditLog`, and manually acknowledges Kafka offsets (`Acknowledgment.acknowledge()`).
- Duplicate detection is two-layered: pre-check via `DuplicateDetectionPolicy.existsByEventId` plus DB unique key on `search_records.event_id`.
- Persistence adapters map domain models to JPA entities via dedicated mappers (`SearchRecordMapper`, `AuditLogMapper`); domain classes stay annotation-free.
- Audit query path is separate: `AuditLogController` -> `QueryAuditLogUseCase` -> `AuditLogQueryService` -> `AuditLogRepository`.

## Runtime and operational behavior
- Kafka listener uses manual immediate ack and explicit consumer concurrency (`KafkaConsumerConfig`, `application.yml`).
- Listener schema/deserialization failures are treated as terminal: write `REJECTED` audit and ack so consumer continues (`KafkaSearchEventListener`).
- Current persisted `topic/partition/offset` in processed records are placeholders (`"unknown"`, `-1`) from service mapping, not Kafka metadata (`SearchEventProcessorService`).
- Health/readiness relies on Actuator plus custom Kafka indicator (`KafkaHealthIndicator`); metrics exposed at `/actuator/prometheus`.
- Logging profile behavior differs: human-readable logs for `local,test`, structured JSON for other profiles with MDC keys (`logback-spring.xml`).

## Developer workflows (copy/paste)
```bash
docker compose up -d
./gradlew bootRun --args='--spring.profiles.active=local'
./gradlew build
./gradlew test
./gradlew test --tests "*IntegrationTest"
./gradlew test --tests "*ContractTest"
./gradlew jacocoTestReport
```

## Testing patterns to follow
- Integration tests extend `AbstractIntegrationTest` and use Testcontainers Kafka + PostgreSQL; dynamic properties are wired centrally there.
- Async ingestion assertions use Awaitility (`await.atMost(...)`) with direct SQL checks via `JdbcTemplate`.
- Contract tests validate JSON schema compatibility from classpath `contracts/search-event-v1.json`.
- Keep migration compatibility visible with `FlywayMigrationTest` and SQL in `src/main/resources/db/migration`.

## Codebase-specific conventions
- JSON is snake_case externally (`JacksonConfig`, `@JsonProperty` usage in DTO/message classes).
- Unknown top-level event fields are captured into `extraFields` via `@JsonAnySetter` and stored in JSONB.
- REST errors are RFC 7807 `ProblemDetail` via `GlobalExceptionHandler`; reuse this path for new HTTP errors.
- Database column naming intentionally avoids reserved words (`partition_num`, `kafka_offset`) and is mirrored in JPA entities.
- Required env vars in non-local profiles: `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, `KAFKA_BOOTSTRAP_SERVERS` (plus optional `KAFKA_TOPICS`, `KAFKA_LISTENER_CONCURRENCY`).

## Useful design context
- Feature docs and contracts live in `specs/001-kafka-event-consumer/` (`plan.md`, `quickstart.md`, `contracts/*`).
- `CLAUDE.md` points agents to those artifacts first; use them when changing behavior or contracts.

