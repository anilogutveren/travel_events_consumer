package com.travel_events_consumer.application.service

import com.travel_events_consumer.application.port.inbound.QueryAuditLogUseCase
import com.travel_events_consumer.application.port.outbound.AuditLogRepository
import com.travel_events_consumer.domain.model.AuditLogNotFoundException
import com.travel_events_consumer.domain.model.EventAuditLog
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
@Transactional(readOnly = true)
class AuditLogQueryService(private val auditLogRepository: AuditLogRepository) : QueryAuditLogUseCase {

    override fun queryAuditLog(filter: AuditLogFilter, pageable: Pageable): Page<EventAuditLog> =
        auditLogRepository.findAll(
            eventId = filter.eventId,
            status = filter.status,
            from = filter.from,
            to = filter.to,
            pageable = pageable,
        )

    override fun getById(id: UUID): EventAuditLog =
        auditLogRepository.findById(id) ?: throw AuditLogNotFoundException(id)
}
