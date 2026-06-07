package com.travel_events_consumer.domain.model

data class Destination(
    val destinationId: String,
    val destinationName: String,
    val destinationType: String?,
    val region: String?,
    val country: String?,
)
