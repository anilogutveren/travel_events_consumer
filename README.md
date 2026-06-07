# Travel Events Consumer

Spring Boot 4 / Kotlin 2.4 service that consumes user-search events from Kafka and
persists them to PostgreSQL for reporting and analytics.

## Tech Stack

| Component | Version |
|---|---|
| Kotlin | 2.4.0 |
| Spring Boot | 4.0.6 |
| PostgreSQL | 16 |
| Flyway | 11.x |
| Testcontainers | 1.20.x |

## Environment Variables

| Variable | Description | Example |
|---|---|---|
| `DB_URL` | JDBC URL | `jdbc:postgresql://localhost:5432/travel_events` |
| `DB_USERNAME` | DB user | `travel_user` |
| `DB_PASSWORD` | DB password | `travel_pass` |
| `KAFKA_BOOTSTRAP_SERVERS` | Kafka brokers | `localhost:9092` |
| `KAFKA_TOPICS` | Comma-separated topic list | `search-events` |
| `KAFKA_LISTENER_CONCURRENCY` | Listener threads | `3` |

## Local Development

```bash
# Start infrastructure
docker compose up -d

# Run the service
./gradlew bootRun --args='--spring.profiles.active=local'
```

## Build & Test

```bash
# Build (includes all tests)
./gradlew build

# Tests only
./gradlew test

# Coverage report (target: ≥ 90%)
./gradlew jacocoTestReport
open build/reports/jacoco/test/html/index.html
```

## Endpoints

| Endpoint | Description |
|---|---|
| `GET /actuator/health` | Liveness probe |
| `GET /actuator/health/readiness` | Readiness probe |
| `GET /actuator/prometheus` | Prometheus metrics |
| `GET /api/v1/audit-log` | Paginated audit log query |
| `GET /api/v1/audit-log/{id}` | Single audit log entry |

See `specs/001-kafka-event-consumer/quickstart.md` for end-to-end validation scenarios.
