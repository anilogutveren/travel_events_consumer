package com.travel_events_consumer.domain.model

import java.util.UUID

class AuditLogNotFoundException(id: UUID) : RuntimeException("Audit log entry not found: $id")
