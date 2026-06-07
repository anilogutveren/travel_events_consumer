package com.travel_events_consumer.adapter.inbound.rest

import org.springframework.boot.actuate.health.Health
import org.springframework.boot.actuate.health.HealthIndicator
import org.springframework.kafka.core.KafkaAdmin
import org.springframework.stereotype.Component

@Component("kafka")
class KafkaHealthIndicator(private val kafkaAdmin: KafkaAdmin) : HealthIndicator {

    override fun health(): Health =
        runCatching { kafkaAdmin.describeTopics("__consumer_offsets") }
            .fold(
                onSuccess = { Health.up().build() },
                onFailure = { ex -> Health.down().withException(ex).build() },
            )
}
