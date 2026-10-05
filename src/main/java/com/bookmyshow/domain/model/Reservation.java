package com.bookmyshow.domain.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * A time-bound reservation holding one or more seats for a show.
 * Managed primarily in Redis for performance, persisted to DB for auditability.
 */
@Entity
@Table(name = "reservations", indexes = {
    @Index(name = "idx_reservation_show", columnList = "show_id"),
    @Index(name = "idx_reservation_idempotency", columnList = "idempotencyKey", unique = true),
    @Index(name = "idx_reservation_status", columnList = "status")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Reservation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, updatable = false)
    @Builder.Default
    private String reservationId = UUID.randomUUID().toString();

    @Column(nullable = false, unique = true, updatable = false)
    private String idempotencyKey;

    @Column(name = "show_id", nullable = false)
    private Long showId;

    @Column(nullable = false)
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private ReservationStatus status = ReservationStatus.ACTIVE;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "reservation_seats", joinColumns = @JoinColumn(name = "reservation_id"))
    @Column(name = "seat_id")
    @Builder.Default
    private List<Long> seatIds = new ArrayList<>();

    @Column(nullable = false)
    private LocalDateTime expiresAt;

    @Column(nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    private LocalDateTime updatedAt;

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public boolean isExpired() {
        return LocalDateTime.now().isAfter(expiresAt);
    }
}
