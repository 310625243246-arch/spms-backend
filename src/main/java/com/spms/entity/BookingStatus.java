package com.spms.entity;

/**
 * Mirrors the frontend's `ProcurementStatus` union type in src/types/index.ts.
 * toDisplay() returns the exact string the React frontend expects, so the
 * DTO layer can send labels the UI already knows how to render (StatusBadge, etc.)
 * without any frontend changes.
 */
public enum BookingStatus {
    BOOKED("Booked"),
    CHECKED_IN("Checked In"),
    IN_QUEUE("In Queue"),
    GRADING("Grading"),
    WEIGHING("Weighing"),
    PAYMENT_PENDING("Payment Pending"),
    COMPLETED("Completed"),
    REJECTED("Rejected");

    private final String display;

    BookingStatus(String display) {
        this.display = display;
    }

    public String toDisplay() {
        return display;
    }

    public static BookingStatus fromDisplay(String display) {
        for (BookingStatus s : values()) {
            if (s.display.equalsIgnoreCase(display) || s.name().equalsIgnoreCase(display)) {
                return s;
            }
        }
        throw new IllegalArgumentException("Unknown booking status: " + display);
    }
}
