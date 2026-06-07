package com.travel_events_consumer.domain.event

import com.travel_events_consumer.domain.model.ProcessingStatus
import java.time.OffsetDateTime

data class EventProcessingFailed(
    val eventId: String,
    val status: ProcessingStatus,
    val reason: String,
    val occurredAt: OffsetDateTime,
)
