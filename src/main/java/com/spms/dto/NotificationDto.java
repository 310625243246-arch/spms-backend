package com.spms.dto;

public record NotificationDto(
        String id,
        String message,
        String timestamp,
        boolean read
) {
}
