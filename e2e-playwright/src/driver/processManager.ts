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
