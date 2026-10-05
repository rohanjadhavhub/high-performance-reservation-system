package com.bookmyshow.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

/**
 * Request to confirm a booking after successful payment.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ConfirmBookingRequest {

    @NotBlank(message = "Payment intent ID is required")
    private String paymentIntentId;

    @NotBlank(message = "Gateway transaction ID is required")
    private String gatewayTransactionId;
}
