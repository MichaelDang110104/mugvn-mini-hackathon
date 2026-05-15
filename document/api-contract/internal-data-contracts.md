# Internal Data Contracts

## 1. Purpose

This document defines the internal persisted and derived data contracts required for the recommendation system.

---

## 2. Persisted Collections

### 2.1 `movies`

```json
{
  "_id": "movie_123",
  "title": "Interstellar",
  "overview": "...",
  "genres": ["Sci-Fi", "Drama"],
  "tags": ["space", "emotional", "time"],
  "releaseYear": 2014,
  "language": "en",
  "posterUrl": "https://...",
  "ratingAvg": 8.7,
  "popularityScore": 0.91,
  "availability": {
    "isAvailable": true,
    "region": "global"
  },
  "embedding": [0.123, 0.456],
  "embeddingModel": "text-embedding-3-large",
  "embeddingVersion": "v1",
  "embeddingUpdatedAt": "2026-05-15T10:00:00Z"
}
```

### 2.2 `users`

```json
{
  "_id": "user_123",
  "sessionId": "session_123",
  "createdAt": "2026-05-15T10:00:00Z",
  "lastSeenAt": "2026-05-15T10:20:00Z"
}
```

### 2.3 `user_events`

```json
{
  "_id": "...",
  "eventId": "evt_123",
  "userId": "user_123",
  "sessionId": "session_123",
  "eventType": "rate",
  "movieId": "movie_123",
  "queryText": null,
  "eventValue": 5,
  "metadata": {
    "source": "movie_detail"
  },
  "timestamp": "2026-05-15T10:30:00Z"
}
```

### 2.4 `user_profiles`

```json
{
  "_id": "user_123",
  "userId": "user_123",
  "topGenres": ["Sci-Fi", "Drama"],
  "topThemes": ["space", "emotional"],
  "likedMovieIds": ["movie_1", "movie_2"],
  "recentMovieIds": ["movie_3", "movie_2"],
  "profileEmbedding": [0.12, 0.98],
  "lastComputedAt": "2026-05-15T10:31:00Z",
  "lastSignalsAppliedAt": "2026-05-15T10:31:00Z"
}
```

### 2.5 `recommendation_logs`

```json
{
  "_id": "...",
  "userId": "user_123",
  "sessionId": "session_123",
  "requestRegion": "global",
  "mode": "personalized",
  "context": {
    "surface": "homepage"
  },
  "candidateSources": {
    "semantic": ["movie_1"],
    "profile": ["movie_2"],
    "collaborative": null
  },
  "candidateMovieIds": ["movie_1", "movie_2"],
  "recommendedMovieIds": ["movie_2", "movie_4"],
  "scores": {
    "movie_2": 0.91,
    "movie_4": 0.82
  },
  "scoreBreakdown": {
    "movie_2": {
      "semantic": 0.4,
      "profile": 0.3,
      "collaborative": null,
      "popularity": 0.21
    }
  },
  "itemReasons": {
    "movie_2": [
      {
        "code": "liked_similar_theme",
        "source": "profile"
      }
    ]
  },
  "reasonCodes": ["liked_similar_theme"],
  "fallbackUsed": false,
  "degradationCause": null,
  "filteredOutMovieIds": ["movie_999"],
  "createdAt": "2026-05-15T10:31:00Z"
}
```

### 2.6 `search_request_logs`

```json
{
  "_id": "...",
  "sessionId": "session_123",
  "requestRegion": "global",
  "query": "mind bending emotional sci-fi",
  "mode": "fallback_text",
  "fallbackUsed": true,
  "degradationCause": "vector_search_unavailable",
  "returnedMovieIds": ["movie_123", "movie_456"],
  "createdAt": "2026-05-15T10:30:30Z"
}
```

---

## 3. Derived Collections

Derived collection names are frozen for implementation and aggregation planning.

### 3.1 Collaborative Neighbor Documents

Collection name: `movie_neighbors` when collaborative support is enabled

```json
{
  "_id": "movie_123",
  "movieId": "movie_123",
  "neighbors": [
    {
      "movieId": "movie_456",
      "score": 0.91,
      "sharedPositiveUserCount": 14
    },
    {
      "movieId": "movie_789",
      "score": 0.87,
      "sharedPositiveUserCount": 11
    }
  ],
  "updatedAt": "2026-05-15T10:00:00Z"
}
```

### 3.2 Trending Output Documents

Collection name: `movie_trending_daily`

```json
{
  "_id": "movie_123",
  "viewCount": 120,
  "clickCount": 40,
  "likeCount": 18,
  "rateCount": 6,
  "saveCount": 10,
  "trendingScore": 234,
  "updatedAt": "2026-05-15T10:00:00Z"
}
```

### 3.3 Editorial Seed Documents

Collection name: `editorial_seed_sets`

```json
{
  "_id": "homepage_default",
  "seedMovieIds": ["movie_101", "movie_205", "movie_330"],
  "region": "global",
  "updatedAt": "2026-05-15T10:00:00Z"
}
```

Editorial seed documents are required when cold-start or degraded fallback behavior depends on curated content.

---

## 4. Internal Contract Rules

- `movies.availability` is required for all user-facing result filtering
- `user_events.eventId` must be unique
- `users.sessionId` must be unique
- `rate` must use an explicit scale of `1` to `5`
- `4` and `5` are positive signals
- `3` is neutral
- `1` and `2` are low ratings and must not be treated as positive preference signals
- `recommendation_logs` must be rich enough to debug serving mode, candidates, reasons, and final outputs
- `search_request_logs` must exist when degraded or fallback search behavior must be auditable
- collaborative neighbor edges must retain enough support evidence to be validated and thresholded safely
- editorial seed documents must exist if cold-start or degraded fallback behavior depends on curated content
- `requestRegion` and filtered-out item tracking must be present in recommendation logs for region-aware auditability
- degraded search and recommendation flows must be auditable through persisted logs that capture fallback mode, degradation cause, and returned IDs

---

## 5. Aggregation Job Inputs And Outputs

### User Profile Aggregation Input

- `user_events`
- `movies`

### Search Logging Output

- `search_request_logs` documents for degraded or fallback search auditability

### User Profile Aggregation Output

- updated `user_profiles` documents

### Collaborative Neighbor Aggregation Input

- `user_events` with strong positive interactions such as `like` and `save`

### Collaborative Neighbor Aggregation Output

- `movie_neighbors` documents keyed by `_id` and `movieId` when collaborative support is enabled

---

## 6. Feasibility Summary

These internal contracts are feasible because they directly mirror the current MongoDB recommendation design and implementation plan. They do not require undocumented services or hidden state outside MongoDB Atlas and the Spring Boot backend.
