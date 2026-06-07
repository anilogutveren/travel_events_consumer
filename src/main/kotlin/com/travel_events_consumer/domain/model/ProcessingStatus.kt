package com.travel_events_consumer.domain.model

enum class ProcessingStatus {
    PROCESSED,
    REJECTED,
    RETRY,
    DUPLICATE,
    DEAD_LETTERED,
}
