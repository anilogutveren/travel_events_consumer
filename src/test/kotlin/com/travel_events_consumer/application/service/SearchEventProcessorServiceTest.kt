package com.travel_events_consumer.application.service

import com.travel_events_consumer.application.port.outbound.AuditLogRepository
import com.travel_events_consumer.application.port.outbound.SearchRecordRepository
import com.travel_events_consumer.domain.model.ProcessingStatus
import com.travel_events_consumer.domain.model.valueobjects.EventId
import com.travel_events_consumer.domain.service.DuplicateDetectionPolicy
import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.jupiter.api.Test
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.kafka.support.Acknowledgment
import java.time.OffsetDateTime
import java.util.UUID
import kotlin.test.assertEquals

class SearchEventProcessorServiceTest {

    private val searchRecordRepository = mockk<SearchRecordRepository>(relaxed = true)
    private val auditLogRepository = mockk<AuditLogRepository>(relaxed = true)
    private val duplicatePolicy = mockk<DuplicateDetectionPolicy>()
    private val meterRegistry = SimpleMeterRegistry()
    private val acknowledgment = mockk<Acknowledgment>(relaxed = true)

    private val service = SearchEventProcessorService(
        searchRecordRepository,
        auditLogRepository,
        duplicatePolicy,
        meterRegistry,
    )

    @Test
    fun `happy path persists record, writes PROCESSED audit, acks offset`() {
        val message = validMessage()
        every { duplicatePolicy.isDuplicate(any()) } returns false
        every { searchRecordRepository.save(any()) } answers { firstArg() }

        service.processEvent(message, acknowledgment)

        verify { searchRecordRepository.save(any()) }
        val auditSlot = slot<com.travel_events_consumer.domain.model.EventAuditLog>()
        verify { auditLogRepository.save(capture(auditSlot)) }
        assertEquals(ProcessingStatus.PROCESSED, auditSlot.captured.status)
        verify { acknowledgment.acknowledge() }
    }

    @Test
    fun `duplicate event writes DUPLICATE audit, does not save record, acks offset`() {
        val message = validMessage()
        every { duplicatePolicy.isDuplicate(any()) } returns true

        service.processEvent(message, acknowledgment)

        verify(exactly = 0) { searchRecordRepository.save(any()) }
        val auditSlot = slot<com.travel_events_consumer.domain.model.EventAuditLog>()
        verify { auditLogRepository.save(capture(auditSlot)) }
        assertEquals(ProcessingStatus.DUPLICATE, auditSlot.captured.status)
        verify { acknowledgment.acknowledge() }
    }

    @Test
    fun `DataIntegrityViolationException writes DUPLICATE audit and acks`() {
        val message = validMessage()
        every { duplicatePolicy.isDuplicate(any()) } returns false
        every { searchRecordRepository.save(any()) } throws DataIntegrityViolationException("duplicate key")

        service.processEvent(message, acknowledgment)

        val auditSlot = slot<com.travel_events_consumer.domain.model.EventAuditLog>()
        verify { auditLogRepository.save(capture(auditSlot)) }
        assertEquals(ProcessingStatus.DUPLICATE, auditSlot.captured.status)
        verify { acknowledgment.acknowledge() }
    }

    @Test
    fun `events_received counter incremented on every call`() {
        val message = validMessage()
        every { duplicatePolicy.isDuplicate(any()) } returns false
        every { searchRecordRepository.save(any()) } answers { firstArg() }

        service.processEvent(message, acknowledgment)

        val counter = meterRegistry.find("events.received").counter()
        assertEquals(1.0, counter?.count())
    }

    private fun validMessage() = com.travel_events_consumer.adapter.inbound.event.SearchEventMessage(
        eventId = UUID.randomUUID().toString(),
        eventVersion = "1.0",
        eventTimestamp = OffsetDateTime.now().toString(),
        user = com.travel_events_consumer.adapter.inbound.event.UserMessage("usr-001", "REGISTERED", "en-GB", null),
        destination = com.travel_events_consumer.adapter.inbound.event.DestinationMessage("BCN", "Barcelona", null, null, null),
        searchCriteria = emptyMap(),
        searchResult = com.travel_events_consumer.adapter.inbound.event.SearchResultMessage("SUCCESS", null, null, null),
        extraFields = emptyMap(),
    )
}
