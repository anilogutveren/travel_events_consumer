package com.travel_events_consumer.adapter.outbound.persistence

import com.travel_events_consumer.adapter.outbound.persistence.mapper.AuditLogMapper
import com.travel_events_consumer.domain.model.EventAuditLog
import com.travel_events_consumer.domain.model.ProcessingStatus
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.context.annotation.Import
import org.springframework.data.domain.PageRequest
import org.springframework.test.context.ActiveProfiles
import java.time.OffsetDateTime
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

@DataJpaTest
@ActiveProfiles("test")
@Import(AuditLogPersistenceAdapter::class, AuditLogMapper::class)
class AuditLogPersistenceAdapterTest {

    @Autowired
    lateinit var adapter: AuditLogPersistenceAdapter

    @Test
    fun `save persists audit log entry`() {
        val entry = buildEntry()
        adapter.save(entry)
        val found = adapter.findByEventId(entry.eventId)
        assertEquals(1, found.size)
        assertEquals(ProcessingStatus.PROCESSED, found[0].status)
    }

    @Test
    fun `findByEventId returns empty list for unknown eventId`() {
        val result = adapter.findByEventId("unknown-id")
        assertEquals(0, result.size)
    }

    @Test
    fun `findById returns entry when present`() {
        val entry = buildEntry()
        adapter.save(entry)
        val found = adapter.findById(entry.id)
        assertNotNull(found)
        assertEquals(entry.eventId, found.eventId)
    }

    @Test
    fun `findAll with status filter returns only matching entries`() {
        adapter.save(buildEntry(status = ProcessingStatus.PROCESSED))
        adapter.save(buildEntry(status = ProcessingStatus.REJECTED))

        val page = adapter.findAll(status = ProcessingStatus.REJECTED, pageable = PageRequest.of(0, 10))
        assertEquals(1, page.content.size)
        assertEquals(ProcessingStatus.REJECTED, page.content[0].status)
    }

    private fun buildEntry(status: ProcessingStatus = ProcessingStatus.PROCESSED) = EventAuditLog(
        id = UUID.randomUUID(),
        eventId = UUID.randomUUID().toString(),
        topic = "search-events",
        partition = 0,
        offset = 1L,
        status = status,
        errorMessage = null,
        rawPayloadRef = null,
        attemptNumber = 1,
        processedAt = OffsetDateTime.now(),
    )
}
