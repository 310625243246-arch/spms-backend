package com.spms.dto;

public record BookingResponse(
        String bookingId,
        String tokenNumber,
        int queuePosition,
        int estimatedWaitMinutes,
        String status,
        String centerName,
        String crop,
        String date,
        String timeSlot
) {
}
