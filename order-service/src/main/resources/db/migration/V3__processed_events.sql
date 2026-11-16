CREATE TABLE processed_events (
    event_id     UUID        PRIMARY KEY,           -- the unique key is what rejects the duplicate
    processed_at TIMESTAMPTZ NOT NULL DEFAULT now()
);