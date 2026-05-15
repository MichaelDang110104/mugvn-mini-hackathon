# API And Data Contract High-Level Design

## 1. Purpose

This document defines the high-level design for the API and data contract package for the movie recommendation platform.

It exists to make all contract decisions explicit before implementation details drift across backend, frontend, MongoDB derivation jobs, verification logic, and submission packaging.

This contract package covers both:

- external application contracts
- internal system contracts

---

## 2. Goals

- keep all external API contracts stable and explicit
- keep internal MongoDB-backed data contracts clear enough for aggregation and verification
- ensure the recommendation engine remains feasible as currently designed
- support the Spring Boot, Spring AI, and MongoDB Atlas baseline
- keep collaborative support compatible with the semantic-and-profile MVP baseline

---

## 3. Contract Philosophy

The platform must use a layered contract model.

### External Contracts

These are the contracts that frontend and other direct consumers rely on.

They include:

- request shapes
- response shapes
- validation behavior
- error behavior
- serving mode semantics

### Internal Contracts

These are the contracts that backend modules, MongoDB collections, derivation jobs, and verification tooling rely on.

They include:

- persisted collection shapes
- derived collection shapes
- event persistence rules
- recommendation log shape
- collaborative neighbor shape
- aggregation job input and output expectations

### Contract Stability Rule

External contracts must be more stable than internal contracts.

Internal contracts may evolve more often, but they must still remain consistent with:

- recommendation design
- verification spec
- implementation plan

---

## 4. Contract Package Structure

The API contract package consists of 5 documents:

1. `document/api-contract/high-level-design.md`
2. `document/api-contract/external-api-contracts.md`
3. `document/api-contract/internal-data-contracts.md`
4. `document/api-contract/event-and-error-contracts.md`
5. `document/api-contract/versioning-and-compatibility.md`

---

## 5. Core Design Decisions

### 5.1 Single-List Result Contract For List Endpoints

Search and homepage recommendation endpoints must use a single top-level `items` list.

Fallback candidates are still returned inside `items`.

Fallback state must be visible via top-level metadata such as:

- `mode`
- `fallbackUsed`
- optional `hint`

Separate undocumented fallback blocks are not allowed.

This rule applies to:

- `GET /api/movies/search`
- `GET /api/recommendations`

It does not replace the `movie` plus `similarMovies` shape used by `GET /api/movies/{movieId}`.

### 5.2 Top-Level Serving Mode

Serving mode must be top-level response metadata, not per-item metadata.

Allowed serving modes:

- `semantic`
- `personalized`
- `cold_start`
- `fallback_text`

### 5.3 Session-Centric Identity

The MVP contract must assume anonymous session-backed identity.

That means:

- `sessionId` is required across recommendation-critical flows
- public APIs must be feasible without full auth
- internal contracts must still support user-profile derivation from session activity

### 5.4 Frontend Flow Compatibility

The contract package must support the actual frontend flow, not just backend storage.

That means the contracts must make the following flows feasible without extra undocumented calls or hidden state:

- homepage cold-start recommendations
- search results with fallback behavior
- movie detail retrieval with similar-movie suggestions
- anonymous session bootstrap and reuse across refresh and revisit
- event submission after search, view, click, like, save, and rate actions
- refreshed recommendation retrieval after positive actions

The frontend must be able to determine from the response alone:

- what mode produced the results
- whether fallback was used
- whether the response shape is search, detail, or recommendation specific

The contract package must therefore explicitly define how a brand-new frontend session obtains and reuses `sessionId`.

Canonical bootstrap rule:

- the backend may mint a new anonymous session when a recommendation-critical request arrives without `sessionId`
- the backend must return the minted session through the `X-Session-Id` response header
- the frontend must reuse that value through the `X-Session-Id` request header or an explicitly supported request parameter

### 5.5 Explicit Rating Semantics

`rate` cannot be treated as a generic positive action.

The contract must define:

- the accepted rating scale
- whether low ratings are neutral or negative
- how rating events influence user profiles and recommendation behavior

Canonical baseline:

- `4` and `5` are positive signals
- `3` is neutral
- `1` and `2` are low ratings and must not be treated as positive preference signals

### 5.6 Event Idempotency And Timestamp Rules

Event contracts must make these guarantees explicit:

- `eventId` is required and idempotent
- every persisted event must contain a valid timestamp
- if timestamp is omitted by the client, the backend assigns one before persistence

### 5.7 Availability Is A Reserved Serving Field

`movies.availability` is a required contract field because:

- unavailable items must never appear in final user-facing result lists
- verification depends on availability filtering being explicit and stable

### 5.8 Collaborative Signals Are Gated

Collaborative support is allowed, but it must not leak into the contract as if it is always active.

That means:

- collaborative reason codes must only appear when collaborative signals actually participated
- collaborative-derived fields must be optional where appropriate
- the system must remain fully contract-valid if collaborative support is not active yet

If top-level `mode` is `cold_start` or `fallback_text`, collaborative explanations must not appear.

---

## 6. External API Surface

The minimum external API surface required by current docs is:

- `GET /api/movies/search`
- `GET /api/movies/{movieId}`
- `POST /api/events`
- `GET /api/recommendations`

These endpoints are sufficient for:

- semantic discovery
- movie detail retrieval
- event capture
- homepage recommendation retrieval

Recommendation list responses must also carry endpoint-specific metadata where required:

- search responses must carry `query` and may carry `hint`
- recommendation responses must carry `generatedAt`

No extra public endpoints should be introduced unless a verified need appears.

---

## 7. Internal Contract Surface

The internal contract package must include at least these data shapes:

- `movies`
- `users`
- `user_events`
- `user_profiles`
- `recommendation_logs`
- collaborative neighbor documents when collaborative support is enabled
- trending outputs
- editorial seed outputs
- internal ranking inputs and outputs where needed

Region-aware serving and auditability must also be represented explicitly in internal contracts.

The internal package must also define:

- which fields are persisted
- which fields are derived
- which fields are required for verification
- `movies.availability` as a required serving and filtering field

---

## 8. Feasibility Rules

An API contract is only valid if it is feasible with the current system design.

That means:

- no response shape may require undocumented background state
- no contract may assume collaborative support if the canonical docs still treat it as optional
- no field may be required externally if the backend cannot deterministically produce it
- no error semantics may contradict fallback behavior

---

## 9. Verification Model

Each document in this package must be verified at least 3 times:

1. consistency against the implementation plan
2. consistency against the recommendation design and deliverables
3. feasibility and edge-case review against the verification spec

---

## 10. Source Of Truth Hierarchy

For this package:

- external endpoint behavior must align with `document/information/2026-05-14-movie-recommendation-platform.md`
- internal MongoDB shapes must align with `document/information/mongodb-recommendation-design.md`
- required outputs and artifacts must align with `document/information/deliverable.md`
- expected behavior under test must align with `document/test-verification/verification-and-test-spec.md`

If a mismatch is discovered, fix the underlying canonical docs or explicitly reconcile the contract package with them before implementation continues.
