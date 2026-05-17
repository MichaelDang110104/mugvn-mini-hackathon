# How The 3 Brains Fit Together

## Purpose

This document explains the practical serving design for the recommendation system using:

- MongoDB Atlas Vector Search for Brain 1
- MongoDB Aggregation Pipeline for Brain 2 and Brain 3
- backend Java merge and rerank logic

The important design rule is simple:

- MongoDB should produce candidate sets and summarized signals
- backend code should combine them into one final ranked response

## Phase 1 vs Phase 2

### Phase 1

- Brain 1: required
- Brain 2: required
- trending fallback: required
- Brain 3: optional and normally skipped

### Phase 2

- Brain 3 materialization added
- collaborative score included in final ranking

## Collections by responsibility

### Brain 1

- `embedded_movies`

### Brain 2

- `user_events`
- `session_profiles`
- `user_profiles`

### Brain 3

- `user_events`
- `movie_neighbor_pairs`
- `movie_neighbors`

### Shared fallback and audit

- `movie_trending_daily`
- `recommendation_logs`

## Exact online request flow

Example request shape:

```json
{
  "sessionId": "sess_abc",
  "userId": "user_42",
  "query": "space survival",
  "surface": "home",
  "limit": 20
}
```

### Step 1: Identify request mode

The backend first decides which request type it is handling.

- search request with free-text query
- detail-page recommendation request with current movie id
- homepage recommendation request with no explicit query

Why this matters:

- Brain 1 query construction is different for search and detail pages
- Brain 2 and fallback behavior are similar across all surfaces

### Step 2: Brain 1 generates the candidate pool

#### Search surface

1. embed the query text
2. run the Brain 1 semantic pipeline on `embedded_movies`
3. collect top semantic candidates with `semanticScore`

#### Detail page

1. read the current movie's `plot_embedding`
2. run the Brain 1 similar-movie pipeline
3. collect top semantic candidates with `semanticScore`

#### Homepage without query

1. if `session_profiles.recentIntentEmbedding` exists, use it as a semantic seed
2. else if `user_profiles.profileEmbedding` exists, use it as a semantic seed
3. else skip direct semantic retrieval and rely more on trending plus any recent events

Why Brain 1 runs first:

- it is the broadest and cheapest way to get a catalog-scale candidate pool by meaning
- Brain 2 is better at reranking than full-catalog retrieval

### Step 3: Brain 2 loads personalization signals

The backend then reads:

- the latest strong `user_events` for the current session
- `session_profiles` by `sessionId`
- `user_profiles` by `userId`, if available

What Brain 2 contributes:

- recent behavior boost
- long-term profile boost
- exclusion hints such as movies already heavily consumed in the current session

Why this is a separate step instead of one giant pipeline:

- request-time personalization should stay fast and composable
- precomputed profile collections avoid rereading all events on every request

### Step 4: Brain 3 optionally adds collaborative candidates

If phase 2 is active and enough signal exists:

1. extract seed movie ids from recent likes, saves, or high ratings
2. query `movie_neighbors`
3. collect collaborative candidates with `collaborativeScore`

If phase 2 is not active:

- skip this step entirely

Why this is safe:

- the overall system still works with Brain 1 + Brain 2 + trending

### Step 5: Add trending fallback candidates

The backend reads from `movie_trending_daily`.

This protects:

- cold-start users
- sparse sessions
- degraded embedding service situations
- empty semantic result sets

### Step 6: Merge, dedupe, filter, and rank

At this point the backend may have candidates from:

- Brain 1 semantic retrieval
- Brain 2 session/profile boosts
- Brain 3 collaborative lookup
- trending fallback

The backend should then:

1. union candidate ids from all sources
2. dedupe them
3. remove invalid items
4. attach all partial scores available for each item
5. compute the final score
6. produce explanation reason codes

## Which parts stay in MongoDB vs backend code

### Best done in MongoDB

- vector nearest-neighbor retrieval
- recent event filtering
- user/session grouping
- materialized summary collection writes
- collaborative neighbor materialization

### Best done in backend Java code

- query embedding generation
- weighted vector averaging for session or user profiles
- final candidate merge across sources
- score normalization across brains
- explanation formatting for the API

Why this split is good:

- each layer does the job it is best at
- operational debugging is easier
- the docs stay close to what the repo can actually implement soon

## Practical phase-1 ranking formula

```text
finalScore =
  0.40 * semanticScore +
  0.30 * recentBehaviorScore +
  0.20 * profileScore +
  0.10 * popularityScore
```

### How to interpret it

- `semanticScore`: comes from Brain 1 `$vectorSearch`
- `recentBehaviorScore`: comes from Brain 2 session-aware logic
- `profileScore`: comes from Brain 2 long-term profile logic
- `popularityScore`: comes from `movie_trending_daily`

### Tradeoffs behind this formula

- Brain 1 gets the largest weight because semantic quality is the most reliable signal early on
- recent behavior is stronger than long-term profile because short-term intent changes quickly
- popularity stays small so it can save sparse cases without dominating relevance

## Practical phase-2 ranking formula

```text
finalScore =
  0.35 * semanticScore +
  0.25 * recentBehaviorScore +
  0.20 * profileScore +
  0.10 * collaborativeScore +
  0.10 * popularityScore
```

Why collaborative stays modest:

- collaborative data is valuable, but it is also the easiest source to skew toward popularity or sparse noise

## Exact background job flow

### Job 1: Event ingestion

- write incoming frontend interactions to `user_events`
- enforce idempotency by `eventId`
- normalize `metadata.movieId` and `metadata.queryText` into top-level persisted fields when needed

### Job 2: Session profile refresh

- triggered quickly after strong events such as `like`, `save`, `rating >= 4`, or `watch_start`
- runs the Brain 2 session-input pipeline
- backend computes `recentIntentEmbedding`
- upserts `session_profiles`

### Job 3: User profile rebuild

- runs on a schedule for active users
- runs the Brain 2 user-summary pipeline
- backend computes `profileEmbedding`
- upserts `user_profiles`

### Job 4: Trending rebuild

- aggregates recent events into `movie_trending_daily`

### Job 5: Collaborative rebuild, phase 2 only

- aggregate positive movie sets from `user_events`
- expand pairs in backend batch code
- materialize `movie_neighbors`

## Failure and fallback behavior

### If embedding generation fails

- skip semantic retrieval for that request path
- use text fallback if applicable
- backfill from trending and Brain 2 signals

### If vector search returns no results

- use text fallback for search
- for other surfaces, use Brain 2 plus trending

### If profile collections are stale or missing

- continue with Brain 1 plus trending

### If Brain 3 data is missing

- do nothing special
- the system is explicitly designed to work without it

## Main tradeoffs in this architecture

### Tradeoff 1: precompute profiles instead of reading full history at request time

Why chosen:

- lower latency
- simpler serving code
- better operational predictability

### Tradeoff 2: compute profile embeddings in backend code instead of aggregation

Why chosen:

- weighted vector math is much easier to read and test in Java than inside a large aggregation expression

### Tradeoff 3: phase-2 collaborative pair expansion in backend code

Why chosen:

- pure aggregation pair generation is possible but much less maintainable for MVP-scale implementation

## Final takeaway

The practical design is:

1. Brain 1 finds candidates by meaning
2. Brain 2 personalizes them for the current user or session
3. Brain 3 later adds crowd discovery when the data is mature enough
4. trending ensures the system always has a safe fallback
5. backend Java code is the final decision layer that merges and reranks everything

## References

- MongoDB Atlas Vector Search `$vectorSearch`
  - https://www.mongodb.com/docs/atlas/atlas-vector-search/vector-search-stage/
- MongoDB Aggregation Pipeline overview
  - https://www.mongodb.com/docs/manual/core/aggregation-pipeline/
- MongoDB `$lookup`
  - https://www.mongodb.com/docs/manual/reference/operator/aggregation/lookup/
- MongoDB `$group`
  - https://www.mongodb.com/docs/manual/reference/operator/aggregation/group/
- MongoDB `$merge`
  - https://www.mongodb.com/docs/manual/reference/operator/aggregation/merge/
