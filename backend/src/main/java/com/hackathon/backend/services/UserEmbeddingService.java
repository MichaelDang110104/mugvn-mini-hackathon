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

    /**
     * Computes a user's profile embedding from their event history.
     * Uses weighted average of movie embeddings with exponential time decay.
     *
     * TODO: đợi micheal làm xong phần user rồi write vào DB sau, chưa verify tính đúng sai
     *
     * @param username the username (MflixUser.name) used to identify the user in this hackathon
     */
    public void computeUserEmbedding(String username) {
        if (username == null || username.isBlank()) {
            log.debug("computeUserEmbedding skipped: no username");
            return;
        }

        List<UserEvent> events = userEventRepository.findByUserIdOrderByTimestampDesc(username);
        if (events.isEmpty()) {
            log.debug("computeUserEmbedding skipped: no events for user [{}]", username);
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
            log.debug("computeUserEmbedding skipped: no usable movie embeddings for user [{}]", username);
            return;
        }

        List<Double> userEmbedding = new ArrayList<>(weightedSum.size());
        for (double val : weightedSum) {
            userEmbedding.add(val / totalWeight);
        }

        saveUserProfile(username, userEmbedding);

        log.info("Computed user embedding for [{}]: moviesUsed={}, totalWeight={}", username, moviesUsed, totalWeight);
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

    private void saveUserProfile(String username, List<Double> embedding) {
        mongoTemplate.upsert(
                Query.query(Criteria.where("_id").is(username)),
                new Update()
                        .set("username", username)
                        .set("profileEmbedding", embedding)
                        .set("lastComputedAt", Instant.now().toString()),
                Document.class,
                "user_profiles"
        );
    }
}
