package com.travel_events_consumer.domain.model

data class SearchResult(
    val resultCode: String,
    val resultSummary: String?,
    val resultCount: Int?,
    val responseTimeMs: Long?,
)
