package com.travel_events_consumer.application.port.inbound

import com.travel_events_consumer.application.service.AuditLogFilter
import com.travel_events_consumer.domain.model.EventAuditLog
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import java.util.UUID

interface QueryAuditLogUseCase {
    fun queryAuditLog(filter: AuditLogFilter, pageable: Pageable): Page<EventAuditLog>
    fun getById(id: UUID): EventAuditLog
}
