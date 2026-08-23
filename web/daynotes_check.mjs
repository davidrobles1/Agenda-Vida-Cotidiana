import { chromium } from 'playwright';

const SHOTS = '/private/tmp/claude-503/-Users-Familia-Documents-Infraestructure-Vida-Cotidiana/23c75c15-dae0-432a-a718-3ce0c3a66cdc/scratchpad/shots';
const consoleErrors = [];

const browser = await chromium.launch();
const page = await browser.newPage({ viewport: { width: 1280, height: 1100 } });

page.on('console', (msg) => {
  if (msg.type() === 'error') consoleErrors.push(msg.text());
});
page.on('pageerror', (err) => consoleErrors.push('PAGEERROR: ' + err.message));

await page.goto('http://localhost:5173');
await page.waitForLoadState('networkidle');

const loginBtn = page.getByText('Iniciar sesión', { exact: false });
if (await loginBtn.count() > 0) {
  await loginBtn.first().click();
  await page.waitForLoadState('networkidle');
}
await page.waitForTimeout(500);

if (page.url().includes('localhost:8081') || (await page.locator('input[name="username"]').count()) > 0) {
  await page.fill('input[name="username"]', 'testuser');
  await page.fill('input[name="password"]', 'TestPass123!');
  await page.click('input[type="submit"], button[type="submit"]');
  await page.waitForLoadState('networkidle');
}
await page.waitForTimeout(1500);
console.log('URL after login:', page.url());

if (!page.url().includes('/calendar')) {
  const calendarLink = page.locator('a[href*="/calendar"], a:has-text("Calendario")');
  if (await calendarLink.count() > 0) {
    await calendarLink.first().click();
    await page.waitForTimeout(800);
  }
}

// Click "Día" tab for Vista diaria
await page.getByText('Día', { exact: true }).click();
await page.waitForTimeout(1000);
await page.screenshot({ path: `${SHOTS}/02-vista-diaria.png`, fullPage: true });

const notasDelDia = page.getByText('Notas del día', { exact: false });
console.log('Notas del día visible:', await notasDelDia.count());

console.log('Console errors so far:', JSON.stringify(consoleErrors));

// Keep browser open state saved for next step via storageState? We can't persist in-memory auth across processes.
// Instead do everything in this single script run.

if (await notasDelDia.count() > 0) {
  // Scroll to Notas del día panel
  await notasDelDia.first().scrollIntoViewIfNeeded();
  await page.waitForTimeout(300);

  // Click "+ Banner"
  const addBanner = page.getByRole('button', { name: /Banner/i });
  await addBanner.click();
  await page.waitForTimeout(500);
  await page.screenshot({ path: `${SHOTS}/03-after-add-banner.png`, fullPage: true });

  // Close editor by clicking elsewhere (e.g., the toolbar title)
  await page.getByText('Notas del día', { exact: false }).first().click({ force: true });
  await page.waitForTimeout(500);
  await page.screenshot({ path: `${SHOTS}/04-banner-closed.png`, fullPage: true });

  // Zoomed screenshot of just the day notes canvas area
  const canvas = page.locator('[class*="canvas"]').last();
  await canvas.screenshot({ path: `${SHOTS}/05-canvas-zoom.png` }).catch(async () => {
    console.log('canvas locator screenshot failed, trying alt selector');
  });

  // Add Texto too
  const addTexto = page.getByRole('button', { name: /Texto/i });
  await addTexto.click();
  await page.waitForTimeout(500);
  await page.getByText('Notas del día', { exact: false }).first().click({ force: true });
  await page.waitForTimeout(500);
  await page.screenshot({ path: `${SHOTS}/06-both-elements.png`, fullPage: true });
}

console.log('FINAL Console errors:', JSON.stringify(consoleErrors));

await browser.close();
