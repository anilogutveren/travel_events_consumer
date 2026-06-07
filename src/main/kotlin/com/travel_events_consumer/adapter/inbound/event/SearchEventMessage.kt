package com.travel_events_consumer.adapter.inbound.event

import com.fasterxml.jackson.annotation.JsonAnySetter
import com.fasterxml.jackson.annotation.JsonProperty

data class SearchEventMessage(
    @JsonProperty("event_id") val eventId: String,
    @JsonProperty("event_version") val eventVersion: String,
    @JsonProperty("event_timestamp") val eventTimestamp: String,
    val user: UserMessage,
    val destination: DestinationMessage,
    @JsonProperty("search_criteria") val searchCriteria: Map<String, Any>,
    @JsonProperty("search_result") val searchResult: SearchResultMessage,
    val extraFields: MutableMap<String, Any> = mutableMapOf(),
) {
    @JsonAnySetter
    fun setExtraField(key: String, value: Any) {
        val knownKeys = setOf("event_id", "event_version", "event_timestamp", "user", "destination", "search_criteria", "search_result")
        if (key !in knownKeys) extraFields[key] = value
    }
}

data class UserMessage(
    @JsonProperty("user_id") val userId: String,
    @JsonProperty("user_type") val userType: String?,
    @JsonProperty("user_locale") val userLocale: String?,
    @JsonProperty("session_id") val sessionId: String?,
)

data class DestinationMessage(
    @JsonProperty("destination_id") val destinationId: String,
    @JsonProperty("destination_name") val destinationName: String,
    @JsonProperty("destination_type") val destinationType: String?,
    val region: String?,
    val country: String?,
)

data class SearchResultMessage(
    @JsonProperty("result_code") val resultCode: String,
    @JsonProperty("result_summary") val resultSummary: String?,
    @JsonProperty("result_count") val resultCount: Int?,
    @JsonProperty("response_time_ms") val responseTimeMs: Long?,
)
