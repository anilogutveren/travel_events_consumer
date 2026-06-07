package com.travel_events_consumer.adapter.inbound.event

import com.fasterxml.jackson.databind.ObjectMapper
import com.networknt.schema.JsonSchemaFactory
import com.networknt.schema.SpecVersion
import com.travel_events_consumer.application.port.inbound.ProcessSearchEventUseCase
import com.travel_events_consumer.application.port.outbound.AuditLogRepository
import com.travel_events_consumer.domain.model.EventAuditLog
import com.travel_events_consumer.domain.model.ProcessingStatus
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.slf4j.LoggerFactory
import org.slf4j.MDC
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.kafka.support.Acknowledgment
import org.springframework.stereotype.Component
import java.time.OffsetDateTime
import java.util.UUID

@Component
class KafkaSearchEventListener(
    private val processSearchEventUseCase: ProcessSearchEventUseCase,
    private val auditLogRepository: AuditLogRepository,
) {

    private val log = LoggerFactory.getLogger(javaClass)
    private val objectMapper = ObjectMapper().apply { findAndRegisterModules() }
    private val schemaFactory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V7)
    private val schema = schemaFactory.getSchema(
        javaClass.classLoader.getResourceAsStream("contracts/search-event-v1.json")
            ?: error("Kafka event schema not found on classpath")
    )

    @KafkaListener(topics = ["#{'\${app.kafka.topics}'.split(',')}"])
    fun onMessage(record: ConsumerRecord<String, String>, acknowledgment: Acknowledgment) {
        val rawPayload = record.value()
        val eventIdForLog = "unknown"

        try {
            val jsonNode = objectMapper.readTree(rawPayload)
            val violations = schema.validate(jsonNode)

            if (violations.isNotEmpty()) {
                val reason = violations.joinToString("; ") { it.message }
                log.error("Event schema validation failed topic={} partition={} offset={} reason={}",
                    record.topic(), record.partition(), record.offset(), reason)
                writeRejectedAudit(record, reason, rawPayload)
                acknowledgment.acknowledge()
                return
            }

            val message = objectMapper.treeToValue(jsonNode, SearchEventMessage::class.java)

            MDC.put("correlation_id", message.eventId)
            MDC.put("event_id", message.eventId)
            MDC.put("topic", record.topic())
            MDC.put("partition", record.partition().toString())
            MDC.put("offset", record.offset().toString())

            processSearchEventUseCase.processEvent(message, acknowledgment)

        } catch (ex: Exception) {
            log.error("Failed to deserialize event from topic={} partition={} offset={}",
                record.topic(), record.partition(), record.offset(), ex)
            writeRejectedAudit(record, ex.message ?: "Deserialization error", rawPayload)
            acknowledgment.acknowledge()
        } finally {
            MDC.clear()
        }
    }

    private fun writeRejectedAudit(
        record: ConsumerRecord<String, String>,
        reason: String,
        rawPayload: String,
    ) {
        runCatching {
            auditLogRepository.save(
                EventAuditLog(
                    id = UUID.randomUUID(),
                    eventId = extractEventId(record.value()),
                    topic = record.topic(),
                    partition = record.partition(),
                    offset = record.offset(),
                    status = ProcessingStatus.REJECTED,
                    errorMessage = reason.take(2000),
                    rawPayloadRef = rawPayload.take(500),
                    attemptNumber = 1,
                    processedAt = OffsetDateTime.now(),
                )
            )
        }.onFailure { ex ->
            log.error("Failed to write REJECTED audit entry", ex)
        }
    }

    private fun extractEventId(payload: String): String =
        runCatching { objectMapper.readTree(payload)?.get("event_id")?.asText() }
            .getOrNull() ?: "unparseable"
}
