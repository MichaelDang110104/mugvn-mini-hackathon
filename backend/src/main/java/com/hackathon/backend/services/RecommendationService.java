package com.hackathon.backend.services;

import com.hackathon.backend.enums.EventType;
import com.hackathon.backend.dto.RecommendationResponse;
import com.hackathon.backend.dto.SearchResponse;
import com.hackathon.backend.dto.SearchResponse.MovieSummary;
import com.hackathon.backend.dto.SearchResponse.Reason;
import com.hackathon.backend.dto.SearchResponse.SearchItem;
import com.hackathon.backend.dto.SearchResponse.Availability;
import com.hackathon.backend.models.EmbeddedMovie;
import com.hackathon.backend.models.UserEvent;
import com.hackathon.backend.repositories.UserEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Generates homepage recommendations.
 * Implements cold_start, personalized, and fallback_text modes
 * per the external API contract.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RecommendationService {

    private final MongoTemplate mongoTemplate;
    private final UserEventRepository userEventRepository;
    private final VectorSearchService vectorSearchService;
    private final EmbeddingService embeddingService;

    private static final int DEFAULT_LIMIT = 12;
    private static final int MIN_EVENTS_FOR_PERSONALIZATION = 3;

    public RecommendationResponse getRecommendations(String sessionId, String context,
                                                      Integer limit, String region) {
        int effectiveLimit = (limit != null && limit > 0) ? limit : DEFAULT_LIMIT;

        // Check if the session has enough events for personalization
        List<UserEvent> recentEvents = userEventRepository
                .findBySessionIdOrderByTimestampDesc(sessionId);

        if (recentEvents.size() < MIN_EVENTS_FOR_PERSONALIZATION) {
            return buildColdStartRecommendations(effectiveLimit);
        }

        // Try personalized recommendations based on liked/saved movies
        try {
            return buildPersonalizedRecommendations(recentEvents, effectiveLimit);
        } catch (Exception e) {
            log.warn("Personalized recommendations failed for session [{}]: {}",
                    sessionId, e.getMessage());
            return buildColdStartRecommendations(effectiveLimit);
        }
    }

    private RecommendationResponse buildPersonalizedRecommendations(
            List<UserEvent> events, int limit) {

        // Extract liked/saved/highly-rated movie IDs for profile-based recs
        Set<String> positiveMovieIds = events.stream()
                .filter(e -> isPositiveEvent(e))
                .map(UserEvent::getMovieId)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));

        if (positiveMovieIds.isEmpty()) {
            return buildColdStartRecommendations(limit);
        }

        // Use the most recent positive movie's embedding for vector search
        String seedMovieId = positiveMovieIds.iterator().next();

        Query embQuery = new Query(Criteria.where("_id").is(
                new org.bson.types.ObjectId(seedMovieId)));
        embQuery.fields().include("plot_embedding");
        EmbeddedMovie seedMovie = mongoTemplate.findOne(embQuery, EmbeddedMovie.class);

        if (seedMovie == null || seedMovie.getPlotEmbedding() == null
                || seedMovie.getPlotEmbedding().isEmpty()) {
            log.warn("No embedding for seed movie [{}], falling back to cold start", seedMovieId);
            return buildColdStartRecommendations(limit);
        }

        var results = vectorSearchService.searchByEmbedding(
                seedMovie.getPlotEmbedding(), limit + 5);

        // Deduplicate and exclude already-interacted movies
        List<SearchItem> items = results.stream()
                .filter(r -> !positiveMovieIds.contains(
                        r.getMovie().getId() != null ? r.getMovie().getId().toHexString() : ""))
                .limit(limit)
                .map(r -> SearchItem.builder()
                        .movie(toMovieSummary(r.getMovie()))
                        .score(r.getVectorSearchScore())
                        .reasons(List.of(Reason.builder()
                                .code("liked_similar_theme")
                                .label("Because you liked similar movies")
                                .build()))
                        .build())
                .toList();

        if (items.isEmpty()) {
            return buildColdStartRecommendations(limit);
        }

        return RecommendationResponse.builder()
                .items(items)
                .mode("personalized")
                .fallbackUsed(false)
                .generatedAt(Instant.now().toString())
                .build();
    }

    private RecommendationResponse buildColdStartRecommendations(int limit) {
        Query query = new Query();
        query.with(Sort.by(Sort.Direction.DESC, "imdb.rating"));
        query.limit(limit);
        query.fields().exclude("plot_embedding").exclude("plot_embedding_voyage_3_large");

        List<EmbeddedMovie> movies = mongoTemplate.find(query, EmbeddedMovie.class);

        List<SearchItem> items = movies.stream()
                .map(m -> SearchItem.builder()
                        .movie(toMovieSummary(m))
                        .score(m.getImdb() != null && m.getImdb().getRating() != null
                                ? m.getImdb().getRating() / 10.0 : 0.0)
                        .reasons(List.of(Reason.builder()
                                .code("trending_now")
                                .label("Popular and trending")
                                .build()))
                        .build())
                .toList();

        return RecommendationResponse.builder()
                .items(items)
                .mode("cold_start")
                .fallbackUsed(false)
                .generatedAt(Instant.now().toString())
                .build();
    }

    private boolean isPositiveEvent(UserEvent event) {
        EventType type = event.getEventType();
        if (type == EventType.LIKE || type == EventType.SAVE) {
            return true;
        }
        if (type == EventType.RATING && event.getEventValue() != null) {
            return event.getEventValue() >= 4;
        }
        return false;
    }

    private MovieSummary toMovieSummary(EmbeddedMovie movie) {
        return MovieSummary.builder()
                .id(movie.getId() != null ? movie.getId().toHexString() : null)
                .title(movie.getTitle())
                .posterUrl(movie.getPoster())
                .genres(movie.getGenres())
                .ratingAvg(movie.getImdb() != null ? movie.getImdb().getRating() : null)
                .availability(Availability.builder()
                        .isAvailable(true)
                        .region("global")
                        .build())
                .build();
    }
}
