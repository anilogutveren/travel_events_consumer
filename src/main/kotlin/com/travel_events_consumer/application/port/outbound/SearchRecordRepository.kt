package com.travel_events_consumer.application.port.outbound

import com.travel_events_consumer.domain.model.SearchRecord
import com.travel_events_consumer.domain.model.valueobjects.EventId

interface SearchRecordRepository {
    fun save(record: SearchRecord): SearchRecord
    fun existsByEventId(eventId: EventId): Boolean
}
