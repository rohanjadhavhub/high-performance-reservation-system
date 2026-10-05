package com.bookmyshow.dto.response;

import lombok.*;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Response returned when a booking is confirmed.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BookingResponse {

    private String bookingId;
    private String reservationId;
    private String paymentIntentId;
    private Long showId;
    private List<Long> seatIds;
    private Double totalAmount;
    private String status;
    private LocalDateTime createdAt;
}
