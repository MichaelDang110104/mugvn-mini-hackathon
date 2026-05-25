import dotenv from 'dotenv'
import path from 'node:path'

dotenv.config({ path: path.resolve(process.cwd(), '.env') })
import { ensureRunDirs, newRunId, writeText } from '../driver/artifacts'
import { Driver } from '../driver/Driver'
import { pickRandomEmail } from '../driver/mongoEmailPicker'
import type { ManagedProcess } from '../driver/processManager'
import { waitForPort } from '../driver/waitForPort'

function getArg(name: string): string | null {
  const idx = process.argv.indexOf(name)
  if (idx === -1) return null
  return process.argv[idx + 1] ?? null
}

function hasFlag(name: string): boolean {
  return process.argv.includes(name)
}

const shouldStart = hasFlag('--start') || !hasFlag('--attach')

async function main() {
  const repoRoot = path.resolve(process.cwd(), '..')
  const frontendUrl = getArg('--frontend-url') ?? 'http://localhost:3001'
  const backendPort = Number(getArg('--backend-port') ?? '9000')
  const frontendPort = Number(getArg('--frontend-port') ?? '3001')
  const mongoUri = process.env.MONGO_URI
  if (!mongoUri) {
    throw new Error('MONGO_URI is required to pick a random login email')
  }

  const runId = newRunId()
  const artifacts = ensureRunDirs(path.resolve(process.cwd(), 'artifacts'), runId)

  process.stdout.write(`runId=${runId}\n`)
  process.stdout.write(`artifacts=${artifacts.rootDir}\n`)
  process.stdout.write(`frontendUrl=${frontendUrl}\n`)
  process.stdout.write(`backendPort=${backendPort} frontendPort=${frontendPort}\n`)
  process.stdout.write(`mode=${shouldStart ? '--start' : '--attach'}\n`)

  let backendProc: ManagedProcess | null = null;
  let frontendProc: ManagedProcess | null = null;
  let shuttingDown = false;
  const { startBackend, startFrontend, stopProcess } = await import('../driver/processManager')
  if (shouldStart) {
    backendProc = await startBackend({ repoRoot, backendPort, frontendPort, logsDir: artifacts.logsDir, env: process.env })
    frontendProc = await startFrontend({ repoRoot, backendPort, frontendPort, logsDir: artifacts.logsDir, env: process.env })

    process.stdout.write(`backendLog=${backendProc.logPath}\n`)
    process.stdout.write(`frontendLog=${frontendProc.logPath}\n`)

    await waitForPort({ host: '127.0.0.1', port: backendPort, timeoutMs: 120000, pollMs: 500 })
    await waitForPort({ host: '127.0.0.1', port: frontendPort, timeoutMs: 120000, pollMs: 500 })
  }
  function shutdown() {
    if (shuttingDown) return;
    shuttingDown = true;
    void stopProcess(frontendProc)
    void stopProcess(backendProc)
  }
  process.on('exit', shutdown)
  process.on('SIGINT', () => { shutdown(); process.exit(); })
  process.on('SIGTERM', () => { shutdown(); process.exit(); })

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
  shutdown();
  writeText(path.join(artifacts.rootDir, 'run-summary.json'), JSON.stringify({ runId, email, artifacts }, null, 2))
  process.stdout.write(`Artifacts: ${artifacts.rootDir}\n`)
}

main().catch(err => {
  process.stderr.write(String(err) + '\n')
  process.exit(1)
})
