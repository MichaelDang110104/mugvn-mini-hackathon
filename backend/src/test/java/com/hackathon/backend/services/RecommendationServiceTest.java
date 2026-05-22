package com.hackathon.backend.services;

import com.hackathon.backend.dto.RecommendationResponse;
import com.hackathon.backend.dto.VectorSearchResult;
import com.hackathon.backend.enums.EventType;
import com.hackathon.backend.models.EmbeddedMovie;
import com.hackathon.backend.models.UserEvent;
import com.hackathon.backend.repositories.UserEventRepository;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RecommendationServiceTest {

    @Mock
    private MongoTemplate mongoTemplate;

    @Mock
    private UserEventRepository userEventRepository;

    @Mock
    private VectorSearchService vectorSearchService;

    @Mock
    private EmbeddingService embeddingService;

    @InjectMocks
    private RecommendationService service;

    @Test
    void getRecommendations_personalized_usesSeedEmbeddingWithoutReembeddingText() {
        String sessionId = "session-1";
        String movieId = "507f1f77bcf86cd799439011";

        when(userEventRepository.findBySessionIdOrderByTimestampDesc(sessionId)).thenReturn(List.of(
                event(movieId, EventType.LIKE),
                event("507f1f77bcf86cd799439012", EventType.VIEW),
                event("507f1f77bcf86cd799439013", EventType.SAVE)
        ));

        when(mongoTemplate.findOne(any(Query.class), eq(EmbeddedMovie.class))).thenReturn(
                EmbeddedMovie.builder()
                        .id(new ObjectId(movieId))
                        .plotEmbedding(List.of(0.1, 0.2, 0.3))
                        .build()
        );

        when(vectorSearchService.searchByEmbedding(List.of(0.1, 0.2, 0.3), 17)).thenReturn(List.of(
                VectorSearchResult.builder()
                        .movie(EmbeddedMovie.builder()
                                .id(new ObjectId("507f1f77bcf86cd799439099"))
                                .title("Recommended")
                                .build())
                        .vectorSearchScore(0.8)
                        .build()
        ));

        RecommendationResponse response = service.getRecommendations(sessionId, "homepage", 12, null);

        assertThat(response.getMode()).isEqualTo("personalized");
        assertThat(response.isFallbackUsed()).isFalse();
        assertThat(response.getItems()).hasSize(1);
        verify(vectorSearchService).searchByEmbedding(List.of(0.1, 0.2, 0.3), 17);
        verify(vectorSearchService, never()).findSimilarMovies(any(), any(), any());
        verifyNoTextEmbedding();
    }

    private UserEvent event(String movieId, EventType type) {
        return UserEvent.builder()
                .eventId(new ObjectId().toHexString())
                .sessionId("session-1")
                .movieId(movieId)
                .eventType(type)
                .timestamp(Instant.now())
                .build();
    }

    private void verifyNoTextEmbedding() {
        verify(embeddingService, never()).embed(any(String.class));
    }
}
