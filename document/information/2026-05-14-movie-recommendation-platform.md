# Movie Recommendation Platform Implementation Plan

> **For agentic workers:** REQUIRED: Use superpowers:subagent-driven-development (if subagents available) or superpowers:executing-plans to implement this plan. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build a MongoDB-first, AWS-hosted movie recommendation platform that demonstrates semantic search, behavior-aware personalization, explainable recommendations, and a reliable end-to-end demo suitable for the MUGVN Mini Hackathon 2026.

**Architecture:** Implement the product as a modular monolith with a Next.js frontend, a single backend API service, MongoDB Atlas as the operational and recommendation data platform, and AWS-managed services for hosting, secrets, logging, and static assets. The system should optimize for recommendation quality, explainability, demo reliability, and clear MongoDB usage rather than broad product scope.

**Tech Stack:** Next.js, TypeScript, Node.js, MongoDB Atlas, MongoDB Vector Search, MongoDB Aggregation Pipeline, Tailwind CSS, AWS Amplify Hosting, AWS App Runner, Amazon S3, AWS Secrets Manager, Amazon CloudWatch, embedding provider such as OpenAI or sentence-transformers

---

## 1. Executive Summary

This project should not be positioned as a movie streaming website or a Netflix clone. It should be positioned as a **Recommendation Intelligence Platform** for movie discovery.

The hackathon challenge is explicitly centered on recommendation systems using MongoDB. The strongest project framing is:

- a simple movie catalog
- natural language movie discovery
- user behavior tracking
- personalized recommendations
- explainable recommendation reasoning
- clear MongoDB-first system design

This aligns directly with the hackathon expectations:

- MongoDB is mandatory
- suggested solution patterns include Vector Search and Aggregation Pipeline
- deliverables require MVP, system architecture, data schema, and sample data
- scoring prioritizes creativity, technical implementation, and impact

---

## 2. Document Control

- **Owner:** Hackathon team lead
- **Contributors:** Frontend engineer, backend engineer, data/AI engineer
- **Status:** Draft
- **Last Updated:** 2026-05-14
- **Target Release:** Round 1 submission by 2026-05-31 18:00 VNT
- **Related Docs:** Demo script, architecture diagram, schema reference, sample data notes
- **Hackathon URL:** `https://mini-hackathon-2026.mugvn.com/`

### Owners and Roles

- **Team lead / submission owner**
  - owns scope control, final submission, review cadence, and demo rehearsal sign-off
- **Frontend / demo owner**
  - owns UI flow, explanation rendering, mobile usability, and video walkthrough readiness
- **Backend / recommendation owner**
  - owns APIs, event model, recommendation logic, and deployment runtime behavior
- **Data / platform owner**
  - owns dataset quality, embeddings, MongoDB schema/indexes, and technical documentation

### Decision Freeze Policy

- Backend framework decision must be closed on Day 1
- Dataset source must be closed on Day 1
- Primary embedding provider must be closed on Day 1
- Optional features cannot start until all Round 1 required artifacts are green

---

## 3. Judge-Facing Positioning

### Core Positioning

This is a **movie recommendation intelligence engine** that helps users find what to watch faster by understanding:

- mood
- semantic intent
- viewing behavior
- preference patterns
- similarity between users and titles

### One-Line Pitch

> Traditional recommendation systems know what users clicked. Our system understands why users like it.

### Why This Positioning Works

- **Creativity (30%)**
  - Natural language search plus explainable recommendations is more distinctive than a standard movie catalog.
- **Technical (30%)**
  - MongoDB Vector Search, Aggregation Pipeline, event tracking, and ranking logic create a strong technical story.
- **Impact (30%)**
  - The product addresses decision fatigue and poor discovery quality.
- **Presentation (10%)**
  - The demo can show visible recommendation improvement in a short time.

---

## 4. Hackathon Constraints and Submission Requirements

### Official Constraints

- MongoDB must be part or all of the backend database
- Team size is 1 to 3 members
- Submission materials may be in Vietnamese or English
- Round 1 deadline is `2026-05-31 18:00 VNT`
- Final presentation date is `2026-06-06`

### Required Round 1 Deliverables

- [ ] Demo video up to 10 minutes
- [ ] MVP definition
- [ ] System architecture document
- [ ] Data architecture and schema document
- [ ] Sample data document

### Suggested Technical Direction from Event Page

- MongoDB Vector Search
- Similarity Search
- Aggregation Pipeline
- Collaborative Filtering

### Planning Implications

The project must be:

- demoable
- technically credible
- strongly tied to MongoDB features
- small enough to finish on time
- documented well enough to look production-minded

### Non-Scoring but Operational Constraints

- Team should designate one team lead as the official submission contact
- Team should track workshop attendance separately if it wants the MongoDB certification benefit
- Submission package should avoid any confidential or secret material because the terms explicitly do not guarantee confidentiality

---

## 5. Success Metrics

- [ ] A first-time user receives at least 12 homepage recommendations with no empty state
- [ ] Semantic search returns relevant results for at least 5 validated demo queries
- [ ] Recommendations visibly change after 2 to 3 likes from the same session
- [ ] Every recommended card shown in the demo displays at least 1 truthful explanation chip
- [ ] Demo flow completes reliably in under 7 minutes, leaving buffer for narration
- [ ] All required Round 1 artifacts are completed 24 hours before submission

---

## 6. Goals

- [ ] Deliver a working recommendation MVP using MongoDB as the core backend database
- [ ] Demonstrate semantic search using MongoDB Vector Search
- [ ] Track user behavior and use it in recommendation logic
- [ ] Show personalized recommendations that improve based on actions
- [ ] Display explainable recommendation reasons in the UI
- [ ] Produce clean architecture and data schema documentation for submission
- [ ] Produce a strong demo flow that clearly shows value and technical depth

## 7. Non-Goals

- [ ] Full streaming platform
- [ ] Real movie playback infrastructure
- [ ] Social features
- [ ] Complex authentication and authorization
- [ ] Large-scale ML training pipeline
- [ ] Native mobile app
- [ ] Microservice decomposition

These items are explicitly excluded to keep the build aligned with judging criteria and delivery constraints.

---

## 8. MVP Scope

### In Scope

- [ ] Movie catalog with metadata
- [ ] Semantic search using natural language
- [ ] Movie detail page
- [ ] Similar movie recommendations
- [ ] User behavior tracking
- [ ] Personalized recommendation feed
- [ ] Explainable recommendation reasons
- [ ] Sample dataset ingestion
- [ ] Submission-ready technical documentation

### Optional if Time Allows

- [ ] Near real-time recommendation refresh
- [ ] Collaborative filtering enhancements
- [ ] Trending or popularity ranking section
- [ ] Admin or analytics debug page
- [ ] A/B comparison of ranking strategies

Optional work may start only after:

- all required documentation sections are complete
- the deployed demo URL is stable
- the fallback search and cold-start flows are validated
- the demo video script is frozen

### Out of Scope

- [ ] Full watch-history product
- [ ] Multi-user account management
- [ ] Payments or subscriptions
- [ ] Video streaming pipeline
- [ ] Complex ML experimentation platform

---

## 9. Users and Primary Flows

### Primary Persona

- A user who wants to find a movie quickly without scrolling through generic recommendations

### Happy Path

1. User opens the homepage.
2. User sees featured and recommended movies.
3. User searches using a natural language query such as `mind bending emotional sci-fi`.
4. Backend performs semantic retrieval using MongoDB Vector Search.
5. User clicks a movie and sees metadata, similar movies, and recommendation reasons.
6. User likes or saves a movie.
7. The behavior event is stored in MongoDB.
8. The recommendation engine immediately re-ranks suggestions using recent session events.
9. The homepage recommendations improve.
10. The UI explains why the new titles are being recommended.

### Demo Flow

1. Start with a seeded anonymous demo user.
2. Run a semantic search query.
3. Open a movie detail page.
4. Like 2 or 3 movies.
5. Refresh or revisit the homepage.
6. Show that recommendations changed.
7. Show explanation labels such as:
   - `Because you liked emotional sci-fi`
   - `Semantically similar to Interstellar`
   - `Popular among users with similar interests`

### Important Edge Flows

- Cold-start user with no behavior history
- Invalid or vague search query
- Embedding service outage or rate limit
- Recommendation request when the user has only one interaction
- Empty result set for long-tail queries

### Mandatory Fallback Behaviors

- Cold start must return `trending + editorial seed set + diversified genres`
- Failed query embedding must fall back to text search plus curated genre matches
- Degraded or unavailable MongoDB Vector Search must fall back to text search plus curated trending and genre blocks
- Sparse user history must fall back to semantic similarity plus trending within inferred interests
- Empty primary search results must still return a fallback section rather than a dead-end screen

---

## 10. Recommended Product Flow

### Flow A: Semantic Discovery

- user enters free-text search
- backend attempts to convert the query into an embedding within a strict timeout budget
- MongoDB Vector Search finds semantically similar titles when embedding succeeds
- if embedding fails or times out, the backend falls back to text search and curated genre matches
- if Vector Search is degraded or unavailable, the backend returns fallback text and catalog-based recommendation blocks with a clear response mode
- the UI displays relevant results with metadata and tags

### Flow B: Behavior Learning

- user performs actions: `view`, `click`, `like`, `save`, `rate`, `search`
- actions are written to `user_events`
- the recommendation layer uses recent session events immediately for reranking
- the user profile is updated synchronously for MVP-safe signals such as `like`, `save`, and `rate`

### Flow C: Personalized Recommendations

- recommendation service gathers candidate movies
- candidates are scored using vector similarity, preference overlap, popularity, and recent behavior
- top results are returned with reason codes

### Flow D: Explainability

- recommendation responses include structured reasons
- frontend renders short explanation chips
- this increases user trust and improves demo clarity

### Flow E: Demo-Safe Recovery

- UI displays whether results came from `semantic`, `fallback_text`, or `cold_start` mode
- operator can reset the demo session from a dedicated control or script
- team can switch to a seeded demo persona if live behavior is inconsistent

---

## 11. Proposed Architecture

### Architecture Style

Use a **modular monolith**.

### Why

- fastest to implement
- easiest to debug during a hackathon
- simplest to deploy
- easiest to explain in technical documentation
- still structured enough to scale into separate services later if needed

### High-Level Components

- **Frontend**
  - Next.js app for catalog UI, search, details, recommendations, and explainability
- **Backend API**
  - search API
  - movie detail API
  - recommendation API
  - event ingestion API
  - synchronous session reranking and lightweight profile updates
- **MongoDB Atlas**
  - application data
  - vector search index
  - event store
  - derived user profiles
  - recommendation logs
- **Embedding pipeline**
  - precomputes movie embeddings from title, overview, genres, and tags
- **AWS infrastructure**
  - Amplify Hosting for frontend
  - App Runner for backend API
  - S3 for import assets and optional demo artifacts
  - Secrets Manager for API keys and DB credentials
  - CloudWatch for logs and alarms

### Architecture Narrative

The frontend remains intentionally simple and judge-friendly. The backend exposes a small number of focused APIs. MongoDB Atlas stores both operational data and recommendation-relevant data structures, proving MongoDB is central to search, behavior analytics, and ranking. AWS provides managed hosting and observability while avoiding on-premise operational overhead. For MVP reliability, the system does not depend on a separate background worker for the core recommendation refresh loop.

---

## 12. Technology Choices

### Preferred Application Stack

#### Frontend
- Next.js
- TypeScript
- Tailwind CSS

#### Backend
- Node.js
- TypeScript
- Express

#### Database
- MongoDB Atlas hosted in an AWS region

#### AI / Embeddings
- OpenAI embeddings API for speed
- Alternative: sentence-transformers batch job if API cost or rate limits become an issue

### Fixed MVP Decisions

- Backend framework: `Express`
- Identity model: anonymous session-backed users only
- Primary embedding strategy: precomputed movie embeddings plus live query embeddings with strict timeout
- Query embedding fallback: text search plus curated genre/tag fallback
- Dataset direction: TMDB-derived movie metadata sample with 500 to 1000 titles

#### Cloud Hosting
- AWS Amplify Hosting for the Next.js frontend
- AWS App Runner for the backend API
- Amazon S3 for seeded dataset artifacts, posters cache if needed, and demo assets
- AWS Secrets Manager for credentials
- Amazon CloudWatch for application logs and alarms

### Why This Stack

- fast to build
- strong TypeScript ecosystem
- simple MongoDB Atlas integration
- cloud-native and presentation-friendly
- easy to deploy without custom server maintenance

### Rejected Alternatives

- **Microservices on ECS/EKS**
  - rejected because delivery speed matters more than service decomposition
- **On-premise deployment**
  - rejected because the user requested cloud deployment and managed infrastructure reduces demo risk
- **Full serverless Lambda-first backend**
  - rejected for MVP because vector-search-heavy flows and background jobs are simpler in a long-running App Runner service

---

## 13. AWS Cloud Architecture

### AWS Service Mapping

- **AWS Amplify Hosting**
  - host the Next.js frontend
- **AWS App Runner**
  - host the backend API container
- **Amazon S3**
  - store import files, generated artifacts, and optional cached static assets
- **AWS Secrets Manager**
  - store MongoDB URI, embedding provider key, and session secrets
- **Amazon CloudWatch**
  - aggregate logs, basic alarms, and troubleshooting metrics
- **IAM**
  - minimum required permissions for deployment and service access

### AWS Design Principles

- prefer fully managed services over self-hosted infrastructure
- keep the number of AWS services small enough for a hackathon team to operate
- avoid service choices that require substantial network or container orchestration complexity
- keep secrets out of source control and local shell history where possible
- freeze the production-demo environment 24 hours before submission

### Environment Strategy

- **local** for development
- **demo/staging** for rehearsals
- **production-demo** for final presentation and video capture

### MongoDB Atlas Connectivity Note

- For MVP speed, the backend may use temporary Atlas IP allowlisting during development.
- For the stable demo environment, document either a narrowed allowlist strategy or a VPC-connected egress approach before final deployment.
- Atlas database credentials must be scoped to the application database and stored only in Secrets Manager and local development env files.

---

## 14. MongoDB Data Architecture

### `movies`

Purpose: canonical movie catalog.

Suggested fields:

- `_id`
- `title`
- `overview`
- `genres`
- `tags`
- `cast`
- `directors`
- `releaseYear`
- `language`
- `posterUrl`
- `ratingAvg`
- `popularityScore`
- `availability`
- `embedding`
- `embeddingVersion`
- `embeddingUpdatedAt`

`availability` should minimally support:

- `isAvailable`
- `region`

This field is required because recommendation serving and verification both depend on filtering unavailable titles from final results.

### `users`

Purpose: lightweight anonymous session-backed identity for the MVP.

Suggested fields:

- `_id`
- `sessionId`
- `createdAt`
- `lastSeenAt`

### `user_events`

Purpose: immutable event log for recommendation signals.

Suggested fields:

- `_id`
- `eventId`
- `userId`
- `sessionId`
- `eventType`
- `movieId`
- `queryText`
- `eventValue`
- `metadata`
- `timestamp`

Supported event types:

- `view`
- `click`
- `like`
- `save`
- `rate`
- `search`

Validation rules:

- `search` requires `queryText` and does not require `movieId`
- `view`, `click`, `like`, `save`, and `rate` require `movieId`
- `eventId` is mandatory for idempotent write handling

### `user_profiles`

Purpose: derived preference profile for each user.

Suggested fields:

- `_id`
- `userId`
- `topGenres`
- `topThemes`
- `likedMovieIds`
- `recentMovieIds`
- `profileEmbedding`
- `lastComputedAt`
- `lastSignalsAppliedAt`

### `recommendation_logs`

Purpose: debugability, explainability, and demo analysis.

Suggested fields:

- `_id`
- `userId`
- `sessionId`
- `mode`
- `context`
- `candidateMovieIds`
- `recommendedMovieIds`
- `scores`
- `reasonCodes`
- `createdAt`

`recommendation_logs` is the canonical debugging record for:

- which serving mode was used
- which candidates were considered
- which titles were returned
- which explanation reasons were exposed

---

## 15. MongoDB-Specific Considerations

### Schema Design

- [ ] Keep raw event data immutable
- [ ] Keep movie metadata rich enough for embeddings
- [ ] Store derived user profile separately from raw events
- [ ] Keep explanation reason codes as structured output
- [ ] Keep embeddings inside `movies` for MVP simplicity and lower join complexity

### Index Plan

- [ ] Create a Vector Search index on `movies.embedding`
- [ ] Create optional normal indexes on `movies.genres`, `movies.releaseYear`, and `movies.popularityScore` if used in filters or fallback ranking
- [ ] Create a unique index on `users.sessionId`
- [ ] Create a unique index on `user_profiles.userId`
- [ ] Create a unique index on `user_events.eventId`
- [ ] Create an index on `user_events.userId, timestamp`
- [ ] Create an index on `user_events.sessionId, timestamp`
- [ ] Create an index on `user_events.movieId, timestamp`
- [ ] Create an optional fallback text index on `movies.title`, `movies.overview`, and `movies.tags` if needed

### Query and Access Patterns

- [ ] Semantic search by free-text query
- [ ] Similar movies by vector similarity
- [ ] User history lookup by user or session
- [ ] Recent events aggregation
- [ ] Recommendation ranking pipeline
- [ ] Trending movie aggregation
- [ ] Cold-start recommendation feed
- [ ] Empty-result fallback blocks

### Consistency and Durability

- [ ] Use stable write behavior for user events
- [ ] Make event ingestion idempotent using mandatory `eventId`
- [ ] Avoid overcomplicated transactions unless truly necessary
- [ ] Ensure profile updates are derived from accepted events only

### Scaling

- [ ] Keep scope at 500 to 1000 movies for the demo
- [ ] Design collections so they can scale later
- [ ] Document how the same design can support larger catalogs

### Reliability

- [ ] Retry transient recommendation failures with bounded retry logic
- [ ] Gracefully degrade if personalization is unavailable
- [ ] Fall back to trending or semantic matches when user history is sparse
- [ ] Fall back to text search if query embedding generation fails or times out
- [ ] Fall back to text and catalog-based recommendations if Vector Search is unavailable

### Security

- [ ] Keep DB credentials in Secrets Manager and local `.env` files only
- [ ] Never expose secrets in the frontend bundle
- [ ] Redact sensitive values from logs

---

## 16. Recommendation Strategy

### Phase 1: Hybrid Ranking MVP

Use a weighted scoring model with:

- semantic similarity
- genre or theme overlap
- recent user interactions
- soft search-intent influence
- like and save signals
- popularity or trending support
- optional diversity penalty to reduce near-duplicates

Search events may contribute weak profile hints, but they must not outweigh stronger positive interaction signals such as `like`, `save`, or `rate`.

Cold-start formula:

- trending score
- editorial seed set
- genre diversification

### Why This Is Best for the Hackathon

- easy to implement
- easy to explain
- easy to tune for the demo
- visibly improves recommendations
- gives a clean MongoDB story

### Phase 2: Collaborative Filtering

Add collaborative filtering via Aggregation Pipeline:

- identify users with similar likes or saves
- find movies liked by similar users
- merge collaborative candidates into ranking

### Explainability Strategy

Every recommendation should have at least one reason code, for example:

- `liked_similar_theme`
- `semantic_match_to_search`
- `similar_users_liked`
- `popular_in_preferred_genre`
- `similar_to_recently_viewed`
- `trending_now`
- `editorial_starter_pick`
- `fallback_text_match`

Frontend should translate these into human-readable text. A reason code may only be shown if the corresponding signal actually participated in ranking.

---

## 17. Dataset Strategy

### Dataset Recommendation

Use a TMDB-derived or equivalent movie dataset with:

- title
- overview
- genres
- tags
- popularity
- ratings
- poster URLs if available

### Demo Dataset Size

- 500 to 1000 movies

### Why

- enough variety for semantic search
- enough coverage for recommendation logic
- small enough to preprocess quickly
- manageable for hackathon debugging

### Dataset Tasks

- [ ] clean inconsistent fields
- [ ] normalize genres
- [ ] derive tags or themes if needed
- [ ] build embedding input text
- [ ] verify sample search quality manually
- [ ] verify embedding completeness and version consistency
- [ ] preserve one validated dataset and embedding snapshot for the final demo

---

## 18. API and Interface Design

### External APIs

#### Shared Recommendation Item Shape

```json
{
  "movie": {
    "id": "movie_123",
    "title": "Interstellar"
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

#### `GET /api/movies/search`

Purpose: semantic search by user query.

Query params:

- `q`
- `limit`
- `sessionId`

Response:

- top-level `items` using the shared recommendation item shape
- top-level `mode` values such as `semantic` or `fallback_text`
- top-level `fallbackUsed`
- top-level `query`
- optional query reformulation hint if no strong matches exist

#### `GET /api/movies/:movieId`

Purpose: retrieve movie metadata and related recommendations.

Response:

- movie detail
- `similarMovies` using the shared recommendation item shape

#### `POST /api/events`

Purpose: capture behavior signals.

Request body:

- `sessionId`
- `movieId`
- `eventType`
- `eventId`
- `queryText`
- `eventValue`
- `metadata`
- optional client `timestamp`

If the client omits `timestamp`, the backend must assign it during ingest and persist it in the stored event record.

Response:

- `accepted`
- `profileUpdated`
- `rerankedUsingRecentEvents`

#### `GET /api/recommendations`

Purpose: fetch personalized recommendations for a session.

Query params:

- `sessionId`
- `context`
- `limit`

Response:

- top-level `items` using the shared recommendation item shape
- top-level `mode` values such as `personalized`, `cold_start`, or `fallback_text`
- top-level `fallbackUsed`
- `generatedAt`

### Internal Interfaces

- embedding generation job
- profile recomputation job
- recommendation scoring service

### Versioning Strategy

- use a single internal API version for MVP
- preserve backwards-compatible response fields once the frontend is integrated

### Idempotency and Retry Behavior

- require client-generated `eventId` values for all event writes
- recommendation fetches must be safe to retry
- event writes must fail cleanly and not partially update profiles without an event record
- `like`, `save`, and `rate` must be idempotent
- `view` and `search` should be coalesced or rate-limited to avoid noisy duplicate signals

---

## 19. Security and Privacy

- Use anonymous sessions for MVP unless account features become necessary
- Keep PII out of the product scope
- Store secrets in AWS Secrets Manager
- Restrict AWS IAM permissions to least privilege
- Restrict MongoDB Atlas network and user access appropriately
- Avoid logging raw API keys, DB URIs, or full secret values
- Ensure public submission materials contain no internal credentials or private business data

Because hackathon submissions are not guaranteed confidential, the project should only use data and assets the team is comfortable making public.

---

## 20. Observability and Operational Readiness

### Logging

- [ ] log search requests and latency
- [ ] log recommendation pipeline steps at an informative but not noisy level
- [ ] log event ingestion success and failure
- [ ] avoid logging secrets or oversized payloads

### Metrics

- [ ] request count
- [ ] search latency
- [ ] recommendation latency
- [ ] event ingestion failure count
- [ ] recommendation generation success rate
- [ ] query embedding failure rate
- [ ] fallback mode rate
- [ ] empty search result rate
- [ ] duplicate event reject count

### Debug Support

- [ ] optionally build an admin or debug page
- [ ] show the latest events for the demo user
- [ ] show the last recommendation reasons
- [ ] show the current response mode: `semantic`, `fallback_text`, `personalized`, or `cold_start`
- [ ] show the latest embedding health status for demo troubleshooting

### Recovery

- [ ] keep a reseed script for the demo environment
- [ ] keep data import idempotent when possible
- [ ] keep at least one prevalidated demo session ready
- [ ] keep one validated query-to-results fixture set for demo-critical prompts

---

## 21. Risks and Mitigations

### Risk: Recommendation quality is weak

Mitigation:

- [ ] use rich metadata
- [ ] tune embedding text carefully
- [ ] use hybrid scoring, not vector-only ranking
- [ ] prepare tested demo queries

### Risk: Scope gets too large

Mitigation:

- [ ] freeze MVP early
- [ ] keep UI intentionally simple
- [ ] deprioritize non-core features
- [ ] treat real-time updates and collaborative filtering as optional

### Risk: MongoDB usage feels superficial

Mitigation:

- [ ] make MongoDB central to search
- [ ] store events in MongoDB
- [ ] use Aggregation Pipeline in recommendation logic
- [ ] document indexes and schema clearly
- [ ] call out MongoDB features in the demo

### Risk: Explainability is vague

Mitigation:

- [ ] generate explanations from real ranking features
- [ ] keep structured reason codes
- [ ] map codes to concrete human-readable labels

### Risk: Live demo fails

Mitigation:

- [ ] pre-seed demo users and data
- [ ] prepare fixed validated queries
- [ ] keep fallback recordings
- [ ] keep a stable deployed environment separate from active development
- [ ] verify the demo still works in fallback mode without live Vector Search

### Risk: Cold-start recommendations are poor

Mitigation:

- [ ] use trending titles
- [ ] use semantic results
- [ ] use genre-based starter recommendations

### Risk: Embedding API cost or rate limit blocks delivery

Mitigation:

- [ ] precompute embeddings offline in batches
- [ ] cache query embeddings when sensible
- [ ] keep an open-source fallback model option

### Risk-Based De-Scope Triggers

If the team slips more than 2 days behind the critical path, cut work in this order:

- [ ] cut collaborative filtering
- [ ] cut admin or debug page polish, keeping only minimum troubleshooting support
- [ ] cut multi-environment complexity beyond one stable demo deployment
- [ ] cut nonessential CloudWatch alarms beyond basic logs
- [ ] cut optional UI polish that does not improve recommendation clarity

---

## 22. Round 1 Execution Schedule

### Critical Path Calendar

- **2026-05-14 to 2026-05-15**
  - freeze stack decisions
  - validate dataset
  - define schemas and API contracts
- **2026-05-16 to 2026-05-18**
  - import movies
  - generate embeddings
  - verify search quality
- **2026-05-19 to 2026-05-22**
  - implement event ingestion
  - implement recommendation endpoint
  - validate cold-start behavior
- **2026-05-23 to 2026-05-25**
  - build frontend search, detail, and recommendation flows
  - complete explainability UI
- **2026-05-26 to 2026-05-27**
  - deploy stable AWS demo environment
  - complete technical documentation first draft
- **2026-05-28**
  - feature freeze for Round 1 MVP
- **2026-05-29**
  - full demo rehearsal
  - fix only blocker defects
- **2026-05-30**
  - record final demo video
  - finalize submission artifacts
- **2026-05-31**
  - final review and submit before 18:00 VNT with buffer

### Parallel Workstream Rules

- Frontend can proceed against frozen sample payloads before ranking is perfect
- Backend cannot change response shapes after frontend integration without team lead approval
- Data and embedding changes after feature freeze require full demo rehearsal again

---

## 23. Delivery Plan and Milestones

### Phase 0: Scope and Setup

**Objective:** freeze the direction and establish the foundation.

- [ ] confirm project framing
- [ ] confirm must-have and optional features
- [ ] choose final backend framework
- [ ] set up repository and environments
- [ ] provision MongoDB Atlas in an AWS region
- [ ] define collections and indexes
- [ ] pick and validate the sample dataset
- [ ] assign owners and backup owners for each workstream

### Phase 1: Data Foundation and Search

**Objective:** get the movie data pipeline and semantic search working.

- [ ] ingest dataset into MongoDB
- [ ] generate embeddings
- [ ] create the Vector Search index
- [ ] implement the semantic search API
- [ ] build the basic search UI
- [ ] verify search quality manually
- [ ] implement text-search fallback for query embedding failures

### Phase 2: User Behavior Tracking

**Objective:** capture recommendation signals.

- [ ] create anonymous session flow
- [ ] implement the event ingestion API
- [ ] track search, click, like, save, and view actions
- [ ] store raw events in MongoDB
- [ ] build user profile derivation logic
- [ ] validate duplicate-event protection

### Phase 3: Personalized Recommendations

**Objective:** build the recommendation engine core.

- [ ] implement candidate generation
- [ ] implement the hybrid ranking pipeline
- [ ] implement similar movie recommendations
- [ ] implement the homepage recommendation feed
- [ ] test behavior-based recommendation updates
- [ ] implement deterministic cold-start recommendations

### Phase 4: Explainability and Demo Quality

**Objective:** make the product compelling for judges.

- [ ] add recommendation reason codes
- [ ] expose explanations in the API
- [ ] render explainability in the UI
- [ ] build strong demo scenarios
- [ ] prepare before-and-after personalization examples

### Phase 5: Hardening and Submission

**Objective:** make the demo reliable and package the work professionally.

- [ ] seed final demo data
- [ ] deploy frontend and backend to AWS-managed services
- [ ] document architecture
- [ ] document data schema
- [ ] document sample data
- [ ] record the demo video
- [ ] finalize the submission package
- [ ] freeze the production-demo environment 24 hours before submission

### Phase 6: Finals Preparation

**Objective:** maximize presentation quality.

- [ ] add optional near real-time refresh or collaborative filtering only if stable
- [ ] refine the pitch
- [ ] create a fallback demo path
- [ ] rehearse the live presentation

### Milestones

- **M1:** Search working against seeded dataset, embeddings complete, and 5 validated demo queries pass manual review
- **M2:** Events for `view`, `click`, `like`, `save`, and `search` are stored, queryable by session, and protected against duplicate writes
- **M3:** Recommendation feed changes within one refresh after 2 to 3 likes from the same session
- **M4:** Every recommended item in the demo returns at least 1 human-readable explanation tied to a real signal
- **M5:** Public demo URL works, required docs are complete, and the final submission bundle is reviewed

---

## 24. Judging Criteria Traceability Matrix

| Judging Criterion | Plan Feature or Asset | Evidence in Demo | Evidence in Docs | Owner |
| --- | --- | --- | --- | --- |
| Creativity | Semantic search plus explainable recommendations | Natural language query and reason chips | Product framing and demo script | Frontend/demo owner |
| Technical | MongoDB Vector Search, Aggregation Pipeline, event model, AWS deployment | Search, personalization, system walkthrough | Architecture and schema docs | Backend/recommendation owner |
| Impact | Recommendation improvement and reduced decision fatigue | Before/after personalization flow | MVP and user problem summary | Team lead |
| Presentation | Stable UI, clean diagrams, short script, reliable public demo | Recorded walkthrough and live rehearsal | Demo script and architecture visuals | Frontend/demo owner |

---

## 25. Implementation Task Breakdown

### Task 1: Product Framing and Scope

**Files:**
- Create: `docs/project-framing.md`
- Create: `docs/demo-script.md`
- Create: `docs/submission-checklist.md`

- [ ] Define the final project title
- [ ] Freeze the MVP scope
- [ ] Write the final one-line pitch
- [ ] Define 3 demo scenarios
- [ ] Map each feature to judging criteria

### Task 2: Dataset and Embedding Preparation

**Files:**
- Create: `data/sample-movies.json`
- Create: `scripts/import-movies.ts`
- Create: `scripts/generate-embeddings.ts`
- Create: `docs/sample-data.md`

- [ ] Select the movie dataset
- [ ] Normalize metadata
- [ ] Build the embedding input string format
- [ ] Generate embeddings
- [ ] Store embedding records in MongoDB
- [ ] Validate query quality with real prompts

### Task 3: MongoDB Schema and Indexes

**Files:**
- Create: `docs/data-architecture.md`
- Create: `src/db/schema/*`
- Create: `src/db/indexes/*`

- [ ] Define collection shapes
- [ ] Define the Vector Search index
- [ ] Define user event indexes
- [ ] Define recommendation log structure
- [ ] Document read and write patterns

### Task 4: Search API

**Files:**
- Create: `src/modules/search/*`
- Test: `tests/search/*`

- [ ] Create the semantic search endpoint
- [ ] Map the query to an embedding
- [ ] Perform MongoDB Vector Search
- [ ] Return enriched result metadata
- [ ] Test relevance with sample queries

### Task 5: Event Tracking API

**Files:**
- Create: `src/modules/events/*`
- Test: `tests/events/*`

- [ ] Create the event ingestion endpoint
- [ ] Support core event types
- [ ] Store events reliably
- [ ] Validate payloads
- [ ] Add logging for debugging

### Task 6: Recommendation Engine

**Files:**
- Create: `src/modules/recommendations/*`
- Test: `tests/recommendations/*`

- [ ] Build candidate generation flow
- [ ] Build the hybrid ranking pipeline
- [ ] Build the similar movie endpoint
- [ ] Build the personalized homepage feed
- [ ] Add fallback behavior for cold-start users

### Task 7: Explainability Layer

**Files:**
- Create: `src/modules/explanations/*`
- Test: `tests/explanations/*`

- [ ] Define the reason code taxonomy
- [ ] Produce reasons from ranking signals
- [ ] Return explanation payloads
- [ ] Keep reasons simple and human-readable

### Task 8: Frontend Experience

**Files:**
- Create: `app/*` or `src/app/*`
- Test: `tests/e2e/*`

- [ ] Build the homepage
- [ ] Build the search page
- [ ] Build the movie detail page
- [ ] Render recommendation sections
- [ ] Render explainability chips
- [ ] Track interaction events from the UI
- [ ] Make the UI usable on desktop and mobile

### Task 9: AWS Deployment and Operations

**Files:**
- Create: `infra/README.md`
- Create: `docs/deployment.md`
- Create: `.github/workflows/deploy.yml`

- [ ] Configure Amplify Hosting for the frontend
- [ ] Configure App Runner for the backend
- [ ] Configure Secrets Manager entries
- [ ] Configure CloudWatch log groups and alarms
- [ ] Document deployment variables and rollout order
- [ ] Document Atlas connectivity and allowlist strategy for the demo environment

### Task 10: Docs and Submission Assets

**Files:**
- Create: `docs/architecture.md`
- Create: `docs/data-architecture.md`
- Create: `docs/mvp.md`
- Create: `docs/sample-data.md`
- Create: `docs/demo-script.md`

- [ ] Write the MVP definition
- [ ] Write the system architecture
- [ ] Write schema and data architecture
- [ ] Write the sample data summary
- [ ] Write the recommendation logic summary
- [ ] Prepare diagrams and screenshots

---

## 26. Testing Strategy

### Unit Tests

- [ ] scoring helpers
- [ ] explanation mapping
- [ ] event normalization
- [ ] profile derivation logic

### Integration Tests

- [ ] MongoDB query behavior
- [ ] search endpoint with vector search
- [ ] recommendation endpoint
- [ ] event ingestion endpoint
- [ ] duplicate event rejection or coalescing
- [ ] fallback text search path

### End-to-End Tests

- [ ] search to detail flow
- [ ] like movie and refresh recommendations
- [ ] explanation display
- [ ] cold-start fallback recommendations
- [ ] empty-result fallback behavior
- [ ] anonymous session continuity across refresh and revisit

### Manual Verification

- [ ] semantic search results feel relevant
- [ ] user interactions are stored
- [ ] recommendations change after likes
- [ ] explanation tags match actual system behavior
- [ ] demo works from a clean environment

### Demo Reliability Tests

- [ ] pre-seeded demo user works
- [ ] stable query set works
- [ ] fallback scenario exists
- [ ] video backup exists

---

## 27. Acceptance Criteria

### Functional Acceptance

- [ ] User can perform semantic search using natural language
- [ ] User can view movie details
- [ ] User can like or interact with movies
- [ ] System stores user behavior in MongoDB
- [ ] Recommendations change based on user behavior
- [ ] Each recommendation shows at least one clear reason
- [ ] Cold-start users always receive a non-empty homepage feed
- [ ] Empty or failed semantic search still returns useful fallback content

### Technical Acceptance

- [ ] MongoDB Atlas is the core backend database
- [ ] MongoDB Vector Search is used in search or recommendation flow
- [ ] Aggregation Pipeline is used in ranking or behavioral analysis
- [ ] Collections and indexes are documented
- [ ] Sample data can be reproduced
- [ ] System is deployable to AWS-managed services for the demo

### Presentation Acceptance

- [ ] Demo completes in under 10 minutes
- [ ] Recommendation value is obvious
- [ ] MongoDB technical choices are easy to explain
- [ ] Architecture documentation looks complete and credible

---

## 28. Deployment and Release Plan

### Environments

- local development
- demo or staging
- production-demo

### Deployment Checklist

- [ ] configure MongoDB Atlas in the target AWS region
- [ ] configure backend environment variables in App Runner
- [ ] configure frontend environment variables in Amplify
- [ ] set up Secrets Manager entries
- [ ] run data import
- [ ] run embedding generation
- [ ] verify the Vector Search index
- [ ] verify live endpoints
- [ ] verify the full demo flow from public URLs

### Rollback Plan

- [ ] keep the previous stable deployment available
- [ ] keep a reseed script or data snapshot
- [ ] fall back to trending and semantic search if personalization fails
- [ ] keep a prerecorded backup demo
- [ ] keep a release bundle with frontend version, backend image tag, dataset snapshot, and environment manifest
- [ ] require full rehearsal after any schema or index change near the deadline

---

## 29. Submission Checklist

- [ ] Demo video is 10 minutes or less
- [ ] MVP definition is complete
- [ ] System architecture document is complete
- [ ] Data schema and architecture document is complete
- [ ] Sample data document is complete
- [ ] Public demo URL is verified
- [ ] Team lead reviews all artifacts before submission
- [ ] Final package is ready at least 24 hours before the deadline

---

## 30. Demo and Documentation Package

### Demo Video `<= 10 minutes`

Recommended structure:

1. Problem and positioning
2. Product walkthrough
3. Semantic search demo
4. Behavior tracking and personalized recommendations
5. Explainability demo
6. MongoDB architecture and technical highlights
7. Impact and next steps

### Demo Evidence Plan

The video must explicitly show:

- the user problem and use case
- the live solution flow
- the results achieved after user interactions
- the MongoDB-specific technical implementation
- why the recommendation output is better than a generic catalog

### Technical Documentation

Must include:

- [ ] MVP definition
- [ ] system architecture
- [ ] data schema and data architecture
- [ ] sample data description
- [ ] MongoDB usage details
- [ ] recommendation logic overview
- [ ] deployment and setup notes
- [ ] limitations and future enhancements

### Repo and Submission Assets

- [ ] source code repository
- [ ] setup instructions
- [ ] API endpoint summary
- [ ] screenshots or diagrams
- [ ] seeded demo dataset instructions
- [ ] demo script
- [ ] fallback prerecorded demo clip

---

## 31. Immediate Kickoff Checklist

- [ ] Freeze the project as a recommendation intelligence platform, not a streaming app
- [ ] Confirm the backend framework is Express
- [ ] Provision MongoDB Atlas immediately
- [ ] Finalize schema and index plan before deep implementation
- [ ] Prepare sample dataset and embeddings early
- [ ] Build semantic search before polishing the UI
- [ ] Build event tracking before advanced ranking refinements
- [ ] Start the demo script early
- [ ] Keep the system explainable at every stage

---

## 32. Final Recommendation

The strongest version of this hackathon project is:

- simple UI
- strong recommendation quality
- clear MongoDB-first architecture
- visible personalization
- explainable output
- reliable cloud-hosted demo flow

If implemented with discipline, this project can score well because it is:

- creative without being overbuilt
- technically aligned with the event
- impactful to real users
- easy to present clearly

## 33. Suggested Final Pitch

> We built a movie recommendation intelligence platform that helps users decide what to watch faster through semantic understanding, behavioral learning, and explainable recommendations. MongoDB is not just our database; it is the engine behind vector retrieval, behavior analytics, and personalized ranking, while AWS gives us a clean, managed cloud deployment story for reliability and scale.
