package com.example.order_service.repository;

import com.example.order_service.outbox.OutboxEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface OutboxRepository extends JpaRepository<OutboxEvent, UUID> {

    // FOR UPDATE SKIP LOCKED: this relay locks the rows it takes, and a second instance of the service
    // skips the locked rows instead of waiting. So two instances never send the same row at the same time.
    @Query(value = """
            SELECT * FROM outbox_events
            WHERE status = 'PENDING'
            ORDER BY created_at
            LIMIT :limit
            FOR UPDATE SKIP LOCKED
            """, nativeQuery = true)
    List<OutboxEvent> lockPending(@Param("limit") int limit);
}