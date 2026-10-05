package com.bookmyshow.domain.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Tracks a payment intent against a reservation.
 * Created when user initiates payment, updated on gateway callback.
 */
@Entity
@Table(name = "payment_intents", indexes = {
    @Index(name = "idx_payment_reservation", columnList = "reservationId"),
    @Index(name = "idx_payment_idempotency", columnList = "idempotencyKey", unique = true),
    @Index(name = "idx_payment_status", columnList = "status")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentIntent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, updatable = false)
    @Builder.Default
    private String paymentIntentId = UUID.randomUUID().toString();

    @Column(nullable = false, unique = true, updatable = false)
    private String idempotencyKey;

    @Column(nullable = false)
    private String reservationId;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false)
    private Double amount;

    @Column(nullable = false)
    private String currency;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private PaymentStatus status = PaymentStatus.PENDING;

    /** The payment gateway used (e.g., "RAZORPAY", "DODO") */
    @Column(nullable = false)
    private String gatewayName;

    /** Transaction ID returned by the external gateway */
    private String gatewayTransactionId;

    private String failureReason;

    @Column(nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    private LocalDateTime updatedAt;

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
