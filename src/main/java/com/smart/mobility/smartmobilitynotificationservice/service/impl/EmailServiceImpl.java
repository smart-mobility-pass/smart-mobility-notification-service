package com.smart.mobility.smartmobilitynotificationservice.service.impl;

import com.smart.mobility.smartmobilitynotificationservice.model.Notification;
import com.smart.mobility.smartmobilitynotificationservice.service.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailServiceImpl implements EmailService {

    private final JavaMailSender javaMailSender;

    @Value("${mail.from:no-reply@smartmobility.com}")
    private String fromEmail;

    @Override
    public void sendEmail(String toEmail, String subject, Notification notification) {
        log.info("Preparing to send email to {} with subject {}", toEmail, subject);
        try {
            SimpleMailMessage mailMessage = new SimpleMailMessage();
            mailMessage.setFrom(fromEmail);
            mailMessage.setTo(toEmail);
            mailMessage.setSubject(subject);
            mailMessage.setText(notification.getMessage());

            javaMailSender.send(mailMessage);
            log.info("Email sent successfully to {}", toEmail);
        } catch (Exception e) {
            log.error("Failed to send email to {}", toEmail, e);
            throw new RuntimeException("Email sending failed", e);
        }
    }
}
