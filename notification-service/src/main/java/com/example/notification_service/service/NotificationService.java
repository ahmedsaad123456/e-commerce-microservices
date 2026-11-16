package com.example.notification_service.service;

import com.example.notification_service.customer.CustomerClient;
import com.example.notification_service.email.EmailSender;
import com.example.notification_service.email.EmailTemplates;
import com.example.notification_service.entity.Notification;
import com.example.notification_service.entity.NotificationStatus;
import com.example.notification_service.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notifications;
    private final CustomerClient customers;
    private final EmailSender mail;

    /** Saves the notification once per event; a redelivered event gets the existing one back. */
    public Notification saveNotification(Notification draft) {
        return notifications.findByEventId(draft.getEventId()).orElseGet(() -> {
            try {
                return notifications.insert(draft);
            } catch (DuplicateKeyException race) {      // two threads inserted at the same moment: the index rejected one
                return notifications.findByEventId(draft.getEventId()).orElseThrow();
            }
        });
    }

    /** Safe to call any number of times for the same notification. */
    public void deliver(String notificationId) {
        var n = notifications.findById(notificationId)
                .orElseThrow(() -> new IllegalArgumentException("Unknown notification " + notificationId));
        if (n.getStatus() == NotificationStatus.SENT) {
            log.info("Notification {} was already sent, skipping", notificationId);
            return;
        }
        var customer = customers.findCustomerById(n.getCustomerId());
        mail.send(customer.email(), EmailTemplates.subject(n), EmailTemplates.body(n, customer));
        n.markSent();                                     // an email cannot be "rolled back", so we record the success AFTER sending
        notifications.save(n);
    }

    /** Called by the worker when a job is given up: the Mongo history shows WHY an email never went out. */
    public void markFailed(String eventId, String reason) {
        notifications.findByEventId(eventId).ifPresent(n -> {
            if (n.getStatus() != NotificationStatus.SENT) {
                n.markFailed(reason);
                notifications.save(n);
            }
        });
    }
}
