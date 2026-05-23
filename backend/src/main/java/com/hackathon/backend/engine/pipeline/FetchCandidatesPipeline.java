package com.hackathon.backend.engine.pipeline;

import com.hackathon.backend.commons.pipeline.ParallelPipeline;
import com.hackathon.backend.engine.entities.RecommendationContext;
import com.hackathon.backend.engine.tasks.fetcher.FetchByGenreTask;
import com.hackathon.backend.engine.tasks.fetcher.FetchByKeywordSearchTask;
import com.hackathon.backend.engine.tasks.fetcher.FetchByRecentWatchTask;
import com.hackathon.backend.engine.tasks.fetcher.FetchBySemanticSearchTask;
import com.hackathon.backend.engine.tasks.fetcher.FetchByUserVectorTask;
import com.hackathon.backend.engine.tasks.fetcher.FetchSimilarToMovieTask;
import com.hackathon.backend.engine.tasks.fetcher.FetchTrendingTask;
import org.springframework.stereotype.Component;

@Component
public class FetchCandidatesPipeline {

    private final FetchBySemanticSearchTask fetchBySemanticSearchTask;
    private final FetchByKeywordSearchTask fetchByKeywordSearchTask;
    private final FetchByUserVectorTask fetchByUserVectorTask;
    private final FetchByGenreTask fetchByGenreTask;
    private final FetchTrendingTask fetchTrendingTask;
    private final FetchSimilarToMovieTask fetchSimilarToMovieTask;
    private final FetchByRecentWatchTask fetchByRecentWatchTask;

    public FetchCandidatesPipeline(FetchBySemanticSearchTask fetchBySemanticSearchTask,
                                   FetchByKeywordSearchTask fetchByKeywordSearchTask,
                                   FetchByUserVectorTask fetchByUserVectorTask,
                                   FetchByGenreTask fetchByGenreTask,
                                   FetchTrendingTask fetchTrendingTask,
                                   FetchSimilarToMovieTask fetchSimilarToMovieTask,
                                   FetchByRecentWatchTask fetchByRecentWatchTask) {
        this.fetchBySemanticSearchTask = fetchBySemanticSearchTask;
        this.fetchByKeywordSearchTask = fetchByKeywordSearchTask;
        this.fetchByUserVectorTask = fetchByUserVectorTask;
        this.fetchByGenreTask = fetchByGenreTask;
        this.fetchTrendingTask = fetchTrendingTask;
        this.fetchSimilarToMovieTask = fetchSimilarToMovieTask;
        this.fetchByRecentWatchTask = fetchByRecentWatchTask;
    }

    public ParallelPipeline<RecommendationContext> build() {
        return new ParallelPipeline<RecommendationContext>()
                .add(fetchBySemanticSearchTask)
                .add(fetchByKeywordSearchTask)
                .add(fetchByUserVectorTask)
                .add(fetchByGenreTask)
                .add(fetchTrendingTask)
                .add(fetchSimilarToMovieTask)
                .add(fetchByRecentWatchTask);
    }
}
