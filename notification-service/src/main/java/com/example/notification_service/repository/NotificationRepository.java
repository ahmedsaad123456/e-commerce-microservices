package com.example.notification_service.repository;

import com.example.notification_service.entity.Notification;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface NotificationRepository extends MongoRepository<Notification, String> {
    Optional<Notification> findByEventId(String eventId);
}
