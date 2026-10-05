package com.bookmyshow.worker;

import com.bookmyshow.domain.model.PaymentIntent;
import com.bookmyshow.domain.model.Reservation;
import com.bookmyshow.domain.strategy.PaymentGatewayRegistry;
import com.bookmyshow.domain.strategy.PaymentGatewayStrategy;
import com.bookmyshow.dto.request.ConfirmBookingRequest;
import com.bookmyshow.repository.BookingRepository;
import com.bookmyshow.repository.PaymentIntentRepository;
import com.bookmyshow.repository.ReservationRepository;
import com.bookmyshow.service.BookingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Reconciliation Worker - heals the system during partial failures.
 *
 * Scenario 1 (PRD Section 6): Payment succeeded but booking service crashed.
 * This cron job finds PaymentIntents marked SUCCESS that lack a corresponding Booking,
 * and safely retries the booking confirmation using idempotency keys.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ReconciliationWorker {

    private final PaymentIntentRepository paymentIntentRepository;
    private final BookingRepository bookingRepository;
    private final ReservationRepository reservationRepository;
    private final BookingService bookingService;
    private final PaymentGatewayRegistry gatewayRegistry;

    @Value("${booking.reconciliation.max-age-minutes:30}")
    private int maxAgeMinutes;

    @Scheduled(cron = "${booking.reconciliation.cron:0 */5 * * * *}")
    public void reconcileOrphanedPayments() {
        LocalDateTime since = LocalDateTime.now().minusMinutes(maxAgeMinutes);
        List<PaymentIntent> orphaned = paymentIntentRepository
            .findOrphanedSuccessfulPayments(since);

        if (orphaned.isEmpty()) {
            return;
        }

        log.warn("Reconciliation: found {} orphaned successful payments", orphaned.size());

        for (PaymentIntent pi : orphaned) {
            try {
                // Check if reservation is still valid
                Reservation reservation = reservationRepository
                    .findByReservationId(pi.getReservationId())
                    .orElse(null);

                if (reservation == null) {
                    log.error("Reconciliation: reservation not found for payment {}",
                        pi.getPaymentIntentId());
                    continue;
                }

                if (reservation.isExpired()) {
                    // Reservation expired - issue refund
                    log.warn("Reconciliation: reservation expired for payment {}. Issuing refund.",
                        pi.getPaymentIntentId());
                    PaymentGatewayStrategy gateway = gatewayRegistry.getGateway(pi.getGatewayName());
                    gateway.refund(pi.getGatewayTransactionId(), pi.getAmount());
                    pi.setStatus(com.bookmyshow.domain.model.PaymentStatus.REFUNDED);
                    paymentIntentRepository.save(pi);
                    continue;
                }

                // Retry booking confirmation (idempotent - safe to retry)
                log.info("Reconciliation: retrying booking for payment {}",
                    pi.getPaymentIntentId());
                bookingService.confirmBooking(ConfirmBookingRequest.builder()
                    .paymentIntentId(pi.getPaymentIntentId())
                    .gatewayTransactionId(pi.getGatewayTransactionId())
                    .build());

            } catch (Exception e) {
                log.error("Reconciliation failed for payment {}: {}",
                    pi.getPaymentIntentId(), e.getMessage());
            }
        }
    }
}
