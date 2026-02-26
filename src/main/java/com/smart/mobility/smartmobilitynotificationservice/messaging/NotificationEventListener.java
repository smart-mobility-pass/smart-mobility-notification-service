package com.smart.mobility.smartmobilitynotificationservice.messaging;

import com.smart.mobility.smartmobilitynotificationservice.config.RabbitMQConfig;
import com.smart.mobility.smartmobilitynotificationservice.dto.AccountCreditedEvent;
import com.smart.mobility.smartmobilitynotificationservice.dto.PaymentEvent;
import com.smart.mobility.smartmobilitynotificationservice.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class NotificationEventListener {

    private final NotificationService notificationService;

    @RabbitListener(queues = RabbitMQConfig.NOTIFICATION_PAYMENT_QUEUE)
    public void handlePaymentEvent(PaymentEvent event) {
        log.info("Received PaymentEvent for tripId: {}, status: {}", event.tripId(), event.status());
        notificationService.processPaymentEvent(event);
    }

    @RabbitListener(queues = RabbitMQConfig.NOTIFICATION_ACCOUNT_QUEUE)
    public void handleAccountCreditedEvent(AccountCreditedEvent event) {
        log.info("Received AccountCreditedEvent for userId: {}, amount: {}", event.userId(), event.amount());
        notificationService.processAccountCreditedEvent(event);
    }
}
