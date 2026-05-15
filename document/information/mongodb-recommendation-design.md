# MongoDB Recommendation Engine Design

## 1. Purpose

This document explains how the recommendation engine should work, why MongoDB is central to the architecture, what aggregation pipelines and vector retrieval patterns should be used, and what tradeoffs those design choices introduce.

This is an internal engineering design document for the hackathon project. It is focused on building a recommendation engine that is technically credible, simple enough to ship quickly, and aligned with the event’s explicit MongoDB requirement.

---

## 2. Design Goals

- Make MongoDB visibly central to recommendation behavior
- Support semantic search and similar-item retrieval through MongoDB Vector Search
- Support user-behavior-aware ranking through MongoDB Aggregation Pipeline
- Keep the MVP explainable
- Keep cold-start and degraded-mode behavior deterministic
- Avoid architecture that is too large for a hackathon team

---

## 3. Recommendation Philosophy

The hackathon is not asking for a Netflix-scale platform. It is asking for a credible recommendation engine built with MongoDB.

The correct MVP strategy is a **hybrid recommender**:

- content-based retrieval for semantic similarity
- event-driven preference signals for personalization
- aggregation-based trending and profile derivation
- deterministic fallbacks for cold-start and failure modes

This is stronger than either of these extremes:

- pure vector search with no behavioral learning
- pure collaborative filtering with no cold-start safety

---

## 4. Why MongoDB Fits This Problem

MongoDB is a good fit because it can hold:

- catalog data
- embeddings
- user events
- derived user profiles
- materialized recommendation support documents
- recommendation logs

This lets the team build search, profiling, ranking support, and debugging on one platform.

Important MongoDB-native capabilities:

- `$vectorSearch` for semantic retrieval
- Aggregation Pipeline for profiling, trending, and candidate ranking support
- `$merge` for materialized derived collections
- Change Streams for incremental updates if needed later
- hybrid search support through `$rankFusion` or `$scoreFusion` if adopted later

---

## 5. High-Level Recommendation Flow

### Request Types

The engine supports three primary recommendation flows:

1. search-driven discovery
2. item-to-item recommendations on movie detail pages
3. personalized homepage recommendations

### Serving Flow

1. determine serving mode
   - `personalized`
   - `semantic`
   - `cold_start`
   - `fallback_text`
2. gather candidate movies
3. filter candidates by hard business rules
4. score or rerank candidates
5. attach truthful reason codes
6. log the result for verification and debugging
7. return top N results

---

## 6. Core Collections

### `movies`

Purpose:

- canonical movie catalog
- semantic retrieval source
- item-to-item similarity source

Suggested fields:

```json
{
  "_id": "movie_123",
  "title": "Interstellar",
  "overview": "...",
  "genres": ["Sci-Fi", "Drama"],
  "tags": ["space", "emotional", "time"],
  "releaseYear": 2014,
  "language": "en",
  "posterUrl": "...",
  "ratingAvg": 8.7,
  "popularityScore": 0.91,
  "availability": {
    "isAvailable": true,
    "region": "global"
  },
  "embedding": [0.123, 0.456],
  "embeddingModel": "text-embedding-3-large",
  "embeddingVersion": "v1",
  "embeddingUpdatedAt": "2026-05-14T00:00:00Z"
}
```

### `users`

Purpose:

- anonymous session-backed identity for MVP

Suggested fields:

```json
{
  "_id": "user_123",
  "sessionId": "session_123",
  "createdAt": "2026-05-14T00:00:00Z",
  "lastSeenAt": "2026-05-14T00:10:00Z"
}
```

### `user_events`

Purpose:

- immutable behavioral log
- source of truth for recommendation learning signals

Suggested fields:

```json
{
  "_id": "...",
  "eventId": "evt_123",
  "userId": "user_123",
  "sessionId": "session_123",
  "eventType": "like",
  "movieId": "movie_123",
  "queryText": null,
  "eventValue": 5,
  "metadata": {
    "source": "movie_detail"
  },
  "timestamp": "2026-05-14T00:12:00Z"
}
```

Validation rules:

- `search` requires `queryText`
- `like`, `save`, `view`, `click`, and `rate` require `movieId`
- `eventId` must be unique for idempotency
- `timestamp` may be supplied by the client, but if omitted the backend must assign it during ingest
- `rate` must use an explicit rating scale, for example `1` through `5`, and low ratings must not be treated as positive preference signals

### `user_profiles`

Purpose:

- derived preference state used for recommendation serving

Suggested fields:

```json
{
  "_id": "user_123",
  "userId": "user_123",
  "topGenres": ["Sci-Fi", "Drama"],
  "topThemes": ["space", "emotional"],
  "likedMovieIds": ["movie_1", "movie_2"],
  "recentMovieIds": ["movie_3", "movie_2"],
  "profileEmbedding": [0.12, 0.98],
  "lastComputedAt": "2026-05-14T00:15:00Z",
  "lastSignalsAppliedAt": "2026-05-14T00:15:00Z"
}
```

### `recommendation_logs`

Purpose:

- debugging
- verification
- explainability validation

Suggested fields:

```json
{
  "_id": "...",
  "userId": "user_123",
  "sessionId": "session_123",
  "mode": "personalized",
  "context": {
    "surface": "homepage"
  },
  "candidateMovieIds": ["movie_1", "movie_2"],
  "recommendedMovieIds": ["movie_2", "movie_4"],
  "scores": {
    "movie_2": 0.91,
    "movie_4": 0.82
  },
  "reasonCodes": ["liked_similar_theme"],
  "createdAt": "2026-05-14T00:15:01Z"
}
```

---

## 7. Index Strategy

### Required Indexes

#### `movies`

- Vector Search index on `embedding`
- optional index on `genres`
- optional index on `releaseYear`
- optional index on `popularityScore`
- optional text index for fallback search on `title`, `overview`, and `tags`

#### `users`

- unique index on `sessionId`

#### `user_events`

- unique index on `eventId`
- index on `{ userId: 1, timestamp: -1 }`
- index on `{ sessionId: 1, timestamp: -1 }`
- index on `{ movieId: 1, timestamp: -1 }`

#### `user_profiles`

- unique index on `userId`

#### `recommendation_logs`

- index on `{ userId: 1, createdAt: -1 }`

### Why This Matters

These indexes support:

- fast semantic retrieval
- deterministic session lookups
- profile derivation from recent events
- debugging recommendation output

---

## 8. Serving Modes

### `personalized`

Used when the session has enough recent positive signals.

Candidate sources:

- recent liked movie similarity
- profile affinity to movie metadata
- trending within inferred interests

### `semantic`

Used for explicit discovery queries and item-to-item similarity.

Candidate sources:

- vector similarity from query embedding
- vector similarity from current movie embedding

### `cold_start`

Used for new sessions or profiles with weak signal.

Candidate sources:

- trending titles
- editorial seed set
- diversified genres

### `fallback_text`

Used when vector retrieval or query embedding is unavailable.

Candidate sources:

- text index matches
- curated genre or trending blocks

---

## 9. Candidate Generation Design

The engine should not rank the entire catalog from scratch for every request. It should gather candidate sets first.

### Candidate Source A: Query-To-Movie Semantic Retrieval

Used for search-driven discovery.

Method:

- embed the query
- run `$vectorSearch` on `movies.embedding`
- return top semantic candidates

### Candidate Source B: Movie-To-Movie Semantic Retrieval

Used for similar titles on movie detail page.

Method:

- reuse the active movie embedding
- run `$vectorSearch`
- exclude the current movie ID

### Candidate Source C: Recent Likes Expansion

Used for lightweight personalization.

Method:

- take the 1 to 3 most recent liked movie IDs
- run semantic similarity from each seed
- merge results

### Candidate Source D: Trending By Cohort Or Genre

Used for cold-start and fallback support.

Method:

- aggregate recent engagement from `user_events`
- group by `movieId`
- join to `movies`
- optionally filter by genre cluster or availability

### Candidate Source E: Optional Collaborative Neighbors

Used only if enough time remains.

Method:

- derive item co-occurrence from `user_events`
- surface neighbors of liked movies

This is optional because it is more expensive to design and validate than the content-based MVP path.

### Candidate Source F: Search-Intent Hints

Used for weak session-scoped preference shaping only.

Method:

- inspect recent `search` events for the active session
- extract coarse genre, mood, or theme hints where possible
- apply only a small downstream ranking boost

Constraint:

- search-intent hints must never outweigh explicit positive actions such as `like`, `save`, or `rate`

---

## 10. Ranking Strategy

### MVP Ranking Principle

Use a simple weighted hybrid scorer. Do not try to build a black-box model in the hackathon timeframe.

### Ranking Signals

- semantic similarity
- genre overlap with profile
- tag or theme overlap with profile
- recent interaction affinity
- soft search-intent influence
- popularity support
- diversity penalty
- availability filter

### Example Conceptual Score

```text
finalScore =
  0.45 * semanticSimilarity
  + 0.20 * genreAffinity
  + 0.15 * tagAffinity
  + 0.10 * popularityScore
  + 0.10 * recencyAffinity
  - diversityPenalty
```

These weights are placeholders. The important part is that:

- they are explicit
- they are testable
- they map to reason codes

### Important Constraint

`vectorSearchScore` is not the final business relevance score. It is one retrieval signal. Do not expose it as if it were full recommendation confidence.

---

## 11. Aggregation Pipeline Design

This section defines the MongoDB aggregation patterns the team should use.

### Pipeline A: Trending Movies By Time Window

Purpose:

- produce trending support for cold-start and fallback modules

Input collection:

- `user_events`

Relevant event types:

- `view`
- `click`
- `like`
- `save`
- `rate`

Example pipeline:

```javascript
db.user_events.aggregate([
  {
    $match: {
      eventType: { $in: ["view", "click", "like", "save", "rate"] },
      timestamp: { $gte: ISODate("2026-05-07T00:00:00Z") }
    }
  },
  {
    $group: {
      _id: "$movieId",
      viewCount: {
        $sum: {
          $cond: [{ $eq: ["$eventType", "view"] }, 1, 0]
        }
      },
      clickCount: {
        $sum: {
          $cond: [{ $eq: ["$eventType", "click"] }, 1, 0]
        }
      },
      likeCount: {
        $sum: {
          $cond: [{ $eq: ["$eventType", "like"] }, 1, 0]
        }
      },
      rateCount: {
        $sum: {
          $cond: [{ $eq: ["$eventType", "rate"] }, 1, 0]
        }
      },
      saveCount: {
        $sum: {
          $cond: [{ $eq: ["$eventType", "save"] }, 1, 0]
        }
      }
    }
  },
  {
    $addFields: {
      trendingScore: {
        $add: [
          "$viewCount",
          { $multiply: [2, "$clickCount"] },
          { $multiply: [4, "$likeCount"] },
          { $multiply: [4, "$rateCount"] },
          { $multiply: [3, "$saveCount"] }
        ]
      }
    }
  },
  { $sort: { trendingScore: -1 } },
  { $limit: 100 },
  {
    $merge: {
      into: "movie_trending_daily",
      on: "_id",
      whenMatched: "replace",
      whenNotMatched: "insert"
    }
  }
])
```

What this accomplishes:

- computes a simple engagement-weighted trending list
- materializes results for quick serving
- reduces repeated live aggregation cost during demos

Tradeoffs:

- weights are hand-tuned, not learned
- susceptible to noisy tester traffic unless filtered
- short windows can overreact to one burst of activity

### Pipeline B: User Profile Derivation

Purpose:

- produce `topGenres`, `recentMovieIds`, and other features for personalized reranking

Example pipeline:

```javascript
db.user_events.aggregate([
  {
    $match: {
      userId: "user_123",
      eventType: { $in: ["like", "save", "view", "click", "rate"] }
    }
  },
  {
    $lookup: {
      from: "movies",
      localField: "movieId",
      foreignField: "_id",
      as: "movie"
    }
  },
  { $unwind: "$movie" },
  {
    $unwind: "$movie.genres"
  },
  {
    $group: {
      _id: "$movie.genres",
      signalScore: {
        $sum: {
          $switch: {
            branches: [
              { case: { $eq: ["$eventType", "like"] }, then: 5 },
              { case: { $eq: ["$eventType", "save"] }, then: 4 },
              { case: { $eq: ["$eventType", "rate"] }, then: 4 },
              { case: { $eq: ["$eventType", "click"] }, then: 2 },
              { case: { $eq: ["$eventType", "view"] }, then: 1 }
            ],
            default: 0
          }
        }
      }
    }
  },
  { $sort: { signalScore: -1 } }
])
```

What this accomplishes:

- derives genre affinity from raw events
- weights stronger positive actions more heavily
- gives an explainable profile basis

Tradeoffs:

- genre-level profiling is coarse
- repeated clicks can still distort results if dedupe is weak
- requires clean joins between `user_events` and `movies`

### Pipeline C: Item-Item Co-Occurrence For Optional Collaborative Support

Purpose:

- derive simple `users who liked X also liked Y` signals

Example concept:

```javascript
db.user_events.aggregate([
  {
    $match: {
      eventType: { $in: ["like", "save"] }
    }
  },
  {
    $group: {
      _id: "$userId",
      likedMovies: { $addToSet: "$movieId" }
    }
  },
  {
    $project: {
      pairs: {
        $reduce: {
          input: "$likedMovies",
          initialValue: [],
          in: "$${REMOVE}"
        }
      }
    }
  }
])
```

This pipeline is intentionally incomplete in code form because pair generation logic is bulky and better done in a dedicated batch process if needed. The important engineering point is the pattern:

- group liked items by user
- generate item pairs
- count pair frequency
- persist neighbor lists

Recommended materialized shape:

```json
{
  "movieId": "movie_123",
  "neighbors": [
    { "movieId": "movie_456", "score": 0.91 },
    { "movieId": "movie_789", "score": 0.87 }
  ],
  "updatedAt": "2026-05-14T00:00:00Z"
}
```

Tradeoffs:

- stronger personalization when enough data exists
- weak or misleading on tiny hackathon datasets
- easy to overfit to tester behavior

### Pipeline D: Final Candidate Filtering And Reranking Support

Purpose:

- enforce business constraints after candidate retrieval

Example concept:

```javascript
db.movies.aggregate([
  {
    $match: {
      _id: { $in: ["movie_1", "movie_2", "movie_3"] },
      "availability.isAvailable": true
    }
  },
  {
    $addFields: {
      genreBoost: {
        $cond: [{ $in: ["Sci-Fi", "$genres"] }, 0.2, 0]
      }
    }
  },
  {
    $addFields: {
      finalScore: {
        $add: ["$popularityScore", "$genreBoost"]
      }
    }
  },
  { $sort: { finalScore: -1 } }
])
```

What this accomplishes:

- ensures blocked items are removed
- lets ranking combine retrieval candidates with business logic

Tradeoffs:

- final score calibration can become brittle if too many hand-tuned boosts exist
- must not be mistaken for a full ML ranker

---

## 12. Vector Search Design

### Query Embeddings

Use live query embeddings for natural language search, but with a strict timeout.

If embedding generation fails:

- do not error the user flow
- fall back to text and catalog-based retrieval

### Movie Embeddings

For MVP, store a single primary embedding in `movies.embedding` generated from a concatenated text source such as:

```text
title + genres + tags + overview
```

### Why Store Embeddings In `movies`

Recommended choice:

- store embeddings directly in the `movies` collection

Why:

- simpler serving path
- fewer joins
- easier debugging
- better fit for 500 to 1000 movie demo catalog

Tradeoff:

- larger movie documents
- less separation if multiple embedding variants are added later

### ANN Tuning Guidance

MongoDB guidance indicates `numCandidates >= 20 * limit` as a starting point for ANN retrieval tuning.

Implication for the team:

- do not trust good latency with poor recall
- verify rare-intent query quality against a fixed query set

---

## 13. Cold-Start Design

### New User Cold-Start

Use deterministic fallback, not empty personalization.

Recommended cold-start stack:

- trending score
- editorial starter titles
- genre diversification

Optional enhancement:

- ask the user to pick 1 to 3 genres or moods to bootstrap recommendations

### New Item Cold-Start

New movies can still be recommended because the system is content-based first.

Sources:

- title
- overview
- genres
- tags
- semantic similarity

Tradeoff:

- new items can appear before real engagement proves user interest
- still much safer than collaborative-only systems for an MVP

---

## 14. Fallback Design

### Required Fallback Modes

#### Query embedding failure

Fallback to:

- text search
- genre and tag fallback blocks

#### MongoDB Vector Search unavailable

Fallback to:

- text search
- trending blocks
- editorial or genre starter set

### Search Response Shape Clarification

Search responses should use a single top-level `items` list rather than separate primary and fallback blocks. When semantic retrieval fails or returns weak results, fallback candidates should still be returned in `items`, and the response must make that visible through top-level `mode`, `fallbackUsed`, and optional hint metadata.

#### Personalization unavailable

Fallback to:

- recent-session semantic similarity if possible
- cold-start style trending and genre support

### Why This Matters

The recommendation engine must stay demoable even when the ideal serving path is unavailable.

---

## 15. Explainability Design

### Principle

Explanations must be generated from real ranking inputs.

### Allowed Reason Codes

- `semantic_match_to_search`
- `similar_to_recently_viewed`
- `liked_similar_theme`
- `similar_users_liked`
- `popular_in_preferred_genre`
- `trending_now`
- `editorial_starter_pick`
- `fallback_text_match`

`similar_users_liked` is allowed only when a real collaborative candidate source is implemented and active for the current request. It must not appear in the default MVP path if collaborative support remains optional or disabled.

### Forbidden Behavior

- fabricated explanation text
- collaborative explanation without collaborative signal
- personalized explanation in cold-start mode

### Why This Matters

The easiest way to lose technical credibility is to show reasons that are not grounded in the actual ranking flow.

---

## 16. Tradeoffs In This Design

### Tradeoff 1: Hybrid MVP vs pure collaborative filtering

Chosen:

- hybrid MVP with vector search and event-based reranking

Why:

- stronger cold-start behavior
- easier to explain
- easier to implement with a small dataset

Cost:

- less sophisticated than a mature collaborative system

### Tradeoff 2: Embeddings in `movies` vs separate embeddings collection

Chosen:

- embeddings in `movies`

Why:

- less join complexity
- simpler debug path
- simpler API shaping

Cost:

- larger documents
- less flexibility if many embedding types are added later

### Tradeoff 3: Synchronous lightweight reranking vs background workers

Chosen:

- synchronous lightweight reranking for MVP

Why:

- more deterministic for demo
- no separate worker dependency for critical path

Cost:

- less scalable than a more asynchronous architecture

### Tradeoff 4: Hand-tuned ranking weights vs trained model

Chosen:

- hand-tuned weighted scorer

Why:

- transparent
- fast to ship
- easy to debug

Cost:

- manual tuning
- lower ceiling than learned ranking

### Tradeoff 5: MongoDB-only core vs external ranking stack

Chosen:

- MongoDB-centered serving core

Why:

- aligns strongly with hackathon theme
- smaller architecture
- less integration risk

Cost:

- fewer advanced ML capabilities than a larger feature-store + model-serving system

---

## 17. How Other MongoDB-Based Designs Commonly Work

This design is aligned with common MongoDB recommendation patterns seen in official docs and solution materials:

### Pattern A: Embeddings Stored In The Source Documents

Common approach:

- store vectors directly in the catalog documents
- run `$vectorSearch` first in the pipeline

Tradeoff:

- simplest operational path
- not ideal if many embedding variants are needed

### Pattern B: Aggregation For Derived Recommendation Support Data

Common approach:

- derive trending docs, profile summaries, and item affinities using Aggregation Pipeline and `$merge`

Tradeoff:

- very MongoDB-native and explainable
- still requires careful event hygiene and weighting choices

### Pattern C: Hybrid Search For Entity And Semantic Queries

Common approach:

- combine vector and lexical search when exact entities matter

Tradeoff:

- stronger real-user discovery
- more moving parts and tuning work

### Pattern D: Change Streams Or Stream Processing For Continuous Updates

Common approach:

- react to event writes and update derived state incrementally

Tradeoff:

- fresher recommendation support data
- more operational complexity than this MVP needs

### Why We Are Not Using Every Pattern In MVP

The team should borrow the parts that support the hackathon goal directly:

- vector retrieval
- aggregation-based derived signals
- deterministic fallback logic

The team should avoid optional complexity that is hard to verify quickly:

- full streaming updates
- heavy collaborative modeling
- large multi-stage hybrid fusion unless needed

---

## 18. External References And Concrete Takeaways

### MongoDB Vector Search Overview

Source:

- `https://www.mongodb.com/docs/atlas/atlas-vector-search/`

Takeaway:

- MongoDB is explicitly designed to support vector retrieval directly in Atlas.

### `$vectorSearch` Stage Documentation

Source:

- `https://www.mongodb.com/docs/vector-search/query/aggregation-stages/vector-search-stage/`

Takeaways:

- `$vectorSearch` must be the first stage in the pipeline
- ANN tuning should start with `numCandidates >= 20 * limit`
- filtered vector queries can cost more and reduce recall

### Hybrid Search Documentation

Source:

- `https://www.mongodb.com/docs/vector-search/hybrid-search/hybrid-search/`

Takeaways:

- MongoDB supports hybrid vector + lexical retrieval
- useful when exact title, actor, franchise, or short entity queries matter

### Embedding Storage Guidance

Source:

- `https://www.mongodb.com/docs/vector-search/crud-embeddings/create-embeddings-manual/`

Takeaways:

- embeddings can be stored in the source document
- alternative storage optimizations exist, but simplicity is better for MVP

### Aggregation Framework Documentation

Source:

- `https://www.mongodb.com/docs/manual/aggregation/`
- `https://www.mongodb.com/docs/manual/core/aggregation-pipeline/`

Takeaways:

- Aggregation Pipeline is the native way to derive trending and profile summaries
- `$merge` supports materializing derived data for fast reads

### Data Modeling Guidance

Source:

- `https://www.mongodb.com/docs/manual/data-modeling/`

Takeaway:

- data accessed together should be stored together, which supports storing embeddings in `movies` for this MVP

### Change Streams Documentation

Source:

- `https://www.mongodb.com/docs/manual/changeStreams/`

Takeaway:

- incremental updates are possible later if the team needs fresher profile or trend recomputation

---

## 19. Risks Specific To MongoDB Recommendation Design

- query embeddings can fail and silently force weak fallback experiences
- vector retrieval can appear fast but miss relevant long-tail items if `numCandidates` is too low
- event duplication can poison profile quality
- tiny hackathon datasets can make collaborative patterns misleading
- mixed embedding versions can silently corrupt search quality
- explanation text can become dishonest if it is not tied to actual ranking inputs
- filters such as availability or region can starve vector results if not designed carefully

---

## 20. Final Recommendation

For this hackathon, the team should implement the recommendation engine in this order:

1. clean movie dataset in MongoDB
2. one embedding per movie in `movies.embedding`
3. semantic search via `$vectorSearch`
4. immutable `user_events`
5. aggregation-based user profile derivation
6. weighted hybrid reranking for personalized homepage recommendations
7. truthful reason codes
8. deterministic cold-start and fallback behavior

This is the smallest design that still makes MongoDB clearly central to the recommendation engine rather than merely present in the stack.
