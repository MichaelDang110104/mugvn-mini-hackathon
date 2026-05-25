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
