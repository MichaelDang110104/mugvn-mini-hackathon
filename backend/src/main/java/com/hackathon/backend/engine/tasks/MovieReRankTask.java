package com.hackathon.backend.engine.tasks;

import com.hackathon.backend.commons.pipeline.Task;
import com.hackathon.backend.engine.entities.RecommendationContext;
import com.hackathon.backend.engine.entities.ScoredMovie;
import com.hackathon.backend.engine.strategy.ScoringStrategyRegistry;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

@Component
public class MovieReRankTask extends Task<RecommendationContext> {

    private final ScoringStrategyRegistry registry;
    private final Executor cpuExecutor;

    public MovieReRankTask(ScoringStrategyRegistry registry,
                           @Qualifier("cpuExecutor") Executor cpuExecutor) {
        this.registry = registry;
        this.cpuExecutor = cpuExecutor;
    }

    @Override
    public String name() {
        return "re_rank";
    }

    @Override
    public boolean shouldSkip(RecommendationContext ctx) {
        return ctx.getCandidates() == null || ctx.getCandidates().isEmpty();
    }

    @Override
    public CompletableFuture<RecommendationContext> execute(RecommendationContext ctx) {
        return CompletableFuture.supplyAsync(() -> {
            List<ScoredMovie> reRanked = registry.get(ctx.getMode()).score(ctx.getCandidates(), ctx);
            ctx.setRankedCandidates(reRanked);
            return ctx;
        }, cpuExecutor);
    }
}
