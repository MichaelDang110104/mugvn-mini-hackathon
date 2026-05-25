import { chromium, type Browser, type BrowserContext, type Page } from '@playwright/test'
import path from 'node:path'
import { appendLine, writeText, type ArtifactPaths } from './artifacts'
import { settle } from './renderWait'

export interface ReducedState {
  ts: string
  url: string
  title: string
  visibleTextSample: string
  consoleErrors: string[]
}

export class Driver {
  private browser: Browser | null = null
  private context: BrowserContext | null = null
  private page: Page | null = null
  private consoleErrors: string[] = []
  private step = 0

  constructor(private readonly artifacts: ArtifactPaths, private readonly baseUrl: string) {}

  async start(): Promise<void> {
    this.browser = await chromium.launch({ headless: false })
    this.context = await this.browser.newContext({
      recordVideo: { dir: this.artifacts.videoDir },
      recordHar: { path: path.join(this.artifacts.networkDir, 'network.har'), mode: 'minimal' },
    })

    await this.context.tracing.start({ screenshots: false, snapshots: true, sources: true })

    this.page = await this.context.newPage()

    this.page.on('console', msg => {
      const line = `[${msg.type()}] ${msg.text()}`
      appendLine(path.join(this.artifacts.logsDir, 'browser-console.log'), line)
      if (msg.type() === 'error') this.consoleErrors.push(line)
    })

    this.page.on('pageerror', err => {
      const line = `[pageerror] ${String(err)}`
      appendLine(path.join(this.artifacts.logsDir, 'browser-console.log'), line)
      this.consoleErrors.push(line)
    })
  }

  async stop(): Promise<void> {
    if (this.context) {
      await this.context.tracing.stop({ path: path.join(this.artifacts.traceDir, 'trace.zip') })
    }
    await this.context?.close().catch(() => {})
    await this.browser?.close().catch(() => {})
  }

  private ensurePage(): Page {
    if (!this.page) throw new Error('Driver not started')
    return this.page
  }

  async goto(pathname: string): Promise<ReducedState> {
    const page = this.ensurePage()
    const url = pathname.startsWith('http') ? pathname : `${this.baseUrl}${pathname}`
    await page.goto(url)
    await settle(page)
    return this.captureState('goto')
  }

  async click(selector: string): Promise<ReducedState> {
    const page = this.ensurePage()
    await page.click(selector)
    await settle(page)
    return this.captureState('click')
  }

  async type(selector: string, value: string): Promise<ReducedState> {
    const page = this.ensurePage()
    await page.fill(selector, value)
    await settle(page)
    return this.captureState('type')
  }

  async press(key: string): Promise<ReducedState> {
    const page = this.ensurePage()
    await page.keyboard.press(key)
    await settle(page)
    return this.captureState('press')
  }

  async wait(ms: number): Promise<ReducedState> {
    const page = this.ensurePage()
    await page.waitForTimeout(ms)
    return this.captureState('wait')
  }

  async back(): Promise<ReducedState> {
    const page = this.ensurePage()
    await page.goBack().catch(() => null)
    await settle(page)
    return this.captureState('back')
  }

  async reload(): Promise<ReducedState> {
    const page = this.ensurePage()
    await page.reload()
    await settle(page)
    return this.captureState('reload')
  }

  private async captureState(action: string): Promise<ReducedState> {
    const page = this.ensurePage()
    this.step += 1

    const html = await page.content()
    const title = await page.title()
    const url = page.url()

    const bodyText = await page.evaluate(() => document.body?.innerText ?? '')
    const visibleTextSample = bodyText.length > 2000 ? bodyText.slice(0, 2000) : bodyText

    const htmlPath = path.join(this.artifacts.htmlDir, `step-${String(this.step).padStart(4, '0')}-${action}.html`)
    writeText(htmlPath, html)

    return {
      ts: new Date().toISOString(),
      url,
      title,
      visibleTextSample,
      consoleErrors: this.consoleErrors.slice(-50),
    }
  }
}
