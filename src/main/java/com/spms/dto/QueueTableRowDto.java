package com.spms.dto;

public record QueueTableRowDto(
        String tokenNumber,
        String farmerName,
        String crop,
        String slot,
        String status
) {
}
