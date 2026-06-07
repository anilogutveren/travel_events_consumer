package com.travel_events_consumer.domain.model.valueobjects

import java.util.UUID

data class EventId(val value: UUID) {
    companion object {
        fun of(raw: String): EventId {
            require(raw.isNotBlank()) { "EventId must not be blank" }
            return try {
                EventId(UUID.fromString(raw))
            } catch (e: IllegalArgumentException) {
                throw IllegalArgumentException("Invalid UUID for EventId: $raw", e)
            }
        }
    }
}
