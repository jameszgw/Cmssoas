// 将仓库 web/landing/ 的官宣页与帮助中心、以及 docs/*.html 文档中心同步到本前端的 public/,
// 使其随控制台一起发布:
//   /help.html · /help-en.html · /landing.html · /docs/index.html · /docs/<doc>.html
// 单一事实来源仍是 web/landing/* 与 docs/*(由 scripts/build-docs.mjs 生成);本脚本仅"拷贝 + 链接重写",
// 在 predev/prebuild 自动执行。public 生成物已被 .gitignore。
import { readFileSync, writeFileSync, existsSync, mkdirSync, readdirSync } from 'node:fs'
import { dirname, resolve } from 'node:path'
import { fileURLToPath } from 'node:url'

const here = dirname(fileURLToPath(import.meta.url))
const landing = resolve(here, '../../landing')
const docsDir = resolve(here, '../../../docs')
const pub = resolve(here, '../public')
const pubDocs = resolve(pub, 'docs')
const REPO = 'https://github.com/jameszgw/cmssoas/blob/main'
const RAW = 'https://github.com/jameszgw/cmssoas/raw/main'

if (!existsSync(pub)) mkdirSync(pub, { recursive: true })
if (!existsSync(pubDocs)) mkdirSync(pubDocs, { recursive: true })

// ---- 官宣页 / 帮助中心(位于 public 根)----
// 文档链接(已是 .html)指向同样发布的 /docs/*;部署目录/README 无 HTML → GitHub;截图 → GitHub raw。
function rewriteLanding(html) {
  return html
    .replaceAll('../../docs/', 'docs/')
    .replaceAll('../../deploy/', `${REPO}/deploy/`)
    .replaceAll('href="../../README.md"', `href="${REPO}/README.md"`)
    .replaceAll('../console/shots/', `${RAW}/web/console/shots/`)
}

const landingJobs = [
  { src: 'help.html', out: 'help.html', extra: (h) => h.replaceAll('href="index.html"', 'href="landing.html"') },
  { src: 'help-en.html', out: 'help-en.html', extra: (h) => h.replaceAll('href="index.html"', 'href="landing.html"') },
  { src: 'index.html', out: 'landing.html', extra: (h) => h },
]

let n = 0
for (const j of landingJobs) {
  const sp = resolve(landing, j.src)
  if (!existsSync(sp)) { console.warn(`[sync-help] skip missing ${j.src}`); continue }
  writeFileSync(resolve(pub, j.out), j.extra(rewriteLanding(readFileSync(sp, 'utf8'))))
  n++
}

// ---- 文档中心 docs/*.html(发布到 public/docs/)----
// 相对路径在 /docs/ 下重写:帮助中心/官宣页在上一级;同级 doc 互链与 index.html 保持原样;
// 源 md / deploy / 其它 web 路径 → GitHub;功能截图 → GitHub raw。
function rewriteDoc(html) {
  return html
    .replaceAll('../web/landing/help.html', '../help.html')
    .replaceAll('../web/landing/index.html', '../landing.html')
    .replaceAll('../web/console/shots/', `${RAW}/web/console/shots/`)
    .replaceAll('../web/', `${REPO}/web/`)
    .replaceAll('../deploy/', `${REPO}/deploy/`)
    .replaceAll('href="../README.md"', `href="${REPO}/README.md"`)
    // 顶栏「源文件(md)」:同目录裸文件名 *.md → GitHub blob
    .replace(/href="([^"/]+\.md)"/g, (_, f) => `href="${REPO}/docs/${f}"`)
}

let d = 0
if (existsSync(docsDir)) {
  for (const f of readdirSync(docsDir).filter((x) => x.endsWith('.html'))) {
    writeFileSync(resolve(pubDocs, f), rewriteDoc(readFileSync(resolve(docsDir, f), 'utf8')))
    d++
  }
}

console.log(`[sync-help] synced ${n} page(s) to public/ + ${d} doc(s) to public/docs/`)
