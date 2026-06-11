#!/usr/bin/env node
/**
 * CmPrint License 本地验签演示(Node ≥ 20):
 *   node verify-demo.mjs <license.lic 路径> <公钥Base64(SPKI)>
 * 公钥取自平台 GET /api/licenses/public-key 的 publicKeyBase64。
 *
 * 输出:验签结果、claims 摘要、以及与 CmPrint resolveEdition 同语义解析出的全键能力表
 * (真实集成请直接使用 cmprint 包导出的 resolveEdition;此处内联实现仅为脱离前端包演示)。
 */
import { readFileSync } from 'node:fs'
import { loadCmprintLicense } from './cmprint-license.mjs'

// 与 cmprint/src/core/capabilities.js 的 CAPABILITY_KEYS 一致(演示用)
const CAPABILITY_KEYS = [
  'language', 'paperSelect', 'customPaper', 'pageMargin', 'theme',
  'dataSource', 'editJson', 'export',
  'exportPdf', 'exportPdfText', 'sharePdf', 'exportWord', 'exportExcel', 'exportImage', 'exportJson',
  'subTemplate', 'templateGallery', 'save',
  'zoom', 'rotate', 'grid', 'preview', 'print', 'directPrint',
  'pageNumber', 'watermark', 'overprint', 'calibration',
  'fontControls', 'align', 'undoRedo', 'history', 'shortcuts', 'layers', 'deleteEl',
  'palette', 'propertyPanel',
]
const EDITIONS = {
  community: Object.fromEntries(['exportPdf', 'exportPdfText', 'sharePdf', 'exportWord', 'exportExcel',
    'exportImage', 'directPrint', 'overprint', 'calibration', 'watermark',
    'subTemplate', 'templateGallery', 'theme'].map((k) => [k, false])),
  professional: { directPrint: false, templateGallery: false },
  enterprise: {},
}
function resolveEdition(edition, overrides) {
  const preset = EDITIONS[edition] || EDITIONS.enterprise
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
  const { claims, edition, overrides } = await loadCmprintLicense(lic, pubKey)
  console.log('✅ 验签通过 · License 有效')
  console.log(`   licenseId : ${claims.licenseId} (v${claims.licenseVersion})`)
  console.log(`   customer  : ${claims.customer} / ${claims.tenantCode}`)
  console.log(`   edition   : ${claims.edition} · ${claims.notBefore} ~ ${claims.notAfter} · ${claims.appVersionRange}`)
  console.log(`   overrides : ${JSON.stringify(overrides)}`)
  const caps = resolveEdition(edition, overrides)
  console.log('   capabilities(resolveEdition 解析结果):')
  const off = CAPABILITY_KEYS.filter((k) => !caps[k])
  console.log(`     开启 ${CAPABILITY_KEYS.length - off.length}/${CAPABILITY_KEYS.length} 键;关闭:${off.length ? off.join(', ') : '(无 — 全功能)'}`)
} catch (e) {
  console.error('❌ ' + e.message)
  process.exit(1)
}
