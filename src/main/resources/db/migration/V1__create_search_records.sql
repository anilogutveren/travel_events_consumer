CREATE TABLE search_records (
    id                  UUID            PRIMARY KEY DEFAULT gen_random_uuid(),
    event_id            UUID            NOT NULL,
    event_version       VARCHAR(20)     NOT NULL,
    event_timestamp     TIMESTAMPTZ     NOT NULL,
    user_id             VARCHAR(255)    NOT NULL,
    user_type           VARCHAR(50),
    user_locale         VARCHAR(20),
    session_id          VARCHAR(255),
    destination_id      VARCHAR(255)    NOT NULL,
    destination_name    VARCHAR(500)    NOT NULL,
    destination_type    VARCHAR(50),
    region              VARCHAR(255),
    country             VARCHAR(100),
    search_criteria     JSONB           NOT NULL DEFAULT '{}',
    result_code         VARCHAR(50)     NOT NULL,
    result_summary      VARCHAR(1000),
    result_count        INTEGER,
    response_time_ms    BIGINT,
    extra_fields        JSONB           NOT NULL DEFAULT '{}',
    topic               VARCHAR(255)    NOT NULL,
    partition_num       INTEGER         NOT NULL,
    kafka_offset        BIGINT          NOT NULL,
    created_at          TIMESTAMPTZ     NOT NULL DEFAULT now(),
    CONSTRAINT uq_search_records_event_id UNIQUE (event_id)
);

CREATE INDEX idx_search_records_user_id     ON search_records (user_id);
CREATE INDEX idx_search_records_event_ts    ON search_records (event_timestamp DESC);
CREATE INDEX idx_search_records_destination ON search_records (destination_id);
CREATE INDEX idx_search_records_criteria    ON search_records USING gin (search_criteria);
