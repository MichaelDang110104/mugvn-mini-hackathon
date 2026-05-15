# Verification And Test Specification

## 1. Purpose

This document defines how the team will verify that the movie recommendation platform is functionally correct, demo-safe, and technically aligned with the hackathon requirement that the core value comes from a MongoDB-based recommendation engine.

This is an internal engineering document. It is not optimized for presentation. It is optimized for catching mistakes before they appear in the demo, the video, or the final submission.

The verification goal is not only to prove that endpoints respond. The goal is to prove that:

- the recommendation engine behaves logically
- the MongoDB-backed search and ranking flow is working as intended
- fallback behavior is deterministic and visible
- event tracking is not poisoning results
- the demo can still succeed even if one dependency degrades

---

## 2. Verification Principles

- Verify the recommendation engine before polishing the UI.
- Prefer deterministic seeded scenarios over subjective ad hoc testing.
- Verify both happy paths and degraded paths.
- Verify user-facing behavior and internal evidence together.
- Never trust a recommendation just because it looks plausible once.
- Never trust vector similarity scores as proof of recommendation quality.
- Always record whether a result came from `semantic`, `personalized`, `cold_start`, or `fallback_text` mode.
- Every demo-critical scenario must have a known-good expected outcome.

---

## 3. Scope

This specification covers:

- data and environment readiness
- search verification
- event ingestion verification
- user profile derivation verification
- recommendation engine verification
- explainability verification
- fallback and degraded-mode verification
- AWS deployment verification
- demo rehearsal verification

This specification does not cover:

- load testing for internet-scale traffic
- deep ML offline evaluation beyond MVP sanity metrics
- compliance certification work

---

## 4. System Under Test

### Core Components

- Next.js frontend
- Spring Boot backend API
- Spring Data MongoDB
- Spring AI
- MongoDB Atlas
- MongoDB Vector Search on `movies.embedding`
- MongoDB Aggregation Pipeline for trending, profile derivation, and reranking
- Amazon S3 + CloudFront or equivalent static frontend hosting
- AWS App Runner
- Amazon S3 for assets and snapshot artifacts
- AWS Secrets Manager
- Amazon CloudWatch

### Core Collections

- `movies`
- `users`
- `user_events`
- `user_profiles`
- `recommendation_logs`

### Core APIs

- `GET /api/movies/search`
- `GET /api/movies/:movieId`
- `POST /api/events`
- `GET /api/recommendations`

---

## 5. Required Seed Data And Test Fixtures

The team must not rely on arbitrary test data during final verification. Verification must use fixed fixtures.

### Dataset Requirements

- 500 to 1000 movies
- stable IDs across catalog, events, recommendations, and UI
- rich metadata for title, overview, genres, tags, poster URL, rating, popularity
- validated embeddings for all movies
- `embeddingVersion` consistent across the dataset

### Mandatory Demo Personas

Create at least 3 seeded anonymous test personas:

1. `demo_sci_fi`
   - likes cerebral sci-fi, emotional sci-fi, space films
2. `demo_family`
   - likes family, animation, uplifting content
3. `demo_thriller`
   - likes crime, mystery, suspense, detective stories

### Mandatory Query Fixtures

Validate at least these 10 queries before the demo:

- `mind bending emotional sci-fi`
- `movies like Interstellar but more emotional`
- `dark cyberpunk detective`
- `uplifting family movie for weekend`
- `slow mystery thriller`
- `space survival drama`
- `alien invasion but thoughtful`
- `funny animated adventure`
- `courtroom drama 90s`
- `zzzz_nonexistent_query_for_fallback`

### Required Fixture Artifacts

- seeded persona summary
- expected top categories per persona
- expected search categories per query
- expected fallback behavior per failure mode
- validated dataset snapshot identifier

---

## 6. Release Gate

The system is not considered demo-ready unless all of the following are true:

- all P0 tests pass
- all API contract checks pass
- all fallback behavior checks pass
- all demo-critical seeded personas work
- recommendation responses are non-empty in every demo path
- no duplicate or blocked items appear in the final ranked list
- explanations are truthful to the actual ranking signals
- the public deployed demo URL passes a full rehearsal

---

## 7. Test Environments

### Local

Used for quick development checks only.

### Demo Or Staging

Used for full verification and rehearsal.

Requirements:

- same environment variables structure as production-demo
- same MongoDB schema shape
- same index definitions
- same embedding version
- same API response shape

### Production-Demo

Used only for the final recorded walkthrough and final rehearsal.

Requirements:

- frozen 24 hours before submission
- no unverified deploys after freeze
- fixed seeded personas available
- fallback mode tested after freeze

---

## 8. Verification Evidence

Every full verification cycle must capture:

- verification date and owner
- environment name
- dataset snapshot ID
- embedding version
- API version or commit SHA
- screenshots for search and recommendation flows
- sample API responses for seeded scenarios
- list of failures found and resolution status

---

## 9. P0 Test Matrix

These are the tests that must pass before the team can claim demo readiness.

| Area | Test | Expected Outcome |
| --- | --- | --- |
| Search | Semantic query returns relevant results | Top results match expected category or theme |
| Search | Empty query | 200 response with fallback-safe items, no blank screen |
| Search | No-result query | 200 response with fallback-safe items, no blank screen |
| Search | Vector degradation | Response falls back to text and catalog-based items in the same list contract |
| Events | `search` event ingest | Event stored with `queryText` and `eventId` |
| Events | `like` event ingest | Event stored exactly once even on retry |
| Profile | Persona profile derivation | Profile reflects recent likes and top genres |
| Recommendations | Cold-start recommendation | Non-empty feed from deterministic cold-start logic |
| Recommendations | After 2 to 3 likes | Homepage recommendations visibly change |
| Recommendations | Availability filtering | Unavailable items never appear in final response |
| Explainability | Reason codes | Each item has truthful reason codes |
| Deployment | Public demo URL | Full rehearsal succeeds end-to-end |

---

## 10. Search Verification

### Purpose

Search verification proves that MongoDB Vector Search and its fallbacks behave correctly and consistently for demo-critical queries.

### Expected Response Contract

`GET /api/movies/search` must return:

- `items`
- `mode`
- `query`
- `fallbackUsed`
- optional `hint`

Each item must contain:

- `movie.id`
- `movie.title`
- `score`
- `reasons`

### Search Tests

#### Test S1: Exact semantic intent

Input:

- query: `mind bending emotional sci-fi`

Expected outcome:

- response status is `200`
- `mode` is `semantic`
- at least 8 results returned if `limit=8`
- top results include mostly sci-fi or adjacent emotional/space/introspective titles
- no clearly unrelated categories dominate the top 5

#### Test S2: Similarity phrasing query

Input:

- query: `movies like Interstellar but more emotional`

Expected outcome:

- response status is `200`
- results contain semantically adjacent titles
- exact title matching is not required, but theme similarity must be visible
- if lexical match helps, hybrid behavior must not overwhelm semantic intent

#### Test S3: Typo-like or weak lexical query

Input:

- query: `intrestellar emotional space`

Expected outcome:

- response status is `200`
- no server error
- either semantic results or visible fallback candidates in the returned `items` list
- UI never renders an empty broken state

#### Test S4: Empty query

Input:

- query: empty string

Expected outcome:

- response status is `200`
- `mode` is `cold_start`
- response returns trending or editorial content
- no vector query is required for success

#### Test S5: No-result query

Input:

- query: `zzzz_nonexistent_query_for_fallback`

Expected outcome:

- response status is `200`
- primary semantic results may be empty
- fallback candidates must still be present in the returned `items` list
- response must explain fallback through `mode` or `fallbackUsed`

#### Test S6: Vector Search unavailable

Condition:

- simulate unavailable or disabled vector retrieval

Expected outcome:

- response status remains `200`
- `mode` becomes `fallback_text` or equivalent
- fallback results still render a useful experience
- no recommendation item falsely claims semantic similarity from unavailable vector search

### Search Acceptance Criteria

- all 10 mandatory query fixtures return stable results or stable fallbacks
- no query returns `500`
- no query returns a blank screen
- vector and fallback modes are distinguishable in logs and API response

---

## 11. Event Ingestion Verification

### Purpose

Event verification proves the recommendation engine is learning from clean, deduplicated signals instead of noisy or duplicated events.

### Event Contract Requirements

Each event must include:

- `eventId`
- `sessionId`
- `eventType`
- persisted `timestamp`
- `userId` if available
- `movieId` for movie interaction events
- `queryText` for `search` events

`timestamp` may be supplied by the client, but if omitted the backend must generate it during ingest. Verification must assert that every stored event has a valid timestamp regardless of request shape.

### Event Validation Rules

- `search` requires `queryText`
- `like`, `save`, `view`, `click`, and `rate` require `movieId`
- `eventId` is mandatory for idempotency

### Event Tests

#### Test E1: Search event recorded

Action:

- submit search query from seeded session

Expected outcome:

- one `search` event written
- `queryText` is present
- `eventId` is stored
- event appears once only

#### Test E2: Like event deduplicated

Action:

- submit the same `like` event twice with identical `eventId`

Expected outcome:

- first write accepted
- second write ignored or treated idempotently
- stored event count remains `1`

#### Test E3: Re-render does not double-count

Action:

- refresh UI or trigger client re-render

Expected outcome:

- duplicate `view` events are coalesced per design, and duplicate `click` events are either prevented or handled through strict idempotency rules
- profile is not over-weighted by UI noise

#### Test E4: Invalid schema rejected

Action:

- submit `search` event without `queryText`

Expected outcome:

- response status is `400`
- error body is structured
- event is not stored

### Event Acceptance Criteria

- duplicate protection works for `like`, `save`, and `rate`
- malformed events do not enter the recommendation pipeline
- event logs can reconstruct a full demo session in order

---

## 12. User Profile Verification

### Purpose

Profile verification proves that recent session behavior becomes usable recommendation state.

### Profile Expectations

`user_profiles` should reflect:

- top genres
- recent movie IDs
- liked movie IDs
- last update time
- optional profile embedding if implemented

### Profile Tests

#### Test P1: Empty profile for cold start

Input:

- new session with no events

Expected outcome:

- no broken profile computation
- cold-start path chosen safely

#### Test P2: Profile updates after 3 likes

Input:

- seeded session likes 3 sci-fi titles

Expected outcome:

- `topGenres` includes sci-fi or related cluster
- `recentMovieIds` includes the liked titles
- update happens within one refresh of recommendations

#### Test P3: Search events do not dominate profile incorrectly

Input:

- many searches but no likes

Expected outcome:

- profile may infer soft interests
- recommendation mode remains conservative
- search text alone does not completely override actual positive interactions

---

## 13. Recommendation Engine Verification

### Purpose

This is the most important verification section. It proves that the MongoDB-centered recommendation engine behaves logically and that ranking changes are driven by valid data.

### Required Recommendation Response Fields

`GET /api/recommendations` must return:

- `items`
- `mode`
- `generatedAt`
- `fallbackUsed`

Each item must include:

- `movie`
- `score`
- `reasons`

### Recommendation Tests

#### Test R1: Cold-start homepage

Input:

- new session, no user history

Expected outcome:

- response status is `200`
- `mode` is `cold_start`
- at least 12 items returned
- no duplicate movies
- results include diversified genres or curated spread

#### Test R2: After 2 to 3 likes

Input:

- `demo_sci_fi` likes 2 to 3 sci-fi movies

Expected outcome:

- next recommendation response differs from cold-start baseline
- more sci-fi or semantically adjacent content appears
- previously dominant unrelated genres drop in ranking

#### Test R3: Similar movie recommendations on detail page

Input:

- open a known sci-fi movie detail page

Expected outcome:

- similar movies are thematically aligned
- no unrelated family or comedy cluster in top results unless metadata justifies it

#### Test R4: Availability filtering

Condition:

- some candidate movies are marked unavailable

Expected outcome:

- unavailable titles do not appear in final returned list
- filtering does not cause a server error
- if filtering reduces count, fallback fills the gap

#### Test R5: Sparse user history

Input:

- one like only

Expected outcome:

- `mode` may still be `personalized`
- fallback and popularity support prevent low-quality overfitting
- final list is not entirely near-duplicates of one movie

### Recommendation Acceptance Criteria

- every demo persona receives a coherent recommendation feed
- recommendation lists are non-empty
- no duplicate IDs
- no broken posters/titles due to join mismatch
- score ordering is stable across repeated calls with unchanged input

---

## 14. Explainability Verification

### Purpose

Explainability must be truthful, not decorative.

### Rules

- a reason code may only appear if that signal was actually used
- fallback recommendations must use fallback-specific reason codes
- vector similarity must not be described as collaborative filtering
- trending fallback must not be labeled as personalized taste learning

### Explainability Tests

#### Test X1: Personalized reason

Condition:

- user liked emotional sci-fi movies

Expected outcome:

- reasons may include `liked_similar_theme` or `similar_to_recently_viewed`
- no `trending_now` reason unless trending actually influenced ranking

#### Test X2: Cold-start reason

Condition:

- new session with no profile

Expected outcome:

- reasons may include `trending_now` or `editorial_starter_pick`
- personalized reasons must not appear

#### Test X3: Vector failure fallback reason

Condition:

- vector retrieval disabled

Expected outcome:

- reason codes reflect fallback behavior such as `fallback_text_match`
- semantic reason labels are not shown

---

## 15. Fallback And Failure-Mode Verification

### Required Fallback Order

1. personalized recommendation
2. semantic similarity or recent-session reranking
3. trending by cohort or genre
4. editorial starter set
5. empty state only if explicitly allowed, which for this project it is not

### Failure-Mode Tests

#### Test F1: Query embedding timeout

Expected outcome:

- response `200`
- fallback search path used
- UI remains usable

#### Test F2: MongoDB Vector Search unavailable

Expected outcome:

- response `200`
- text and catalog-based fallback candidates returned in the same `items` list contract
- logs show degraded mode clearly

#### Test F3: Personalization unavailable

Expected outcome:

- response `200`
- non-personalized but still useful recommendations returned
- no broken ranking payload

#### Test F4: Empty primary candidate set

Expected outcome:

- fallback items fill the response
- no empty recommendation module in demo paths

---

## 16. MongoDB-Specific Verification

### Purpose

This section verifies the hackathon’s core technical requirement: MongoDB is not only present, but central to recommendation behavior.

### MongoDB Checks

#### Test M1: Vector Search index readiness

Expected outcome:

- vector index exists on `movies.embedding`
- index uses the expected embedding field
- embedding dimension matches the model in use

#### Test M2: Embedding completeness

Expected outcome:

- all seeded demo movies have embeddings
- no null embeddings in demo dataset
- `embeddingVersion` is consistent

#### Test M3: Aggregation-based profile derivation

Expected outcome:

- aggregation or equivalent logic produces expected `topGenres` and `recentMovieIds`
- no schema mismatch between events and profile derivation

#### Test M4: Recommendation logs are inspectable

Expected outcome:

- recommendation requests can be traced to `mode`, reason codes, and returned movie IDs
- logs allow debugging of wrong recommendations before demo

### MongoDB Acceptance Criteria

- MongoDB Vector Search demonstrably drives semantic search or similar-item retrieval
- MongoDB Aggregation Pipeline demonstrably drives trending, profiling, or reranking logic
- no critical recommendation flow depends on a non-MongoDB hidden ranking store for MVP

---

## 17. AWS Deployment Verification

### AWS Checks

#### Test A1: Frontend deployment health

Expected outcome:

- public frontend URL loads successfully
- no missing environment variables

#### Test A2: Backend deployment health

Expected outcome:

- App Runner service healthy
- API endpoints reachable

#### Test A3: Secrets availability

Expected outcome:

- embedding provider key available
- MongoDB URI available
- app fails safely if a secret is missing

#### Test A4: Region and connectivity sanity

Expected outcome:

- application can reach MongoDB Atlas reliably from deployed environment
- no last-minute connectivity mismatch caused by network rules

---

## 18. Demo Rehearsal Verification

### Required Demo Scenarios

- semantic search happy path
- cold-start homepage
- like 2 to 3 movies and refresh recommendations
- movie detail similar-items flow
- one degraded-mode fallback scenario

### Rehearsal Checklist

- use fixed demo persona
- use known-good queries only
- verify recommendation changes before recording
- verify explanation chips are truthful
- verify public URL from the demo device and network
- keep prerecorded fallback video ready

### Demo Pass Criteria

- full walkthrough completes in under 7 minutes
- no manual database fix is required during the run
- no blank state appears
- team can explain why results changed

---

## 19. Test Commands And Expected Outcomes

These commands are placeholders and must be adapted to the repo once implementation exists. For the Spring Boot backend, prefer Maven or Gradle based verification entry points rather than Node-centric commands.

### API Contract Tests

Run:

```bash
./mvnw test -Dtest=ApiContractTest
```

Expected outcome:

- contract tests pass
- no response shape drift on demo-critical endpoints

### Integration Tests

Run:

```bash
./mvnw test -Dtest=IntegrationTest
```

Expected outcome:

- MongoDB-backed flows pass
- fallback paths pass
- duplicate event handling passes

### End-To-End Tests

Run:

```bash
./mvnw test -Dtest=EndToEndVerificationTest
```

Expected outcome:

- full demo path passes in deployed-like environment

### Manual Query Audit

Run:

```bash
./mvnw test -Dtest=QueryFixtureVerificationTest
```

Expected outcome:

- all validated query fixtures produce acceptable results or expected fallback modes

### Demo Rehearsal Checklist

Run:

```bash
./mvnw test -Dtest=DemoReadinessVerificationTest
```

Expected outcome:

- seeded demo scenarios complete without blocker issues

---

## 20. Sign-Off Criteria

The team may sign off on the build only when:

- P0 matrix is green
- search fallbacks are green
- recommendation mode transitions are green
- event deduplication is green
- MongoDB-specific checks are green
- AWS public deployment is green
- demo rehearsal passes twice on the target environment

---

## 21. Known Failure Smells

Treat these as blockers until disproven:

- recommendations look plausible once but are not reproducible
- explanations mention reasons not present in logs
- fallback mode works only locally and not on deployed environment
- one tester’s noisy events dominate ranking for everyone
- duplicated items appear due to ID mismatch across layers
- vector results collapse for long-tail queries when filters are applied
- UI relies on page refresh timing instead of stable API behavior

---

## 22. Final Verification Report Template

Use this template for each full pass:

```md
### Verification Run

- Date:
- Environment:
- Commit SHA:
- Dataset Snapshot:
- Embedding Version:
- Owner:

#### Results
- Search:
- Events:
- Profiles:
- Recommendations:
- Explainability:
- Fallbacks:
- Deployment:
- Demo rehearsal:

#### Findings
- P0:
- P1:
- P2:

#### Decision
- Pass / Fail
- Notes:
```
