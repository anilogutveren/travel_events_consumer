package com.travel_events_consumer.adapter.outbound.persistence

import com.travel_events_consumer.adapter.outbound.persistence.mapper.AuditLogMapper
import com.travel_events_consumer.application.port.outbound.AuditLogRepository
import com.travel_events_consumer.domain.model.EventAuditLog
import com.travel_events_consumer.domain.model.ProcessingStatus
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Component
import java.time.OffsetDateTime
import java.util.UUID

@Component
class AuditLogPersistenceAdapter(
    private val jpaRepository: AuditLogJpaRepository,
    private val mapper: AuditLogMapper,
) : AuditLogRepository {

    override fun save(entry: EventAuditLog): EventAuditLog {
        val saved = jpaRepository.save(mapper.toEntity(entry))
        return mapper.toDomain(saved)
    }

    override fun findByEventId(eventId: String): List<EventAuditLog> =
        jpaRepository.findByEventId(eventId).map { mapper.toDomain(it) }

    override fun findAll(
        eventId: String?,
        status: ProcessingStatus?,
        from: OffsetDateTime?,
        to: OffsetDateTime?,
        pageable: Pageable,
    ): Page<EventAuditLog> =
        jpaRepository.findWithFilters(eventId, status, from, to, pageable)
            .map { mapper.toDomain(it) }

    override fun findById(id: UUID): EventAuditLog? =
        jpaRepository.findById(id).orElse(null)?.let { mapper.toDomain(it) }
}
