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
public class SemanticSearchTask extends Task<RecommendationContext> {

    private final VectorSearchService vectorSearchService;
    private final Executor ioExecutor;

    public SemanticSearchTask(VectorSearchService vectorSearchService,
                              @Qualifier("ioExecutor") Executor ioExecutor) {
        this.vectorSearchService = vectorSearchService;
        this.ioExecutor = ioExecutor;
    }

    @Override
    public String name() {
        return "semantic_search";
    }

    @Override
    public boolean shouldSkip(RecommendationContext ctx) {
        return !ctx.hasQuery();
    }

    @Override
    public CompletableFuture<RecommendationContext> execute(RecommendationContext ctx) {
        return CompletableFuture.supplyAsync(() -> {
            int limit = ctx.getLimit() > 0 ? ctx.getLimit() : 10;
            List<ScoredMovie> candidates = vectorSearchService.searchByQueryText(ctx.getSearchQuery(), limit)
                    .stream()
                    .map(r -> ObjectUtils.toScoredMovie(r, "semantic_search"))
                    .toList();
            ctx.addCandidates(candidates);
            return ctx;
        }, ioExecutor);
    }
}
