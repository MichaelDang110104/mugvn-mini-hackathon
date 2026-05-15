# Versioning And Compatibility

## 1. Purpose

This document defines how the API and data contracts may evolve without breaking the frontend, verification suite, or recommendation behavior.

---

## 2. External API Compatibility Rules

- existing top-level response fields must not be removed once frontend integration begins
- new response fields may be added only in an additive manner
- serving mode names must remain stable unless all canonical docs are updated together
- fallback behavior must remain representable through the top-level contract

---

## 3. Internal Data Compatibility Rules

- persisted collection changes must not silently break aggregation jobs
- new derived fields are allowed if they do not invalidate existing verification assumptions
- `movies`, `users`, `user_events`, `user_profiles`, `recommendation_logs`, `search_request_logs`, `movie_neighbors`, `movie_trending_daily`, and `editorial_seed_sets` are contract-sensitive collections
- index definitions and embedding model or version changes are compatibility-sensitive

---

## 4. Frozen Fields

These fields should be treated as frozen once frontend and verification work start:

### Search Response

- `items`
- `mode`
- `fallbackUsed`
- `query`
- `hint`

### Shared Recommendation Item Shape

- `movie.id`
- `movie.title`
- `movie.posterUrl`
- `movie.genres`
- `movie.ratingAvg`
- `movie.availability`
- `score`
- `reasons[].code`
- `reasons[].label`

### Serving Mode Enum

- `semantic`
- `personalized`
- `cold_start`
- `fallback_text`

### Recommendation Response

- `items`
- `mode`
- `fallbackUsed`
- `generatedAt`

### Movie Detail Response

- `movie`
- `similarMovies`
- `mode`
- `fallbackUsed`

### Event Ingestion Response

- `accepted`
- `profileUpdated`
- `rerankedUsingRecentEvents`

### Error Response

- `error`

### Event Contract

- `sessionId`
- `eventId`
- `eventType`
- `movieId`
- `queryText`
- `eventValue`
- `metadata`
- `timestamp`

### Event Type Enum

- `search`
- `view`
- `click`
- `like`
- `save`
- `rate`

### Rating Semantics Baseline

- `4` and `5` are positive signals
- `3` is neutral
- `1` and `2` are low ratings and must not be treated as positive preference signals

### Reason Code Enum Baseline

- `semantic_match_to_search`
- `similar_to_recently_viewed`
- `liked_similar_theme`
- `similar_users_liked`
- `popular_in_preferred_genre`
- `trending_now`
- `editorial_starter_pick`
- `fallback_text_match`

### Session Bootstrap Contract

- the mechanism by which a new `sessionId` is issued
- the field, header, or cookie used to return it to the frontend

---

## 5. Change Control Rules

- if external response shape changes, update frontend contract assumptions and verification docs together
- if internal schema changes, update recommendation design and verification docs together
- if collaborative support changes from optional to always-on, update all contract docs and canonical docs together
- if rating semantics change, update event contract, data contract, recommendation design, and verification rules together
- if index definitions change, update implementation, verification, and internal data contracts together
- if `embeddingModel` or `embeddingVersion` changes, update internal contracts and verification rules together

---

## 6. Compatibility Review Checklist

Before changing any contract field, verify:

- does the implementation plan still describe the same behavior?
- does the recommendation design still support the same semantics?
- does the verification spec still test the same shape and behavior?
- does README still summarize the contract accurately enough?

---

## 7. Feasibility Summary

This compatibility model is feasible because the system has a narrow external API surface and a MongoDB-centered internal data surface. Contract drift is manageable as long as changes are synchronized across the canonical docs.
