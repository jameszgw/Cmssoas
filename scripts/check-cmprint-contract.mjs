#!/usr/bin/env node
/**
 * CmPrint 契约一致性校验 —— 防双侧漂移(CI 强制):
 * 解析 server/.../cmprint/CmprintEditions.java 的能力键与三档预设,
 * 与契约文件 examples/cmprint-integration/cmprint-contract.json 逐键比对。
 * cmprint 仓库以 test/cmprint-contract.test.js 对其 docs/cmprint-contract.json 做同样校验;
 * 两边契约文件须保持字节一致(改 CAPABILITY_KEYS/EDITIONS 时同步两个 JSON + 双侧实现)。
 */
import { readFileSync } from 'node:fs'
import { createHash } from 'node:crypto'
import path from 'node:path'
import { fileURLToPath } from 'node:url'

const ROOT = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..')
const JAVA = path.join(ROOT, 'server/license-platform/src/main/java/com/codeman/platform/cmprint/CmprintEditions.java')
const CONTRACT = path.join(ROOT, 'examples/cmprint-integration/cmprint-contract.json')

const contract = JSON.parse(readFileSync(CONTRACT, 'utf8'))
const java = readFileSync(JAVA, 'utf8')

// —— 解析 CAPABILITY_KEYS:List.of( "a", "b", ... ) ——
const keysBlock = java.match(/CAPABILITY_KEYS\s*=\s*List\.of\(([\s\S]*?)\);/)
if (!keysBlock) fail('未找到 CAPABILITY_KEYS')
const javaKeys = [...keysBlock[1].matchAll(/"([\w]+)"/g)].map((m) => m[1])

// —— 解析 COMMUNITY 预设(false 键列表) ——
const commBlock = java.match(/Map<String, Boolean> community[\s\S]*?List\.of\(([\s\S]*?)\)\)\s*\{/)
if (!commBlock) fail('未找到 COMMUNITY 预设')
const javaCommunity = [...commBlock[1].matchAll(/"([\w]+)"/g)].map((m) => m[1])

// —— 解析 PROFESSIONAL 预设:professional.put("key", false) ——
const javaProfessional = [...java.matchAll(/professional\.put\("([\w]+)",\s*false\)/g)].map((m) => m[1])

let errors = []
function eq(label, a, b) {
  const aj = JSON.stringify(a)
  const bj = JSON.stringify(b)
  if (aj !== bj) errors.push(`${label} 不一致:\n  Java     : ${aj}\n  contract : ${bj}`)
}

eq('能力键(顺序敏感)', javaKeys, contract.capabilityKeys)
eq('COMMUNITY 关闭键', javaCommunity.sort(), Object.keys(contract.editions.COMMUNITY).sort())
eq('PROFESSIONAL 关闭键', javaProfessional.sort(), Object.keys(contract.editions.PROFESSIONAL).sort())
if (Object.keys(contract.editions.ENTERPRISE).length !== 0) errors.push('ENTERPRISE 预设应为空(全开)')
const wrongVal = [...Object.values(contract.editions.COMMUNITY), ...Object.values(contract.editions.PROFESSIONAL)]
  .some((v) => v !== false)
if (wrongVal) errors.push('预设键的值必须全为 false(相对全开关闭)')

const hash = createHash('sha256').update(readFileSync(CONTRACT)).digest('hex').slice(0, 16)
if (errors.length) fail(errors.join('\n'))
console.log(`✅ CmPrint 契约一致(${javaKeys.length} 能力键;COMMUNITY 关 ${javaCommunity.length} 键 / PROFESSIONAL 关 ${javaProfessional.length} 键)`)
console.log(`   contract sha256[:16]=${hash} —— 应与 cmprint 仓库 docs/cmprint-contract.json 相同`)

function fail(msg) {
  console.error('❌ CmPrint 契约校验失败:\n' + msg)
  process.exit(1)
}
