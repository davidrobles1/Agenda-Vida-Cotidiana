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

// Zoom on each existing banner/text element's bounding box with padding, at high DPI
const bannerEls = page.locator('[class*="elementBanner"]');
const textEls = page.locator('[class*="elementText"]');
console.log('banners:', await bannerEls.count(), 'texts:', await textEls.count());

for (let i = 0; i < await bannerEls.count(); i++) {
  const box = await bannerEls.nth(i).boundingBox();
  if (!box) continue;
  await page.screenshot({
    path: `${SHOTS}/zoom-banner-${i}.png`,
    clip: { x: box.x - 15, y: box.y - 15, width: box.width + 30, height: box.height + 30 },
  });
}
for (let i = 0; i < await textEls.count(); i++) {
  const box = await textEls.nth(i).boundingBox();
  if (!box) continue;
  await page.screenshot({
    path: `${SHOTS}/zoom-text-${i}.png`,
    clip: { x: box.x - 15, y: box.y - 15, width: box.width + 30, height: box.height + 30 },
  });
}

// Hover test: check delete button opacity changes on hover for first banner
const firstBanner = bannerEls.first();
const beforeHoverOpacity = await firstBanner.locator('[class*="deleteButton"]').evaluate((el) => getComputedStyle(el).opacity);
await firstBanner.hover();
await page.waitForTimeout(300);
const afterHoverOpacity = await firstBanner.locator('[class*="deleteButton"]').evaluate((el) => getComputedStyle(el).opacity);
console.log('delete button opacity before hover:', beforeHoverOpacity, 'after hover:', afterHoverOpacity);

// Test double-click to edit banner shape swatches, switch shape
await firstBanner.dblclick();
await page.waitForTimeout(500);
await page.screenshot({ path: `${SHOTS}/07-banner-editor-open.png`, fullPage: true });

const shapeSwatches = page.locator('[class*="shapeSwatch"]');
const swatchCount = await shapeSwatches.count();
console.log('shape swatches:', swatchCount);
if (swatchCount > 1) {
  await shapeSwatches.nth(1).click({ force: true });
  await page.waitForTimeout(300);
  await page.screenshot({ path: `${SHOTS}/08-banner-shape-switched.png`, fullPage: true });
  const box2 = await firstBanner.boundingBox();
  if (box2) {
    await page.screenshot({
      path: `${SHOTS}/zoom-banner-shape-switched.png`,
      clip: { x: box2.x - 15, y: box2.y - 15, width: box2.width + 30, height: box2.height + 30 },
    });
  }
}

// Type long text into the textarea while editor open (still same element in edit mode after shape click)
const textarea = page.locator('[class*="editorTextarea"]');
if (await textarea.count() > 0) {
  await textarea.fill('Este es un texto muy largo para comprobar que se recorta o hace wrap dentro de la forma del banner sin desbordarse visualmente fuera de la caja delimitadora del elemento.');
  await page.waitForTimeout(300);
  await page.screenshot({ path: `${SHOTS}/09-long-text-editing.png`, fullPage: true });
}

// Commit by clicking elsewhere
await page.getByText('Notas del día', { exact: false }).first().click({ force: true });
await page.waitForTimeout(500);
await page.screenshot({ path: `${SHOTS}/10-long-text-committed.png`, fullPage: true });
const box3 = await firstBanner.boundingBox();
if (box3) {
  await page.screenshot({
    path: `${SHOTS}/zoom-long-text.png`,
    clip: { x: box3.x - 15, y: box3.y - 15, width: box3.width + 30, height: box3.height + 30 },
  });
}

// Test delete click
const trashBtn = firstBanner.locator('[class*="deleteButton"]');
await trashBtn.click();
await page.waitForTimeout(500);
const remainingBanners = await bannerEls.count();
console.log('banners remaining after delete:', remainingBanners);
await page.screenshot({ path: `${SHOTS}/11-after-delete.png`, fullPage: true });

console.log('FINAL Console errors:', JSON.stringify(consoleErrors));
await browser.close();
