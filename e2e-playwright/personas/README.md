# Persona Scripts (5)

These are ready-to-use persona briefs + command scripts for driving the app via the interactive HTTP control server.

Model: each persona runs in its own chat and talks to its own server port.

## Start Commands (recommended)

Start one backend and then start one frontend + one control server per persona. Each persona gets:
- its own frontend port
- its own control server port
- its own Playwright session + artifacts

If you already have backend running on 9000, use `--attach` for servers.

### Persona 1

```bash
cd e2e-playwright
npm run dev:server -- --start --backend-port 9000 --frontend-port 3001 --frontend-count 1 --frontend-url http://localhost:3001 --server-port 3210
```

### Persona 2

```bash
cd e2e-playwright
npm run dev:server -- --start --backend-port 9000 --frontend-port 3002 --frontend-count 1 --frontend-url http://localhost:3002 --server-port 3211
```

### Persona 3

```bash
cd e2e-playwright
npm run dev:server -- --start --backend-port 9000 --frontend-port 3003 --frontend-count 1 --frontend-url http://localhost:3003 --server-port 3212
```

### Persona 4

```bash
cd e2e-playwright
npm run dev:server -- --start --backend-port 9000 --frontend-port 3004 --frontend-count 1 --frontend-url http://localhost:3004 --server-port 3213
```

### Persona 5

```bash
cd e2e-playwright
npm run dev:server -- --start --backend-port 9000 --frontend-port 3005 --frontend-count 1 --frontend-url http://localhost:3005 --server-port 3214
```

## How To Use In A New Chat

1) Start the persona server.
2) In the persona chat, set the persona's base URL (server port) and have it drive the session using:

```bash
curl -s http://127.0.0.1:<serverPort>/health
curl -s -XPOST http://127.0.0.1:<serverPort>/cmd -H 'content-type: application/json' -d '{"cmd":"goto /login"}'
```

3) Each persona script below includes:
- a role/personality brief (for the agent prompt)
- an exploration checklist
- a command sequence template (as `/cmd` values)

## Files

- `persona-01-new-user.md`
- `persona-02-power-browser.md`
- `persona-03-search-first.md`
- `persona-04-picky-onboarding.md`
- `persona-05-breaker.md`
