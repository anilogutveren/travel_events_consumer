package com.travel_events_consumer.integration

import io.restassured.RestAssured
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.jdbc.core.JdbcTemplate
import org.hamcrest.CoreMatchers.equalTo
import java.util.UUID

class AuditLogRestIntegrationTest : AbstractIntegrationTest() {

    @Autowired
    lateinit var jdbcTemplate: JdbcTemplate

    @BeforeEach
    fun setup() {
        RestAssured.port = port
        jdbcTemplate.update("DELETE FROM event_audit_log")
        seedAuditRows()
    }

    @Test
    fun `GET audit-log returns paginated results`() {
        RestAssured.given()
            .get("/api/v1/audit-log?page=0&size=10")
            .then()
            .statusCode(200)
            .body("content.size()", equalTo(3))
            .body("total_elements", equalTo(3))
    }

    @Test
    fun `GET audit-log filtered by status returns matching entries`() {
        RestAssured.given()
            .get("/api/v1/audit-log?status=REJECTED")
            .then()
            .statusCode(200)
            .body("content.size()", equalTo(1))
            .body("content[0].status", equalTo("REJECTED"))
    }

    @Test
    fun `GET audit-log filtered by event_id returns matching entry`() {
        val eventId = "test-event-id-001"
        RestAssured.given()
            .get("/api/v1/audit-log?event_id=$eventId")
            .then()
            .statusCode(200)
            .body("content.size()", equalTo(1))
            .body("content[0].event_id", equalTo(eventId))
    }

    private fun seedAuditRows() {
        val insert = """
            INSERT INTO event_audit_log (id, event_id, topic, partition_num, kafka_offset, status, attempt_number, processed_at)
            VALUES (?::uuid, ?, 'search-events', 0, ?, ?, 1, now())
        """
        jdbcTemplate.update(insert, UUID.randomUUID().toString(), "test-event-id-001", 1L, "PROCESSED")
        jdbcTemplate.update(insert, UUID.randomUUID().toString(), "test-event-id-002", 2L, "PROCESSED")
        jdbcTemplate.update(insert, UUID.randomUUID().toString(), "test-event-id-003", 3L, "REJECTED")
    }
}
