import net from 'node:net'

export interface WaitForPortOptions {
  host: string
  port: number
  timeoutMs: number
  pollMs: number
}

export async function waitForPort(opts: WaitForPortOptions): Promise<void> {
  const start = Date.now()

  while (Date.now() - start < opts.timeoutMs) {
    const ok = await new Promise<boolean>(resolve => {
      const socket = net.connect({ host: opts.host, port: opts.port })

      const finish = (result: boolean) => {
        socket.removeAllListeners()
        socket.end()
        socket.destroy()
        resolve(result)
      }

      socket.once('connect', () => finish(true))
      socket.once('error', () => finish(false))
      socket.setTimeout(2000, () => finish(false))
    })

    if (ok) return
    await new Promise(r => setTimeout(r, opts.pollMs))
  }

  throw new Error(`Timed out waiting for ${opts.host}:${opts.port}`)
}
