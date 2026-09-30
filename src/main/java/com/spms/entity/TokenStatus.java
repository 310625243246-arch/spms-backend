package com.spms.entity;

/**
 * Internal queue-call lifecycle for a Token, as requested in the backend spec (section 8).
 * This is distinct from BookingStatus: BookingStatus is the farmer-facing procurement
 * status shown on screen; TokenStatus tracks the officer's queue-calling actions
 * (call next / skip / cancel) that drive when BookingStatus changes.
 */
public enum TokenStatus {
    WAITING,
    CALLED,
    IN_PROGRESS,
    COMPLETED,
    SKIPPED,
    CANCELLED
}
