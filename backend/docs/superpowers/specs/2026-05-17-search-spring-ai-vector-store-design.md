# Design: Refactor Search to Use Spring AI MongoDB Atlas VectorStore

**Date:** 2026-05-17  
**Branch:** pipeline  
**Scope:** `SearchController#search` and the full vector search path

---

## Goal

Replace the hand-rolled `$vectorSearch` MongoDB aggregation in `VectorSearchService` with Spring AI's `MongoDBAtlasVectorStore`. The existing `embedded_movies` collection and Atlas vector index (`vector_index` on `plot_embedding`) are reused as-is — no data migration or re-indexing.

---

## Architecture

```
SearchController
    └── MovieSearchService
            ├── VectorSearchService          ← rewritten to use VectorStore
            │       └── VectorStore (Spring AI MongoDBAtlasVectorStore)
            │               └── EmbeddingModel (OpenAI ada-002, already configured)
            └── MongoTemplate                ← unchanged (cold-start, text fallback)
```

Spring AI's `MongoDBAtlasVectorStore` auto-configures from `spring.ai.vectorstore.mongodb.*` and uses the `EmbeddingModel` already on the classpath. No extra beans required.

---

## Changes

### 1. `pom.xml`

Add one dependency (version managed by the existing `spring-ai-bom`):

```xml
<dependency>
    <groupId>org.springframework.ai</groupId>
    <artifactId>spring-ai-starter-vector-store-mongodb-atlas</artifactId>
</dependency>
```

### 2. `application.yaml`

Add vectorstore config block pointing at the existing collection and index:

```yaml
spring:
  ai:
    vectorstore:
      mongodb:
        collection-name: embedded_movies
        index-name: vector_index
        path-name: plot_embedding
        initialize-schema: false
```

### 3. `VectorSearchService` — full rewrite

**Remove:** raw `$vectorSearch` aggregation, `@Value` config fields, all BSON Document mapping methods.

**Add:** `VectorStore` injection; two public methods:

- `searchByQueryText(String queryText, int limit)` — calls `vectorStore.similaritySearch(SearchRequest.builder().query(queryText).topK(limit).build())`, maps `List<Document>` to `List<VectorSearchResult>`.
- `findSimilarMovies(String moviePlot, String excludeMovieId, int limit)` — same but fetches `topK(limit + 1)`, post-filters the excluded movie ID, then limits to `limit`.

**Mapping** (`Document` → `VectorSearchResult`):
- `doc.getId()` → `EmbeddedMovie.id` (parsed as `ObjectId`)
- `doc.getScore()` → `VectorSearchResult.vectorSearchScore`
- `doc.getMetadata()` → all movie fields (`title`, `plot`, `genres`, `cast`, `imdb`, `tomatoes`, `awards`, etc.)
- Nested BSON objects (`imdb`, `tomatoes`, `awards`) arrive as `Map<String, Object>` in metadata — mapped with null-safe helpers (same logic as the old mapper, different source type).

**Error handling:** catch all exceptions, log, return `List.of()` — same contract as before so `MovieSearchService` falls through to text search.

### 4. `MovieSearchService` — two targeted changes

**`search()` method:**
- Remove `embeddingService.embed(queryText)` call and the empty-embedding fallback guard.
- Replace `vectorSearchService.searchByQueryVector(queryEmbedding, limit)` with `vectorSearchService.searchByQueryText(queryText, limit)`.
- Remove `EmbeddingService` field injection (no longer used here).

**`findSimilarMovies()` method:**
- Change the projection query to load `plot` instead of `plot_embedding`.
- Guard: if `movie.getPlot()` is null or blank, return `List.of()` early (same behavior as the existing null-embedding guard).
- Pass `movie.getPlot()` (String) to `vectorSearchService.findSimilarMovies()` instead of `movie.getPlotEmbedding()` (List<Double>).

---

## What Does NOT Change

| Component | Status |
|---|---|
| `SearchController` | Unchanged |
| `SearchResponse` / `VectorSearchResult` DTOs | Unchanged |
| `EmbeddingService` | Unchanged (still used by `MovieEmbeddingService`) |
| `MovieEmbeddingService` | Unchanged |
| Cold-start path (`buildColdStartResponse`) | Unchanged |
| Text-fallback path (`buildTextFallbackResponse`) | Unchanged |
| Atlas vector index & collection schema | Unchanged |

---

## Behavior Differences

| Behavior | Before | After |
|---|---|---|
| Query embedding | `EmbeddingService.embed()` called explicitly | Spring AI handles internally |
| `findSimilarMovies` vector | Uses stored `plot_embedding` vector | Re-embeds `plot` text at query time |
| Self-exclusion in `findSimilarMovies` | Filter passed in `$vectorSearch` | Post-filter on returned documents |

The `findSimilarMovies` re-embed is semantically equivalent (same model, same text source) but adds a small latency cost per call.

---

## Files Modified

1. `pom.xml`
2. `src/main/resources/application.yaml`
3. `src/main/java/com/hackathon/backend/services/VectorSearchService.java`
4. `src/main/java/com/hackathon/backend/services/MovieSearchService.java`
