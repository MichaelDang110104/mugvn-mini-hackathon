package com.hackathon.backend.engine.tasks.fetcher;

import com.hackathon.backend.engine.entities.EngineMode;
import com.hackathon.backend.engine.entities.RecommendationContext;
import com.hackathon.backend.engine.entities.ScoredMovie;
import com.hackathon.backend.engine.tasks.RecommendationTaskBase;
import com.hackathon.backend.engine.utils.ObjectUtils;
import com.hackathon.backend.repositories.EmbeddedMovieRepository;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

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
        return Set.of(EngineMode.GENRE);
    }

    @Override
    public boolean shouldSkip(RecommendationContext ctx) {
        if (super.shouldSkip(ctx)) return true;
        boolean hasExplicitGenre = ctx.getGenre() != null && !ctx.getGenre().isBlank();
        boolean hasTopGenres = ctx.getProfile() != null
                && ctx.getProfile().getTopGenres() != null
                && !ctx.getProfile().getTopGenres().isEmpty();
        return !hasExplicitGenre && !hasTopGenres;
    }

    @Override
    public CompletableFuture<RecommendationContext> execute(RecommendationContext ctx) {
        int limit = ctx.getLimit() > 0 ? ctx.getLimit() : 20;

        List<String> genres = (ctx.getGenre() != null && !ctx.getGenre().isBlank())
                ? List.of(ctx.getGenre())
                : ctx.getProfile().getTopGenres();

        return CompletableFuture.supplyAsync(() -> {
            List<ScoredMovie> candidates = embeddedMovieRepository
                    .findByGenresIn(genres, PageRequest.of(0, limit))
                    .stream()
                    .map(movie -> ObjectUtils.toScoredMovie(movie, "genre"))
                    .toList();
            ctx.addCandidates(candidates);
            return ctx;
        }, ioExecutor);
    }
}
