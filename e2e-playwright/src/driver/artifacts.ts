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
