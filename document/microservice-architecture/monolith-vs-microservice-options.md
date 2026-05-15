# Monolith Vs Microservice Options

## 1. Purpose

This document compares the two intended architecture options for the project:

1. the current Spring Boot modular monolith
2. a minimal microservice split

The goal is to make the tradeoffs explicit and to show how the current frontend/backend API contract can be preserved in both models.

---

## 2. Shared Product Scope

Both options support the same product behavior:

- semantic movie search
- movie detail with similar recommendations
- event tracking
- personalized homepage recommendations
- optional collaborative support
- truthful explanations
- deterministic cold-start and fallback behavior

Both options should keep MongoDB Atlas as the central operational and recommendation data platform.

---

## 3. Option 1: Current Modular Monolith

### Shape

```text
Cloudflare
    |
CloudFront
    |
EC2: Next.js frontend
    |
EC2/App Runner: Spring Boot backend
    |
MongoDB Atlas
```

Note:

- this EC2-hosted Next.js frontend is a microservice-track exception
- the current monolith baseline still documents `S3 + CloudFront` frontend hosting

### Characteristics

- one backend deployable
- one main runtime for API logic
- internal modules instead of separate services
- lower operational complexity
- easier debugging
- faster hackathon execution path

### Strengths

- simplest implementation path
- lowest coordination cost
- lowest deployment complexity
- easiest to verify end-to-end

### Weaknesses

- less isolation between responsibilities
- less scalable service separation story
- harder to independently scale recommendation-heavy paths later

---

## 4. Option 2: Minimal Microservice Split

### Shape

```text
Cloudflare
    |
CloudFront
    |
EC2: Next.js frontend
    |
EC2: API Gateway / BFF
    |
    +-------------------+-------------------+-------------------+
    |                   |                   |                   |
    v                   v                   v                   v
Catalog Service   Recommendation Service  Event Service    Profile Service
    |                   |                   |                   |
    +-------------------+-------------------+-------------------+
                            |
                            v
                      MongoDB Atlas
                            |
                            v
                     Derivation Worker
                (trending, profiles, neighbors)
```

### Characteristics

- one gateway as the only frontend-facing backend surface
- backend responsibilities separated by domain
- recommendation-heavy logic isolated from catalog and event capture
- internal services may evolve independently
- more operational complexity than the monolith, but still minimal compared to a full platform split

### Strengths

- clearer ownership boundaries
- easier future scaling of recommendation and event workloads
- preserves one stable API contract at the gateway boundary
- easier to evolve toward queues, workers, and specialized compute later

### Weaknesses

- more deployment units
- more networking, operational, and debugging complexity
- more internal contract management
- higher coordination cost for a small team

---

## 5. What Stays The Same

These elements should remain stable in both options:

- frontend user flows
- public API behavior at the frontend boundary
- serving modes:
  - `semantic`
  - `personalized`
  - `cold_start`
  - `fallback_text`
- MongoDB collections and recommendation concepts
- recommendation explanation rules
- verification expectations

## 6. What Can Be Reused From The Current Documentation Set

### Reuse Without Major Changes

- `document/api-contract/`
  - keep the full frontend-facing contract package unchanged at the gateway boundary, not just the endpoint names
- `document/information/mongodb-recommendation-design.md`
  - keep the same recommendation behavior, serving modes, fallback semantics, and MongoDB-centered retrieval model
- `document/information/deliverable.md`
  - keep the same submission and engineering deliverable expectations

### Reuse With Topology Updates

- `document/test-verification/verification-and-test-spec.md`
  - reuse the feature and behavior verification sections
  - update deployment, service health, and inter-service verification for the microservice topology
- `document/information/technology-stack.md`
  - reuse the Spring Boot, Spring AI, MongoDB, and AWS technology decisions
  - update runtime placement to reflect gateway + service split on EC2
- `document/information/2026-05-14-movie-recommendation-platform.md`
  - reuse MVP scope, goals, serving modes, fallback rules, and acceptance criteria
  - update architecture, deployment, and workstream structure if the team actually migrates to this option

### Reuse As A Migration Reference

- `document/plan/collaborative-hybrid-recommendation-implementation-plan.md`
  - reuse recommendation sequencing, hybrid-ranking priorities, and collaborative gating logic
  - adapt implementation tasks from module-level work to service-level work

---

## 7. What Changes In The Microservice Option

### Gateway Ownership

The API Gateway becomes responsible for:

- session bootstrap
- request routing
- response composition
- public error semantics
- public response metadata such as:
  - `mode`
  - `fallbackUsed`
  - `query`
  - `generatedAt`

### Internal Services

The gateway delegates to internal services instead of modules in the same process.

Suggested responsibility split:

- `catalog-service`
  - movie catalog reads
  - movie detail reads
  - availability-aware filtering support

- `recommendation-service`
  - recommendation orchestration
  - semantic retrieval usage
  - hybrid ranking
  - explanation assembly

- `event-service`
  - event validation
  - event persistence
  - idempotency
  - noisy event coalescing rules

- `profile-service`
  - profile reads
  - synchronous lightweight profile update coordination for recommendation-critical refresh paths
  - preference state serving

- `derivation-worker`
  - trending derivation
  - non-critical profile aggregation jobs and recomputation support
  - collaborative neighbor generation
  - editorial seed material refresh if needed

The derivation worker must not be required on the synchronous path from user event to immediate recommendation refresh.

---

## 8. Recommendation For This Track

Use the modular monolith as the active build path for hackathon delivery.

Use the minimal microservice split as:

- a future-state architecture reference
- a decomposition guide if the team later chooses to scale out the system
- a communication artifact for showing how the recommendation platform could evolve beyond the monolith

This keeps the public contract stable while allowing the internal architecture to grow in complexity later.
