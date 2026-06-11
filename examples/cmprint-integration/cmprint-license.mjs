/**
 * CmPrint × CODEMAN License 集成适配器(零依赖,浏览器 / Node ≥ 20 通用)。
 *
 * 职责:解析并验签 CODEMAN 签发的 .lic(base64url(claims JSON).base64url(Ed25519 签名)),
 * 校验产品/有效期/状态,再把 claims 映射为 CmPrint 的能力解析入参:
 *
 *   import { resolveEdition } from 'cmprint'
 *   import { loadCmprintLicense } from './cmprint-license.mjs'
 *
 *   const { edition, overrides } = await loadCmprintLicense(licText, publicKeyBase64)
 *   const capabilities = resolveEdition(edition, overrides)   // ← 喂给 <cmprint-designer :capabilities>
 *
 * 契约:claims.edition ∈ COMMUNITY/PROFESSIONAL/ENTERPRISE(传给 resolveEdition 前转小写);
 * claims.features 是签发时固化的「档位预设 ∪ 合同微调」能力表 —— 作为 overrides 显式传入,
 * 即使 CmPrint 侧 EDITIONS 预设日后调整,已签发 License 的能力门禁也不漂移。
 *
 * 验签:Ed25519(WebCrypto,公钥为平台 /api/licenses/public-key 返回的 X.509 SPKI base64)。
 * sigAlg=SM2 的 License 请改走平台在线验签(POST /api/licenses/verify)或 Java SDK(BouncyCastle)。
 */

const PRODUCT_CODE = 'CMPRINT'

function b64uToBytes(s) {
  const b64 = s.replace(/-/g, '+').replace(/_/g, '/').padEnd(Math.ceil(s.length / 4) * 4, '=')
  if (typeof Buffer !== 'undefined') return new Uint8Array(Buffer.from(b64, 'base64'))
  const bin = atob(b64)
  return Uint8Array.from(bin, (c) => c.charCodeAt(0))
}

function b64ToBytes(s) {
  if (typeof Buffer !== 'undefined') return new Uint8Array(Buffer.from(s, 'base64'))
  return Uint8Array.from(atob(s), (c) => c.charCodeAt(0))
}

/** 解析 .lic 文本 → { claims, payload, signature }(不验签)。 */
export function parseLic(licText) {
  const parts = String(licText).trim().split('.')
  if (parts.length !== 2) throw new Error('License 格式错误:应为 payload.signature 两段')
  const payload = b64uToBytes(parts[0])
  const signature = b64uToBytes(parts[1])
  const claims = JSON.parse(new TextDecoder().decode(payload))
  return { claims, payload, signature }
}

/** Ed25519 验签(WebCrypto)。publicKeyBase64 为 X.509 SPKI 的标准 base64。 */
export async function verifyEd25519(payload, signature, publicKeyBase64) {
  const subtle = globalThis.crypto?.subtle
  if (!subtle) throw new Error('当前环境无 WebCrypto(crypto.subtle),请改用平台在线验签或 Java SDK')
  const key = await subtle.importKey('spki', b64ToBytes(publicKeyBase64), { name: 'Ed25519' }, false, ['verify'])
  return subtle.verify({ name: 'Ed25519' }, key, signature, payload)
}

/** 业务校验:产品 / 状态 / 有效期窗口。返回 { ok, reason }。 */
export function validateClaims(claims, now = new Date()) {
  if (claims.productCode !== PRODUCT_CODE) {
    return { ok: false, reason: `非 CmPrint License(productCode=${claims.productCode})` }
  }
  if (claims.status && claims.status !== 'ACTIVE') {
    return { ok: false, reason: `License 状态为 ${claims.status}` }
  }
  const day = now.toISOString().slice(0, 10)
  if (claims.notBefore && day < claims.notBefore) return { ok: false, reason: `尚未生效(notBefore=${claims.notBefore})` }
  if (claims.notAfter && day > claims.notAfter) return { ok: false, reason: `已过期(notAfter=${claims.notAfter})` }
  return { ok: true, reason: '有效' }
}

/** claims → CmPrint resolveEdition 的入参。 */
export function toCapabilities(claims) {
  return {
    edition: String(claims.edition || 'community').toLowerCase(),
    overrides: claims.features || {},
  }
}

/**
 * 一站式:解析 → 验签 → 业务校验 → 映射。
 * @returns {Promise<{claims, edition, overrides}>} 失败抛 Error(含原因)。
 */
export async function loadCmprintLicense(licText, publicKeyBase64, { now } = {}) {
  const { claims, payload, signature } = parseLic(licText)
  if (claims.sigAlg && claims.sigAlg !== 'Ed25519') {
    throw new Error(`签名算法 ${claims.sigAlg} 不支持本地验签,请用平台在线验签(POST /api/licenses/verify)`)
  }
  if (!(await verifyEd25519(payload, signature, publicKeyBase64))) {
    throw new Error('签名无效(伪造或被篡改)')
  }
  const v = validateClaims(claims, now)
  if (!v.ok) throw new Error(v.reason)
  return { claims, ...toCapabilities(claims) }
}

/**
 * 可选:校验平台已签名 CRL(GET /pub/crl 的 signedCrl 形态)并检查吊销。
 * @param signedCrl /api/licenses/crl/signed 返回对象({payloadB64, signature, ...})
 */
export async function isRevoked(licenseId, signedCrl, publicKeyBase64) {
  const payload = b64uToBytes(signedCrl.payloadB64)
  const sig = b64uToBytes(signedCrl.signature)
  if (!(await verifyEd25519(payload, sig, publicKeyBase64))) throw new Error('CRL 签名无效')
  const body = JSON.parse(new TextDecoder().decode(payload))
  return (body.revoked || []).some((r) => r.licenseId === licenseId)
}
