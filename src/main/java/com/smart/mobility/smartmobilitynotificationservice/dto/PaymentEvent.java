package com.smart.mobility.smartmobilitynotificationservice.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record PaymentEvent(
                String tripId,
                String userId,
                BigDecimal amount,
                String status,
                String reason,
                LocalDateTime timestamp) {
}
