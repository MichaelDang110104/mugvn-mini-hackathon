package com.hackathon.backend.engine.tasks.fetcher;

import com.hackathon.backend.engine.entities.EngineMode;
import com.hackathon.backend.engine.entities.RecommendationContext;
import com.hackathon.backend.engine.entities.ScoredMovie;
import com.hackathon.backend.engine.tasks.RecommendationTaskBase;
import com.hackathon.backend.engine.utils.ObjectUtils;
import com.hackathon.backend.repositories.MovieStatsRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

@Slf4j
@Component
public class FetchTrendingTask extends RecommendationTaskBase {

    private final MovieStatsRepository movieStatsRepository;
    private final Executor ioExecutor;

    public FetchTrendingTask(MovieStatsRepository movieStatsRepository,
                             @Qualifier("ioExecutor") Executor ioExecutor) {
        this.movieStatsRepository = movieStatsRepository;
        this.ioExecutor = ioExecutor;
    }

    @Override
    public String name() {
        return "trending";
    }

    @Override
    protected Set<EngineMode> supportedModes() {
        return Set.of(EngineMode.TRENDING, EngineMode.GENRE, EngineMode.USER_RECOMMEND);
    }

    @Override
    public CompletableFuture<RecommendationContext> execute(RecommendationContext ctx) {
        int limit = ctx.getLimit() > 0 ? ctx.getLimit() : 20;
        log.info("[FetchTrendingTask] userId={} mode={} limit={}", ctx.getUserId(), ctx.getMode(), limit);

        return CompletableFuture.supplyAsync(() -> {
            List<ScoredMovie> trending = movieStatsRepository
                    .findAll(PageRequest.of(0, limit, Sort.by("trendingScore").descending()))
                    .stream()
                    .map(ObjectUtils::toScoredMovie)
                    .toList();

            log.info("[FetchTrendingTask] done — {} candidates", trending.size());
            ctx.addCandidates(trending);
            return ctx;
        }, ioExecutor);
    }
}
