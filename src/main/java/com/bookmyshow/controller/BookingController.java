package com.bookmyshow.controller;

import com.bookmyshow.dto.request.ConfirmBookingRequest;
import com.bookmyshow.dto.response.ApiResponse;
import com.bookmyshow.dto.response.BookingResponse;
import com.bookmyshow.service.BookingFacade;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Booking Controller - booking confirmation and retrieval.
 *
 * POST /api/v1/bookings/confirm - Confirm booking (webhook / client polling)
 * GET /api/v1/bookings/{id} - Get booking details
 * GET /api/v1/bookings/user/{userId} - Get user's bookings
 */
@RestController
@RequestMapping("/api/v1/bookings")
@RequiredArgsConstructor
public class BookingController {

    private final BookingFacade bookingFacade;

    @PostMapping("/confirm")
    public ResponseEntity<ApiResponse<BookingResponse>> confirmBooking(
            @Valid @RequestBody ConfirmBookingRequest request) {

        BookingResponse response = bookingFacade.confirmBooking(request);
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.success(response, "Booking confirmed"));
    }

    @GetMapping("/{bookingId}")
    public ResponseEntity<ApiResponse<BookingResponse>> getBooking(
            @PathVariable String bookingId) {

        BookingResponse response = bookingFacade.getBooking(bookingId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<ApiResponse<List<BookingResponse>>> getUserBookings(
            @PathVariable Long userId) {

        List<BookingResponse> bookings = bookingFacade.getUserBookings(userId);
        return ResponseEntity.ok(ApiResponse.success(bookings));
    }
}
