package com.bookmyshow.service;

import com.bookmyshow.domain.model.*;
import com.bookmyshow.domain.strategy.PaymentGatewayRegistry;
import com.bookmyshow.domain.strategy.PaymentGatewayStrategy;
import com.bookmyshow.dto.request.InitiatePaymentRequest;
import com.bookmyshow.dto.response.PaymentIntentResponse;
import com.bookmyshow.exception.PaymentException;
import com.bookmyshow.exception.ReservationExpiredException;
import com.bookmyshow.exception.ResourceNotFoundException;
import com.bookmyshow.repository.PaymentIntentRepository;
import com.bookmyshow.repository.SeatRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * Checkout Service - handles payment initiation.
 * Creates a PaymentIntent, calls the external gateway via Strategy pattern.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CheckoutService {

    private final PaymentIntentRepository paymentIntentRepository;
    private final ReservationService reservationService;
    private final SeatRepository seatRepository;
    private final PaymentGatewayRegistry gatewayRegistry;

    /**
     * Initiate a payment for a reservation with idempotency.
     */
    @Transactional
    public PaymentIntentResponse initiatePayment(String idempotencyKey, InitiatePaymentRequest request) {
        // Idempotency check
        Optional<PaymentIntent> existing = paymentIntentRepository.findByIdempotencyKey(idempotencyKey);
        if (existing.isPresent()) {
            log.info("Idempotent hit for payment key={}", idempotencyKey);
            return toResponse(existing.get());
        }

        // Verify reservation is still active
        if (!reservationService.isReservationActive(request.getReservationId())) {
            throw new ReservationExpiredException(request.getReservationId());
        }

        Reservation reservation = reservationService.getReservation(request.getReservationId());

        // Calculate total amount from seat prices
        List<Seat> seats = seatRepository.findByIdIn(reservation.getSeatIds());
        double totalAmount = seats.stream()
            .mapToDouble(Seat::getPrice)
            .sum();

        if (totalAmount <= 0) {
            throw new PaymentException("Invalid total amount: " + totalAmount);
        }

        // Select payment gateway
        String gatewayName = request.getGatewayName() != null
            ? request.getGatewayName()
            : "RAZORPAY";
        PaymentGatewayStrategy gateway = gatewayRegistry.getGateway(gatewayName);

        // Call external gateway
        String gatewayTxnId = gateway.initiateCharge(
            totalAmount,
            "INR",
            reservation.getReservationId()
        );

        // Persist payment intent
        PaymentIntent paymentIntent = PaymentIntent.builder()
            .idempotencyKey(idempotencyKey)
            .reservationId(reservation.getReservationId())
            .userId(request.getUserId())
            .amount(totalAmount)
            .currency("INR")
            .status(PaymentStatus.PROCESSING)
            .gatewayName(gatewayName)
            .gatewayTransactionId(gatewayTxnId)
            .build();

        paymentIntent = paymentIntentRepository.save(paymentIntent);
        log.info("Payment initiated: intentId={}, reservationId={}, amount={}, gateway={}",
            paymentIntent.getPaymentIntentId(), reservation.getReservationId(),
            totalAmount, gatewayName);

        return toResponse(paymentIntent);
    }

    /**
     * Get a payment intent by ID.
     */
    public PaymentIntent getPaymentIntent(String paymentIntentId) {
        return paymentIntentRepository.findByPaymentIntentId(paymentIntentId)
            .orElseThrow(() -> new ResourceNotFoundException("PaymentIntent", paymentIntentId));
    }

    private PaymentIntentResponse toResponse(PaymentIntent pi) {
        return PaymentIntentResponse.builder()
            .paymentIntentId(pi.getPaymentIntentId())
            .reservationId(pi.getReservationId())
            .amount(pi.getAmount())
            .currency(pi.getCurrency())
            .status(pi.getStatus().name())
            .gatewayName(pi.getGatewayName())
            .gatewayTransactionId(pi.getGatewayTransactionId())
            .createdAt(pi.getCreatedAt())
            .build();
    }
}
