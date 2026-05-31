package com.hackathon.backend.engine.tasks.ranker;

import com.hackathon.backend.engine.entities.RecommendationContext;
import com.hackathon.backend.engine.entities.ScoredMovie;
import com.hackathon.backend.engine.strategy.ScoringStrategyRegistry;
import com.hackathon.backend.engine.tasks.RecommendationTaskBase;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

@Slf4j
@Component
public class MovieReRankTask extends RecommendationTaskBase {

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
        return super.shouldSkip(ctx) || ctx.getCandidates() == null || ctx.getCandidates().isEmpty();
    }

    @Override
    public CompletableFuture<RecommendationContext> execute(RecommendationContext ctx) {
        log.info("[MovieReRankTask] userId={} mode={} candidates={}",
                ctx.getUserId(), ctx.getMode(), ctx.getCandidates().size());

        return CompletableFuture.supplyAsync(() -> {
            List<ScoredMovie> reRanked = registry.get(ctx.getMode()).score(ctx.getCandidates(), ctx);
            log.info("[MovieReRankTask] done — {} ranked candidates", reRanked.size());
            ctx.setRankedCandidates(reRanked);
            return ctx;
        }, cpuExecutor);
    }
}
