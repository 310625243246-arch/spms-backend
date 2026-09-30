package com.spms.dto;

public record OfficerSummaryDto(
        Long id,
        String officerId,
        String name,
        String email,
        String phone,
        String assignedCenter
) {
}
