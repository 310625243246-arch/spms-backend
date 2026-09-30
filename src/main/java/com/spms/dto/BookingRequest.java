package com.spms.dto;

import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.time.LocalTime;

public record BookingRequest(
        @NotNull(message = "Procurement center is required") Long procurementCenterId,
        @NotNull(message = "Crop is required") Long cropId,
        @NotNull(message = "Date is required")
        @FutureOrPresent(message = "Booking date cannot be in the past")
        LocalDate date,
        @NotNull(message = "Start time is required") LocalTime startTime,
        Double quantityKg
) {
}
