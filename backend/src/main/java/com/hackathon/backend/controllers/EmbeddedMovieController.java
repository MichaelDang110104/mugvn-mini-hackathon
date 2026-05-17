package com.hackathon.backend.controllers;

import com.hackathon.backend.models.EmbeddedMovie;
import com.hackathon.backend.repositories.EmbeddedMovieRepository;
import lombok.RequiredArgsConstructor;
import org.bson.types.ObjectId;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/embedded-movies")
@RequiredArgsConstructor
public class EmbeddedMovieController {

    private final EmbeddedMovieRepository embeddedMovieRepository;

    @GetMapping
    public Page<EmbeddedMovie> getAll(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "year") String sortBy,
            @RequestParam(defaultValue = "desc") String direction) {

        Sort sort = direction.equalsIgnoreCase("asc")
                ? Sort.by(sortBy).ascending()
                : Sort.by(sortBy).descending();

        return embeddedMovieRepository.findAll(PageRequest.of(page, size, sort));
    }

    @GetMapping("/{id}")
    public ResponseEntity<EmbeddedMovie> getById(@PathVariable String id) {
        return embeddedMovieRepository.findById(new ObjectId(id))
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/search")
    public List<EmbeddedMovie> searchByTitle(@RequestParam String title) {
        return embeddedMovieRepository.findByTitleContainingIgnoreCase(title);
    }

    @GetMapping("/genre/{genre}")
    public List<EmbeddedMovie> getByGenre(@PathVariable String genre) {
        return embeddedMovieRepository.findByGenresContaining(genre);
    }

    @GetMapping("/year/{year}")
    public List<EmbeddedMovie> getByYear(@PathVariable Integer year) {
        return embeddedMovieRepository.findByYear(year);
    }

    @GetMapping("/cast/{actor}")
    public List<EmbeddedMovie> getByCast(@PathVariable String actor) {
        return embeddedMovieRepository.findByCastContaining(actor);
    }

    @GetMapping("/director/{director}")
    public List<EmbeddedMovie> getByDirector(@PathVariable String director) {
        return embeddedMovieRepository.findByDirectorsContaining(director);
    }
}
