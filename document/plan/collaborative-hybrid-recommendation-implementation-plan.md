# Collaborative Hybrid Recommendation Engine Implementation Plan

> **For agentic workers:** REQUIRED: Use superpowers:subagent-driven-development (if subagents available) or superpowers:executing-plans to implement this plan. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build a collaborative-hybrid-capable movie recommendation engine on MongoDB and Spring Boot that ships a semantic-and-profile MVP first, while adding collaborative signals when time and validated support data allow it.

**Architecture:** The implementation should use a Spring Boot modular monolith backend, a Next.js frontend, MongoDB Atlas as the core operational and recommendation platform, and AWS-managed hosting. The recommendation system should combine semantic retrieval, event-driven profile signals, and collaborative co-occurrence support behind a single serving contract that can be implemented concurrently by 3 developers.

**Tech Stack:** Java 21, Spring Boot 3.5.x, Spring Web, Spring Validation, Spring Boot Starter Actuator, Spring Boot Starter Data MongoDB, Spring AI, MongoDB Atlas, MongoDB Vector Search, MongoDB Aggregation Pipeline, Next.js, TypeScript, AWS App Runner, Amazon S3, Amazon CloudFront, AWS Secrets Manager, Amazon CloudWatch

---

## Chunk 1: Scope And Delivery Model

## 1. Purpose

This plan explains how to implement the collaborative-hybrid recommendation engine in a way that 3 developers can work concurrently with low blocking risk.

This plan assumes the existing canonical docs remain the source of truth for:

- product boundary
- stack decisions
- recommendation behavior
- verification and release gate

Related docs:

- `document/information/2026-05-14-movie-recommendation-platform.md`
- `document/information/mongodb-recommendation-design.md`
- `document/information/technology-stack.md`
- `document/information/deliverable.md`
- `document/test-verification/verification-and-test-spec.md`

## 2. Recommended Team Split

Use 3 concurrent workstreams with hard interface contracts.

### Developer 1: Backend And Recommendation Serving

Owns:

- Spring Boot API
- serving modes
- candidate merging and ranking
- explainability payloads
- deployment of backend service

### Developer 2: Data And MongoDB Derivations

Owns:

- dataset ingestion
- embeddings generation integration
- MongoDB indexes
- aggregation pipelines
- user profile derivation
- collaborative neighbor generation

### Developer 3: Frontend And Demo Flow

Owns:

- Next.js UI
- session flow
- event emission from UI
- recommendation rendering
- search experience
- demo script integration and visible fallback behavior

### Shared Submission Ownership

- Developer 1 owns backend architecture and API contract documentation.
- Developer 2 owns sample data, schema, aggregation, and recommendation-evidence documentation.
- Developer 3 owns demo script, screenshots, walkthrough assets, and visible UX evidence.
- Team lead, who must be one of the 3 developers above, owns final packaging and submission completeness against `document/information/deliverable.md`.

## 3. Concurrency Rules

- Backend and frontend must agree on API response contracts before implementation starts.
- Data and backend must agree on collection schema and derived collection names before pipeline work starts.
- Frontend should work against mocked API payloads while backend/data logic is still being built.
- Collaborative filtering must not block the semantic and profile-based baseline path.
- The MVP must remain usable if collaborative signals are late or unavailable.

---

## Chunk 2: Canonical Implementation Boundaries

## 4. What Must Be Built First

The hybrid recommendation engine must be implemented in this order:

1. canonical movie catalog
2. semantic retrieval
3. event ingestion
4. profile derivation
5. personalized reranking
6. explainability
7. fallback behavior
8. collaborative support data if schedule allows
9. verification and demo rehearsal

## 5. Mandatory Engine Capabilities

- semantic search using MongoDB Vector Search
- movie-to-movie recommendations
- event tracking for `search`, `view`, `click`, `like`, `save`, `rate`
- user profile derivation from strong and weak signals
- hybrid ranking that merges semantic and profile signals in the MVP baseline
- collaborative co-occurrence support using aggregation-derived neighbors when enabled
- truthful reason codes
- single-list fallback-aware response contract

## 6. Serving Modes

The implementation must keep these serving modes stable:

- `semantic`
- `personalized`
- `cold_start`
- `fallback_text`

Collaborative logic is not a separate serving mode. It is a candidate source that may contribute within `personalized` mode.

---

## Chunk 3: File And Module Structure

## 7. Suggested Backend File Structure

This section uses exact conceptual paths for planning. Adapt package roots to the actual repo once implementation starts.

### Core Backend Files

- Create: `backend/src/main/java/com/hackathon/app/Application.java`
- Create: `backend/src/main/java/com/hackathon/app/config/RecommendationProperties.java`
- Create: `backend/src/main/java/com/hackathon/app/api/SearchController.java`
- Create: `backend/src/main/java/com/hackathon/app/api/MovieController.java`
- Create: `backend/src/main/java/com/hackathon/app/api/EventController.java`
- Create: `backend/src/main/java/com/hackathon/app/api/RecommendationController.java`

### Domain Model Files

- Create: `backend/src/main/java/com/hackathon/app/model/MovieDocument.java`
- Create: `backend/src/main/java/com/hackathon/app/model/UserDocument.java`
- Create: `backend/src/main/java/com/hackathon/app/model/UserEventDocument.java`
- Create: `backend/src/main/java/com/hackathon/app/model/UserProfileDocument.java`
- Create: `backend/src/main/java/com/hackathon/app/model/RecommendationLogDocument.java`
- Create: `backend/src/main/java/com/hackathon/app/model/MovieNeighborDocument.java`

### Repository Files

- Create: `backend/src/main/java/com/hackathon/app/repository/MovieRepository.java`
- Create: `backend/src/main/java/com/hackathon/app/repository/UserRepository.java`
- Create: `backend/src/main/java/com/hackathon/app/repository/UserEventRepository.java`
- Create: `backend/src/main/java/com/hackathon/app/repository/UserProfileRepository.java`
- Create: `backend/src/main/java/com/hackathon/app/repository/RecommendationLogRepository.java`
- Create: `backend/src/main/java/com/hackathon/app/repository/MovieNeighborRepository.java`

### Recommendation Service Files

- Create: `backend/src/main/java/com/hackathon/app/recommendation/ServingModeResolver.java`
- Create: `backend/src/main/java/com/hackathon/app/recommendation/SemanticCandidateService.java`
- Create: `backend/src/main/java/com/hackathon/app/recommendation/ProfileCandidateService.java`
- Create: `backend/src/main/java/com/hackathon/app/recommendation/CollaborativeCandidateService.java`
- Create: `backend/src/main/java/com/hackathon/app/recommendation/HybridRankingService.java`
- Create: `backend/src/main/java/com/hackathon/app/recommendation/ExplanationService.java`
- Create: `backend/src/main/java/com/hackathon/app/recommendation/FallbackRecommendationService.java`
- Create: `backend/src/main/java/com/hackathon/app/recommendation/RecommendationOrchestrator.java`

### Data Pipeline Files

- Create: `backend/src/main/java/com/hackathon/app/pipeline/MovieImportJob.java`
- Create: `backend/src/main/java/com/hackathon/app/pipeline/EmbeddingGenerationJob.java`
- Create: `backend/src/main/java/com/hackathon/app/pipeline/UserProfileAggregationJob.java`
- Create: `backend/src/main/java/com/hackathon/app/pipeline/TrendingAggregationJob.java`
- Create: `backend/src/main/java/com/hackathon/app/pipeline/CollaborativeNeighborAggregationJob.java`

### Frontend Files

- Create: `frontend/src/app/page.tsx`
- Create: `frontend/src/app/search/page.tsx`
- Create: `frontend/src/app/movie/[movieId]/page.tsx`
- Create: `frontend/src/lib/api.ts`
- Create: `frontend/src/lib/session.ts`
- Create: `frontend/src/components/RecommendationCarousel.tsx`
- Create: `frontend/src/components/RecommendationReasonChips.tsx`
- Create: `frontend/src/components/SearchResults.tsx`

### Test Files

- Create: `backend/src/test/java/com/hackathon/app/api/SearchControllerTest.java`
- Create: `backend/src/test/java/com/hackathon/app/api/EventControllerTest.java`
- Create: `backend/src/test/java/com/hackathon/app/api/RecommendationControllerTest.java`
- Create: `backend/src/test/java/com/hackathon/app/recommendation/HybridRankingServiceTest.java`
- Create: `backend/src/test/java/com/hackathon/app/recommendation/ServingModeResolverTest.java`
- Create: `backend/src/test/java/com/hackathon/app/pipeline/UserProfileAggregationJobTest.java`
- Create: `backend/src/test/java/com/hackathon/app/pipeline/CollaborativeNeighborAggregationJobTest.java`
- Create: `frontend/tests/e2e/recommendation-flow.spec.ts`

---

## Chunk 4: Phase Plan For 3 Concurrent Developers

## 8. Phase 0: Contract Freeze

Objective:

- freeze contracts so all 3 developers can work independently with low rework.

### Shared Outputs

- canonical API response examples
- final collection field list
- final event semantics
- final collaborative candidate source definition

Endpoint-specific metadata must be frozen separately:

- search responses: `items`, `mode`, `fallbackUsed`, `query`, optional `hint`
- recommendation responses: `items`, `mode`, `fallbackUsed`, `generatedAt`

### Developer 1

- [ ] Define REST endpoint request and response contracts
- [ ] Define endpoint-specific top-level response metadata rules
- [ ] Define error response shape

### Developer 2

- [ ] Freeze MongoDB collection shapes
- [ ] Freeze index list
- [ ] Freeze aggregation outputs for profiles, trending, and neighbors
- [ ] Freeze cold-start sources: trending, editorial seed set, and genre diversification

### Developer 3

- [ ] Build mocked UI payload contracts
- [ ] Freeze screen-level data requirements for home, search, and movie detail pages
- [ ] Freeze demo asset checklist and submission-visible UI evidence with team lead

## 9. Phase 1: Semantic Baseline

Objective:

- ship a working search and similar-item baseline first.

### Developer 1

- [ ] Implement `GET /api/movies/search`
- [ ] Implement `GET /api/movies/:movieId`
- [ ] Return single-list `items` payloads with mode metadata

### Developer 2

- [ ] Import the movie dataset
- [ ] Generate embeddings
- [ ] Create MongoDB Vector Search index on `movies.embedding`
- [ ] Validate 10 fixed search queries
- [ ] Define or materialize editorial seed inputs and diversified cold-start content

### Developer 3

- [ ] Build the search page
- [ ] Build the movie detail page
- [ ] Render search results and similar-movie sections
- [ ] Render fallback candidates in the same `items` list without separate fallback blocks

## 10. Phase 2: Behavior Tracking And Profile Signals

Objective:

- make user actions shape recommendation quality.

### Developer 1

- [ ] Implement `POST /api/events`
- [ ] Enforce idempotency via `eventId`
- [ ] Implement `GET /api/recommendations` cold-start and profile-baseline path
- [ ] Coalesce or rate-limit noisy `view` and `search` events per canonical rules

### Developer 2

- [ ] Implement user profile aggregation logic using MongoDB Aggregation Pipeline
- [ ] Support strong positive signals from `like`, `save`, and positive `rate`
- [ ] Enforce explicit `rate` semantics so low ratings are not treated as positive preference signals
- [ ] Support weak search-intent hints without allowing them to dominate the profile

### Developer 3

- [ ] Emit search, view, click, like, save, and rate events from UI
- [ ] Persist anonymous session behavior across pages
- [ ] Show visible recommendation changes after 2 to 3 positive actions

## 11. Phase 3: Collaborative Hybrid Upgrade If Schedule Allows

Objective:

- add collaborative support as a real candidate source in personalized mode only after the semantic-profile baseline and demo-hardening path are already stable.

### Developer 1

- [ ] Add collaborative candidates into hybrid ranking orchestration
- [ ] Gate collaborative reason codes behind real collaborative participation in ranking

### Developer 2

- [ ] Build collaborative neighbor aggregation job from `user_events`
- [ ] Materialize `movie_neighbors` collection
- [ ] Validate co-occurrence quality on seeded personas and known movie clusters

### Developer 3

- [ ] Update recommendation UI to surface collaborative reasons only when present
- [ ] Validate that collaborative wording is not shown in fallback or non-collaborative paths

Exit gate for this phase:

- baseline MVP search, events, personalization, explainability, and fallback verification are already green
- collaborative support does not regress the demo path

## 12. Phase 4: Fallbacks, Explainability, And Demo Hardening

Objective:

- make the system stable and demo-safe.

### Developer 1

- [ ] Finalize fallback service behavior for query embedding failure, Vector Search degradation, sparse profile, and personalization failure
- [ ] Finalize explanation payload shape

### Developer 2

- [ ] Validate fallback candidate generation from text, trending, editorial seed, and diversified genre paths
- [ ] Validate that unavailable items are filtered in all user-facing lists

### Developer 3

- [ ] Build visible degraded-mode-safe rendering
- [ ] Rehearse seeded demo personas and query fixtures
- [ ] Prepare backup recorded flow and operator notes

### Shared Documentation Track

- [ ] Developer 1 drafts backend architecture and API contract sections
- [ ] Developer 2 drafts schema, sample data, aggregation, and recommendation-evidence sections
- [ ] Developer 3 drafts demo script, screenshots, and walkthrough assets
- [ ] Team lead assembles final submission artifacts against `document/information/deliverable.md`

---

## Chunk 5: Detailed Task Plan

### Task 1: Freeze Shared Contracts

**Files:**
- Create: `backend/src/main/java/com/hackathon/app/api/contracts/*.java`
- Create: `frontend/src/lib/contracts.ts`
- Create: `docs/api-contracts.md`
- Test: `backend/src/test/java/com/hackathon/app/api/ContractShapeTest.java`

- [ ] **Step 1: Write failing contract tests for search and recommendations**

```java
@Test
void searchResponseContainsItemsModeFallbackAndQuery() {
    // expected serialized contract shape assertion
}
```

- [ ] **Step 2: Run contract test to verify it fails**

Run: `./mvnw test -Dtest=ContractShapeTest`
Expected: FAIL because contract DTOs or serializers do not exist yet

- [ ] **Step 3: Create minimal DTOs and response wrappers**

```java
public record SearchResponse(
    List<RecommendationItem> items,
    String mode,
    boolean fallbackUsed,
    String query,
    String hint
) {}

public record RecommendationResponse(
    List<RecommendationItem> items,
    String mode,
    boolean fallbackUsed,
    Instant generatedAt
) {}
```

- [ ] **Step 4: Run the contract test to verify it passes**

Run: `./mvnw test -Dtest=ContractShapeTest`
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add backend/src/main/java/com/hackathon/app/api/contracts frontend/src/lib/contracts.ts docs/api-contracts.md
git commit -m "docs: freeze recommendation API contracts"
```

### Task 2: Build Semantic Retrieval Baseline

**Files:**
- Create: `backend/src/main/java/com/hackathon/app/recommendation/SemanticCandidateService.java`
- Create: `backend/src/main/java/com/hackathon/app/api/SearchController.java`
- Test: `backend/src/test/java/com/hackathon/app/api/SearchControllerTest.java`

- [ ] **Step 1: Write failing search controller test**

```java
@Test
void returnsSemanticSearchResultsForValidQuery() {
    // expect 200 and semantic mode
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./mvnw test -Dtest=SearchControllerTest`
Expected: FAIL because the search controller or service is missing

- [ ] **Step 3: Implement minimal search service and controller**

```java
public SearchResponse search(String query, String sessionId, int limit) {
    return semanticCandidateService.search(query, sessionId, limit);
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `./mvnw test -Dtest=SearchControllerTest`
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add backend/src/main/java/com/hackathon/app/recommendation/SemanticCandidateService.java backend/src/main/java/com/hackathon/app/api/SearchController.java backend/src/test/java/com/hackathon/app/api/SearchControllerTest.java
git commit -m "feat: add semantic search baseline"
```

### Task 3: Build Event Ingestion And Idempotency

**Files:**
- Create: `backend/src/main/java/com/hackathon/app/api/EventController.java`
- Create: `backend/src/main/java/com/hackathon/app/service/EventIngestionService.java`
- Test: `backend/src/test/java/com/hackathon/app/api/EventControllerTest.java`

- [ ] **Step 1: Write failing event ingestion test**

```java
@Test
void duplicateEventIdIsIgnoredIdempotently() {
    // expect first accepted and second ignored or idempotent success
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./mvnw test -Dtest=EventControllerTest`
Expected: FAIL because event ingestion is not implemented

- [ ] **Step 3: Implement minimal idempotent event ingestion**

```java
public EventResponse ingest(EventRequest request) {
    // assign timestamp if missing, store once by eventId
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `./mvnw test -Dtest=EventControllerTest`
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add backend/src/main/java/com/hackathon/app/api/EventController.java backend/src/main/java/com/hackathon/app/service/EventIngestionService.java backend/src/test/java/com/hackathon/app/api/EventControllerTest.java
git commit -m "feat: add idempotent event ingestion"
```

### Task 4: Build Profile Aggregation

**Files:**
- Create: `backend/src/main/java/com/hackathon/app/pipeline/UserProfileAggregationJob.java`
- Test: `backend/src/test/java/com/hackathon/app/pipeline/UserProfileAggregationJobTest.java`

- [ ] **Step 1: Write failing aggregation test for profile derivation**

```java
@Test
void derivesTopGenresFromStrongPositiveSignals() {
    // expect sci-fi to outrank weaker genres after likes and positive ratings
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./mvnw test -Dtest=UserProfileAggregationJobTest`
Expected: FAIL because aggregation logic does not exist yet

- [ ] **Step 3: Implement aggregation pipeline wrapper**

```java
public UserProfileDocument recomputeProfile(String userId) {
    // run Mongo aggregation over user_events and movies
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `./mvnw test -Dtest=UserProfileAggregationJobTest`
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add backend/src/main/java/com/hackathon/app/pipeline/UserProfileAggregationJob.java backend/src/test/java/com/hackathon/app/pipeline/UserProfileAggregationJobTest.java
git commit -m "feat: derive user profiles from event signals"
```

### Task 5: Build Collaborative Neighbor Generation

**Files:**
- Create: `backend/src/main/java/com/hackathon/app/pipeline/CollaborativeNeighborAggregationJob.java`
- Create: `backend/src/main/java/com/hackathon/app/model/MovieNeighborDocument.java`
- Test: `backend/src/test/java/com/hackathon/app/pipeline/CollaborativeNeighborAggregationJobTest.java`

- [ ] **Step 1: Write failing collaborative neighbor test**

```java
@Test
void generatesNeighborsFromCoLikedMovies() {
    // expect movie A to include movie B when enough users had strong positive interactions with both
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./mvnw test -Dtest=CollaborativeNeighborAggregationJobTest`
Expected: FAIL because neighbor derivation is missing

- [ ] **Step 3: Implement co-occurrence aggregation and persistence**

```java
public void rebuildNeighbors() {
    // aggregate user co-likes and persist top neighbors per movie
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `./mvnw test -Dtest=CollaborativeNeighborAggregationJobTest`
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add backend/src/main/java/com/hackathon/app/pipeline/CollaborativeNeighborAggregationJob.java backend/src/main/java/com/hackathon/app/model/MovieNeighborDocument.java backend/src/test/java/com/hackathon/app/pipeline/CollaborativeNeighborAggregationJobTest.java
git commit -m "feat: add collaborative neighbor derivation"
```

### Task 6: Build Hybrid Ranking Orchestrator

**Files:**
- Create: `backend/src/main/java/com/hackathon/app/recommendation/HybridRankingService.java`
- Create: `backend/src/main/java/com/hackathon/app/recommendation/RecommendationOrchestrator.java`
- Test: `backend/src/test/java/com/hackathon/app/recommendation/HybridRankingServiceTest.java`

- [ ] **Step 1: Write failing hybrid ranking test**

```java
@Test
void mergesSemanticProfileAndCollaborativeCandidates() {
    // expect collaborative candidates to contribute only when available
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./mvnw test -Dtest=HybridRankingServiceTest`
Expected: FAIL because the hybrid orchestrator is missing

- [ ] **Step 3: Implement minimal hybrid merge and scoring**

```java
public List<RecommendationItem> rank(HybridCandidateSet set, UserProfileDocument profile) {
    // combine semantic, profile, collaborative, popularity, and fallback signals
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `./mvnw test -Dtest=HybridRankingServiceTest`
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add backend/src/main/java/com/hackathon/app/recommendation/HybridRankingService.java backend/src/main/java/com/hackathon/app/recommendation/RecommendationOrchestrator.java backend/src/test/java/com/hackathon/app/recommendation/HybridRankingServiceTest.java
git commit -m "feat: add collaborative hybrid ranking"
```

### Task 7: Build Explainability And Collaborative Gating

**Files:**
- Create: `backend/src/main/java/com/hackathon/app/recommendation/ExplanationService.java`
- Test: `backend/src/test/java/com/hackathon/app/recommendation/ExplanationServiceTest.java`

- [ ] **Step 1: Write failing explanation test**

```java
@Test
void collaborativeReasonAppearsOnlyWhenCollaborativeSignalWasUsed() {
    // expect similar_users_liked only when collaborative source contributed
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./mvnw test -Dtest=ExplanationServiceTest`
Expected: FAIL because explanation gating logic is missing

- [ ] **Step 3: Implement reason generation and gating**

```java
public List<ReasonDto> explain(RankingContext context) {
    // emit only truthful reasons from active signals
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `./mvnw test -Dtest=ExplanationServiceTest`
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add backend/src/main/java/com/hackathon/app/recommendation/ExplanationService.java backend/src/test/java/com/hackathon/app/recommendation/ExplanationServiceTest.java
git commit -m "feat: add truthful explanation gating"
```

### Task 8: Build Frontend End-To-End Flow

**Files:**
- Create: `frontend/src/app/page.tsx`
- Create: `frontend/src/app/search/page.tsx`
- Create: `frontend/src/app/movie/[movieId]/page.tsx`
- Test: `frontend/tests/e2e/recommendation-flow.spec.ts`

- [ ] **Step 1: Write failing end-to-end test for collaborative-hybrid flow**

```ts
test('user likes movies and sees recommendation changes', async ({ page }) => {
  // verify cold start, like flow, and updated recommendations
})
```

- [ ] **Step 2: Run test to verify it fails**

Run: `pnpm test:e2e recommendation-flow.spec.ts`
Expected: FAIL because pages or wiring are incomplete

- [ ] **Step 3: Implement minimal UI and event wiring**

```ts
// render recommendation sections and reason chips using the canonical payload shape
```

- [ ] **Step 4: Run test to verify it passes**

Run: `pnpm test:e2e recommendation-flow.spec.ts`
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add frontend/src/app frontend/src/components frontend/tests/e2e/recommendation-flow.spec.ts
git commit -m "feat: add collaborative hybrid demo flow"
```

---

## Chunk 6: Phase Exit Criteria

## 13. Phase Exit Criteria

### Phase 0 Exit

- API contracts are frozen
- collection schemas are frozen
- all 3 developers can work independently

### Phase 1 Exit

- semantic search works for validated query fixtures
- similar-movie retrieval works
- frontend can render search and detail responses

### Phase 2 Exit

- event ingestion is idempotent
- user profiles update after positive actions
- recommendations change after 2 to 3 likes

### Phase 3 Exit

- collaborative neighbors are generated from co-liked data
- collaborative candidates contribute in personalized mode when available
- collaborative reasons are shown only when truly used

### Phase 4 Exit

- fallback behavior is deterministic and visible
- unavailable titles are filtered from all user-facing results
- demo personas and query fixtures are fully rehearsed

---

## Chunk 7: Verification Handoff

## 14. Verification Commands

- Contract tests: `./mvnw test -Dtest=ContractShapeTest`
- Search tests: `./mvnw test -Dtest=SearchControllerTest`
- Event tests: `./mvnw test -Dtest=EventControllerTest`
- Profile aggregation tests: `./mvnw test -Dtest=UserProfileAggregationJobTest`
- Collaborative neighbor tests: `./mvnw test -Dtest=CollaborativeNeighborAggregationJobTest`
- Hybrid ranking tests: `./mvnw test -Dtest=HybridRankingServiceTest`
- Explanation tests: `./mvnw test -Dtest=ExplanationServiceTest`
- Frontend end-to-end flow: `pnpm test:e2e recommendation-flow.spec.ts`

## 15. Final Notes

- Collaborative signals must strengthen the hybrid engine, not replace semantic and profile safety paths.
- The system must remain demoable if collaborative support is incomplete, and collaborative support should only ship when it is stable, truthful, and does not regress the MVP baseline.
- If collaborative logic is enabled, `similar_users_liked` must be backed by real collaborative candidate generation and ranking participation.
- This plan is designed so 3 developers can work concurrently with low coordination overhead once contracts are frozen.
