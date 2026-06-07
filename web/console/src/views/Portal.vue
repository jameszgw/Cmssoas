<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { ElMessage } from 'element-plus'
import LangSwitcher from '@/components/LangSwitcher.vue'
import { portalLogin, setPortalToken } from '@/api/portal'

const { t } = useI18n()
const router = useRouter()
const tenantCode = ref('')
const accessCode = ref('')
const loading = ref(false)

async function login() {
  if (!tenantCode.value.trim() || !accessCode.value.trim()) { ElMessage.warning(t('portal.required')); return }
  loading.value = true
  try {
    const r = await portalLogin(tenantCode.value.trim(), accessCode.value.trim())
    setPortalToken(r.token)
    localStorage.setItem('codeman.portal.name', r.tenantName)
    ElMessage.success(t('portal.welcome', { n: r.tenantName }))
    router.replace('/portal/home')
  } catch (e: any) {
    ElMessage.error(e?.response?.data?.message || t('portal.loginFail'))
  } finally { loading.value = false }
}
</script>

<template>
  <div class="pwrap">
    <div class="ptools"><LangSwitcher /></div>
    <div class="pcard">
      <div class="brand"><span class="logo">C</span><div><div class="bt">CODEMAN</div><div class="bs">{{ t('portal.subtitle') }}</div></div></div>
      <h2>{{ t('portal.title') }}</h2>
      <p class="muted" style="font-size:.85rem;margin:.3rem 0 1.3rem">{{ t('portal.lead') }}</p>
      <el-form label-position="top" @submit.prevent>
        <el-form-item :label="t('portal.tenantCode')">
          <el-input v-model="tenantCode" size="large" placeholder="T-xxxxxx" />
        </el-form-item>
        <el-form-item :label="t('portal.accessCode')">
          <el-input v-model="accessCode" size="large" :placeholder="t('portal.accessPh')" @keyup.enter="login" />
        </el-form-item>
        <el-button type="primary" size="large" style="width:100%;margin-top:.4rem" :loading="loading" @click="login">
          {{ t('portal.login') }}
        </el-button>
      </el-form>
      <div class="hint faint">{{ t('portal.hint') }}</div>
    </div>
  </div>
</template>

<style scoped>
.pwrap{min-height:100vh;display:grid;place-items:center;position:relative;
  background:radial-gradient(1200px 600px at 80% -10%,color-mix(in srgb,var(--brand) 16%,transparent),transparent 60%),linear-gradient(180deg,var(--app-bg),var(--app-bg2))}
.ptools{position:absolute;top:1.2rem;right:1.4rem}
.pcard{width:min(420px,92vw);background:var(--surface);border:1px solid var(--border);border-radius:var(--r-xl);
  box-shadow:var(--shadow);padding:2.2rem 2.2rem 1.6rem}
.brand{display:flex;align-items:center;gap:.7rem;margin-bottom:1.4rem}
.brand .logo{width:2.4rem;height:2.4rem;border-radius:11px;display:grid;place-items:center;font-weight:900;color:#fff;
  background:linear-gradient(120deg,var(--brand),var(--brand-2))}
.brand .bt{font-weight:800;font-size:1.1rem}
.brand .bs{font-size:.7rem;color:var(--muted);font-weight:600}
.pcard h2{font-size:1.3rem;font-weight:800}
.hint{font-size:.74rem;text-align:center;margin-top:1rem}
</style>
