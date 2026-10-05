package com.bookmyshow.domain.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Outbox table entry for reliable event publishing.
 * Written in the same DB transaction as the booking.
 * A background worker reads unpublished events and publishes them
 * to the message broker, then marks them as published.
 */
@Entity
@Table(name = "outbox_events", indexes = {
    @Index(name = "idx_outbox_published", columnList = "published"),
    @Index(name = "idx_outbox_created", columnList = "createdAt")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OutboxEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, updatable = false)
    @Builder.Default
    private String eventId = UUID.randomUUID().toString();

    /** The aggregate type that produced this event (e.g., "Booking") */
    @Column(nullable = false)
    private String aggregateType;

    /** The aggregate ID (e.g., bookingId) */
    @Column(nullable = false)
    private String aggregateId;

    /** Event type (e.g., "BOOKING_CONFIRMED", "BOOKING_CANCELLED") */
    @Column(nullable = false)
    private String eventType;

    /** JSON payload */
    @Column(nullable = false, columnDefinition = "TEXT")
    private String payload;

    @Column(nullable = false)
    @Builder.Default
    private Boolean published = false;

    @Column(nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    private LocalDateTime publishedAt;
}
