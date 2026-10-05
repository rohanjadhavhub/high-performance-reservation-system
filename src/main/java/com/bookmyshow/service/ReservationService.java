package com.bookmyshow.service;

import com.bookmyshow.domain.model.Reservation;
import com.bookmyshow.domain.model.ReservationStatus;
import com.bookmyshow.dto.request.CreateReservationRequest;
import com.bookmyshow.dto.response.ReservationResponse;
import com.bookmyshow.exception.ResourceNotFoundException;
import com.bookmyshow.exception.SeatUnavailableException;
import com.bookmyshow.repository.ReservationRepository;
import com.bookmyshow.repository.ShowRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Reservation Service - handles seat holds with strong consistency.
 * Uses Redis Lua scripts for atomic check-and-set operations.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ReservationService {

    private final ReservationRepository reservationRepository;
    private final ShowRepository showRepository;
    private final RedisTemplate<String, String> redisTemplate;
    private final RedisScript<String> seatHoldScript;
    private final RedisScript<String> seatReleaseScript;

    @Value("${booking.reservation.ttl-seconds:300}")
    private int reservationTtlSeconds;

    /**
     * Create a reservation (hold seats) with idempotency.
     *
     * If the idempotency key already exists, return the existing reservation.
     * Otherwise, atomically hold all requested seats in Redis via Lua script.
     */
    @Transactional
    public ReservationResponse createReservation(String idempotencyKey, CreateReservationRequest request) {
        // Idempotency check: return existing reservation if key matches
        Optional<Reservation> existing = reservationRepository.findByIdempotencyKey(idempotencyKey);
        if (existing.isPresent()) {
            log.info("Idempotent hit for reservation key={}", idempotencyKey);
            return toResponse(existing.get());
        }

        // Validate show exists
        showRepository.findById(request.getShowId())
            .orElseThrow(() -> new ResourceNotFoundException("Show", request.getShowId()));

        // Build Redis keys for all requested seats
        List<String> seatKeys = request.getSeatIds().stream()
            .map(seatId -> InventoryService.buildSeatKey(request.getShowId(), seatId))
            .collect(Collectors.toList());

        // Build the reservation entity
        Reservation reservation = Reservation.builder()
            .idempotencyKey(idempotencyKey)
            .showId(request.getShowId())
            .userId(request.getUserId())
            .seatIds(request.getSeatIds())
            .expiresAt(LocalDateTime.now().plusSeconds(reservationTtlSeconds))
            .status(ReservationStatus.ACTIVE)
            .build();

        // Execute atomic Lua script: check all seats AVAILABLE, then hold them
        String result = redisTemplate.execute(
            seatHoldScript,
            seatKeys,
            reservation.getReservationId(),
            String.valueOf(reservationTtlSeconds)
        );

        if (result == null || !result.equals("OK")) {
            String conflictKey = result != null ? result.replace("CONFLICT:", "") : "unknown";
            throw new SeatUnavailableException(
                "One or more seats are unavailable. Conflict at: " + conflictKey);
        }

        // Persist reservation to DB
        reservation = reservationRepository.save(reservation);
        log.info("Reservation created: id={}, showId={}, seats={}, expiresAt={}",
            reservation.getReservationId(), request.getShowId(),
            request.getSeatIds(), reservation.getExpiresAt());

        return toResponse(reservation);
    }

    /**
     * Release a reservation (free held seats).
     */
    @Transactional
    public void releaseReservation(String reservationId) {
        Reservation reservation = reservationRepository.findByReservationId(reservationId)
            .orElseThrow(() -> new ResourceNotFoundException("Reservation", reservationId));

        if (reservation.getStatus() != ReservationStatus.ACTIVE) {
            log.warn("Cannot release reservation in status: {}", reservation.getStatus());
            return;
        }

        // Release seats in Redis
        List<String> seatKeys = reservation.getSeatIds().stream()
            .map(seatId -> InventoryService.buildSeatKey(reservation.getShowId(), seatId))
            .collect(Collectors.toList());

        redisTemplate.execute(seatReleaseScript, seatKeys, reservationId);

        // Update DB
        reservation.setStatus(ReservationStatus.EXPIRED);
        reservationRepository.save(reservation);
        log.info("Reservation released: {}", reservationId);
    }

    /**
     * Get a reservation by its ID.
     */
    public Reservation getReservation(String reservationId) {
        return reservationRepository.findByReservationId(reservationId)
            .orElseThrow(() -> new ResourceNotFoundException("Reservation", reservationId));
    }

    /**
     * Check if a reservation is still active (not expired).
     */
    public boolean isReservationActive(String reservationId) {
        Reservation reservation = getReservation(reservationId);
        if (reservation.getStatus() != ReservationStatus.ACTIVE) {
            return false;
        }
        if (reservation.isExpired()) {
            // Auto-expire
            releaseReservation(reservationId);
            return false;
        }
        return true;
    }

    private ReservationResponse toResponse(Reservation reservation) {
        long remainingTtl = Duration.between(LocalDateTime.now(), reservation.getExpiresAt()).getSeconds();
        return ReservationResponse.builder()
            .reservationId(reservation.getReservationId())
            .showId(reservation.getShowId())
            .seatIds(reservation.getSeatIds())
            .status(reservation.getStatus().name())
            .expiresAt(reservation.getExpiresAt())
            .remainingTtlSeconds(Math.max(0, remainingTtl))
            .createdAt(reservation.getCreatedAt())
            .build();
    }
}
