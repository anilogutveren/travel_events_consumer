CREATE TABLE event_audit_log (
    id              UUID            PRIMARY KEY DEFAULT gen_random_uuid(),
    event_id        VARCHAR(255)    NOT NULL,
    topic           VARCHAR(255)    NOT NULL,
    partition_num   INTEGER         NOT NULL,
    kafka_offset    BIGINT          NOT NULL,
    status          VARCHAR(30)     NOT NULL,
    error_message   VARCHAR(2000),
    raw_payload_ref VARCHAR(500),
    attempt_number  INTEGER         NOT NULL DEFAULT 1,
    processed_at    TIMESTAMPTZ     NOT NULL DEFAULT now()
);

CREATE INDEX idx_audit_event_id  ON event_audit_log (event_id);
CREATE INDEX idx_audit_status    ON event_audit_log (status);
CREATE INDEX idx_audit_processed ON event_audit_log (processed_at DESC);
CREATE INDEX idx_audit_topic_pos ON event_audit_log (topic, partition_num, kafka_offset);
