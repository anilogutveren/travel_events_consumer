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

class SchemaEvolutionIntegrationTest : AbstractIntegrationTest() {

    @Autowired
    lateinit var kafkaTemplate: KafkaTemplate<String, String>

    @Autowired
    lateinit var jdbcTemplate: JdbcTemplate

    @Test
    fun `event with unknown extra field is persisted and extra field stored in extra_fields column`() {
        val eventId = UUID.randomUUID().toString()
        kafkaTemplate.send("search-events", eventJsonWithExtraField(eventId))

        await.atMost(Duration.ofSeconds(5)).untilAsserted {
            val row = jdbcTemplate.queryForMap(
                "SELECT extra_fields::text as extra_fields FROM search_records WHERE event_id = ?::uuid",
                eventId
            )
            assertNotNull(row["extra_fields"])
            val extraFields = row["extra_fields"] as String
            assert(extraFields.contains("new_field_added_in_v2")) {
                "extra_fields should contain new_field_added_in_v2 but was: $extraFields"
            }
        }
    }

    @Test
    fun `event with extra field is not rejected — status is PROCESSED`() {
        val eventId = UUID.randomUUID().toString()
        kafkaTemplate.send("search-events", eventJsonWithExtraField(eventId))

        await.atMost(Duration.ofSeconds(5)).untilAsserted {
            val status = jdbcTemplate.queryForObject(
                "SELECT status FROM event_audit_log WHERE event_id = ?",
                String::class.java, eventId
            )
            assertEquals("PROCESSED", status)
        }
    }

    private fun eventJsonWithExtraField(eventId: String) = """
        {
          "event_id": "$eventId",
          "event_version": "1.0",
          "event_timestamp": "2026-06-07T16:00:00Z",
          "user": {"user_id": "usr-020"},
          "destination": {"destination_id": "MAD", "destination_name": "Madrid"},
          "search_criteria": {},
          "search_result": {"result_code": "SUCCESS"},
          "new_field_added_in_v2": "some-value"
        }
    """.trimIndent()
}
