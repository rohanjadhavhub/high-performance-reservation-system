package com.bookmyshow.repository;

import com.bookmyshow.domain.model.Show;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ShowRepository extends JpaRepository<Show, Long> {

    List<Show> findByMovieIdAndActiveTrue(Long movieId);

    List<Show> findByScreenIdAndActiveTrue(Long screenId);

    @Query("SELECT s FROM Show s WHERE s.movie.id = :movieId AND s.startTime >= :from AND s.active = true ORDER BY s.startTime")
    List<Show> findUpcomingShowsForMovie(@Param("movieId") Long movieId, @Param("from") LocalDateTime from);

    @Query("SELECT s FROM Show s JOIN s.screen sc JOIN sc.theater t " +
           "WHERE s.movie.id = :movieId AND t.city = :city AND s.startTime >= :from AND s.active = true " +
           "ORDER BY s.startTime")
    List<Show> findShowsByMovieAndCity(@Param("movieId") Long movieId,
                                       @Param("city") String city,
                                       @Param("from") LocalDateTime from);
}
