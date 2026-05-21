package com.hackathon.backend.services;

import com.hackathon.backend.models.MovieStats;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Component
public class TrendingScoreCalculator {

    private static final double GRAVITY       = 1.8;
    private static final double WILSON_WEIGHT = 0.4;
    private static final double DECAY_WEIGHT  = 0.6;

    public double wilsonScore(long positive, long total) {
        if (total == 0) return 0;

        double p = (double) positive / total;
        double z = 1.96;

        return (p + z*z/(2*total)
                - z * Math.sqrt((p*(1-p) + z*z/(4*total)) / total))
                / (1 + z*z/total);
    }

    public double decayScore(long interactions, Instant firstTrendingAt) {
        if (interactions == 0) return 0;

        double ageHours = Duration.between(firstTrendingAt, Instant.now()).toHours();
        return interactions / Math.pow(ageHours + 2, GRAVITY);
    }

    public double combined(MovieStats movieStats) {
        double wilson = wilsonScore(
                movieStats.getLikeCount() + movieStats.getWatchCompleteCount(),
                movieStats.getViewCount()
        );

        double decay = decayScore(
                movieStats.getViewCount7d() + movieStats.getLikeCount7d() * 3,
                movieStats.getFirstTrendingAt() != null
                        ? movieStats.getFirstTrendingAt()
                        : Instant.now().minus(7, ChronoUnit.DAYS)
        );

        return wilson * WILSON_WEIGHT + decay * DECAY_WEIGHT;
    }
}