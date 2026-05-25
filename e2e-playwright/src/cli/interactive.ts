import dotenv from 'dotenv'
import readline from 'node:readline'
import path from 'node:path'

dotenv.config({ path: path.resolve(process.cwd(), '.env') })
import { ensureRunDirs, newRunId, writeText } from '../driver/artifacts'
import { Driver } from '../driver/Driver'
import { parseCommand } from './commands'
import { formatState } from './formatState'
import type { ManagedProcess } from '../driver/processManager'

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

  const runId = newRunId()
  const artifacts = ensureRunDirs(path.resolve(process.cwd(), 'artifacts'), runId)

  let backendProc: ManagedProcess | null = null;
  let frontendProc: ManagedProcess | null = null;
  let shuttingDown = false;
  const { startBackend, startFrontend, stopProcess } = await import('../driver/processManager')
  if (shouldStart) {
    backendProc = await startBackend({ repoRoot, backendPort, frontendPort, logsDir: artifacts.logsDir, env: process.env })
    frontendProc = await startFrontend({ repoRoot, backendPort, frontendPort, logsDir: artifacts.logsDir, env: process.env })
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
    shutdown();
    writeText(path.join(artifacts.rootDir, 'run-summary.json'), JSON.stringify({ runId, artifacts }, null, 2))
    process.stdout.write(`Artifacts: ${artifacts.rootDir}\n`)
    process.exit(0)
  })
}

main().catch(err => {
  process.stderr.write(String(err) + '\n')
  process.exit(1)
})
