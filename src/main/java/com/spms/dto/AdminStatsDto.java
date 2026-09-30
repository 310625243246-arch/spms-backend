package com.spms.dto;

public record AdminStatsDto(
        long totalFarmers,
        long activeCenters,
        long bookingsToday,
        long completedProcurements,
        long pendingProcurements,
        double totalAmountPaid
) {
}
