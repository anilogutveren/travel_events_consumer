package com.travel_events_consumer.integration

import io.restassured.RestAssured
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.hamcrest.CoreMatchers.equalTo

class HealthEndpointIntegrationTest : AbstractIntegrationTest() {

    @BeforeEach
    fun setup() {
        RestAssured.port = port
    }

    @Test
    fun `liveness endpoint returns 200 with UP status`() {
        RestAssured.given()
            .get("/actuator/health")
            .then()
            .statusCode(200)
            .body("status", equalTo("UP"))
    }

    @Test
    fun `readiness endpoint returns 200 when both DB and Kafka are available`() {
        RestAssured.given()
            .get("/actuator/health/readiness")
            .then()
            .statusCode(200)
            .body("status", equalTo("UP"))
    }
}
