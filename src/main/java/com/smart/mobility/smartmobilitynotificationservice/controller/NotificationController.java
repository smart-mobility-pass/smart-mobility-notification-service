package com.smart.mobility.smartmobilitynotificationservice.controller;

import com.smart.mobility.smartmobilitynotificationservice.model.Notification;
import com.smart.mobility.smartmobilitynotificationservice.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/notifications")
@RequiredArgsConstructor
@Slf4j
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<Notification>> getNotifications(@PathVariable String userId) {
        log.info("REST request to get notifications for user: {}", userId);
        return ResponseEntity.ok(notificationService.getNotificationsByUserId(userId));
    }
}
