package com.travel_events_consumer.domain.model

import com.travel_events_consumer.domain.model.valueobjects.EventId
import com.travel_events_consumer.domain.model.valueobjects.SessionId
import com.travel_events_consumer.domain.model.valueobjects.UserId
import java.time.OffsetDateTime
import java.util.UUID

data class SearchRecord(
    val id: UUID,
    val eventId: EventId,
    val eventVersion: String,
    val eventTimestamp: OffsetDateTime,
    val userId: UserId,
    val userType: String?,
    val userLocale: String?,
    val sessionId: SessionId?,
    val destination: Destination,
    val searchCriteria: Map<String, Any>,
    val searchResult: SearchResult,
    val extraFields: Map<String, Any>,
    val topic: String,
    val partition: Int,
    val offset: Long,
    val createdAt: OffsetDateTime = OffsetDateTime.now(),
)
