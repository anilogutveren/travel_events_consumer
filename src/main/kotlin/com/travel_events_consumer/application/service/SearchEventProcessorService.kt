package com.travel_events_consumer.application.service

import com.travel_events_consumer.adapter.inbound.event.SearchEventMessage
import com.travel_events_consumer.application.port.inbound.ProcessSearchEventUseCase
import com.travel_events_consumer.application.port.outbound.AuditLogRepository
import com.travel_events_consumer.application.port.outbound.SearchRecordRepository
import com.travel_events_consumer.domain.model.Destination
import com.travel_events_consumer.domain.model.EventAuditLog
import com.travel_events_consumer.domain.model.ProcessingStatus
import com.travel_events_consumer.domain.model.SearchRecord
import com.travel_events_consumer.domain.model.SearchResult
import com.travel_events_consumer.domain.model.valueobjects.EventId
import com.travel_events_consumer.domain.model.valueobjects.SessionId
import com.travel_events_consumer.domain.model.valueobjects.UserId
import com.travel_events_consumer.domain.service.DuplicateDetectionPolicy
import io.micrometer.core.instrument.MeterRegistry
import org.slf4j.LoggerFactory
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.kafka.support.Acknowledgment
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.OffsetDateTime
import java.util.UUID

@Service
class SearchEventProcessorService(
    private val searchRecordRepository: SearchRecordRepository,
    private val auditLogRepository: AuditLogRepository,
    private val duplicateDetectionPolicy: DuplicateDetectionPolicy,
    private val meterRegistry: MeterRegistry,
) : ProcessSearchEventUseCase {

    private val log = LoggerFactory.getLogger(javaClass)

    private val eventsReceived = meterRegistry.counter("events.received")
    private val eventsPersisted = meterRegistry.counter("events.persisted")
    private val eventsRejected = meterRegistry.counter("events.rejected")
    private val eventsDuplicate = meterRegistry.counter("events.duplicate")
    private val processingTimer = meterRegistry.timer("events.processing.duration")

    @Transactional
    override fun processEvent(message: SearchEventMessage, acknowledgment: Acknowledgment) {
        eventsReceived.increment()
        processingTimer.record {
            runCatching { processInternal(message, acknowledgment) }
                .onFailure { ex ->
                    log.error("Unexpected error processing event ${message.eventId}", ex)
                    writeAudit(message, ProcessingStatus.REJECTED, ex.message, null)
                    acknowledgment.acknowledge()
                }
        }
    }

    private fun processInternal(message: SearchEventMessage, acknowledgment: Acknowledgment) {
        val eventId = EventId.of(message.eventId)

        if (duplicateDetectionPolicy.isDuplicate(eventId)) {
            log.info("Duplicate event skipped: {}", message.eventId)
            eventsDuplicate.increment()
            writeAudit(message, ProcessingStatus.DUPLICATE, null, null)
            acknowledgment.acknowledge()
            return
        }

        try {
            val record = toSearchRecord(message)
            searchRecordRepository.save(record)
            eventsPersisted.increment()
            writeAudit(message, ProcessingStatus.PROCESSED, null, null)
            log.info("Event persisted: {}", message.eventId)
        } catch (ex: DataIntegrityViolationException) {
            log.warn("Concurrent duplicate detected for event: {}", message.eventId)
            eventsDuplicate.increment()
            writeAudit(message, ProcessingStatus.DUPLICATE, ex.message, null)
        }

        acknowledgment.acknowledge()
    }

    private fun writeAudit(
        message: SearchEventMessage,
        status: ProcessingStatus,
        errorMessage: String?,
        rawPayloadRef: String?,
    ) {
        if (status == ProcessingStatus.REJECTED) eventsRejected.increment()
        auditLogRepository.save(
            EventAuditLog(
                id = UUID.randomUUID(),
                eventId = message.eventId,
                topic = "unknown",
                partition = -1,
                offset = -1L,
                status = status,
                errorMessage = errorMessage?.take(2000),
                rawPayloadRef = rawPayloadRef?.take(500),
                attemptNumber = 1,
                processedAt = OffsetDateTime.now(),
            )
        )
    }

    private fun toSearchRecord(message: SearchEventMessage): SearchRecord = SearchRecord(
        id = UUID.randomUUID(),
        eventId = EventId.of(message.eventId),
        eventVersion = message.eventVersion,
        eventTimestamp = OffsetDateTime.parse(message.eventTimestamp),
        userId = UserId(message.user.userId),
        userType = message.user.userType,
        userLocale = message.user.userLocale,
        sessionId = message.user.sessionId?.let { SessionId(it) },
        destination = Destination(
            destinationId = message.destination.destinationId,
            destinationName = message.destination.destinationName,
            destinationType = message.destination.destinationType,
            region = message.destination.region,
            country = message.destination.country,
        ),
        searchCriteria = message.searchCriteria,
        searchResult = SearchResult(
            resultCode = message.searchResult.resultCode,
            resultSummary = message.searchResult.resultSummary,
            resultCount = message.searchResult.resultCount,
            responseTimeMs = message.searchResult.responseTimeMs,
        ),
        extraFields = message.extraFields,
        topic = "unknown",
        partition = -1,
        offset = -1L,
    )
}
