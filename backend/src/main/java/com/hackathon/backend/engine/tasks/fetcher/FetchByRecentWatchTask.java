package com.hackathon.backend.engine.tasks.fetcher;

import com.hackathon.backend.engine.entities.EngineMode;
import com.hackathon.backend.engine.entities.RecommendationContext;
import com.hackathon.backend.engine.entities.ScoredMovie;
import com.hackathon.backend.engine.tasks.RecommendationTaskBase;
import com.hackathon.backend.enums.EventType;
import com.hackathon.backend.models.UserEvent;
import com.hackathon.backend.repositories.UserEventRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

@Slf4j
@Component
public class FetchByRecentWatchTask extends RecommendationTaskBase {

    private final UserEventRepository userEventRepository;
    private final Executor ioExecutor;

    public FetchByRecentWatchTask(UserEventRepository userEventRepository,
                                  @Qualifier("ioExecutor") Executor ioExecutor) {
        this.userEventRepository = userEventRepository;
        this.ioExecutor = ioExecutor;
    }

    @Override
    public String name() {
        return "recent_watch";
    }

    @Override
    protected Set<EngineMode> supportedModes() {
        return Set.of(EngineMode.RECENT_WATCH);
    }

    @Override
    public boolean shouldSkip(RecommendationContext ctx) {
        return super.shouldSkip(ctx) || !ctx.isAuthenticated();
    }

    @Override
    public CompletableFuture<RecommendationContext> execute(RecommendationContext ctx) {
        int limit = ctx.getLimit() > 0 ? ctx.getLimit() : 20;
        log.info("[FetchByRecentWatchTask] userId={} mode={} limit={}", ctx.getUserId(), ctx.getMode(), limit);

        return CompletableFuture.supplyAsync(() -> {
            List<UserEvent> events = userEventRepository
                    .findByUserIdAndEventTypeOrderByTimestampDesc(
                            ctx.getUserId(), EventType.WATCH_START, PageRequest.of(0, limit * 5));

            // Dedup by movieId — events are ordered by timestamp desc, so keep the latest per movie.
            // No watch-progress signal is captured for watch_start events, so we rank purely by recency.
            Map<String, UserEvent> deduped = new LinkedHashMap<>();
            for (UserEvent event : events) {
                if (event.getMovieId() != null) {
                    deduped.putIfAbsent(event.getMovieId(), event);
                }
            }

            // Score each unique event by recency only
            List<ScoredMovie> candidates = new ArrayList<>(deduped.size());
            Instant now = Instant.now();
            for (UserEvent event : deduped.values()) {
                long hoursAgo = event.getTimestamp() != null
                        ? Duration.between(event.getTimestamp(), now).toHours()
                        : 168L;
                double recencyScore = Math.max(0.0, 1.0 - (hoursAgo / 168.0));

                candidates.add(ScoredMovie.builder()
                        .movieId(event.getMovieId())
                        .score(recencyScore)
                        .source("recent_watch")
                        .build());
            }

            // Sort descending by score, limit to ctx.getLimit()
            candidates.sort(Comparator.comparingDouble(ScoredMovie::getScore).reversed());
            if (candidates.size() > limit) {
                candidates = candidates.subList(0, limit);
            }

            log.info("[FetchByRecentWatchTask] done — {} candidates", candidates.size());
            ctx.addCandidates(candidates);
            return ctx;
        }, ioExecutor);
    }
}
