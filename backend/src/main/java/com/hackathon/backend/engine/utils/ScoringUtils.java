package com.hackathon.backend.engine.utils;

import com.hackathon.backend.engine.entities.ScoredMovie;
import com.hackathon.backend.models.RecommendationProfile;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ScoringUtils {
    public static List<ScoredMovie> aggregate(List<ScoredMovie> candidates) {
        Map<String, ScoredMovie> map = new LinkedHashMap<>();

        for (ScoredMovie movie : candidates) {
            map.merge(movie.getMovieId(), movie, (existing, incoming) ->
                    existing.withScore(
                            existing.getScore() + incoming.getScore() * 0.5
                    )
            );
        }

        return new ArrayList<>(map.values());
    }

    public static List<ScoredMovie> applyFreshness(List<ScoredMovie> candidates) {
        Instant cutoff = Instant.now().minus(30, ChronoUnit.DAYS);
        return candidates.stream()
                .map(sm -> sm.getReleasedAt() != null && sm.getReleasedAt().isAfter(cutoff)
                        ? sm.withScore(sm.getScore() * 1.15)
                        : sm)
                .toList();
    }

    public static List<ScoredMovie> applyDiversityPenalty(List<ScoredMovie> candidates) {
        Map<String, Integer> genreCount = new HashMap<>();
        List<ScoredMovie> result = new ArrayList<>();
        for (ScoredMovie sm : candidates) {
            List<String> genres = sm.getGenres() != null ? sm.getGenres() : List.of();
            int maxCount = genres.stream()
                    .mapToInt(g -> genreCount.getOrDefault(g, 0))
                    .max()
                    .orElse(0);
            double penalizedScore = maxCount >= 3
                    ? sm.getScore() * Math.pow(0.8, maxCount - 2)
                    : sm.getScore();
            result.add(sm.withScore(penalizedScore));
            genres.forEach(g -> genreCount.merge(g, 1, Integer::sum));
        }
        return result;
    }

    public static List<ScoredMovie> applyPersonalizationBoost(
            List<ScoredMovie> candidates, RecommendationProfile profile) {
        if (profile == null || profile.getTopGenres() == null || profile.getTopGenres().isEmpty()) {
            return candidates;
        }
        Set<String> topGenres = new HashSet<>(profile.getTopGenres());
        return candidates.stream()
                .map(sm -> {
                    boolean matches = sm.getGenres() != null
                            && sm.getGenres().stream().anyMatch(topGenres::contains);
                    return matches ? sm.withScore(sm.getScore() * 1.25) : sm;
                })
                .toList();
    }
}
