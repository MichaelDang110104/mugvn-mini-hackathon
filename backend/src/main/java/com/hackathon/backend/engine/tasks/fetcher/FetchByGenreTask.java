package com.hackathon.backend.engine.tasks.fetcher;

import com.hackathon.backend.engine.entities.EngineMode;
import com.hackathon.backend.engine.entities.RecommendationContext;
import com.hackathon.backend.engine.entities.ScoredMovie;
import com.hackathon.backend.engine.tasks.RecommendationTaskBase;
import com.hackathon.backend.engine.utils.ObjectUtils;
import com.hackathon.backend.repositories.EmbeddedMovieRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

@Slf4j
@Component
public class FetchByGenreTask extends RecommendationTaskBase {

    private final EmbeddedMovieRepository embeddedMovieRepository;
    private final Executor ioExecutor;

    public FetchByGenreTask(EmbeddedMovieRepository embeddedMovieRepository,
                            @Qualifier("ioExecutor") Executor ioExecutor) {
        this.embeddedMovieRepository = embeddedMovieRepository;
        this.ioExecutor = ioExecutor;
    }

    @Override
    public String name() {
        return "genre";
    }

    @Override
    protected Set<EngineMode> supportedModes() {
        return Set.of(EngineMode.GENRE, EngineMode.TRENDING, EngineMode.USER_RECOMMEND);
    }

    @Override
    public boolean shouldSkip(RecommendationContext ctx) {
        if (super.shouldSkip(ctx)) return true;
        return resolveGenres(ctx).isEmpty();
    }

    private List<String> resolveGenres(RecommendationContext ctx) {
        if (ctx.getGenre() != null && !ctx.getGenre().isBlank()) {
            return List.of(ctx.getGenre());
        }
        if (ctx.getProfile() != null
                && ctx.getProfile().getTopGenres() != null
                && !ctx.getProfile().getTopGenres().isEmpty()) {
            return ctx.getProfile().getTopGenres();
        }
        return List.of();
    }

    @Override
    public CompletableFuture<RecommendationContext> execute(RecommendationContext ctx) {
        int limit = ctx.getLimit() > 0 ? ctx.getLimit() : 20;

        List<String> genres = resolveGenres(ctx);

        log.info("[FetchByGenreTask] userId={} mode={} genres={} limit={}",
                ctx.getUserId(), ctx.getMode(), genres, limit);

        return CompletableFuture.supplyAsync(() -> {
            List<ScoredMovie> candidates = embeddedMovieRepository
                    .findByGenresIn(genres, PageRequest.of(0, limit))
                    .stream()
                    .map(movie -> ObjectUtils.toScoredMovie(movie, "genre"))
                    .toList();
            log.info("[FetchByGenreTask] fetched {} candidates for genres={}", candidates.size(), genres);
            ctx.addCandidates(candidates);
            return ctx;
        }, ioExecutor);
    }
}
