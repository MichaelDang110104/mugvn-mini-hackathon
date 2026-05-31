package com.hackathon.backend.engine.tasks.loader;

import com.hackathon.backend.engine.entities.RecommendationContext;
import com.hackathon.backend.engine.tasks.RecommendationTaskBase;
import com.hackathon.backend.enums.EventType;
import com.hackathon.backend.models.UserEvent;
import com.hackathon.backend.repositories.UserEventRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.CompletableFuture;

@Slf4j
@Component
public class LoadSeenMoviesTask extends RecommendationTaskBase {

    private final UserEventRepository userEventRepository;

    public LoadSeenMoviesTask(UserEventRepository userEventRepository) {
        this.userEventRepository = userEventRepository;
    }

    @Override
    public String name() {
        return "load_seen_movies";
    }

    @Override
    public boolean shouldSkip(RecommendationContext ctx) {
        return super.shouldSkip(ctx) || ctx.getUserId() == null || ctx.getUserId().isBlank();
    }

    private static final List<EventType> SEEN_EVENT_TYPES = List.of(
            EventType.WATCH_START
    );

    @Override
    public CompletableFuture<RecommendationContext> execute(RecommendationContext ctx) {
        log.info("[LoadSeenMoviesTask] userId={} mode={}", ctx.getUserId(), ctx.getMode());

        List<String> seenMovieIds = userEventRepository
                .findSeenMovieEventsByUserId(ctx.getUserId(), SEEN_EVENT_TYPES)
                .stream()
                .map(UserEvent::getMovieId)
                .distinct()
                .toList();

        log.info("[LoadSeenMoviesTask] done — {} seen movieIds", seenMovieIds.size());
        ctx.getExcludedMovieIds().addAll(seenMovieIds);
        return CompletableFuture.completedFuture(ctx);
    }
}
