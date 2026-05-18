package com.hackathon.backend.services;

import com.hackathon.backend.enums.EventType;
import com.hackathon.backend.models.EmbeddedMovie;
import com.hackathon.backend.models.UserEvent;
import com.hackathon.backend.repositories.EmbeddedMovieRepository;
import com.hackathon.backend.repositories.UserEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bson.types.ObjectId;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserEmbeddingService {

    private final UserEventRepository userEventRepository;
    private final EmbeddedMovieRepository embeddedMovieRepository;
    private final MongoTemplate mongoTemplate;

    private static final String USER_PROFILES_COLLECTION = "user_profiles";
    private static final int USER_PROFILE_EVENT_LIMIT = 100;
    private static final double USER_PROFILE_LAMBDA = Math.log(2) / 21.0;

    public List<Double> createUserEmbedding(String userId) {
        if (userId == null || userId.isBlank()) {
            log.warn("createUserEmbedding called with null or blank userId");
            return List.of();
        }

        List<UserEvent> events = userEventRepository.findByUserIdOrderByTimestampDesc(
                userId, PageRequest.of(0, USER_PROFILE_EVENT_LIMIT));

        if (events.isEmpty()) {
            log.info("No events found for user [{}], skipping embedding computation", userId);
            return List.of();
        }

        Map<String, List<Double>> movieEmbeddings = loadMovieEmbeddings(events);
        if (movieEmbeddings.isEmpty()) {
            log.info("No movie embeddings found for user [{}]'s events", userId);
            return List.of();
        }

        List<Double> userEmbedding = computeWeightedAverage(events, movieEmbeddings);
        if (userEmbedding.isEmpty()) {
            log.warn("Weighted average computation returned empty for user [{}]", userId);
            return List.of();
        }

        saveUserProfile(userId, userEmbedding, events);

        log.info("Created user embedding for user [{}] (dim={}, events={})",
                userId, userEmbedding.size(), events.size());

        return userEmbedding;
    }

    private Map<String, List<Double>> loadMovieEmbeddings(List<UserEvent> events) {
        List<ObjectId> movieIds = events.stream()
                .map(UserEvent::getMovieId)
                .filter(id -> id != null && !id.isBlank())
                .map(ObjectId::new)
                .distinct()
                .toList();

        if (movieIds.isEmpty()) {
            return Map.of();
        }

        List<EmbeddedMovie> movies = embeddedMovieRepository.findAllById(movieIds);

        Map<String, List<Double>> embeddingMap = new HashMap<>();
        for (EmbeddedMovie movie : movies) {
            if (movie.getPlotEmbedding() != null && !movie.getPlotEmbedding().isEmpty()) {
                embeddingMap.put(movie.getId().toHexString(), movie.getPlotEmbedding());
            }
        }

        return embeddingMap;
    }

    private List<Double> computeWeightedAverage(
            List<UserEvent> events,
            Map<String, List<Double>> movieEmbeddings) {

        Instant now = Instant.now();
        double[] weightedSum = null;
        double totalWeight = 0.0;
        int embeddingDim = 0;

        for (UserEvent event : events) {
            if (event.getMovieId() == null || event.getMovieId().isBlank()) {
                continue;
            }

            List<Double> movieEmbedding = movieEmbeddings.get(event.getMovieId());
            if (movieEmbedding == null || movieEmbedding.isEmpty()) {
                continue;
            }

            if (embeddingDim == 0) {
                embeddingDim = movieEmbedding.size();
                weightedSum = new double[embeddingDim];
            }

            double baseWeight = getEventWeight(event);
            double decay = computeDecay(event.getTimestamp(), now);
            double effectiveWeight = baseWeight * decay;

            for (int i = 0; i < embeddingDim; i++) {
                weightedSum[i] += effectiveWeight * movieEmbedding.get(i);
            }

            totalWeight += effectiveWeight;
        }

        if (weightedSum == null || totalWeight == 0.0) {
            return List.of();
        }

        List<Double> result = new ArrayList<>(embeddingDim);
        for (int i = 0; i < embeddingDim; i++) {
            result.add(weightedSum[i] / totalWeight);
        }

        return result;
    }

    private double getEventWeight(UserEvent event) {
        EventType eventType = event.getEventType();
        if (eventType == null) {
            return 0.0;
        }

        double baseWeight = eventType.getWeight() / 10.0;

        if (eventType == EventType.RATING && event.getEventValue() != null) {
            if (event.getEventValue() < 4) {
                return 0.0;
            }
        }

        return baseWeight;
    }

    private double computeDecay(Instant eventTimestamp, Instant now) {
        if (eventTimestamp == null) {
            return 1.0;
        }

        double secondsSinceEvent = (double) (now.getEpochSecond() - eventTimestamp.getEpochSecond());
        double daysSinceEvent = secondsSinceEvent / (24.0 * 60.0 * 60.0);

        return Math.exp(-USER_PROFILE_LAMBDA * daysSinceEvent);
    }

    private void saveUserProfile(String userId, List<Double> embedding, List<UserEvent> events) {
        Query query = new Query(Criteria.where("userId").is(userId));

        List<String> recentMovieIds = events.stream()
                .map(UserEvent::getMovieId)
                .filter(id -> id != null && !id.isBlank())
                .distinct()
                .limit(20)
                .toList();

        List<String> likedMovieIds = events.stream()
                .filter(e -> e.getEventType() == EventType.LIKE
                        || e.getEventType() == EventType.SAVE)
                .map(UserEvent::getMovieId)
                .filter(id -> id != null && !id.isBlank())
                .distinct()
                .toList();

        Update update = new Update()
                .set("userId", userId)
                .set("profileEmbedding", embedding)
                .set("recentMovieIds", recentMovieIds)
                .set("likedMovieIds", likedMovieIds)
                .set("lastComputedAt", Instant.now())
                .set("eventCount", events.size());

        mongoTemplate.upsert(query, update, USER_PROFILES_COLLECTION);
    }
}
