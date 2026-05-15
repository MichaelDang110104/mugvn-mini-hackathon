# Technology Stack

## 1. Purpose

This document defines the preferred technology stack for the project with an explicit goal of keeping dependencies minimal while still satisfying the hackathon requirement that MongoDB is central to the recommendation engine.

This stack is aligned to the Spring Boot ecosystem preference and should be treated as the canonical implementation baseline unless the team intentionally revises the docs together.

---

## 2. Technology Selection Principles

- prefer minimal dependencies
- prefer technologies that directly support the recommendation engine
- avoid adding libraries that duplicate what Spring Boot or MongoDB already provide
- prefer managed AWS services over self-hosted infrastructure where possible
- optimize for hackathon delivery speed and debuggability

---

## 3. Required Technologies

### 3.1 Java

Use:

- Java 21

Why:

- modern LTS baseline
- strong Spring Boot compatibility
- good performance and runtime stability

### 3.2 Spring Boot

Use:

- Spring Boot `3.5.x`

Why:

- canonical backend framework for the project
- minimal setup overhead
- strong integration with REST APIs, configuration, logging, and dependency management

Recommended modules only:

- `spring-boot-starter-web`
- `spring-boot-starter-validation`
- `spring-boot-starter-actuator`

### 3.3 Spring Data MongoDB

Use:

- `spring-boot-starter-data-mongodb`

Why:

- natural integration with MongoDB Atlas
- keeps repository and document access simple
- avoids introducing extra ORM or query abstraction layers

### 3.4 Spring AI

Use:

- Spring AI

Why:

- provides structured integration for embedding generation and AI provider access
- fits the Spring ecosystem requirement
- avoids building ad hoc provider glue code

Compatibility note:

- keep Spring AI aligned with a Spring Boot `3.5.x` baseline for the MVP

Spring AI should be used for:

- query embedding generation
- optional offline embedding jobs if needed through a simple integration layer

### 3.5 MongoDB Atlas

Use:

- MongoDB Atlas

Why:

- mandatory for the hackathon’s technical direction
- central store for catalog, events, profiles, and recommendation logs
- supports MongoDB Vector Search and Aggregation Pipeline in the same platform

Required MongoDB capabilities:

- document collections
- indexes
- Vector Search
- Aggregation Pipeline

---

## 4. Frontend Technologies

### 4.1 Next.js

Use:

- Next.js
- TypeScript

Why:

- fast to build a demoable interface
- easy deployment to static or server-backed frontend hosting patterns

### 4.2 Frontend Styling

Preferred approach:

- keep styling lightweight and avoid large UI frameworks unless clearly needed

Why:

- the hackathon is about recommendation quality, not UI framework complexity

---

## 5. AWS Technologies

### 5.1 AWS App Runner

Use:

- AWS App Runner for the Spring Boot backend

Why:

- managed deployment for a containerized backend
- simpler than ECS/EKS for a hackathon team

### 5.2 Amazon S3

Use:

- frontend build hosting or static assets
- dataset or demo artifact storage if needed

### 5.3 Amazon CloudFront

Use:

- frontend delivery in front of S3 if needed

### 5.4 AWS Secrets Manager

Use:

- MongoDB connection secret
- Spring AI provider secrets
- any backend runtime secrets

### 5.5 Amazon CloudWatch

Use:

- backend logs
- deployment troubleshooting
- minimal operational visibility

---

## 6. Minimal Dependency Backend Recommendation

The backend should stay close to this minimum set:

- `spring-boot-starter-web`
- `spring-boot-starter-validation`
- `spring-boot-starter-actuator`
- `spring-boot-starter-test`
- `spring-boot-starter-data-mongodb`
- `spring-ai-*` provider starter that matches the chosen embedding provider

Avoid by default:

- heavy workflow engines
- extra API gateway libraries
- multiple persistence abstractions
- large mapping frameworks unless clearly needed
- multiple AI provider SDKs at once

---

## 7. Recommendation Engine Technology Mapping

### Semantic Search

Technologies:

- Spring AI for query embeddings
- MongoDB Atlas Vector Search for semantic retrieval

### User Behavior Tracking

Technologies:

- Spring Boot REST API
- Spring Data MongoDB
- MongoDB `user_events` collection

### Personalization

Technologies:

- MongoDB Aggregation Pipeline
- Spring Boot recommendation service

### Explainability

Technologies:

- Spring Boot service logic
- MongoDB recommendation logs and reason-code-aware ranking flow

### Fallback Behavior

Technologies:

- MongoDB text or catalog-based fallback queries
- Spring Boot decision layer for serving mode selection

---

## 8. Technologies To Avoid Unless Necessary

- Spring Cloud unless a real distributed systems need appears
- Kafka unless event scale forces it
- Redis unless performance testing proves it is required
- Elasticsearch or OpenSearch because MongoDB should remain central
- LangChain-style multi-abstraction stacks if Spring AI already covers the needed embedding flow
- separate feature stores or model-serving platforms for MVP

---

## 9. Final Recommended Stack

### Backend

- Java 21
- Spring Boot
- Spring Web
- Spring Validation
- Spring Data MongoDB
- Spring AI

### Database And Recommendation Platform

- MongoDB Atlas
- MongoDB Vector Search
- MongoDB Aggregation Pipeline

### Frontend

- Next.js
- TypeScript

### Cloud

- AWS App Runner
- Amazon S3
- Amazon CloudFront
- AWS Secrets Manager
- Amazon CloudWatch

---

## 10. Final Recommendation

The minimal-dependency architecture should be:

- Spring Boot for the backend
- Spring AI for embedding integration
- MongoDB Atlas for all recommendation-critical data and retrieval
- Next.js for the demo frontend
- AWS managed services for deployment and operations

That gives the team a stack that is small, explainable, aligned with the original Spring ecosystem direction, and strong for a MongoDB-centered recommendation engine hackathon project.
