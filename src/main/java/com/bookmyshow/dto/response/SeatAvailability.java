package com.bookmyshow.dto.response;

import com.bookmyshow.domain.statemachine.SeatStatus;
import lombok.*;

/**
 * Represents a single seat's availability information for the seat map.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SeatAvailability {

    private Long seatId;
    private String rowLabel;
    private Integer seatNumber;
    private String category;
    private Double price;
    private SeatStatus status;
}
