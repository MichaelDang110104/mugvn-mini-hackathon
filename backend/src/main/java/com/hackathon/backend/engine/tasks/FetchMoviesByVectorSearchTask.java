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
public class FetchMoviesByVectorSearchTask extends Task<RecommendationContext> {

    private final VectorSearchService vectorSearchService;
    private final Executor ioExecutor;


    public FetchMoviesByVectorSearchTask(
            VectorSearchService vectorSearchService,
            @Qualifier("ioExecutor") Executor ioExecutor
    ) {
        this.vectorSearchService = vectorSearchService;
        this.ioExecutor = ioExecutor;
    }

    @Override
    public String name() {
        return "movie_vector_search";
    }

    @Override
    public boolean shouldSkip(RecommendationContext context) {
        return context.hasQuery();
    }

    @Override
    public CompletableFuture<RecommendationContext> execute(RecommendationContext ctx) {
        return CompletableFuture.supplyAsync(() -> {
            String excludedId = ctx.getExcludedMovieIds().isEmpty() ? null : ctx.getExcludedMovieIds().getFirst();
            List<ScoredMovie> candidates = vectorSearchService.searchByQueryText(ctx.getSearchQuery(), 10)
                    .stream()
                    .map(r -> ObjectUtils.toScoredMovie(r, name()))
                    .toList();
            ctx.setCandidates(candidates);
            return ctx;
        }, ioExecutor);
    }
}
