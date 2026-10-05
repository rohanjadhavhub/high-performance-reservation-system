package com.bookmyshow.repository;

import com.bookmyshow.domain.model.Booking;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface BookingRepository extends JpaRepository<Booking, Long> {

    Optional<Booking> findByBookingId(String bookingId);

    Optional<Booking> findByReservationId(String reservationId);

    List<Booking> findByUserId(Long userId);

    List<Booking> findByShowId(Long showId);

    boolean existsByReservationId(String reservationId);
}
