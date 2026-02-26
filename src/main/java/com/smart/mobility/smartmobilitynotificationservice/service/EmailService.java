package com.smart.mobility.smartmobilitynotificationservice.service;

import com.smart.mobility.smartmobilitynotificationservice.model.Notification;

public interface EmailService {
    void sendEmail(String toEmail, String subject, Notification notification);
}
