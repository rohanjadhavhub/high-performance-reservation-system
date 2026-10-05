package com.bookmyshow.repository;

import com.bookmyshow.domain.model.Movie;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface MovieRepository extends JpaRepository<Movie, Long> {

    List<Movie> findByActiveTrue();


    List<Movie> findByGenreIgnoreCase(String genre);

    // For searching by title
    List<Movie> findByTitleContainingIgnoreCase(String title);
}
