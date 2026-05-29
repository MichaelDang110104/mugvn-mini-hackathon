package com.hackathon.backend.engine.tasks.fetcher;

import com.hackathon.backend.engine.entities.EngineMode;
import com.hackathon.backend.engine.entities.RecommendationContext;
import com.hackathon.backend.engine.entities.ScoredMovie;
import com.hackathon.backend.engine.tasks.RecommendationTaskBase;
import com.hackathon.backend.engine.utils.ObjectUtils;
import com.hackathon.backend.services.VectorSearchService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

@Slf4j
@Component
public class FetchByUserVectorTask extends RecommendationTaskBase {

    private static final double USER_VECTOR_MIN_SCORE = 0.75;
    private static final int USER_VECTOR_FETCH_LIMIT = 100;

    private final VectorSearchService vectorSearchService;
    private final Executor ioExecutor;

    public FetchByUserVectorTask(VectorSearchService vectorSearchService,
                                 @Qualifier("ioExecutor") Executor ioExecutor) {
        this.vectorSearchService = vectorSearchService;
        this.ioExecutor = ioExecutor;
    }

    @Override
    public String name() {
        return "user_profile_vector";
    }

    @Override
    protected Set<EngineMode> supportedModes() {
        return Set.of(EngineMode.SEARCH, EngineMode.GENRE, EngineMode.SIMILAR_TO_MOVIE, EngineMode.USER_RECOMMEND);
    }

    @Override
    public boolean shouldSkip(RecommendationContext ctx) {
        return super.shouldSkip(ctx) || ctx.getUserProfileEmbedding() == null || ctx.getUserProfileEmbedding().isEmpty();
    }

    @Override
    public CompletableFuture<RecommendationContext> execute(RecommendationContext ctx) {
        log.info("[FetchByUserVectorTask] userId={} mode={} limit={}", ctx.getUserId(), ctx.getMode(), ctx.getLimit());

        return CompletableFuture.supplyAsync(() -> {
            List<String> excludedMovieIds = ctx.getExcludedMovieIds() != null ? ctx.getExcludedMovieIds() : List.of();

            List<ScoredMovie> candidates = vectorSearchService.searchByEmbedding(ctx.getUserProfileEmbedding(), USER_VECTOR_FETCH_LIMIT)
                    .stream()
                    .filter(result -> result.getVectorSearchScore() >= USER_VECTOR_MIN_SCORE)
                    .filter(result -> result.getMovie() != null && result.getMovie().getId() != null)
                    .filter(result -> !excludedMovieIds.contains(result.getMovie().getId().toHexString()))
                    .map(result -> ObjectUtils.toScoredMovie(result, "content_based"))
                    .limit(ctx.getLimit() > 0 ? ctx.getLimit() : 10)
                    .toList();
            log.info("[FetchByUserVectorTask] done — {} candidates", candidates.size());
            ctx.addCandidates(candidates);
            return ctx;
        }, ioExecutor);
    }
}
