import { test, expect } from '@playwright/test'

/**
 * WEB-006 real verification: owner (testuser) logs in, clicks "Debug: trigger
 * error" (throws during render, caught by the Sentry.ErrorBoundary in
 * main.tsx), and the event is confirmed via GlitchTip's own Sentry-compatible
 * API — not just by looking at GlitchTip's UI — that it actually reached the
 * self-hosted instance (docker-compose.yml, glitchtip-web/-worker/-redis,
 * reusing the existing postgres service's "glitchtip" database).
 *
 * GLITCHTIP_API_TOKEN/GLITCHTIP_ORG_SLUG/GLITCHTIP_PROJECT_SLUG come from the
 * one-time local setup documented in 01-technical-backlog.md (WEB-006) —
 * they identify a local-dev-only GlitchTip project, not a real secret.
 */
const GLITCHTIP_BASE_URL = 'http://localhost:8000'
const GLITCHTIP_API_TOKEN = '15829d560c914d19debb3500329d40764f79c207f6a3d758ab2dc5726ea24705'
const GLITCHTIP_ORG_SLUG = 'vida-cotidiana'
const GLITCHTIP_PROJECT_SLUG = 'vida-cotidiana-web'

test('a real thrown error reaches GlitchTip, confirmed via its API', async ({ page, request }) => {
  const marker = `WEB-006 debug crash: manually triggered from RemindersPage`

  await page.goto('/')
  await page.getByRole('button', { name: 'Log in' }).click()

  await page.waitForURL(/realms\/vida-cotidiana/)
  await page.getByLabel('Username or email').fill('testuser')
  await page.getByRole('textbox', { name: 'Password' }).fill('TestPass123!')
  await page.getByRole('button', { name: 'Sign In' }).click()

  await expect(page.getByPlaceholder('New reminder')).toBeVisible({ timeout: 20_000 })

  // Catch the page crash Playwright would otherwise surface as a test failure —
  // it's expected here, caught by our own ErrorBoundary, not a real test bug.
  page.on('pageerror', () => {})
  await page.getByRole('button', { name: 'Debug: trigger error' }).click()

  await expect(page.getByText('Something went wrong. Please reload the page.')).toBeVisible()

  // Give GlitchTip's worker (Celery, async event ingestion) a moment to
  // process the event before asking its API — real network round trip.
  let found = false
  for (let attempt = 0; attempt < 15 && !found; attempt++) {
    const response = await request.get(
      `${GLITCHTIP_BASE_URL}/api/0/projects/${GLITCHTIP_ORG_SLUG}/${GLITCHTIP_PROJECT_SLUG}/issues/`,
      { headers: { Authorization: `Bearer ${GLITCHTIP_API_TOKEN}` } },
    )
    expect(response.ok()).toBeTruthy()
    const issues = (await response.json()) as Array<{ title: string; culprit: string }>
    found = issues.some((issue) => issue.title.includes(marker) || issue.culprit?.includes(marker))
    if (!found) await page.waitForTimeout(1000)
  }

  expect(found).toBeTruthy()
})
