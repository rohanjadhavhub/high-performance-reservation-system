package com.bookmyshow.worker;

import com.bookmyshow.domain.model.Reservation;
import com.bookmyshow.domain.model.ReservationStatus;
import com.bookmyshow.repository.ReservationRepository;
import com.bookmyshow.service.ReservationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Reservation Cleanup Worker - releases expired reservations.
 *
 * Runs periodically to catch any reservations whose Redis TTL expired
 * but the DB status hasn't been updated yet.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ReservationCleanupWorker {

    private final ReservationRepository reservationRepository;
    private final ReservationService reservationService;

    @Scheduled(fixedDelayString = "${booking.reservation.cleanup-interval-ms:30000}")
    public void cleanupExpiredReservations() {
        List<Reservation> expired = reservationRepository
            .findByStatusAndExpiresAtBefore(ReservationStatus.ACTIVE, LocalDateTime.now());

        if (expired.isEmpty()) {
            return;
        }

        log.info("Cleaning up {} expired reservations", expired.size());

        for (Reservation reservation : expired) {
            try {
                reservationService.releaseReservation(reservation.getReservationId());
                log.info("Released expired reservation: {}", reservation.getReservationId());
            } catch (Exception e) {
                log.error("Failed to release reservation: {}",
                    reservation.getReservationId(), e);
            }
        }
    }
}
