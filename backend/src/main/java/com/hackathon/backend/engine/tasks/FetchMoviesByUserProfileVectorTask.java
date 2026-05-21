package com.hackathon.backend.engine.tasks;

import com.hackathon.backend.commons.pipeline.Task;
import com.hackathon.backend.engine.entities.RecommendationContext;
import com.hackathon.backend.engine.entities.ScoredMovie;
import com.hackathon.backend.engine.utils.ObjectUtils;
import com.hackathon.backend.services.VectorSearchService;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

@Component
public class FetchMoviesByUserProfileVectorTask extends Task<RecommendationContext> {

    private static final String CANDIDATE_GROUP = "user_profile_vector";

    private final VectorSearchService vectorSearchService;
    private final Executor ioExecutor;

    public FetchMoviesByUserProfileVectorTask(
            VectorSearchService vectorSearchService,
            @Qualifier("ioExecutor") Executor ioExecutor
    ) {
        this.vectorSearchService = vectorSearchService;
        this.ioExecutor = ioExecutor;
    }

    @Override
    public String name() {
        return CANDIDATE_GROUP;
    }

    @Override
    public boolean shouldSkip(RecommendationContext ctx) {
        return ctx.getUserProfileEmbedding() == null || ctx.getUserProfileEmbedding().isEmpty();
    }

    @Override
    public CompletableFuture<RecommendationContext> execute(RecommendationContext ctx) {
        return CompletableFuture.supplyAsync(() -> {
            int limit = ctx.getLimit() > 0 ? ctx.getLimit() : 10;
            List<ScoredMovie> candidates = vectorSearchService.searchByEmbedding(ctx.getUserProfileEmbedding(), limit)
                    .stream()
                    .map(result -> ObjectUtils.toScoredMovie(result, name()))
                    .toList();
            ctx.putCandidateGroup(CANDIDATE_GROUP, candidates);
            return ctx;
        }, ioExecutor);
    }
}
