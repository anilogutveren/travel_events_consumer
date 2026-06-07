# Data Model: Kafka Search Event Consumer

**Branch**: `001-kafka-event-consumer` | **Date**: 2026-06-07

## Domain Layer (Pure Kotlin — no JPA, no Spring)

### Value Objects

```
EventId(value: UUID)
UserId(value: String)
SessionId(value: String)
CorrelationId(value: String)   // = EventId.value.toString() in processing context
```

### Aggregate: SearchRecord

Root aggregate representing a single persisted search event.

| Field | Type | Constraints | Notes |
|---|---|---|---|
| id | UUID | PK, generated | Internal surrogate key |
| eventId | EventId | Unique, not null | Source system UUID, dedup key |
| eventVersion | String | not null | e.g. "1.0" |
| eventTimestamp | OffsetDateTime | not null | Original source-system time, preserved with TZ |
| userId | UserId | not null | Denormalised from event |
| userType | String | nullable | e.g. "REGISTERED", "GUEST" |
| userLocale | String | nullable | BCP-47 e.g. "en-GB" |
| sessionId | SessionId | nullable | Browser/app session |
| destination | Destination | embedded value object | |
| searchCriteria | Map<String, Any> | not null | Flexible KV, stored as JSONB |
| searchResult | SearchResult | embedded value object | |
| extraFields | Map<String, Any> | not null, default empty | Unknown fields from event |
| topic | String | not null | Kafka topic name |
| partition | Int | not null | Kafka partition |
| offset | Long | not null | Kafka offset |
| createdAt | OffsetDateTime | not null, DB default | Row insertion time |

**State transitions**: SearchRecord is immutable once created. No state machine required.

### Value Object: Destination

Embedded within SearchRecord.

| Field | Type | Constraints |
|---|---|---|
| destinationId | String | not null (may be code or free-text ID) |
| destinationName | String | not null |
| destinationType | String | nullable (CITY / AIRPORT / REGION / HOTEL) |
| region | String | nullable |
| country | String | nullable |

### Value Object: SearchResult

Embedded within SearchRecord.

| Field | Type | Constraints |
|---|---|---|
| resultCode | String | not null (e.g. "SUCCESS", "NO_RESULTS", "ERROR") |
| resultSummary | String | nullable |
| resultCount | Int | nullable |
| responseTimeMs | Long | nullable |

### Entity: EventAuditLog

One row per processing attempt. Not part of the SearchRecord aggregate — written
independently, including on failure paths where SearchRecord is not created.

| Field | Type | Constraints | Notes |
|---|---|---|---|
| id | UUID | PK, generated | |
| eventId | String | not null, indexed | Raw string (may be malformed UUID) |
| topic | String | not null | |
| partition | Int | not null | |
| offset | Long | not null | |
| status | ProcessingStatus | not null | Enum: PROCESSED, REJECTED, RETRY, DUPLICATE, DEAD_LETTERED |
| errorMessage | String | nullable | Truncated at 2000 chars |
| rawPayloadRef | String | nullable | First 500 chars of raw payload for diagnostics |
| attemptNumber | Int | not null, default 1 | Incremented on retry |
| processedAt | OffsetDateTime | not null | Wall-clock time of this attempt |

**Index**: `(event_id, status)` for audit queries. `(topic, partition, offset)` for
Kafka position lookups.

---

## Database Schema (PostgreSQL 16)

### Table: `search_records`

```sql
CREATE TABLE search_records (
    id                  UUID            PRIMARY KEY DEFAULT gen_random_uuid(),
    event_id            UUID            NOT NULL,
    event_version       VARCHAR(20)     NOT NULL,
    event_timestamp     TIMESTAMPTZ     NOT NULL,
    user_id             VARCHAR(255)    NOT NULL,
    user_type           VARCHAR(50),
    user_locale         VARCHAR(20),
    session_id          VARCHAR(255),
    destination_id      VARCHAR(255)    NOT NULL,
    destination_name    VARCHAR(500)    NOT NULL,
    destination_type    VARCHAR(50),
    region              VARCHAR(255),
    country             VARCHAR(100),
    search_criteria     JSONB           NOT NULL DEFAULT '{}',
    result_code         VARCHAR(50)     NOT NULL,
    result_summary      VARCHAR(1000),
    result_count        INTEGER,
    response_time_ms    BIGINT,
    extra_fields        JSONB           NOT NULL DEFAULT '{}',
    topic               VARCHAR(255)    NOT NULL,
    partition_num       INTEGER         NOT NULL,
    kafka_offset        BIGINT          NOT NULL,
    created_at          TIMESTAMPTZ     NOT NULL DEFAULT now(),
    CONSTRAINT uq_search_records_event_id UNIQUE (event_id)
);

CREATE INDEX idx_search_records_user_id       ON search_records (user_id);
CREATE INDEX idx_search_records_event_ts      ON search_records (event_timestamp DESC);
CREATE INDEX idx_search_records_destination   ON search_records (destination_id);
CREATE INDEX idx_search_records_criteria      ON search_records USING gin (search_criteria);
```

### Table: `event_audit_log`

```sql
CREATE TABLE event_audit_log (
    id              UUID            PRIMARY KEY DEFAULT gen_random_uuid(),
    event_id        VARCHAR(255)    NOT NULL,
    topic           VARCHAR(255)    NOT NULL,
    partition_num   INTEGER         NOT NULL,
    kafka_offset    BIGINT          NOT NULL,
    status          VARCHAR(30)     NOT NULL,
    error_message   VARCHAR(2000),
    raw_payload_ref VARCHAR(500),
    attempt_number  INTEGER         NOT NULL DEFAULT 1,
    processed_at    TIMESTAMPTZ     NOT NULL DEFAULT now()
);

CREATE INDEX idx_audit_event_id   ON event_audit_log (event_id);
CREATE INDEX idx_audit_status     ON event_audit_log (status);
CREATE INDEX idx_audit_processed  ON event_audit_log (processed_at DESC);
CREATE INDEX idx_audit_topic_pos  ON event_audit_log (topic, partition_num, kafka_offset);
```

---

## Flyway Migration Plan

| Version | File | Description |
|---|---|---|
| V1 | `V1__create_search_records.sql` | Creates `search_records` table with all indexes |
| V2 | `V2__create_event_audit_log.sql` | Creates `event_audit_log` table with all indexes |
| V3 | `V3__add_search_records_extra_index.sql` | Optional: GIN index on `extra_fields` if needed |

All migrations are in `src/main/resources/db/migration/`.

---

## Incoming Kafka Event Schema

### JSON Schema (assumed v1.0)

```json
{
  "$schema": "http://json-schema.org/draft-07/schema#",
  "$id": "search-event-v1.json",
  "type": "object",
  "required": ["event_id", "event_version", "event_timestamp", "user", "destination",
               "search_criteria", "search_result"],
  "additionalProperties": true,
  "properties": {
    "event_id":        { "type": "string", "format": "uuid" },
    "event_version":   { "type": "string", "pattern": "^\\d+\\.\\d+$" },
    "event_timestamp": { "type": "string", "format": "date-time" },
    "user": {
      "type": "object",
      "required": ["user_id"],
      "properties": {
        "user_id":     { "type": "string", "minLength": 1 },
        "user_type":   { "type": "string" },
        "user_locale": { "type": "string" },
        "session_id":  { "type": "string" }
      }
    },
    "destination": {
      "type": "object",
      "required": ["destination_id", "destination_name"],
      "properties": {
        "destination_id":   { "type": "string", "minLength": 1 },
        "destination_name": { "type": "string", "minLength": 1 },
        "destination_type": { "type": "string" },
        "region":           { "type": "string" },
        "country":          { "type": "string" }
      }
    },
    "search_criteria": {
      "type": "object",
      "additionalProperties": true
    },
    "search_result": {
      "type": "object",
      "required": ["result_code"],
      "properties": {
        "result_code":       { "type": "string", "minLength": 1 },
        "result_summary":    { "type": "string" },
        "result_count":      { "type": "integer", "minimum": 0 },
        "response_time_ms":  { "type": "integer", "minimum": 0 }
      }
    }
  }
}
```

**Schema evolution rule**: New fields at any level use `additionalProperties: true`. The
consumer captures unknown top-level fields in `extra_fields`. Unknown nested fields within
`search_criteria` and `search_result` are preserved via JSONB storage.

---

## Package → Class Mapping

```
com.travel_events_consumer/
├── domain/
│   ├── model/
│   │   ├── SearchRecord.kt          # Aggregate root (data class, immutable)
│   │   ├── EventAuditLog.kt         # Entity
│   │   ├── Destination.kt           # Value object
│   │   ├── SearchResult.kt          # Value object
│   │   ├── ProcessingStatus.kt      # Enum
│   │   └── valueobjects/
│   │       ├── EventId.kt
│   │       ├── UserId.kt
│   │       └── SessionId.kt
│   ├── service/
│   │   └── DuplicateDetectionPolicy.kt  # Domain rule: is this event a duplicate?
│   └── event/
│       ├── SearchEventReceived.kt   # Domain event published after successful persist
│       └── EventProcessingFailed.kt # Domain event on rejection/dead-letter
├── application/
│   ├── port/
│   │   ├── inbound/
│   │   │   ├── ProcessSearchEventUseCase.kt   # Primary port
│   │   │   └── QueryAuditLogUseCase.kt        # Primary port (US3)
│   │   └── outbound/
│   │       ├── SearchRecordRepository.kt      # Secondary port
│   │       └── AuditLogRepository.kt          # Secondary port
│   └── service/
│       ├── SearchEventProcessorService.kt     # Implements ProcessSearchEventUseCase
│       └── AuditLogQueryService.kt            # Implements QueryAuditLogUseCase
└── adapter/
    ├── inbound/
    │   ├── event/
    │   │   ├── KafkaSearchEventListener.kt    # @KafkaListener, translates to domain cmd
    │   │   └── SearchEventMessage.kt          # DTO (data class) — Kafka payload shape
    │   └── rest/
    │       ├── HealthController.kt            # /health, /ready
    │       ├── AuditLogController.kt          # /api/v1/audit-log
    │       └── dto/
    │           └── AuditLogResponse.kt        # Response DTO (data class)
    └── outbound/
        └── persistence/
            ├── SearchRecordJpaEntity.kt       # JPA entity (@Entity)
            ├── EventAuditLogJpaEntity.kt      # JPA entity (@Entity)
            ├── SearchRecordJpaRepository.kt   # Spring Data JPA interface
            ├── AuditLogJpaRepository.kt       # Spring Data JPA interface
            ├── SearchRecordPersistenceAdapter.kt  # Implements SearchRecordRepository port
            ├── AuditLogPersistenceAdapter.kt      # Implements AuditLogRepository port
            └── mapper/
                ├── SearchRecordMapper.kt      # Domain ↔ JPA entity
                └── AuditLogMapper.kt          # Domain ↔ JPA entity
```
