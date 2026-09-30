package com.spms.dto;

import jakarta.validation.constraints.NotBlank;

public record ProcurementCenterRequest(
        @NotBlank(message = "Name is required") String name,
        @NotBlank(message = "Location is required") String location,
        @NotBlank(message = "District is required") String district,
        String address,
        boolean active
) {
}
