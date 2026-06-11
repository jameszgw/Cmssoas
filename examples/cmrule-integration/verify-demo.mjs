#!/usr/bin/env node
/**
 * CmRuleEngine License 本地验签演示(Node ≥ 20):
 *   node verify-demo.mjs <license.lic 路径> <公钥Base64(SPKI)>
 * 公钥取自平台 GET /api/licenses/public-key 的 publicKeyBase64。
 *
 * 输出:验签结果、claims 摘要、以及与 CmRuleEngine rule-engine-server/src/capabilities.js
 * 的 resolve 同语义解析出的全键能力表(真实集成请直接配置 RE_LICENSE/RE_LICENSE_PUBKEY
 * 让 rule-engine-server 自行验签;此处内联实现仅为脱离引擎包演示)。
 */
import { readFileSync } from 'node:fs'
import { loadCmruleLicense } from './cmrule-license.mjs'

// 与 rule-engine-server/src/capabilities.js 的 CAPABILITY_KEYS 一致(演示用,19 键)
const CAPABILITY_KEYS = [
  'ruleChainCore', 'zeroCodeWizard', 'lintCheck', 'nodeSearch',
  'templateLibrary', 'decisionTable', 'ruleFlow', 'edgeChains', 'versionHistory',
  'orchestrationBasic', 'auditQuery',
  'versionDiff', 'auditExport', 'orchestrationAdvanced', 'dbDialects', 'haCluster',
  'aiRuleGen', 'distributedTx', 'xinChuang',
]
const PRESETS = {
  COMMUNITY: Object.fromEntries(['templateLibrary', 'decisionTable', 'ruleFlow', 'edgeChains',
    'versionHistory', 'orchestrationBasic', 'auditQuery', 'versionDiff', 'auditExport',
    'orchestrationAdvanced', 'dbDialects', 'haCluster', 'aiRuleGen', 'distributedTx', 'xinChuang']
    .map((k) => [k, false])),
  PROFESSIONAL: Object.fromEntries(['versionDiff', 'auditExport', 'orchestrationAdvanced',
    'dbDialects', 'haCluster', 'aiRuleGen', 'distributedTx', 'xinChuang'].map((k) => [k, false])),
  ENTERPRISE: Object.fromEntries(['aiRuleGen', 'distributedTx', 'xinChuang'].map((k) => [k, false])),
  ULTIMATE: {},
}
function resolve(edition, overrides) {
  const preset = PRESETS[edition] || PRESETS.ULTIMATE
  const merged = { ...preset, ...(overrides || {}) }
  const out = {}
  CAPABILITY_KEYS.forEach((k) => { out[k] = true })
  Object.keys(merged).forEach((k) => { if (k in out) out[k] = merged[k] !== false })
  return out
}

const [, , licPath, pubKey] = process.argv
if (!licPath || !pubKey) {
  console.error('用法: node verify-demo.mjs <license.lic> <公钥Base64(SPKI)>')
  process.exit(2)
}

try {
  const lic = readFileSync(licPath, 'utf8')
  const { claims, edition, overrides } = await loadCmruleLicense(lic, pubKey)
  console.log('✅ 验签通过 · License 有效')
  console.log(`   licenseId : ${claims.licenseId} (v${claims.licenseVersion})`)
  console.log(`   customer  : ${claims.customer} / ${claims.tenantCode}`)
  console.log(`   edition   : ${claims.edition} · ${claims.notBefore} ~ ${claims.notAfter} · ${claims.appVersionRange}`)
  console.log(`   overrides : ${JSON.stringify(overrides)}`)
  const caps = resolve(edition, overrides)
  console.log('   capabilities(resolve 解析结果):')
  const off = CAPABILITY_KEYS.filter((k) => !caps[k])
  console.log(`     开启 ${CAPABILITY_KEYS.length - off.length}/${CAPABILITY_KEYS.length} 键;关闭:${off.length ? off.join(', ') : '(无 — 全功能)'}`)
  console.log('   rule-engine-server 配置:RE_LICENSE=' + licPath + ' RE_LICENSE_PUBKEY=<公钥Base64>')
} catch (e) {
  console.error('❌ ' + e.message)
  process.exit(1)
}
