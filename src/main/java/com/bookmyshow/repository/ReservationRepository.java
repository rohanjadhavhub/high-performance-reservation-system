package com.bookmyshow.repository;

import com.bookmyshow.domain.model.Reservation;
import com.bookmyshow.domain.model.ReservationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ReservationRepository extends JpaRepository<Reservation, Long> {

    Optional<Reservation> findByReservationId(String reservationId);

    Optional<Reservation> findByIdempotencyKey(String idempotencyKey);

    List<Reservation> findByStatusAndExpiresAtBefore(ReservationStatus status, LocalDateTime before);

    List<Reservation> findByShowIdAndStatus(Long showId, ReservationStatus status);
}
