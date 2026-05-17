package com.hackathon.backend.controllers;

import com.hackathon.backend.models.Movie;
import com.hackathon.backend.repositories.MovieRepository;
import lombok.RequiredArgsConstructor;
import org.bson.types.ObjectId;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/movies")
@RequiredArgsConstructor
public class MovieController {

    private final MovieRepository movieRepository;

    @GetMapping
    public Page<Movie> getAllMovies(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "year") String sortBy,
            @RequestParam(defaultValue = "desc") String direction) {

        Sort sort = direction.equalsIgnoreCase("asc")
                ? Sort.by(sortBy).ascending()
                : Sort.by(sortBy).descending();

        return movieRepository.findAll(PageRequest.of(page, size, sort));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Movie> getMovieById(@PathVariable String id) {
        return movieRepository.findById(new ObjectId(id))
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/search")
    public List<Movie> searchByTitle(@RequestParam String title) {
        return movieRepository.findByTitleContainingIgnoreCase(title);
    }

    @GetMapping("/genre/{genre}")
    public Page<Movie> getByGenre(
            @PathVariable String genre,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return movieRepository.findByGenresContaining(genre, PageRequest.of(page, size));
    }

    @GetMapping("/year")
    public Page<Movie> getByYearRange(
            @RequestParam int from,
            @RequestParam int to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return movieRepository.findByYearBetween(from, to, PageRequest.of(page, size));
    }

    @GetMapping("/cast/{actor}")
    public List<Movie> getByCast(@PathVariable String actor) {
        return movieRepository.findByCastContaining(actor);
    }

    @GetMapping("/director/{director}")
    public List<Movie> getByDirector(@PathVariable String director) {
        return movieRepository.findByDirectorsContaining(director);
    }

    @GetMapping("/country/{country}")
    public List<Movie> getByCountry(@PathVariable String country) {
        return movieRepository.findByCountriesContaining(country);
    }

    @GetMapping("/language/{language}")
    public List<Movie> getByLanguage(@PathVariable String language) {
        return movieRepository.findByLanguagesContaining(language);
    }

    @GetMapping("/rated/{rated}")
    public List<Movie> getByRated(@PathVariable String rated) {
        return movieRepository.findByRated(rated);
    }

    @GetMapping("/top-rated")
    public List<Movie> getTopRated(@RequestParam(defaultValue = "8.0") Double minRating) {
        return movieRepository.findByImdbRatingGreaterThanEqual(minRating);
    }
}
