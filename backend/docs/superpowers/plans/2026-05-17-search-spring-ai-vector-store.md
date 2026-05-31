# Spring AI MongoDB Atlas VectorStore — Search Refactor Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace the hand-rolled `$vectorSearch` MongoDB aggregation in `VectorSearchService` with Spring AI's `MongoDBAtlasVectorStore`, so search and similar-movie lookups go through `vectorStore.similaritySearch()`.

**Architecture:** Add `spring-ai-starter-vector-store-mongodb-atlas` (auto-configures `MongoDBAtlasVectorStore` using the existing `EmbeddingModel` on the classpath). Point it at the existing `embedded_movies` collection / `vector_index` / `plot_embedding` field via `application.yaml`. Rewrite `VectorSearchService` to call `VectorStore.similaritySearch(SearchRequest)` and map returned Spring AI `Document` metadata back to `EmbeddedMovie`. Simplify `MovieSearchService` to remove the manual embedding step and pass text directly.

**Tech Stack:** Spring Boot 4.0.6, Spring AI 2.0.0-M6, `spring-ai-starter-vector-store-mongodb-atlas`, MongoDB Atlas, JUnit 5, Mockito, AssertJ

---

## File Map

| File | Action | Responsibility |
|------|--------|----------------|
| `pom.xml` | Modify | Add MongoDB Atlas Vector Store starter |
| `src/main/resources/application.yaml` | Modify | Add `spring.ai.vectorstore.mongodb.*` config, remove `app.vector-search.*` |
| `src/main/java/com/hackathon/backend/services/VectorSearchService.java` | Full rewrite | Spring AI VectorStore calls + Document→EmbeddedMovie mapping |
| `src/main/java/com/hackathon/backend/services/MovieSearchService.java` | Targeted edits | Remove EmbeddingService, call `searchByQueryText`, fetch `plot` not `plot_embedding` |
| `src/test/java/com/hackathon/backend/services/VectorSearchServiceTest.java` | Create | Unit tests for new VectorSearchService |
| `src/test/java/com/hackathon/backend/services/MovieSearchServiceTest.java` | Create | Unit tests for MovieSearchService search path |

---

## Task 1: Add dependency and configuration

**Files:**
- Modify: `pom.xml`
- Modify: `src/main/resources/application.yaml`

- [ ] **Step 1: Add the vector store starter to pom.xml**

In `pom.xml`, add inside `<dependencies>` (after the existing `spring-ai-starter-model-openai` entry):

```xml
<dependency>
    <groupId>org.springframework.ai</groupId>
    <artifactId>spring-ai-starter-vector-store-mongodb-atlas</artifactId>
</dependency>
```

- [ ] **Step 2: Add vectorstore config to application.yaml**

Replace the current `app.vector-search.*` block with the Spring AI vectorstore config. The full file becomes:

```yaml
spring:
  application:
    name: backend
  ai:
    openai:
      api-key: ${OPENAI_API_KEY}
      embedding:
        options:
          model: text-embedding-ada-002
    vectorstore:
      mongodb:
        collection-name: embedded_movies
        index-name: vector_index
        path-name: plot_embedding
        initialize-schema: false
  mongodb:
    uri: mongodb+srv://michaeldang0104_db_user:m9tiK5rbFOYbMuGN@mongodb-hackathon.yxj8jm9.mongodb.net/sample_mflix?appName=MongoDB-Hackathon
```

- [ ] **Step 3: Verify the project compiles**

Run from `backend/`:
```bash
mvn compile -q
```
Expected: `BUILD SUCCESS` with no errors. The auto-configuration wires `MongoDBAtlasVectorStore` using the existing `EmbeddingModel` bean.

- [ ] **Step 4: Commit**

```bash
git add pom.xml src/main/resources/application.yaml
git commit -m "feat: add spring-ai-mongodb-atlas-store dependency and vectorstore config"
```

---

## Task 2: Rewrite VectorSearchService

**Files:**
- Create: `src/test/java/com/hackathon/backend/services/VectorSearchServiceTest.java`
- Full rewrite: `src/main/java/com/hackathon/backend/services/VectorSearchService.java`

- [ ] **Step 1: Write the failing tests**

Create `src/test/java/com/hackathon/backend/services/VectorSearchServiceTest.java`:

```java
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
        when(doc.getId()).thenReturn(id);
        when(doc.getScore()).thenReturn(score);
        when(doc.getMetadata()).thenReturn(meta);
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
    void findSimilarMovies_requestsTopKPlusOne() {
        when(vectorStore.similaritySearch(any(SearchRequest.class))).thenReturn(List.of());

        service.findSimilarMovies("some plot", "anyId", 5);

        ArgumentCaptor<SearchRequest> captor = ArgumentCaptor.forClass(SearchRequest.class);
        verify(vectorStore).similaritySearch(captor.capture());
        assertThat(captor.getValue().getTopK()).isEqualTo(6); // limit + 1
    }
}
```

- [ ] **Step 2: Run tests to confirm they fail**

```bash
mvn test -Dtest=VectorSearchServiceTest -q
```
Expected: compilation error — `VectorSearchService` does not yet have `searchByQueryText` or `findSimilarMovies(String, String, int)`.

- [ ] **Step 3: Rewrite VectorSearchService**

Replace the entire contents of `src/main/java/com/hackathon/backend/services/VectorSearchService.java`:

```java
package com.hackathon.backend.services;

import com.hackathon.backend.dto.VectorSearchResult;
import com.hackathon.backend.models.EmbeddedMovie;
import com.hackathon.backend.models.Movie;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bson.types.ObjectId;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class VectorSearchService {

    private final VectorStore vectorStore;

    public List<VectorSearchResult> searchByQueryText(String queryText, int limit) {
        try {
            SearchRequest request = SearchRequest.builder()
                    .query(queryText)
                    .topK(limit)
                    .build();
            return vectorStore.similaritySearch(request).stream()
                    .map(this::mapToVectorSearchResult)
                    .toList();
        } catch (Exception e) {
            log.error("Vector search failed for query [{}]: {}", queryText, e.getMessage(), e);
            return List.of();
        }
    }

    public List<VectorSearchResult> findSimilarMovies(String moviePlot, String excludeMovieId, int limit) {
        try {
            SearchRequest request = SearchRequest.builder()
                    .query(moviePlot)
                    .topK(limit + 1)
                    .build();
            return vectorStore.similaritySearch(request).stream()
                    .filter(doc -> !excludeMovieId.equals(doc.getId()))
                    .limit(limit)
                    .map(this::mapToVectorSearchResult)
                    .toList();
        } catch (Exception e) {
            log.error("Similar movie search failed: {}", e.getMessage(), e);
            return List.of();
        }
    }

    @SuppressWarnings("unchecked")
    private VectorSearchResult mapToVectorSearchResult(Document doc) {
        Map<String, Object> meta = doc.getMetadata();

        EmbeddedMovie movie = EmbeddedMovie.builder()
                .id(doc.getId() != null ? new ObjectId(doc.getId()) : null)
                .title((String) meta.get("title"))
                .plot((String) meta.get("plot"))
                .fullplot((String) meta.get("fullplot"))
                .genres((List<String>) meta.get("genres"))
                .cast((List<String>) meta.get("cast"))
                .directors((List<String>) meta.get("directors"))
                .writers((List<String>) meta.get("writers"))
                .languages((List<String>) meta.get("languages"))
                .countries((List<String>) meta.get("countries"))
                .runtime(toInteger(meta.get("runtime")))
                .year(toInteger(meta.get("year")))
                .rated((String) meta.get("rated"))
                .type((String) meta.get("type"))
                .poster((String) meta.get("poster"))
                .lastupdated((String) meta.get("lastupdated"))
                .released((Date) meta.get("released"))
                .numMflixComments(toInteger(meta.get("num_mflix_comments")))
                .imdb(mapImdb(meta.get("imdb")))
                .tomatoes(mapTomatoes(meta.get("tomatoes")))
                .awards(mapAwards(meta.get("awards")))
                .build();

        double score = doc.getScore() != null ? doc.getScore() : 0.0;
        return VectorSearchResult.builder().movie(movie).vectorSearchScore(score).build();
    }

    @SuppressWarnings("unchecked")
    private Movie.Imdb mapImdb(Object obj) {
        if (obj == null) return null;
        Map<String, Object> doc = (Map<String, Object>) obj;
        return Movie.Imdb.builder()
                .rating(toDouble(doc.get("rating")))
                .votes(toInteger(doc.get("votes")))
                .id(toInteger(doc.get("id")))
                .build();
    }

    @SuppressWarnings("unchecked")
    private Movie.Awards mapAwards(Object obj) {
        if (obj == null) return null;
        Map<String, Object> doc = (Map<String, Object>) obj;
        return Movie.Awards.builder()
                .wins(toInteger(doc.get("wins")))
                .nominations(toInteger(doc.get("nominations")))
                .text((String) doc.get("text"))
                .build();
    }

    @SuppressWarnings("unchecked")
    private Movie.Tomatoes mapTomatoes(Object obj) {
        if (obj == null) return null;
        Map<String, Object> doc = (Map<String, Object>) obj;
        return Movie.Tomatoes.builder()
                .viewer(mapTomatoesReview(doc.get("viewer")))
                .critic(mapTomatoesReview(doc.get("critic")))
                .dvd((Date) doc.get("dvd"))
                .lastUpdated((Date) doc.get("lastUpdated"))
                .rotten(toInteger(doc.get("rotten")))
                .fresh(toInteger(doc.get("fresh")))
                .production((String) doc.get("production"))
                .build();
    }

    @SuppressWarnings("unchecked")
    private Movie.TomatoesReview mapTomatoesReview(Object obj) {
        if (obj == null) return null;
        Map<String, Object> doc = (Map<String, Object>) obj;
        return Movie.TomatoesReview.builder()
                .rating(toDouble(doc.get("rating")))
                .numReviews(toInteger(doc.get("numReviews")))
                .meter(toInteger(doc.get("meter")))
                .build();
    }

    private Integer toInteger(Object obj) {
        if (obj == null) return null;
        if (obj instanceof Integer i) return i;
        if (obj instanceof Long l) return l.intValue();
        if (obj instanceof Double d) return d.intValue();
        return null;
    }

    private Double toDouble(Object obj) {
        if (obj == null) return null;
        if (obj instanceof Double d) return d;
        if (obj instanceof Integer i) return i.doubleValue();
        if (obj instanceof Long l) return l.doubleValue();
        return null;
    }
}
```

- [ ] **Step 4: Run tests to confirm they pass**

```bash
mvn test -Dtest=VectorSearchServiceTest -q
```
Expected: `BUILD SUCCESS`, 5 tests pass.

- [ ] **Step 5: Commit**

```bash
git add src/test/java/com/hackathon/backend/services/VectorSearchServiceTest.java \
        src/main/java/com/hackathon/backend/services/VectorSearchService.java
git commit -m "feat: rewrite VectorSearchService to use Spring AI MongoDBAtlasVectorStore"
```

---

## Task 3: Update MovieSearchService

**Files:**
- Create: `src/test/java/com/hackathon/backend/services/MovieSearchServiceTest.java`
- Modify: `src/main/java/com/hackathon/backend/services/MovieSearchService.java`

- [ ] **Step 1: Write the failing tests**

Create `src/test/java/com/hackathon/backend/services/MovieSearchServiceTest.java`:

```java
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

    private VectorSearchResult buildResult(String title) {
        EmbeddedMovie movie = EmbeddedMovie.builder()
                .id(new ObjectId())
                .title(title)
                .build();
        return VectorSearchResult.builder().movie(movie).vectorSearchScore(0.9).build();
    }
}
```

- [ ] **Step 2: Run tests to confirm they fail**

```bash
mvn test -Dtest=MovieSearchServiceTest -q
```
Expected: FAIL — `MovieSearchService` still calls `embeddingService.embed()` and `searchByQueryVector()`, which don't match the new contract.

- [ ] **Step 3: Update MovieSearchService**

Replace the entire contents of `src/main/java/com/hackathon/backend/services/MovieSearchService.java`:

```java
package com.hackathon.backend.services;

import com.hackathon.backend.dto.SearchResponse;
import com.hackathon.backend.dto.SearchResponse.MovieSummary;
import com.hackathon.backend.dto.SearchResponse.Reason;
import com.hackathon.backend.dto.SearchResponse.SearchItem;
import com.hackathon.backend.dto.VectorSearchResult;
import com.hackathon.backend.models.EmbeddedMovie;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bson.types.ObjectId;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.TextCriteria;
import org.springframework.data.mongodb.core.query.TextQuery;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class MovieSearchService {

    private final VectorSearchService vectorSearchService;
    private final MongoTemplate mongoTemplate;

    private static final int DEFAULT_LIMIT = 10;

    public SearchResponse search(String queryText, Integer limit) {
        int effectiveLimit = (limit != null && limit > 0) ? limit : DEFAULT_LIMIT;

        if (queryText == null || queryText.isBlank()) {
            return buildColdStartResponse(effectiveLimit);
        }

        List<VectorSearchResult> results = vectorSearchService.searchByQueryText(queryText, effectiveLimit);

        if (results.isEmpty()) {
            log.warn("Vector search returned empty for query [{}], falling back to text search", queryText);
            return buildTextFallbackResponse(queryText, effectiveLimit);
        }

        List<SearchItem> items = results.stream()
                .map(r -> mapToSearchItem(r, "semantic_match_to_search",
                        "Matches your search: " + queryText))
                .toList();

        return SearchResponse.builder()
                .items(items)
                .mode("semantic")
                .fallbackUsed(false)
                .query(queryText)
                .build();
    }

    public List<SearchItem> findSimilarMovies(String movieId, int limit) {
        Query query = new Query(Criteria.where("_id").is(new ObjectId(movieId)));
        query.fields().include("plot");
        EmbeddedMovie movie = mongoTemplate.findOne(query, EmbeddedMovie.class);

        if (movie == null || movie.getPlot() == null || movie.getPlot().isBlank()) {
            log.warn("No plot found for movie [{}]", movieId);
            return List.of();
        }

        List<VectorSearchResult> results = vectorSearchService.findSimilarMovies(
                movie.getPlot(), movieId, limit);

        return results.stream()
                .map(r -> mapToSearchItem(r, "similar_to_recently_viewed",
                        "Semantically similar to the current movie"))
                .toList();
    }

    private SearchResponse buildColdStartResponse(int limit) {
        Query query = new Query();
        query.with(Sort.by(Sort.Direction.DESC, "imdb.rating"));
        query.limit(limit);
        query.fields().exclude("plot_embedding").exclude("plot_embedding_voyage_3_large");

        List<EmbeddedMovie> movies = mongoTemplate.find(query, EmbeddedMovie.class);

        List<SearchItem> items = movies.stream()
                .map(m -> SearchItem.builder()
                        .movie(toMovieSummary(m))
                        .score(m.getImdb() != null && m.getImdb().getRating() != null
                                ? m.getImdb().getRating() / 10.0 : 0.0)
                        .reasons(List.of(Reason.builder()
                                .code("trending_now")
                                .label("Popular and trending")
                                .build()))
                        .build())
                .toList();

        return SearchResponse.builder()
                .items(items)
                .mode("cold_start")
                .fallbackUsed(false)
                .query(null)
                .build();
    }

    private SearchResponse buildTextFallbackResponse(String queryText, int limit) {
        try {
            TextCriteria textCriteria = TextCriteria.forDefaultLanguage()
                    .matchingAny(queryText.split("\\s+"));

            Query query = TextQuery.queryText(textCriteria)
                    .sortByScore()
                    .limit(limit);

            query.fields().exclude("plot_embedding").exclude("plot_embedding_voyage_3_large");

            List<EmbeddedMovie> movies = mongoTemplate.find(query, EmbeddedMovie.class);

            List<SearchItem> items = movies.stream()
                    .map(m -> SearchItem.builder()
                            .movie(toMovieSummary(m))
                            .score(0.0)
                            .reasons(List.of(Reason.builder()
                                    .code("fallback_text_match")
                                    .label("Matched by text search")
                                    .build()))
                            .build())
                    .toList();

            return SearchResponse.builder()
                    .items(items)
                    .mode("fallback_text")
                    .fallbackUsed(true)
                    .query(queryText)
                    .hint("Semantic search was unavailable. Results are based on text matching.")
                    .build();
        } catch (Exception e) {
            log.error("Text fallback search also failed: {}", e.getMessage(), e);
            return SearchResponse.builder()
                    .items(List.of())
                    .mode("fallback_text")
                    .fallbackUsed(true)
                    .query(queryText)
                    .hint("Search is temporarily unavailable.")
                    .build();
        }
    }

    private SearchItem mapToSearchItem(VectorSearchResult result,
                                       String reasonCode, String reasonLabel) {
        return SearchItem.builder()
                .movie(toMovieSummary(result.getMovie()))
                .score(result.getVectorSearchScore())
                .reasons(List.of(Reason.builder()
                        .code(reasonCode)
                        .label(reasonLabel)
                        .build()))
                .build();
    }

    private MovieSummary toMovieSummary(EmbeddedMovie movie) {
        return MovieSummary.builder()
                .id(movie.getId() != null ? movie.getId().toHexString() : null)
                .title(movie.getTitle())
                .posterUrl(movie.getPoster())
                .genres(movie.getGenres())
                .ratingAvg(movie.getImdb() != null ? movie.getImdb().getRating() : null)
                .availability(SearchResponse.Availability.builder()
                        .isAvailable(true)
                        .region("global")
                        .build())
                .build();
    }
}
```

- [ ] **Step 4: Run the tests to confirm they pass**

```bash
mvn test -Dtest=MovieSearchServiceTest -q
```
Expected: `BUILD SUCCESS`, 6 tests pass.

- [ ] **Step 5: Run the full test suite**

```bash
mvn test -q
```
Expected: `BUILD SUCCESS`. All tests pass including `BackendApplicationTests.contextLoads`.

> **Note:** `contextLoads` starts the full Spring context including `MongoDBAtlasVectorStore`. It requires the MongoDB Atlas URI in `application.yaml` to be reachable. If the test environment is offline, skip it with `-Dtest='!BackendApplicationTests'` and verify manually.

- [ ] **Step 6: Commit**

```bash
git add src/test/java/com/hackathon/backend/services/MovieSearchServiceTest.java \
        src/main/java/com/hackathon/backend/services/MovieSearchService.java
git commit -m "feat: update MovieSearchService to use Spring AI VectorStore via searchByQueryText"
```

---

## Self-Review Checklist

- [x] **Spec coverage:** pom.xml ✓ | application.yaml ✓ | VectorSearchService rewrite ✓ | MovieSearchService EmbeddingService removal ✓ | findSimilarMovies plot text ✓ | null plot guard ✓ | cold-start/fallback unchanged ✓
- [x] **No placeholders:** All steps have exact code or commands.
- [x] **Type consistency:** `searchByQueryText(String, int)` defined in Task 2, called in Task 3 tests and implementation. `findSimilarMovies(String, String, int)` same. `VectorSearchResult` builder fields match the DTO definition throughout.
