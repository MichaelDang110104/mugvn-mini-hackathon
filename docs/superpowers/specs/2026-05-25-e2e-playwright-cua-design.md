# E2E Playwright Runner + Interactive DOM Controller (CUA-lite)

Date: 2026-05-25

## Goal

Provide a repeatable way to validate the frontend/backed integration by:

- running a deterministic smoke journey (login → onboarding if needed → home → movie detail actions → search)
- optionally running an interactive session where an operator can issue commands live
- capturing artifacts (video, trace, network, HTML snapshots, logs)

The interactive mode is “CUA-lite”: it returns the live rendered DOM (HTML) and an agent-friendly reduced view after each action rather than relying on screenshots.

## Non-goals

- No full autonomous vision-based web agent.
- No changes required to production app flows.
- No new backend endpoints for testing-only (we read Mongo directly from the runner to find an existing email).

## Repository Layout

Add a new root-level folder:

```
/e2e-playwright
  package.json
  tsconfig.json
  playwright.config.ts
  src/
    driver/
      Driver.ts
      artifacts.ts
      renderWait.ts
      mongoEmailPicker.ts
      processManager.ts
    cli/
      interactive.ts
      commands.ts
      formatState.ts
    flows/
      smoke.ts
  artifacts/
```

Artifacts should be gitignored.

## Configuration

Runner reads environment variables (same variables used by backend/frontend):

- `MONGO_URI` (required when using `--start` or when selecting a random email)
- `OPENAI_API_KEY` (backend embedding/chat)
- `REDIS_HOST` (backend cache)
- `NEXT_PUBLIC_API_BASE_URL` (frontend API base)

Default ports:

- backend: 9000
- frontend: 3001

CLI flags override:

- `--backend-url http://localhost:9000`
- `--frontend-url http://localhost:3000`
- `--start` (start backend+frontend)
- `--attach` (assume backend+frontend already running)

## Modes

### Mode 1: Smoke (scripted)

Command:

- `pnpm e2e:smoke --start`
- `pnpm e2e:smoke --attach`

Steps:

1. Pick a random existing user email from MongoDB (collection inferred from `MflixUser` mapping; fallback probing supported).
2. Navigate to `/login` and authenticate via UI.
3. If redirected to `/onboarding`, complete it with minimal valid choices.
4. Navigate to `/home`, scroll and click at least one movie card.
5. On movie detail page, click Like/Save/Rate to trigger event ingestion.
6. Navigate to search page and validate results render.

Assertions are lightweight: the goal is detecting runtime errors and broken integration, not strict UI correctness.

### Mode 2: Interactive CLI Controller

Command:

- `pnpm e2e:interactive --start`
- `pnpm e2e:interactive --attach`

Loop:

- present prompt `e2e>`
- accept commands (examples)
  - `goto /login`
  - `type css=input[type=email] value=<email>`
  - `click text="Log In"`
  - `press Enter`
  - `wait 1000`
  - `back`, `forward`, `reload`
  - `screenshot` (optional, HTML-first remains primary)
  - `end`

After every command:

- wait for settle (see next section)
- emit reduced JSON state to stdout
- write full HTML snapshot to artifacts

## Render-settle Strategy

After actions that may change UI:

- if navigation occurred: wait `domcontentloaded`, then `networkidle` with timeout
- always apply a short debounce (250–500ms)
- optionally support explicit waits:
  - wait for selector visible
  - wait for URL contains

The goal is stable enough output for an agent/operator; it does not attempt perfect idleness for pages with background polling.

## State Capture

### Reduced state (printed)

- timestamp
- url
- title
- load state
- console warnings/errors (bounded)
- page errors (bounded)
- recent network summary (bounded)
- visible text sample (first N characters)
- detected interactive elements summary (links/buttons/inputs)

### Full state (saved)

- `page.content()` written to `artifacts/<runId>/html/step-XXXX.html`
- optional `body.innerText` written to `step-XXXX.txt`

## Artifact Capture

Per run directory `e2e-playwright/artifacts/<runId>/`:

- `video.webm`
- `trace.zip`
- `network.har`
- `browser-console.log`
- `backend.log`, `frontend.log` (when `--start`)
- `html/step-XXXX.html`
- `run-summary.json`

## Process Management

When `--start`:

- spawn backend (`./backend/mvnw spring-boot:run`) with env
- spawn frontend (`pnpm dev`) with env
- wait for readiness by polling URLs:
  - frontend `/login`
  - backend `/api/onboarding/options`

When `--attach`:

- do not spawn processes
- only validate endpoints are reachable.

## Success Criteria

- A single command runs a full smoke journey and produces a runnable artifact bundle.
- Interactive mode allows manual/agent-driven exploration and returns DOM snapshots after each action.
- Any frontend console errors, page errors, backend failures, or non-2xx API responses are captured in artifacts.
