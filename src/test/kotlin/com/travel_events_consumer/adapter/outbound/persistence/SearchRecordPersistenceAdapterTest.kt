package com.travel_events_consumer.adapter.outbound.persistence

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.travel_events_consumer.adapter.outbound.persistence.mapper.SearchRecordMapper
import com.travel_events_consumer.domain.model.Destination
import com.travel_events_consumer.domain.model.SearchRecord
import com.travel_events_consumer.domain.model.SearchResult
import com.travel_events_consumer.domain.model.valueobjects.EventId
import com.travel_events_consumer.domain.model.valueobjects.UserId
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.context.annotation.Import
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.test.context.ActiveProfiles
import java.time.OffsetDateTime
import java.util.UUID
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@DataJpaTest
@ActiveProfiles("test")
@Import(SearchRecordPersistenceAdapter::class, SearchRecordMapper::class)
class SearchRecordPersistenceAdapterTest {

    @Autowired
    lateinit var adapter: SearchRecordPersistenceAdapter

    @Test
    fun `save persists a search record`() {
        val record = buildRecord()
        val saved = adapter.save(record)
        assertTrue(adapter.existsByEventId(saved.eventId))
    }

    @Test
    fun `existsByEventId returns false when eventId is unknown`() {
        assertFalse(adapter.existsByEventId(EventId(UUID.randomUUID())))
    }

    @Test
    fun `save with duplicate eventId throws DataIntegrityViolationException`() {
        val eventId = EventId(UUID.randomUUID())
        adapter.save(buildRecord(eventId))
        assertThrows<DataIntegrityViolationException> { adapter.save(buildRecord(eventId)) }
    }

    private fun buildRecord(eventId: EventId = EventId(UUID.randomUUID())) = SearchRecord(
        id = UUID.randomUUID(),
        eventId = eventId,
        eventVersion = "1.0",
        eventTimestamp = OffsetDateTime.now(),
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
