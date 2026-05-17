package com.hackathon.backend.controllers;

import com.hackathon.backend.dto.ErrorResponse;
import com.hackathon.backend.dto.MovieDetailResponse;
import com.hackathon.backend.dto.SearchResponse;
import com.hackathon.backend.models.EmbeddedMovie;
import com.hackathon.backend.services.MovieSearchService;
import com.hackathon.backend.services.SessionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Contract-aligned movie endpoints:
 * - GET /api/movies/search
 * - GET /api/movies/{movieId}
 *
 * Per external-api-contracts.md
 */
@RestController
@RequestMapping("/api/movies")
@RequiredArgsConstructor
@Slf4j
public class ContractMovieController {

    private final MovieSearchService movieSearchService;
    private final SessionService sessionService;
    private final MongoTemplate mongoTemplate;

    /**
     * GET /api/movies/search
     * Search movies using semantic retrieval with deterministic fallback behavior.
     */
    @GetMapping("/search")
    public ResponseEntity<?> searchMovies(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) Integer limit,
            @RequestParam(required = false) String sessionId,
            @RequestParam(required = false) String region,
            @RequestHeader(value = "X-Session-Id", required = false) String headerSessionId) {

        try {
            String resolvedSessionId = sessionService.resolveSessionId(headerSessionId, sessionId);

            SearchResponse response = movieSearchService.search(q, limit);

            HttpHeaders headers = new HttpHeaders();
            headers.set("X-Session-Id", resolvedSessionId);

            return ResponseEntity.ok()
                    .headers(headers)
                    .body(response);

        } catch (SessionService.SessionConflictException e) {
            return ResponseEntity.badRequest()
                    .body(ErrorResponse.validationError(e.getMessage(),
                            List.of(new ErrorResponse.FieldError("sessionId", "conflict"))));
        } catch (Exception e) {
            log.error("Search failed: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(ErrorResponse.internalError("unable to perform search"));
        }
    }

    /**
     * GET /api/movies/{movieId}
     * Return movie detail plus similar-movie recommendations.
     */
    @GetMapping("/{movieId}")
    public ResponseEntity<?> getMovieDetail(
            @PathVariable String movieId,
            @RequestParam(required = false) String sessionId,
            @RequestParam(required = false) String region,
            @RequestHeader(value = "X-Session-Id", required = false) String headerSessionId) {

        try {
            String resolvedSessionId = sessionService.resolveSessionId(headerSessionId, sessionId);

            // Fetch the movie
            Query query = new Query(Criteria.where("_id").is(new ObjectId(movieId)));
            query.fields().exclude("plot_embedding").exclude("plot_embedding_voyage_3_large");
            EmbeddedMovie movie = mongoTemplate.findOne(query, EmbeddedMovie.class);

            if (movie == null) {
                return ResponseEntity.status(404)
                        .body(ErrorResponse.notFound("movie not found"));
            }

            // Build detailed movie response
            MovieDetailResponse.MovieDetail detail = MovieDetailResponse.MovieDetail.builder()
                    .id(movie.getId() != null ? movie.getId().toHexString() : null)
                    .title(movie.getTitle())
                    .overview(movie.getPlot())
                    .fullplot(movie.getFullplot())
                    .genres(movie.getGenres())
                    .posterUrl(movie.getPoster())
                    .ratingAvg(movie.getImdb() != null ? movie.getImdb().getRating() : null)
                    .cast(movie.getCast())
                    .directors(movie.getDirectors())
                    .writers(movie.getWriters())
                    .languages(movie.getLanguages())
                    .countries(movie.getCountries())
                    .runtime(movie.getRuntime())
                    .year(movie.getYear())
                    .rated(movie.getRated())
                    .availability(MovieDetailResponse.Availability.builder()
                            .isAvailable(true)
                            .region("global")
                            .build())
                    .build();

            // Find similar movies
            List<SearchResponse.SearchItem> similarMovies;
            String mode = "semantic";
            boolean fallbackUsed = false;

            try {
                similarMovies = movieSearchService.findSimilarMovies(movieId, 10);
                if (similarMovies.isEmpty()) {
                    mode = "cold_start";
                    fallbackUsed = true;
                }
            } catch (Exception e) {
                log.warn("Similar movie lookup failed for [{}]: {}", movieId, e.getMessage());
                similarMovies = List.of();
                mode = "fallback_text";
                fallbackUsed = true;
            }

            MovieDetailResponse response = MovieDetailResponse.builder()
                    .movie(detail)
                    .similarMovies(similarMovies)
                    .mode(mode)
                    .fallbackUsed(fallbackUsed)
                    .build();

            HttpHeaders headers = new HttpHeaders();
            headers.set("X-Session-Id", resolvedSessionId);

            return ResponseEntity.ok()
                    .headers(headers)
                    .body(response);

        } catch (SessionService.SessionConflictException e) {
            return ResponseEntity.badRequest()
                    .body(ErrorResponse.validationError(e.getMessage(),
                            List.of(new ErrorResponse.FieldError("sessionId", "conflict"))));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(404)
                    .body(ErrorResponse.notFound("movie not found"));
        } catch (Exception e) {
            log.error("Movie detail failed for [{}]: {}", movieId, e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(ErrorResponse.internalError("unable to fetch movie details"));
        }
    }
}
