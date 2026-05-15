# Deliverables

## 1. Purpose

This document defines everything the team must deliver for the hackathon submission and for internal engineering readiness.

It is aligned with the canonical implementation plan, recommendation design, and verification specification. If any deliverable listed here conflicts with those documents, update the canonical docs together before changing this file.

---

## 2. Deliverable Categories

The team must deliver work in 4 categories:

- product deliverables
- technical documentation deliverables
- demo deliverables
- engineering readiness deliverables

---

## 3. Required Hackathon Submission Deliverables

These are the official deliverables required for Round 1.

### 3.1 Demo Video

The team must deliver:

- one demo video
- maximum duration: 10 minutes
- clear explanation of:
  - use case
  - solution demo
  - results achieved

### 3.2 MVP Definition

The team must deliver a document that clearly states:

- what the MVP is
- what is in scope
- what is not in scope
- what problem it solves

This is currently covered by:

- `document/information/2026-05-14-movie-recommendation-platform.md`

### 3.3 System Architecture Document

The team must deliver a technical architecture description that covers:

- frontend
- backend
- MongoDB role
- AWS role
- request flow
- recommendation flow
- fallback behavior

This is currently covered by:

- `document/information/2026-05-14-movie-recommendation-platform.md`
- `document/information/mongodb-recommendation-design.md`

### 3.4 Data Architecture / Schema Document

The team must deliver:

- core MongoDB collections
- field shapes
- indexing strategy
- event model
- profile model
- recommendation log model

This is currently covered by:

- `document/information/2026-05-14-movie-recommendation-platform.md`
- `document/information/mongodb-recommendation-design.md`

### 3.5 Sample Data Documentation

The team must deliver:

- what dataset is used
- where it came from
- what fields it contains
- what transformations were applied
- what seeded demo personas and query fixtures are used

This must be delivered as a first-class submission artifact, even if its content is derived from multiple internal documents.

This is currently covered partially by:

- `document/information/2026-05-14-movie-recommendation-platform.md`
- `document/test-verification/verification-and-test-spec.md`

The team should package a final sample-data deliverable that consolidates:

- dataset source
- dataset field coverage
- transformations
- seeded personas
- validated query fixtures
- dataset snapshot identifier

---

## 4. Internal Engineering Deliverables

These are not optional for the team even if they are not all part of the official upload package.

### 4.1 Deployed Application

The team must deliver a working deployed environment with:

- frontend accessible from a public URL
- backend accessible from the frontend
- MongoDB Atlas connected
- recommendation engine functioning end-to-end

### 4.2 Working Recommendation Engine

The team must deliver a recommendation engine that can:

- perform semantic search using MongoDB Vector Search
- track user behavior through MongoDB-stored events
- derive or use profile signals for personalization
- return personalized or fallback recommendations
- expose truthful reason codes

### 4.3 Verification Evidence

The team must deliver internal proof that the system was tested.

Minimum required evidence:

- seeded persona test evidence
- validated query fixture results
- event ingestion verification evidence
- recommendation change verification evidence
- fallback verification evidence
- deployment rehearsal verification evidence
- verification date and owner per run
- environment name per run
- dataset snapshot identifier
- embedding version
- commit SHA or build identifier
- screenshots for demo-critical flows
- sample API responses for seeded scenarios
- failure list and final pass or fail decision

The verification standard is defined by:

- `document/test-verification/verification-and-test-spec.md`

---

## 5. Feature Deliverables

The MVP must deliver these user-visible capabilities.

### 5.1 Movie Catalog

- browseable movie catalog
- stable title, overview, genres, and poster display
- no broken or invalid movie entries in demo-critical flows

### 5.2 Semantic Search

- natural language search
- MongoDB Vector Search-backed retrieval
- empty-result fallback behavior
- degraded vector fallback behavior

### 5.3 Movie Detail Recommendations

- similar-movie section
- recommendation reasons where appropriate
- unavailable items filtered out

### 5.4 User Behavior Tracking

- capture `search`, `view`, `click`, `like`, `save`, `rate`
- deduplicate by `eventId`
- store persisted timestamps

### 5.5 Personalized Homepage Recommendations

- cold-start mode
- personalized mode
- fallback mode
- visible recommendation improvement after positive actions

### 5.6 Explainability

- expose reason codes
- map reason codes to human-readable labels
- ensure explanations are truthful to actual ranking signals

---

## 6. Technical Deliverables

The backend and data platform must deliver the following technical capabilities.

### 6.1 MongoDB Deliverables

- `movies` collection
- `users` collection
- `user_events` collection
- `user_profiles` collection
- `recommendation_logs` collection
- vector index on `movies.embedding`
- required supporting indexes for session, event, and profile access
- aggregation-driven trending, profile derivation, or reranking support logic

### 6.2 Backend Deliverables

- `GET /api/movies/search`
- `GET /api/movies/:movieId`
- `POST /api/events`
- `GET /api/recommendations`
- anonymous `sessionId` support across recommendation-critical flows
- top-level `mode` and `fallbackUsed` response metadata where required
- top-level `query` for search responses
- top-level `generatedAt` for recommendation responses

### 6.3 AWS Deliverables

- deployed backend runtime
- deployed frontend runtime
- secrets management
- logging and basic observability
- stable demo environment

---

## 7. Documentation Deliverables

The final developer-facing document set must include:

- `README.md`
- `document/information/2026-05-14-movie-recommendation-platform.md`
- `document/information/mongodb-recommendation-design.md`
- `document/test-verification/verification-and-test-spec.md`
- `document/information/deliverable.md`
- `document/information/technology-stack.md`

---

## 8. Demo Deliverables

The team must have these ready before recording or presenting:

- at least 3 fixed demo personas
- at least 10 validated demo query fixtures
- known-good recommendation outcomes
- fallback scenario that still looks credible
- stable public URL
- backup prerecorded video or fallback clip
- expected top categories per persona and query
- expected fallback behavior per failure mode
- validated dataset snapshot identifier

---

## 9. Acceptance Checklist

The deliverable set is complete only when all of the following are true:

- [ ] official Round 1 required documents are complete
- [ ] demo video is ready and under 10 minutes
- [ ] deployed app works from public URLs
- [ ] MongoDB-centered recommendation engine works end-to-end
- [ ] fallback behavior works for empty queries and degraded semantic retrieval
- [ ] verification spec release gate has passed
- [ ] the final document set is internally consistent

---

## 10. Non-Deliverables

These are explicitly not required for the hackathon MVP:

- full streaming playback
- native mobile apps
- complex user account systems
- large-scale ML training infrastructure
- production-grade collaborative filtering at scale
- multi-service microservice decomposition

---

## 11. Final Recommendation

The team should treat this document as the submission and execution checklist. If a capability exists in code but is not packaged into a deliverable that can be demonstrated, verified, or explained, it is not complete.
