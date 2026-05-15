# External API Contracts

## 1. Purpose

This document defines the external application API contracts required for the movie recommendation platform to function.

These contracts are intended for:

- frontend consumers
- end-to-end verification
- backend implementation

---

## 2. Shared Rules

### 2.1 Shared Recommendation Item Shape

```json
{
  "movie": {
    "id": "movie_123",
    "title": "Interstellar",
    "posterUrl": "https://...",
    "genres": ["Sci-Fi", "Drama"],
    "ratingAvg": 8.7,
    "availability": {
      "isAvailable": true,
      "region": "global"
    }
  },
  "score": 0.91,
  "reasons": [
    {
      "code": "semantic_match_to_search",
      "label": "Matches your emotional sci-fi search"
    }
  ]
}
```

### 2.2 Serving Modes

Allowed top-level `mode` values:

- `semantic`
- `personalized`
- `cold_start`
- `fallback_text`

### 2.3 Session Rule

- `sessionId` is required for recommendation-critical flows.
- the backend may accept anonymous session-only callers.
- the frontend should treat `sessionId` as the stable anonymous identity key across search, detail, event, and recommendation flows.

### 2.4 Session Bootstrap Rule

- if a request arrives without `sessionId`, the backend may create a new anonymous session
- when a new anonymous session is created, the backend must return it through the `X-Session-Id` response header
- once issued, the frontend must reuse the same `sessionId` through the `X-Session-Id` request header or an explicitly supported request parameter
- if both `X-Session-Id` and `sessionId` are supplied and disagree, the request must be rejected as a contract violation

---

## 3. `GET /api/movies/search`

### Purpose

Search movies using semantic retrieval with deterministic fallback behavior.

### Query Parameters

- `q`: string, optional for empty-query discovery
- `limit`: integer, optional
- `sessionId`: string, optional if `X-Session-Id` request header is present; may be omitted on the first anonymous bootstrap request
- `region`: string, optional

### Request Headers

- `X-Session-Id`: optional for the first anonymous request, otherwise required unless `sessionId` query parameter is supplied

### Success Response `200`

```json
{
  "items": [
    {
      "movie": {
        "id": "movie_123",
        "title": "Interstellar",
        "posterUrl": "https://...",
        "genres": ["Sci-Fi", "Drama"],
        "ratingAvg": 8.7,
        "availability": {
          "isAvailable": true,
          "region": "global"
        }
      },
      "score": 0.91,
      "reasons": [
        {
          "code": "semantic_match_to_search",
          "label": "Matches your emotional sci-fi search"
        }
      ]
    }
  ],
  "mode": "semantic",
  "fallbackUsed": false,
  "query": "mind bending emotional sci-fi",
  "hint": null
}
```

### Contract Rules

- result candidates must be returned in a single top-level `items` list
- fallback candidates must still appear in `items`
- `query` must reflect the original incoming query string
- `hint` may be used when query reformulation guidance is needed
- unavailable titles must not appear in final `items`

### Failure And Fallback Behavior

- if query embedding fails, return `mode: fallback_text`, `fallbackUsed: true`, and fallback candidates in `items`
- if Vector Search degrades for a non-empty query, still return `200`, set `mode: fallback_text`, set `fallbackUsed: true`, and return fallback candidates in `items`
- if query is empty, use `mode: cold_start` for discovery-style response generation

---

## 4. `GET /api/movies/{movieId}`

### Purpose

Return movie detail plus similar-movie recommendations.

### Path Parameters

- `movieId`: string, required

### Query Parameters

- `sessionId`: string, optional
- `region`: string, optional

### Request Headers

- `X-Session-Id`: optional; if absent on a direct-entry detail-page request, the backend may mint a new anonymous session and return it through the `X-Session-Id` response header

### Success Response `200`

```json
{
  "movie": {
    "id": "movie_123",
    "title": "Interstellar",
    "overview": "...",
    "genres": ["Sci-Fi", "Drama"],
    "posterUrl": "https://...",
    "ratingAvg": 8.7,
    "availability": {
      "isAvailable": true,
      "region": "global"
    }
  },
  "similarMovies": [
    {
      "movie": {
        "id": "movie_456",
        "title": "Arrival",
        "posterUrl": "https://...",
        "genres": ["Sci-Fi", "Drama"],
        "ratingAvg": 8.1,
        "availability": {
          "isAvailable": true,
          "region": "global"
        }
      },
      "score": 0.84,
      "reasons": [
        {
          "code": "similar_to_recently_viewed",
          "label": "Semantically similar to the current movie"
        }
      ]
    }
  ],
  "mode": "semantic",
  "fallbackUsed": false
}
```

### Contract Rules

- `similarMovies` must use the shared recommendation item shape
- current `movieId` must not appear in `similarMovies`
- unavailable similar titles must be filtered out
- `mode` and `fallbackUsed` must be present so the frontend can render degraded detail-page recommendation behavior truthfully
- detail-page recommendation reasons must describe similarity to the current movie unless session-aware signals actually contributed

---

## 5. `POST /api/events`

### Purpose

Capture recommendation-relevant user behavior signals.

### Request Body

```json
{
  "sessionId": "session_123",
  "eventId": "evt_123",
  "eventType": "like",
  "movieId": "movie_123",
  "queryText": null,
  "eventValue": 5,
  "metadata": {
    "source": "movie_detail"
  },
  "timestamp": "2026-05-15T10:30:00Z"
}
```

### Request Rules

- `sessionId` is required
- `eventId` is required
- `eventType` is required
- `search` requires `queryText`
- `like`, `save`, `view`, `click`, and `rate` require `movieId`
- `rate` must use a defined scale such as `1` to `5`
- `rate` must use an integer scale of `1` to `5`
- `4` and `5` are positive signals
- `3` is neutral
- `1` and `2` are low ratings and must not be treated as positive preference signals
- `timestamp` may be omitted by the client; if omitted, the backend must assign one

### Success Response `200`

```json
{
  "accepted": true,
  "profileUpdated": true,
  "rerankedUsingRecentEvents": true
}
```

### Contract Rules

- duplicate `eventId` submissions must be idempotent
- duplicate `eventId` with the same payload must return a safe success or no-op outcome
- duplicate comparison must use `sessionId`, `eventType`, `movieId`, `queryText`, `eventValue`, and normalized idempotency-relevant `metadata`, but must ignore server-assigned `timestamp`
- duplicate `eventId` with a conflicting payload must be rejected as a contract violation
- malformed events must not be persisted
- low rating values must not be treated as positive preference signals
- noisy `view` and `search` events must use canonical coalescing rules rather than raw free-form metadata differences

---

## 6. `GET /api/recommendations`

### Purpose

Return homepage or context-aware recommendations for a session.

### Query Parameters

- `sessionId`: string, optional if `X-Session-Id` request header is present; may be omitted on the first anonymous bootstrap request
- `context`: string, optional, e.g. `homepage`
- `limit`: integer, optional
- `region`: string, optional

### Request Headers

- `X-Session-Id`: optional for the first anonymous request, otherwise required unless `sessionId` query parameter is supplied

### Success Response `200`

```json
{
  "items": [
    {
      "movie": {
        "id": "movie_789",
        "title": "Dune",
        "posterUrl": "https://...",
        "genres": ["Sci-Fi", "Adventure"],
        "ratingAvg": 8.3,
        "availability": {
          "isAvailable": true,
          "region": "global"
        }
      },
      "score": 0.88,
      "reasons": [
        {
          "code": "liked_similar_theme",
          "label": "Because you liked emotional sci-fi"
        }
      ]
    }
  ],
  "mode": "personalized",
  "fallbackUsed": false,
  "generatedAt": "2026-05-15T10:31:00Z"
}
```

### Contract Rules

- `generatedAt` is required on recommendation responses
- `mode` must reflect the actual serving path
- collaborative explanations such as `similar_users_liked` may appear only when collaborative signals truly participated
- cold-start flows must still return non-empty `items`
- when `context=homepage` and `limit` is omitted, the endpoint must default to at least `12` returned items for cold-start-compatible homepage rendering
- when personalization is unavailable or too weak, the endpoint must still return `200` with non-empty fallback-safe `items`
- `fallbackUsed` must remain present and truthful on degraded recommendation responses
- collaborative explanations must not appear in `cold_start` or `fallback_text` responses
- personalized reasons such as `liked_similar_theme` must not appear in `cold_start` responses
- semantic explanations must not appear in `fallback_text` responses unless semantic retrieval actually contributed
- when semantic candidates, profile candidates, or collaborative candidates are partially unavailable, the endpoint must still select one truthful top-level `mode` and surface `fallbackUsed` accordingly
- duplicate movie IDs from merged candidate sources must be removed before final response output

### Mode Precedence Rule

When recommendation results mix multiple candidate sources, top-level `mode` must follow this precedence:

1. `personalized` when profile-driven ranking is the primary homepage recommendation serving path
2. `cold_start` when discovery is driven by empty-query or weak-profile homepage logic
3. `fallback_text` when recommendation generation degraded and the final homepage list is primarily driven by text, curated, or fallback-safe candidate behavior

---

## 7. Feasibility Summary

These contracts are feasible with the current design because:

- external surface area is limited to 4 endpoints
- all top-level response fields are already documented canonically
- recommendation items use one shared shape
- fallback behavior is already defined as single-list and top-level mode-driven
