/**
 * CmPrint 在线授权客户端(浏览器/Node ≥ 20,零依赖)—— 对接平台 /sdk 通道:
 * 激活占用浮动席位 → 周期心跳(nonce 防重放)→ 关闭释放席位;吊销/到期在下一次心跳即时生效。
 *
 * 用法(浏览器,与 resolveEdition 门禁配合):
 *   import { createOnlineClient } from './cmprint-online-client.mjs'
 *   const client = createOnlineClient({
 *     baseUrl: 'https://平台地址', licenseId: 'LIC-2026-0001',
 *     publicKeyBase64,                       // 可选:验证服务端响应签名(防伪造服务器)
 *     onStatus(ev) {                          // 'activated'|'ok'|'seat-full'|'revoked'|'expired'|
 *       if (ev.type === 'revoked') lockUi()   // 'not-activated'|'replay'|'network-error'|'grace-expired'|'deactivated'
 *     },
 *   })
 *   await client.start()                      // 激活 + 自动心跳;席位满抛错(err.status==='SEAT_FULL')
 *   ...
 *   client.stop()                             // 显式释放;页面关闭时自动 sendBeacon 反激活
 *
 * 实例标识:默认按 licenseId 持久于 localStorage(同一浏览器=一个席位,刷新不重复占座);
 * 需要「每标签页一座」传 instanceId: crypto.randomUUID()。machineCode 为持久化随机浏览器
 * 标识——浏览器拿不到真实硬件指纹,席位约束以服务端(并发数+心跳+宽限回收)为准。
 */
import { verifyEd25519 } from './cmprint-license.mjs';

function randomId() {
  if (globalThis.crypto && crypto.randomUUID) return crypto.randomUUID();
  return `i-${Date.now().toString(36)}-${Math.random().toString(36).slice(2, 10)}`;
}

function persisted(key, make) {
  try {
    const ls = globalThis.localStorage;
    if (!ls) return make();
    let v = ls.getItem(key);
    if (!v) { v = make(); ls.setItem(key, v); }
    return v;
  } catch (e) {
    return make(); // 隐私模式等不可用:退化为会话级随机
  }
}

export function createOnlineClient(options = {}) {
  const {
    baseUrl, licenseId, publicKeyBase64, onStatus,
    fetch: fetchImpl, heartbeatSec: hbOverride, autoRelease = true,
  } = options;
  if (!baseUrl || !licenseId) throw new Error('createOnlineClient: 需要 baseUrl 与 licenseId');
  const base = String(baseUrl).replace(/\/$/, '');
  const f = fetchImpl || (typeof fetch !== 'undefined' ? (...a) => fetch(...a) : null);
  if (!f) throw new Error('当前环境无 fetch');

  const instanceId = options.instanceId
    || persisted(`cmprint.online.instance.${licenseId}`, randomId);
  const machineCode = options.machineCode
    || persisted('cmprint.online.machine', () => `web-${randomId()}`);

  const state = {
    active: false, lastStatus: '', lastServerTime: '',
    seatsUsed: 0, seatsTotal: 0, heartbeatSec: 300, graceSec: 1800,
  };
  let timer = null;
  let offlineSince = 0; // 首次网络失败时间戳(进入宽限)
  let unloadHooked = false;

  function emit(type, detail) {
    if (typeof onStatus === 'function') {
      try { onStatus({ type, licenseId, instanceId, ...detail }); } catch (e) { /* 集成方回调异常不打断心跳 */ }
    }
  }

  async function post(path, body) {
    const res = await f(`${base}${path}`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(body),
    });
    let json = null;
    try { json = await res.json(); } catch (e) { /* 非 JSON 按错误处理 */ }
    return { http: res.status, body: json };
  }

  /** 验证服务端响应签名:Ed25519 over `licenseId|instanceId|serverTime|STATUS`。 */
  async function verifyReply(serverTime, status, signature) {
    if (!publicKeyBase64 || !signature) return true; // 未配公钥则跳过(信任 TLS)
    const payload = new TextEncoder().encode([licenseId, instanceId, serverTime, status].join('|'));
    const b64 = signature.replace(/-/g, '+').replace(/_/g, '/')
      .padEnd(Math.ceil(signature.length / 4) * 4, '=');
    const sig = Uint8Array.from(atob(b64), (c) => c.charCodeAt(0));
    return verifyEd25519(payload, sig, publicKeyBase64);
  }

  /** 激活占座。席位满抛 Error(err.status='SEAT_FULL')。 */
  async function activate() {
    const { http, body } = await post('/sdk/activate', { licenseId, instanceId, machineCode });
    if (http === 409) {
      const err = new Error((body && body.message) || '并发席位已满');
      err.status = 'SEAT_FULL';
      emit('seat-full', { message: err.message });
      throw err;
    }
    if (!body || body.ok !== true) {
      throw new Error((body && body.message) || `激活失败(HTTP ${http})`);
    }
    if (!(await verifyReply(body.serverTime, 'ACTIVATED', body.signature))) {
      throw new Error('激活响应签名无效(疑似伪造服务端)');
    }
    state.active = true;
    state.lastStatus = 'ACTIVATED';
    state.lastServerTime = body.serverTime;
    state.seatsUsed = body.seatsUsed;
    state.seatsTotal = body.seatsTotal;
    if (body.heartbeatSec) state.heartbeatSec = body.heartbeatSec;
    if (body.graceSec) state.graceSec = body.graceSec;
    offlineSince = 0;
    emit('activated', { seatsUsed: body.seatsUsed, seatsTotal: body.seatsTotal, authSnapshot: body.authSnapshot });
    return body;
  }

  /** 单次心跳。返回服务端状态串;终态(REVOKED/EXPIRED/NOT_FOUND)会停止循环。 */
  async function heartbeat() {
    let http; let body;
    try {
      ({ http, body } = await post('/sdk/heartbeat', { licenseId, instanceId, nonce: randomId(), ts: Date.now() }));
    } catch (e) {
      // 网络故障:进入宽限——服务端按 graceSec 容忍;超宽限席位被回收,恢复后自动重激活
      if (!offlineSince) offlineSince = Date.now();
      const offSec = (Date.now() - offlineSince) / 1000;
      emit(offSec > state.graceSec ? 'grace-expired' : 'network-error', { offlineSec: Math.round(offSec) });
      return 'NETWORK_ERROR';
    }
    offlineSince = 0;
    const status = (body && body.status) || `HTTP_${http}`;
    state.lastStatus = status;
    if (body) {
      state.lastServerTime = body.serverTime || state.lastServerTime;
      state.seatsUsed = body.seatsUsed ?? state.seatsUsed;
      state.seatsTotal = body.seatsTotal ?? state.seatsTotal;
      if (body.signature && !(await verifyReply(body.serverTime, status, body.signature))) {
        emit('network-error', { message: '心跳响应签名无效' });
        return 'BAD_SIGNATURE';
      }
    }
    if (status === 'OK') { emit('ok', { seatsUsed: state.seatsUsed, seatsTotal: state.seatsTotal }); return status; }
    if (status === 'REPLAY') { emit('replay', {}); return status; } // 下次心跳换 nonce 自然恢复
    if (status === 'NOT_ACTIVATED') {
      // 席位被宽限回收(如长时间断网后恢复):自动重激活续座
      emit('not-activated', {});
      try { await activate(); } catch (e) { /* 席位可能已被占满,保持循环重试 */ }
      return status;
    }
    // 终态:吊销/过期/不存在 → 停止循环,集成方据此锁功能
    state.active = false;
    stopTimer();
    emit(status === 'REVOKED' ? 'revoked' : status === 'EXPIRED' ? 'expired' : 'not-activated',
      { message: body && body.message });
    return status;
  }

  function stopTimer() {
    if (timer) { clearInterval(timer); timer = null; }
  }

  function hookUnload() {
    if (unloadHooked || !autoRelease || typeof window === 'undefined') return;
    unloadHooked = true;
    window.addEventListener('pagehide', () => {
      // sendBeacon:页面关闭时尽力释放席位(失败也无妨,服务端宽限期后自动回收)
      try {
        const blob = new Blob([JSON.stringify({ licenseId, instanceId })], { type: 'application/json' });
        navigator.sendBeacon(`${base}/sdk/deactivate`, blob);
      } catch (e) { /* ignore */ }
    });
  }

  /** 激活 + 启动自动心跳循环。 */
  async function start() {
    const r = await activate();
    stopTimer();
    const period = Math.max(5, hbOverride || state.heartbeatSec) * 1000;
    timer = setInterval(() => { heartbeat(); }, period);
    hookUnload();
    return r;
  }

  /** 停止心跳并释放席位。 */
  async function stop() {
    stopTimer();
    if (!state.active) return;
    state.active = false;
    try { await post('/sdk/deactivate', { licenseId, instanceId }); } catch (e) { /* 服务端宽限回收兜底 */ }
    emit('deactivated', {});
  }

  return {
    instanceId,
    machineCode,
    activate,
    heartbeat,
    start,
    stop,
    deactivate: stop,
    getState() { return { ...state }; },
  };
}

export default { createOnlineClient };
