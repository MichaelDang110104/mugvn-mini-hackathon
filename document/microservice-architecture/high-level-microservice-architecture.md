# High-Level Microservice Architecture

## 1. Purpose

This document defines the proposed minimal microservice architecture for the project.

It is intentionally minimal. It keeps the frontend-facing API contract stable while separating the backend into a small set of services with clear responsibilities.

---

## 2. Design Goals

- preserve the current external API contract at the gateway boundary
- split the backend by domain, not by every technical concern
- keep MongoDB Atlas central to recommendation behavior
- keep the frontend deployable as a real Next.js application on EC2
- use AWS and Cloudflare infrastructure that is realistic for a future-state evolution

This EC2-hosted frontend is an explicit alternative-track exception to the current monolith baseline, which still documents `S3 + CloudFront` for frontend hosting.

---

## 3. High-Level Infrastructure

```text
                +-------------------+
                |   Cloudflare      |
                | DNS / WAF / Cache |
                +---------+---------+
                          |
                          v
                +-------------------+
                |   CloudFront      |
                | CDN / Edge Cache  |
                +---------+---------+
                          |
         +----------------+----------------+
         |                                 |
         v                                 v
+----------------------+         +----------------------+
| EC2: Next.js Frontend|         | S3 Static Assets     |
| - pages              |         | - images             |
| - SSR / runtime UI   |         | - build artifacts    |
| - session handling   |         | - import files       |
+----------+-----------+         +----------------------+
           |
           v
+----------------------+
| EC2: API Gateway/BFF |
| - session bootstrap  |
| - auth/session pass  |
| - route orchestration|
| - response shaping   |
+----+---------+-------+
     |         |        \
     |         |         \
     v         v          v
+----------+ +--------------------+ +--------------+
| Catalog  | | Recommendation     | | Event        |
| Service  | | Service            | | Service      |
+----------+ +--------------------+ +--------------+
                    |                      |
                    v                      v
              +-----------+          +-----------+
              | Profile   |          | Derivation|
              | Service   |          | Worker    |
              +-----------+          +-----------+
                    \                  /
                     \                /
                      v              v
                    +----------------------+
                    |    MongoDB Atlas     |
                    | movies               |
                    | users                |
                    | user_events          |
                    | user_profiles        |
                    | recommendation_logs  |
                    | movie_neighbors      |
                    | movie_trending_daily |
                    | editorial_seed_sets  |
                    +----------------------+
```

---

## 4. Frontend Flow Through The Gateway

```text
User opens app
  |
  v
Next.js frontend on EC2
  |
  +--> if no sessionId, request passes through gateway
  |        |
  |        +--> gateway mints anonymous session if needed
  |        +--> returns X-Session-Id
  |
  +--> frontend requests Home / Search / Detail through gateway
  |
  +--> gateway routes request to correct backend service(s)
  |
  +--> gateway returns normalized contract shape to frontend
  |
  +--> frontend renders page and sends user events back through gateway
```

---

## 5. Service Responsibilities

### 5.1 API Gateway / BFF

Owns:

- stable frontend-facing API contract
- session bootstrap and propagation
- request routing
- aggregation of service responses when needed
- response metadata shaping
- public fallback truthfulness

The gateway should preserve the full external API contract package unchanged, including:

- endpoint paths
- request parameters and headers
- session bootstrap behavior via `X-Session-Id`
- response metadata such as `mode`, `fallbackUsed`, `query`, and `generatedAt`
- public error semantics
- compatibility-sensitive behavior documented in `document/api-contract/`

It should be the only service directly called by the frontend.

### 5.2 Catalog Service

Owns:

- movie catalog reads
- movie detail reads
- availability-aware movie retrieval support

### 5.3 Recommendation Service

Owns:

- recommendation orchestration
- semantic retrieval usage
- hybrid ranking
- serving mode selection support
- explanation assembly

### 5.4 Event Service

Owns:

- event validation
- event idempotency
- event persistence
- noisy event coalescing rules

### 5.5 Profile Service

Owns:

- user profile reads
- serving-time profile access
- synchronous lightweight profile update coordination where needed for recommendation-critical refresh flows

### 5.6 Derivation Worker

Owns:

- trending aggregation
- non-critical profile aggregation jobs and recomputation support
- collaborative neighbor generation
- editorial seed maintenance if needed

The derivation worker must not sit on the critical synchronous path from `POST /api/events` to refreshed recommendation output.

---

## 6. Contract Boundary Strategy

### External Boundary

The existing frontend/backend API contract should be preserved at the gateway boundary unchanged.

That means the frontend should continue to consume the same public shapes for:

- `GET /api/movies/search`
- `GET /api/movies/{movieId}`
- `POST /api/events`
- `GET /api/recommendations`

This reuse must include the full external contract package, not only route names. In particular, the gateway should preserve:

- `X-Session-Id` bootstrap behavior
- top-level response metadata
- event idempotency semantics
- degraded-mode truthfulness
- compatibility and frozen-field expectations

### Internal Boundary

Internal service-to-service contracts can be narrower and domain-specific.

Examples:

- recommendation service may call catalog service for movie hydration
- gateway may call recommendation service and profile service in one request path
- event service may trigger derivation workflows asynchronously or indirectly

---

## 7. Request Flow Examples

### 7.1 Search Flow

```text
Frontend
  -> API Gateway
     -> Recommendation Service
        -> MongoDB Vector Search / fallback path
     -> Catalog Service (optional hydration)
  <- API Gateway returns search contract
```

### 7.2 Movie Detail Flow

```text
Frontend
  -> API Gateway
     -> Catalog Service for movie detail
     -> Recommendation Service for similar movies
  <- API Gateway returns movie + similarMovies + mode + fallbackUsed
```

### 7.3 Event Flow

```text
Frontend
  -> API Gateway
     -> Event Service validates and persists event
     -> optional downstream trigger to profile/derivation path
  <- API Gateway returns accepted/profileUpdated/rerankedUsingRecentEvents
```

### 7.4 Homepage Recommendation Flow

```text
Frontend
  -> API Gateway
     -> Profile Service for user profile context
     -> Recommendation Service for hybrid ranking
     -> optional Catalog Service hydration
  <- API Gateway returns items + mode + fallbackUsed + generatedAt
```

---

## 8. Why EC2 For The Frontend

The frontend is modeled on EC2 because this architecture assumes a real Next.js runtime rather than a purely static export.

That supports:

- server-side rendering if needed
- runtime request handling
- direct control over session-aware frontend behavior
- consistent alignment with the gateway-based backend topology

S3 is still useful, but only for:

- images
- artifacts
- static assets
- import files

---

## 9. Why This Split Is Minimal

This is intentionally not a full platform decomposition.

It avoids creating separate services for every concern such as:

- auth service
- search-only service
- analytics service
- queue processing service
- observability service

Those can be added later if needed.

The current split is the smallest one that still gives:

- clear service boundaries
- gateway-controlled public contracts
- independent recommendation-domain evolution
- a believable future-state architecture

---

## 10. Risks And Tradeoffs

### Strengths

- preserves the current API contract investment
- isolates recommendation-heavy logic
- gives a clear scaling story
- keeps MongoDB central

### Tradeoffs

- higher ops complexity than the monolith
- more network hops
- more internal contract maintenance
- more deployment coordination

---

## 11. Reuse Strategy

The microservice architecture should reuse the current documentation set in these ways:

### Reuse Directly

- external API contracts from `document/api-contract/`
- recommendation semantics from `document/information/mongodb-recommendation-design.md`
- deliverable expectations from `document/information/deliverable.md`

### Reuse With Service-Boundary Adaptation

- implementation sequencing from `document/plan/collaborative-hybrid-recommendation-implementation-plan.md`
- verification scenarios from `document/test-verification/verification-and-test-spec.md`
- stack baseline from `document/information/technology-stack.md`

### Reuse With Architecture Re-mapping

- MVP scope, acceptance criteria, and fallback rules from `document/information/2026-05-14-movie-recommendation-platform.md`
- remap module responsibilities into service responsibilities:
  - monolith controller logic -> API Gateway + domain services
  - monolith recommendation modules -> Recommendation Service
  - monolith event ingestion -> Event Service
  - monolith profile logic -> Profile Service
  - monolith derivation jobs -> Derivation Worker

---

## 12. Recommendation

Use this architecture as the minimal microservice alternative track.

Keep the current modular monolith as the active delivery path unless the team intentionally chooses to migrate. The gateway should preserve the public contract so the frontend does not need to be redesigned when moving from monolith to microservices.
