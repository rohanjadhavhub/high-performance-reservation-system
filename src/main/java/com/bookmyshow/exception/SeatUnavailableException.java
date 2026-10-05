package com.bookmyshow.exception;

/**
 * Thrown when one or more requested seats are not available (already held or booked).
 */
public class SeatUnavailableException extends RuntimeException {

    public SeatUnavailableException(String message) {
        super(message);
    }
}
