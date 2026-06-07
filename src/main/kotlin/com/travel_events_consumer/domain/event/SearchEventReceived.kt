package com.travel_events_consumer.domain.event

import com.travel_events_consumer.domain.model.valueobjects.EventId
import java.time.OffsetDateTime

data class SearchEventReceived(
    val eventId: EventId,
    val occurredAt: OffsetDateTime,
)
