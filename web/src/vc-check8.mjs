import { chromium } from 'playwright'
const browser = await chromium.launch()
const page = await browser.newPage({ viewport: { width: 1440, height: 900 } })
const errs = []
page.on('pageerror', e => errs.push('PAGEERROR: ' + String(e).slice(0,200)))
await page.goto('http://localhost:5173/', { waitUntil: 'domcontentloaded' })
await page.waitForTimeout(2500)
if (page.url().includes('8081')) {
  await page.fill('#username','testuser'); await page.fill('#password','TestPass123!')
  await page.click('#kc-login'); await page.waitForTimeout(5000)
}
const probe = async (l) => console.log(l, JSON.stringify(await page.evaluate(() => {
  const c = document.elementFromPoint(innerWidth/2, innerHeight/2)
  const covers = [...document.querySelectorAll('body *')].filter(el => {
    const r = el.getBoundingClientRect(), cs = getComputedStyle(el)
    return r.width>=innerWidth*0.85 && r.height>=innerHeight*0.85 && cs.position==='fixed' && cs.display!=='none'
  }).map(el => ({ cls: String(el.className).slice(0,40), bg: getComputedStyle(el).backgroundColor }))
  return { url: location.pathname, center: c? c.tagName.toLowerCase()+'.'+String(c.className).slice(0,35):null,
           covers, text: (document.body.innerText||'').replace(/\s+/g,' ').slice(0,60) }
})))
await page.getByRole('link', { name: 'Personal', exact: true }).first().click()
await page.waitForTimeout(2500); await probe('Personal:')
const cal = page.getByRole('link', { name: 'Calendario personal' }).first()
if (await cal.count()) { await cal.click(); await page.waitForTimeout(3500); await probe('Calendario personal:') }
else console.log('no hay enlace "Calendario personal"')
await page.screenshot({ path: '/tmp/vc-personal-cal.png' })
console.log('pageerrors:', errs)
await browser.close()
