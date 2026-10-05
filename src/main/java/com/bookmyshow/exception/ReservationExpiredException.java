package com.bookmyshow.exception;

/**
 * Thrown when a reservation has expired (TTL exceeded).
 */
public class ReservationExpiredException extends RuntimeException {

    private final String reservationId;

    public ReservationExpiredException(String reservationId) {
        super("Reservation has expired: " + reservationId);
        this.reservationId = reservationId;
    }

    public String getReservationId() {
        return reservationId;
    }
}
