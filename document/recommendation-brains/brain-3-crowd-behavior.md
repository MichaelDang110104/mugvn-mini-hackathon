# Brain 3 Spec: Crowd Behavior

## Purpose

Brain 3 is the collaborative signal brain.

It answers:

- users who liked this movie also liked what?
- which titles appear together in strong positive behavior?
- what crowd patterns should boost discovery beyond pure semantic similarity?

## MVP rule

Brain 3 is still phase 2.

It is useful, but it should not block the MVP because:

- it needs enough positive interaction volume
- it is easier to make noisy than Brain 1 or Brain 2
- the pair-building batch job is more operationally complex than the phase-1 brains

## Current repo-aligned design

The practical design here is intentionally split into two parts.

1. MongoDB aggregation extracts positive movie sets per user
2. backend batch code expands those sets into movie pairs
3. MongoDB aggregation materializes serving-ready `movie_neighbors`

This is more practical than forcing all pair generation into one large aggregation pipeline.

## Collections and fields used

### Input collection: `user_events`

Fields used:

- `userId`
- `sessionId`
- `eventType`
- `movieId`
- `eventValue`
- `timestamp`

### Temporary helper collection: `movie_neighbor_pairs`

Suggested batch output shape after application-side pair expansion:

```json
{
  "movieId": "573a1390f29313caabcd4135",
  "neighborMovieId": "573a1391f29313caabcd5111",
  "sharedPositiveUserCount": 14,
  "score": 0.91,
  "updatedAt": { "$date": "2026-05-17T10:20:00Z" }
}
```

### Serving collection: `movie_neighbors`

```json
{
  "_id": "573a1390f29313caabcd4135",
  "movieId": "573a1390f29313caabcd4135",
  "neighbors": [
    {
      "movieId": "573a1391f29313caabcd5111",
      "score": 0.91,
      "sharedPositiveUserCount": 14
    }
  ],
  "updatedAt": { "$date": "2026-05-17T10:20:00Z" }
}
```

## Positive signal rules

Only strong positive events should feed Brain 3.

Phase-2 rules:

- `like`
- `save`
- `rating` where `eventValue >= 4`
- optionally `watch_start` later, but only if completion/watch quality becomes reliable

Weak events such as `view` and `click` should stay out of Brain 3 because they make collaborative data noisy.

## Near-runnable pipeline A: Positive movie sets per user

### What it does

This pipeline produces the grouped positive movie sets that the batch job will expand into pairs.

### Pipeline

```javascript
db.user_events.aggregate([
  {
    $match: {
      $or: [
        { eventType: "like" },
        { eventType: "save" },
        {
          eventType: "rating",
          eventValue: { $gte: 4 }
        }
      ]
    }
  },
  {
    $group: {
      _id: "$userId",
      movieIds: { $addToSet: "$movieId" },
      lastPositiveAt: { $max: "$timestamp" },
      positiveEventCount: { $sum: 1 }
    }
  },
  {
    $project: {
      _id: 0,
      userId: "$_id",
      movieIds: 1,
      movieCount: { $size: "$movieIds" },
      positiveEventCount: 1,
      lastPositiveAt: 1
    }
  },
  {
    $match: {
      movieCount: { $gte: 2 }
    }
  }
])
```

### Stage-by-stage explanation

#### first `$match`

- keeps only strong positive events
- applies the explicit high-rating threshold inside MongoDB

#### `$group`

- groups one document per user
- collects unique positive `movieIds` with `$addToSet`
- keeps summary stats useful for debugging the batch job

#### first `$project`

- converts `_id` back into a named `userId` field
- computes `movieCount`

#### second `$match`

- keeps only users with at least two unique positive movies
- this avoids generating useless empty pair sets

### Why pair generation is not fully in MongoDB for MVP

This is the key practical tradeoff.

Generating all unique ordered or unordered movie pairs from an array inside a pure aggregation pipeline is possible, but it becomes much harder to read, debug, and evolve.

For MVP-plus work, the cleaner design is:

1. MongoDB returns grouped user movie sets
2. backend batch code creates pairs and support counts
3. MongoDB stores the serving-ready result

That is still a MongoDB-centered architecture because MongoDB handles the source aggregation and the serving materialization.

## Application-side batch step between pipeline A and pipeline B

For each grouped user document:

1. sort the `movieIds`
2. generate all unique unordered pairs
3. increment `sharedPositiveUserCount` for each pair
4. write normalized rows into `movie_neighbor_pairs`

Suggested scoring formula for phase 2:

```text
score = sharedPositiveUserCount / sqrt(movieAPositiveUserCount * movieBPositiveUserCount)
```

Why this is better than raw count alone:

- raw count overfavors globally popular movies
- normalization reduces that popularity bias

## Near-runnable pipeline B: Materialize `movie_neighbors`

### What it does

This pipeline turns pair rows into serving-ready neighbor documents.

### Pipeline

```javascript
db.movie_neighbor_pairs.aggregate([
  {
    $sort: {
      movieId: 1,
      score: -1,
      neighborMovieId: 1
    }
  },
  {
    $group: {
      _id: "$movieId",
      neighbors: {
        $push: {
          movieId: "$neighborMovieId",
          score: "$score",
          sharedPositiveUserCount: "$sharedPositiveUserCount"
        }
      },
      updatedAt: { $max: "$updatedAt" }
    }
  },
  {
    $project: {
      _id: "$_id",
      movieId: "$_id",
      neighbors: { $slice: ["$neighbors", 50] },
      updatedAt: 1
    }
  },
  {
    $merge: {
      into: "movie_neighbors",
      on: "movieId",
      whenMatched: "replace",
      whenNotMatched: "insert"
    }
  }
])
```

### Stage-by-stage explanation

#### `$sort`

- sorts pair rows so the later `neighbors` array is already ordered by best score first

#### `$group`

- collects all neighbors for each source movie into one array
- keeps the most recent `updatedAt`

#### `$project`

- shapes the final serving document
- trims the neighbor list to the top 50 for fast online reads

#### `$merge`

- replaces existing neighbor docs atomically at the document level
- gives the serving collection a stable shape for request-time use

## Near-runnable pipeline C: Request-time collaborative lookup

### What it does

This is the fast online-serving read.

It takes a small set of seed movies, such as recent likes or saves, and produces collaborative candidate scores.

### Pipeline

```javascript
db.movie_neighbors.aggregate([
  {
    $match: {
      movieId: { $in: SEED_MOVIE_IDS }
    }
  },
  {
    $unwind: "$neighbors"
  },
  {
    $group: {
      _id: "$neighbors.movieId",
      collaborativeScore: { $sum: "$neighbors.score" },
      supportCount: { $sum: "$neighbors.sharedPositiveUserCount" }
    }
  },
  {
    $match: {
      _id: { $nin: SEED_MOVIE_IDS }
    }
  },
  {
    $sort: {
      collaborativeScore: -1,
      supportCount: -1,
      _id: 1
    }
  },
  {
    $limit: 30
  }
])
```

### Stage-by-stage explanation

#### `$match`

- reads only the neighbor docs for the seed movies relevant to the current request

#### `$unwind`

- turns each embedded neighbor into its own row

#### `$group`

- if several seed movies point to the same candidate, their scores are combined
- this is what makes a candidate stronger when it is supported by several recent likes

#### second `$match`

- prevents returning the seed movies themselves as recommendations

#### `$sort` and `$limit`

- keep only the strongest collaborative candidates for final reranking

## Required indexes

### `user_events`

```javascript
db.user_events.createIndex({ userId: 1, timestamp: -1 })
db.user_events.createIndex({ eventType: 1, timestamp: -1 })
db.user_events.createIndex({ movieId: 1, timestamp: -1 })
```

### `movie_neighbor_pairs`

```javascript
db.movie_neighbor_pairs.createIndex({ movieId: 1, score: -1 })
db.movie_neighbor_pairs.createIndex({ neighborMovieId: 1 })
```

### `movie_neighbors`

```javascript
db.movie_neighbors.createIndex({ movieId: 1 }, { unique: true })
```

## Phase-2 tradeoffs

- using a helper collection such as `movie_neighbor_pairs` is more operationally explicit than trying to do everything in one aggregation pipeline
- that helper collection is worth it because it makes batch debugging and recomputation much easier
- collaborative data can collapse into popularity if pair scoring is not normalized

## References

- MongoDB Aggregation Pipeline overview
  - https://www.mongodb.com/docs/manual/core/aggregation-pipeline/
- MongoDB `$group`
  - https://www.mongodb.com/docs/manual/reference/operator/aggregation/group/
- MongoDB `$merge`
  - https://www.mongodb.com/docs/manual/reference/operator/aggregation/merge/
