<script setup lang="ts">
import { ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { ElMessage } from 'element-plus'
import ThemeSwitcher from '@/components/ThemeSwitcher.vue'
import LangSwitcher from '@/components/LangSwitcher.vue'
import { useAuthStore } from '@/stores/auth'

const { t } = useI18n()
const router = useRouter()
const route = useRoute()
const auth = useAuthStore()

const username = ref('admin')
const password = ref('')
const loading = ref(false)

async function submit() {
  if (!username.value || !password.value) { ElMessage.warning(t('login.required')); return }
  loading.value = true
  try {
    await auth.login(username.value, password.value)
    ElMessage.success(t('login.ok'))
    const redirect = (route.query.redirect as string) || '/overview'
    router.replace(redirect)
  } catch (e: any) {
    ElMessage.error(e?.response?.data?.message || t('login.fail'))
  } finally { loading.value = false }
}
</script>

<template>
  <div class="auth">
    <header class="bar">
      <div class="brand"><span class="logo">C</span>CMSSOAS</div>
      <div class="tools"><ThemeSwitcher /><LangSwitcher /></div>
    </header>

    <main class="center">
      <div class="card">
        <div class="head">
          <div class="logo-lg">C</div>
          <h2>{{ t('login.title') }}</h2>
          <p class="muted">{{ t('login.subtitle') }}</p>
        </div>
        <el-form label-position="top" @submit.prevent="submit">
          <el-form-item :label="t('login.username')">
            <el-input v-model="username" size="large" prefix-icon="User" />
          </el-form-item>
          <el-form-item :label="t('login.password')">
            <el-input v-model="password" type="password" show-password size="large" prefix-icon="Lock"
              @keyup.enter="submit" :placeholder="t('login.pwdPlaceholder')" />
          </el-form-item>
          <el-button type="primary" size="large" class="wide" :loading="loading" @click="submit">{{ t('login.submit') }}</el-button>
        </el-form>
        <div class="hint">💡 {{ t('login.demo') }} <code class="data">admin / 8888</code></div>
      </div>
    </main>
  </div>
</template>

<style scoped>
.auth{min-height:100vh}
.bar{display:flex;align-items:center;justify-content:space-between;background:var(--topbar);color:#fff;padding:.8rem clamp(20px,3vw,48px)}
.bar .brand{display:flex;align-items:center;gap:.6rem;font-weight:800;font-size:1.1rem;letter-spacing:.4px}
.bar .logo{width:2rem;height:2rem;border-radius:8px;display:grid;place-items:center;background:rgba(255,255,255,.18);font-weight:900}
.bar .tools{display:flex;gap:.5rem}
.center{display:grid;place-items:center;padding:5rem 1rem}
.card{width:min(420px,94vw);background:var(--surface);border:1px solid var(--border);border-radius:var(--r-xl);box-shadow:var(--shadow);padding:2.2rem 2.2rem}
.head{text-align:center;margin-bottom:1.4rem}
.logo-lg{width:3rem;height:3rem;border-radius:12px;display:inline-grid;place-items:center;font-weight:900;font-size:1.4rem;background:linear-gradient(120deg,var(--brand),var(--brand-2));color:#fff;margin-bottom:.6rem}
.head h2{font-size:1.4rem;font-weight:800}
.wide{width:100%}
.hint{text-align:center;font-size:.8rem;color:var(--muted);margin-top:1rem}
.hint code{background:var(--surface-2);border:1px solid var(--border);border-radius:6px;padding:.1rem .4rem}
</style>
