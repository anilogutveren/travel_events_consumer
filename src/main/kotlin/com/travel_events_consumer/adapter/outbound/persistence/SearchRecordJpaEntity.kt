package com.travel_events_consumer.adapter.outbound.persistence

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes
import java.time.OffsetDateTime
import java.util.UUID

@Entity
@Table(name = "search_records")
class SearchRecordJpaEntity(
    @Id
    val id: UUID,

    @Column(name = "event_id", nullable = false, unique = true)
    val eventId: UUID,

    @Column(name = "event_version", nullable = false)
    val eventVersion: String,

    @Column(name = "event_timestamp", nullable = false)
    val eventTimestamp: OffsetDateTime,

    @Column(name = "user_id", nullable = false)
    val userId: String,

    @Column(name = "user_type")
    val userType: String?,

    @Column(name = "user_locale")
    val userLocale: String?,

    @Column(name = "session_id")
    val sessionId: String?,

    @Column(name = "destination_id", nullable = false)
    val destinationId: String,

    @Column(name = "destination_name", nullable = false)
    val destinationName: String,

    @Column(name = "destination_type")
    val destinationType: String?,

    @Column(name = "region")
    val region: String?,

    @Column(name = "country")
    val country: String?,

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "search_criteria", nullable = false, columnDefinition = "jsonb")
    val searchCriteria: Map<String, Any>,

    @Column(name = "result_code", nullable = false)
    val resultCode: String,

    @Column(name = "result_summary")
    val resultSummary: String?,

    @Column(name = "result_count")
    val resultCount: Int?,

    @Column(name = "response_time_ms")
    val responseTimeMs: Long?,

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "extra_fields", nullable = false, columnDefinition = "jsonb")
    val extraFields: Map<String, Any>,

    @Column(name = "topic", nullable = false)
    val topic: String,

    @Column(name = "partition_num", nullable = false)
    val partitionNum: Int,

    @Column(name = "kafka_offset", nullable = false)
    val kafkaOffset: Long,

    @Column(name = "created_at", nullable = false)
    val createdAt: OffsetDateTime = OffsetDateTime.now(),
)
