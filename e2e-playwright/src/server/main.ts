import dotenv from 'dotenv'
import path from 'node:path'

dotenv.config({ path: path.resolve(process.cwd(), '.env'), override: true })

import { ensureRunDirs, newRunId, writeText } from '../driver/artifacts'
import { Driver } from '../driver/Driver'
import type { ManagedProcess } from '../driver/processManager'
import { waitForPort } from '../driver/waitForPort'
import { startServer } from './server'

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
  const serverPort = Number(getArg('--server-port') ?? '3210')

  const runId = newRunId()
  const artifacts = ensureRunDirs(path.resolve(process.cwd(), 'artifacts'), runId)

  process.stdout.write(`runId=${runId}\n`)
  process.stdout.write(`artifacts=${artifacts.rootDir}\n`)
  process.stdout.write(`frontendUrl=${frontendUrl}\n`)
  process.stdout.write(`backendPort=${backendPort} frontendPort=${frontendPort}\n`)
  process.stdout.write(`serverUrl=http://127.0.0.1:${serverPort}\n`)
  process.stdout.write(`mode=${shouldStart ? '--start' : '--attach'}\n`)

  let backendProc: ManagedProcess | null = null
  let frontendProc: ManagedProcess | null = null
  let shuttingDown = false

  const { startBackend, startFrontend, stopProcess } = await import('../driver/processManager')

  if (shouldStart) {
    backendProc = await startBackend({ repoRoot, backendPort, frontendPort, logsDir: artifacts.logsDir, env: process.env })
    frontendProc = await startFrontend({ repoRoot, backendPort, frontendPort, logsDir: artifacts.logsDir, env: process.env })

    process.stdout.write(`backendLog=${backendProc.logPath}\n`)
    process.stdout.write(`frontendLog=${frontendProc.logPath}\n`)

    await waitForPort({ host: '127.0.0.1', port: backendPort, timeoutMs: 120000, pollMs: 500 })
    await waitForPort({ host: '127.0.0.1', port: frontendPort, timeoutMs: 120000, pollMs: 500 })
  }

  const driver = new Driver(artifacts, frontendUrl)
  await driver.start()

  async function shutdown() {
    if (shuttingDown) return
    shuttingDown = true

    await driver.stop().catch(() => {})
    await stopProcess(frontendProc)
    await stopProcess(backendProc)

    writeText(path.join(artifacts.rootDir, 'run-summary.json'), JSON.stringify({ runId, artifacts }, null, 2))
  }

  process.on('SIGINT', () => void shutdown().finally(() => process.exit(0)))
  process.on('SIGTERM', () => void shutdown().finally(() => process.exit(0)))

  await startServer({
    host: '127.0.0.1',
    port: serverPort,
    driver,
    info: {
      runId,
      artifactsDir: artifacts.rootDir,
      mode: shouldStart ? '--start' : '--attach',
      frontendUrl,
    },
    onShutdown: shutdown,
  })

  process.stdout.write('ready=true\n')
}

main().catch(err => {
  process.stderr.write(String(err) + '\n')
  process.exit(1)
})
