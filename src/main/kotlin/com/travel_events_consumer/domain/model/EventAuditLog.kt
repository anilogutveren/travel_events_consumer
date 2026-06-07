package com.travel_events_consumer.domain.model

import java.time.OffsetDateTime
import java.util.UUID

data class EventAuditLog(
    val id: UUID,
    val eventId: String,
    val topic: String,
    val partition: Int,
    val offset: Long,
    val status: ProcessingStatus,
    val errorMessage: String?,
    val rawPayloadRef: String?,
    val attemptNumber: Int,
    val processedAt: OffsetDateTime,
)
