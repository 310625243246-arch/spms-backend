package com.spms.dto;

public record BookingSummaryDto(
        String bookingId,
        String farmerName,
        String centerName,
        String crop,
        String date,
        String status
) {
}
