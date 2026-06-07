package com.travel_events_consumer

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class TravelEventsConsumerApplication

fun main(args: Array<String>) {
    runApplication<TravelEventsConsumerApplication>(*args)
}
