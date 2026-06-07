package com.travel_events_consumer.application.service

import com.travel_events_consumer.domain.model.ProcessingStatus
import java.time.OffsetDateTime

data class AuditLogFilter(
    val eventId: String? = null,
    val status: ProcessingStatus? = null,
    val from: OffsetDateTime? = null,
    val to: OffsetDateTime? = null,
)
