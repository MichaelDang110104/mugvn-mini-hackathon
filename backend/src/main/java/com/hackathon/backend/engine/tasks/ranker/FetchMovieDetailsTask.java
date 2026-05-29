package com.hackathon.backend.engine.tasks.ranker;

import com.hackathon.backend.engine.entities.RecommendationContext;
import com.hackathon.backend.engine.tasks.RecommendationTaskBase;
import com.hackathon.backend.models.Movie;
import com.hackathon.backend.repositories.MovieRepository;
import lombok.extern.slf4j.Slf4j;
import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

@Slf4j
@Component
public class FetchMovieDetailsTask extends RecommendationTaskBase {

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
        return super.shouldSkip(ctx) || ctx.getRankedCandidates() == null || ctx.getRankedCandidates().isEmpty();
    }

    @Override
    public CompletableFuture<RecommendationContext> execute(RecommendationContext ctx) {
        log.info("[FetchMovieDetailsTask] userId={} mode={} rankedCandidates={}",
                ctx.getUserId(), ctx.getMode(), ctx.getRankedCandidates().size());

        return CompletableFuture.supplyAsync(() -> {
            List<ObjectId> ids = ctx.getRankedCandidates().stream()
                    .map(sm -> sm.getMovieId())
                    .filter(Objects::nonNull)
                    .map(ObjectId::new)
                    .toList();

            List<Movie> details = movieRepository.findAllById(ids);
            log.info("[FetchMovieDetailsTask] done — {} movies resolved", details.size());
            ctx.setMovieDetails(details);
            return ctx;
        }, ioExecutor);
    }
}
