package com.bookmyshow.service;

import com.bookmyshow.domain.model.Movie;
import com.bookmyshow.domain.model.Show;
import com.bookmyshow.exception.ResourceNotFoundException;
import com.bookmyshow.repository.MovieRepository;
import com.bookmyshow.repository.ShowRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Discovery Service - handles movie/show browsing.
 * Designed for eventual consistency and high availability.
 * In production, results would be served from read replicas / CDN / Redis cache.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DiscoveryService {

    private final MovieRepository movieRepository;
    private final ShowRepository showRepository;

    public List<Movie> getActiveMovies() {
        return movieRepository.findByActiveTrue();
    }

    public List<Movie> searchMovies(String query) {
        return movieRepository.findByTitleContainingIgnoreCase(query);
    }

    public Movie getMovieById(Long id) {
        return movieRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Movie", id));
    }

    public List<Show> getShowsForMovie(Long movieId) {
        return showRepository.findUpcomingShowsForMovie(movieId, LocalDateTime.now());
    }

    public List<Show> getShowsForMovieInCity(Long movieId, String city) {
        return showRepository.findShowsByMovieAndCity(movieId, city, LocalDateTime.now());
    }

    public Show getShowById(Long showId) {
        return showRepository.findById(showId)
            .orElseThrow(() -> new ResourceNotFoundException("Show", showId));
    }
}
