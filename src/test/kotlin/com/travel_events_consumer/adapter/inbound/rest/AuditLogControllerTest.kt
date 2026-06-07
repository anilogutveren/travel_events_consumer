package com.travel_events_consumer.adapter.inbound.rest

import com.ninjasquad.springmockk.MockkBean
import com.travel_events_consumer.application.port.inbound.QueryAuditLogUseCase
import com.travel_events_consumer.domain.model.AuditLogNotFoundException
import com.travel_events_consumer.domain.model.EventAuditLog
import com.travel_events_consumer.domain.model.ProcessingStatus
import io.mockk.every
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.data.domain.PageImpl
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import java.time.OffsetDateTime
import java.util.UUID

@WebMvcTest(AuditLogController::class)
class AuditLogControllerTest {

    @Autowired
    lateinit var mockMvc: MockMvc

    @MockkBean
    lateinit var queryAuditLogUseCase: QueryAuditLogUseCase

    @Test
    fun `GET audit-log returns 200 with paginated results`() {
        every { queryAuditLogUseCase.queryAuditLog(any(), any()) } returns PageImpl(listOf(sampleEntry()))

        mockMvc.get("/api/v1/audit-log")
            .andExpect {
                status { isOk() }
                content { contentType(MediaType.APPLICATION_JSON) }
                jsonPath("$.content") { isArray() }
            }
    }

    @Test
    fun `GET audit-log by id returns 200 with entry`() {
        val id = UUID.randomUUID()
        every { queryAuditLogUseCase.getById(id) } returns sampleEntry(id)

        mockMvc.get("/api/v1/audit-log/$id")
            .andExpect {
                status { isOk() }
                jsonPath("$.id") { value(id.toString()) }
            }
    }

    @Test
    fun `GET audit-log by unknown id returns 404 ProblemDetail`() {
        val id = UUID.randomUUID()
        every { queryAuditLogUseCase.getById(id) } throws AuditLogNotFoundException(id)

        mockMvc.get("/api/v1/audit-log/$id")
            .andExpect {
                status { isNotFound() }
                content { contentType("application/problem+json") }
                jsonPath("$.status") { value(404) }
            }
    }

    @Test
    fun `GET audit-log with size above 100 returns 400`() {
        mockMvc.get("/api/v1/audit-log?size=200")
            .andExpect { status { isBadRequest() } }
    }

    @Test
    fun `response uses snake_case field names`() {
        val id = UUID.randomUUID()
        every { queryAuditLogUseCase.getById(id) } returns sampleEntry(id)

        mockMvc.get("/api/v1/audit-log/$id")
            .andExpect {
                jsonPath("$.event_id") { exists() }
                jsonPath("$.processed_at") { exists() }
                jsonPath("$.kafka_offset") { exists() }
            }
    }

    private fun sampleEntry(id: UUID = UUID.randomUUID()) = EventAuditLog(
        id = id,
        eventId = "some-event-id",
        topic = "search-events",
        partition = 0,
        offset = 1L,
        status = ProcessingStatus.PROCESSED,
        errorMessage = null,
        rawPayloadRef = null,
        attemptNumber = 1,
        processedAt = OffsetDateTime.now(),
    )
}
