CREATE TABLE outbox_events (
    id             UUID PRIMARY KEY,              -- also the eventId inside the payload
    aggregate_type VARCHAR(50)  NOT NULL,
    aggregate_id   VARCHAR(64)  NOT NULL,
    topic          VARCHAR(100) NOT NULL,
    payload        TEXT         NOT NULL,         -- the JSON, exactly as it will be sent
    status         VARCHAR(20)  NOT NULL DEFAULT 'PENDING',
    attempts       INT          NOT NULL DEFAULT 0,
    created_at     TIMESTAMPTZ  NOT NULL DEFAULT now(),
    published_at   TIMESTAMPTZ
);

-- the relay only looks at PENDING rows, so index only those (partial index stays tiny)
CREATE INDEX idx_outbox_pending ON outbox_events (created_at) WHERE status = 'PENDING';