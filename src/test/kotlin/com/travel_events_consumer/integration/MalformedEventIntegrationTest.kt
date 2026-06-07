package com.travel_events_consumer.integration

import org.awaitility.kotlin.await
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.kafka.core.KafkaTemplate
import java.time.Duration
import java.util.UUID
import kotlin.test.assertEquals

class MalformedEventIntegrationTest : AbstractIntegrationTest() {

    @Autowired
    lateinit var kafkaTemplate: KafkaTemplate<String, String>

    @Autowired
    lateinit var jdbcTemplate: JdbcTemplate

    @Test
    fun `malformed JSON does not create search record and writes REJECTED audit`() {
        val invalidPayload = "not valid json {{{"
        kafkaTemplate.send("search-events", invalidPayload)

        // Give consumer time to process the bad message
        Thread.sleep(2000)

        val recordCount = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM search_records",
            Int::class.java
        )
        assertEquals(0, recordCount)

        val rejectedCount = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM event_audit_log WHERE status = 'REJECTED'",
            Int::class.java
        )
        assertEquals(1, rejectedCount)
    }

    @Test
    fun `consumer continues processing valid events after malformed event`() {
        val badPayload = "not valid json {{{"
        val goodId = UUID.randomUUID().toString()
        val goodPayload = eventJson(goodId)

        kafkaTemplate.send("search-events", badPayload)
        kafkaTemplate.send("search-events", goodPayload)

        await.atMost(Duration.ofSeconds(10)).untilAsserted {
            val count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM search_records WHERE event_id = ?::uuid",
                Int::class.java, goodId
            )
            assertEquals(1, count, "Consumer should continue after malformed event")
        }
    }

    private fun eventJson(eventId: String) = """
        {
          "event_id": "$eventId",
          "event_version": "1.0",
          "event_timestamp": "2026-06-07T14:30:00Z",
          "user": {"user_id": "usr-post-error"},
          "destination": {"destination_id": "LHR", "destination_name": "London Heathrow"},
          "search_criteria": {},
          "search_result": {"result_code": "SUCCESS"}
        }
    """.trimIndent()
}
