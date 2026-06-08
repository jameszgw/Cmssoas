import { chromium } from 'playwright'
import { resolve, dirname } from 'path'
import { fileURLToPath } from 'url'
const __dir = dirname(fileURLToPath(import.meta.url))
const base = 'http://localhost:4173'

const shots = [
  { name:'app-01-总览-科技蓝-2K',   path:'/overview', theme:'tech',     lang:'zh-CN', w:2560, h:1440 },
  { name:'app-02-总览-暗夜-2K',     path:'/overview', theme:'midnight', lang:'zh-CN', w:2560, h:1440 },
  { name:'app-03-总览-商务金-2K',   path:'/overview', theme:'gold',     lang:'zh-CN', w:2560, h:1440 },
  { name:'app-04-租户管理-2K',      path:'/tenants',  theme:'tech',     lang:'zh-CN', w:2560, h:1440 },
  { name:'app-05-总览-英文-2K',     path:'/overview', theme:'tech',     lang:'en-US', w:2560, h:1440 },
  { name:'app-06-总览-4K',          path:'/overview', theme:'tech',     lang:'zh-CN', w:3840, h:2160 },
]

const browser = await chromium.launch()
for (const s of shots){
  const ctx = await browser.newContext({ viewport:{ width:s.w, height:s.h }, deviceScaleFactor:1 })
  // 预置 localStorage 以应用主题/语言
  await ctx.addInitScript(([t,l]) => {
    localStorage.setItem('codeman.theme', t)
    localStorage.setItem('codeman.locale', l)
  }, [s.theme, s.lang])
  const page = await ctx.newPage()
  await page.goto(base + s.path, { waitUntil:'networkidle' })
  await page.waitForTimeout(600)
  await page.screenshot({ path: resolve(__dir,'shots',s.name+'.png') })
  console.log('shot:', s.name)
  await ctx.close()
}

// 开通租户对话框（点击开通按钮）
{
  const ctx = await browser.newContext({ viewport:{ width:2560, height:1440 }, deviceScaleFactor:1 })
  await ctx.addInitScript(() => { localStorage.setItem('codeman.theme','tech'); localStorage.setItem('codeman.locale','zh-CN') })
  const page = await ctx.newPage()
  await page.goto(base + '/tenants', { waitUntil:'networkidle' })
  await page.getByText('开通租户', { exact:false }).first().click()
  await page.waitForTimeout(700)
  await page.screenshot({ path: resolve(__dir,'shots','app-07-开通租户与邮件-2K.png') })
  console.log('shot: app-07-开通租户与邮件-2K')
  await ctx.close()
}
await browser.close()
console.log('done')
