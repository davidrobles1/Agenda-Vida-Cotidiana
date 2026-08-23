import { chromium } from 'playwright';
const browser = await chromium.launch();
const page = await browser.newPage({ viewport: { width: 1280, height: 1100 } });
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
for (let i = 0; i < 20; i++) { await nextChevron.click(); }
await page.waitForTimeout(500);
const banners = page.locator('[class*="elementBanner"]');
const n = await banners.count();
console.log('banner count', n);
for (let i=0;i<n;i++){ console.log(i, JSON.stringify(await banners.nth(i).boundingBox())); }
await browser.close();
