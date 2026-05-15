# High-Level Microservice Architecture

## 1. Purpose

This document defines the revised minimal microservice architecture for the project.

It is intentionally minimal. It keeps the frontend-facing API contract stable while separating the backend into a small set of services with clear responsibilities.

---

## 2. Design Goals

- preserve the current external API contract at the gateway boundary
- split the backend by domain, not by every technical concern
- keep MongoDB Atlas central to recommendation behavior while using Postgres for structured operational data
- keep the frontend deployable as a real Next.js application on EC2
- use AWS and Cloudflare infrastructure that is realistic for a future-state evolution
- add an explicit asynchronous derivation path for heavy or slow processing

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
                    v
             +----------------------+
             | AWS ALB              |
             | Public Load Balancer |
             +-----+-----------+----+
                   |           |
                   |           v
                   |    +----------------------+
                   |    | S3 Static Assets     |
                   |    | - images             |
                   |    | - build artifacts    |
                   |    | - import files       |
                   |    +----------------------+
                   |
                   v
        +------------------------------+
        | Private App Subnets          |
        |------------------------------|
        | EC2: Next.js Frontend        |
        | EC2: API Gateway / BFF       |
        | EC2: Catalog Service         |
        | EC2: Recommendation Service  |
        | EC2: Event Service           |
        | EC2: Profile Service         |
        | EC2: Embedding Service       |
        | EC2: Derivation Worker       |
        | RabbitMQ                     |
        +--------------+---------------+
                       |
          +------------+--------------------+
          |                                 |
          v                                 v
 +----------------------+         +----------------------+
 | MongoDB Atlas        |         | Postgres             |
 | - movies             |         | - users/accounts     |
 | - user_events        |         | - admin/editorial    |
 | - user_profiles      |         | - job tracking       |
 | - recommendation_logs|         | - config/workflows   |
 | - search_request_logs|         | - operational data   |
 | - movie_neighbors    |         +----------------------+
 | - movie_trending     |
 | - editorial_seed_sets|
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

## 5. Network And Runtime Layout

- Cloudflare is the outermost public edge and DNS provider.
- CloudFront is the CDN in front of AWS origins.
- ALB is the explicit AWS public load balancer.
- ALB should route traffic into private app subnets.
- Frontend, gateway, and backend services should live in private app subnets, not be directly internet-exposed.
- MongoDB Atlas and Postgres should be reachable only through controlled backend network paths.
- RabbitMQ should remain private and internal-only.

## 6. Service Responsibilities

### 6.1 API Gateway / BFF

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

### 6.2 Catalog Service

Owns:

- movie catalog reads
- movie detail reads
- recommendation-facing movie projection access
- availability-aware movie retrieval support

Primary data:

- MongoDB for recommendation-facing movie documents with embeddings
- Postgres optional source of truth for structured movie business metadata

### 6.3 Recommendation Service

Owns:

- recommendation orchestration
- semantic retrieval usage
- hybrid ranking
- serving mode selection support
- explanation assembly

Primary data:

- MongoDB

### 6.4 Event Service

Owns:

- event validation
- event idempotency
- event persistence
- noisy event coalescing rules

Primary data:

- MongoDB for raw recommendation events
- optional Postgres for operational idempotency/control-plane records if needed later

### 6.5 Profile Service

Owns:

- user profile reads
- serving-time profile access
- synchronous lightweight profile update coordination where needed for recommendation-critical refresh flows

Primary data:

- MongoDB for derived recommendation profiles
- Postgres optional for user/account data if the platform grows beyond anonymous sessions

### 6.6 Embedding Service

Owns:

- embedding generation requests
- calls to third-party embedding APIs or local Ollama-style model runtimes
- writing completed embeddings back to MongoDB recommendation-facing movie documents

Primary pattern:

- asynchronous, queue-driven processing

### 6.7 Derivation Worker

Owns:

- trending aggregation
- non-critical profile aggregation jobs and recomputation support
- collaborative neighbor generation
- editorial seed maintenance if needed

Primary pattern:

- asynchronous, queue-driven processing

The derivation worker must not sit on the critical synchronous path from `POST /api/events` to refreshed recommendation output.

### 6.8 RabbitMQ

Owns:

- delivery of asynchronous jobs to workers
- decoupling heavy CPU or slow network work from user-facing request paths

Use it for:

- embedding generation jobs
- collaborative neighbor rebuild jobs
- trending recomputation jobs
- non-critical profile recomputation jobs
- editorial seed refresh jobs

---

## 7. Contract Boundary Strategy

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

## 8. Request Flow Examples

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
     -> optional synchronous lightweight profile refresh path
     -> optional async queue publish to RabbitMQ for heavy derivation jobs
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

### 8.5 Embedding Generation Flow

```text
Catalog change or import event
  -> API Gateway or internal admin path
     -> publish embedding job to RabbitMQ
        -> Embedding Service consumes job
           -> calls provider via Spring AI or local Ollama-compatible runtime
           -> writes embedding back to MongoDB movie document
```

---

## 9. Data Ownership Strategy

### MongoDB

Use MongoDB for:

- recommendation-facing movie documents
- embeddings
- user events
- user profiles
- recommendation logs
- search request logs
- collaborative neighbors
- trending outputs
- editorial seed sets

### Postgres

Use Postgres for:

- structured user and account data if accounts exist
- admin and editorial workflow tables
- job execution tracking
- operational metadata and governance records
- structured movie business metadata if a relational source of truth is required

This split keeps recommendation logic fast and document-friendly in MongoDB while avoiding forcing all structured operational data into the same database.

---

## 10. Pattern Recommendation

Use a simple pattern set:

- synchronous request/response for user-facing recommendation freshness
- asynchronous queue-based derivation for heavy or slow tasks
- no full SAGA by default

### Why Not Full SAGA

- too much complexity for this platform right now
- most user-facing recommendation flows do not need distributed business transactions
- idempotent events plus async derivation are simpler and safer here

### Recommended Patterns

- request/response for search, movie detail, and homepage recommendations
- RabbitMQ for heavy background processing
- idempotent event handling
- outbox/event choreography only later if the platform grows

---

## 11. Why EC2 For The Frontend

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

## 12. Why This Split Is Minimal

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

## 13. Pain Points And Best-Practice Improvements

The original minimal microservice draft had several weaknesses. This revised version addresses them explicitly.

### Pain Point 1: No explicit load balancer

Fix:

- add AWS ALB explicitly as the public AWS load balancer

### Pain Point 2: No clear network model

Fix:

- add VPC, public ingress, and private app-subnet assumptions

### Pain Point 3: Everything in MongoDB

Fix:

- split recommendation-domain data into MongoDB and structured operational data into Postgres

### Pain Point 4: Worker ambiguity on critical path

Fix:

- keep Embedding Service and Derivation Worker fully asynchronous through RabbitMQ
- keep recommendation refresh path synchronous and lightweight

### Pain Point 5: Gateway contract drift risk

Fix:

- preserve the full external API contract package at the gateway boundary unchanged

---

## 14. Risks And Tradeoffs

### Strengths

- preserves the current API contract investment
- isolates recommendation-heavy logic
- gives a clear scaling story
- keeps MongoDB central where it adds value
- avoids forcing all structured operational data into MongoDB
- gives a clear async pattern for embedding and derivation work

### Tradeoffs

- higher ops complexity than the monolith
- more network hops
- more internal contract maintenance
- more deployment coordination
- more infrastructure than the App Runner monolith baseline
- RabbitMQ introduces another moving part to operate

---

## 15. Reuse Strategy

The microservice architecture should reuse the current documentation set in these ways:

### Reuse Directly

- external API contracts from `document/api-contract/`
- recommendation semantics from `document/information/mongodb-recommendation-design.md`
- deliverable expectations from `document/information/deliverable.md`

### Reuse With Service-Boundary Adaptation

- implementation sequencing from `document/plan/collaborative-hybrid-recommendation-implementation-plan.md`
- verification scenarios from `document/test-verification/verification-and-test-spec.md`
- stack baseline from `document/information/technology-stack.md`

Important reuse exception:

- the monolith baseline still places the frontend on `S3 + CloudFront`
- this microservice track intentionally uses a real Next.js runtime on EC2 instead

### Reuse With Architecture Re-mapping

- MVP scope, acceptance criteria, and fallback rules from `document/information/2026-05-14-movie-recommendation-platform.md`
- remap module responsibilities into service responsibilities:
  - monolith controller logic -> API Gateway + domain services
  - monolith recommendation modules -> Recommendation Service
  - monolith event ingestion -> Event Service
  - monolith profile logic -> Profile Service
  - monolith derivation jobs -> Derivation Worker

---

## 16. Recommendation

Use this architecture as the minimal microservice alternative track.

Keep the current modular monolith as the active delivery path unless the team intentionally chooses to migrate. The gateway should preserve the public contract so the frontend does not need to be redesigned when moving from monolith to microservices.
