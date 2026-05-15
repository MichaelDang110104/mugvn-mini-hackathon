# Microservice Architecture Track

This directory describes an alternative architecture track for the project.

It does not replace the current Spring Boot modular monolith documentation. It exists as a separate architecture option for the same product and recommendation engine.

## What This Directory Contains

- `monolith-vs-microservice-options.md`
  - compares the current modular monolith with a minimal microservice split
- `high-level-microservice-architecture.md`
  - defines the proposed minimal microservice architecture on AWS and Cloudflare

## What Can Be Reused

This architecture track is meant to reuse as much of the current documentation set as possible.

Important exception:

- the current monolith baseline documents the frontend as `S3 + CloudFront` hosted
- this microservice track intentionally diverges by modeling a real Next.js runtime on EC2
- that divergence applies only to this alternative architecture track and does not replace the canonical monolith hosting baseline

The most important reusable assets are:

- `document/api-contract/`
  - the full frontend-facing API contract package should stay stable at the gateway boundary, including session bootstrap, headers, error semantics, and compatibility rules
- `document/information/mongodb-recommendation-design.md`
  - recommendation logic, serving modes, fallback rules, and MongoDB data shapes remain relevant
- `document/information/deliverable.md`
  - submission and engineering deliverables still apply
- `document/test-verification/verification-and-test-spec.md`
  - most functional verification scenarios still apply, though deployment and topology checks would need microservice-specific updates
- `document/information/technology-stack.md`
  - Spring Boot, Spring AI, MongoDB Atlas, and AWS service choices still apply, though the deployment topology changes
- `document/plan/collaborative-hybrid-recommendation-implementation-plan.md`
  - service responsibilities, hybrid recommendation sequencing, and collaborative gating logic can be reused conceptually, even if tasks are redistributed across services

## Architecture Options

### Option 1: Current Modular Monolith

Use when:

- the team wants the fastest implementation path
- the hackathon delivery risk must stay low
- recommendation quality matters more than deployment topology

Main shape:

- Next.js frontend
- Spring Boot backend
- MongoDB Atlas
- one deployed backend application

### Option 2: Minimal Microservice Split

Use when:

- the team wants a future-state architecture reference
- the backend should be split by responsibility without over-fragmenting the system
- the frontend-to-backend contract should remain stable through a gateway

Main shape:

- Next.js frontend on EC2
- API Gateway on EC2
- Catalog Service
- Recommendation Service
- Event Service
- Profile Service
- Derivation Worker
- MongoDB Atlas
- S3 for assets and build artifacts
- CloudFront and Cloudflare in front

## Recommended Reading Order

1. `monolith-vs-microservice-options.md`
2. `high-level-microservice-architecture.md`

## Important Rule

The external frontend/backend contract should be preserved at the gateway boundary unchanged whenever possible. Internal service-to-service contracts may differ behind the gateway.
