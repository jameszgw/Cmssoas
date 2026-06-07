<script setup lang="ts">
import { ref, computed, onMounted, watch } from 'vue'
import { useRoute } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { ElMessage } from 'element-plus'
import QRCode from 'qrcode'
import ThemeSwitcher from '@/components/ThemeSwitcher.vue'
import LangSwitcher from '@/components/LangSwitcher.vue'
import { getActivationInfo, activate, type ActivationInfo } from '@/api/activation'

const route = useRoute()
const { t } = useI18n()
const token = String(route.params.token || '')

const loading = ref(true)
const info = ref<ActivationInfo | null>(null)
const submitting = ref(false)
const done = ref(false)

const password = ref('')
const confirm = ref('')
const mfaCode = ref('')
const qrDataUrl = ref('')

const valid = computed(() => info.value?.valid === true)

const strength = computed(() => {
  const p = password.value
  let s = 0
  if (p.length >= 8) s++
  if (/[A-Z]/.test(p) && /[a-z]/.test(p)) s++
  if (/\d/.test(p)) s++
  if (/[^A-Za-z0-9]/.test(p)) s++
  return s // 0..4
})
const strengthLabel = computed(() => [t('act.weak'), t('act.weak'), t('act.fair'), t('act.good'), t('act.strong')][strength.value])
const strengthColor = computed(() => ['var(--danger)', 'var(--danger)', 'var(--warning)', 'var(--brand)', 'var(--success)'][strength.value])

async function load() {
  loading.value = true
  try {
    info.value = await getActivationInfo(token)
    if (info.value?.mfaOtpauthUri) {
      qrDataUrl.value = await QRCode.toDataURL(info.value.mfaOtpauthUri, { width: 320, margin: 1 })
    }
  } catch (e: any) {
    info.value = { valid: false, message: t('act.loadFailed') } as ActivationInfo
  } finally {
    loading.value = false
  }
}

async function submit() {
  if (password.value.length < 8) { ElMessage.warning(t('act.pwdShort')); return }
  if (password.value !== confirm.value) { ElMessage.warning(t('act.pwdMismatch')); return }
  if (mfaCode.value && !/^\d{6}$/.test(mfaCode.value)) { ElMessage.warning(t('act.mfaFormat')); return }
  submitting.value = true
  try {
    await activate(token, { password: password.value, mfaCode: mfaCode.value || undefined })
    done.value = true
  } catch (e: any) {
    ElMessage.error(e?.response?.data?.message || t('act.failed'))
  } finally {
    submitting.value = false
  }
}

watch(() => route.params.token, load)
onMounted(load)
</script>

<template>
  <div class="auth">
    <header class="bar">
      <div class="brand"><span class="logo">C</span>CODEMAN</div>
      <div class="tools"><ThemeSwitcher /><LangSwitcher /></div>
    </header>

    <main class="center">
      <!-- 加载 -->
      <div v-if="loading" class="card pad"><div class="spin">⏳ {{ t('act.loading') }}</div></div>

      <!-- 链接无效/过期 -->
      <div v-else-if="!valid && !done" class="card pad bad">
        <div class="big">⚠️</div>
        <h2>{{ t('act.invalidTitle') }}</h2>
        <p class="muted">{{ info?.message }}</p>
        <p class="faint small">{{ t('act.invalidHint') }}</p>
      </div>

      <!-- 激活成功 -->
      <div v-else-if="done" class="card pad ok">
        <div class="big">✅</div>
        <h2>{{ t('act.doneTitle') }}</h2>
        <p class="muted">{{ t('act.doneDesc') }}</p>
        <a class="btn solid wide" href="/">{{ t('act.toLogin') }}</a>
      </div>

      <!-- 激活表单 -->
      <div v-else class="card">
        <div class="head">
          <div class="logo-lg">C</div>
          <h2>{{ t('act.title') }}</h2>
          <p class="muted">{{ t('act.subtitle') }}</p>
        </div>

        <div class="kv">
          <div class="l"><span>{{ t('mail.kt') }}</span><b>{{ info?.tenantName }}</b></div>
          <div class="l"><span>{{ t('mail.kc') }}</span><b class="data">{{ info?.tenantCode }}</b></div>
          <div class="l"><span>{{ t('th.email') }}</span><b class="data">{{ info?.email }}</b></div>
          <div class="l"><span>{{ t('mail.kp') }}</span><b class="data">{{ info?.version }}</b></div>
        </div>

        <!-- Step 1 设密 -->
        <div class="step">
          <div class="step-h"><span class="num">1</span>{{ t('act.step1') }}</div>
          <el-input v-model="password" type="password" show-password size="large" :placeholder="t('act.pwd')" />
          <div v-if="password" class="strength">
            <div class="track"><div class="fill" :style="{ width: (strength * 25) + '%', background: strengthColor }"></div></div>
            <span class="slabel" :style="{ color: strengthColor }">{{ strengthLabel }}</span>
          </div>
          <el-input v-model="confirm" type="password" show-password size="large" :placeholder="t('act.pwd2')" style="margin-top:.6rem" />
        </div>

        <!-- Step 2 MFA -->
        <div class="step">
          <div class="step-h"><span class="num">2</span>{{ t('act.step2') }} <span class="opt">{{ t('act.optional') }}</span></div>
          <div class="mfa">
            <img v-if="qrDataUrl" :src="qrDataUrl" class="qr" alt="MFA QR" />
            <div class="mfa-r">
              <p class="small muted">{{ t('act.mfaDesc') }}</p>
              <div class="secret"><span class="faint small">{{ t('act.secret') }}</span><code class="data">{{ info?.mfaSecret }}</code></div>
              <el-input v-model="mfaCode" size="large" maxlength="6" :placeholder="t('act.mfaCode')" class="mfa-input" />
            </div>
          </div>
        </div>

        <el-button type="primary" size="large" class="wide" :loading="submitting" @click="submit">
          {{ t('act.submit') }}
        </el-button>
        <p class="faint small center-t">{{ t('act.tip') }}</p>
      </div>
    </main>
  </div>
</template>

<style scoped>
.auth{min-height:100vh}
.bar{display:flex;align-items:center;justify-content:space-between;background:var(--topbar);color:#fff;
  padding:.8rem clamp(20px,3vw,48px)}
.bar .brand{display:flex;align-items:center;gap:.6rem;font-weight:800;font-size:1.1rem;letter-spacing:.4px}
.bar .logo{width:2rem;height:2rem;border-radius:8px;display:grid;place-items:center;background:rgba(255,255,255,.18);font-weight:900}
.bar .tools{display:flex;gap:.5rem}
.center{display:grid;place-items:center;padding:3rem 1rem}
.card{width:min(560px,94vw);background:var(--surface);border:1px solid var(--border);border-radius:var(--r-xl);
  box-shadow:var(--shadow);padding:2rem 2.1rem}
.card.pad{text-align:center;padding:3rem 2rem}
.big{font-size:3rem}
.bad h2,.ok h2{margin-top:.6rem}
.small{font-size:.8rem}.center-t{text-align:center;margin-top:.8rem}
.head{text-align:center;margin-bottom:1.3rem}
.logo-lg{width:3rem;height:3rem;border-radius:12px;display:inline-grid;place-items:center;font-weight:900;font-size:1.4rem;
  background:linear-gradient(120deg,var(--brand),var(--brand-2));color:#fff;margin-bottom:.6rem}
.head h2{font-size:1.4rem;font-weight:800}
.kv{background:var(--surface-2);border:1px solid var(--border);border-radius:12px;padding:.5rem .9rem;margin-bottom:1.3rem}
.kv .l{display:flex;justify-content:space-between;font-size:.85rem;padding:.32rem 0}
.kv .l span{color:var(--muted)} .kv .l b{font-weight:700}
.step{margin-bottom:1.3rem}
.step-h{display:flex;align-items:center;gap:.5rem;font-weight:700;font-size:.96rem;margin-bottom:.7rem}
.step-h .num{width:1.5rem;height:1.5rem;border-radius:50%;background:var(--brand);color:#fff;display:grid;place-items:center;font-size:.8rem}
.step-h .opt{font-size:.72rem;font-weight:600;color:var(--faint);background:var(--surface-3);padding:.1rem .5rem;border-radius:999px}
.strength{display:flex;align-items:center;gap:.6rem;margin-top:.5rem}
.strength .track{flex:1;height:.4rem;border-radius:999px;background:var(--surface-3);overflow:hidden}
.strength .fill{height:100%;border-radius:999px;transition:.25s}
.strength .slabel{font-size:.76rem;font-weight:700}
.mfa{display:flex;gap:1rem;align-items:flex-start}
.qr{width:120px;height:120px;border-radius:12px;border:1px solid var(--border);background:#fff;padding:6px;flex:none}
.mfa-r{flex:1}
.secret{display:flex;flex-direction:column;gap:.2rem;margin:.5rem 0 .7rem}
.secret code{font-size:.85rem;word-break:break-all;background:var(--surface-2);border:1px solid var(--border);border-radius:8px;padding:.35rem .5rem}
.wide{width:100%}
.btn.solid{display:inline-block;text-align:center;background:linear-gradient(100deg,var(--brand),var(--brand-2));color:#fff;
  text-decoration:none;font-weight:800;padding:.8rem;border-radius:11px;margin-top:1rem}
.spin{font-size:1rem;color:var(--muted)}
</style>
