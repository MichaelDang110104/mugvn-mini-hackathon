# Brain 2 Spec: User Taste

## Purpose

Brain 2 is the personalization brain.

It answers:

- what does this session appear to want right now?
- what does this user tend to like over time?
- how should the candidate pool from Brain 1 be reranked for this specific user?

## Current repo reality

These examples are written to match the current project direction, including one important schema mismatch that the docs now handle explicitly.

- `embedded_movies._id` is an `ObjectId`
- planning docs model `user_events.movieId` as a persisted hex string such as `"573a1390f29313caabcd4135"`
- therefore Brain 2 lookups must convert `user_events.movieId` from string to `ObjectId` before joining to `embedded_movies`

That conversion is one of the most important practical details in these pipelines.

## Collections and fields used

### Collection: `user_events`

Near-runnable assumed document shape:

```json
{
  "_id": "evt_001",
  "eventId": "evt_001",
  "userId": "user_42",
  "sessionId": "sess_abc",
  "eventType": "like",
  "movieId": "573a1390f29313caabcd4135",
  "queryText": null,
  "eventValue": 1,
  "screen": "movie_detail",
  "component": "like_button",
  "metadata": {
    "region": "US",
    "position": 2
  },
  "timestamp": { "$date": "2026-05-17T10:16:00Z" }
}
```

### Collection: `embedded_movies`

Joined fields used by Brain 2:

```json
{
  "_id": { "$oid": "573a1390f29313caabcd4135" },
  "title": "Interstellar",
  "plot": "A team travels through a wormhole...",
  "genres": ["Adventure", "Drama", "Sci-Fi"],
  "poster": "https://...",
  "imdb": { "rating": 8.6 },
  "plot_embedding": [0.012, -0.084, 0.221, 0.009]
}
```

### Derived collections

- `session_profiles`
- `user_profiles`

## MVP design choice

Brain 2 should use MongoDB aggregation for:

- filtering relevant events
- limiting to recent windows
- joining movies
- grouping and summarizing behavior

Brain 2 should use backend Java code for:

- weighted embedding averaging
- final `recentIntentEmbedding`
- final `profileEmbedding`
- any normalization that becomes awkward inside aggregation

That split is deliberate. It keeps phase 1 accurate, explainable, and easier to debug.

## Event weight rules used in examples

These are simple enough to implement now and good enough for MVP behavior.

```text
like = 1.0
save = 0.8
rating >= 4 = 1.0
watch_start = 0.5
click = 0.2
view = 0.1
search = 0.2 (session intent only)
```

## Near-runnable pipeline A: Recent session input builder

### What it does

This pipeline prepares the current session's recent recommendation signals.

It does not compute the final `recentIntentEmbedding` inside MongoDB.
Instead, it returns a clean weighted event stream that backend code can turn into one `session_profiles` document.

### Pipeline

```javascript
db.user_events.aggregate([
  {
    $match: {
      sessionId: SESSION_ID,
      eventType: {
        $in: ["search", "click", "view", "watch_start", "like", "save", "rating"]
      }
    }
  },
  {
    $sort: {
      timestamp: -1
    }
  },
  {
    $limit: 30
  },
  {
    $addFields: {
      eventWeight: {
        $switch: {
          branches: [
            { case: { $eq: ["$eventType", "like"] }, then: 1.0 },
            { case: { $eq: ["$eventType", "save"] }, then: 0.8 },
            {
              case: {
                $and: [
                  { $eq: ["$eventType", "rating"] },
                  { $gte: ["$eventValue", 4] }
                ]
              },
              then: 1.0
            },
            { case: { $eq: ["$eventType", "watch_start"] }, then: 0.5 },
            { case: { $eq: ["$eventType", "search"] }, then: 0.2 },
            { case: { $eq: ["$eventType", "click"] }, then: 0.2 },
            { case: { $eq: ["$eventType", "view"] }, then: 0.1 }
          ],
          default: 0
        }
      },
      movieObjectId: {
        $convert: {
          input: "$movieId",
          to: "objectId",
          onError: null,
          onNull: null
        }
      }
    }
  },
  {
    $lookup: {
      from: "embedded_movies",
      let: {
        movieObjectId: "$movieObjectId"
      },
      pipeline: [
        {
          $match: {
            $expr: {
              $eq: ["$_id", "$$movieObjectId"]
            }
          }
        },
        {
          $project: {
            _id: 1,
            title: 1,
            genres: 1,
            poster: 1,
            imdb: 1,
            plot_embedding: 1
          }
        }
      ],
      as: "movie"
    }
  },
  {
    $unwind: {
      path: "$movie",
      preserveNullAndEmptyArrays: true
    }
  },
  {
    $project: {
      _id: 0,
      sessionId: 1,
      eventType: 1,
      eventValue: 1,
      eventWeight: 1,
      movieId: 1,
      queryText: 1,
      timestamp: 1,
      movie: 1
    }
  }
])
```

### Stage-by-stage explanation

#### `$match`

- restricts the pipeline to one session
- keeps only recommendation-relevant event types

#### `$sort`

- puts newest events first
- this matters because session intent should be shaped by what just happened

#### `$limit`

- caps the working set to the most recent 30 events
- protects latency and keeps the session profile focused

#### `$addFields`

- computes a simple `eventWeight`
- converts string `movieId` into `movieObjectId`

Why the conversion matters:

- `embedded_movies._id` is an `ObjectId`
- `user_events.movieId` is modeled as a string in the planning docs
- a direct `localField` to `foreignField` join would fail or miss matches

#### `$lookup`

- joins any movie-backed events to `embedded_movies`
- keeps search events alive even when they have no `movieId`

Why this form is accurate:

- `$lookup` with `let` and `$expr` is the cleanest way to join across converted types

#### `$unwind`

- flattens the joined movie array
- `preserveNullAndEmptyArrays: true` is important because `search` events may have no joined movie

#### `$project`

- emits a small, practical payload for backend profile construction

### Backend step after this pipeline

The backend should then:

1. keep the ordered recent events
2. extract `recentMovieIds`
3. extract recent non-empty `queryText` values
4. compute `recentIntentEmbedding` from the weighted `movie.plot_embedding` vectors
5. upsert one `session_profiles` document

Expected `session_profiles` shape:

```json
{
  "_id": "sess_abc",
  "sessionId": "sess_abc",
  "recentMovieIds": ["573a1390f29313caabcd4135", "573a1391f29313caabcd5111"],
  "recentQueries": ["space survival"],
  "recentIntentEmbedding": [0.011, -0.122, 0.299, 0.071],
  "lastComputedAt": { "$date": "2026-05-17T10:19:00Z" }
}
```

### Tradeoffs made

- embedding averaging stays in backend code because doing weighted vector math inside aggregation would make the pipeline much harder to read and maintain
- repeated views are not deduped in this exact pipeline; phase 1 can cap them in application code

## Near-runnable pipeline B: Long-term user profile summary

### What it does

This pipeline builds the raw ingredients for `user_profiles`.

It is intentionally practical:

- MongoDB computes grouped summaries and weighted counts
- backend code computes `profileEmbedding`

### Pipeline

```javascript
db.user_events.aggregate([
  {
    $match: {
      userId: USER_ID,
      eventType: {
        $in: ["view", "click", "watch_start", "like", "save", "rating"]
      }
    }
  },
  {
    $addFields: {
      eventWeight: {
        $switch: {
          branches: [
            { case: { $eq: ["$eventType", "like"] }, then: 1.0 },
            { case: { $eq: ["$eventType", "save"] }, then: 0.8 },
            {
              case: {
                $and: [
                  { $eq: ["$eventType", "rating"] },
                  { $gte: ["$eventValue", 4] }
                ]
              },
              then: 1.0
            },
            { case: { $eq: ["$eventType", "watch_start"] }, then: 0.5 },
            { case: { $eq: ["$eventType", "click"] }, then: 0.2 },
            { case: { $eq: ["$eventType", "view"] }, then: 0.1 }
          ],
          default: 0
        }
      },
      positiveMovieId: {
        $cond: [
          {
            $or: [
              { $eq: ["$eventType", "like"] },
              { $eq: ["$eventType", "save"] },
              {
                $and: [
                  { $eq: ["$eventType", "rating"] },
                  { $gte: ["$eventValue", 4] }
                ]
              }
            ]
          },
          "$movieId",
          null
        ]
      },
      movieObjectId: {
        $convert: {
          input: "$movieId",
          to: "objectId",
          onError: null,
          onNull: null
        }
      }
    }
  },
  {
    $lookup: {
      from: "embedded_movies",
      let: {
        movieObjectId: "$movieObjectId"
      },
      pipeline: [
        {
          $match: {
            $expr: {
              $eq: ["$_id", "$$movieObjectId"]
            }
          }
        },
        {
          $project: {
            _id: 1,
            genres: 1,
            plot_embedding: 1
          }
        }
      ],
      as: "movie"
    }
  },
  {
    $unwind: "$movie"
  },
  {
    $facet: {
      topGenres: [
        { $unwind: "$movie.genres" },
        {
          $group: {
            _id: "$movie.genres",
            weight: { $sum: "$eventWeight" }
          }
        },
        {
          $sort: {
            weight: -1,
            _id: 1
          }
        },
        { $limit: 10 },
        {
          $project: {
            _id: 0,
            name: "$_id",
            weight: 1
          }
        }
      ],
      likedMovieIds: [
        {
          $match: {
            positiveMovieId: { $ne: null }
          }
        },
        {
          $group: {
            _id: null,
            ids: { $addToSet: "$positiveMovieId" }
          }
        },
        {
          $project: {
            _id: 0,
            ids: 1
          }
        }
      ],
      recentMovieIds: [
        {
          $sort: {
            timestamp: -1
          }
        },
        {
          $group: {
            _id: null,
            ids: { $push: "$movieId" }
          }
        },
        {
          $project: {
            _id: 0,
            ids: { $slice: ["$ids", 20] }
          }
        }
      ],
      embeddingSeeds: [
        {
          $match: {
            eventWeight: { $gte: 0.5 }
          }
        },
        {
          $project: {
            _id: 0,
            movieId: 1,
            eventWeight: 1,
            plot_embedding: "$movie.plot_embedding"
          }
        }
      ],
      stats: [
        {
          $group: {
            _id: null,
            eventCount: { $sum: 1 },
            lastActiveAt: { $max: "$timestamp" }
          }
        },
        {
          $project: {
            _id: 0,
            eventCount: 1,
            lastActiveAt: 1
          }
        }
      ]
    }
  },
  {
    $project: {
      topGenres: 1,
      likedMovieIds: {
        $ifNull: [
          { $first: "$likedMovieIds.ids" },
          []
        ]
      },
      recentMovieIds: {
        $ifNull: [
          { $first: "$recentMovieIds.ids" },
          []
        ]
      },
      embeddingSeeds: 1,
      stats: {
        $ifNull: [
          { $first: "$stats" },
          {
            eventCount: 0,
            lastActiveAt: null
          }
        ]
      }
    }
  }
])
```

### Stage-by-stage explanation

#### `$match`

- restricts work to one user
- keeps only events relevant to long-term taste

#### `$addFields`

- converts raw events into weighted recommendation signals
- builds `positiveMovieId` so only strong positive interactions feed `likedMovieIds`
- converts string movie ids into `ObjectId` for the join

#### `$lookup`

- joins each event to its movie metadata and embedding
- this is what enables genre summarization and later profile embedding construction

#### `$unwind`

- makes the join one row per event with one movie
- unmatched movie ids are dropped here because a user profile cannot learn from a missing movie document

#### `$facet`

This is the key practical stage.

It lets one pass over the joined events feed several summary outputs:

- `topGenres`
- `likedMovieIds`
- `recentMovieIds`
- `embeddingSeeds`
- `stats`

Why this is a good MVP tradeoff:

- one aggregation call returns all the raw pieces needed for `user_profiles`
- the backend can then finish the final vector math without rereading the event history

#### final `$project`

- flattens facet results into one summary payload
- replaces missing arrays with empty arrays

### Backend step after this pipeline

The backend should:

1. use `embeddingSeeds`
2. compute the weighted average embedding
3. optionally normalize the vector
4. write one `user_profiles` document

Expected `user_profiles` shape:

```json
{
  "_id": "user_42",
  "userId": "user_42",
  "topGenres": [
    { "name": "Sci-Fi", "weight": 2.4 },
    { "name": "Drama", "weight": 1.7 }
  ],
  "likedMovieIds": ["573a1390f29313caabcd4135", "573a1391f29313caabcd5111"],
  "recentMovieIds": ["573a1390f29313caabcd4135"],
  "profileEmbedding": [0.022, -0.151, 0.408, 0.101],
  "lastComputedAt": { "$date": "2026-05-17T10:20:00Z" },
  "stats": {
    "eventCount": 93,
    "lastActiveAt": { "$date": "2026-05-17T10:16:00Z" }
  }
}
```

### Tradeoffs made

- `topThemes` is intentionally omitted from the near-runnable pipeline because `embedded_movies` in the current repo does not clearly expose a canonical `themes` field
- if themes become available later, add another facet branch exactly like `topGenres`
- repeated actions are not fully deduped in MongoDB here; that is acceptable for MVP and can be capped in backend code

## Near-runnable pipeline C: Fast request-time recent behavior read

### What it does

This is the online-serving helper query.

It is intentionally smaller than the materialization pipelines because recommendation requests need a very fast read.

### Pipeline

```javascript
db.user_events.aggregate([
  {
    $match: {
      sessionId: SESSION_ID,
      eventType: {
        $in: ["search", "watch_start", "like", "save", "rating"]
      }
    }
  },
  {
    $sort: {
      timestamp: -1
    }
  },
  {
    $limit: 10
  },
  {
    $project: {
      _id: 0,
      eventType: 1,
      movieId: 1,
      queryText: 1,
      eventValue: 1,
      timestamp: 1
    }
  }
])
```

### Why this exists

- the request-time path should avoid heavy joins when possible
- sometimes the service only needs the latest few strong events to adjust scores quickly

## Required indexes

### `user_events`

```javascript
db.user_events.createIndex({ eventId: 1 }, { unique: true })
db.user_events.createIndex({ userId: 1, timestamp: -1 })
db.user_events.createIndex({ sessionId: 1, timestamp: -1 })
db.user_events.createIndex({ movieId: 1, timestamp: -1 })
```

### `user_profiles`

```javascript
db.user_profiles.createIndex({ userId: 1 }, { unique: true })
```

### `session_profiles`

```javascript
db.session_profiles.createIndex({ sessionId: 1 }, { unique: true })
```

## MVP limits

- Brain 2 works best when enough events exist
- anonymous sessions still rely mainly on Brain 1 plus trending fallback
- this design does not try to learn a full ML model online

That is an intentional phase-1 tradeoff.

## References

- MongoDB Aggregation Pipeline overview
  - https://www.mongodb.com/docs/manual/core/aggregation-pipeline/
- MongoDB `$lookup`
  - https://www.mongodb.com/docs/manual/reference/operator/aggregation/lookup/
- MongoDB `$group`
  - https://www.mongodb.com/docs/manual/reference/operator/aggregation/group/
