package com.travel_events_consumer.domain.service

import com.travel_events_consumer.application.port.outbound.SearchRecordRepository
import com.travel_events_consumer.domain.model.valueobjects.EventId
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Test
import java.util.UUID
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DuplicateDetectionPolicyTest {

    private val repository = mockk<SearchRecordRepository>()
    private val policy = DuplicateDetectionPolicy(repository)

    @Test
    fun `returns true when eventId already exists`() {
        val eventId = EventId(UUID.randomUUID())
        every { repository.existsByEventId(eventId) } returns true
        assertTrue(policy.isDuplicate(eventId))
    }

    @Test
    fun `returns false when eventId does not exist`() {
        val eventId = EventId(UUID.randomUUID())
        every { repository.existsByEventId(eventId) } returns false
        assertFalse(policy.isDuplicate(eventId))
    }
}
