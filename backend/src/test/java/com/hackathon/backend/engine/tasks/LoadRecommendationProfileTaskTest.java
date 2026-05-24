package com.hackathon.backend.engine.tasks;

import com.hackathon.backend.engine.entities.RecommendationContext;
import com.hackathon.backend.engine.tasks.loader.LoadRecommendationProfileTask;
import com.hackathon.backend.models.RecommendationProfile;
import com.hackathon.backend.models.UserEvent;
import com.hackathon.backend.repositories.RecommendationProfileRepository;
import com.hackathon.backend.repositories.UserEventRepository;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class LoadRecommendationProfileTaskTest {

    private final RecommendationProfileRepository recommendationProfileRepository = mock(RecommendationProfileRepository.class);
    private final UserEventRepository userEventRepository = mock(UserEventRepository.class);
    private final LoadRecommendationProfileTask task = new LoadRecommendationProfileTask(
            recommendationProfileRepository,
            userEventRepository
    );

    @Test
    void execute_loadsProfileEmbeddingAndExcludedMovieIds() {
        RecommendationContext context = RecommendationContext.builder()
                .userId("user-1")
                .excludedMovieIds(List.of())
                .recentQueries(List.of())
                .build();

        when(recommendationProfileRepository.findById("user-1")).thenReturn(Optional.of(
                RecommendationProfile.builder()
                        .userId("user-1")
                        .profileEmbedding(List.of(0.1, 0.2))
                        .build()
        ));
        when(userEventRepository.findByUserIdOrderByTimestampDesc("user-1")).thenReturn(List.of(
                event("movie-1"),
                event("movie-2"),
                event("movie-1"),
                event(null)
        ));

        RecommendationContext result = task.execute(context).join();

        assertThat(result.getUserProfileEmbedding()).containsExactly(0.1, 0.2);
        assertThat(result.getExcludedMovieIds()).containsExactly("movie-1", "movie-2");
    }

    private UserEvent event(String movieId) {
        return UserEvent.builder()
                .movieId(movieId)
                .timestamp(Instant.now())
                .build();
    }
}
