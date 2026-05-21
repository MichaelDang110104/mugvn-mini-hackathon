package com.hackathon.backend.engine.tasks;

import com.hackathon.backend.commons.pipeline.Task;
import com.hackathon.backend.engine.entities.RecommendationContext;
import com.hackathon.backend.engine.entities.ScoredMovie;
import com.hackathon.backend.models.MovieStats;
import com.hackathon.backend.repositories.MovieStatsRepository;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

@Component
public class FetchTrendingMoviesTask extends Task<RecommendationContext> {

    private final MovieStatsRepository movieStatsRepository;
    private final Executor ioExecutor;

    public FetchTrendingMoviesTask(MovieStatsRepository movieStatsRepository,
                                   @Qualifier("ioExecutor") Executor ioExecutor) {
        this.movieStatsRepository = movieStatsRepository;
        this.ioExecutor = ioExecutor;
    }

    @Override
    public String name() {
        return "trending";
    }

    @Override
    public boolean shouldSkip(RecommendationContext ctx) {
        return false;
    }

    @Override
    public CompletableFuture<RecommendationContext> execute(RecommendationContext ctx) {
        return CompletableFuture.supplyAsync(() -> {
            int limit = ctx.getLimit() > 0 ? ctx.getLimit() : 20;

            List<ScoredMovie> trending = movieStatsRepository
                    .findAll(PageRequest.of(0, limit, Sort.by("trendingScore").descending()))
                    .stream()
                    .map(this::toScoredMovie)
                    .toList();

            ctx.addCandidates(trending);

            return ctx;
        }, ioExecutor);
    }

    private ScoredMovie toScoredMovie(MovieStats stats) {
        return ScoredMovie.builder()
                .movieId(stats.getMovieId())
                .score(stats.getTrendingScore())
                .source("trending")
                .genres(stats.getGenres())
                .releasedAt(stats.getReleasedAt())
                .build();
    }
}
