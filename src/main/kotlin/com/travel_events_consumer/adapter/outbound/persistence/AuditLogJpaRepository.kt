package com.travel_events_consumer.adapter.outbound.persistence

import com.travel_events_consumer.domain.model.ProcessingStatus
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.time.OffsetDateTime
import java.util.UUID

interface AuditLogJpaRepository : JpaRepository<EventAuditLogJpaEntity, UUID> {

    fun findByEventId(eventId: String): List<EventAuditLogJpaEntity>

    @Query("""
        SELECT a FROM EventAuditLogJpaEntity a
        WHERE (:eventId IS NULL OR a.eventId = :eventId)
          AND (:status IS NULL OR a.status = :status)
          AND (:from IS NULL OR a.processedAt >= :from)
          AND (:to IS NULL OR a.processedAt <= :to)
    """)
    fun findWithFilters(
        @Param("eventId") eventId: String?,
        @Param("status") status: ProcessingStatus?,
        @Param("from") from: OffsetDateTime?,
        @Param("to") to: OffsetDateTime?,
        pageable: Pageable,
    ): Page<EventAuditLogJpaEntity>
}
