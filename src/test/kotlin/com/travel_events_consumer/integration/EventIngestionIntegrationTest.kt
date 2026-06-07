package com.travel_events_consumer.integration

import org.awaitility.kotlin.await
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.kafka.core.KafkaTemplate
import java.time.Duration
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class EventIngestionIntegrationTest : AbstractIntegrationTest() {

    @Autowired
    lateinit var kafkaTemplate: KafkaTemplate<String, String>

    @Autowired
    lateinit var jdbcTemplate: JdbcTemplate

    @Test
    fun `valid event is persisted within 5 seconds`() {
        val eventId = UUID.randomUUID().toString()
        kafkaTemplate.send("search-events", eventJson(eventId))

        await.atMost(Duration.ofSeconds(5)).untilAsserted {
            val count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM search_records WHERE event_id = ?::uuid",
                Int::class.java, eventId
            )
            assertEquals(1, count)
        }
    }

    @Test
    fun `persisted record preserves original event_timestamp`() {
        val eventId = UUID.randomUUID().toString()
        val timestamp = "2026-06-07T14:30:00+01:00"
        kafkaTemplate.send("search-events", eventJson(eventId, timestamp = timestamp))

        await.atMost(Duration.ofSeconds(5)).untilAsserted {
            val row = jdbcTemplate.queryForMap(
                "SELECT event_timestamp FROM search_records WHERE event_id = ?::uuid", eventId
            )
            assertNotNull(row["event_timestamp"])
        }
    }

    @Test
    fun `PROCESSED audit row is written after successful persistence`() {
        val eventId = UUID.randomUUID().toString()
        kafkaTemplate.send("search-events", eventJson(eventId))

        await.atMost(Duration.ofSeconds(5)).untilAsserted {
            val row = jdbcTemplate.queryForMap(
                "SELECT status FROM event_audit_log WHERE event_id = ?", eventId
            )
            assertEquals("PROCESSED", row["status"])
        }
    }

    private fun eventJson(
        eventId: String,
        timestamp: String = "2026-06-07T14:30:00+01:00",
    ) = """
        {
          "event_id": "$eventId",
          "event_version": "1.0",
          "event_timestamp": "$timestamp",
          "user": {"user_id": "usr-001", "user_type": "REGISTERED", "user_locale": "en-GB"},
          "destination": {"destination_id": "BCN", "destination_name": "Barcelona"},
          "search_criteria": {"departure_date": "2026-08-15", "adults": 2},
          "search_result": {"result_code": "SUCCESS", "result_count": 142, "response_time_ms": 312}
        }
    """.trimIndent()
}
