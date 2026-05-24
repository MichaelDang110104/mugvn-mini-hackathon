package com.hackathon.backend.services;

import com.hackathon.backend.dto.RecommendationResponse;
import com.hackathon.backend.dto.SearchResponse.Availability;
import com.hackathon.backend.dto.SearchResponse.MovieSummary;
import com.hackathon.backend.dto.SearchResponse.Reason;
import com.hackathon.backend.dto.SearchResponse.SearchItem;
import com.hackathon.backend.enums.EventType;
import com.hackathon.backend.models.EmbeddedMovie;
import com.hackathon.backend.models.StarterRecommendationCache;
import com.hackathon.backend.models.UserEvent;
import com.hackathon.backend.repositories.StarterRecommendationCacheRepository;
import com.hackathon.backend.repositories.UserEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bson.types.ObjectId;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
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
    private final StarterRecommendationCacheRepository starterRecommendationCacheRepository;
    private final VectorSearchService vectorSearchService;
    private final EmbeddingService embeddingService;

    private static final int DEFAULT_LIMIT = 12;
    private static final int MIN_EVENTS_FOR_PERSONALIZATION = 3;

    public RecommendationResponse getRecommendations(String sessionId, String context,
            Integer limit, String region) {
        int effectiveLimit = (limit != null && limit > 0) ? limit : DEFAULT_LIMIT;

        List<UserEvent> recentEvents = userEventRepository
                .findBySessionIdOrderByTimestampDesc(sessionId);

        if (recentEvents.size() < MIN_EVENTS_FOR_PERSONALIZATION) {
            RecommendationResponse onboardingResponse = buildOnboardingStarterRecommendations(sessionId, effectiveLimit);
            if (onboardingResponse != null) {
                return onboardingResponse;
            }
            return buildColdStartRecommendations(effectiveLimit);
        }

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

        Set<String> positiveMovieIds = events.stream()
                .filter(this::isPositiveEvent)
                .map(UserEvent::getMovieId)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));

        if (positiveMovieIds.isEmpty()) {
            return buildColdStartRecommendations(limit);
        }

        String seedMovieId = positiveMovieIds.iterator().next();

        Query embQuery = new Query(Criteria.where("_id").is(new ObjectId(seedMovieId)));
        embQuery.fields().include("plot_embedding");
        EmbeddedMovie seedMovie = mongoTemplate.findOne(embQuery, EmbeddedMovie.class);

        if (seedMovie == null || seedMovie.getPlotEmbedding() == null
                || seedMovie.getPlotEmbedding().isEmpty()) {
            log.warn("No embedding for seed movie [{}], falling back to cold start", seedMovieId);
            return buildColdStartRecommendations(limit);
        }

        var results = vectorSearchService.searchByEmbedding(seedMovie.getPlotEmbedding(), limit + 5);

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

    private RecommendationResponse buildOnboardingStarterRecommendations(String sessionId, int limit) {
        Optional<StarterRecommendationCache> cacheOptional = starterRecommendationCacheRepository
                .findTopBySessionIdOrderByGeneratedAtDesc(sessionId);

        if (cacheOptional.isEmpty()) {
            return null;
        }

        StarterRecommendationCache cache = cacheOptional.get();
        if (cache.getCandidateMovieIds() == null || cache.getCandidateMovieIds().isEmpty()) {
            return null;
        }

        Query query = new Query(Criteria.where("_id").in(
                cache.getCandidateMovieIds().stream().map(ObjectId::new).toList()));
        query.fields().exclude("plot_embedding").exclude("plot_embedding_voyage_3_large");

        List<EmbeddedMovie> movies = mongoTemplate.find(query, EmbeddedMovie.class);
        List<SearchItem> items = movies.stream()
                .limit(limit)
                .map(m -> SearchItem.builder()
                        .movie(toMovieSummary(m))
                        .score(scoreFor(cache, m.getId().toHexString()))
                        .reasons(List.of(Reason.builder()
                                .code("semantic_match_to_onboarding")
                                .label("Matches the tastes you selected")
                                .build()))
                        .build())
                .toList();

        if (items.isEmpty()) {
            return null;
        }

        return RecommendationResponse.builder()
                .items(items)
                .mode("cold_start")
                .fallbackUsed(false)
                .generatedAt(Instant.now().toString())
                .build();
    }

    private Double scoreFor(StarterRecommendationCache cache, String movieId) {
        int index = cache.getCandidateMovieIds().indexOf(movieId);
        if (index < 0 || cache.getCandidateScores() == null || index >= cache.getCandidateScores().size()) {
            return 0.0;
        }
        return cache.getCandidateScores().get(index);
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
