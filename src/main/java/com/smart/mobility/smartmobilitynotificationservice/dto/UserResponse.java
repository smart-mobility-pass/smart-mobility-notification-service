package com.smart.mobility.smartmobilitynotificationservice.dto;

public record UserResponse(
        Long id,
        String keycloakId,
        String email,
        String firstName,
        String lastName,
        String phone,
        String role) {
}
