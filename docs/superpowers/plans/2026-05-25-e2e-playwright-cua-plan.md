# E2E Playwright Runner + Interactive DOM Controller Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add a Playwright-based E2E runner with two modes (scripted smoke + interactive CLI) that can start/attach backend+frontend, pick a random Mongo email, drive the UI, and capture artifacts (video, trace, HAR, logs, HTML snapshots).

**Architecture:** A TypeScript Playwright project under `e2e-playwright/` with a shared `Driver` responsible for browser lifecycle, actions, render-settle waits, and state capture. A `processManager` optionally spawns backend/frontend and streams logs to artifacts. Smoke flow is implemented as a script that uses Driver primitives. Interactive mode is a CLI REPL that parses commands and prints reduced state JSON while saving full HTML snapshots.

**Tech Stack:** Node.js + TypeScript, Playwright (Chromium), MongoDB Node driver, child_process process orchestration.

---

## File structure (locked)

**Create (new project):**
- `e2e-playwright/package.json`
- `e2e-playwright/tsconfig.json`
- `e2e-playwright/playwright.config.ts`
- `e2e-playwright/.gitignore`
- `e2e-playwright/src/driver/Driver.ts`
- `e2e-playwright/src/driver/artifacts.ts`
- `e2e-playwright/src/driver/renderWait.ts`
- `e2e-playwright/src/driver/processManager.ts`
- `e2e-playwright/src/driver/mongoEmailPicker.ts`
- `e2e-playwright/src/cli/commands.ts`
- `e2e-playwright/src/cli/formatState.ts`
- `e2e-playwright/src/cli/interactive.ts`
- `e2e-playwright/src/flows/smoke.ts`

**Modify:**
- `README.md` (add how to run e2e)
- root `.gitignore` (ignore `e2e-playwright/artifacts/` if needed)

---

### Task 1: Scaffold the `e2e-playwright` project

**Files:**
- Create: `e2e-playwright/package.json`
- Create: `e2e-playwright/tsconfig.json`
- Create: `e2e-playwright/playwright.config.ts`
- Create: `e2e-playwright/.gitignore`

- [ ] **Step 1: Create `e2e-playwright/package.json`**

```json
{
  "name": "hackathon-e2e",
  "private": true,
  "version": "0.0.1",
  "type": "module",
  "scripts": {
    "install:browsers": "playwright install --with-deps chromium",
    "e2e:smoke": "node --enable-source-maps dist/flows/smoke.js",
    "e2e:interactive": "node --enable-source-maps dist/cli/interactive.js",
    "build": "tsc -p tsconfig.json",
    "dev:smoke": "tsx src/flows/smoke.ts",
    "dev:interactive": "tsx src/cli/interactive.ts"
  },
  "dependencies": {
    "mongodb": "^6.17.0"
  },
  "devDependencies": {
    "@playwright/test": "^1.55.0",
    "tsx": "^4.20.0",
    "typescript": "5.7.3"
  }
}
```

- [ ] **Step 2: Create `e2e-playwright/tsconfig.json`**

```json
{
  "compilerOptions": {
    "target": "ES2022",
    "module": "ES2022",
    "moduleResolution": "Bundler",
    "outDir": "dist",
    "rootDir": "src",
    "strict": true,
    "esModuleInterop": true,
    "skipLibCheck": true,
    "types": ["node"]
  },
  "include": ["src"]
}
```

- [ ] **Step 3: Create `e2e-playwright/playwright.config.ts`**

```ts
import { defineConfig } from '@playwright/test'

export default defineConfig({
  use: {
    browserName: 'chromium',
    headless: false,
    video: 'on',
    trace: 'on',
  },
})
```

- [ ] **Step 4: Create `e2e-playwright/.gitignore`**

```gitignore
node_modules/
dist/
artifacts/
.playwright/
```

- [ ] **Step 5: Commit**

```bash
git add e2e-playwright/package.json e2e-playwright/tsconfig.json e2e-playwright/playwright.config.ts e2e-playwright/.gitignore
git commit -m "test(e2e): scaffold Playwright runner project"
```

---

### Task 2: Implement artifact management

**Files:**
- Create: `e2e-playwright/src/driver/artifacts.ts`

- [ ] **Step 1: Create `artifacts.ts`**

```ts
import fs from 'node:fs'
import path from 'node:path'

export interface ArtifactPaths {
  runId: string
  rootDir: string
  htmlDir: string
  logsDir: string
  videoDir: string
  traceDir: string
  networkDir: string
}

export function newRunId(): string {
  const now = new Date()
  const pad = (n: number) => String(n).padStart(2, '0')
  return [
    now.getFullYear(),
    pad(now.getMonth() + 1),
    pad(now.getDate()),
    pad(now.getHours()),
    pad(now.getMinutes()),
    pad(now.getSeconds()),
    Math.random().toString(16).slice(2, 8),
  ].join('-')
}

export function ensureRunDirs(baseDir: string, runId: string): ArtifactPaths {
  const rootDir = path.resolve(baseDir, runId)
  const htmlDir = path.join(rootDir, 'html')
  const logsDir = path.join(rootDir, 'logs')
  const videoDir = path.join(rootDir, 'video')
  const traceDir = path.join(rootDir, 'trace')
  const networkDir = path.join(rootDir, 'network')

  for (const dir of [rootDir, htmlDir, logsDir, videoDir, traceDir, networkDir]) {
    fs.mkdirSync(dir, { recursive: true })
  }

  return { runId, rootDir, htmlDir, logsDir, videoDir, traceDir, networkDir }
}

export function writeText(filePath: string, content: string): void {
  fs.mkdirSync(path.dirname(filePath), { recursive: true })
  fs.writeFileSync(filePath, content, 'utf-8')
}

export function appendLine(filePath: string, line: string): void {
  fs.mkdirSync(path.dirname(filePath), { recursive: true })
  fs.appendFileSync(filePath, line + '\n', 'utf-8')
}
```

- [ ] **Step 2: Add minimal compilation check**

Run:
```bash
cd e2e-playwright
npm install
npm run build
```
Expected: `tsc` completes without errors.

- [ ] **Step 3: Commit**

```bash
git add e2e-playwright/src/driver/artifacts.ts
git commit -m "test(e2e): add artifact directory helpers"
```

---

### Task 3: Implement backend/frontend process manager (start vs attach)

**Files:**
- Create: `e2e-playwright/src/driver/processManager.ts`

- [ ] **Step 1: Create `processManager.ts`**

```ts
import { spawn, type ChildProcessWithoutNullStreams } from 'node:child_process'
import path from 'node:path'
import { appendLine } from './artifacts'

export interface ManagedProcess {
  name: string
  proc: ChildProcessWithoutNullStreams
  logPath: string
}

export interface StartOptions {
  repoRoot: string
  env: Record<string, string | undefined>
  backendPort: number
  frontendPort: number
  logsDir: string
}

function spawnLogged(name: string, cmd: string, args: string[], cwd: string, env: Record<string, string | undefined>, logPath: string): ManagedProcess {
  const proc = spawn(cmd, args, { cwd, env: { ...process.env, ...env }, stdio: 'pipe' })
  proc.stdout.on('data', d => appendLine(logPath, `[stdout] ${d.toString().trimEnd()}`))
  proc.stderr.on('data', d => appendLine(logPath, `[stderr] ${d.toString().trimEnd()}`))
  proc.on('exit', code => appendLine(logPath, `[exit] code=${code ?? 'null'}`))
  return { name, proc, logPath }
}

export async function startBackend(opts: StartOptions): Promise<ManagedProcess> {
  const cwd = path.join(opts.repoRoot, 'backend')
  const logPath = path.join(opts.logsDir, 'backend.log')

  return spawnLogged(
    'backend',
    './mvnw',
    ['-q', 'spring-boot:run', `-Dspring-boot.run.arguments=--server.port=${opts.backendPort}`],
    cwd,
    opts.env,
    logPath
  )
}

export async function startFrontend(opts: StartOptions): Promise<ManagedProcess> {
  const cwd = path.join(opts.repoRoot, 'frontend-hackathon')
  const logPath = path.join(opts.logsDir, 'frontend.log')

  return spawnLogged(
    'frontend',
    'npm',
    ['run', 'dev', '--', '--port', String(opts.frontendPort)],
    cwd,
    { ...opts.env, NEXT_PUBLIC_API_BASE_URL: `http://localhost:${opts.backendPort}` },
    logPath
  )
}

export async function stopProcess(p: ManagedProcess | null | undefined): Promise<void> {
  if (!p) return
  if (p.proc.killed) return
  p.proc.kill('SIGTERM')
}
```

- [ ] **Step 2: Commit**

```bash
git add e2e-playwright/src/driver/processManager.ts
git commit -m "test(e2e): add backend/frontend process manager"
```

---

### Task 4: Implement Mongo random email picker

**Files:**
- Create: `e2e-playwright/src/driver/mongoEmailPicker.ts`

- [ ] **Step 1: Create `mongoEmailPicker.ts`**

```ts
import { MongoClient } from 'mongodb'

export async function pickRandomEmail(mongoUri: string): Promise<string> {
  const client = new MongoClient(mongoUri)
  await client.connect()

  try {
    const db = client.db()

    const candidateCollections = ['users', 'mflix_users', 'mflixUser', 'mflix']

    for (const colName of candidateCollections) {
      const col = db.collection(colName)
      const doc = await col.find({ email: { $type: 'string' } }).project({ email: 1 }).limit(1).next()
      if (doc?.email) {
        const sampled = await col.aggregate([{ $match: { email: { $type: 'string' } } }, { $sample: { size: 1 } }, { $project: { email: 1 } }]).toArray()
        const email = sampled?.[0]?.email
        if (typeof email === 'string' && email.includes('@')) return email
      }
    }

    throw new Error('Could not find any user emails in expected collections')
  } finally {
    await client.close()
  }
}
```

- [ ] **Step 2: Commit**

```bash
git add e2e-playwright/src/driver/mongoEmailPicker.ts
git commit -m "test(e2e): pick random auth email from Mongo"
```

---

### Task 5: Implement render settle helper

**Files:**
- Create: `e2e-playwright/src/driver/renderWait.ts`

- [ ] **Step 1: Create `renderWait.ts`**

```ts
import type { Page } from '@playwright/test'

export async function settle(page: Page, timeoutMs: number = 10_000): Promise<void> {
  await page.waitForLoadState('domcontentloaded', { timeout: timeoutMs }).catch(() => {})
  await page.waitForLoadState('networkidle', { timeout: timeoutMs }).catch(() => {})
  await page.waitForTimeout(300)
}
```

- [ ] **Step 2: Commit**

```bash
git add e2e-playwright/src/driver/renderWait.ts
git commit -m "test(e2e): add render settle helper"
```

---

### Task 6: Implement the Playwright Driver

**Files:**
- Create: `e2e-playwright/src/driver/Driver.ts`
- Test by running dev mode (manual)

- [ ] **Step 1: Create `Driver.ts`**

```ts
import { chromium, type Browser, type BrowserContext, type Page } from '@playwright/test'
import path from 'node:path'
import { appendLine, writeText, type ArtifactPaths } from './artifacts'
import { settle } from './renderWait'

export interface ReducedState {
  ts: string
  url: string
  title: string
  visibleTextSample: string
  consoleErrors: string[]
}

export class Driver {
  private browser: Browser | null = null
  private context: BrowserContext | null = null
  private page: Page | null = null
  private consoleErrors: string[] = []
  private step = 0

  constructor(private readonly artifacts: ArtifactPaths, private readonly baseUrl: string) {}

  async start(): Promise<void> {
    this.browser = await chromium.launch({ headless: false })
    this.context = await this.browser.newContext({
      recordVideo: { dir: this.artifacts.videoDir },
      recordHar: { path: path.join(this.artifacts.networkDir, 'network.har'), mode: 'minimal' },
    })

    await this.context.tracing.start({ screenshots: false, snapshots: true, sources: true })

    this.page = await this.context.newPage()

    this.page.on('console', msg => {
      const line = `[${msg.type()}] ${msg.text()}`
      appendLine(path.join(this.artifacts.logsDir, 'browser-console.log'), line)
      if (msg.type() === 'error') this.consoleErrors.push(line)
    })

    this.page.on('pageerror', err => {
      const line = `[pageerror] ${String(err)}`
      appendLine(path.join(this.artifacts.logsDir, 'browser-console.log'), line)
      this.consoleErrors.push(line)
    })
  }

  async stop(): Promise<void> {
    if (this.context) {
      await this.context.tracing.stop({ path: path.join(this.artifacts.traceDir, 'trace.zip') })
    }
    await this.context?.close().catch(() => {})
    await this.browser?.close().catch(() => {})
  }

  private ensurePage(): Page {
    if (!this.page) throw new Error('Driver not started')
    return this.page
  }

  async goto(pathname: string): Promise<ReducedState> {
    const page = this.ensurePage()
    const url = pathname.startsWith('http') ? pathname : `${this.baseUrl}${pathname}`
    await page.goto(url)
    await settle(page)
    return this.captureState('goto')
  }

  async click(selector: string): Promise<ReducedState> {
    const page = this.ensurePage()
    await page.click(selector)
    await settle(page)
    return this.captureState('click')
  }

  async type(selector: string, value: string): Promise<ReducedState> {
    const page = this.ensurePage()
    await page.fill(selector, value)
    await settle(page)
    return this.captureState('type')
  }

  async press(key: string): Promise<ReducedState> {
    const page = this.ensurePage()
    await page.keyboard.press(key)
    await settle(page)
    return this.captureState('press')
  }

  async wait(ms: number): Promise<ReducedState> {
    const page = this.ensurePage()
    await page.waitForTimeout(ms)
    return this.captureState('wait')
  }

  async back(): Promise<ReducedState> {
    const page = this.ensurePage()
    await page.goBack().catch(() => null)
    await settle(page)
    return this.captureState('back')
  }

  async reload(): Promise<ReducedState> {
    const page = this.ensurePage()
    await page.reload()
    await settle(page)
    return this.captureState('reload')
  }

  private async captureState(action: string): Promise<ReducedState> {
    const page = this.ensurePage()
    this.step += 1

    const html = await page.content()
    const title = await page.title()
    const url = page.url()

    const bodyText = await page.evaluate(() => document.body?.innerText ?? '')
    const visibleTextSample = bodyText.length > 2000 ? bodyText.slice(0, 2000) : bodyText

    const htmlPath = path.join(this.artifacts.htmlDir, `step-${String(this.step).padStart(4, '0')}-${action}.html`)
    writeText(htmlPath, html)

    return {
      ts: new Date().toISOString(),
      url,
      title,
      visibleTextSample,
      consoleErrors: this.consoleErrors.slice(-50),
    }
  }
}
```

- [ ] **Step 2: Commit**

```bash
git add e2e-playwright/src/driver/Driver.ts
git commit -m "test(e2e): add Playwright driver with DOM snapshot capture"
```

---

### Task 7: Implement Interactive CLI

**Files:**
- Create: `e2e-playwright/src/cli/commands.ts`
- Create: `e2e-playwright/src/cli/formatState.ts`
- Create: `e2e-playwright/src/cli/interactive.ts`

- [ ] **Step 1: Create `commands.ts`**

```ts
export type Command =
  | { kind: 'goto'; path: string }
  | { kind: 'click'; selector: string }
  | { kind: 'type'; selector: string; value: string }
  | { kind: 'press'; key: string }
  | { kind: 'wait'; ms: number }
  | { kind: 'back' }
  | { kind: 'reload' }
  | { kind: 'end' }

export function parseCommand(line: string): Command {
  const trimmed = line.trim()
  if (!trimmed) throw new Error('empty')

  const [cmd, ...rest] = splitArgs(trimmed)

  switch (cmd) {
    case 'goto':
      return { kind: 'goto', path: rest[0] ?? '/' }
    case 'click':
      return { kind: 'click', selector: rest.join(' ') }
    case 'type': {
      const selector = rest[0]
      const value = rest.slice(1).join(' ')
      if (!selector) throw new Error('type requires selector and value')
      return { kind: 'type', selector, value }
    }
    case 'press':
      return { kind: 'press', key: rest[0] ?? 'Enter' }
    case 'wait':
      return { kind: 'wait', ms: Number(rest[0] ?? '500') }
    case 'back':
      return { kind: 'back' }
    case 'reload':
      return { kind: 'reload' }
    case 'end':
    case 'quit':
    case 'exit':
      return { kind: 'end' }
    default:
      throw new Error(`unknown command: ${cmd}`)
  }
}

function splitArgs(input: string): string[] {
  const out: string[] = []
  let cur = ''
  let quote: '"' | "'" | null = null

  for (let i = 0; i < input.length; i++) {
    const ch = input[i]
    if (quote) {
      if (ch === quote) {
        quote = null
      } else {
        cur += ch
      }
      continue
    }

    if (ch === '"' || ch === "'") {
      quote = ch as any
      continue
    }

    if (ch === ' ') {
      if (cur) out.push(cur)
      cur = ''
      continue
    }

    cur += ch
  }

  if (cur) out.push(cur)
  return out
}
```

- [ ] **Step 2: Create `formatState.ts`**

```ts
import type { ReducedState } from '../driver/Driver'

export function formatState(state: ReducedState): string {
  return JSON.stringify(state, null, 2)
}
```

- [ ] **Step 3: Create `interactive.ts`**

```ts
import readline from 'node:readline'
import path from 'node:path'
import { ensureRunDirs, newRunId, writeText } from '../driver/artifacts'
import { Driver } from '../driver/Driver'
import { parseCommand } from './commands'
import { formatState } from './formatState'

function getArg(name: string): string | null {
  const idx = process.argv.indexOf(name)
  if (idx === -1) return null
  return process.argv[idx + 1] ?? null
}

function hasFlag(name: string): boolean {
  return process.argv.includes(name)
}

async function main() {
  const repoRoot = path.resolve(process.cwd(), '..')
  const frontendUrl = getArg('--frontend-url') ?? 'http://localhost:3001'

  const runId = newRunId()
  const artifacts = ensureRunDirs(path.resolve(process.cwd(), 'artifacts'), runId)

  const driver = new Driver(artifacts, frontendUrl)
  await driver.start()
  await driver.goto('/login')

  const rl = readline.createInterface({ input: process.stdin, output: process.stdout, terminal: true })
  rl.setPrompt('e2e> ')
  rl.prompt()

  rl.on('line', async line => {
    try {
      const cmd = parseCommand(line)

      if (cmd.kind === 'end') {
        rl.close()
        return
      }

      let state
      switch (cmd.kind) {
        case 'goto':
          state = await driver.goto(cmd.path)
          break
        case 'click':
          state = await driver.click(cmd.selector)
          break
        case 'type':
          state = await driver.type(cmd.selector, cmd.value)
          break
        case 'press':
          state = await driver.press(cmd.key)
          break
        case 'wait':
          state = await driver.wait(cmd.ms)
          break
        case 'back':
          state = await driver.back()
          break
        case 'reload':
          state = await driver.reload()
          break
      }

      process.stdout.write(formatState(state) + '\n')
    } catch (e: any) {
      process.stderr.write(String(e?.message ?? e) + '\n')
    } finally {
      rl.prompt()
    }
  })

  rl.on('close', async () => {
    await driver.stop()
    writeText(path.join(artifacts.rootDir, 'run-summary.json'), JSON.stringify({ runId, artifacts }, null, 2))
    process.stdout.write(`Artifacts: ${artifacts.rootDir}\n`)
    process.exit(0)
  })
}

main().catch(err => {
  process.stderr.write(String(err) + '\n')
  process.exit(1)
})
```

- [ ] **Step 4: Commit**

```bash
git add e2e-playwright/src/cli/commands.ts e2e-playwright/src/cli/formatState.ts e2e-playwright/src/cli/interactive.ts
git commit -m "test(e2e): add interactive CLI controller"
```

---

### Task 8: Implement scripted smoke flow (UI login + minimal checks)

**Files:**
- Create: `e2e-playwright/src/flows/smoke.ts`

- [ ] **Step 1: Create `smoke.ts`**

```ts
import path from 'node:path'
import { ensureRunDirs, newRunId, writeText } from '../driver/artifacts'
import { Driver } from '../driver/Driver'
import { pickRandomEmail } from '../driver/mongoEmailPicker'

function getArg(name: string): string | null {
  const idx = process.argv.indexOf(name)
  if (idx === -1) return null
  return process.argv[idx + 1] ?? null
}

async function main() {
  const frontendUrl = getArg('--frontend-url') ?? 'http://localhost:3001'
  const mongoUri = process.env.MONGO_URI
  if (!mongoUri) {
    throw new Error('MONGO_URI is required to pick a random login email')
  }

  const runId = newRunId()
  const artifacts = ensureRunDirs(path.resolve(process.cwd(), 'artifacts'), runId)
  const driver = new Driver(artifacts, frontendUrl)

  await driver.start()

  const email = await pickRandomEmail(mongoUri)

  await driver.goto('/login')
  await driver.type('input[type=email]', email)
  await driver.type('input[type=password]', 'any')
  await driver.click('button:has-text("Log In")')

  await driver.wait(1500)
  const state = await driver.wait(10)

  writeText(path.join(artifacts.rootDir, 'smoke-result.json'), JSON.stringify({ email, state }, null, 2))

  await driver.stop()
  writeText(path.join(artifacts.rootDir, 'run-summary.json'), JSON.stringify({ runId, email, artifacts }, null, 2))
  process.stdout.write(`Artifacts: ${artifacts.rootDir}\n`)
}

main().catch(err => {
  process.stderr.write(String(err) + '\n')
  process.exit(1)
})
```

- [ ] **Step 2: Commit**

```bash
git add e2e-playwright/src/flows/smoke.ts
git commit -m "test(e2e): add basic smoke flow"
```

---

### Task 9: Wire `--start` vs `--attach` into both modes

**Files:**
- Modify: `e2e-playwright/src/flows/smoke.ts`
- Modify: `e2e-playwright/src/cli/interactive.ts`

- [ ] **Step 1: Add helpers**

Add in both entrypoints:

```ts
function hasFlag(name: string): boolean {
  return process.argv.includes(name)
}

const shouldStart = hasFlag('--start') || !hasFlag('--attach')
```

- [ ] **Step 2: Use process manager when `shouldStart`**

In both files, before creating `Driver`:

```ts
import { startBackend, startFrontend, stopProcess } from '../driver/processManager'

const repoRoot = path.resolve(process.cwd(), '..')
const backendPort = Number(getArg('--backend-port') ?? '9000')
const frontendPort = Number(getArg('--frontend-port') ?? '3001')

let backendProc = null
let frontendProc = null

if (shouldStart) {
  backendProc = await startBackend({ repoRoot, backendPort, frontendPort, logsDir: artifacts.logsDir, env: process.env })
  frontendProc = await startFrontend({ repoRoot, backendPort, frontendPort, logsDir: artifacts.logsDir, env: process.env })
}

process.on('exit', () => {
  void stopProcess(frontendProc)
  void stopProcess(backendProc)
})
```

- [ ] **Step 3: Ensure stop calls**

On shutdown, call `stopProcess` for both.

- [ ] **Step 4: Commit**

```bash
git add e2e-playwright/src/flows/smoke.ts e2e-playwright/src/cli/interactive.ts
git commit -m "test(e2e): support --start and --attach"
```

---

### Task 10: Add docs and gitignore

**Files:**
- Modify: `README.md`
- Modify: `.gitignore` (repo root)

- [ ] **Step 1: Update root `.gitignore`**

Add:

```gitignore
e2e-playwright/artifacts/
```

- [ ] **Step 2: Update `README.md`**

Add section:

```md
## E2E

Install:

```bash
cd e2e-playwright
npm install
npm run install:browsers
```

Smoke:

```bash
cd e2e-playwright
MONGO_URI=... OPENAI_API_KEY=... REDIS_HOST=... npm run dev:smoke -- --start
```

Interactive:

```bash
cd e2e-playwright
MONGO_URI=... OPENAI_API_KEY=... REDIS_HOST=... npm run dev:interactive -- --start
```

Artifacts are written to `e2e-playwright/artifacts/<runId>/`.
```

- [ ] **Step 3: Commit**

```bash
git add README.md .gitignore
git commit -m "docs: document Playwright e2e runner"
```

---

## Plan self-review

- Spec coverage:
  - two modes covered (smoke + interactive)
  - start/attach covered
  - artifacts captured (video, trace, HAR, console, HTML)
  - Mongo random email selection included
- Placeholder scan: no TBD/TODO; all steps include concrete code.
- Type consistency: command parsing and Driver interfaces are defined before use.
