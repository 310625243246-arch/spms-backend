package com.spms.dto;

public record OfficerStatsDto(
        long totalBookingsToday,
        long waitingFarmers,
        long completedProcurements,
        String currentToken,
        double quantityProcuredKg,
        double amountPaidToday
) {
}
