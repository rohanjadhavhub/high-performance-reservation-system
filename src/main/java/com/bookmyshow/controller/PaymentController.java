package com.bookmyshow.controller;

import com.bookmyshow.dto.request.InitiatePaymentRequest;
import com.bookmyshow.dto.response.ApiResponse;
import com.bookmyshow.dto.response.PaymentIntentResponse;
import com.bookmyshow.service.BookingFacade;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Payment Controller - payment initiation endpoint.
 * Requires Idempotency-Key to prevent double-charging (Scenario 3 from PRD).
 *
 * POST /api/v1/payments/intent - Create a payment intent
 */
@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final BookingFacade bookingFacade;

    @PostMapping("/intent")
    public ResponseEntity<ApiResponse<PaymentIntentResponse>> initiatePayment(
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @Valid @RequestBody InitiatePaymentRequest request) {

        PaymentIntentResponse response = bookingFacade.initiatePayment(idempotencyKey, request);
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.success(response, "Payment initiated"));
    }
}
