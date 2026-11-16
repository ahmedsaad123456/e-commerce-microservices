-- existing rows are Kafka events, hence the default
ALTER TABLE outbox_events ADD COLUMN destination VARCHAR(20) NOT NULL DEFAULT 'KAFKA';
ALTER TABLE outbox_events ADD COLUMN routing_key VARCHAR(100);       -- RabbitMQ only