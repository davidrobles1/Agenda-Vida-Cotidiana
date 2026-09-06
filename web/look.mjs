import { chromium } from 'playwright'
const b = await chromium.launch()
const D='/private/tmp/claude-503/-Users-Familia-Documents-Infraestructure-Vida-Cotidiana/23c75c15-dae0-432a-a718-3ce0c3a66cdc/scratchpad/'
for (const [f,label] of [['android-light.html','light'],['android-complete.html','complete']]) {
  const p = await b.newPage({viewport:{width:1000,height:1000}})
  const errs=[]; p.on('pageerror',e=>errs.push(String(e).slice(0,160)))
  await p.goto('file://'+D+f); await p.waitForTimeout(1200)
  await p.screenshot({path:`/tmp/${label}-1-login.png`})
  await p.click('[data-act="login"]'); await p.waitForTimeout(500)
  await p.screenshot({path:`/tmp/${label}-2-home.png`})
  if (label==='light') { await p.click('[data-tab="more"]'); await p.waitForTimeout(400); await p.screenshot({path:'/tmp/light-3-more.png'})
    await p.click('[data-go="payments"]'); await p.waitForTimeout(400); await p.screenshot({path:'/tmp/light-4-pagos.png'}) }
  else { await p.click('[data-act="drawer"]'); await p.waitForTimeout(500); await p.screenshot({path:'/tmp/complete-3-drawer.png'})
    await p.click('[data-nav="payments"]'); await p.waitForTimeout(500); await p.screenshot({path:'/tmp/complete-4-pagos.png'}) }
  console.log(label, errs.length? 'ERR: '+errs[0] : 'sin errores')
  await p.close()
}
await b.close()
