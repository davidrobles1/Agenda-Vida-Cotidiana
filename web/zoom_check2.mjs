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

if (!page.url().includes('/calendar')) {
  const calendarLink = page.locator('a[href*="/calendar"], a:has-text("Calendario")');
  if (await calendarLink.count() > 0) { await calendarLink.first().click(); await page.waitForTimeout(800); }
}
await page.getByText('Día', { exact: true }).click();
await page.waitForTimeout(1000);

// Go to a far-future day with (presumably) no clutter: click "next day" several times
const nextBtn = page.locator('button').filter({ hasText: '' }).nth(0); // fallback not used
// Use the ">" chevron button next to "Ir a hoy"
const chevronNext = page.getByRole('button').filter({ has: page.locator('svg') });
// Simpler: click the second small round button after "Ir a hoy" (the right chevron)
const iraHoy = page.getByText('Ir a hoy');
const nextChevron = iraHoy.locator('xpath=following-sibling::button[1]');
for (let i = 0; i < 20; i++) {
  await nextChevron.click();
}
await page.waitForTimeout(500);
await page.screenshot({ path: `${SHOTS}/20-fresh-day.png`, fullPage: true });

console.log('existing elements on this day:', await page.locator('[class*="elementBanner"], [class*="elementText"]').count());

// Add a Banner — editor should auto-open on the new element
const addBanner = page.getByRole('button', { name: /Banner/i });
await addBanner.click();
await page.waitForTimeout(500);
await page.screenshot({ path: `${SHOTS}/21-banner-auto-editing.png`, fullPage: true });

// Switch shape via swatch (2nd option) — should be uncontested now
const shapeSwatches = page.locator('[class*="shapeSwatch"]');
const swatchCount = await shapeSwatches.count();
console.log('shape swatches in fresh editor:', swatchCount);
if (swatchCount > 1) {
  await shapeSwatches.nth(1).click({ force: true });
  await page.waitForTimeout(300);
}

// Type long text
const textarea = page.locator('[class*="editorTextarea"]');
await textarea.fill('Este es un texto muy largo para comprobar que se recorta o hace wrap dentro de la forma del banner sin desbordarse visualmente fuera de la caja delimitadora del elemento.');
await page.waitForTimeout(300);
await page.screenshot({ path: `${SHOTS}/22-banner-long-text-editing.png`, fullPage: true });

// Commit by clicking the toolbar title (outside the element)
await page.getByText('Notas del día', { exact: false }).first().click({ force: true });
await page.waitForTimeout(500);
await page.screenshot({ path: `${SHOTS}/23-banner-committed.png`, fullPage: true });

const banner = page.locator('[class*="elementBanner"]').first();
const bbox = await banner.boundingBox();
if (bbox) {
  await page.screenshot({ path: `${SHOTS}/zoom2-banner-final.png`, clip: { x: bbox.x - 15, y: bbox.y - 15, width: bbox.width + 30, height: bbox.height + 30 } });
}

// Hover + delete check
const delBtn = banner.locator('[class*="deleteButton"]');
const opBefore = await delBtn.evaluate((el) => getComputedStyle(el).opacity);
await banner.hover();
await page.waitForTimeout(200);
const opAfter = await delBtn.evaluate((el) => getComputedStyle(el).opacity);
console.log('banner delete opacity before/after hover:', opBefore, opAfter);
await delBtn.click();
await page.waitForTimeout(400);
console.log('banners remaining after delete:', await page.locator('[class*="elementBanner"]').count());
await page.screenshot({ path: `${SHOTS}/24-banner-deleted.png`, fullPage: true });

// Now do Texto element the same way
const addTexto = page.getByRole('button', { name: /Texto/i });
await addTexto.click();
await page.waitForTimeout(500);
const textareaT = page.locator('[class*="editorTextarea"]');
await textareaT.fill('Otro texto muy largo para verificar wrap dentro del recuadro sin desbordarse fuera de sus bordes visibles en la tarjeta.');
await page.waitForTimeout(300);
await page.getByText('Notas del día', { exact: false }).first().click({ force: true });
await page.waitForTimeout(500);
await page.screenshot({ path: `${SHOTS}/25-text-committed.png`, fullPage: true });

const textEl = page.locator('[class*="elementText"]').first();
const bbox2 = await textEl.boundingBox();
if (bbox2) {
  await page.screenshot({ path: `${SHOTS}/zoom2-text-final.png`, clip: { x: bbox2.x - 15, y: bbox2.y - 15, width: bbox2.width + 30, height: bbox2.height + 30 } });
}
const delBtn2 = textEl.locator('[class*="deleteButton"]');
const op2Before = await delBtn2.evaluate((el) => getComputedStyle(el).opacity);
await textEl.hover();
await page.waitForTimeout(200);
const op2After = await delBtn2.evaluate((el) => getComputedStyle(el).opacity);
console.log('text delete opacity before/after hover:', op2Before, op2After);
await delBtn2.click();
await page.waitForTimeout(400);
console.log('texts remaining after delete:', await page.locator('[class*="elementText"]').count());

console.log('FINAL Console errors:', JSON.stringify(consoleErrors));
await browser.close();
