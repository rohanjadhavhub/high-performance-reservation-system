package com.bookmyshow.service;

import com.bookmyshow.domain.model.Seat;
import com.bookmyshow.domain.model.Show;
import com.bookmyshow.domain.statemachine.SeatStatus;
import com.bookmyshow.dto.response.SeatAvailability;
import com.bookmyshow.exception.ResourceNotFoundException;
import com.bookmyshow.repository.SeatRepository;
import com.bookmyshow.repository.ShowRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Inventory Service - reads seat availability from Redis.
 * Provides a mildly-stale informational seat map for the UI.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class InventoryService {

    private final SeatRepository seatRepository;
    private final ShowRepository showRepository;
    private final RedisTemplate<String, String> redisTemplate;

    /**
     * Get the seat map with real-time availability for a show.
     * Reads status from Redis; absent keys imply AVAILABLE.
     */
    public List<SeatAvailability> getSeatMap(Long showId) {
        Show show = showRepository.findById(showId)
            .orElseThrow(() -> new ResourceNotFoundException("Show", showId));

        List<Seat> seats = seatRepository.findByScreenId(show.getScreen().getId());
        List<SeatAvailability> seatMap = new ArrayList<>(seats.size());

        for (Seat seat : seats) {
            String redisKey = buildSeatKey(showId, seat.getId());
            String redisValue = redisTemplate.opsForValue().get(redisKey);
            SeatStatus status = parseSeatStatus(redisValue);

            seatMap.add(SeatAvailability.builder()
                .seatId(seat.getId())
                .rowLabel(seat.getRowLabel())
                .seatNumber(seat.getSeatNumber())
                .category(seat.getCategory().name())
                .price(seat.getPrice())
                .status(status)
                .build());
        }

        return seatMap;
    }

    /**
     * Build the Redis key for a show-seat combination.
     * Format: "show:{showId}:seat:{seatId}"
     */
    public static String buildSeatKey(Long showId, Long seatId) {
        return "show:" + showId + ":seat:" + seatId;
    }

    /**
     * Parse seat status from Redis value.
     * Values are stored as "STATUS:reservationId" (e.g., "HELD:abc-123").
     * Null/absent keys mean AVAILABLE.
     */
    private SeatStatus parseSeatStatus(String redisValue) {
        if (redisValue == null || redisValue.isEmpty()) {
            return SeatStatus.AVAILABLE;
        }
        if (redisValue.startsWith("HELD:")) {
            return SeatStatus.HELD;
        }
        if (redisValue.startsWith("BOOKED:")) {
            return SeatStatus.BOOKED;
        }
        return SeatStatus.fromString(redisValue);
    }
}
