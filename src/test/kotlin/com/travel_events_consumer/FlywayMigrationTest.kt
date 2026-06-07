package com.travel_events_consumer

import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.test.context.ActiveProfiles
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@SpringBootTest
@ActiveProfiles("test")
class FlywayMigrationTest {

    @Autowired
    lateinit var jdbcTemplate: JdbcTemplate

    @Test
    fun `all Flyway migrations apply cleanly`() {
        val rows = jdbcTemplate.queryForList(
            "SELECT version, description, success FROM flyway_schema_history ORDER BY installed_rank"
        )
        assertEquals(2, rows.size, "Expected exactly 2 migrations")
        rows.forEach { row ->
            assertTrue(row["success"] as Boolean, "Migration ${row["version"]} did not succeed")
        }
    }

    @Test
    fun `search_records table exists with unique constraint on event_id`() {
        val count = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM information_schema.tables WHERE table_name = 'search_records'",
            Int::class.java
        )
        assertEquals(1, count)
    }

    @Test
    fun `event_audit_log table exists`() {
        val count = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM information_schema.tables WHERE table_name = 'event_audit_log'",
            Int::class.java
        )
        assertEquals(1, count)
    }
}
