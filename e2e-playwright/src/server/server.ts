import http from 'node:http'
import type { Driver } from '../driver/Driver'
import { parseCommand } from '../cli/commands'
import { badRequest, internalError, notFound, readJson, writeJson } from './http'

export interface ServerInfo {
  runId: string
  artifactsDir: string
  mode: '--start' | '--attach'
  frontendUrl: string
}

export interface StartServerOptions {
  host: string
  port: number
  driver: Driver
  info: ServerInfo
  onShutdown: () => Promise<void>
}

export async function startServer(opts: StartServerOptions): Promise<http.Server> {
  let shuttingDown = false

  const server = http.createServer(async (req, res) => {
    try {
      const method = req.method ?? 'GET'
      const url = req.url ?? '/'

      if (method === 'GET' && url === '/health') {
        writeJson(res, 200, { ok: true, ...opts.info })
        return
      }

      if (method === 'POST' && url === '/cmd') {
        let body: any
        try {
          body = await readJson(req)
        } catch {
          badRequest(res, 'invalid_json')
          return
        }
        const cmdText = body?.cmd
        if (typeof cmdText !== 'string' || !cmdText.trim()) {
          badRequest(res, 'cmd_required')
          return
        }

        const cmd = parseCommand(cmdText)
        if (cmd.kind === 'end') {
          badRequest(res, 'use_shutdown_endpoint')
          return
        }

        let state
        switch (cmd.kind) {
          case 'goto':
            state = await opts.driver.goto(cmd.path)
            break
          case 'click':
            state = await opts.driver.click(cmd.selector)
            break
          case 'type':
            state = await opts.driver.type(cmd.selector, cmd.value)
            break
          case 'press':
            state = await opts.driver.press(cmd.key)
            break
          case 'wait':
            state = await opts.driver.wait(cmd.ms)
            break
          case 'back':
            state = await opts.driver.back()
            break
          case 'reload':
            state = await opts.driver.reload()
            break
        }

        writeJson(res, 200, { ok: true, state })
        return
      }

      if (method === 'POST' && url === '/shutdown') {
        if (shuttingDown) {
          writeJson(res, 200, { ok: true, shuttingDown: true })
          return
        }

        shuttingDown = true
        writeJson(res, 200, { ok: true })
        await opts.onShutdown()
        server.close(() => process.exit(0))
        return
      }

      notFound(res)
    } catch (e: any) {
      internalError(res, String(e?.message ?? e))
    }
  })

  await new Promise<void>((resolve, reject) => {
    server.once('error', reject)
    server.listen(opts.port, opts.host, () => resolve())
  })

  return server
}
