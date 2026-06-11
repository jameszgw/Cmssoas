<script setup lang="ts">
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { ElMessageBox } from 'element-plus'
import ThemeSwitcher from '@/components/ThemeSwitcher.vue'
import LangSwitcher from '@/components/LangSwitcher.vue'
import ForcePwdDialog from '@/components/ForcePwdDialog.vue'
import NoticeGate from '@/components/NoticeGate.vue'
import CsWidget from '@/components/CsWidget.vue'
import { useAuthStore } from '@/stores/auth'

const { t, locale } = useI18n()
const router = useRouter()
const auth = useAuthStore()

// 帮助中心(静态页,随前端发布到 public/);按当前语言选择中/英版本
const helpUrl = computed(
  () => `${import.meta.env.BASE_URL}${String(locale.value).startsWith('en') ? 'help-en.html' : 'help.html'}`,
)

const allNavs = [
  { to: '/overview', key: 'nav.overview', perm: 'overview' },
  { to: '/tenants', key: 'nav.tenants', perm: 'tenant' },
  { to: '/licensing', key: 'nav.license', perm: 'license' },
  { to: '/online', key: 'nav.online', perm: 'online' },
  { to: '/products', key: 'nav.catalog', perm: 'catalog' },
  { to: '/plans', key: 'nav.plan', perm: 'plan' },
  { to: '/cmprint', key: 'nav.cmprint', perm: 'cmprint' },
  { to: '/customers', key: 'nav.customer', perm: 'customer' },
  { to: '/billing', key: 'nav.billing', perm: 'billing' },
  { to: '/contracts', key: 'nav.contract', perm: 'contract' },
  { to: '/notices', key: 'nav.notice', perm: 'notice' },
  { to: '/cs', key: 'nav.cs', perm: 'cs' },
  { to: '/harden', key: 'nav.harden', perm: 'harden' },
  { to: '/portal-admin', key: 'nav.portalAdmin', perm: 'tenant:portal' },
  { to: '/audit', key: 'nav.audit', perm: 'audit' },
  { to: '/system/roles', key: 'nav.system', perm: 'role:view' },
  { to: '/system/users', key: 'nav.users', perm: 'user:view' },
]
// 仅展示用户有权限的菜单
const navs = computed(() => allNavs.filter((n) => auth.has(n.perm)))

const avatarText = computed(() => (auth.username ? auth.username[0].toUpperCase() : '运'))

async function logout() {
  try {
    await ElMessageBox.confirm(t('user.logoutConfirm'), t('user.logout'), { type: 'warning' })
    auth.logout()
    router.replace('/login')
  } catch { /* cancel */ }
}
</script>

<template>
  <header class="topbar">
    <div class="inner">
      <div class="brand">
        <span class="logo">C</span>
        <span>CODEMAN<small>{{ t('brand.sub') }}</small></span>
      </div>
      <nav class="nav">
        <router-link v-for="it in navs" :key="it.to" :to="it.to" active-class="active">
          {{ t(it.key) }}
        </router-link>
      </nav>
      <div class="spacer"></div>
      <div class="tools">
        <span class="pillbtn hidden-md-and-down">🔍 {{ t('common.search') }}</span>
        <a class="iconbtn" :href="helpUrl" target="_blank" rel="noopener" :title="t('common.help')">❔</a>
        <ThemeSwitcher />
        <LangSwitcher />
        <button class="iconbtn">🔔<span class="dot"></span></button>
        <el-dropdown trigger="click" @command="(c: string) => c === 'logout' && logout()">
          <div class="userbox">
            <div class="avatar">{{ avatarText }}</div>
            <div class="uinfo">
              <div class="uname">{{ auth.username || '—' }}</div>
              <div class="urole">{{ auth.roleName || auth.role }}</div>
            </div>
          </div>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item disabled>{{ auth.username }} · {{ auth.roleName }}</el-dropdown-item>
              <el-dropdown-item command="logout" divided>🚪 {{ t('user.logout') }}</el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </div>
    </div>
  </header>

  <main>
    <slot />
  </main>

  <ForcePwdDialog />
  <NoticeGate />
  <CsWidget />
</template>

<style scoped>
.topbar{position:sticky;top:0;z-index:30;background:var(--topbar);color:#fff;box-shadow:0 10px 30px -16px rgba(0,0,0,.45)}
.inner{width:100%;display:flex;align-items:center;gap:2rem;padding:.8rem clamp(28px,2.6vw,80px)}
.brand{display:flex;align-items:center;gap:.7rem;font-weight:800;font-size:1.18rem;letter-spacing:.4px}
.brand .logo{width:2.1rem;height:2.1rem;border-radius:9px;display:grid;place-items:center;background:rgba(255,255,255,.16);box-shadow:inset 0 0 0 1px rgba(255,255,255,.25);font-weight:900}
.brand small{display:block;font-size:.62rem;font-weight:600;opacity:.8;letter-spacing:2px;margin-top:1px}
.nav{display:flex;gap:.3rem;margin-left:1rem}
.nav a{color:rgba(255,255,255,.86);padding:.5rem .95rem;border-radius:10px;font-size:.92rem;font-weight:600;transition:.18s;white-space:nowrap}
.nav a:hover{background:rgba(255,255,255,.14);color:#fff}
.nav a.active{background:rgba(255,255,255,.2);color:#fff;box-shadow:inset 0 0 0 1px rgba(255,255,255,.18)}
.spacer{flex:1}
.tools{display:flex;align-items:center;gap:.55rem}
.pillbtn{display:inline-flex;align-items:center;gap:.4rem;background:rgba(255,255,255,.14);color:#fff;border:1px solid rgba(255,255,255,.18);border-radius:999px;padding:.42rem .8rem;font-size:.82rem;font-weight:600;cursor:pointer}
.iconbtn{width:2.2rem;height:2.2rem;border-radius:11px;background:rgba(255,255,255,.14);border:1px solid rgba(255,255,255,.16);color:#fff;display:grid;place-items:center;cursor:pointer;position:relative;font-size:1rem;text-decoration:none}
.iconbtn .dot{position:absolute;top:6px;right:7px;width:7px;height:7px;border-radius:50%;background:#ff5b5b}
.userbox{display:flex;align-items:center;gap:.5rem;cursor:pointer;padding:.2rem .3rem;border-radius:10px}
.userbox:hover{background:rgba(255,255,255,.12)}
.avatar{width:2.2rem;height:2.2rem;border-radius:50%;background:rgba(255,255,255,.22);display:grid;place-items:center;font-weight:800;font-size:.9rem}
.uinfo{line-height:1.1}
.uname{font-size:.84rem;font-weight:700}
.urole{font-size:.66rem;opacity:.82}
</style>
