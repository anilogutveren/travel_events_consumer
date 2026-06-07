package com.travel_events_consumer.adapter.outbound.persistence

import com.travel_events_consumer.domain.model.ProcessingStatus
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.OffsetDateTime
import java.util.UUID

@Entity
@Table(name = "event_audit_log")
class EventAuditLogJpaEntity(
    @Id
    val id: UUID,

    @Column(name = "event_id", nullable = false)
    val eventId: String,

    @Column(name = "topic", nullable = false)
    val topic: String,

    @Column(name = "partition_num", nullable = false)
    val partitionNum: Int,

    @Column(name = "kafka_offset", nullable = false)
    val kafkaOffset: Long,

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    val status: ProcessingStatus,

    @Column(name = "error_message", length = 2000)
    val errorMessage: String?,

    @Column(name = "raw_payload_ref", length = 500)
    val rawPayloadRef: String?,

    @Column(name = "attempt_number", nullable = false)
    val attemptNumber: Int,

    @Column(name = "processed_at", nullable = false)
    val processedAt: OffsetDateTime,
)
