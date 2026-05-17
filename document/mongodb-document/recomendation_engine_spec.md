# Final Implementation Document  
## MongoDB-Centered Hybrid Movie Recommendation System

---

## Summary

This document describes a simple, practical movie recommendation system built around MongoDB.

The goal is to make movie discovery feel smart without making the system too complicated.

Instead of relying on only one method, this system uses a **hybrid approach**:

- semantic understanding of movie meaning
- recent user actions
- long-term user taste
- collaborative patterns from other users
- fallback trending/popular movies

This works well because no single signal is perfect. A user may search for one thing, click another, and still have a long-term taste profile that matters. So the system combines several weak signals into one stronger final recommendation score [1].

---

## One-Sentence Memory Aid

**Collect events, build profiles, generate many candidates, filter them, rank them with hybrid signals, explain the results, and log everything** [1].

---

# 1. What This System Does

When someone asks for recommendations, the system:

1. reads the request
2. loads recent user actions
3. loads the user profile if available
4. finds possible movie candidates from several sources
5. merges and filters those candidates
6. scores each movie
7. returns the top results
8. saves a log for debugging and analysis [1]

This is the main online flow of the system [1].

---

# 2. Why This Design Is Good

This design is good because it is:

- easy to explain
- easy to debug
- easy to improve later
- strong enough for a real MVP
- friendly to both new users and active users [1][3]

MongoDB fits this design well because it can store:

- movie catalog data
- embeddings
- user events
- user profiles
- recommendation logs

It also supports vector search and aggregation pipelines, which are central to this system [3].

---

# 3. Main Idea of the Recommender

The recommender does **not** directly scan all movies and magically know the answer.

Instead, it works in two major stages:

## Stage A: Candidate Generation
First, gather a pool of movies that *might* be good.

## Stage B: Reranking
Then score those candidates using several signals and return the top results [1].

This is cleaner and more reliable than using only one retrieval method.

---

# 4. Core Collections

This system uses four main MongoDB collections [1][3].

---

## 4.1 `movies`

This is the main movie catalog.

It stores:

- title
- year
- genres
- themes
- cast
- director
- language
- availability
- popularity score
- embedding [1]

### Why it matters
This collection is used for:

- semantic retrieval
- filtering
- scoring
- popularity fallback [1]

### Example document

```json
{
  "_id": "movie_123",
  "title": "Interstellar",
  "year": 2014,
  "genres": ["Sci-Fi", "Drama", "Adventure"],
  "themes": ["space", "time", "survival", "family"],
  "cast": ["Matthew McConaughey", "Anne Hathaway"],
  "director": "Christopher Nolan",
  "language": "en",
  "availability": {
    "isActive": true,
    "regions": ["US", "CA", "UK"]
  },
  "qualitySignals": {
    "avgRating": 8.6,
    "ratingCount": 1200000,
    "popularityScore": 0.91
  },
  "embedding": [0.012, -0.203, 0.447, 0.088]
}
```

### Simple meaning
The `embedding` is a list of numbers representing the movie’s meaning.  
Movies with similar meanings have similar embeddings [4].

---

## 4.2 `user_events`

This stores raw user actions.

Examples:

- `search`
- `click`
- `view`
- `like`
- `save`
- `rate` [1]

### Why it matters
This is the source of truth for user behavior.

The system uses it to understand:

- what the user is doing now
- what the user likes
- what should be added to the user profile [1]

### Important rule
This collection should be **append-only**.  
Do not overwrite old events [1].

### Example document

```json
{
  "_id": "evt_001",
  "userId": "user_42",
  "sessionId": "sess_abc",
  "eventType": "like",
  "movieId": "movie_123",
  "query": null,
  "rating": null,
  "context": {
    "page": "movie_detail",
    "device": "mobile",
    "region": "US"
  },
  "timestamp": "2026-01-05T12:30:00Z"
}
```

---

## 4.3 `user_profiles`

This stores a summarized version of user taste.

It is built from `user_events` [1].

### It may contain
- top genres
- top themes
- liked movie IDs
- recent movie IDs
- profile embedding [1][4]

### Why it matters
This gives the system a quick long-term taste summary without rereading every event every time [1].

### Example document

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
  "stats": {
    "eventCount": 93,
    "lastActiveAt": "2026-01-05T12:30:00Z"
  },
  "updatedAt": "2026-01-05T12:35:00Z"
}
```

### Simple meaning
The `profileEmbedding` is the system’s compact summary of the user’s taste.  
It can be built by averaging embeddings of movies the user liked [1][4].

---

## 4.4 `recommendation_logs`

This stores a full record of recommendation decisions [1].

### Why it matters
This helps with:

- debugging
- analysis
- explaining why something was recommended
- future evaluation [1]

### Example document

```json
{
  "_id": "rec_9001",
  "requestId": "req_9001",
  "userId": "user_42",
  "sessionId": "sess_abc",
  "context": {
    "page": "home",
    "region": "US",
    "device": "mobile"
  },
  "query": "space movies",
  "candidatePool": [
    {
      "movieId": "movie_123",
      "sources": ["semantic", "profile"],
      "rawScores": {
        "semanticScore": 0.92,
        "recentBehaviorScore": 0.15,
        "profileScore": 0.81,
        "collaborativeScore": 0.36,
        "popularityScore": 0.91
      }
    }
  ],
  "finalRecommendations": [
    {
      "movieId": "movie_123",
      "finalScore": 0.78,
      "rank": 1,
      "reasonCodes": ["SEMANTIC_MATCH", "PROFILE_GENRE_MATCH", "POPULAR_NOW"]
    }
  ]
}
```

---

# 5. What Comes Into the System

A recommendation request may contain:

- `userId` if known
- `sessionId`
- `context`
- optional `query`
- `limit` [1]

### Example request

```json
{
  "userId": "user_42",
  "sessionId": "sess_abc",
  "context": {
    "page": "home",
    "region": "US",
    "device": "mobile"
  },
  "query": "space survival",
  "limit": 5
}
```

### Meaning
- if `userId` exists, use long-term profile
- if only `sessionId` exists, use session behavior
- if `query` exists, semantic retrieval becomes more important [1]

---

# 6. What Comes Out of the System

The recommender returns:

- movie ID
- movie title
- final score
- reason codes
- optional debug scores
- request ID [1]

### Example response

```json
{
  "requestId": "req_9001",
  "userId": "user_42",
  "sessionId": "sess_abc",
  "recommendations": [
    {
      "movieId": "movie_777",
      "title": "Ad Astra",
      "score": 0.689,
      "reasonCodes": [
        "SEMANTIC_MATCH",
        "RECENTLY_SIMILAR",
        "MATCHES_FAVORITE_THEME"
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

---

# 7. Full Pipeline: Step by Step

This is the full system from input to output [1].

---

## Step 1: Request Intake

The service receives the request.

### Input
- `userId`
- `sessionId`
- `context`
- optional `query`
- optional `limit`

### Goal
Understand what kind of recommendation is needed:

- homepage recommendations
- search-style recommendations
- detail-page related recommendations
- session-based recommendations [1]

---

## Step 2: Load User State

The service loads:

- recent `user_events`
- `user_profiles`
- session-only events if no `userId` exists [1]

### Goal
Understand:

- what the user liked before
- what the user touched recently
- what the user may want right now [1]

This is where the system combines:
- short-term intent
- long-term taste

---

## Step 3: Generate Candidate Movies

Now the system creates a **broad pool** of possible movies [1].

It should not rank too early.  
First it gathers enough good options [1].

Candidate movies come from several sources.

---

### 3A. Semantic Retrieval

Use embeddings to find movies close to:

- the query embedding
- recent movie embeddings
- profile embedding [1]

This is powered by MongoDB vector search [2][3].

### Good for
- free-text search
- concept matching
- theme matching
- “movies like this”

### Example
User types:

```text
mind bending emotional sci-fi
```

The backend performs semantic retrieval using MongoDB Vector Search [2].

---

### 3B. Recent Behavior Candidates

Use recent actions like:

- recent clicks
- recent views
- recent likes
- recent saves
- recent high ratings [1]

Then find movies similar to those recent items.

### Good for
- current session intent
- short-term mood
- immediate personalization [1][2]

The happy path says that after a user likes or saves a movie, the recommendation engine immediately re-ranks suggestions using recent session events [2].

---

### 3C. Profile Affinity Candidates

Use long-term profile signals like:

- top genres
- top themes
- liked movie history
- profile embedding [1]

### Good for
- stable personalization
- returning users
- homepage ranking [1]

---

### 3D. Collaborative Candidates

Use patterns such as:

- users who liked the same movies
- movies often liked together
- similar-user support
- item-to-item behavior patterns [1]

### Good for
- discovery
- less obvious recommendations
- “users like you also liked...” [1]

---

### 3E. Fallback / Trending Candidates

If user information is weak or missing, use:

- trending movies
- popular movies in the region
- strong recent engagement movies
- deterministic fallback defaults [1][3]

### Good for
- cold start
- anonymous traffic
- sparse history [1]

---

## Step 4: Union and Dedupe

All candidate lists are merged into one pool [1].

### Rules
- union all candidate IDs
- deduplicate by `movieId`
- keep source labels
- keep raw scores from each source [1]

### Example
If a movie is found by:
- semantic retrieval
- profile retrieval
- collaborative retrieval

it should appear once in the candidate pool, but keep all those signals [1].

---

## Step 5: Filtering

Before ranking, remove movies that should not be shown [1].

### Common filters
- unavailable in region
- inactive movie
- invalid metadata
- already seen, depending on product policy
- duplicated items
- content policy rules [1]

### Example
Remove:
- movies not licensed in the user’s region
- movies with missing embeddings if semantic stage needs them
- movies already watched recently, if your product wants novelty [1]

---

## Step 6: Hybrid Reranking

Now each remaining movie gets a final score using multiple signals [1].

Inputs include:

- semantic score
- recent behavior score
- profile score
- collaborative score
- popularity score [1]

### Goal
Balance:
- what the user wants now
- what the user usually likes
- what similar users liked
- what is broadly popular [1]

---

## Step 7: Select Top N

After scoring:

1. sort by final score descending
2. apply final diversity or business rules if needed
3. return top `N` [1]

You may also add diversity rules such as:
- do not return too many movies from the same franchise
- avoid too many nearly identical results [1]

---

## Step 8: Return Reason Codes

Each recommended movie should include short explanation tags [1].

Examples:
- `SEMANTIC_MATCH`
- `QUERY_MATCH`
- `RECENTLY_SIMILAR`
- `SESSION_MATCH`
- `MATCHES_FAVORITE_GENRE`
- `MATCHES_FAVORITE_THEME`
- `SIMILAR_USERS_LIKED`
- `POPULAR_NOW`
- `FALLBACK_TRENDING` [1]

These reason codes should be tied to real scoring evidence, not made up [1].

---

## Step 9: Log the Recommendation Decision

Store the full decision in `recommendation_logs` [1].

The log should include:
- request data
- user state summary
- candidate sources
- score breakdown
- filters applied
- final ranking
- reason codes [1]

This is critical for:
- debugging bad recommendations
- offline replay
- future evaluation [1]

---

# 8. The Main Formulas

This section explains the formulas in simple terms.

---

## 8.1 Semantic Score

This measures how close the meaning of a movie is to the meaning of:

- the user query
- the profile embedding
- or recent movie embeddings [1]

### Formula

```text
cosine(a, b) = (a · b) / (|a| |b|)
```

### Semantic score

```text
semanticScore = cosine(userVector, movieEmbedding)
```

Where:
- `userVector` can be the query embedding, recent intent embedding, or profile embedding
- `movieEmbedding` is the movie vector [1]

### Simple meaning
If the vectors point in a similar direction, the score is high.

---

## 8.2 Recent Behavior Score

This measures how similar a candidate movie is to movies the user interacted with recently [1].

### Formula

```text
recentBehaviorScore(movie) = sum of [event weight × similarity(candidate, recent movie)]
```

More formally:

```text
recentBehaviorScore(m) = Σ (w_i × sim(m, m_i))
```

Where:
- `m` = candidate movie
- `m_i` = recent interacted movie
- `w_i` = weight of that event [1]

### Example event weights
- search = 0.2
- click = 0.3
- view = 0.5
- save = 0.7
- like = 1.0
- rate >= 4 = 1.0 [1]

### Simple meaning
A strong recent like matters more than a weak click.

---

## 8.3 Profile Embedding

This creates one vector representing the user’s long-term taste [1][4].

### Formula

```text
profileEmbedding = weighted average of liked movie embeddings
```

More formally:

```text
profileEmbedding = (Σ (w_j × e_j)) / (Σ w_j)
```

Where:
- `e_j` = embedding of liked movie `j`
- `w_j` = weight of that feedback [1]

### Simple meaning
Take the movies the user liked, combine their embeddings, and produce one taste vector.

---

## 8.4 Profile Score

This measures how well a movie fits the user’s long-term taste [1].

### Formula

```text
profileScore(movie) =
  α × cosine(profileEmbedding, movieEmbedding)
+ β × genreMatch(movie)
+ γ × themeMatch(movie)
```

Where:
- `α`, `β`, `γ` are weights
- `genreMatch` measures overlap with top genres
- `themeMatch` measures overlap with top themes [1]

### Simple genre match example

```text
genreMatch(movie) = sum of matched genre weights
```

### Simple meaning
A movie scores better if:
- it is close to the user profile vector
- it matches genres the user likes
- it matches themes the user likes [1]

---

## 8.5 Collaborative Score

This measures support from other users’ behavior [1].

### Formula

```text
collaborativeScore(movie) =
  average similarity to liked movies
  + similar user support
```

More formally:

```text
collaborativeScore(m) = (1 / |L|) × Σ sim(m, j) + similarUserSupport(m)
```

Where:
- `L` = set of liked movies
- `j` = one liked movie
- `similarUserSupport(m)` = extra boost from similar users engaging positively with movie `m` [1]

### Simple meaning
This helps recommend movies that often appear together in user behavior patterns.

---

## 8.6 Popularity Score

This is a normalized popularity value between 0 and 1 [1][4].

It can be based on:
- views
- likes
- watch completion
- trend score [1]

### Simple meaning
Popularity helps with cold start and stabilizes ranking.

---

## 8.7 Final Score

This combines all signals into one final score [1].

### Formula

```text
finalScore(movie) =
  w_s × semanticScore
+ w_r × recentBehaviorScore
+ w_p × profileScore
+ w_c × collaborativeScore
+ w_pop × popularityScore
```

### Example weights
- semantic = 0.30
- recent behavior = 0.25
- profile = 0.25
- collaborative = 0.10
- popularity = 0.10 [1]

### Notes
- weights should sum to 1.0
- different pages can use different weights
- search pages may increase semantic weight
- homepage may increase profile weight [1]

---

# 9. Example Walkthrough

This example shows the full path from request to result [1].

---

## User Context

User:
- `userId = user_42`
- `sessionId = sess_abc`
- region = `US`
- query = `"space survival"`

Recent positive behavior:
- liked `Interstellar`
- viewed `The Martian`
- saved `Gravity`

Profile preferences:
- top genres: `Sci-Fi`, `Drama`
- top themes: `space`, `survival`, `future` [1]

---

## Candidate Generation Output

Suppose candidate generation produces:

| Movie | Semantic | Recent | Profile | Collaborative | Popularity |
|---|---:|---:|---:|---:|---:|
| Gravity | 0.95 | 0.80 | 0.72 | 0.40 | 0.76 |
| The Martian | 0.93 | 0.85 | 0.70 | 0.38 | 0.79 |
| Ad Astra | 0.88 | 0.65 | 0.69 | 0.35 | 0.55 |
| Arrival | 0.60 | 0.30 | 0.82 | 0.52 | 0.74 |
| Top Gun | 0.20 | 0.10 | 0.15 | 0.12 | 0.85 |

Now suppose:
- `Gravity` is already seen
- `The Martian` is already seen

After filtering, remaining:
- `Ad Astra`
- `Arrival`
- `Top Gun` [1]

---

## Final Score Calculation

Using weights:
- semantic = 0.30
- recent = 0.25
- profile = 0.25
- collaborative = 0.10
- popularity = 0.10 [1]

### Ad Astra

```text
0.30(0.88) + 0.25(0.65) + 0.25(0.69) + 0.10(0.35) + 0.10(0.55)
= 0.689
```

### Arrival

```text
0.30(0.60) + 0.25(0.30) + 0.25(0.82) + 0.10(0.52) + 0.10(0.74)
= 0.586
```

### Top Gun

```text
0.30(0.20) + 0.25(0.10) + 0.25(0.15) + 0.10(0.12) + 0.10(0.85)
= 0.2195
```

---

## Final Ranking

| Rank | Movie | Final Score | Main Reasons |
|---|---|---:|---|
| 1 | Ad Astra | 0.689 | `SEMANTIC_MATCH`, `RECENTLY_SIMILAR`, `MATCHES_FAVORITE_THEME` |
| 2 | Arrival | 0.586 | `MATCHES_FAVORITE_GENRE`, `SIMILAR_USERS_LIKED` |
| 3 | Top Gun | 0.220 | `POPULAR_NOW` |

### What this shows
- `Ad Astra` wins because it matches current intent and recent behavior well
- `Arrival` is still good because it matches long-term taste
- `Top Gun` is popular, but not relevant enough [1]

---

# 10. Simple End-to-End User Story

A user opens the homepage.

Then:

1. they see featured and recommended movies
2. they search using natural language such as `mind bending emotional sci-fi`
3. backend performs semantic retrieval using MongoDB Vector Search
4. they click a movie and see metadata, similar movies, and recommendation reasons
5. they like or save a movie
6. the behavior event is stored in MongoDB
7. the recommendation engine immediately reranks suggestions using recent session events
8. homepage recommendations improve
9. the UI explains why the new titles are being recommended [2]

This is the intended happy path.

---

# 11. Edge Cases

---

## 11.1 New User, No History

### Problem
No profile, no events.

### Solution
Use:
- trending movies
- popular movies in the user’s region
- new releases
- query-based semantic retrieval if query exists [1]

### Example reason codes
- `FALLBACK_TRENDING`
- `POPULAR_NOW`
- `QUERY_MATCH` [1]

---

## 11.2 Anonymous Session

### Problem
No `userId`, but `sessionId` exists.

### Solution
Use:
- same-session events
- recent clicks and views
- search query
- fallback if the session is empty [1]

### Example reason codes
- `SESSION_MATCH`
- `RECENTLY_SIMILAR`
- `QUERY_MATCH` [1]

---

## 11.3 Sparse History

### Problem
User only has one or two actions.

### Solution
Use a balanced mix of:
- semantic similarity to touched movies
- popularity
- early genre/theme extraction [1]

### Important note
Do not overfit too hard to one single movie [1].

---

## 11.4 Heavy User With Strong History

### Problem
Many signals may conflict.

### Solution
- recent behavior handles current intent
- profile handles stable taste
- collaborative handles discovery
- final ranking balances them [1]

---

## 11.5 No Good Candidates After Filtering

### Problem
Everything got filtered out.

### Solution
Backfill with:
- popular unseen titles
- broader genre matches
- regional trending
- weaker related-title thresholds [1]

---

## 11.6 Missing Embeddings

### Problem
Some movies do not have embeddings yet.

### Solution
- allow metadata-based profile matching
- allow popularity fallback
- exclude from semantic retrieval only if necessary [1]

---

## 11.7 Query Conflicts With User History

### Problem
The query says one thing, but long-term profile says another.

### Solution
- for search-like pages, increase semantic weight
- for homepage, keep stronger profile influence [1]

---

# 12. Reason Codes

Reason codes should be short, clear, and truthful [1][3].

## Recommended reason codes

- `SEMANTIC_MATCH`
- `QUERY_MATCH`
- `RECENTLY_SIMILAR`
- `SESSION_MATCH`
- `PROFILE_VECTOR_MATCH`
- `MATCHES_FAVORITE_GENRE`
- `MATCHES_FAVORITE_THEME`
- `SIMILAR_USERS_LIKED`
- `LIKED_ITEM_NEIGHBOR`
- `POPULAR_NOW`
- `TRENDING_IN_REGION`
- `FALLBACK_TRENDING`
- `NEW_RELEASE_BOOST` [1]

## Rule
Only assign a reason code if the score evidence actually supports it [1].

---

# 13. Logging and Debugging

Good logs are essential [1].

## What to log
- request
- user/session context
- whether profile was found
- recent event count
- candidate count per source
- filters applied
- raw scores
- final scores
- final recommended movies
- reason codes [1]

## Useful debugging questions
1. Did we load the correct profile?
2. Were recent events available?
3. Did candidate generation produce enough movies?
4. Did filtering remove the best results?
5. Did one weight overpower the others?
6. Did popularity dominate personalization?
7. Were reason codes truthful? [1]

---

# 14. Implementation Phases

This is the cleanest build order for the project [1][3].

---

## Phase 1: Foundation

Build:
- `movies`
- `user_events`
- basic ingestion
- basic recommendation API
- fallback/trending recommendations [1]

Goal:
- working end-to-end system even without personalization [1]

---

## Phase 2: User Profiles

Build:
- profile derivation job
- top genres
- top themes
- liked and recent movie lists
- profile embedding [1]

Goal:
- stable personalization for known users [1]

---

## Phase 3: Multi-Source Candidate Generation

Build:
- semantic retrieval
- recent behavior candidates
- profile candidates
- collaborative candidates
- source labels [1]

Goal:
- broader and better candidate pools [1]

---

## Phase 4: Hybrid Reranking

Build:
- scoring features
- weighted final ranker
- filter policies
- reason code generation [1]

Goal:
- better recommendation quality [1]

---

## Phase 5: Logging and Evaluation

Build:
- `recommendation_logs`
- score breakdown logging
- offline analysis jobs
- simple dashboard for candidate counts and filters [1]

Goal:
- measurable and debuggable system [1]

---

## Phase 6: Optimization

Possible improvements:
- tune weights by page type
- add diversity constraints
- add freshness boosts
- add business rules
- train a learned ranker later if needed [1]

Goal:
- improve quality without replacing the architecture [1]

---

# 15. Practical Notes

## Keep online serving simple
The online service should mainly do this:

- load user state
- generate candidates
- filter
- score
- return
- log [1]

Heavy recomputation should stay offline [1].

## Why MongoDB is central
MongoDB supports:
- flexible movie metadata
- event storage
- derived profile documents
- recommendation logs
- vector retrieval
- aggregation pipelines [3]

That makes it a strong fit for this architecture [3].

---

# 16. Final Recap

This recommendation system is based on one simple principle:

> **Use several understandable signals together instead of relying on one complicated signal alone** [1].

## The four collections to remember
- `movies`
- `user_events`
- `user_profiles`
- `recommendation_logs` [1]

## The online flow to remember
1. receive request
2. load user state
3. generate candidates
4. merge and dedupe
5. filter
6. rerank with hybrid score
7. return top N with reason codes
8. log the decision [1]

---

# 17. Final Checklist

## Data
- [ ] Create `movies` with metadata and embeddings
- [ ] Create immutable `user_events`
- [ ] Create derived `user_profiles`
- [ ] Create `recommendation_logs` [1]

## Offline / Derived
- [ ] Build profile derivation job
- [ ] Compute top genres and themes
- [ ] Compute liked and recent movie IDs
- [ ] Compute `profileEmbedding` [1]

## Online Candidate Generation
- [ ] Add semantic retrieval
- [ ] Add recent behavior candidate source
- [ ] Add profile candidate source
- [ ] Add collaborative candidate source
- [ ] Add fallback/trending source [1]

## Filtering and Ranking
- [ ] Union and dedupe candidates
- [ ] Filter unavailable and invalid titles
- [ ] Filter already seen items by policy
- [ ] Compute semantic, recent, profile, collaborative, and popularity scores
- [ ] Combine them with final weighted formula
- [ ] Return top N [1]

## Explainability and Reliability
- [ ] Add reason codes
- [ ] Log candidate pool and final picks
- [ ] Log score breakdown
- [ ] Track source coverage and filter rates
- [ ] Validate cold-start behavior [1]

## Production Readiness
- [ ] Tune weights by page type
- [ ] Add alerting for low candidate counts
- [ ] Add offline evaluation loop
- [ ] Add diversity controls if needed
- [ ] Document filtering policy and score thresholds [1]

---

# Final One-Line Reminder

**Store movies, store events, build profiles, find many candidate movies, score them with hybrid signals, return the best ones, and log everything** [1].

If you want, I can next turn this into:
1. a shorter 1-page version,
2. a technical architecture version for engineers,
3. a MongoDB schema file,
4. or API endpoint specs in markdown.
