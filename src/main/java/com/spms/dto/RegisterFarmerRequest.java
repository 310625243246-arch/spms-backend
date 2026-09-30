package com.spms.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Mirrors the frontend's `RegisterPayload` interface in src/services/authService.ts
 * exactly (fullName, mobile, farmerId, village, district, password) so the
 * existing registration form needs no new fields. Since the form collects no
 * email, the backend derives a synthetic login email as "{farmerId}@spms.local".
 */
public record RegisterFarmerRequest(
        @NotBlank(message = "Full name is required") String fullName,

        @NotBlank(message = "Mobile number is required")
        @Pattern(regexp = "\\d{10}", message = "Mobile number must be 10 digits")
        String mobile,

        @NotBlank(message = "Farmer ID is required") String farmerId,

        @NotBlank(message = "Village is required") String village,

        @NotBlank(message = "District is required") String district,

        @NotBlank(message = "Password is required")
        @Size(min = 6, message = "Password must be at least 6 characters")
        String password
) {
}
