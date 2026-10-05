package com.bookmyshow.controller;

import com.bookmyshow.dto.response.ApiResponse;
import com.bookmyshow.dto.response.SeatAvailability;
import com.bookmyshow.service.BookingFacade;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Inventory Controller - seat map endpoint.
 * Returns mildly stale informational data from Redis/cache.
 */
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class InventoryController {

    private final BookingFacade bookingFacade;

    @GetMapping("/shows/{showId}/seats")
    public ResponseEntity<ApiResponse<List<SeatAvailability>>> getSeatMap(
            @PathVariable Long showId) {
        List<SeatAvailability> seatMap = bookingFacade.getSeatMap(showId);
        return ResponseEntity.ok(ApiResponse.success(seatMap));
    }
}
