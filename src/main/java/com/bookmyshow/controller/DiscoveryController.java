package com.bookmyshow.controller;

import com.bookmyshow.domain.model.Movie;
import com.bookmyshow.domain.model.Show;
import com.bookmyshow.dto.response.ApiResponse;
import com.bookmyshow.service.DiscoveryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Discovery Controller - movie/show browsing endpoints.
 * Designed for high availability with eventual consistency.
 */
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class DiscoveryController {

    private final DiscoveryService discoveryService;

    @GetMapping("/movies")
    public ResponseEntity<ApiResponse<List<Movie>>> getMovies() {
        List<Movie> movies = discoveryService.getActiveMovies();
        return ResponseEntity.ok(ApiResponse.success(movies));
    }

    @GetMapping("/movies/search")
    public ResponseEntity<ApiResponse<List<Movie>>> searchMovies(@RequestParam String query) {
        List<Movie> movies = discoveryService.searchMovies(query);
        return ResponseEntity.ok(ApiResponse.success(movies));
    }

    @GetMapping("/movies/{movieId}")
    public ResponseEntity<ApiResponse<Movie>> getMovie(@PathVariable Long movieId) {
        Movie movie = discoveryService.getMovieById(movieId);
        return ResponseEntity.ok(ApiResponse.success(movie));
    }

    @GetMapping("/movies/{movieId}/shows")
    public ResponseEntity<ApiResponse<List<Show>>> getShows(@PathVariable Long movieId,
                                                             @RequestParam(required = false) String city) {
        List<Show> shows;
        if (city != null && !city.isBlank()) {
            shows = discoveryService.getShowsForMovieInCity(movieId, city);
        } else {
            shows = discoveryService.getShowsForMovie(movieId);
        }
        return ResponseEntity.ok(ApiResponse.success(shows));
    }

    @GetMapping("/shows/{showId}")
    public ResponseEntity<ApiResponse<Show>> getShow(@PathVariable Long showId) {
        Show show = discoveryService.getShowById(showId);
        return ResponseEntity.ok(ApiResponse.success(show));
    }
}
