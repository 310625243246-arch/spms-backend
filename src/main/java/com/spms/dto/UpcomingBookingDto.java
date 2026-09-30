package com.spms.dto;

public record UpcomingBookingDto(
        String bookingId,
        String centerName,
        String crop,
        String date,
        String timeSlot,
        String tokenNumber
) {
}
