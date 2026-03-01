package com.smart.mobility.smartmobilitynotificationservice.service;

import com.smart.mobility.smartmobilitynotificationservice.client.UserServiceClient;
import com.smart.mobility.smartmobilitynotificationservice.dto.AccountCreditedEvent;
import com.smart.mobility.smartmobilitynotificationservice.dto.PaymentEvent;
import com.smart.mobility.smartmobilitynotificationservice.dto.UserResponse;
import com.smart.mobility.smartmobilitynotificationservice.model.Notification;
import com.smart.mobility.smartmobilitynotificationservice.model.NotificationChannel;
import com.smart.mobility.smartmobilitynotificationservice.model.NotificationStatus;
import com.smart.mobility.smartmobilitynotificationservice.model.NotificationType;
import com.smart.mobility.smartmobilitynotificationservice.model.ReferenceType;
import com.smart.mobility.smartmobilitynotificationservice.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserServiceClient userServiceClient;
    private final EmailService emailService;

    @Transactional
    public void processPaymentEvent(PaymentEvent event) {
        String idempotencyKey = "PAYMENT_" + event.tripId() + "_" + event.status();

        if (inProccess(idempotencyKey))
            return;

        String message = ("SUCCESS".equalsIgnoreCase(event.status()) || "COMPLETED".equalsIgnoreCase(event.status()))
                ? String.format("Paiement r\u00E9ussi : %s F CFA pour votre trajet.", event.amount())
                : String.format("Paiement refus\u00E9 : %s.",
                        event.reason() != null ? event.reason() : "Erreur inconnue");

        Notification notification = Notification.builder()
                .userId(event.userId())
                .referenceId(event.tripId())
                .referenceType(ReferenceType.TRIP)
                .type(NotificationType.PAYMENT)
                .channel(NotificationChannel.EMAIL)
                .message(message)
                .status(NotificationStatus.PENDING)
                .idempotencyKey(idempotencyKey)
                .build();

        processAndSendNotification(notification, "Mise \u00E0 jour de paiement - Smart Mobility");
    }

    @Transactional
    public void processAccountCreditedEvent(AccountCreditedEvent event) {
        String idempotencyKey = "TOP_UP_" + event.userId() + "_" + event.timestamp().toString();

        if (inProccess(idempotencyKey))
            return;

        String message = String.format("Votre compte a \u00E9t\u00E9 cr\u00E9dit\u00E9 de %s F CFA.", event.amount());

        Notification notification = Notification.builder()
                .userId(event.userId())
                .referenceType(ReferenceType.TOP_UP)
                .type(NotificationType.ACCOUNT_CREDITED)
                .channel(NotificationChannel.EMAIL)
                .message(message)
                .status(NotificationStatus.PENDING)
                .idempotencyKey(idempotencyKey)
                .build();

        processAndSendNotification(notification, "Votre compte a \u00E9t\u00E9 recharg\u00E9 - Smart Mobility");
    }

    private boolean inProccess(String idempotencyKey) {
        if (notificationRepository.findByIdempotencyKey(idempotencyKey).isPresent()) {
            log.info("Notification for idempotencyKey {} already processed. Ignoring.",
                    idempotencyKey);
            return true;
        }
        return false;
    }

    private void processAndSendNotification(Notification notification, String subject) {
        // Save as PENDING
        Notification savedNotification = notificationRepository.save(notification);

        try {
            // Fetch User Details to get Email
            UserResponse user = userServiceClient.getUserById(notification.getUserId());
            if (user == null || user.email() == null) {
                throw new RuntimeException("User email not found for ID: " + notification.getUserId());
            }

            // Sync or Async Send - For V1 we can do Sync to keep it simple and update
            // status right after,
            // or use @Async on EmailService. Let's do Sync for now and rely on RabbitMQ DLQ
            // if it fails completely.
            emailService.sendEmail(user.email(), subject, savedNotification);

            // Update to SENT
            savedNotification.setStatus(NotificationStatus.SENT);
            notificationRepository.save(savedNotification);

            log.info("Notification successfully processed and MARKED as SENT for userId={}", notification.getUserId());

        } catch (Exception e) {
            log.error("Failed to process notification for userId={}", notification.getUserId(), e);
            savedNotification.setStatus(NotificationStatus.FAILED);
            savedNotification.setErrorReason(
                    e.getMessage() != null ? e.getMessage().substring(0, Math.min(e.getMessage().length(), 255))
                            : "Unknown Error");
            notificationRepository.save(savedNotification);
            // We throw the exception to NACK the message in RabbitMQ so it can retry or go
            // to DLQ
            throw new RuntimeException("Notification processing failed", e);
        }
    }
}
