package com.bookmyshow.dto.response;

import lombok.*;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Response returned when a reservation is created.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReservationResponse {

    private String reservationId;
    private Long showId;
    private List<Long> seatIds;
    private String status;
    private LocalDateTime expiresAt;
    private Long remainingTtlSeconds;
    private LocalDateTime createdAt;
}
