package com.travel_events_consumer.domain.service

import com.travel_events_consumer.application.port.outbound.SearchRecordRepository
import com.travel_events_consumer.domain.model.valueobjects.EventId
import org.springframework.stereotype.Service

@Service
class DuplicateDetectionPolicy(private val searchRecordRepository: SearchRecordRepository) {

    fun isDuplicate(eventId: EventId): Boolean = searchRecordRepository.existsByEventId(eventId)
}
