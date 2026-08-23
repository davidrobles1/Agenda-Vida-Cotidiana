const { chromium } = require('playwright');

const SCREEN_DIR = '/private/tmp/claude-503/-Users-Familia-Documents-Infraestructure-Vida-Cotidiana/23c75c15-dae0-432a-a718-3ce0c3a66cdc/scratchpad';

function rewriteHost(url) {
  return url
    .replace('192.168.0.18:8080', 'localhost:8080')
    .replace('192.168.0.18:8081', 'localhost:8081');
}

(async () => {
  const browser = await chromium.launch({ channel: 'chrome', headless: true });
  const context = await browser.newContext();
  const page = await context.newPage();

  const consoleErrors = [];
  const networkErrors = [];
  page.on('console', (msg) => {
    if (msg.type() === 'error') consoleErrors.push(msg.text());
  });
  page.on('response', (resp) => {
    if (resp.status() >= 400) networkErrors.push(`${resp.status()} ${resp.url()}`);
  });

  // Note: verified this run's dev server already targets localhost (no
  // 192.168.x env override active), so no route interception is needed —
  // and interception via route.continue({url}) was found to break the
  // Authorization header (all API calls 401'd), so it's deliberately
  // omitted here.

  try {
    await page.goto('http://localhost:5173/');
    await page.getByRole('button', { name: 'Iniciar sesión' }).click();
    await page.waitForURL(/realms\/vida-cotidiana/);
    await page.getByLabel('Username or email').fill('testuser');
    await page.getByRole('textbox', { name: 'Password' }).fill('TestPass123!');
    await page.getByRole('button', { name: 'Sign In' }).click();

    await page.getByText('Vista mensual').waitFor({ timeout: 20000 });
    console.log('LOGIN_OK');

    await page.getByRole('link', { name: 'Calendario' }).click();
    await page.getByText('Vista mensual').waitFor({ timeout: 10000 });

    await page.getByRole('radio', { name: 'Día' }).click();
    await page.getByText('Vista diaria').waitFor({ timeout: 10000 });
    console.log('DAY_VIEW_OK');

    await page.getByText('Notas del día').waitFor({ timeout: 10000 });
    console.log('DAY_NOTES_PANEL_OK');

    // Create banner #1
    await page.getByRole('button', { name: /Banner/ }).click();
    const textarea = page.locator('textarea').first();
    await textarea.waitFor({ timeout: 10000 });
    await textarea.fill('Prueba negrita');
    console.log('BANNER1_TEXT_SET');

    // Click Negrita
    const boldBtn = page.getByRole('button', { name: 'Negrita' });
    await boldBtn.click();
    await page.waitForTimeout(300);

    const editorStillOpenAfterBold = await textarea.isVisible().catch(() => false);
    const fontWeightAfterBold = editorStillOpenAfterBold ? await textarea.evaluate((el) => getComputedStyle(el).fontWeight) : 'N/A (editor closed)';
    console.log('AFTER_BOLD_CLICK editorOpen=' + editorStillOpenAfterBold + ' fontWeight=' + fontWeightAfterBold);

    if (editorStillOpenAfterBold) {
      // Click Cursiva
      const italicBtn = page.getByRole('button', { name: 'Cursiva' });
      await italicBtn.click();
      await page.waitForTimeout(300);
      const editorStillOpenAfterItalic = await textarea.isVisible().catch(() => false);
      const fontStyleAfterItalic = editorStillOpenAfterItalic ? await textarea.evaluate((el) => getComputedStyle(el).fontStyle) : 'N/A (editor closed)';
      console.log('AFTER_ITALIC_CLICK editorOpen=' + editorStillOpenAfterItalic + ' fontStyle=' + fontStyleAfterItalic);

      if (editorStillOpenAfterItalic) {
        // keep typing after toggling
        await textarea.type(' más texto');
        console.log('TYPED_AFTER_TOGGLE_OK current value=' + (await textarea.inputValue()));

        // Blur to save - click elsewhere on canvas background
        await page.locator('body').click({ position: { x: 5, y: 5 } });
        await page.waitForTimeout(500);
      }
    }

    await page.screenshot({ path: `${SCREEN_DIR}/daynotes-after-toggle.png`, fullPage: true });
    console.log('SCREENSHOT_1_TAKEN');

    // Verify persistence via in-app nav away and back (not reload):
    // Día -> Semana -> Mes -> Día.
    await page.getByRole('radio', { name: 'Semana' }).click();
    await page.getByText('Vista semanal').waitFor({ timeout: 10000 });
    await page.getByRole('radio', { name: 'Mes' }).click();
    await page.getByText('Vista mensual').waitFor({ timeout: 10000 });
    await page.getByRole('radio', { name: 'Día' }).click();
    await page.getByText('Vista diaria').waitFor({ timeout: 10000 });
    await page.getByText('Notas del día').waitFor({ timeout: 10000 });
    await page.waitForTimeout(500);

    const label = page.locator('span', { hasText: 'Prueba negrita' }).first();
    const labelExists = await label.count();
    let persistedWeight = 'N/A';
    let persistedStyle = 'N/A';
    if (labelExists > 0) {
      persistedWeight = await label.evaluate((el) => getComputedStyle(el).fontWeight);
      persistedStyle = await label.evaluate((el) => getComputedStyle(el).fontStyle);
    }
    console.log('PERSISTED after nav-away-and-back: exists=' + labelExists + ' fontWeight=' + persistedWeight + ' fontStyle=' + persistedStyle);

    // Create 2 more banners to check for the "bowtie" corrupted shape
    for (let i = 2; i <= 3; i++) {
      await page.getByRole('button', { name: /Banner/ }).click();
      const ta = page.locator('textarea').first();
      await ta.waitFor({ timeout: 10000 });
      await ta.fill(`Banner ${i}`);
      await page.locator('body').click({ position: { x: 5, y: 5 } });
      await page.waitForTimeout(400);
    }

    await page.waitForTimeout(500);
    await page.screenshot({ path: `${SCREEN_DIR}/daynotes-multiple-banners.png`, fullPage: true });
    console.log('SCREENSHOT_2_TAKEN');

    console.log('CONSOLE_ERRORS: ' + JSON.stringify(consoleErrors));
    console.log('NETWORK_ERRORS: ' + JSON.stringify(networkErrors));
  } catch (err) {
    console.log('SCRIPT_ERROR: ' + err.message);
    await page.screenshot({ path: `${SCREEN_DIR}/daynotes-error.png`, fullPage: true }).catch(() => {});
    console.log('CONSOLE_ERRORS: ' + JSON.stringify(consoleErrors));
    console.log('NETWORK_ERRORS: ' + JSON.stringify(networkErrors));
  } finally {
    await browser.close();
  }
})();
