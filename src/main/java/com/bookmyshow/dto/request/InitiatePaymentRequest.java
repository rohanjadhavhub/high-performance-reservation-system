package com.bookmyshow.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

/**
 * Request to initiate a payment for a reservation.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InitiatePaymentRequest {

    @NotBlank(message = "Reservation ID is required")
    private String reservationId;

    @NotNull(message = "User ID is required")
    private Long userId;

    /** Optional: specify the payment gateway (defaults to RAZORPAY) */
    private String gatewayName;
}
