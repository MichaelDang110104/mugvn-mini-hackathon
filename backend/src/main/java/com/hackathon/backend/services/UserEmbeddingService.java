package com.hackathon.backend.services;

import com.hackathon.backend.enums.EventType;
import com.hackathon.backend.models.EmbeddedMovie;
import com.hackathon.backend.models.UserEvent;
import com.hackathon.backend.repositories.EmbeddedMovieRepository;
import com.hackathon.backend.repositories.UserEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserEmbeddingService {

    private final UserEventRepository userEventRepository;
    private final EmbeddedMovieRepository embeddedMovieRepository;
    private final MongoTemplate mongoTemplate;

    private static final int MAX_EVENTS = 100;
    private static final double HALF_LIFE_DAYS = 21.0;
    private static final double LAMBDA = Math.log(2) / HALF_LIFE_DAYS;

    public void computeUserEmbedding(String userId) {
        if (userId == null || userId.isBlank()) {
            log.debug("computeUserEmbedding skipped: no userId");
            return;
        }

        List<UserEvent> events = userEventRepository.findByUserIdOrderByTimestampDesc(userId);
        if (events.isEmpty()) {
            log.debug("computeUserEmbedding skipped: no events for user [{}]", userId);
            return;
        }

        List<UserEvent> cappedEvents = events.stream()
                .limit(MAX_EVENTS)
                .toList();

        List<Double> weightedSum = null;
        double totalWeight = 0.0;
        int moviesUsed = 0;

        Instant now = Instant.now();

        for (UserEvent event : cappedEvents) {
            if (event.getMovieId() == null || event.getMovieId().isBlank()) {
                continue;
            }

            double eventWeight = getEmbeddingWeight(event);
            if (eventWeight <= 0) {
                continue;
            }

            double decay = computeDecay(event.getTimestamp(), now);
            double effectiveWeight = eventWeight * decay;

            EmbeddedMovie movie = embeddedMovieRepository.findById(
                    new ObjectId(event.getMovieId())).orElse(null);
            if (movie == null || movie.getPlotEmbedding() == null || movie.getPlotEmbedding().isEmpty()) {
                continue;
            }

            List<Double> embedding = movie.getPlotEmbedding();

            if (weightedSum == null) {
                weightedSum = new ArrayList<>(embedding.size());
                for (double val : embedding) {
                    weightedSum.add(val * effectiveWeight);
                }
            } else {
                for (int i = 0; i < embedding.size(); i++) {
                    weightedSum.set(i, weightedSum.get(i) + embedding.get(i) * effectiveWeight);
                }
            }

            totalWeight += effectiveWeight;
            moviesUsed++;
        }

        if (weightedSum == null || totalWeight == 0) {
            log.debug("computeUserEmbedding skipped: no usable movie embeddings for user [{}]", userId);
            return;
        }

        List<Double> userEmbedding = new ArrayList<>(weightedSum.size());
        for (double val : weightedSum) {
            userEmbedding.add(val / totalWeight);
        }

        saveUserProfile(userId, userEmbedding, moviesUsed);

        log.info("Computed user embedding for [{}]: moviesUsed={}, totalWeight={}", userId, moviesUsed, totalWeight);
    }

    private double getEmbeddingWeight(UserEvent event) {
        EventType type = event.getEventType();

        if (type == EventType.RATING) {
            return (event.getEventValue() != null && event.getEventValue() >= 4) ? type.getWeight() : 0.0;
        }

        return type.getWeight();
    }

    private double computeDecay(Instant eventTimestamp, Instant now) {
        if (eventTimestamp == null) {
            return 1.0;
        }
        double secondsSinceEvent = (double) (now.getEpochSecond() - eventTimestamp.getEpochSecond());
        double daysSinceEvent = secondsSinceEvent / 86400.0;
        return Math.exp(-LAMBDA * daysSinceEvent);
    }

    private void saveUserProfile(String userId, List<Double> embedding, int sourceEventCount) {
        mongoTemplate.upsert(
                Query.query(Criteria.where("_id").is(userId)),
                new Update()
                        .set("userId", userId)
                        .set("profileEmbedding", embedding)
                        .set("lastComputedAt", Instant.now().toString())
                        .set("sourceEventCount", sourceEventCount),
                Document.class,
                "user_profiles"
        );
    }
}
