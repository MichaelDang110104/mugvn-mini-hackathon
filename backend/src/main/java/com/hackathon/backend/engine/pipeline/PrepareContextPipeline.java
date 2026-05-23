package com.hackathon.backend.engine.pipeline;

import com.hackathon.backend.commons.pipeline.ParallelPipeline;
import com.hackathon.backend.engine.entities.RecommendationContext;
import com.hackathon.backend.engine.tasks.loader.LoadRecommendationProfileTask;
import com.hackathon.backend.engine.tasks.loader.LoadSeenMoviesTask;
import org.springframework.stereotype.Component;

@Component
public class PrepareContextPipeline {

    private final LoadRecommendationProfileTask loadRecommendationProfileTask;
    private final LoadSeenMoviesTask loadSeenMoviesTask;

    public PrepareContextPipeline(LoadRecommendationProfileTask loadRecommendationProfileTask,
                                  LoadSeenMoviesTask loadSeenMoviesTask) {
        this.loadRecommendationProfileTask = loadRecommendationProfileTask;
        this.loadSeenMoviesTask = loadSeenMoviesTask;
    }

    public ParallelPipeline<RecommendationContext> build() {
        return new ParallelPipeline<RecommendationContext>()
                .add(loadRecommendationProfileTask)
                .add(loadSeenMoviesTask);
    }
}
