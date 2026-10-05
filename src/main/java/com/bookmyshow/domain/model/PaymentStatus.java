package com.bookmyshow.domain.model;

/**
 * Payment lifecycle status.
 */
public enum PaymentStatus {
    PENDING,
    PROCESSING,
    SUCCESS,
    FAILED,
    REFUNDED
}
