package com.spms.dto;

public record CenterActivityDto(
        String centerName,
        long bookingsToday,
        long completed,
        long pending
) {
}
