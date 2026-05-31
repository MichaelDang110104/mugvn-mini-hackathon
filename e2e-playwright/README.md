# Playwright E2E Runner (Hackathon)

This directory contains a Playwright-based E2E harness that can:

- run a scripted smoke journey
- run an interactive session
- run an interactive session over a localhost HTTP control plane (so another process/agent can drive it)

Artifacts are always written to `e2e-playwright/artifacts/<runId>/`.

## What It Does

The runner wraps a single Playwright browser session with:

- video recording
- trace capture (`trace.zip`)
- network HAR (`network.har`)
- browser console logs
- HTML DOM snapshots per step (`page.content()`)

It also supports:

- `--start`: spawn backend + frontend(s)
- `--attach`: attach to already-running backend/frontend

Defaults:

- backend port: `9000`
- frontend port: `3001`

## Setup

From this directory:

```bash
npm install
```

Install browsers (recommended), but if `--with-deps` fails on your machine you can try `npx playwright install chromium`:

```bash
npm run install:browsers
```

### Environment variables

The runner reads environment variables from `e2e-playwright/.env` (loaded by the entrypoints).

Example `e2e-playwright/.env`:

```env
MONGO_URI=mongodb+srv://.../sample_mflix?appName=MongoDB-Hackathon
OPENAI_API_KEY=...
REDIS_HOST=...
```

Notes:
- `MONGO_URI` must point at a MongoDB containing existing users (collection `users`) so we can pick a random email for login.
- Backend auth accepts email-only; password is ignored by backend for this hackathon.

## Artifacts Layout

Each run produces:

- `artifacts/<runId>/video/` (Playwright `.webm`)
- `artifacts/<runId>/trace/trace.zip`
- `artifacts/<runId>/network/network.har`
- `artifacts/<runId>/logs/backend.log`
- `artifacts/<runId>/logs/frontend-<port>.log` (one per frontend when multiple are started)
- `artifacts/<runId>/logs/browser-console.log`
- `artifacts/<runId>/html/step-XXXX-<action>.html`
- `artifacts/<runId>/run-summary.json`

The reduced state printed/returned by the driver includes `lastHtmlPath` so you can jump straight to the exact DOM snapshot for the last step.

## Mode 1: Scripted Smoke

Smoke logs in (random Mongo email) and proceeds through a minimal onboarding flow, then lands on `/home`.

```bash
npm run dev:smoke -- --start --backend-port 9000 --frontend-port 3001
```

Attach mode:

```bash
npm run dev:smoke -- --attach --frontend-url http://localhost:3001
```

Common flags:

- `--backend-port 9000`
- `--frontend-port 3001`
- `--frontend-url http://localhost:3001`
- `--frontend-count 1` (only used in `--start` mode; spawns `frontendPort + i`)

## Mode 2: Interactive REPL

This provides a local terminal prompt (`e2e>`) to issue commands.

```bash
npm run dev:interactive -- --start --backend-port 9000 --frontend-port 3001
```

Supported commands:

- `goto /login`
- `click <selector>`
- `type <selector> <value>`
- `press <key>`
- `wait <ms>`
- `back`
- `reload`
- `end`

Selector tips:
- Prefer Playwright text selectors that survive quoting: `text=Continue to recommendations`, `text=Action`, etc.

## Mode 3: Interactive HTTP Control Server (Best For Multi-Agent/Persona Driving)

This is the key mode for letting an external process (or multiple separate chat agents) explore the app in real time.

Start the server:

```bash
npm run dev:server -- --start --backend-port 9000 --frontend-port 3001 --frontend-count 1 --frontend-url http://localhost:3001 --server-port 3210
```

Attach mode:

```bash
npm run dev:server -- --attach --frontend-url http://localhost:3001 --server-port 3210
```

### HTTP API

- `GET /health`

Returns:

```json
{ "ok": true, "runId": "...", "artifactsDir": "...", "mode": "--start", "frontendUrl": "http://localhost:3001" }
```

- `POST /cmd`

Body:

```json
{ "cmd": "goto /login" }
```

Response:

```json
{ "ok": true, "state": { "url": "...", "visibleTextSample": "...", "consoleErrors": [], "lastHtmlPath": "..." } }
```

- `POST /shutdown`

Gracefully closes Playwright (ensures video/trace/HAR flush), stops spawned processes, writes `run-summary.json`, then exits.

### Example driving commands

```bash
curl -s http://127.0.0.1:3210/health
curl -s -XPOST http://127.0.0.1:3210/cmd -H 'content-type: application/json' -d '{"cmd":"goto /login"}'
curl -s -XPOST http://127.0.0.1:3210/cmd -H 'content-type: application/json' -d '{"cmd":"click text=Continue to recommendations"}'
curl -s -XPOST http://127.0.0.1:3210/shutdown -H 'content-type: application/json' -d '{}'
```

## Spawning Many Personas (Multi-Agent Exploration)

The HTTP server is designed so a separate agent can drive it using HTTP requests.

There are two recommended ways to use this for multiple personas:

### A) One persona per server (recommended)

- Start backend once (shared).
- Start one frontend per persona (different port per persona).
- Start one server per persona (different `--server-port`) pointing at that persona's frontend URL.

Example with 3 personas:

Persona 1:

```bash
npm run dev:server -- --start --backend-port 9000 --frontend-port 3001 --frontend-count 1 --frontend-url http://localhost:3001 --server-port 3210
```

Persona 2 (attach to same backend, new frontend port):

```bash
npm run dev:server -- --start --backend-port 9000 --frontend-port 3002 --frontend-count 1 --frontend-url http://localhost:3002 --server-port 3211
```

Persona 3:

```bash
npm run dev:server -- --start --backend-port 9000 --frontend-port 3003 --frontend-count 1 --frontend-url http://localhost:3003 --server-port 3212
```

Now you can have separate chat agents each talk to a different server port:

- Persona 1 agent uses `http://127.0.0.1:3210`
- Persona 2 agent uses `http://127.0.0.1:3211`
- Persona 3 agent uses `http://127.0.0.1:3212`

Each persona gets:
- a separate browser context (video/trace/network isolated)
- separate artifacts directory (separate runId)

### B) One server, many frontends (not very useful)

`--frontend-count N` can start multiple frontend processes, but the server controls only one Playwright session and one `--frontend-url` at a time.

If you want multiple independent exploratory sessions, prefer approach (A).

## Practical Persona Playbooks

Use these as “character scripts” for agents. Each bullet is a `POST /cmd` call.

### Persona: New user onboarding

- `goto /login`
- `type "input[type=email]" "<existing-user-email>"`
- `type "input[type=password]" "any"`
- `click button`
- `wait 2000`
- Select 3 genres: `click text=Action`, `click text=Adventure`, `click text=Animation`
- Select 3 moods: `click text=emotional`, `click text=mind-bending`, `click text=dark`
- Select 1+ movies: `click text=Add` (repeat)
- `click text=Continue to recommendations`
- `wait 2000`

### Persona: Home page browsing

- `goto /home`
- `wait 1000`
- Click into a movie card (selectors vary by UI; start with `click text=<movie title>`)

If a selector is flaky, inspect the last snapshot:
- read `state.lastHtmlPath`
- search that HTML for stable attributes/text

## Tips For Debugging

- Backend/frontend startup logs are in `artifacts/<runId>/logs/`.
- If the frontend port is already in use, the frontend process will exit; check `frontend-<port>.log`.
- If Playwright gets stuck, use `/shutdown` to flush artifacts.

## Security Notes

- Do not commit `.env` files.
- Do not put API keys into `backend/src/main/resources/application.yaml`.
