package com.hackathon.backend.controllers;

import com.hackathon.backend.services.MovieEmbeddingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Admin/internal controller for managing movie embeddings.
 * <p>
 * These endpoints are for development and initialization — not for
 * end-user traffic. In production, they should be secured or disabled.
 */
@RestController
@RequestMapping("/api/admin/embeddings")
@RequiredArgsConstructor
public class EmbeddingController {

    private final MovieEmbeddingService movieEmbeddingService;

    /**
     * Generate and persist embedding for a single movie.
     *
     * @param movieId the movie to embed
     */
    @PostMapping("/movies/{movieId}")
    public ResponseEntity<Map<String, String>> embedMovie(@PathVariable String movieId) {
        movieEmbeddingService.embedMovie(movieId);
        return ResponseEntity.ok(Map.of("status", "ok", "movieId", movieId));
    }

    /**
     * Batch-embed all movies that are missing embeddings.
     * <p>
     * This is intended for initial setup or after a model change.
     */
    @PostMapping("/movies/batch")
    public ResponseEntity<Map<String, Object>> embedAllMovies() {
        int count = movieEmbeddingService.embedAllMovies();
        return ResponseEntity.ok(Map.of("status", "ok", "embeddedCount", count));
    }
}
