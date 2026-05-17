package com.hackathon.backend.services;

import com.hackathon.backend.dto.SearchResponse;
import com.hackathon.backend.dto.SearchResponse.SearchItem;
import com.hackathon.backend.dto.VectorSearchResult;
import com.hackathon.backend.models.EmbeddedMovie;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MovieSearchServiceTest {

    @Mock
    private VectorSearchService vectorSearchService;

    @Mock
    private MongoTemplate mongoTemplate;

    @InjectMocks
    private MovieSearchService service;

    @Test
    void search_withQuery_delegatesToVectorSearchByText() {
        when(vectorSearchService.searchByQueryText("action movies", 10))
                .thenReturn(List.of(buildResult("The Matrix")));

        SearchResponse response = service.search("action movies", null);

        verify(vectorSearchService).searchByQueryText("action movies", 10);
        assertThat(response.getMode()).isEqualTo("semantic");
        assertThat(response.isFallbackUsed()).isFalse();
    }

    @Test
    void search_withBlankQuery_returnsColdStartWithoutCallingVectorSearch() {
        when(mongoTemplate.find(any(Query.class), eq(EmbeddedMovie.class)))
                .thenReturn(List.of());

        SearchResponse response = service.search("", null);

        assertThat(response.getMode()).isEqualTo("cold_start");
        verifyNoInteractions(vectorSearchService);
    }

    @Test
    void search_whenVectorReturnsEmpty_fallsBackToTextSearch() {
        when(vectorSearchService.searchByQueryText(anyString(), anyInt()))
                .thenReturn(List.of());
        when(mongoTemplate.find(any(Query.class), eq(EmbeddedMovie.class)))
                .thenReturn(List.of());

        SearchResponse response = service.search("very obscure query", null);

        assertThat(response.isFallbackUsed()).isTrue();
    }

    @Test
    void findSimilarMovies_passesPlotTextToVectorSearch() {
        EmbeddedMovie movie = EmbeddedMovie.builder()
                .id(new ObjectId("507f1f77bcf86cd799439011"))
                .plot("A hacker discovers the world is a simulation.")
                .build();
        when(mongoTemplate.findOne(any(Query.class), eq(EmbeddedMovie.class)))
                .thenReturn(movie);
        when(vectorSearchService.findSimilarMovies(anyString(), anyString(), anyInt()))
                .thenReturn(List.of());

        service.findSimilarMovies("507f1f77bcf86cd799439011", 5);

        verify(vectorSearchService).findSimilarMovies(
                "A hacker discovers the world is a simulation.",
                "507f1f77bcf86cd799439011",
                5);
    }

    @Test
    void findSimilarMovies_whenMovieNotFound_returnsEmpty() {
        when(mongoTemplate.findOne(any(Query.class), eq(EmbeddedMovie.class)))
                .thenReturn(null);

        List<SearchItem> results = service.findSimilarMovies("507f1f77bcf86cd799439011", 5);

        assertThat(results).isEmpty();
        verifyNoInteractions(vectorSearchService);
    }

    @Test
    void findSimilarMovies_whenPlotIsNull_returnsEmpty() {
        EmbeddedMovie movie = EmbeddedMovie.builder()
                .id(new ObjectId("507f1f77bcf86cd799439011"))
                .plot(null)
                .build();
        when(mongoTemplate.findOne(any(Query.class), eq(EmbeddedMovie.class)))
                .thenReturn(movie);

        List<SearchItem> results = service.findSimilarMovies("507f1f77bcf86cd799439011", 5);

        assertThat(results).isEmpty();
        verifyNoInteractions(vectorSearchService);
    }

    @Test
    void findSimilarMovies_whenPlotIsBlank_returnsEmpty() {
        EmbeddedMovie movie = EmbeddedMovie.builder()
                .id(new ObjectId("507f1f77bcf86cd799439011"))
                .plot("   ")
                .build();
        when(mongoTemplate.findOne(any(Query.class), eq(EmbeddedMovie.class)))
                .thenReturn(movie);

        List<SearchItem> results = service.findSimilarMovies("507f1f77bcf86cd799439011", 5);

        assertThat(results).isEmpty();
        verifyNoInteractions(vectorSearchService);
    }

    private VectorSearchResult buildResult(String title) {
        EmbeddedMovie movie = EmbeddedMovie.builder()
                .id(new ObjectId())
                .title(title)
                .build();
        return VectorSearchResult.builder().movie(movie).vectorSearchScore(0.9).build();
    }
}
