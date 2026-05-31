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
public class FetchBySemanticSearchTask extends RecommendationTaskBase {

    private final VectorSearchService vectorSearchService;
    private final Executor ioExecutor;

    public FetchBySemanticSearchTask(VectorSearchService vectorSearchService,
                                     @Qualifier("ioExecutor") Executor ioExecutor) {
        this.vectorSearchService = vectorSearchService;
        this.ioExecutor = ioExecutor;
    }

    @Override
    public String name() {
        return "semantic_search";
    }

    @Override
    protected Set<EngineMode> supportedModes() {
        return Set.of(EngineMode.SEARCH);
    }

    @Override
    public boolean shouldSkip(RecommendationContext ctx) {
        return super.shouldSkip(ctx) || ctx.hasQuery();
    }

    @Override
    public CompletableFuture<RecommendationContext> execute(RecommendationContext ctx) {
        int limit = ctx.getLimit() > 0 ? ctx.getLimit() : 10;
        log.info("[FetchBySemanticSearchTask] userId={} mode={} query='{}' limit={}",
                ctx.getUserId(), ctx.getMode(), ctx.getSearchQuery(), limit);

        return CompletableFuture.supplyAsync(() -> {
            List<ScoredMovie> candidates = vectorSearchService.searchByQueryText(ctx.getSearchQuery(), limit)
                    .stream()
                    .map(r -> ObjectUtils.toScoredMovie(r, "semantic_search"))
                    .toList();
            log.info("[FetchBySemanticSearchTask] done — {} candidates", candidates.size());
            ctx.addCandidates(candidates);
            return ctx;
        }, ioExecutor);
    }
}
