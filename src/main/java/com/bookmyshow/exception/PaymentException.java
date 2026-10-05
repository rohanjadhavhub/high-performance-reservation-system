package com.bookmyshow.exception;

/**
 * Thrown when a payment processing error occurs.
 */
public class PaymentException extends RuntimeException {

    public PaymentException(String message) {
        super(message);
    }

    public PaymentException(String message, Throwable cause) {
        super(message, cause);
    }
}
