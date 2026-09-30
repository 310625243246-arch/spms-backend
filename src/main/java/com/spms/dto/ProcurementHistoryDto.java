package com.spms.dto;

public record ProcurementHistoryDto(
        String id,
        String date,
        String crop,
        String centerName,
        double quantityKg,
        double amountPaid,
        String status
) {
}
