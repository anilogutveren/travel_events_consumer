package com.travel_events_consumer.adapter.inbound.event

import com.travel_events_consumer.application.port.inbound.ProcessSearchEventUseCase
import com.travel_events_consumer.application.port.outbound.AuditLogRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.junit.jupiter.api.Test
import org.springframework.kafka.support.Acknowledgment

class KafkaSearchEventListenerTest {

    private val useCase = mockk<ProcessSearchEventUseCase>(relaxed = true)
    private val auditLogRepository = mockk<AuditLogRepository>(relaxed = true)
    private val acknowledgment = mockk<Acknowledgment>(relaxed = true)

    private val listener = KafkaSearchEventListener(useCase, auditLogRepository)

    @Test
    fun `valid JSON event calls use case and acknowledges`() {
        val record = consumerRecord(validJson())

        listener.onMessage(record, acknowledgment)

        verify { useCase.processEvent(any(), acknowledgment) }
    }

    @Test
    fun `invalid JSON writes REJECTED audit and acknowledges without calling use case`() {
        val record = consumerRecord("not valid json {{{")

        listener.onMessage(record, acknowledgment)

        verify(exactly = 0) { useCase.processEvent(any(), any()) }
        verify { auditLogRepository.save(match { it.status.name == "REJECTED" }) }
        verify { acknowledgment.acknowledge() }
    }

    @Test
    fun `missing required field writes REJECTED audit and acknowledges`() {
        val record = consumerRecord(missingDestinationJson())

        listener.onMessage(record, acknowledgment)

        verify(exactly = 0) { useCase.processEvent(any(), any()) }
        verify { auditLogRepository.save(match { it.status.name == "REJECTED" }) }
        verify { acknowledgment.acknowledge() }
    }

    private fun consumerRecord(value: String) =
        ConsumerRecord<String, String>("search-events", 0, 1L, null, value)

    private fun validJson() = """
        {
          "event_id": "550e8400-e29b-41d4-a716-446655440001",
          "event_version": "1.0",
          "event_timestamp": "2026-06-07T14:30:00+01:00",
          "user": {"user_id": "usr-001"},
          "destination": {"destination_id": "BCN", "destination_name": "Barcelona"},
          "search_criteria": {},
          "search_result": {"result_code": "SUCCESS"}
        }
    """.trimIndent()

    private fun missingDestinationJson() = """
        {
          "event_id": "550e8400-e29b-41d4-a716-446655440002",
          "event_version": "1.0",
          "event_timestamp": "2026-06-07T14:30:00Z",
          "user": {"user_id": "usr-002"},
          "search_criteria": {},
          "search_result": {"result_code": "SUCCESS"}
        }
    """.trimIndent()
}
