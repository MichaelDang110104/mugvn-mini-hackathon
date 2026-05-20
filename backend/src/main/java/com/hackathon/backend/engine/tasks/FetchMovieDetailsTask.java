package com.hackathon.backend.engine.tasks;

import com.hackathon.backend.commons.pipeline.Task;
import com.hackathon.backend.engine.entities.RecommendationContext;
import com.hackathon.backend.models.Movie;
import com.hackathon.backend.repositories.MovieRepository;
import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

@Component
public class FetchMovieDetailsTask extends Task<RecommendationContext> {

    private final MovieRepository movieRepository;
    private final Executor ioExecutor;

    public FetchMovieDetailsTask(MovieRepository movieRepository,
                                 @Qualifier("ioExecutor") Executor ioExecutor) {
        this.movieRepository = movieRepository;
        this.ioExecutor = ioExecutor;
    }

    @Override
    public String name() {
        return "fetch_movie_details";
    }

    @Override
    public boolean shouldSkip(RecommendationContext ctx) {
        return ctx.getRankedCandidates() == null || ctx.getRankedCandidates().isEmpty();
    }

    @Override
    public CompletableFuture<RecommendationContext> execute(RecommendationContext ctx) {
        return CompletableFuture.supplyAsync(() -> {
            List<ObjectId> ids = ctx.getRankedCandidates().stream()
                    .map(sm -> sm.getMovieId())
                    .filter(Objects::nonNull)
                    .map(ObjectId::new)
                    .toList();

            List<Movie> details = movieRepository.findAllById(ids);
            ctx.setMovieDetails(details);
            return ctx;
        }, ioExecutor);
    }
}
