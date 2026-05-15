# Event And Error Contracts

## 1. Purpose

This document defines event request contracts, persisted event rules, validation failures, and degraded-mode response behavior.

---

## 2. Event Contract

### 2.1 Event Request Shape

```json
{
  "sessionId": "session_123",
  "eventId": "evt_123",
  "eventType": "search",
  "movieId": null,
  "queryText": "mind bending emotional sci-fi",
  "eventValue": null,
  "metadata": {
    "source": "search_page"
  },
  "timestamp": "2026-05-15T10:30:00Z"
}
```

### 2.2 Validation Rules

- `sessionId` is required
- `eventId` is required
- `eventType` is required
- `search` requires `queryText`
- `like`, `save`, `view`, `click`, and `rate` require `movieId`
- `rate` must use a defined rating scale
- if client `timestamp` is present, it must be a valid parseable timestamp
- if `timestamp` is omitted, the backend must generate one before persistence

### 2.3 Event Persistence Rules

- persisted event must always contain `timestamp`
- `eventId` must be idempotent
- malformed events must be rejected and not persisted
- noisy `view` and `search` events may be coalesced or rate-limited according to canonical rules
- canonical coalescing rule for `view` and `search`: identical `sessionId`, `eventType`, `movieId`, `queryText`, and normalized idempotency-relevant metadata within a 30-second dedupe window must be treated as the same logical event
- duplicate `eventId` submissions must return an idempotent success or safe no-op outcome, never double-persist the event
- duplicate comparison must ignore server-assigned `timestamp`
- duplicate `eventId` with a different payload must be rejected and must not overwrite the original event

---

## 3. Error Contract

### 3.1 Validation Error Response

```json
{
  "error": {
    "code": "VALIDATION_ERROR",
    "message": "queryText is required for search events",
    "details": [
      {
        "field": "queryText",
        "reason": "required"
      }
    ]
  }
}
```

### 3.2 Not Found Error Response

```json
{
  "error": {
    "code": "NOT_FOUND",
    "message": "movie not found"
  }
}
```

### 3.3 Internal Error Response

```json
{
  "error": {
    "code": "INTERNAL_ERROR",
    "message": "unable to generate recommendations"
  }
}
```

This error envelope is for true validation, not-found, and unexpected internal failures only. It must not be used for documented degraded search or recommendation fallback paths.

---

## 4. Degraded-Mode Response Rules

### 4.1 Search Degraded Mode

- still return `200`
- still return top-level `items`
- for non-empty degraded semantic search, set `mode` to `fallback_text`
- set `fallbackUsed` to `true`
- for empty-query discovery, use `cold_start` rather than `fallback_text`

### 4.2 Recommendation Degraded Mode

- still return `200`
- still return top-level `items`
- expose fallback mode truthfully
- set `fallbackUsed` to `true`
- keep `generatedAt` present on degraded recommendation responses
- do not emit collaborative reasons unless collaborative signals were actually used
- if personalization or one candidate source degrades, the chosen top-level `mode` must still reflect the actual serving path used for the final items

### 4.3 Collaborative Optionality Rule

- collaborative support may be absent without breaking external contracts
- when absent, recommendation responses must still be valid under `personalized`, `cold_start`, or `fallback_text`

---

## 5. Feasibility Summary

These event and error contracts are feasible because they align with the existing verification spec, Spring Boot validation model, and MongoDB-backed event persistence rules.
