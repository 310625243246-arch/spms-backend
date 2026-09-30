package com.spms.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * NOTE ON FIELD NAME: kept as "email" to match section 13 of the spec's exact
 * request shape. In practice its value is resolved as a generic identifier:
 * the existing frontend login screens ask for "Farmer ID / Mobile Number",
 * "Officer ID" and "Admin ID / Email" (not strictly an email address), so
 * AuthService.login() accepts an email OR a farmerId OR an officerId OR a
 * phone number in this same field. This was the smallest change that avoids
 * touching the already-working login pages.
 */
public record LoginRequest(
        @NotBlank(message = "Email/ID is required") String email,
        @NotBlank(message = "Password is required") String password
) {
}
