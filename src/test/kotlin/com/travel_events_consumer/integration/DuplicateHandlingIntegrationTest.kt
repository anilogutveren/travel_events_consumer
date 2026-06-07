package com.travel_events_consumer.integration

import org.awaitility.kotlin.await
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.kafka.core.KafkaTemplate
import java.time.Duration
import java.util.UUID
import kotlin.test.assertEquals

class DuplicateHandlingIntegrationTest : AbstractIntegrationTest() {

    @Autowired
    lateinit var kafkaTemplate: KafkaTemplate<String, String>

    @Autowired
    lateinit var jdbcTemplate: JdbcTemplate

    @Test
    fun `publishing same event twice creates one record and two audit rows`() {
        val eventId = UUID.randomUUID().toString()
        val payload = eventJson(eventId)

        kafkaTemplate.send("search-events", payload)
        kafkaTemplate.send("search-events", payload)

        await.atMost(Duration.ofSeconds(10)).untilAsserted {
            val recordCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM search_records WHERE event_id = ?::uuid",
                Int::class.java, eventId
            )
            assertEquals(1, recordCount, "Expected exactly 1 search record")

            val auditCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM event_audit_log WHERE event_id = ?",
                Int::class.java, eventId
            )
            assertEquals(2, auditCount, "Expected exactly 2 audit rows (PROCESSED + DUPLICATE)")
        }
    }

    @Test
    fun `second occurrence is recorded as DUPLICATE in audit log`() {
        val eventId = UUID.randomUUID().toString()
        val payload = eventJson(eventId)

        kafkaTemplate.send("search-events", payload)
        kafkaTemplate.send("search-events", payload)

        await.atMost(Duration.ofSeconds(10)).untilAsserted {
            val statuses = jdbcTemplate.queryForList(
                "SELECT status FROM event_audit_log WHERE event_id = ? ORDER BY processed_at",
                String::class.java, eventId
            )
            assertEquals(listOf("PROCESSED", "DUPLICATE"), statuses)
        }
    }

    private fun eventJson(eventId: String) = """
        {
          "event_id": "$eventId",
          "event_version": "1.0",
          "event_timestamp": "2026-06-07T14:30:00+01:00",
          "user": {"user_id": "usr-dup"},
          "destination": {"destination_id": "MAD", "destination_name": "Madrid"},
          "search_criteria": {},
          "search_result": {"result_code": "SUCCESS"}
        }
    """.trimIndent()
}
