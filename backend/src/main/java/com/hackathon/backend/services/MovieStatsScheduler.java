package com.hackathon.backend.services;

import com.hackathon.backend.models.Movie;
import com.hackathon.backend.models.MovieStats;
import com.hackathon.backend.repositories.MovieRepository;
import com.hackathon.backend.repositories.MovieStatsRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;
import org.springframework.data.mongodb.core.aggregation.Fields;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.springframework.data.mongodb.core.aggregation.Aggregation.*;
import static org.springframework.data.mongodb.core.query.Criteria.where;

@Slf4j
@Component
@RequiredArgsConstructor
public class MovieStatsScheduler {

    private final MongoTemplate mongoTemplate;
    private final MovieStatsRepository movieStatsRepository;
    private final MovieRepository movieRepository;
    private final TrendingScoreCalculator calculator;

    @Scheduled(cron = "5 5 * * * *")
    public void buildMovieStats() {
        log.info("Starting movie stats rebuild");
        Instant cutoff7d = Instant.now().minus(7, ChronoUnit.DAYS);

        Map<String, Map<String, Long>> allTimeCounts = aggregateEventCounts(null);
        Map<String, Map<String, Long>> sevenDayCounts = aggregateEventCounts(cutoff7d);

        Set<String> movieIds = allTimeCounts.keySet();
        if (movieIds.isEmpty()) {
            log.info("No movie events found, skipping stats rebuild");
            return;
        }

        Map<String, MovieStats> existingStats = movieStatsRepository.findAllById(movieIds)
                .stream().collect(Collectors.toMap(MovieStats::getMovieId, Function.identity()));

        List<ObjectId> objectIds = movieIds.stream()
                .flatMap(id -> {
                    try { return Stream.of(new ObjectId(id)); }
                    catch (IllegalArgumentException e) { return Stream.empty(); }
                }).collect(Collectors.toList());
        Map<String, Movie> movieMap = movieRepository.findAllById(objectIds)
                .stream().collect(Collectors.toMap(m -> m.getId().toHexString(), Function.identity()));

        int updated = 0;
        for (String movieId : movieIds) {
            try {
                MovieStats stats = buildStats(
                        movieId,
                        allTimeCounts.get(movieId),
                        sevenDayCounts.getOrDefault(movieId, Map.of()),
                        existingStats.get(movieId),
                        movieMap.get(movieId));
                movieStatsRepository.save(stats);
                updated++;
            } catch (Exception e) {
                log.warn("Failed to build stats for movie [{}]: {}", movieId, e.getMessage());
            }
        }
        log.info("Movie stats rebuild complete: {}/{} movies updated", updated, movieIds.size());
    }

    private MovieStats buildStats(String movieId,
                                   Map<String, Long> allTime,
                                   Map<String, Long> sevenDay,
                                   MovieStats existing,
                                   Movie movie) {
        long viewCount           = allTime.getOrDefault("VIEW", 0L);
        long likeCount           = allTime.getOrDefault("LIKE", 0L);
        long watchCompleteCount  = allTime.getOrDefault("WATCH_START", 0L);
        long viewCount7d         = sevenDay.getOrDefault("VIEW", 0L);
        long likeCount7d         = sevenDay.getOrDefault("LIKE", 0L);
        long watchCompleteCount7d = sevenDay.getOrDefault("WATCH_START", 0L);

        Instant firstTrendingAt = existing != null ? existing.getFirstTrendingAt() : Instant.now();

        List<String> genres = movie != null ? movie.getGenres()
                : existing != null ? existing.getGenres() : null;
        Instant releasedAt = movie != null && movie.getReleased() != null
                ? movie.getReleased().toInstant()
                : existing != null ? existing.getReleasedAt() : null;

        MovieStats.MovieStatsBuilder builder = MovieStats.builder()
                .movieId(movieId)
                .viewCount(viewCount)
                .likeCount(likeCount)
                .watchCompleteCount(watchCompleteCount)
                .viewCount7d(viewCount7d)
                .likeCount7d(likeCount7d)
                .watchCompleteCount7d(watchCompleteCount7d)
                .genres(genres)
                .releasedAt(releasedAt)
                .firstTrendingAt(firstTrendingAt);

        double wilsonScore   = calculator.wilsonScore(likeCount + watchCompleteCount, viewCount);
        double decayScore    = calculator.decayScore(viewCount7d + likeCount7d * 3L, firstTrendingAt);
        double trendingScore = calculator.combined(builder.build());

        return builder
                .wilsonScore(wilsonScore)
                .decayScore(decayScore)
                .trendingScore(trendingScore)
                .build();
    }

    private Map<String, Map<String, Long>> aggregateEventCounts(Instant since) {
        Criteria criteria = where("movieId").exists(true).and("movieId").ne(null);
        if (since != null) {
            criteria = criteria.and("timestamp").gte(since);
        }

        Aggregation agg = newAggregation(
                match(criteria),
                group(Fields.from(
                        Fields.field("movieId", "movieId"),
                        Fields.field("eventType", "eventType")
                )).count().as("count")
        );

        AggregationResults<Document> results = mongoTemplate.aggregate(agg, "user_events", Document.class);

        Map<String, Map<String, Long>> byMovie = new HashMap<>();
        for (Document doc : results.getMappedResults()) {
            Document id = (Document) doc.get("_id");
            if (id == null) continue;
            String movieId = id.getString("movieId");
            String eventType = id.getString("eventType");
            if (movieId == null || eventType == null) continue;
            long count = ((Number) doc.get("count")).longValue();
            byMovie.computeIfAbsent(movieId, k -> new HashMap<>()).put(eventType, count);
        }
        return byMovie;
    }
}
