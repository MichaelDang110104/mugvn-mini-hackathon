# Hackathon Documents

Internal engineering documentation for the MUGVN Mini Hackathon 2026 movie recommendation project.

This repository is for developers only. It contains the minimum useful document set needed to implement, reason about, and verify the MongoDB-centered recommendation engine.

## What This Project Is

This project is a movie recommendation platform built for the MongoDB-focused hackathon.

The core of the project is not a streaming UI clone. The core is the recommendation engine.

The system is expected to demonstrate:

- semantic movie discovery using MongoDB Vector Search
- personalized recommendations driven by user behavior
- explainable recommendation reasons
- deterministic cold-start behavior
- deterministic fallback behavior when vector retrieval or personalization is degraded
- AWS-hosted delivery with MongoDB Atlas as the primary data and recommendation platform

## Who Should Read This Repository

This repository is for developers who need to:

- understand the intended MVP scope
- build the backend APIs and frontend behavior
- implement the recommendation engine correctly
- validate that the recommendation behavior is real and not hand-wavy
- verify the deployed system before demo or submission

## Repository Structure

- `document/information/`
  - planning and design guidance
- `document/test-verification/`
  - test, verification, and release-readiness guidance

## Canonical Documents

### `document/information/2026-05-14-movie-recommendation-platform.md`

This is the canonical implementation plan.

It tells developers:

- what the MVP includes
- what the architecture is
- what collections and APIs must exist
- what fallbacks are mandatory
- what milestones and release gates exist
- what operational and submission constraints matter

Use this document first when you need the authoritative project boundary.

### `document/information/mongodb-recommendation-design.md`

This is the canonical recommendation-engine design.

It tells developers:

- how the recommendation engine should work
- how MongoDB Vector Search should be used
- how Aggregation Pipeline should support trending, profile derivation, and reranking
- what serving modes exist
- what tradeoffs were chosen for the MVP
- how the design aligns with MongoDB-native recommendation patterns

Use this document when implementing or reviewing recommendation logic.

### `document/test-verification/verification-and-test-spec.md`

This is the canonical verification and demo-readiness spec.

It tells developers:

- what must be tested
- what outputs are expected
- what fallback behavior must be verified
- what counts as demo-ready
- what evidence is required before claiming readiness

Use this document as the release gate.

### `document/information/technology-stack.md`

This is the canonical stack baseline.

It tells developers:

- which technologies are required
- which Spring Boot modules should be used
- how to keep dependencies minimal
- how MongoDB and Spring AI fit the recommendation engine

Use this document when setting up or reviewing implementation dependencies.

### `document/information/deliverable.md`

This is the canonical deliverable checklist.

It tells developers:

- what must be delivered for submission
- what internal engineering outputs are required
- what feature, technical, demo, and documentation deliverables must exist

Use this document when tracking submission completeness.

## Recommended Reading Order

1. `document/information/2026-05-14-movie-recommendation-platform.md`
2. `document/information/technology-stack.md`
3. `document/information/mongodb-recommendation-design.md`
4. `document/information/deliverable.md`
5. `document/test-verification/verification-and-test-spec.md`

This order matters because:

- the implementation plan defines the project boundary
- the technology stack defines the Spring Boot and MongoDB implementation baseline
- the recommendation design defines how the core engine works
- the deliverables doc defines what the team must package and prove
- the verification spec defines how to prove the implementation is correct

## Architecture Snapshot

Current intended MVP stack:

- Next.js frontend
- Spring Boot backend
- Spring Data MongoDB for persistence access
- Spring AI for embedding and AI integration
- MongoDB Atlas as the canonical application and recommendation database
- MongoDB Vector Search for semantic retrieval
- MongoDB Aggregation Pipeline for trending, profile derivation, and recommendation support logic
- AWS S3 + CloudFront or equivalent static frontend hosting
- AWS App Runner for backend hosting
- AWS Secrets Manager and CloudWatch for basic operational support

## Canonical Serving Modes

The system should only use these serving modes unless the documents are explicitly updated together:

- `semantic`
- `personalized`
- `cold_start`
- `fallback_text`

These mode names must remain consistent across:

- API responses
- frontend rendering logic
- recommendation logs
- verification outputs

## Canonical Data Model Expectations

The minimum important collections are:

- `movies`
- `users`
- `user_events`
- `user_profiles`
- `recommendation_logs`

Important invariants:

- embeddings live in `movies.embedding` for MVP simplicity
- `movies.availability` exists and is used to suppress unavailable titles
- `users.sessionId` is unique
- `user_events.eventId` is unique for idempotency
- `recommendation_logs` capture serving mode, candidate context, returned titles, and reason codes

## Canonical API Expectations

Main endpoints:

- `GET /api/movies/search`
- `GET /api/movies/:movieId`
- `POST /api/events`
- `GET /api/recommendations`

Shared response expectations for search and recommendation endpoints:

- top-level `items`
- top-level `mode`
- top-level `fallbackUsed` when fallback applies
- each item includes `movie`, `score`, and `reasons`

This is an abbreviated summary. Search-specific fields such as top-level `query` and optional `hint`, and recommendation-specific fields such as top-level `generatedAt`, are defined in the canonical implementation plan and verification spec.

## Rules That Must Not Drift

- MongoDB must remain central to the recommendation path, not just present in storage.
- Vector Search is a retrieval signal, not the final business relevance score.
- Search events may contribute weak profile hints, but must not outweigh explicit positive actions like `like`, `save`, or `rate`.
- Cold-start must always return usable recommendations.
- Fallback behavior must always return useful content instead of blank states.
- Explainability must be truthful to the actual ranking path.
- Unavailable titles must not appear in final recommendation results.

## Verification Expectations

Before calling the system demo-ready, developers must verify:

- semantic search works for validated demo queries
- recommendation output changes after positive user actions
- event deduplication works
- cold-start flow is non-empty
- Vector Search degradation still produces usable fallback output
- unavailable titles do not leak into final responses
- explanation reason codes match the actual ranking signals
- deployed AWS environment behaves the same as rehearsed staging

Use `document/test-verification/verification-and-test-spec.md` as the final gate.

## If You Change The Design

If implementation forces a change to any of the following:

- API response shape
- collection schema
- serving modes
- fallback behavior
- ranking signals
- verification pass criteria

update all relevant canonical and baseline documents together. Do not change one document and leave the others stale.

## Document Ownership Boundaries

To avoid drift:

- the implementation plan owns scope, milestones, fixed decisions, and canonical contracts
- the technology stack owns the approved Spring Boot, Spring AI, MongoDB, and AWS dependency baseline
- the recommendation design owns recommendation logic, MongoDB usage, ranking flow, and tradeoffs
- the deliverables doc owns submission and packaging completeness
- the verification spec owns tests, expected outputs, and sign-off criteria

## Current Retained Document Set

This repository intentionally keeps only these dev-facing docs:

- `README.md`
- `document/information/2026-05-14-movie-recommendation-platform.md`
- `document/information/technology-stack.md`
- `document/information/mongodb-recommendation-design.md`
- `document/information/deliverable.md`
- `document/test-verification/verification-and-test-spec.md`

This is the intended minimum useful engineering documentation set for the project.
