package com.travel_events_consumer.adapter.outbound.persistence

import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface SearchRecordJpaRepository : JpaRepository<SearchRecordJpaEntity, UUID> {
    fun existsByEventId(eventId: UUID): Boolean
}
