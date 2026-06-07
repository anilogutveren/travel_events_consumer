package com.travel_events_consumer.adapter.outbound.persistence.mapper

import com.travel_events_consumer.adapter.outbound.persistence.EventAuditLogJpaEntity
import com.travel_events_consumer.domain.model.EventAuditLog
import org.springframework.stereotype.Component

@Component
class AuditLogMapper {

    fun toEntity(domain: EventAuditLog): EventAuditLogJpaEntity = EventAuditLogJpaEntity(
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

    fun toDomain(entity: EventAuditLogJpaEntity): EventAuditLog = EventAuditLog(
        id = entity.id,
        eventId = entity.eventId,
        topic = entity.topic,
        partition = entity.partitionNum,
        offset = entity.kafkaOffset,
        status = entity.status,
        errorMessage = entity.errorMessage,
        rawPayloadRef = entity.rawPayloadRef,
        attemptNumber = entity.attemptNumber,
        processedAt = entity.processedAt,
    )
}
