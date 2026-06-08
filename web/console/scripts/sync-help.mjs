// 将仓库 web/landing/ 下的官宣页与帮助中心同步到本前端的 public/,
// 使其随控制台一起发布(运行时可经 /help.html、/help-en.html、/landing.html 访问)。
// 单一事实来源仍是 web/landing/*;本脚本仅做"拷贝 + 链接重写",在 predev/prebuild 自动执行。
import { readFileSync, writeFileSync, existsSync, mkdirSync } from 'node:fs'
import { dirname, resolve } from 'node:path'
import { fileURLToPath } from 'node:url'

const here = dirname(fileURLToPath(import.meta.url))
const landing = resolve(here, '../../landing')
const pub = resolve(here, '../public')
const REPO = 'https://github.com/jameszgw/cmssoas/blob/main'
const RAW = 'https://github.com/jameszgw/cmssoas/raw/main'

if (!existsSync(pub)) mkdirSync(pub, { recursive: true })

// 把相对仓库根的链接改写为 GitHub 链接(应用运行时不提供仓库 md/部署目录/截图)
function rewrite(html) {
  return html
    .replaceAll('../../docs/', `${REPO}/docs/`)
    .replaceAll('../../deploy/', `${REPO}/deploy/`)
    .replaceAll('href="../../README.md"', `href="${REPO}/README.md"`)
    // 官宣页功能截图:相对 web/console/shots → GitHub raw,使在应用内访问 landing.html 也能显示
    .replaceAll('../console/shots/', `${RAW}/web/console/shots/`)
}

const jobs = [
  // help.html / help-en.html:站内互链 index.html → landing.html
  { src: 'help.html', out: 'help.html', extra: (h) => h.replaceAll('href="index.html"', 'href="landing.html"') },
  { src: 'help-en.html', out: 'help-en.html', extra: (h) => h.replaceAll('href="index.html"', 'href="landing.html"') },
  // 官宣页:其内部 help.html 链接保持不变(同样发布在 public 根)
  { src: 'index.html', out: 'landing.html', extra: (h) => h },
]

let done = 0
for (const j of jobs) {
  const sp = resolve(landing, j.src)
  if (!existsSync(sp)) { console.warn(`[sync-help] skip missing ${j.src}`); continue }
  const html = j.extra(rewrite(readFileSync(sp, 'utf8')))
  writeFileSync(resolve(pub, j.out), html)
  done++
}
console.log(`[sync-help] synced ${done} file(s) to public/`)
