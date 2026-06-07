package com.travel_events_consumer.domain.model.valueobjects

data class UserId(val value: String) {
    init {
        require(value.isNotBlank()) { "UserId must not be blank" }
    }
}
