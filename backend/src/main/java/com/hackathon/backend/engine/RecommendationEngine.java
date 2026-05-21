package com.hackathon.backend.engine;

import com.hackathon.backend.commons.pipeline.ParallelPipeline;
import com.hackathon.backend.commons.pipeline.SequentialPipeline;
import com.hackathon.backend.engine.entities.RecommendationContext;
import com.hackathon.backend.engine.tasks.FetchMovieDetailsTask;
import com.hackathon.backend.engine.tasks.FetchMoviesByUserProfileVectorTask;
import com.hackathon.backend.engine.tasks.KeywordSearchTask;
import com.hackathon.backend.engine.tasks.LoadRecommendationProfileTask;
import com.hackathon.backend.engine.tasks.MovieReRankTask;
import com.hackathon.backend.engine.tasks.SemanticSearchTask;
import com.hackathon.backend.models.Movie;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.CompletableFuture;

@Component
@RequiredArgsConstructor
public class RecommendationEngine {

    private final SemanticSearchTask semanticSearchTask;
    private final KeywordSearchTask keywordSearchTask;
    private final FetchMoviesByUserProfileVectorTask fetchMoviesByUserProfileVectorTask;
    private final MovieReRankTask movieReRankTask;
    private final FetchMovieDetailsTask fetchMovieDetailsTask;
    private final LoadRecommendationProfileTask loadRecommendationProfileTask;

    public CompletableFuture<List<Movie>> execute(RecommendationContext context) {

        return new SequentialPipeline<RecommendationContext>()
                .then(prepareDataPipeline())
                .then(fetchPipeLine())
                .then(deobietdatten())
                .execute(context)
                .thenApply(ctx -> ctx.getMovieDetails() != null ? ctx.getMovieDetails() : List.of());
    }

    public SequentialPipeline<RecommendationContext> prepareDataPipeline() {
        return new SequentialPipeline<RecommendationContext>()
                .then(loadRecommendationProfileTask);
    }


    public ParallelPipeline<RecommendationContext> fetchPipeLine() {
        return new ParallelPipeline<RecommendationContext>()
                .add(semanticSearchTask)
                .add(keywordSearchTask)
                .add(fetchMoviesByUserProfileVectorTask);
    }

    public SequentialPipeline<RecommendationContext> deobietdatten() {
        return new SequentialPipeline<RecommendationContext>()
                .then(movieReRankTask)
                .then(fetchMovieDetailsTask);
    }
}
