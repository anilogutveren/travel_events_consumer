# Quickstart Validation Guide: Kafka Search Event Consumer

**Branch**: `001-kafka-event-consumer` | **Date**: 2026-06-07

This guide describes how to validate that the service works end-to-end after
implementation. It covers prerequisites, startup, and scenario-by-scenario
verification steps. See [data-model.md](data-model.md) for schema details and
[contracts/](contracts/) for the event schema and REST API contract.

---

## Prerequisites

- Docker Desktop (or Podman) running
- Java 21 JDK installed (`java -version` → `21.x`)
- Gradle wrapper available (`.gradlew`)
- `kcat` (formerly `kafkacat`) or any Kafka CLI tool installed
- `curl` or HTTPie installed
- `psql` installed (or any PostgreSQL client)

---

## 1. Start Infrastructure

```bash
# From repo root — starts PostgreSQL and Kafka via Docker Compose
docker compose up -d

# Verify both are healthy
docker compose ps
```

Expected: `kafka` and `postgres` containers show `healthy` status.

---

## 2. Run Database Migrations

Migrations run automatically at application startup via Flyway.
To verify manually:

```bash
# Connect to PostgreSQL
psql postgresql://localhost:5432/travel_events -U travel_user

# Check migration history
SELECT version, description, success FROM flyway_schema_history ORDER BY installed_rank;
```

Expected: Two rows — `V1` (search_records) and `V2` (event_audit_log) with `success = true`.

---

## 3. Start the Application

```bash
./gradlew bootRun --args='--spring.profiles.active=local'
```

Expected startup log output (JSON):
```json
{"level":"INFO","message":"Kafka consumer started","topic":"search-events","group":"travel-events-consumer-group"}
{"level":"INFO","message":"Application started on port 8080"}
```

---

## 4. Verify Health & Readiness

```bash
# Liveness
curl -s http://localhost:8080/actuator/health | jq .
# Expected: {"status":"UP"}

# Readiness — both Kafka and DB must be UP
curl -s http://localhost:8080/actuator/health/readiness | jq .
# Expected: {"status":"UP","components":{"kafka":{"status":"UP"},"db":{"status":"UP"}}}
```

---

## 5. Scenario 1: Valid Event Ingestion (US1 — P1)

Publish a well-formed event matching the schema in
[contracts/kafka-event-schema-v1.json](contracts/kafka-event-schema-v1.json):

```bash
kcat -P -b localhost:9092 -t search-events <<'EOF'
{
  "event_id": "550e8400-e29b-41d4-a716-446655440001",
  "event_version": "1.0",
  "event_timestamp": "2026-06-07T14:30:00+01:00",
  "user": {"user_id": "usr-001", "user_type": "REGISTERED", "user_locale": "en-GB"},
  "destination": {"destination_id": "BCN", "destination_name": "Barcelona", "destination_type": "CITY", "country": "ES"},
  "search_criteria": {"departure_date": "2026-08-15", "adults": 2, "cabin_class": "ECONOMY"},
  "search_result": {"result_code": "SUCCESS", "result_count": 142, "response_time_ms": 312}
}
EOF
```

**Verify persistence** (within 5 seconds):
```sql
SELECT event_id, user_id, destination_name, result_code, event_timestamp
FROM search_records
WHERE event_id = '550e8400-e29b-41d4-a716-446655440001';
```
Expected: One row. `event_timestamp` = `2026-06-07T14:30:00+01:00` preserved exactly.

**Verify audit log**:
```sql
SELECT event_id, status, error_message
FROM event_audit_log
WHERE event_id = '550e8400-e29b-41d4-a716-446655440001';
```
Expected: One row with `status = 'PROCESSED'`, `error_message = null`.

---

## 6. Scenario 2: Duplicate Event Idempotency (US1 — P1)

Publish the exact same event from Scenario 1 again:

```bash
kcat -P -b localhost:9092 -t search-events <<'EOF'
{ "event_id": "550e8400-e29b-41d4-a716-446655440001", ... (same payload) }
EOF
```

**Verify no duplicate**:
```sql
SELECT COUNT(*) FROM search_records
WHERE event_id = '550e8400-e29b-41d4-a716-446655440001';
```
Expected: `COUNT = 1` (unchanged).

**Verify audit log records duplicate**:
```sql
SELECT status FROM event_audit_log
WHERE event_id = '550e8400-e29b-41d4-a716-446655440001'
ORDER BY processed_at;
```
Expected: Two rows — `PROCESSED` then `DUPLICATE`.

---

## 7. Scenario 3: Malformed Event Rejection (US1 — P1)

Publish an event missing the required `destination` field:

```bash
kcat -P -b localhost:9092 -t search-events <<'EOF'
{
  "event_id": "550e8400-e29b-41d4-a716-000000000002",
  "event_version": "1.0",
  "event_timestamp": "2026-06-07T15:00:00Z",
  "user": {"user_id": "usr-002"},
  "search_criteria": {},
  "search_result": {"result_code": "SUCCESS"}
}
EOF
```

**Verify no record created**:
```sql
SELECT COUNT(*) FROM search_records
WHERE event_id = '550e8400-e29b-41d4-a716-000000000002';
```
Expected: `COUNT = 0`.

**Verify audit log records rejection**:
```sql
SELECT status, error_message FROM event_audit_log
WHERE event_id = '550e8400-e29b-41d4-a716-000000000002';
```
Expected: One row with `status = 'REJECTED'`, `error_message` containing reason.

**Verify consumer continued**: Publish another valid event and confirm it is persisted
(the consumer must not have stopped).

---

## 8. Scenario 4: Metrics Visibility (US2 — P2)

```bash
curl -s http://localhost:8080/actuator/prometheus | grep events_
```

Expected output includes:
```
events_received_total{...} 3.0
events_persisted_total{...} 1.0
events_rejected_total{...} 1.0
events_duplicate_total{...} 1.0
```

---

## 9. Scenario 5: Audit Log REST Query (US3 — P3)

```bash
# Query all audit entries
curl -s "http://localhost:8080/api/v1/audit-log?page=0&size=10" | jq .

# Filter by status
curl -s "http://localhost:8080/api/v1/audit-log?status=REJECTED" | jq .

# Filter by event_id
curl -s "http://localhost:8080/api/v1/audit-log?event_id=550e8400-e29b-41d4-a716-446655440001" | jq .
```

Expected: Paginated JSON responses matching the schema in
[contracts/rest-api-openapi.yml](contracts/rest-api-openapi.yml).

---

## 10. Resilience: Database Outage Recovery (Edge Case)

```bash
# Stop PostgreSQL
docker compose stop postgres

# Publish an event (will be buffered in Kafka)
kcat -P -b localhost:9092 -t search-events <<'EOF'
{"event_id": "550e8400-e29b-41d4-a716-000000000010", ...}
EOF

# Readiness should report not-ready
curl -s http://localhost:8080/actuator/health/readiness
# Expected: {"status":"DOWN",...}

# Restore PostgreSQL
docker compose start postgres

# Within 60 seconds, readiness should recover
curl -s http://localhost:8080/actuator/health/readiness
# Expected: {"status":"UP",...}

# Verify event was persisted after recovery
SELECT event_id FROM search_records
WHERE event_id = '550e8400-e29b-41d4-a716-000000000010';
```

---

## 11. Schema Evolution: Unknown Field Tolerance (SC-006)

Publish an event with an additional unknown top-level field:

```bash
kcat -P -b localhost:9092 -t search-events <<'EOF'
{
  "event_id": "550e8400-e29b-41d4-a716-000000000020",
  "event_version": "1.0",
  "event_timestamp": "2026-06-07T16:00:00Z",
  "user": {"user_id": "usr-020"},
  "destination": {"destination_id": "MAD", "destination_name": "Madrid"},
  "search_criteria": {},
  "search_result": {"result_code": "SUCCESS"},
  "new_field_added_in_v2": "some-value"
}
EOF
```

**Verify event was persisted** (not rejected):
```sql
SELECT event_id, extra_fields FROM search_records
WHERE event_id = '550e8400-e29b-41d4-a716-000000000020';
```
Expected: One row. `extra_fields` contains `{"new_field_added_in_v2": "some-value"}`.

---

## Definition of Done Checklist

- [ ] All 5 scenarios above pass without errors
- [ ] Metrics counters match expected event counts
- [ ] No consumer restart required during resilience test
- [ ] Audit REST API returns correct paginated results
- [ ] Schema evolution test passes without rejection
