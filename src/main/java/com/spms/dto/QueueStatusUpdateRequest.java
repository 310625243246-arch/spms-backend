package com.spms.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * `status` accepts either the BookingStatus enum name (e.g. "GRADING") or its
 * display label (e.g. "Grading") - see BookingStatus.fromDisplay().
 */
public record QueueStatusUpdateRequest(
        @NotBlank(message = "Status is required") String status
) {
}
