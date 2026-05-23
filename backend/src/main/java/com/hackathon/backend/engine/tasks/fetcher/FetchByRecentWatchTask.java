package com.hackathon.backend.engine.tasks.fetcher;

import com.hackathon.backend.engine.entities.EngineMode;
import com.hackathon.backend.engine.entities.RecommendationContext;
import com.hackathon.backend.engine.entities.ScoredMovie;
import com.hackathon.backend.engine.tasks.RecommendationTaskBase;
import com.hackathon.backend.enums.EventType;
import com.hackathon.backend.models.UserEvent;
import com.hackathon.backend.repositories.UserEventRepository;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

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
        return super.shouldSkip(ctx) || ctx.getUserId() == null || ctx.getUserId().isBlank();
    }

    @Override
    public CompletableFuture<RecommendationContext> execute(RecommendationContext ctx) {
        return CompletableFuture.supplyAsync(() -> {
            int limit = ctx.getLimit() > 0 ? ctx.getLimit() : 20;

            List<UserEvent> events = userEventRepository
                    .findByUserIdAndEventTypeOrderByTimestampDesc(
                            ctx.getUserId(), EventType.WATCH_START, PageRequest.of(0, limit));

            List<ScoredMovie> candidates = new ArrayList<>(events.size());
            for (int i = 0; i < events.size(); i++) {
                UserEvent event = events.get(i);
                if (event.getMovieId() == null) continue;
                candidates.add(ScoredMovie.builder()
                        .movieId(event.getMovieId())
                        .score(1.0 / (i + 1))
                        .source("recent_watch")
                        .build());
            }

            ctx.addCandidates(candidates);
            return ctx;
        }, ioExecutor);
    }
}
