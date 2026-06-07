package com.travel_events_consumer.adapter.outbound.persistence.mapper

import com.travel_events_consumer.domain.model.EventAuditLog
import com.travel_events_consumer.domain.model.ProcessingStatus
import org.junit.jupiter.api.Test
import java.time.OffsetDateTime
import java.util.UUID
import kotlin.test.assertEquals

class AuditLogMapperTest {

    private val mapper = AuditLogMapper()

    @Test
    fun `maps domain EventAuditLog to entity and back for all statuses`() {
        ProcessingStatus.entries.forEach { status ->
            val log = EventAuditLog(
                id = UUID.randomUUID(),
                eventId = UUID.randomUUID().toString(),
                topic = "search-events",
                partition = 0,
                offset = 42L,
                status = status,
                errorMessage = if (status == ProcessingStatus.REJECTED) "missing field" else null,
                rawPayloadRef = if (status == ProcessingStatus.REJECTED) "{}" else null,
                attemptNumber = 1,
                processedAt = OffsetDateTime.now(),
            )

            val entity = mapper.toEntity(log)
            val roundTripped = mapper.toDomain(entity)

            assertEquals(log.eventId, roundTripped.eventId)
            assertEquals(log.status, roundTripped.status)
            assertEquals(log.errorMessage, roundTripped.errorMessage)
            assertEquals(log.attemptNumber, roundTripped.attemptNumber)
        }
    }
}
