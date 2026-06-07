package com.travel_events_consumer.adapter.inbound.event

import io.micrometer.core.instrument.MeterRegistry
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.kafka.KafkaProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory
import org.springframework.kafka.core.ConsumerFactory
import org.springframework.kafka.core.DefaultKafkaConsumerFactory
import org.springframework.kafka.core.MicrometerConsumerListener
import org.springframework.kafka.listener.ContainerProperties
import org.springframework.kafka.listener.DefaultErrorHandler
import org.springframework.util.backoff.ExponentialBackOff

@Configuration
class KafkaConsumerConfig(
    private val kafkaProperties: KafkaProperties,
    private val meterRegistry: MeterRegistry,
) {

    @Value("\${kafka.listener.concurrency:3}")
    private var concurrency: Int = 3

    @Bean
    fun consumerFactory(): ConsumerFactory<String, String> {
        val props = kafkaProperties.buildConsumerProperties(null)
        props[ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG] = false
        val factory = DefaultKafkaConsumerFactory<String, String>(props)
        factory.addListener(MicrometerConsumerListener(meterRegistry))
        return factory
    }

    @Bean
    fun kafkaListenerContainerFactory(
        consumerFactory: ConsumerFactory<String, String>,
    ): ConcurrentKafkaListenerContainerFactory<String, String> {
        val factory = ConcurrentKafkaListenerContainerFactory<String, String>()
        factory.consumerFactory = consumerFactory
        factory.setConcurrency(concurrency)
        factory.containerProperties.ackMode = ContainerProperties.AckMode.MANUAL_IMMEDIATE
        factory.setCommonErrorHandler(buildErrorHandler())
        return factory
    }

    private fun buildErrorHandler(): DefaultErrorHandler {
        val backOff = ExponentialBackOff(1000L, 2.0).apply {
            maxElapsedTime = 30_000L
        }
        val handler = DefaultErrorHandler(backOff)
        handler.addNotRetryableExceptions(DataIntegrityViolationException::class.java)
        return handler
    }
}
