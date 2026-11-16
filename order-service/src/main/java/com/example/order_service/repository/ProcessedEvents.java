package com.example.order_service.repository;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class ProcessedEvents {

    private final JdbcTemplate jdbc;

    /** @return true if this is the first time we see the event, false for a duplicate */
    public boolean markProcessed(UUID eventId) {
        return jdbc.update("INSERT INTO processed_events (event_id) VALUES (?) ON CONFLICT DO NOTHING", eventId) == 1;
    }
}