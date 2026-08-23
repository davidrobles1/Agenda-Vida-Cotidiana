import { test, expect } from '@playwright/test'
import { loginAsTestuserAndGoToReminders } from './helpers'

/**
 * WEB-006 real verification: owner (testuser) logs in, clicks "Debug: trigger
 * error" (throws during render, caught by the Sentry.ErrorBoundary in
 * main.tsx), and the event is confirmed via GlitchTip's own Sentry-compatible
 * API — not just by looking at GlitchTip's UI — that it actually reached the
 * self-hosted instance (docker-compose.yml, glitchtip-web/-worker/-redis,
 * reusing the existing postgres service's "glitchtip" database).
 *
 * GLITCHTIP_API_TOKEN comes from the one-time local setup documented in
 * 01-technical-backlog.md (WEB-006), read from .env (gitignored) via
 * playwright.config.ts's `dotenv/config` import.
 *
 * Security cross-audit correction (Documentacion/33-security-cross-audit.md):
 * an earlier version of this file hardcoded the real token here and claimed,
 * incorrectly, that it "wasn't a real secret" by analogy with Firebase's
 * public apiKey. That analogy doesn't hold — a GlitchTip/Sentry API token is
 * a genuine bearer credential (unlike a Firebase Web apiKey, which Google's
 * own docs confirm is safe to ship client-side: see the ADR/audit doc for
 * the citation), and `gitleaks detect --source . --log-opts="--all"` flagged
 * it for real. Fixed by moving it out of source; the org/project slugs below
 * are just identifiers, not credentials, so they stay as plain constants.
 */
const GLITCHTIP_BASE_URL = 'http://localhost:8000'
const GLITCHTIP_ORG_SLUG = 'vida-cotidiana'
const GLITCHTIP_PROJECT_SLUG = 'vida-cotidiana-web'

if (!process.env.GLITCHTIP_API_TOKEN) {
  throw new Error(
    'GLITCHTIP_API_TOKEN is not set — add it to web/.env (see .env.example). ' +
      'It is the real API token for the local GlitchTip instance, generated ' +
      'per the one-time setup documented under WEB-006 in 01-technical-backlog.md.',
  )
}
const GLITCHTIP_API_TOKEN = process.env.GLITCHTIP_API_TOKEN

test('a real thrown error reaches GlitchTip, confirmed via its API', async ({ page, request }) => {
  const marker = `WEB-006 debug crash: manually triggered from RemindersPage`

  await loginAsTestuserAndGoToReminders(page)

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
