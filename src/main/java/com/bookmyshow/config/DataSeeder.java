package com.bookmyshow.config;

import com.bookmyshow.domain.model.*;
import com.bookmyshow.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Seeds the database with sample data for development.
 * Only runs with the "dev" profile.
 */
@Slf4j
@Component
@Profile("dev")
@RequiredArgsConstructor
public class DataSeeder implements CommandLineRunner {

    private final MovieRepository movieRepository;
    private final TheaterRepository theaterRepository;
    private final ScreenRepository screenRepository;
    private final SeatRepository seatRepository;
    private final ShowRepository showRepository;

    @Override
    public void run(String... args) {
        if (movieRepository.count() > 0) {
            log.info("Database already seeded, skipping.");
            return;
        }

        log.info("Seeding development database...");

        // Movies
        Movie movie1 = movieRepository.save(Movie.builder()
            .title("Interstellar 2")
            .description("A mind-bending sequel to the epic space odyssey")
            .durationMinutes(165)
            .genre("Sci-Fi")
            .language("English")
            .releaseDate(LocalDateTime.now().minusDays(7))
            .active(true)
            .build());

        Movie movie2 = movieRepository.save(Movie.builder()
            .title("Pushpa 3: The Rule")
            .description("The biggest Indian blockbuster continues")
            .durationMinutes(180)
            .genre("Action")
            .language("Telugu")
            .releaseDate(LocalDateTime.now().minusDays(3))
            .active(true)
            .build());

        Movie movie3 = movieRepository.save(Movie.builder()
            .title("The Dark Knight Returns")
            .description("Batman returns in a spectacular new adventure")
            .durationMinutes(155)
            .genre("Action")
            .language("English")
            .releaseDate(LocalDateTime.now().plusDays(2))
            .active(true)
            .build());

        // Theater
        Theater theater = theaterRepository.save(Theater.builder()
            .name("PVR Phoenix")
            .city("Mumbai")
            .address("Lower Parel, Phoenix Palladium")
            .totalScreens(2)
            .build());

        // Screens
        Screen screen1 = screenRepository.save(Screen.builder()
            .name("Screen 1 - IMAX")
            .totalSeats(50)
            .theater(theater)
            .build());

        Screen screen2 = screenRepository.save(Screen.builder()
            .name("Screen 2 - Dolby")
            .totalSeats(40)
            .theater(theater)
            .build());

        // Seats for Screen 1 (5 rows x 10 seats)
        createSeats(screen1, 5, 10);

        // Seats for Screen 2 (4 rows x 10 seats)
        createSeats(screen2, 4, 10);

        // Shows
        LocalDateTime today = LocalDateTime.now().withHour(10).withMinute(0).withSecond(0);

        showRepository.save(Show.builder()
            .movie(movie1).screen(screen1)
            .startTime(today.plusHours(2))
            .endTime(today.plusHours(2).plusMinutes(movie1.getDurationMinutes()))
            .build());

        showRepository.save(Show.builder()
            .movie(movie1).screen(screen1)
            .startTime(today.plusHours(6))
            .endTime(today.plusHours(6).plusMinutes(movie1.getDurationMinutes()))
            .build());

        showRepository.save(Show.builder()
            .movie(movie2).screen(screen2)
            .startTime(today.plusHours(3))
            .endTime(today.plusHours(3).plusMinutes(movie2.getDurationMinutes()))
            .build());

        showRepository.save(Show.builder()
            .movie(movie3).screen(screen1)
            .startTime(today.plusDays(3).plusHours(4))
            .endTime(today.plusDays(3).plusHours(4).plusMinutes(movie3.getDurationMinutes()))
            .build());

        log.info("Database seeded with {} movies, {} theaters, {} screens, {} shows",
            3, 1, 2, 4);
    }

    private void createSeats(Screen screen, int rows, int seatsPerRow) {
        List<Seat> seats = new ArrayList<>();
        String[] rowLabels = {"A", "B", "C", "D", "E", "F", "G", "H"};

        for (int r = 0; r < rows; r++) {
            SeatCategory category;
            double price;
            if (r < 2) {
                category = SeatCategory.REGULAR;
                price = 200.0;
            } else if (r < 4) {
                category = SeatCategory.PREMIUM;
                price = 350.0;
            } else {
                category = SeatCategory.RECLINER;
                price = 500.0;
            }

            for (int s = 1; s <= seatsPerRow; s++) {
                seats.add(Seat.builder()
                    .screen(screen)
                    .rowLabel(rowLabels[r])
                    .seatNumber(s)
                    .category(category)
                    .price(price)
                    .build());
            }
        }

        seatRepository.saveAll(seats);
        log.info("Created {} seats for screen '{}'", seats.size(), screen.getName());
    }
}
