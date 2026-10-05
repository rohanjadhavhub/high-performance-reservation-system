package com.bookmyshow.controller;

import com.bookmyshow.dto.request.CreateReservationRequest;
import com.bookmyshow.dto.response.ApiResponse;
import com.bookmyshow.dto.response.ReservationResponse;
import com.bookmyshow.service.BookingFacade;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Reservation Controller - seat hold endpoints.
 * Requires Idempotency-Key header for all mutations.
 *
 * POST /api/v1/reservations - Hold seats (strong consistency via Redis Lua)
 * DELETE /api/v1/reservations/{id} - Release held seats
 */
@RestController
@RequestMapping("/api/v1/reservations")
@RequiredArgsConstructor
public class ReservationController {

    private final BookingFacade bookingFacade;

    @PostMapping
    public ResponseEntity<ApiResponse<ReservationResponse>> createReservation(
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @Valid @RequestBody CreateReservationRequest request) {

        ReservationResponse response = bookingFacade.holdSeats(idempotencyKey, request);
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.success(response, "Seats held successfully"));
    }

    @DeleteMapping("/{reservationId}")
    public ResponseEntity<ApiResponse<Void>> cancelReservation(
            @PathVariable String reservationId) {

        bookingFacade.cancelReservation(reservationId);
        return ResponseEntity.ok(ApiResponse.success(null, "Reservation cancelled"));
    }
}
