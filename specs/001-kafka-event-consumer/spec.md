# Feature Specification: Kafka Search Event Consumer

**Feature Branch**: `001-kafka-event-consumer`

**Created**: 2026-06-07

**Status**: Draft

**Input**: User description: "Build a backend application that consumes Kafka events published by another
microservice and persists them into a relational database for reporting and analytics purposes."

## User Scenarios & Testing *(mandatory)*

### User Story 1 — Operator Ingests and Persists Search Events (Priority: P1)

An operations team deploys the consumer service. The service connects to one or more Kafka topics,
reads incoming user-search events, validates them, and writes each valid event to the database.
Operators can confirm events are landing by querying the database or reading structured logs.

**Why this priority**: This is the core purpose of the system. Without reliable ingestion and
persistence no other story is deliverable.

**Independent Test**: Deploy the service pointing at a Kafka topic, publish a well-formed search
event, and verify a matching record appears in the database within the expected processing latency.
The test delivers end-to-end traceability of a single event with no other stories implemented.

**Acceptance Scenarios**:

1. **Given** a running consumer connected to a Kafka topic,
   **When** a valid search event is published to that topic,
   **Then** a record containing all event fields (user, destination, timestamp, result) is persisted
   in the database within 5 seconds of publication, and the event's original timestamp is preserved
   exactly.

2. **Given** a consumer that has already processed an event,
   **When** the identical event (same event ID) is published again,
   **Then** no duplicate record is created and the idempotent behaviour is reflected in the
   processing audit log.

3. **Given** a malformed or schema-invalid event on the topic,
   **When** the consumer reads it,
   **Then** the event is not persisted, an error-level structured log entry is emitted with the
   raw payload and reason for rejection, and the consumer continues processing subsequent events
   without interruption.

---

### User Story 2 — Operator Monitors Event Processing Health (Priority: P2)

An operator wants to understand whether the service is healthy, keeping up with the topic, and
whether any events are failing. They query health endpoints and review operational metrics from
their existing monitoring stack.

**Why this priority**: Operational visibility is essential for a production data pipeline.
Without it failures are invisible until data is already missing.

**Independent Test**: Start the service, publish a mix of valid and invalid events, then query
the `/health` and `/ready` endpoints and confirm the metrics endpoint exposes counts for
events received, persisted, and rejected.

**Acceptance Scenarios**:

1. **Given** a running service,
   **When** `GET /health` is called,
   **Then** the response indicates live status within 200 ms and returns HTTP 200 when healthy
   or HTTP 503 when a critical dependency is unavailable.

2. **Given** a running service that has processed events,
   **When** the metrics endpoint is scraped,
   **Then** counters for events_received, events_persisted, events_rejected, and
   events_duplicate are present and accurate.

3. **Given** a service with a Kafka or database connection that has recovered from a transient
   outage,
   **When** `GET /ready` is called after recovery,
   **Then** the response transitions from HTTP 503 back to HTTP 200 and processing resumes
   without manual intervention.

---

### User Story 3 — Operator Reviews Event Processing Audit Trail (Priority: P3)

An operator or data engineer investigates why certain events were not persisted. They query the
audit table to review each event's processing outcome (success, rejected, retried, dead-lettered)
and the associated error detail.

**Why this priority**: The audit trail is a compliance and diagnostic requirement; it can be
delivered after the core pipeline is working.

**Independent Test**: Publish a mix of valid, duplicate, and malformed events, then query the
audit table directly and confirm one row per processing attempt with the correct status and error
message where applicable.

**Acceptance Scenarios**:

1. **Given** a valid event that was successfully persisted,
   **When** the audit table is queried by event ID,
   **Then** one row exists with status `PROCESSED`, the processing timestamp, and a null error
   field.

2. **Given** a malformed event that was rejected,
   **When** the audit table is queried,
   **Then** one row exists with status `REJECTED`, the raw payload (or a truncated reference),
   and a non-null error description.

3. **Given** an event that failed transiently and was retried,
   **When** the audit table is queried,
   **Then** rows with status `RETRY` appear for each attempt, followed by a final `PROCESSED`
   or `DEAD_LETTERED` row.

---

### Edge Cases

- What happens when the Kafka broker is unreachable at startup?
  The service MUST start but report not-ready; it MUST reconnect automatically with
  exponential back-off and begin consuming without restart.
- What happens when the database is unreachable at startup?
  Same behaviour as above — not-ready reported, reconnection attempted, no data loss for
  events buffered in Kafka (Kafka offset not committed until DB write succeeds).
- What happens when an event payload is valid JSON but missing required fields?
  The event is treated as malformed, logged at ERROR, written to the audit log as REJECTED,
  and the consumer advances past it.
- What happens when the same event arrives on multiple topics simultaneously?
  Deduplication is by event ID regardless of topic; the second occurrence is discarded.
- What happens when the database write succeeds but the Kafka offset commit fails?
  The idempotency key on event ID prevents duplicate DB rows on redelivery; the audit log
  records the re-attempt.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: The system MUST consume search events from one or more configurable Kafka topics.
- **FR-002**: The system MUST validate each consumed event against a defined schema before
  processing.
- **FR-003**: Valid events MUST be persisted in a relational database, preserving the original
  event timestamp without timezone normalisation loss.
- **FR-004**: The system MUST enforce idempotency: re-delivering an event with the same event
  ID MUST NOT create a duplicate database record.
- **FR-005**: Malformed or schema-invalid events MUST be logged at ERROR level with the raw
  payload and rejection reason, then skipped; they MUST NOT cause consumer shutdown.
- **FR-006**: The system MUST maintain an audit table recording the processing outcome
  (PROCESSED, REJECTED, RETRY, DEAD_LETTERED) and error detail for every event attempt.
- **FR-007**: The system MUST expose a liveness endpoint (`/health`) and a readiness endpoint
  (`/ready`) suitable for cloud-native orchestration probes.
- **FR-008**: The system MUST expose operational metrics covering at minimum: events received,
  events persisted, events rejected, events duplicated, consumer lag, and processing latency
  percentiles.
- **FR-009**: The system MUST resume processing automatically after transient Kafka or database
  outages without manual intervention, using exponential back-off with a configurable maximum
  retry delay.
- **FR-010**: Kafka offsets MUST NOT be committed until the corresponding database write has been
  durably confirmed, ensuring at-least-once delivery semantics.
- **FR-011**: The data model MUST be versioned or schema-extensible so that new optional fields
  can be added to incoming events without breaking existing processing logic.
- **FR-012**: The system MUST support structured, correlation-ID-tagged logging across all
  processing paths (constitution Principle IV).

### Key Entities

- **SearchEvent**: The raw event as received from Kafka. Key attributes: event_id (unique),
  event_version, event_timestamp (original, source-system time), user_id, session_id,
  destination (free-form or structured), search_criteria (flexible key-value payload),
  search_result (outcome code + summary returned by source system), topic, partition, offset.

- **User**: Denormalised reference captured from the event. Key attributes: user_id,
  user_type, user_locale. Not a master-record; populated from event payload only.

- **Destination**: Represents the travel destination referenced in the search. Key attributes:
  destination_id (derived or assigned), destination_name, destination_type, region.

- **SearchRecord**: The persisted, validated representation of a search event. Links User,
  Destination, SearchCriteria, and SearchResult. Carries the original event_timestamp.

- **SearchCriteria**: Flexible key-value store of the search parameters (dates, passenger
  counts, cabin class, etc.) attached to a SearchRecord.

- **SearchResult**: The outcome returned by the source system: result_code, result_summary,
  result_count, response_time_ms. Attached to a SearchRecord.

- **EventAuditLog**: One row per processing attempt. Key attributes: audit_id, event_id,
  topic, partition, offset, status (PROCESSED/REJECTED/RETRY/DEAD_LETTERED), error_message,
  raw_payload_ref, processed_at.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: 100 % of well-formed events published to a configured topic are persisted within
  5 seconds of publication under normal operating conditions.
- **SC-002**: Zero duplicate records are created when the same event ID is delivered more than
  once, verified across at least 10,000 re-delivery attempts in acceptance testing.
- **SC-003**: The consumer resumes processing within 60 seconds of Kafka or database
  availability being restored, without manual restart.
- **SC-004**: The `/health` endpoint responds within 200 ms at all times, including during
  dependency outages.
- **SC-005**: Structured logs are emitted for 100 % of events (accepted and rejected) with a
  non-null correlation_id traceable back to the Kafka event_id.
- **SC-006**: Adding a new optional field to the event schema requires no code changes to the
  consumer if the field is not used by existing processing logic (verified by schema evolution
  test).
- **SC-PERF-001**: p99 end-to-end latency (Kafka publish → DB commit) MUST be ≤ 500 ms under
  a sustained load of 500 events/second on a single instance.
- **SC-PERF-002**: Throughput MUST be ≥ 500 events/second on a single instance under a
  representative load test with production-shaped payloads.

## Assumptions

- The source microservice publishes events in JSON format; Avro or Protobuf encoding is out of
  scope for v1 but the design must not preclude it.
- Event IDs are globally unique UUIDs assigned by the source system; the consumer trusts them
  for deduplication.
- The Kafka cluster is accessible via standard broker connection strings; authentication
  (SASL/TLS) configuration is environment-supplied and not hardcoded.
- A relational database (PostgreSQL assumed as the default target) is provisioned and
  accessible; schema migration is the responsibility of this service at startup.
- User and destination data in events are denormalised snapshots; the consumer does not look
  up or enrich from a master user or location service.
- A single consumer group is sufficient for v1; multi-group fan-out is a future concern.
- The dead-letter strategy for events that exhaust retries is to write them to the audit log
  as DEAD_LETTERED and emit an alert metric; a separate DLQ topic is out of scope for v1.
- Mobile and web front-end reporting queries against the persisted data are out of scope;
  this spec covers ingestion only.
- Cloud-native deployment targets Kubernetes; health probe paths follow the `/health` and
  `/ready` convention.
