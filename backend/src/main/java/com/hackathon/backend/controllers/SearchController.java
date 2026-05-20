package com.hackathon.backend.controllers;

import com.hackathon.backend.dto.SearchResponse;
import com.hackathon.backend.dto.SearchResponse.Availability;
import com.hackathon.backend.dto.SearchResponse.MovieSummary;
import com.hackathon.backend.dto.SearchResponse.SearchItem;
import com.hackathon.backend.engine.RecommendationEngine;
import com.hackathon.backend.engine.entities.RecommendationContext;
import com.hackathon.backend.services.MovieSearchService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.concurrent.CompletableFuture;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class SearchController {

    private final MovieSearchService movieSearchService;
    private final RecommendationEngine recommendationEngine;

    @GetMapping("/search/movies")
    public ResponseEntity<SearchResponse> search(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) Integer limit,
            @RequestParam(required = false) String sessionId,
            @RequestHeader(value = "X-Session-Id", required = false) String headerSessionId) {

        SearchResponse response = movieSearchService.search(q, limit);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/v2/search/movies")
    public CompletableFuture<ResponseEntity<SearchResponse>> searchV2(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) Integer limit,
            @RequestHeader(value = "X-User-Id", required = false) String userId,
            @RequestHeader(value = "X-Session-Id", required = false) String sessionId) {

        String effectiveUserId = userId != null ? userId : sessionId;
        int effectiveLimit = (limit != null && limit > 0) ? limit : 10;

        RecommendationContext ctx = RecommendationContext.forSearch(effectiveUserId, q, effectiveLimit);

        return recommendationEngine.execute(ctx)
                .thenApply(movies -> {
                    List<SearchItem> items = movies.stream()
                            .map(m -> SearchItem.builder()
                                    .movie(MovieSummary.builder()
                                            .id(m.getId() != null ? m.getId().toHexString() : null)
                                            .title(m.getTitle())
                                            .posterUrl(m.getPoster())
                                            .genres(m.getGenres())
                                            .ratingAvg(m.getImdb() != null ? m.getImdb().getRating() : null)
                                            .availability(Availability.builder()
                                                    .isAvailable(true)
                                                    .region("global")
                                                    .build())
                                            .build())
                                    .score(0.0)
                                    .reasons(List.of())
                                    .build())
                            .toList();

                    return ResponseEntity.ok(SearchResponse.builder()
                            .items(items)
                            .mode("engine_v2")
                            .fallbackUsed(false)
                            .query(q)
                            .build());
                });
    }
}
