package com.smart.mobility.smartmobilitynotificationservice.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record AccountCreditedEvent(
                String userId,
                BigDecimal amount,
                LocalDateTime timestamp) {
}
