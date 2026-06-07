package com.travel_events_consumer.adapter.outbound.persistence.mapper

import com.fasterxml.jackson.databind.ObjectMapper
import com.travel_events_consumer.adapter.outbound.persistence.SearchRecordJpaEntity
import com.travel_events_consumer.domain.model.Destination
import com.travel_events_consumer.domain.model.SearchRecord
import com.travel_events_consumer.domain.model.SearchResult
import com.travel_events_consumer.domain.model.valueobjects.EventId
import com.travel_events_consumer.domain.model.valueobjects.SessionId
import com.travel_events_consumer.domain.model.valueobjects.UserId
import org.springframework.stereotype.Component

@Component
class SearchRecordMapper(private val objectMapper: ObjectMapper) {

    fun toEntity(domain: SearchRecord): SearchRecordJpaEntity = SearchRecordJpaEntity(
        id = domain.id,
        eventId = domain.eventId.value,
        eventVersion = domain.eventVersion,
        eventTimestamp = domain.eventTimestamp,
        userId = domain.userId.value,
        userType = domain.userType,
        userLocale = domain.userLocale,
        sessionId = domain.sessionId?.value,
        destinationId = domain.destination.destinationId,
        destinationName = domain.destination.destinationName,
        destinationType = domain.destination.destinationType,
        region = domain.destination.region,
        country = domain.destination.country,
        searchCriteria = domain.searchCriteria,
        resultCode = domain.searchResult.resultCode,
        resultSummary = domain.searchResult.resultSummary,
        resultCount = domain.searchResult.resultCount,
        responseTimeMs = domain.searchResult.responseTimeMs,
        extraFields = domain.extraFields,
        topic = domain.topic,
        partitionNum = domain.partition,
        kafkaOffset = domain.offset,
        createdAt = domain.createdAt,
    )

    fun toDomain(entity: SearchRecordJpaEntity): SearchRecord = SearchRecord(
        id = entity.id,
        eventId = EventId(entity.eventId),
        eventVersion = entity.eventVersion,
        eventTimestamp = entity.eventTimestamp,
        userId = UserId(entity.userId),
        userType = entity.userType,
        userLocale = entity.userLocale,
        sessionId = entity.sessionId?.let { SessionId(it) },
        destination = Destination(
            destinationId = entity.destinationId,
            destinationName = entity.destinationName,
            destinationType = entity.destinationType,
            region = entity.region,
            country = entity.country,
        ),
        searchCriteria = entity.searchCriteria,
        searchResult = SearchResult(
            resultCode = entity.resultCode,
            resultSummary = entity.resultSummary,
            resultCount = entity.resultCount,
            responseTimeMs = entity.responseTimeMs,
        ),
        extraFields = entity.extraFields,
        topic = entity.topic,
        partition = entity.partitionNum,
        offset = entity.kafkaOffset,
        createdAt = entity.createdAt,
    )
}
