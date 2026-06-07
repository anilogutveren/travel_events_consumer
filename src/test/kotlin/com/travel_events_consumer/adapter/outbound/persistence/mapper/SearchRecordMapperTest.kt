package com.travel_events_consumer.adapter.outbound.persistence.mapper

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.travel_events_consumer.domain.model.Destination
import com.travel_events_consumer.domain.model.SearchRecord
import com.travel_events_consumer.domain.model.SearchResult
import com.travel_events_consumer.domain.model.valueobjects.EventId
import com.travel_events_consumer.domain.model.valueobjects.SessionId
import com.travel_events_consumer.domain.model.valueobjects.UserId
import org.junit.jupiter.api.Test
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.UUID
import kotlin.test.assertEquals

class SearchRecordMapperTest {

    private val mapper = SearchRecordMapper(jacksonObjectMapper().apply {
        findAndRegisterModules()
    })

    @Test
    fun `maps domain SearchRecord to JPA entity and back preserving all fields`() {
        val eventTimestamp = OffsetDateTime.of(2026, 6, 7, 14, 30, 0, 0, ZoneOffset.ofHours(1))
        val record = SearchRecord(
            id = UUID.randomUUID(),
            eventId = EventId(UUID.randomUUID()),
            eventVersion = "1.0",
            eventTimestamp = eventTimestamp,
            userId = UserId("usr-001"),
            userType = "REGISTERED",
            userLocale = "en-GB",
            sessionId = SessionId("sess-abc"),
            destination = Destination("BCN", "Barcelona", "CITY", "Southern Europe", "ES"),
            searchCriteria = mapOf("departure_date" to "2026-08-15", "adults" to 2),
            searchResult = SearchResult("SUCCESS", "142 flights", 142, 312L),
            extraFields = mapOf("new_field" to "value"),
            topic = "search-events",
            partition = 0,
            offset = 100L,
        )

        val entity = mapper.toEntity(record)
        val roundTripped = mapper.toDomain(entity)

        assertEquals(record.eventId, roundTripped.eventId)
        assertEquals(record.eventVersion, roundTripped.eventVersion)
        assertEquals(record.userId, roundTripped.userId)
        assertEquals(record.destination, roundTripped.destination)
        assertEquals(record.searchResult, roundTripped.searchResult)
        assertEquals(record.topic, roundTripped.topic)
        assertEquals(record.partition, roundTripped.partition)
        assertEquals(record.offset, roundTripped.offset)
    }

    @Test
    fun `preserves event_timestamp timezone offset exactly`() {
        val offsetPlus1 = OffsetDateTime.of(2026, 6, 7, 14, 30, 0, 0, ZoneOffset.ofHours(1))
        val record = minimalRecord(eventTimestamp = offsetPlus1)

        val entity = mapper.toEntity(record)
        val roundTripped = mapper.toDomain(entity)

        assertEquals(offsetPlus1.toInstant(), roundTripped.eventTimestamp.toInstant())
    }

    private fun minimalRecord(eventTimestamp: OffsetDateTime = OffsetDateTime.now()) = SearchRecord(
        id = UUID.randomUUID(),
        eventId = EventId(UUID.randomUUID()),
        eventVersion = "1.0",
        eventTimestamp = eventTimestamp,
        userId = UserId("usr-001"),
        userType = null,
        userLocale = null,
        sessionId = null,
        destination = Destination("BCN", "Barcelona", null, null, null),
        searchCriteria = emptyMap(),
        searchResult = SearchResult("SUCCESS", null, null, null),
        extraFields = emptyMap(),
        topic = "search-events",
        partition = 0,
        offset = 1L,
    )
}
