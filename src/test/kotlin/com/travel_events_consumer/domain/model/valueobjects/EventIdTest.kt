package com.travel_events_consumer.domain.model.valueobjects

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

class EventIdTest {

    @Test
    fun `creates EventId from valid UUID string`() {
        val uuid = UUID.randomUUID().toString()
        val eventId = EventId.of(uuid)
        assertEquals(uuid, eventId.value.toString())
    }

    @Test
    fun `creates EventId from UUID`() {
        val uuid = UUID.randomUUID()
        val eventId = EventId(uuid)
        assertEquals(uuid, eventId.value)
    }

    @Test
    fun `throws when UUID string is invalid`() {
        assertThrows<IllegalArgumentException> { EventId.of("not-a-uuid") }
    }

    @Test
    fun `throws when UUID string is blank`() {
        assertThrows<IllegalArgumentException> { EventId.of("") }
    }

    @Test
    fun `two EventIds with same UUID are equal`() {
        val uuid = UUID.randomUUID()
        assertEquals(EventId(uuid), EventId(uuid))
    }

    @Test
    fun `two EventIds with different UUIDs are not equal`() {
        assertNotEquals(EventId(UUID.randomUUID()), EventId(UUID.randomUUID()))
    }
}
