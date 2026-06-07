package com.travel_events_consumer.domain.model.valueobjects

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import kotlin.test.assertEquals

class UserIdTest {

    @Test
    fun `creates UserId from non-blank string`() {
        val userId = UserId("usr-123")
        assertEquals("usr-123", userId.value)
    }

    @Test
    fun `throws when value is blank`() {
        assertThrows<IllegalArgumentException> { UserId("") }
        assertThrows<IllegalArgumentException> { UserId("   ") }
    }

    @Test
    fun `two UserIds with same value are equal`() {
        assertEquals(UserId("usr-1"), UserId("usr-1"))
    }
}
