package com.travel_events_consumer.adapter.inbound.rest

import com.travel_events_consumer.adapter.inbound.rest.dto.AuditLogResponse
import com.travel_events_consumer.application.port.inbound.QueryAuditLogUseCase
import com.travel_events_consumer.application.service.AuditLogFilter
import com.travel_events_consumer.domain.model.ProcessingStatus
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.time.OffsetDateTime
import java.util.UUID

@RestController
@RequestMapping("/api/v1/audit-log")
@Validated
class AuditLogController(private val queryAuditLogUseCase: QueryAuditLogUseCase) {

    @GetMapping
    fun queryAuditLog(
        @RequestParam(required = false) event_id: String?,
        @RequestParam(required = false) status: ProcessingStatus?,
        @RequestParam(required = false) from: OffsetDateTime?,
        @RequestParam(required = false) to: OffsetDateTime?,
        @RequestParam(defaultValue = "0") @Min(0) page: Int,
        @RequestParam(defaultValue = "20") @Min(1) @Max(100) size: Int,
    ): Page<AuditLogResponse> {
        val filter = AuditLogFilter(eventId = event_id, status = status, from = from, to = to)
        val pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "processedAt"))
        return queryAuditLogUseCase.queryAuditLog(filter, pageable).map { AuditLogResponse.from(it) }
    }

    @GetMapping("/{id}")
    fun getById(@PathVariable id: UUID): AuditLogResponse =
        AuditLogResponse.from(queryAuditLogUseCase.getById(id))
}
