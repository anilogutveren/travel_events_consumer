package com.travel_events_consumer.adapter.inbound.rest

import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Test
import org.springframework.boot.actuate.health.Status
import org.springframework.kafka.core.KafkaAdmin
import kotlin.test.assertEquals

class KafkaHealthIndicatorTest {

    private val kafkaAdmin = mockk<KafkaAdmin>()
    private val indicator = KafkaHealthIndicator(kafkaAdmin)

    @Test
    fun `returns UP when KafkaAdmin can describe cluster`() {
        every { kafkaAdmin.describeTopics(any<String>()) } returns emptyMap()
        val health = indicator.health()
        assertEquals(Status.UP, health.status)
    }

    @Test
    fun `returns DOWN when KafkaAdmin throws exception`() {
        every { kafkaAdmin.describeTopics(any<String>()) } throws RuntimeException("Connection refused")
        val health = indicator.health()
        assertEquals(Status.DOWN, health.status)
    }
}
