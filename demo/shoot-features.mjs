// 无服务器截图:Playwright 直接从 dist 提供前端 + 把 /api、/pub 代理到后端 :8080。
// 用于"须知与授权 / 智能客服 / 合同签约"三项新功能的功能截图。
import { chromium } from 'playwright'
import { resolve, dirname, extname } from 'path'
import { fileURLToPath } from 'url'
import { readFile } from 'fs/promises'

const __dir = dirname(fileURLToPath(import.meta.url))
const DIST = resolve(__dir, '../web/console/dist')
const SHOTS = resolve(__dir, '../web/console/shots')
const BACKEND = 'http://localhost:8080'
const ORIGIN = 'http://localhost:4173'

const MIME = { '.js': 'text/javascript', '.css': 'text/css', '.html': 'text/html',
  '.svg': 'image/svg+xml', '.png': 'image/png', '.json': 'application/json',
  '.woff': 'font/woff', '.woff2': 'font/woff2', '.ico': 'image/x-icon' }

async function login() {
  const r = await fetch(`${BACKEND}/api/auth/login`, {
    method: 'POST', headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ username: 'admin', password: '8888' }),
  })
  return (await r.json()).token
}

const TOKEN = await login()
console.log('token len', TOKEN.length)

const browser = await chromium.launch()

async function makeContext(theme = 'tech', lang = 'zh-CN', w = 2560, h = 1440) {
  const ctx = await browser.newContext({ viewport: { width: w, height: h }, deviceScaleFactor: 1 })
  await ctx.addInitScript(([t, l, tok]) => {
    localStorage.setItem('codeman.theme', t)
    localStorage.setItem('codeman.locale', l)
    localStorage.setItem('codeman.token', tok)
  }, [theme, lang, TOKEN])
  // 路由:API/PUB 代理到后端,其余从 dist 提供(SPA 回退到 index.html)
  await ctx.route('**/*', async (route) => {
    const url = new URL(route.request().url())
    if (url.origin !== ORIGIN) { await route.continue(); return }
    const p = url.pathname
    if (p.startsWith('/api') || p.startsWith('/pub')) {
      const req = route.request()
      const headers = { accept: req.headers()['accept'] || '*/*' }
      const a = req.headers()['authorization']; if (a) headers['authorization'] = a
      const ct = req.headers()['content-type']; if (ct) headers['content-type'] = ct
      try {
        const resp = await fetch(BACKEND + p + url.search, {
          method: req.method(), headers, body: req.postData() || undefined,
        })
        const buf = Buffer.from(await resp.arrayBuffer())
        await route.fulfill({ status: resp.status, body: buf,
          contentType: resp.headers.get('content-type') || 'application/json' })
      } catch (e) { await route.fulfill({ status: 502, body: String(e) }) }
      return
    }
    // 静态资源 or SPA 回退
    let file = p === '/' ? '/index.html' : p
    let ext = extname(file)
    try {
      const data = await readFile(DIST + file)
      await route.fulfill({ status: 200, body: data, contentType: MIME[ext] || 'application/octet-stream' })
    } catch {
      const html = await readFile(DIST + '/index.html')
      await route.fulfill({ status: 200, body: html, contentType: 'text/html' })
    }
  })
  return ctx
}

async function shot(page, name) {
  await page.screenshot({ path: resolve(SHOTS, name + '.png') })
  console.log('shot:', name)
}

// 通过页面上下文调用已注入鉴权的 fetch(借用浏览器,自动带 token 由路由转发)
async function api(page, method, path, body) {
  return page.evaluate(async ([m, p, b, tok]) => {
    const r = await fetch(p, { method: m,
      headers: { 'Content-Type': 'application/json', Authorization: 'Bearer ' + tok },
      body: b ? JSON.stringify(b) : undefined })
    return r.ok ? r.json().catch(() => ({})) : Promise.reject(r.status)
  }, [method, path, body, TOKEN])
}

const ctx = await makeContext()
const page = await ctx.newPage()
await page.goto(ORIGIN + '/overview', { waitUntil: 'networkidle' })
await page.waitForTimeout(500)

// ---- 种子数据 ----
// 1) 强制确认须知(用于 NoticeGate) + 隐私政策(非强制)
const gate = await api(page, 'POST', '/api/notices', { type: 'TERMS', title: '服务条款与用户须知', contentHtml: '<p>欢迎使用 CODEMAN 软件授权运营平台。使用本平台即表示您同意以下条款：</p><p>1. 您应妥善保管账号与密钥；</p><p>2. 平台数据按多租户隔离存储；</p><p>3. 同意记录将留存用于合规取证。</p>', forceAck: true })
await api(page, 'POST', `/api/notices/${gate.id}/publish`, {})
const priv = await api(page, 'POST', '/api/notices', { type: 'PRIVACY', title: '隐私政策', contentHtml: '<p>我们仅收集为提供服务所必需的信息……</p>', forceAck: false })
await api(page, 'POST', `/api/notices/${priv.id}/publish`, {})
// 2) 合同(已签署一份 + 待签一份)
const ct1 = await api(page, 'POST', '/api/contracts', { customer: '北京云科信息技术有限公司', planCode: 'ENTERPRISE', amount: 88000, contentHtml: '<p>甲乙双方就 CODEMAN 企业版软件授权服务达成如下协议：</p><p>一、授权范围……</p><p>二、服务期限：壹年……</p><p>三、合同金额：人民币 88,000 元整。</p>', parties: [{ name: '北京云科信息技术有限公司', partyRole: '甲方' }, { name: 'CODEMAN 运营方', partyRole: '乙方' }] })
await api(page, 'POST', `/api/contracts/${ct1.id}/send`, {})
const d1 = await api(page, 'GET', `/api/contracts/${ct1.id}`)
await api(page, 'POST', `/api/contracts/${ct1.id}/sign/${d1.parties[0].id}`, {})
await api(page, 'POST', `/api/contracts/${ct1.id}/sign/${d1.parties[1].id}`, {})
const ct2 = await api(page, 'POST', '/api/contracts', { customer: '上海某智能制造股份有限公司', planCode: 'PROFESSIONAL', amount: 36000, contentHtml: '<p>专业版授权服务合同……</p>', parties: [{ name: '上海某智能制造股份有限公司', partyRole: '甲方' }, { name: 'CODEMAN 运营方', partyRole: '乙方' }] })
await api(page, 'POST', `/api/contracts/${ct2.id}/send`, {})

// ---- 截图 1:强制须知确认门(NoticeGate) ----
await page.goto(ORIGIN + '/overview', { waitUntil: 'networkidle' })
await page.waitForTimeout(900)
await shot(page, 'feat-01-须知强制确认门')

// 确认须知,解除阻断
await api(page, 'POST', `/api/notices/${gate.id}/ack`, {})
await page.waitForTimeout(300)

// ---- 截图 2:须知管理 ----
await page.goto(ORIGIN + '/notices', { waitUntil: 'networkidle' })
await page.waitForTimeout(800)
await shot(page, 'feat-02-须知管理')

// ---- 截图 3:授权记录(同意 tab) ----
await page.getByText('授权记录', { exact: false }).first().click().catch(() => {})
await page.waitForTimeout(600)
await shot(page, 'feat-03-授权记录')

// ---- 截图 4:智能客服会话审计 ----
await page.goto(ORIGIN + '/cs', { waitUntil: 'networkidle' })
await page.waitForTimeout(800)
await shot(page, 'feat-04-智能客服会话审计')

// ---- 截图 5:智能客服悬浮窗(真实流式回复) ----
await page.goto(ORIGIN + '/overview', { waitUntil: 'networkidle' })
await page.waitForTimeout(500)
await page.locator('.cs-fab').click()
await page.waitForTimeout(500)
await page.locator('.cs-input textarea').fill('License 授权是怎么签发的？')
await page.locator('.cs-input button').click()
await page.waitForTimeout(2500) // 等流式回复
await shot(page, 'feat-05-智能客服流式对话')

// ---- 截图 6:合同列表 ----
await page.goto(ORIGIN + '/contracts', { waitUntil: 'networkidle' })
await page.waitForTimeout(800)
await shot(page, 'feat-06-合同列表')

// ---- 截图 7:合同详情(签署存证抽屉) ----
await page.locator('.el-table__row').first().click()
await page.waitForTimeout(800)
await shot(page, 'feat-07-合同详情存证')

// ---- 截图 8:英文 + 暗夜主题(须知管理) ----
await ctx.close()
const ctxEn = await makeContext('midnight', 'en-US')
const pageEn = await ctxEn.newPage()
await pageEn.goto(ORIGIN + '/notices', { waitUntil: 'networkidle' })
await pageEn.waitForTimeout(800)
await shot(pageEn, 'feat-08-须知管理-暗夜-英文')
await ctxEn.close()

await browser.close()
console.log('done')
