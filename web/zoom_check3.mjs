import { chromium } from 'playwright';

const SHOTS = '/private/tmp/claude-503/-Users-Familia-Documents-Infraestructure-Vida-Cotidiana/23c75c15-dae0-432a-a718-3ce0c3a66cdc/scratchpad/shots';
const consoleErrors = [];

const browser = await chromium.launch();
const page = await browser.newPage({ viewport: { width: 1280, height: 1100 }, deviceScaleFactor: 3 });
page.on('console', (msg) => { if (msg.type() === 'error') consoleErrors.push(msg.text()); });
page.on('pageerror', (err) => consoleErrors.push('PAGEERROR: ' + err.message));

await page.goto('http://localhost:5173');
await page.waitForLoadState('networkidle');
const loginBtn = page.getByText('Iniciar sesión', { exact: false });
if (await loginBtn.count() > 0) { await loginBtn.first().click(); await page.waitForLoadState('networkidle'); }
await page.waitForTimeout(500);
if (page.url().includes('localhost:8081') || (await page.locator('input[name="username"]').count()) > 0) {
  await page.fill('input[name="username"]', 'testuser');
  await page.fill('input[name="password"]', 'TestPass123!');
  await page.click('input[type="submit"], button[type="submit"]');
  await page.waitForLoadState('networkidle');
}
await page.waitForTimeout(1500);
await page.getByText('Día', { exact: true }).click();
await page.waitForTimeout(1000);
const iraHoy = page.getByText('Ir a hoy');
const nextChevron = iraHoy.locator('xpath=following-sibling::button[1]');
// Go to a day even further out to guarantee a clean slate
for (let i = 0; i < 40; i++) { await nextChevron.click(); }
await page.waitForTimeout(500);

const addBanner = page.getByRole('button', { name: /Banner/i });
const banners = page.locator('[class*="elementBanner"]');

// Add banners until one wraps to a lower row (y increases)
let lastY = null;
for (let i = 0; i < 8; i++) {
  await addBanner.click();
  await page.waitForTimeout(300);
  const n = await banners.count();
  const bb = await banners.nth(n - 1).boundingBox();
  console.log('added banner', i, 'count', n, 'bbox', JSON.stringify(bb));
  if (lastY !== null && bb && bb.y > lastY + 20) {
    console.log('WRAPPED to new row at index', i);
    break;
  }
  if (bb) lastY = bb.y;
}

// Click elsewhere to close editor of the last add
await page.getByText('Notas del día', { exact: false }).first().click({ force: true });
await page.waitForTimeout(400);

const n = await banners.count();
const target = banners.nth(n - 1); // the last (deepest row) banner
const tbox = await target.boundingBox();
console.log('target banner box:', JSON.stringify(tbox));

await target.dblclick();
await page.waitForTimeout(400);
await page.screenshot({ path: `${SHOTS}/30-target-editing.png`, fullPage: true });

const shapeSwatches = page.locator('[class*="shapeSwatch"]');
const swatchCount = await shapeSwatches.count();
console.log('swatch count', swatchCount);
if (swatchCount > 1) {
  await shapeSwatches.nth(2).click();
  await page.waitForTimeout(300);
}

const textarea = page.locator('[class*="editorTextarea"]');
await textarea.fill('Este es un texto muy largo para comprobar que se recorta o hace wrap dentro de la forma del banner sin desbordarse visualmente fuera de la caja delimitadora del elemento.');
await page.waitForTimeout(300);
await page.screenshot({ path: `${SHOTS}/31-target-long-text-editing.png`, fullPage: true });

await page.getByText('Notas del día', { exact: false }).first().click({ force: true });
await page.waitForTimeout(500);

const tbox2 = await target.boundingBox();
console.log('final target box', JSON.stringify(tbox2));
if (tbox2) {
  await page.screenshot({ path: `${SHOTS}/zoom3-target-final.png`, clip: { x: tbox2.x - 20, y: tbox2.y - 20, width: tbox2.width + 40, height: tbox2.height + 40 } });
}

console.log('FINAL Console errors:', JSON.stringify(consoleErrors));
await browser.close();
