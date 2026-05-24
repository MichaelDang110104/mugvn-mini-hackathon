package com.hackathon.backend.engine.tasks;

import com.hackathon.backend.dto.VectorSearchResult;
import com.hackathon.backend.engine.entities.RecommendationContext;
import com.hackathon.backend.engine.tasks.fetcher.FetchByUserVectorTask;
import com.hackathon.backend.models.EmbeddedMovie;
import com.hackathon.backend.services.VectorSearchService;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.Executor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class FetchByUserVectorTaskTest {

    private final VectorSearchService vectorSearchService = mock(VectorSearchService.class);
    private final Executor directExecutor = Runnable::run;
    private final FetchByUserVectorTask task = new FetchByUserVectorTask(vectorSearchService, directExecutor);

    @Test
    void execute_filtersLowScoreAndExcludedMoviesAfterOverfetching() {
        RecommendationContext context = RecommendationContext.builder()
                .userProfileEmbedding(List.of(0.1, 0.2, 0.3))
                .limit(10)
                .excludedMovieIds(List.of("507f1f77bcf86cd799439012"))
                .recentQueries(List.of())
                .build();

        when(vectorSearchService.searchByEmbedding(List.of(0.1, 0.2, 0.3), 100)).thenReturn(List.of(
                result("507f1f77bcf86cd799439011", 0.82),
                result("507f1f77bcf86cd799439012", 0.81),
                result("507f1f77bcf86cd799439013", 0.74)
        ));

        RecommendationContext result = task.execute(context).join();

        verify(vectorSearchService).searchByEmbedding(List.of(0.1, 0.2, 0.3), 100);
        assertThat(result.getCandidates()).hasSize(1);
        assertThat(result.getCandidates().getFirst().getMovieId()).isEqualTo("507f1f77bcf86cd799439011");
    }

    private VectorSearchResult result(String movieId, double score) {
        return VectorSearchResult.builder()
                .movie(EmbeddedMovie.builder().id(new ObjectId(movieId)).build())
                .vectorSearchScore(score)
                .build();
    }
}
