const { chromium } = require('playwright');

(async () => {
  const browser = await chromium.launch({ channel: 'chrome', headless: true });
  const context = await browser.newContext();
  const page = await context.newPage();

  await page.goto('http://localhost:5173/');
  await page.getByRole('button', { name: 'Iniciar sesión' }).click();
  await page.waitForURL(/realms\/vida-cotidiana/);
  await page.getByLabel('Username or email').fill('testuser');
  await page.getByRole('textbox', { name: 'Password' }).fill('TestPass123!');
  await page.getByRole('button', { name: 'Sign In' }).click();
  await page.getByText('Vista mensual').waitFor({ timeout: 20000 });

  await page.getByRole('link', { name: 'Calendario' }).click();
  await page.getByText('Vista mensual').waitFor({ timeout: 10000 });
  await page.getByRole('radio', { name: 'Día' }).click();
  await page.getByText('Vista diaria').waitFor({ timeout: 10000 });
  await page.getByText('Notas del día').waitFor({ timeout: 10000 });

  await page.getByRole('button', { name: /Banner/ }).click();
  const textarea = page.locator('textarea').first();
  await textarea.waitFor({ timeout: 10000 });
  await textarea.fill('Debug banner');
  await page.locator('body').click({ position: { x: 5, y: 5 } });
  await page.waitForTimeout(500);

  const info = await page.evaluate(() => {
    const svgs = Array.from(document.querySelectorAll('svg'));
    const svg = svgs.find(s => s.getAttribute('class') && s.getAttribute('class').toLowerCase().includes('banner')) || svgs[svgs.length - 1];
    if (!svg) return { error: 'svg not found' };
    const path = svg.querySelector('path');
    const rect = svg.getBoundingClientRect();
    const containerRect = svg.parentElement.getBoundingClientRect();
    return {
      svgClass: svg.getAttribute('class'),
      viewBox: svg.getAttribute('viewBox'),
      preserveAspectRatio: svg.getAttribute('preserveAspectRatio'),
      pathD: path ? path.getAttribute('d') : null,
      pathComputedFill: path ? getComputedStyle(path).fill : null,
      pathComputedStroke: path ? getComputedStyle(path).stroke : null,
      pathFillRule: path ? getComputedStyle(path).fillRule : null,
      svgRect: { width: rect.width, height: rect.height },
      containerRect: { width: containerRect.width, height: containerRect.height },
      svgOuterHTML: svg.outerHTML,
    };
  });
  console.log(JSON.stringify(info, null, 2));

  await page.screenshot({ path: '/private/tmp/claude-503/-Users-Familia-Documents-Infraestructure-Vida-Cotidiana/23c75c15-dae0-432a-a718-3ce0c3a66cdc/scratchpad/daynotes-debug-banner.png', fullPage: true });

  await browser.close();
})();
