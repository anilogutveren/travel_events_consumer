package com.travel_events_consumer.adapter.outbound.persistence

import com.travel_events_consumer.adapter.outbound.persistence.mapper.SearchRecordMapper
import com.travel_events_consumer.application.port.outbound.SearchRecordRepository
import com.travel_events_consumer.domain.model.SearchRecord
import com.travel_events_consumer.domain.model.valueobjects.EventId
import org.springframework.stereotype.Component

@Component
class SearchRecordPersistenceAdapter(
    private val jpaRepository: SearchRecordJpaRepository,
    private val mapper: SearchRecordMapper,
) : SearchRecordRepository {

    override fun save(record: SearchRecord): SearchRecord {
        val saved = jpaRepository.save(mapper.toEntity(record))
        return mapper.toDomain(saved)
    }

    override fun existsByEventId(eventId: EventId): Boolean =
        jpaRepository.existsByEventId(eventId.value)
}
