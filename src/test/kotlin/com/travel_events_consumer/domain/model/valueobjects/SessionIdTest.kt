package com.travel_events_consumer.domain.model.valueobjects

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class SessionIdTest {

    @Test
    fun `creates SessionId from string`() {
        val sessionId = SessionId("sess-abc")
        assertEquals("sess-abc", sessionId.value)
    }

    @Test
    fun `two SessionIds with same value are equal`() {
        assertEquals(SessionId("sess-1"), SessionId("sess-1"))
    }
}
