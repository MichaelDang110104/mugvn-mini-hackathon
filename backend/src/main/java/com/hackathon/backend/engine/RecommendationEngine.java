package com.hackathon.backend.engine;

import com.hackathon.backend.commons.pipeline.SequentialPipeline;
import com.hackathon.backend.engine.entities.RecommendationContext;
import com.hackathon.backend.engine.pipeline.FetchCandidatesPipeline;
import com.hackathon.backend.engine.pipeline.PrepareContextPipeline;
import com.hackathon.backend.engine.pipeline.RankAndResolvePipeline;
import com.hackathon.backend.models.Movie;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.CompletableFuture;

@Component
@RequiredArgsConstructor
public class RecommendationEngine {

    private final PrepareContextPipeline prepareContextPipeline;
    private final FetchCandidatesPipeline fetchCandidatesPipeline;
    private final RankAndResolvePipeline rankAndResolvePipeline;

    public CompletableFuture<List<Movie>> execute(RecommendationContext context) {
        return new SequentialPipeline<RecommendationContext>()
                .then(prepareContextPipeline.build())
                .then(fetchCandidatesPipeline.build())
                .then(rankAndResolvePipeline.build())
                .execute(context)
                .thenApply(ctx -> ctx.getMovieDetails() != null ? ctx.getMovieDetails() : List.of());
    }
}
