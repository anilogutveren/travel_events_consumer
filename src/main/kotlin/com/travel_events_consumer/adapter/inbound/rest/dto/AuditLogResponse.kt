package com.travel_events_consumer.adapter.inbound.rest.dto

import com.fasterxml.jackson.annotation.JsonProperty
import com.travel_events_consumer.domain.model.EventAuditLog
import com.travel_events_consumer.domain.model.ProcessingStatus
import java.time.OffsetDateTime
import java.util.UUID

data class AuditLogResponse(
    val id: UUID,
    @JsonProperty("event_id") val eventId: String,
    val topic: String,
    @JsonProperty("partition_num") val partitionNum: Int,
    @JsonProperty("kafka_offset") val kafkaOffset: Long,
    val status: ProcessingStatus,
    @JsonProperty("error_message") val errorMessage: String?,
    @JsonProperty("raw_payload_ref") val rawPayloadRef: String?,
    @JsonProperty("attempt_number") val attemptNumber: Int,
    @JsonProperty("processed_at") val processedAt: OffsetDateTime,
) {
    companion object {
        fun from(domain: EventAuditLog) = AuditLogResponse(
            id = domain.id,
            eventId = domain.eventId,
            topic = domain.topic,
            partitionNum = domain.partition,
            kafkaOffset = domain.offset,
            status = domain.status,
            errorMessage = domain.errorMessage,
            rawPayloadRef = domain.rawPayloadRef,
            attemptNumber = domain.attemptNumber,
            processedAt = domain.processedAt,
        )
    }
}
