package com.spms.dto;

public record FarmerSummaryDto(
        Long id,
        String farmerId,
        String name,
        String email,
        String phone,
        String village,
        String district
) {
}
