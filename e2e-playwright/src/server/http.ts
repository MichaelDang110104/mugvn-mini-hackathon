import type { IncomingMessage, ServerResponse } from 'node:http'

export async function readJson(req: IncomingMessage): Promise<any> {
  const chunks: Buffer[] = []
  for await (const chunk of req) chunks.push(Buffer.from(chunk))
  if (chunks.length === 0) return null
  const text = Buffer.concat(chunks).toString('utf-8')
  if (!text.trim()) return null
  return JSON.parse(text)
}

export function writeJson(res: ServerResponse, status: number, body: any): void {
  const json = JSON.stringify(body, null, 2)
  res.statusCode = status
  res.setHeader('content-type', 'application/json; charset=utf-8')
  res.end(json)
}

export function notFound(res: ServerResponse): void {
  writeJson(res, 404, { ok: false, error: 'not_found' })
}

export function badRequest(res: ServerResponse, message: string): void {
  writeJson(res, 400, { ok: false, error: message })
}

export function internalError(res: ServerResponse, message: string): void {
  writeJson(res, 500, { ok: false, error: message })
}
