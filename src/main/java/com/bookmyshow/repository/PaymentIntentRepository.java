package com.bookmyshow.repository;

import com.bookmyshow.domain.model.PaymentIntent;
import com.bookmyshow.domain.model.PaymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentIntentRepository extends JpaRepository<PaymentIntent, Long> {

    Optional<PaymentIntent> findByPaymentIntentId(String paymentIntentId);

    Optional<PaymentIntent> findByIdempotencyKey(String idempotencyKey);

    Optional<PaymentIntent> findByReservationId(String reservationId);

    List<PaymentIntent> findByStatus(PaymentStatus status);

    /**
     * Find successful payments that lack a corresponding booking (for reconciliation).
     */
    @Query("SELECT pi FROM PaymentIntent pi WHERE pi.status = 'SUCCESS' " +
           "AND pi.reservationId NOT IN (SELECT b.reservationId FROM Booking b) " +
           "AND pi.createdAt >= :since")
    List<PaymentIntent> findOrphanedSuccessfulPayments(@Param("since") LocalDateTime since);
}
