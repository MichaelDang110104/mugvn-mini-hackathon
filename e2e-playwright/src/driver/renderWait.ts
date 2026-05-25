import type { Page } from '@playwright/test'

export async function settle(page: Page, timeoutMs: number = 10_000): Promise<void> {
  await page.waitForLoadState('domcontentloaded', { timeout: timeoutMs }).catch(() => {})
  await page.waitForLoadState('networkidle', { timeout: timeoutMs }).catch(() => {})
  await page.waitForTimeout(300)
}
