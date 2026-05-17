package com.hackathon.backend.services;

import com.hackathon.backend.dto.VectorSearchResult;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
class VectorSearchServiceTest {

    @Mock
    private VectorStore vectorStore;

    @InjectMocks
    private VectorSearchService service;

    private Document buildMovieDocument(String id, String title, double score) {
        Map<String, Object> meta = new HashMap<>();
        meta.put("title", title);
        meta.put("plot", "A plot about " + title);
        meta.put("genres", List.of("Action"));
        meta.put("cast", List.of("Actor One"));
        meta.put("directors", List.of("Director One"));
        meta.put("writers", List.of("Writer One"));
        meta.put("languages", List.of("English"));
        meta.put("countries", List.of("USA"));
        meta.put("runtime", 120);
        meta.put("year", 1999);
        meta.put("rated", "PG-13");
        meta.put("type", "movie");
        meta.put("poster", "https://example.com/poster.jpg");
        meta.put("lastupdated", "2015-09-01");
        meta.put("num_mflix_comments", 3);

        Document doc = mock(Document.class);
        lenient().when(doc.getId()).thenReturn(id);
        lenient().when(doc.getScore()).thenReturn(score);
        lenient().when(doc.getMetadata()).thenReturn(meta);
        return doc;
    }

    @Test
    void searchByQueryText_mapsDocumentToResult() {
        Document doc = buildMovieDocument("507f1f77bcf86cd799439011", "The Matrix", 0.95);
        when(vectorStore.similaritySearch(any(SearchRequest.class))).thenReturn(List.of(doc));

        List<VectorSearchResult> results = service.searchByQueryText("sci-fi hacker", 5);

        assertThat(results).hasSize(1);
        assertThat(results.get(0).getVectorSearchScore()).isEqualTo(0.95);
        assertThat(results.get(0).getMovie().getTitle()).isEqualTo("The Matrix");
        assertThat(results.get(0).getMovie().getGenres()).containsExactly("Action");
    }

    @Test
    void searchByQueryText_passesCorrectSearchRequest() {
        when(vectorStore.similaritySearch(any(SearchRequest.class))).thenReturn(List.of());

        service.searchByQueryText("action movies", 7);

        ArgumentCaptor<SearchRequest> captor = ArgumentCaptor.forClass(SearchRequest.class);
        verify(vectorStore).similaritySearch(captor.capture());
        assertThat(captor.getValue().getQuery()).isEqualTo("action movies");
        assertThat(captor.getValue().getTopK()).isEqualTo(7);
    }

    @Test
    void searchByQueryText_whenVectorStoreThrows_returnsEmpty() {
        when(vectorStore.similaritySearch(any(SearchRequest.class)))
                .thenThrow(new RuntimeException("Atlas connection error"));

        List<VectorSearchResult> results = service.searchByQueryText("sci-fi", 5);

        assertThat(results).isEmpty();
    }

    @Test
    void findSimilarMovies_excludesTargetMovieById() {
        String targetId = "507f1f77bcf86cd799439011";
        String otherId  = "507f1f77bcf86cd799439012";

        Document self  = buildMovieDocument(targetId, "The Matrix",          1.00);
        Document other = buildMovieDocument(otherId,  "The Matrix Reloaded", 0.92);

        when(vectorStore.similaritySearch(any(SearchRequest.class)))
                .thenReturn(List.of(self, other));

        List<VectorSearchResult> results = service.findSimilarMovies(
                "A hacker learns the truth.", targetId, 1);

        assertThat(results).hasSize(1);
        assertThat(results.get(0).getMovie().getTitle()).isEqualTo("The Matrix Reloaded");
    }

    @Test
    void findSimilarMovies_whenVectorStoreThrows_returnsEmpty() {
        when(vectorStore.similaritySearch(any(SearchRequest.class)))
                .thenThrow(new RuntimeException("Atlas connection error"));

        List<VectorSearchResult> results = service.findSimilarMovies(
                "A hacker learns the truth.", "507f1f77bcf86cd799439011", 5);

        assertThat(results).isEmpty();
    }

    @Test
    void findSimilarMovies_requestsTopKPlusOne() {
        when(vectorStore.similaritySearch(any(SearchRequest.class))).thenReturn(List.of());

        service.findSimilarMovies("some plot", "anyId", 5);

        ArgumentCaptor<SearchRequest> captor = ArgumentCaptor.forClass(SearchRequest.class);
        verify(vectorStore).similaritySearch(captor.capture());
        assertThat(captor.getValue().getTopK()).isEqualTo(6); // limit + 1
    }
}
