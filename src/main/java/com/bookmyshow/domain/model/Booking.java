package com.bookmyshow.domain.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Confirmed booking - the terminal, durable record of a successful ticket purchase.
 * Created only after payment is verified and reservation is still active.
 */
@Entity
@Table(name = "bookings", indexes = {
    @Index(name = "idx_booking_reservation", columnList = "reservationId", unique = true),
    @Index(name = "idx_booking_user", columnList = "userId"),
    @Index(name = "idx_booking_show", columnList = "showId")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Booking {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, updatable = false)
    @Builder.Default
    private String bookingId = UUID.randomUUID().toString();

    @Column(nullable = false, unique = true)
    private String reservationId;

    @Column(nullable = false)
    private String paymentIntentId;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false)
    private Long showId;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "booking_seats", joinColumns = @JoinColumn(name = "booking_id"))
    @Column(name = "seat_id")
    @Builder.Default
    private List<Long> seatIds = new ArrayList<>();

    @Column(nullable = false)
    private Double totalAmount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private BookingStatus status = BookingStatus.CONFIRMED;

    @Column(nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
}
