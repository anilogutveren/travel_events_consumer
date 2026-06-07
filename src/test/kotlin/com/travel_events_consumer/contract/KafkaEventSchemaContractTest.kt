package com.travel_events_consumer.contract

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.networknt.schema.JsonSchemaFactory
import com.networknt.schema.SpecVersion
import org.junit.jupiter.api.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class KafkaEventSchemaContractTest {

    private val mapper = jacksonObjectMapper()
    private val schemaFactory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V7)
    private val schema = schemaFactory.getSchema(
        javaClass.classLoader.getResourceAsStream("contracts/search-event-v1.json")
            ?: error("Contract schema not found on classpath")
    )

    @Test
    fun `valid event passes schema validation`() {
        val node = mapper.readTree(validEvent())
        val errors = schema.validate(node)
        assertTrue(errors.isEmpty(), "Expected no validation errors but got: $errors")
    }

    @Test
    fun `event missing destination fails schema validation`() {
        val json = validEvent().replace(""""destination":""", """"_destination":""")
        val node = mapper.readTree(json)
        val errors = schema.validate(node)
        assertFalse(errors.isEmpty(), "Expected validation errors for missing destination")
    }

    @Test
    fun `event missing user_id fails schema validation`() {
        val json = validEvent().replace(""""user_id": "usr-001"""", """"user_id": """"")
        val node = mapper.readTree(json)
        val errors = schema.validate(node)
        assertFalse(errors.isEmpty(), "Expected validation errors for blank user_id")
    }

    @Test
    fun `event with extra unknown top-level field passes schema validation`() {
        val withExtra = validEvent().trimEnd('}') + """, "new_field_v2": "extra-value"}"""
        val node: JsonNode = mapper.readTree(withExtra)
        val errors = schema.validate(node)
        assertTrue(errors.isEmpty(), "Unknown extra fields should be allowed: $errors")
    }

    private fun validEvent() = """
        {
          "event_id": "550e8400-e29b-41d4-a716-446655440001",
          "event_version": "1.0",
          "event_timestamp": "2026-06-07T14:30:00+01:00",
          "user": {"user_id": "usr-001", "user_type": "REGISTERED", "user_locale": "en-GB"},
          "destination": {"destination_id": "BCN", "destination_name": "Barcelona"},
          "search_criteria": {"departure_date": "2026-08-15", "adults": 2},
          "search_result": {"result_code": "SUCCESS", "result_count": 142}
        }
    """.trimIndent()
}
