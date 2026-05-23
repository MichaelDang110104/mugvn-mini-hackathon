package com.hackathon.backend.engine.pipeline;

import com.hackathon.backend.commons.pipeline.SequentialPipeline;
import com.hackathon.backend.engine.entities.RecommendationContext;
import com.hackathon.backend.engine.tasks.ranker.FetchMovieDetailsTask;
import com.hackathon.backend.engine.tasks.ranker.MovieReRankTask;
import org.springframework.stereotype.Component;

@Component
public class RankAndResolvePipeline {

    private final MovieReRankTask movieReRankTask;
    private final FetchMovieDetailsTask fetchMovieDetailsTask;

    public RankAndResolvePipeline(MovieReRankTask movieReRankTask,
                                  FetchMovieDetailsTask fetchMovieDetailsTask) {
        this.movieReRankTask = movieReRankTask;
        this.fetchMovieDetailsTask = fetchMovieDetailsTask;
    }

    public SequentialPipeline<RecommendationContext> build() {
        return new SequentialPipeline<RecommendationContext>()
                .then(movieReRankTask)
                .then(fetchMovieDetailsTask);
    }
}
