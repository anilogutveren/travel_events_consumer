package com.travel_events_consumer.application.port.inbound

import com.travel_events_consumer.adapter.inbound.event.SearchEventMessage
import org.springframework.kafka.support.Acknowledgment

interface ProcessSearchEventUseCase {
    fun processEvent(message: SearchEventMessage, acknowledgment: Acknowledgment)
}
