# Unified Recommendation System Implementation Plan

> **For agentic workers:** REQUIRED: Use superpowers:subagent-driven-development (if subagents available) or superpowers:executing-plans to implement this plan. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build a single recommendation API backed by MongoDB that combines semantic retrieval, recent session behavior, lightweight user profiles, and trending fallback for the existing movie app, while leaving collaborative signals as an optional later enhancement.

**Architecture:** The implementation should keep MongoDB Atlas as both the operational data store and the recommendation-serving store. Raw events and movie metadata are persisted directly, while simple aggregation pipelines materialize serving-friendly collections such as `user_profiles`, `session_profiles`, and `movie_trending_daily`. The Spring Boot backend exposes one recommendation endpoint and one batch event-ingest endpoint; the Next.js frontend tracks events, sends them in batches, and renders recommendation results from the unified contract. Phase 1 should prefer simple recent-session reranking over heavier collaborative computation.

**Tech Stack:** Java 21, Spring Boot, Spring Web, Spring Validation, Spring Data MongoDB, Spring AI, MongoDB Atlas, MongoDB Vector Search, MongoDB Aggregation Pipeline, Next.js, React, TypeScript

---

## Overall Architecture Diagram

```text
+-------------------------+
| Next.js Frontend        |
|-------------------------|
| - Home                  |
| - Search                |
| - Movie Detail          |
| - Event tracking queue  |
+------------+------------+
             |
             | 1. GET /api/recommendations
             | 2. POST /api/events/batch
             v
+-------------------------------+
| Spring Boot Backend           |
|-------------------------------|
| - RecommendationController    |
| - EventController             |
| - RecommendationService       |
| - EventIngestService          |
| - SessionProfileService       |
| - UserProfileService          |
| - TrendingCandidateService    |
+---------------+---------------+
                |
                | read/write
                v
+---------------------------------------------------+
| MongoDB Atlas                                     |
|---------------------------------------------------|
| Source-of-truth collections                       |
| - embedded_movies                                 |
| - users                                           |
| - user_events                                     |
|                                                   |
| Derived collections                               |
| - session_profiles                                |
| - user_profiles                                   |
| - movie_trending_daily                            |
|                                                   |
| Audit collections                                 |
| - recommendation_logs                             |
| - search_request_logs                             |
+-------------------+-------------------------------+
                    |
                    | scheduled jobs
                    v
+----------------------------------------------+
| Background Materialization Jobs              |
|----------------------------------------------|
| - SessionProfileAggregationJob               |
| - UserProfileAggregationJob                  |
| - TrendingAggregationJob                     |
| - Optional: Collaborative job in phase 2     |
+----------------------------------------------+

Recommendation flow:
1. Frontend asks for recommendations.
2. Backend reads recent session data, user profile data, semantic candidates, and trending fallback.
3. Backend reranks results and returns one response.

Event flow:
1. Frontend sends tracked events in batches.
2. Backend validates and stores them in `user_events`.
3. Important events refresh session-level recommendation inputs quickly.
4. Heavier profile/trending recomputes happen on a schedule.
```

---

## Recommendation Engine Architecture: The 3 Brains

This diagram focuses only on the recommendation engine itself.

```text
+--------------------------------------------------------------+
| Recommendation Engine                                        |
+---------------------------+----------------------------------+
                            |
                            v
                +-------------------------+
                | Candidate Pool Builder   |
                |-------------------------|
                | Collect movies from:     |
                | - semantic retrieval     |
                | - recent session signals |
                | - user profile matches   |
                | - trending fallback      |
                | - optional collaborative |
                +------------+-------------+
                             |
                             v
+--------------------+  +--------------------+  +------------------------+
| Brain 1            |  | Brain 2            |  | Brain 3                |
| Movie Meaning      |  | User Taste         |  | Crowd Behavior         |
|--------------------|  |--------------------|  |------------------------|
| Input:             |  | Input:             |  | Input:                 |
| - query text       |  | - recent events    |  | - many users' events   |
| - movie text       |  | - liked/saved      |  | - co-watch patterns    |
| - embeddings       |  | - ratings          |  | - co-like patterns     |
|                    |  | - session profile  |  |                        |
| Output:            |  | - user profile     |  | Output:                |
| - semantic score   |  |                    |  | - collaborative score  |
| - similar movies   |  | Output:            |  | - optional neighbors   |
|                    |  | - recent score     |  |                        |
| Phase 1: required  |  | - profile score    |  | Phase 2: optional      |
+---------+----------+  +----------+---------+  +------------+-----------+
          \                       |                         /
           \                      |                        /
            \                     |                       /
             \                    |                      /
              v                   v                     v
                +-----------------------------------+
                | Hybrid Ranker                     |
                |-----------------------------------|
                | Phase 1 score:                    |
                | 0.40 semantic                     |
                | 0.30 recent behavior              |
                | 0.20 profile                      |
                | 0.10 popularity                   |
                |                                   |
                | Phase 2 optional add-on:          |
                | + collaborative score             |
                +----------------+------------------+
                                 |
                                 v
                    +-----------------------------+
                    | Filters + Reason Generator  |
                    |-----------------------------|
                    | - dedupe                    |
                    | - availability filtering    |
                    | - region filtering          |
                    | - reason codes              |
                    +-------------+---------------+
                                  |
                                  v
                    +-----------------------------+
                    | Final Recommendation List   |
                    +-----------------------------+
```

### What The 3 Brains Mean In Simple Terms

- Brain 1 asks: `What is this movie about?`
- Brain 2 asks: `What does this user seem to like right now and overall?`
- Brain 3 asks: `What do similar users tend to like too?`

### How They Fit The MVP

Phase 1 uses:

- Brain 1 strongly
- Brain 2 strongly
- trending fallback as a safety net

Phase 1 does not depend on Brain 3.

Phase 2 can add Brain 3 if:

- enough interaction data exists
- the simpler system is already stable
- the team has time left after the MVP works

### Why This Structure Is Good

- Brain 1 solves search and similar-movie discovery.
- Brain 2 makes the app feel personalized quickly.
- Brain 3 improves discovery later, but does not block the MVP.
- the ranker stays simple because each brain produces a score and the backend combines them.

---

## Compatibility Decisions

These decisions are frozen for this implementation plan so the engineer does not have to guess.

### Existing Search Route Compatibility

The current backend search route is `GET /api/search/movies` in `backend/src/main/java/com/hackathon/backend/controllers/SearchController.java`.

Implementation rule:

- keep `GET /api/search/movies` working during migration
- add `GET /api/recommendations` as the new unified endpoint
- do not remove or rename `GET /api/search/movies` in phase 1
- allow `GET /api/search/movies` to internally reuse recommendation-service logic later

### Existing Secondary Search Route Compatibility

The repo also currently exposes `GET /api/movies/search` in `backend/src/main/java/com/hackathon/backend/controllers/MovieController.java`.

Implementation rule:

- `GET /api/search/movies` is the primary semantic search compatibility route
- `GET /api/movies/search` stays as a lightweight title-search compatibility route in phase 1
- do not expand `GET /api/movies/search` into the main recommendation contract
- if the two routes are consolidated later, that is a separate cleanup step after the unified endpoint is stable

### Existing Movie Collection Compatibility

The current semantic implementation reads from `embedded_movies` and uses `plot_embedding`.

Implementation rule:

- phase 1 continues to read semantic search candidates from `embedded_movies`
- phase 1 does not require a risky full migration from `embedded_movies` to `movies`
- if a normalized `movies` collection is added, it is introduced as a derived or parallel collection with a documented migration path, not an immediate hard switch
- recommendation-serving code must explicitly state whether each read uses `embedded_movies` or a newly introduced `movies` collection

Practical recommendation for MVP:

- keep `embedded_movies` as the semantic retrieval source
- create new recommendation collections such as `user_events`, `user_profiles`, `session_profiles`, `movie_trending_daily`, and `recommendation_logs`
- optionally add a normalized `movies` collection later only if the team needs a cleaner contract layer for frontend-facing movie summaries

### Canonical Event Payload Shape

The frontend currently sends `movieId` and other item identifiers inside `metadata`.

Implementation rule:

- the external batch event contract keeps `movieId` and `queryText` inside `metadata` when posted from the frontend
- the backend ingest layer normalizes these into top-level persisted fields on `user_events`
- validation rules must run against the normalized event shape, not only the raw incoming shape

### Canonical Event Type Names

Implementation rule:

- persisted canonical event types are: `view`, `click`, `search`, `watch_start`, `like`, `save`, `rating`
- do not use `rate` as a stored event type in this codebase
- any documentation or weighting rule that previously said `rate` should be read as `rating`

### Home Page Response Strategy

Implementation rule:

- `GET /api/recommendations` returns a flat `items` list as the base contract
- for `surface=home`, the response also returns lightweight `sections` metadata so the existing homepage can be populated without inventing section structure on the frontend
- search and detail responses may continue using flat item lists only

Why:

- the current home hook expects multiple rows
- forcing the frontend to fabricate home sections from one flat list would create unclear behavior

### Detail Route Compatibility

The current frontend detail page uses `fetchMovieDetail` and expects a detail payload with `similarMovies` and `relatedMovies`.

Implementation rule:

- keep `GET /api/movies/{movieId}` as the detail compatibility route
- extend its backend implementation to reuse the shared recommendation logic for `similarMovies` and `relatedMovies`
- do not force the detail page to switch to the unified endpoint directly in phase 1 if that increases integration risk

---

## Chunk 1: Scope, Constraints, And Why This Design

### What We Are Building

We are not building a generic analytics platform. We are building a recommendation-serving system for the existing movie app in this repository.

The user-facing outcomes are:

- home recommendations that improve after interactions
- search results that use semantic retrieval when query embeddings are available
- detail-page related movies
- truthful fallback behavior when personalization or semantic retrieval is weak
- one recommendation API contract the frontend can rely on
- a simple MVP that works before collaborative filtering is added

### Why The Design Uses Both Raw And Derived MongoDB Collections

This project needs low-latency recommendations and also needs to keep enough evidence to explain them.

That means we should separate data into three groups:

- source-of-truth collections written directly by ingest flows
- derived collections built by aggregation jobs for fast serving
- logs for audit, debugging, and demo evidence

This is better than trying to do all recommendation logic live from `user_events` because:

- live per-request aggregation across all events will get slow quickly
- recommendation behavior becomes harder to debug
- the frontend would wait on work that should have been precomputed

### What We Will Not Do In Phase 1

- no full matrix factorization serving path
- no dependence on the data lake for online recommendations
- no large MongoDB views that recompute heavy recommendation logic on every read
- no attempt to replace the existing search flow before the unified recommendation contract is ready
- no requirement to ship collaborative filtering before the MVP works

### Existing Repo Anchors

The plan is intentionally anchored to the current repo layout:

- backend code lives in `backend/src/main/java/com/hackathon/backend/`
- frontend code lives in `frontend-hackathon/`
- current semantic search lives in `backend/src/main/java/com/hackathon/backend/services/MovieSearchService.java`
- current vector search integration lives in `backend/src/main/java/com/hackathon/backend/services/VectorSearchService.java`
- current event queue and tracking live in:
  - `frontend-hackathon/features/tracking/event-queue.ts`
  - `frontend-hackathon/features/tracking/event-normalizer.ts`
  - `frontend-hackathon/hooks/useTrackEvent.ts`

---

## Chunk 2: MongoDB Data Model

### Source-Of-Truth Collections

#### `embedded_movies`

Purpose:

- canonical movie catalog
- semantic vector retrieval source
- metadata source for ranking and filtering

Fields:

```json
{
  "_id": "movie_123",
  "title": "Interstellar",
  "overview": "A team travels through a wormhole to save humanity.",
  "genres": ["Sci-Fi", "Drama", "Adventure"],
  "themes": ["space", "time", "survival", "family"],
  "cast": ["Matthew McConaughey", "Anne Hathaway"],
  "director": "Christopher Nolan",
  "language": "en",
  "releaseYear": 2014,
  "posterUrl": "https://...",
  "availability": {
    "isActive": true,
    "regions": ["US", "CA", "UK"]
  },
  "qualitySignals": {
    "avgRating": 8.6,
    "ratingCount": 1200000,
    "popularityScore": 0.91
  },
  "plot_embedding": [0.012, -0.203, 0.447, 0.088],
  "embeddingModel": "text-embedding-3-small",
  "embeddingVersion": "v1",
  "embeddingUpdatedAt": "2026-05-17T10:00:00Z"
}
```

Why:

- this matches the current repo and avoids an unnecessary phase-1 collection migration
- the embedding belongs on the movie document because vector search operates over movie content

Optional later:

- introduce a normalized `movies` collection for frontend-safe summaries or denormalized serving use cases if needed

#### `users`

Purpose:

- known user identity
- mapping between authenticated user and one or more sessions

Fields:

```json
{
  "_id": "user_42",
  "userId": "user_42",
  "primarySessionIds": ["sess_abc"],
  "createdAt": "2026-05-17T10:00:00Z",
  "lastSeenAt": "2026-05-17T10:15:00Z"
}
```

Why:

- this keeps the design compatible with both anonymous and signed-in flows

#### `user_events`

Purpose:

- append-only behavioral log
- canonical recommendation-learning event source

Fields:

```json
{
  "_id": "evt_001",
  "eventId": "evt_001",
  "userId": "user_42",
  "sessionId": "sess_abc",
  "eventType": "like",
  "movieId": "movie_123",
  "queryText": null,
  "eventValue": 1,
  "screen": "movie_detail",
  "component": "like_button",
  "itemType": "movie",
  "eventUnit": null,
  "metadata": {
    "region": "US",
    "position": 2,
    "rowTitle": "Recommended For You"
  },
  "timestamp": "2026-05-17T10:16:00Z"
}
```

Why:

- the external event payload can still place `movieId` and `queryText` in `metadata`; the ingest service normalizes those into these persisted fields
- this is the closest thing to the user x movie matrix source data

### Derived Collections

#### `user_profiles`

Purpose:

- long-term known-user taste summary

Fields:

```json
{
  "_id": "user_42",
  "userId": "user_42",
  "topGenres": [
    { "name": "Sci-Fi", "weight": 0.42 },
    { "name": "Drama", "weight": 0.21 }
  ],
  "topThemes": [
    { "name": "space", "weight": 0.33 },
    { "name": "survival", "weight": 0.24 }
  ],
  "likedMovieIds": ["movie_123", "movie_456"],
  "recentMovieIds": ["movie_789", "movie_123"],
  "profileEmbedding": [0.022, -0.151, 0.408, 0.101],
  "lastComputedAt": "2026-05-17T10:20:00Z"
}
```

Why:

- prevents rereading the full event history on each request
- stores the user preference vector for profile-semantic ranking

#### `session_profiles`

Purpose:

- short-term anonymous or near-real-time session intent summary

Fields:

```json
{
  "_id": "sess_abc",
  "sessionId": "sess_abc",
  "recentMovieIds": ["movie_789", "movie_123"],
  "recentQueries": ["space survival"],
  "recentIntentEmbedding": [0.011, -0.122, 0.299, 0.071],
  "lastComputedAt": "2026-05-17T10:19:00Z"
}
```

Why:

- gives session-only recommendation quality without waiting for a full user profile
- aligns with your current frontend session-first design

#### `movie_trending_daily`

Purpose:

- cold start
- region-aware fallback
- popularity stabilization in ranking

Fields:

```json
{
  "_id": "movie_123_US_2026-05-17",
  "movieId": "movie_123",
  "region": "US",
  "day": "2026-05-17",
  "viewCount": 120,
  "clickCount": 40,
  "likeCount": 18,
  "saveCount": 10,
  "highRatingCount": 6,
  "trendingScore": 234.5,
  "updatedAt": "2026-05-17T10:20:00Z"
}
```

Why:

- trend computation is aggregation-friendly and should be materialized

### Audit Collections

#### `recommendation_logs`

Purpose:

- explainability
- debugging
- offline verification

#### `search_request_logs`

Purpose:

- degraded search auditability
- semantic failure evidence

---

## Chunk 3: MongoDB Aggregation And Materialization Strategy

### Recommendation On Views vs Pipelines

Use aggregation pipelines, but keep them simple in phase 1.

Do not rely on MongoDB views for main recommendation serving.

Why:

- views recompute at read time
- recommendation candidate preparation is heavier than simple read projection
- precomputed collections keep the API faster and easier to explain

### Pipelines We Need

#### Pipeline A: User Profile Materialization

Input:

- `user_events`
- `embedded_movies`

Output:

- `user_profiles`

Logic:

1. filter positive and semi-positive events
2. join movies by `movieId`
3. aggregate weighted genre and theme counts
4. collect liked and recent movie ids
5. build the `profileEmbedding` in application code after reading weighted movie embeddings
6. write result back with `$merge` or repository upsert

Why this split:

- MongoDB aggregation is good at grouping and joining metadata
- application code is simpler for vector averaging than complex aggregation expressions

#### Pipeline B: Session Profile Materialization

Input:

- recent `user_events`
- `embedded_movies`

Output:

- `session_profiles`

Logic:

1. read the last N session events
2. weight recent actions by recency and event type
3. collect recent movie ids and queries
4. compute `recentIntentEmbedding`
5. upsert one document per session

Why:

- this makes anonymous recommendation quality better without requiring login

#### Pipeline C: Trending Materialization

Input:

- `user_events`

Output:

- `movie_trending_daily`

Logic:

1. group by movie, region, and day
2. compute counts for views, clicks, likes, saves, and high ratings
3. compute weighted trend score such as:

```text
trendingScore = 0.1 * views + 0.2 * clicks + 0.7 * saves + 1.0 * likes + 1.0 * highRatings
```

4. write top results back to `movie_trending_daily`

Why:

- trend logic is cheap to precompute and useful across all modes

### Scheduling Guidance

- event ingest: synchronous write on every batch
- session profile refresh: near-real-time, triggered after high-value events and periodic batch sweep
- user profile refresh: scheduled every 5 to 10 minutes for active users
- trending refresh: every 5 to 15 minutes
- collaborative neighbor refresh: optional phase 2 job, hourly or nightly only after enough event volume exists

### Simple MVP Trigger Rules

- every interaction: write to `user_events`
- important interactions such as `like`, `save`, `rating >= 4`, and `watch_start`: refresh current-session recommendations
- weaker interactions such as `view`, `click`, and `search`: log immediately and allow debounced recommendation refresh if needed
- repeated watches: first watch full weight, second watch smaller boost, later watches tiny boost or ignore

---

## Chunk 4: Backend File Structure And Responsibilities

### Files To Modify

- Modify: `backend/pom.xml`
- Modify: `backend/src/main/resources/application.yaml`
- Modify: `backend/src/main/java/com/hackathon/backend/config/MongoIndexConfig.java`
- Modify: `backend/src/main/java/com/hackathon/backend/controllers/SearchController.java`
- Modify: `backend/src/main/java/com/hackathon/backend/services/MovieSearchService.java`
- Modify: `backend/src/main/java/com/hackathon/backend/services/VectorSearchService.java`
- Modify: `backend/src/main/java/com/hackathon/backend/controllers/MovieController.java`

### Files To Create

#### DTOs

- Create: `backend/src/main/java/com/hackathon/backend/dto/RecommendationRequest.java`
- Create: `backend/src/main/java/com/hackathon/backend/dto/RecommendationResponse.java`
- Create: `backend/src/main/java/com/hackathon/backend/dto/EventBatchRequest.java`
- Create: `backend/src/main/java/com/hackathon/backend/dto/EventIngestResponse.java`
- Create: `backend/src/main/java/com/hackathon/backend/dto/ErrorResponse.java`

#### Controllers

- Create: `backend/src/main/java/com/hackathon/backend/controllers/RecommendationController.java`
- Create: `backend/src/main/java/com/hackathon/backend/controllers/EventController.java`

#### Models

- Create: `backend/src/main/java/com/hackathon/backend/models/UserEventDocument.java`
- Create: `backend/src/main/java/com/hackathon/backend/models/UserProfileDocument.java`
- Create: `backend/src/main/java/com/hackathon/backend/models/SessionProfileDocument.java`
- Create: `backend/src/main/java/com/hackathon/backend/models/TrendingMovieDocument.java`
- Create: `backend/src/main/java/com/hackathon/backend/models/RecommendationLogDocument.java`

#### Repositories

- Create: `backend/src/main/java/com/hackathon/backend/repositories/UserEventRepository.java`
- Create: `backend/src/main/java/com/hackathon/backend/repositories/UserProfileRepository.java`
- Create: `backend/src/main/java/com/hackathon/backend/repositories/SessionProfileRepository.java`
- Create: `backend/src/main/java/com/hackathon/backend/repositories/TrendingMovieRepository.java`
- Create: `backend/src/main/java/com/hackathon/backend/repositories/RecommendationLogRepository.java`

#### Services

- Create: `backend/src/main/java/com/hackathon/backend/services/ServingModeResolver.java`
- Create: `backend/src/main/java/com/hackathon/backend/services/EventIngestService.java`
- Create: `backend/src/main/java/com/hackathon/backend/services/SessionProfileService.java`
- Create: `backend/src/main/java/com/hackathon/backend/services/UserProfileService.java`
- Create: `backend/src/main/java/com/hackathon/backend/services/TrendingCandidateService.java`
- Create: `backend/src/main/java/com/hackathon/backend/services/RecommendationService.java`
- Create: `backend/src/main/java/com/hackathon/backend/services/RecommendationLoggingService.java`

#### Jobs / Aggregation Components

- Create: `backend/src/main/java/com/hackathon/backend/jobs/UserProfileAggregationJob.java`
- Create: `backend/src/main/java/com/hackathon/backend/jobs/SessionProfileAggregationJob.java`
- Create: `backend/src/main/java/com/hackathon/backend/jobs/TrendingAggregationJob.java`

#### Tests

- Create: `backend/src/test/java/com/hackathon/backend/controllers/RecommendationControllerTest.java`
- Create: `backend/src/test/java/com/hackathon/backend/controllers/EventControllerTest.java`
- Create: `backend/src/test/java/com/hackathon/backend/services/ServingModeResolverTest.java`
- Create: `backend/src/test/java/com/hackathon/backend/services/EventIngestServiceTest.java`
- Create: `backend/src/test/java/com/hackathon/backend/services/RecommendationServiceTest.java`

### Why This Split

- controllers stay thin and focused on HTTP contracts
- services encapsulate recommendation logic and candidate generation
- jobs own materialized collection refreshes
- repositories stay small and collection-specific

---

## Chunk 5: Frontend File Structure And Responsibilities

### Files To Modify

- Modify: `frontend-hackathon/lib/api/client.ts`
- Modify: `frontend-hackathon/features/home/useHomeData.ts`
- Modify: `frontend-hackathon/features/search/useSearchData.ts`
- Modify: `frontend-hackathon/features/movie-detail/useMovieDetail.ts`
- Modify: `frontend-hackathon/features/tracking/event-queue.ts`
- Modify: `frontend-hackathon/features/tracking/event-normalizer.ts`
- Modify: `frontend-hackathon/hooks/useTrackEvent.ts`
- Modify: `frontend-hackathon/hooks/useRecommendationRefresh.ts`

### Files To Create

- Create: `frontend-hackathon/lib/api/contracts.ts`
- Create: `frontend-hackathon/lib/api/mappers.ts`

### Why This Frontend Work Is Minimal

The frontend already contains:

- a session store
- event normalization
- a local queue
- a batch-post abstraction
- home/search/detail hooks

That means the main frontend work is contract alignment, not architectural reinvention.

### Frontend Recommendation Contract Direction

The frontend should eventually consume a unified recommendation item shape and map it to:

- home page sections
- search result grids
- detail page similar/related rows

The backend should keep these compatibility routes during migration:

- `GET /api/search/movies`: primary semantic search compatibility route
- `GET /api/movies/search`: simple title-search compatibility route only
- `GET /api/movies/{id}`: detail compatibility route

Only `GET /api/search/movies` and `GET /api/movies/{id}` should progressively reuse recommendation-service logic in phase 1.

---

## Chunk 6: Endpoint Design

### `GET /api/recommendations`

Purpose:

- one endpoint for home, query-driven, and item-to-item recommendation retrieval

Request params:

- `sessionId` optional when bootstrapping anonymous session
- `userId` optional
- `query` optional
- `movieId` optional
- `surface` optional: `home`, `search`, `detail`
- `limit` optional
- `region` optional
- `trending` optional boolean
- `sort` optional: `hybrid`, `semantic`, `popular`
- `includeDebug` optional boolean

Response shape:

```json
{
  "requestId": "req_9001",
  "sessionId": "sess_abc",
  "userId": "user_42",
  "mode": "personalized",
  "fallbackUsed": false,
  "generatedAt": "2026-05-17T10:30:00Z",
  "items": [
    {
      "movie": {
        "id": "movie_777",
        "title": "Ad Astra",
        "posterUrl": "https://...",
        "genres": ["Sci-Fi", "Drama"],
        "ratingAvg": 7.2
      },
      "score": 0.689,
      "reasons": [
        {
          "code": "SEMANTIC_MATCH",
          "label": "Matches your current sci-fi interest"
        }
      ],
      "debugScores": {
        "semanticScore": 0.88,
        "recentBehaviorScore": 0.65,
        "profileScore": 0.69,
        "collaborativeScore": 0.35,
        "popularityScore": 0.55,
        "finalScore": 0.689
      }
    }
  ]
}
```

For `surface=home`, extend the response with optional section metadata:

```json
{
  "sections": [
    {
      "id": "recommended_for_you",
      "title": "Recommended For You",
      "reasonChip": "Based on your activity",
      "itemIds": ["movie_777", "movie_123"]
    }
  ]
}
```

Why one endpoint:

- simpler frontend integration
- one serving contract to debug
- one place to evolve ranking logic

### `POST /api/events/batch`

Purpose:

- canonical ingest endpoint for frontend interaction tracking

Request body:

```json
{
  "sessionId": "sess_abc",
  "events": [
    {
      "eventId": "evt_001",
      "eventType": "like",
      "screen": "movie_detail",
      "component": "like_button",
      "itemType": "movie",
      "eventValue": "1",
      "eventUnit": null,
      "metadata": {
        "movieId": "movie_123",
        "queryText": null
      },
      "timestamp": "2026-05-17T10:16:00Z"
    }
  ]
}
```

Response body:

```json
{
  "accepted": 1,
  "failed": 0,
  "profileUpdated": true,
  "rerankedUsingRecentEvents": true
}
```

Why batch ingest:

- matches the existing frontend queue design
- reduces request count
- still allows immediate flush for high-value events

Normalization rule inside backend ingest:

- `metadata.movieId` -> persisted `movieId`
- `metadata.queryText` or search `eventValue` -> persisted `queryText`
- controller validation should reject events that cannot produce the canonical persisted fields required by their event type

---

## Chunk 7: Ranking Logic

### Candidate Sources

The recommender should generate candidates from all relevant sources, then union and rerank them.

1. semantic query retrieval
2. detail-page movie similarity retrieval
3. session profile retrieval
4. user profile retrieval
5. trending fallback

Optional phase 2 candidate source:

- collaborative movie neighbors

### Serving Mode Rules

- `semantic`: explicit query or detail-page similarity dominates
- `personalized`: session or user profile signals are strong enough to personalize
- `cold_start`: no meaningful profile/session/query signal exists
- `fallback_text`: semantic query embedding or vector search degraded

### Initial Scoring Formula

```text
finalScore =
  0.40 * semanticScore +
  0.30 * recentBehaviorScore +
  0.20 * profileScore +
  0.10 * popularityScore
```

Why this formula:

- semantic remains strong for search and related movies
- recent behavior gives fast responsiveness
- profile score supports stable personalization
- popularity gives safe fallback and stabilizes sparse cases

Optional phase 2 score:

```text
finalScorePhase2 =
  0.35 * semanticScore +
  0.25 * recentBehaviorScore +
  0.20 * profileScore +
  0.10 * collaborativeScore +
  0.10 * popularityScore
```

### Event Weights For Profile And Session Computation

```text
like = 1.0
save = 0.8
rating >= 4 = 1.0
watch_start = 0.5
click = 0.2
view = 0.1
search = 0.2 for query intent only
```

Why:

- explicit positive feedback should dominate
- passive impressions should have weaker effect

---

## Chunk 8: Step-By-Step Implementation Tasks

### Task 1: Freeze The Unified Contracts

**Files:**

- Modify: `document/api-contract/external-api-contracts.md`
- Modify: `document/api-contract/internal-data-contracts.md`
- Modify: `document/api-contract/event-and-error-contracts.md`
- Modify: `document/api-contract/high-level-design.md`

- [ ] **Step 1: Add the unified recommendation endpoint contract**

Document `GET /api/recommendations` with exact params, modes, and response shape.

- [ ] **Step 2: Add batch event ingest contract**

Document `POST /api/events/batch` using the frontend queue payload shape.

- [ ] **Step 3: Add `session_profiles` and `movie_trending_daily` to internal contracts**

Make the data model explicit before code changes begin.

- [ ] **Step 4: Update high-level design to show raw vs derived collections**

This keeps the docs aligned with implementation.

- [ ] **Step 5: Review the contracts manually against current frontend tracking fields**

Expected outcome: no field mismatch between frontend events and backend ingest docs.

### Task 2: Add Backend Domain Models And Repositories

**Files:**

- Create: `backend/src/main/java/com/hackathon/backend/models/UserEventDocument.java`
- Create: `backend/src/main/java/com/hackathon/backend/models/UserProfileDocument.java`
- Create: `backend/src/main/java/com/hackathon/backend/models/SessionProfileDocument.java`
- Create: `backend/src/main/java/com/hackathon/backend/models/TrendingMovieDocument.java`
- Create: `backend/src/main/java/com/hackathon/backend/models/RecommendationLogDocument.java`
- Create: `backend/src/main/java/com/hackathon/backend/repositories/UserEventRepository.java`
- Create: `backend/src/main/java/com/hackathon/backend/repositories/UserProfileRepository.java`
- Create: `backend/src/main/java/com/hackathon/backend/repositories/SessionProfileRepository.java`
- Create: `backend/src/main/java/com/hackathon/backend/repositories/TrendingMovieRepository.java`
- Create: `backend/src/main/java/com/hackathon/backend/repositories/RecommendationLogRepository.java`

- [ ] **Step 1: Write the failing repository/model smoke tests**

Add tests that bootstrap the application context and verify repositories can be created.

- [ ] **Step 2: Implement the MongoDB documents minimally**

Use focused Lombok-backed documents with only the fields needed by the contracts.

- [ ] **Step 3: Implement repositories with only required access patterns**

Avoid premature custom query methods.

- [ ] **Step 4: Run backend tests**

Run: `./mvnw test`

Expected: repository/model smoke tests pass.

### Task 3: Add Indexes And MongoDB Config

**Files:**

- Modify: `backend/src/main/java/com/hackathon/backend/config/MongoIndexConfig.java`
- Modify: `backend/src/main/resources/application.yaml`

- [ ] **Step 1: Add failing tests or assertions for expected index setup helpers where practical**

- [ ] **Step 2: Add index creation for `user_events`, `user_profiles`, `session_profiles`, `movie_trending_daily`, and `recommendation_logs`**

- [ ] **Step 3: Keep vector index configuration documented in config comments or docs if Atlas manages it externally**

- [ ] **Step 4: Verify application startup**

Run: `./mvnw test`

Expected: startup succeeds with index creation logic in place.

### Task 4: Implement Event Batch Ingest

**Files:**

- Create: `backend/src/main/java/com/hackathon/backend/dto/EventBatchRequest.java`
- Create: `backend/src/main/java/com/hackathon/backend/dto/EventIngestResponse.java`
- Create: `backend/src/main/java/com/hackathon/backend/controllers/EventController.java`
- Create: `backend/src/main/java/com/hackathon/backend/services/EventIngestService.java`
- Create: `backend/src/test/java/com/hackathon/backend/controllers/EventControllerTest.java`
- Create: `backend/src/test/java/com/hackathon/backend/services/EventIngestServiceTest.java`

- [ ] **Step 1: Write the failing controller test for successful batch ingest**

Test payload should match the existing frontend `postEventsBatch` body.

- [ ] **Step 2: Write the failing validation tests**

Cover:

- missing `sessionId`
- missing `eventId`
- `search` without query text
- `like` without movie id

These tests must validate the normalized meaning of the event, for example:

- `search` is invalid if neither `metadata.queryText` nor a normalizable search value exists
- `like` is invalid if `metadata.movieId` cannot be normalized into canonical `movieId`

- [ ] **Step 3: Implement minimal DTO validation and controller wiring**

- [ ] **Step 4: Implement idempotent persistence logic in `EventIngestService`**

Behavior:

- accept safe duplicates
- reject conflicting duplicates
- write only valid events

- [ ] **Step 5: Trigger lightweight session/profile refresh flags for high-value events**

Important events for phase 1:

- `like`
- `save`
- `rating >= 4`
- `watch_start`

Repeated-watch rule for phase 1:

- first watch counts fully
- second watch counts less
- later repeated watches count very little or are ignored

- [ ] **Step 6: Run focused tests**

Run: `./mvnw test -Dtest=EventControllerTest,EventIngestServiceTest`

Expected: ingest and validation tests pass.

### Task 5: Implement Materialization Jobs

**Files:**

- Create: `backend/src/main/java/com/hackathon/backend/jobs/SessionProfileAggregationJob.java`
- Create: `backend/src/main/java/com/hackathon/backend/jobs/UserProfileAggregationJob.java`
- Create: `backend/src/main/java/com/hackathon/backend/jobs/TrendingAggregationJob.java`
- Create: `backend/src/main/java/com/hackathon/backend/jobs/CollaborativeNeighborAggregationJob.java`

- [ ] **Step 1: Write failing service-level tests for each job's core aggregation contract**

- [ ] **Step 2: Implement session profile materialization first**

Reason: this unlocks anonymous personalization early.

- [ ] **Step 3: Implement user profile materialization second**

Reason: profile ranking depends on it.

- [ ] **Step 4: Implement trending materialization third**

Reason: needed for cold start and fallback.

- [ ] **Step 5: Stop here for phase 1 unless the MVP is already stable**

Phase 1 is complete once event ingest, session refresh, user profiles, trending, and unified recommendation serving work correctly.

- [ ] **Step 6: Add collaborative neighbor materialization only if time and event volume allow**

Reason: collaborative filtering is useful, but it is explicitly phase 2.

- [ ] **Step 7: Run backend tests**

Run: `./mvnw test`

Expected: aggregation job tests pass.

### Task 6: Implement Recommendation Service And Endpoint

**Files:**

- Create: `backend/src/main/java/com/hackathon/backend/dto/RecommendationRequest.java`
- Create: `backend/src/main/java/com/hackathon/backend/dto/RecommendationResponse.java`
- Create: `backend/src/main/java/com/hackathon/backend/controllers/RecommendationController.java`
- Create: `backend/src/main/java/com/hackathon/backend/services/ServingModeResolver.java`
- Create: `backend/src/main/java/com/hackathon/backend/services/SessionProfileService.java`
- Create: `backend/src/main/java/com/hackathon/backend/services/UserProfileService.java`
- Create: `backend/src/main/java/com/hackathon/backend/services/TrendingCandidateService.java`
- Create: `backend/src/main/java/com/hackathon/backend/services/RecommendationLoggingService.java`
- Create: `backend/src/main/java/com/hackathon/backend/services/RecommendationService.java`
- Create: `backend/src/test/java/com/hackathon/backend/controllers/RecommendationControllerTest.java`
- Create: `backend/src/test/java/com/hackathon/backend/services/ServingModeResolverTest.java`
- Create: `backend/src/test/java/com/hackathon/backend/services/RecommendationServiceTest.java`

- [ ] **Step 1: Write the failing serving mode tests**

Cover:

- query present -> `semantic`
- movie id present -> `semantic`
- strong session or profile -> `personalized`
- no signal -> `cold_start`
- semantic failure -> `fallback_text`

- [ ] **Step 2: Write the failing controller response shape tests**

Ensure `items`, `mode`, `fallbackUsed`, and `generatedAt` exist.

- [ ] **Step 3: Implement minimal request parsing and serving-mode resolution**

- [ ] **Step 4: Reuse existing `EmbeddingService`, `VectorSearchService`, and `MovieSearchService` logic where possible**

Reason: smallest correct change, avoid duplicated vector search logic.

- [ ] **Step 4a: Make the semantic collection choice explicit in code**

Phase-1 implementation should keep semantic retrieval on `embedded_movies` and must not silently assume a migrated `movies` collection exists.

- [ ] **Step 5: Add candidate generation from session profile, user profile, and trending collections**

- [ ] **Step 6: Implement union, dedupe, filtering, and hybrid weighted reranking**

Phase-1 ranking should only depend on:

- semantic retrieval
- recent session behavior
- user profile affinity
- trending fallback

- [ ] **Step 7: Generate truthful reason codes**

- [ ] **Step 8: Log recommendation decisions**

- [ ] **Step 8a: Keep recommendation refresh behavior simple**

After high-value events, refresh the current session recommendations. Do not trigger full global recomputation on every interaction.

- [ ] **Step 9: Run focused tests**

Run: `./mvnw test -Dtest=RecommendationControllerTest,ServingModeResolverTest,RecommendationServiceTest`

Expected: recommendation tests pass.

### Task 7: Align Search And Detail Flows To Shared Logic

**Files:**

- Modify: `backend/src/main/java/com/hackathon/backend/controllers/SearchController.java`
- Modify: `backend/src/main/java/com/hackathon/backend/services/MovieSearchService.java`
- Modify: `backend/src/main/java/com/hackathon/backend/controllers/MovieController.java`

- [ ] **Step 1: Write failing tests for backwards-compatible search behavior**

- [ ] **Step 2: Route shared ranking pieces through the new recommendation service where appropriate**

- [ ] **Step 3: Preserve the current search-specific response contract during migration**

- [ ] **Step 3a: Preserve `GET /api/movies/{movieId}` detail compatibility**

Update detail flow implementation so `similarMovies` and `relatedMovies` can reuse shared recommendation logic while keeping the current route contract.

- [ ] **Step 4: Run backend tests**

Run: `./mvnw test`

Expected: legacy search still passes while shared logic is reused.

### Task 8: Align Frontend API Client To Real Backend Contracts

**Files:**

- Create: `frontend-hackathon/lib/api/contracts.ts`
- Create: `frontend-hackathon/lib/api/mappers.ts`
- Modify: `frontend-hackathon/lib/api/client.ts`

- [ ] **Step 1: Define exact TypeScript interfaces for event batch and recommendation responses**

- [ ] **Step 2: Replace mock-only client paths with real fetch-based implementations behind small mapping helpers**

- [ ] **Step 3: Preserve mock fallback only if needed behind a clear flag, not mixed into main logic**

- [ ] **Step 4: Run frontend lint**

Run: `npm run lint`

Expected: client contract changes lint cleanly.

### Task 9: Wire Frontend Screens To The Unified Recommendation Endpoint

**Files:**

- Modify: `frontend-hackathon/features/home/useHomeData.ts`
- Modify: `frontend-hackathon/features/search/useSearchData.ts`
- Modify: `frontend-hackathon/features/movie-detail/useMovieDetail.ts`

- [ ] **Step 1: Update home hook to request `surface=home` recommendations**

Use backend-provided `sections` metadata for row titles and grouping.

- [ ] **Step 2: Update search hook to call either existing search endpoint or unified recommendation endpoint with `surface=search` and `query`**

- [ ] **Step 3: Update movie detail hook to request `movieId`-driven related recommendations**

- [ ] **Step 4: Preserve current screen-specific data shapes through local mappers**

- [ ] **Step 5: Run frontend lint**

Run: `npm run lint`

Expected: hooks compile and lint cleanly.

### Task 10: Finish Event Tracking Integration

**Files:**

- Modify: `frontend-hackathon/features/tracking/event-normalizer.ts`
- Modify: `frontend-hackathon/features/tracking/event-queue.ts`
- Modify: `frontend-hackathon/hooks/useTrackEvent.ts`
- Modify: `frontend-hackathon/hooks/useRecommendationRefresh.ts`

- [ ] **Step 1: Align normalized event fields with backend contract exactly**

- [ ] **Step 2: Ensure `sendBeacon` body matches batch contract, not a different shape**

- [ ] **Step 3: Trigger recommendation refresh on high-value events after successful flush**

- [ ] **Step 4: Keep impression dedupe and double-submit prevention logic intact**

- [ ] **Step 5: Run frontend lint**

Run: `npm run lint`

Expected: event pipeline code stays clean after contract alignment.

### Task 11: End-To-End Verification

**Files:**

- Modify: `document/test-verification/verification-and-test-spec.md`

- [ ] **Step 1: Add verification cases for anonymous session bootstrap**

- [ ] **Step 2: Add verification cases for event ingest and recommendation refresh**

- [ ] **Step 3: Add verification cases for semantic degradation to `fallback_text`**

- [ ] **Step 4: Add verification cases showing the MVP still works without collaborative data**

- [ ] **Step 5: Run all verifications**

Run backend: `./mvnw test`

Run frontend: `npm run lint`

Expected: all planned checks pass and the degraded modes are documented.

---

## Chunk 9: Why This Order Works

### Why Event Ingest Comes Before Full Recommendation Serving

Without stable event ingest, there is no trustworthy behavioral data for:

- session profiles
- user profiles
- collaborative neighbors
- trending calculations

### Why Session Profiles Come Before Collaborative Neighbors

Session profiles create visible personalization quickly, even with very little historical data.

Collaborative neighbors are useful, but they need a larger event base and are not required to make the app feel smarter right away. That is why they are phase 2 in this updated plan.

### Why The Unified Endpoint Comes Before Frontend Refactor Cleanup

Once the unified API exists, frontend work becomes mostly contract mapping instead of architecture guessing.

### Why We Keep Aggregation Materialization In MongoDB

This project is explicitly MongoDB-centered. Using `$group`, `$lookup`, and `$merge` for profiles, trending, and neighbors makes MongoDB visibly central to the solution rather than just a passive document store.

For phase 1, keep this as simple as possible:

- `user_events` are always written immediately
- `session_profiles` are refreshed quickly after strong events
- `user_profiles` and `movie_trending_daily` are recomputed on schedules
- collaborative neighbors are delayed until the simpler path is already working

---

## Chunk 10: Commands And Verification Checklist

### Backend

Run local tests:

```bash
./mvnw test
```

Run focused tests:

```bash
./mvnw test -Dtest=EventControllerTest,EventIngestServiceTest
./mvnw test -Dtest=RecommendationControllerTest,ServingModeResolverTest,RecommendationServiceTest
```

### Frontend

```bash
npm run lint
```

### Manual Smoke Checklist

- [ ] open home page without a session and verify session bootstrap behavior
- [ ] search for a natural-language sci-fi query and verify semantic or fallback response
- [ ] open a movie detail page and verify related recommendations are returned
- [ ] like or save a movie and verify events are ingested successfully
- [ ] refresh home recommendations and verify mode and reasons change plausibly
- [ ] inspect `recommendation_logs` and verify candidate sources and final reasons are recorded
- [ ] verify repeated watches do not endlessly amplify one movie's influence

---

## Chunk 11: Implementation Notes For The Engineer

- prefer reusing current semantic search code instead of replacing it wholesale
- prefer small DTOs and document models over giant nested classes
- keep collaborative logic out of the MVP critical path unless the simpler system is already stable
- do not block the MVP on a full user x movie latent factor system
- if the data lake is introduced later, mirror events there after MongoDB persistence; do not put it in the critical online path

Plan complete and saved to `document/plan/2026-05-17-unified-recommendation-implementation-plan.md`. Ready to execute?
