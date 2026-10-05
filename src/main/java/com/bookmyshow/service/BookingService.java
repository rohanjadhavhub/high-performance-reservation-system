package com.bookmyshow.service;

import com.bookmyshow.domain.model.*;
import com.bookmyshow.domain.strategy.PaymentGatewayRegistry;
import com.bookmyshow.domain.strategy.PaymentGatewayStrategy;
import com.bookmyshow.dto.request.ConfirmBookingRequest;
import com.bookmyshow.dto.response.BookingResponse;
import com.bookmyshow.exception.PaymentException;
import com.bookmyshow.exception.ReservationExpiredException;
import com.bookmyshow.exception.ResourceNotFoundException;
import com.bookmyshow.repository.BookingRepository;
import com.bookmyshow.repository.PaymentIntentRepository;
import com.bookmyshow.repository.ReservationRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Booking Service - handles final booking confirmation.
 * Validates payment, checks reservation TTL, transitions seats to BOOKED,
 * writes the durable booking record + outbox event in a single ACID transaction.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BookingService {

    private final BookingRepository bookingRepository;
    private final PaymentIntentRepository paymentIntentRepository;
    private final ReservationRepository reservationRepository;
    private final RedisTemplate<String, String> redisTemplate;
    private final RedisScript<String> seatBookScript;
    private final PaymentGatewayRegistry gatewayRegistry;
    private final OutboxService outboxService;
    private final ObjectMapper objectMapper;

    /**
     * Confirm a booking after payment succeeds.
     *
     * This is the critical path with exactly-once semantics:
     * 1. Validate payment with gateway
     * 2. Check reservation TTL is still active
     * 3. Atomically transition seats from HELD to BOOKED in Redis
     * 4. Write Booking + OutboxEvent in a single DB transaction
     *
     * If reservation expired but payment succeeded, triggers a refund (Scenario 4 from PRD).
     */
    @Transactional
    public BookingResponse confirmBooking(ConfirmBookingRequest request) {
        // 1. Look up the payment intent
        PaymentIntent paymentIntent = paymentIntentRepository
            .findByPaymentIntentId(request.getPaymentIntentId())
            .orElseThrow(() -> new ResourceNotFoundException("PaymentIntent", request.getPaymentIntentId()));

        // Idempotency: if booking already exists for this reservation, return it
        Optional<Booking> existingBooking = bookingRepository
            .findByReservationId(paymentIntent.getReservationId());
        if (existingBooking.isPresent()) {
            log.info("Idempotent hit for booking: reservationId={}",
                paymentIntent.getReservationId());
            return toResponse(existingBooking.get());
        }

        // 2. Verify payment with gateway
        PaymentGatewayStrategy gateway = gatewayRegistry.getGateway(paymentIntent.getGatewayName());
        boolean paymentVerified = gateway.verifyPayment(request.getGatewayTransactionId());
        if (!paymentVerified) {
            paymentIntent.setStatus(PaymentStatus.FAILED);
            paymentIntent.setFailureReason("Gateway verification failed");
            paymentIntentRepository.save(paymentIntent);
            throw new PaymentException("Payment verification failed with gateway: "
                + paymentIntent.getGatewayName());
        }

        // Mark payment as successful
        paymentIntent.setStatus(PaymentStatus.SUCCESS);
        paymentIntent.setGatewayTransactionId(request.getGatewayTransactionId());
        paymentIntentRepository.save(paymentIntent);

        // 3. Check reservation is still active (TTL not expired)
        Reservation reservation = reservationRepository
            .findByReservationId(paymentIntent.getReservationId())
            .orElseThrow(() -> new ResourceNotFoundException("Reservation",
                paymentIntent.getReservationId()));

        if (reservation.isExpired() || reservation.getStatus() != ReservationStatus.ACTIVE) {
            // Scenario 4: Reservation expired mid-payment - trigger refund
            log.warn("Reservation expired during payment. Initiating refund. reservationId={}, paymentIntentId={}",
                reservation.getReservationId(), paymentIntent.getPaymentIntentId());

            String refundId = gateway.refund(
                request.getGatewayTransactionId(),
                paymentIntent.getAmount()
            );
            paymentIntent.setStatus(PaymentStatus.REFUNDED);
            paymentIntentRepository.save(paymentIntent);

            throw new ReservationExpiredException(reservation.getReservationId());
        }

        // 4. Atomically transition seats from HELD to BOOKED in Redis
        List<String> seatKeys = reservation.getSeatIds().stream()
            .map(seatId -> InventoryService.buildSeatKey(reservation.getShowId(), seatId))
            .collect(Collectors.toList());

        String result = redisTemplate.execute(
            seatBookScript,
            seatKeys,
            reservation.getReservationId()
        );

        if (result == null || !result.equals("OK")) {
            log.error("Failed to transition seats to BOOKED in Redis: {}", result);
            // Seats expired in Redis but reservation was still active - refund
            String refundId = gateway.refund(
                request.getGatewayTransactionId(),
                paymentIntent.getAmount()
            );
            paymentIntent.setStatus(PaymentStatus.REFUNDED);
            paymentIntentRepository.save(paymentIntent);
            throw new ReservationExpiredException(reservation.getReservationId());
        }

        // 5. Create the durable booking record
        Booking booking = Booking.builder()
            .reservationId(reservation.getReservationId())
            .paymentIntentId(paymentIntent.getPaymentIntentId())
            .userId(reservation.getUserId())
            .showId(reservation.getShowId())
            .seatIds(reservation.getSeatIds())
            .totalAmount(paymentIntent.getAmount())
            .status(BookingStatus.CONFIRMED)
            .build();

        booking = bookingRepository.save(booking);

        // Update reservation status
        reservation.setStatus(ReservationStatus.CONFIRMED);
        reservationRepository.save(reservation);

        // 6. Write outbox event (same transaction)
        outboxService.createEvent(
            "Booking",
            booking.getBookingId(),
            "BOOKING_CONFIRMED",
            serializeBookingPayload(booking)
        );

        log.info("Booking confirmed: bookingId={}, showId={}, seats={}",
            booking.getBookingId(), booking.getShowId(), booking.getSeatIds());

        return toResponse(booking);
    }

    /**
     * Get a booking by ID.
     */
    public BookingResponse getBooking(String bookingId) {
        Booking booking = bookingRepository.findByBookingId(bookingId)
            .orElseThrow(() -> new ResourceNotFoundException("Booking", bookingId));
        return toResponse(booking);
    }

    /**
     * Get all bookings for a user.
     */
    public List<BookingResponse> getUserBookings(Long userId) {
        return bookingRepository.findByUserId(userId).stream()
            .map(this::toResponse)
            .collect(Collectors.toList());
    }

    private BookingResponse toResponse(Booking booking) {
        return BookingResponse.builder()
            .bookingId(booking.getBookingId())
            .reservationId(booking.getReservationId())
            .paymentIntentId(booking.getPaymentIntentId())
            .showId(booking.getShowId())
            .seatIds(booking.getSeatIds())
            .totalAmount(booking.getTotalAmount())
            .status(booking.getStatus().name())
            .createdAt(booking.getCreatedAt())
            .build();
    }

    private String serializeBookingPayload(Booking booking) {
        try {
            return objectMapper.writeValueAsString(toResponse(booking));
        } catch (Exception e) {
            log.error("Failed to serialize booking payload", e);
            return "{}";
        }
    }
}
