package com.travel_events_consumer.application.service

import com.travel_events_consumer.application.port.outbound.AuditLogRepository
import com.travel_events_consumer.domain.model.AuditLogNotFoundException
import com.travel_events_consumer.domain.model.EventAuditLog
import com.travel_events_consumer.domain.model.ProcessingStatus
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import java.time.OffsetDateTime
import java.util.UUID
import kotlin.test.assertEquals

class AuditLogQueryServiceTest {

    private val repository = mockk<AuditLogRepository>()
    private val service = AuditLogQueryService(repository)

    @Test
    fun `queryAuditLog delegates to repository with filter and pageable`() {
        val filter = AuditLogFilter(status = ProcessingStatus.REJECTED)
        val pageable = PageRequest.of(0, 10)
        val expected = PageImpl(listOf(sampleEntry()))
        every { repository.findAll(null, ProcessingStatus.REJECTED, null, null, pageable) } returns expected

        val result = service.queryAuditLog(filter, pageable)

        assertEquals(1, result.content.size)
        verify { repository.findAll(null, ProcessingStatus.REJECTED, null, null, pageable) }
    }

    @Test
    fun `getById returns entry when found`() {
        val id = UUID.randomUUID()
        val entry = sampleEntry(id = id)
        every { repository.findById(id) } returns entry

        val result = service.getById(id)
        assertEquals(id, result.id)
    }

    @Test
    fun `getById throws AuditLogNotFoundException when not found`() {
        val id = UUID.randomUUID()
        every { repository.findById(id) } returns null

        assertThrows<AuditLogNotFoundException> { service.getById(id) }
    }

    private fun sampleEntry(id: UUID = UUID.randomUUID()) = EventAuditLog(
        id = id,
        eventId = UUID.randomUUID().toString(),
        topic = "search-events",
        partition = 0,
        offset = 1L,
        status = ProcessingStatus.REJECTED,
        errorMessage = "test error",
        rawPayloadRef = null,
        attemptNumber = 1,
        processedAt = OffsetDateTime.now(),
    )
}
