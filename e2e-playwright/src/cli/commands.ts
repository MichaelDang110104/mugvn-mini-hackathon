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
