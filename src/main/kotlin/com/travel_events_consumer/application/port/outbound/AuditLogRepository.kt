package com.travel_events_consumer.application.port.outbound

import com.travel_events_consumer.domain.model.EventAuditLog
import com.travel_events_consumer.domain.model.ProcessingStatus
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import java.time.OffsetDateTime
import java.util.UUID

interface AuditLogRepository {
    fun save(entry: EventAuditLog): EventAuditLog
    fun findByEventId(eventId: String): List<EventAuditLog>
    fun findAll(
        eventId: String? = null,
        status: ProcessingStatus? = null,
        from: OffsetDateTime? = null,
        to: OffsetDateTime? = null,
        pageable: Pageable,
    ): Page<EventAuditLog>
    fun findById(id: UUID): EventAuditLog?
}
