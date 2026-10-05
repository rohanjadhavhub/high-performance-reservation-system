package com.bookmyshow.dto.response;

import lombok.*;
import java.time.LocalDateTime;

/**
 * Response returned when a payment intent is created.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentIntentResponse {

    private String paymentIntentId;
    private String reservationId;
    private Double amount;
    private String currency;
    private String status;
    private String gatewayName;
    private String gatewayTransactionId;
    private LocalDateTime createdAt;
}
