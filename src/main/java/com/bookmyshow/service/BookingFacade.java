package com.bookmyshow.service;

import com.bookmyshow.dto.request.ConfirmBookingRequest;
import com.bookmyshow.dto.request.CreateReservationRequest;
import com.bookmyshow.dto.request.InitiatePaymentRequest;
import com.bookmyshow.dto.response.BookingResponse;
import com.bookmyshow.dto.response.PaymentIntentResponse;
import com.bookmyshow.dto.response.ReservationResponse;
import com.bookmyshow.dto.response.SeatAvailability;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Booking Facade (Facade Pattern)
 *
 * Orchestrates the entire booking flow across multiple services.
 * Controllers only talk to the Facade, shielding the API layer
 * from the complex micro-interactions of underlying services.
 *
 * Flow:
 *   1. View seat map         -> InventoryService
 *   2. Create reservation    -> ReservationService (Redis Lua + DB)
 *   3. Initiate payment      -> CheckoutService (Strategy Pattern)
 *   4. Confirm booking       -> BookingService (ACID + Outbox)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BookingFacade {

    private final InventoryService inventoryService;
    private final ReservationService reservationService;
    private final CheckoutService checkoutService;
    private final BookingService bookingService;

    /**
     * Step 1: Get the seat map for a show.
     */
    public List<SeatAvailability> getSeatMap(Long showId) {
        return inventoryService.getSeatMap(showId);
    }

    /**
     * Step 2: Hold seats for a user (create reservation).
     */
    public ReservationResponse holdSeats(String idempotencyKey, CreateReservationRequest request) {
        log.info("Facade: Holding seats. showId={}, seats={}, idempotencyKey={}",
            request.getShowId(), request.getSeatIds(), idempotencyKey);
        return reservationService.createReservation(idempotencyKey, request);
    }

    /**
     * Step 3: Initiate payment for a reservation.
     */
    public PaymentIntentResponse initiatePayment(String idempotencyKey, InitiatePaymentRequest request) {
        log.info("Facade: Initiating payment. reservationId={}, idempotencyKey={}",
            request.getReservationId(), idempotencyKey);
        return checkoutService.initiatePayment(idempotencyKey, request);
    }

    /**
     * Step 4: Confirm booking after payment succeeds.
     */
    public BookingResponse confirmBooking(ConfirmBookingRequest request) {
        log.info("Facade: Confirming booking. paymentIntentId={}",
            request.getPaymentIntentId());
        return bookingService.confirmBooking(request);
    }

    /**
     * Release a reservation (cancel hold).
     */
    public void cancelReservation(String reservationId) {
        log.info("Facade: Cancelling reservation. reservationId={}", reservationId);
        reservationService.releaseReservation(reservationId);
    }

    /**
     * Get booking details.
     */
    public BookingResponse getBooking(String bookingId) {
        return bookingService.getBooking(bookingId);
    }

    /**
     * Get all bookings for a user.
     */
    public List<BookingResponse> getUserBookings(Long userId) {
        return bookingService.getUserBookings(userId);
    }
}
