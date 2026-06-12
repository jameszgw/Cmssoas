#!/usr/bin/env node
/**
 * 在线浮动席位全链路演示(Node ≥ 20):
 *   BASE_URL=http://localhost:8080 ADMIN_USER=admin ADMIN_PASS=8888 node online-demo.mjs
 * 流程:签发 ONLINE 模式 CmPrint License(并发 2)→ 实例 A/B 激活占满 → C 激活被拒(席位满)
 *      → 心跳 OK(验响应签名)→ B 释放 → C 立即可激活 → 吊销 → 心跳即时 REVOKED。
 */
import { createOnlineClient } from './cmprint-online-client.mjs';

const BASE = process.env.BASE_URL || 'http://localhost:8080';
const USER = process.env.ADMIN_USER || 'admin';
const PASS = process.env.ADMIN_PASS || '8888';

const j = (r) => r.json();
const today = new Date().toISOString().slice(0, 10);
const nextYear = new Date(Date.now() + 365 * 864e5).toISOString().slice(0, 10);

const { token } = await fetch(`${BASE}/api/auth/login`, {
  method: 'POST', headers: { 'Content-Type': 'application/json' },
  body: JSON.stringify({ username: USER, password: PASS }),
}).then(j);
const AUTH = { Authorization: `Bearer ${token}`, 'Content-Type': 'application/json' };

console.log('== 1) 签发 ONLINE 模式 CmPrint License(并发席位 2)==');
const lic = await fetch(`${BASE}/api/cmprint/licenses/issue`, {
  method: 'POST', headers: AUTH,
  body: JSON.stringify({
    tenantCode: 'T-ONLINE-DEMO', customer: '在线席位演示客户', edition: 'ENTERPRISE',
    mode: 'ONLINE', concurrency: 2, notBefore: today, notAfter: nextYear,
  }),
}).then(j);
console.log(`  ${lic.licenseId} · 并发 ${lic.concurrency}`);
const pub = (await fetch(`${BASE}/api/licenses/public-key`, { headers: AUTH }).then(j)).publicKeyBase64;

const log = (tag) => (ev) => console.log(`  [${tag}] ${ev.type}${ev.seatsUsed != null ? ` 席位 ${ev.seatsUsed}/${ev.seatsTotal}` : ''}${ev.message ? ` · ${ev.message}` : ''}`);
const mk = (tag) => createOnlineClient({
  baseUrl: BASE, licenseId: lic.licenseId, instanceId: `demo-${tag}`,
  publicKeyBase64: pub, onStatus: log(tag), autoRelease: false,
});

console.log('== 2) A/B 激活占满 2 席;C 激活应被拒 ==');
const A = mk('A'); const B = mk('B'); const C = mk('C');
await A.activate();
await B.activate();
try {
  await C.activate();
  throw new Error('断言失败:C 不应激活成功');
} catch (e) {
  if (e.status !== 'SEAT_FULL') throw e;
  console.log(`  ✅ C 被拒:${e.message}`);
}

console.log('== 3) 心跳 OK(Ed25519 响应签名已验)==');
const hb = await A.heartbeat();
if (hb !== 'OK') throw new Error('断言失败:心跳应 OK,得到 ' + hb);

console.log('== 4) B 释放席位 → C 立即可激活 ==');
await B.stop();
await C.activate();
console.log('  ✅ C 已接替 B 的席位');

console.log('== 5) 吊销 License → 下一次心跳即时 REVOKED ==');
await fetch(`${BASE}/api/licenses/${lic.licenseId}/revoke`, {
  method: 'POST', headers: AUTH, body: JSON.stringify({ reason: '在线演示吊销' }),
}).then(j);
const st = await A.heartbeat();
if (st !== 'REVOKED') throw new Error('断言失败:应 REVOKED,得到 ' + st);
console.log(`  ✅ A 心跳被拒(${st}),客户端已停止循环 · active=${A.getState().active}`);

console.log('完成:激活/席位互斥/心跳验签/释放接替/吊销即时生效 全部通过。');
