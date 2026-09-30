package com.spms.dto;

public record ProcurementCenterDto(
        Long id,
        String name,
        String location,
        String district,
        String address,
        boolean active
) {
}
