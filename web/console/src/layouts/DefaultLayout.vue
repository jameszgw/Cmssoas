<script setup lang="ts">
import { useI18n } from 'vue-i18n'
import ThemeSwitcher from '@/components/ThemeSwitcher.vue'
import LangSwitcher from '@/components/LangSwitcher.vue'

const { t } = useI18n()
const navs = [
  { to: '/overview', key: 'nav.overview' },
  { to: '/tenants', key: 'nav.tenants' },
  { to: '/licensing', key: 'nav.license' },
  { to: '/online', key: 'nav.online' },
  { to: '/products', key: 'nav.catalog' },
  { to: '/plans', key: 'nav.plan' },
  { to: '/audit', key: 'nav.audit' },
]
</script>

<template>
  <header class="topbar">
    <div class="inner">
      <div class="brand">
        <span class="logo">C</span>
        <span>CMSSOAS<small>{{ t('brand.sub') }}</small></span>
      </div>
      <nav class="nav">
        <router-link v-for="it in navs" :key="it.to" :to="it.to" active-class="active">
          {{ t(it.key) }}
        </router-link>
      </nav>
      <div class="spacer"></div>
      <div class="tools">
        <span class="pillbtn hidden-md-and-down">🔍 {{ t('common.search') }}</span>
        <ThemeSwitcher />
        <LangSwitcher />
        <button class="iconbtn">🔔<span class="dot"></span></button>
        <div class="avatar">运</div>
      </div>
    </div>
  </header>

  <main>
    <slot />
  </main>
</template>

<style scoped>
.topbar{position:sticky;top:0;z-index:30;background:var(--topbar);color:#fff;
  box-shadow:0 10px 30px -16px rgba(0,0,0,.45)}
.inner{width:100%;display:flex;align-items:center;gap:2rem;padding:.8rem clamp(28px,2.6vw,80px)}
.brand{display:flex;align-items:center;gap:.7rem;font-weight:800;font-size:1.18rem;letter-spacing:.4px}
.brand .logo{width:2.1rem;height:2.1rem;border-radius:9px;display:grid;place-items:center;
  background:rgba(255,255,255,.16);box-shadow:inset 0 0 0 1px rgba(255,255,255,.25);font-weight:900}
.brand small{display:block;font-size:.62rem;font-weight:600;opacity:.8;letter-spacing:2px;margin-top:1px}
.nav{display:flex;gap:.3rem;margin-left:1rem}
.nav a{color:rgba(255,255,255,.86);padding:.5rem .95rem;border-radius:10px;font-size:.92rem;font-weight:600;transition:.18s;white-space:nowrap}
.nav a:hover{background:rgba(255,255,255,.14);color:#fff}
.nav a.active{background:rgba(255,255,255,.2);color:#fff;box-shadow:inset 0 0 0 1px rgba(255,255,255,.18)}
.spacer{flex:1}
.tools{display:flex;align-items:center;gap:.55rem}
.pillbtn{display:inline-flex;align-items:center;gap:.4rem;background:rgba(255,255,255,.14);
  color:#fff;border:1px solid rgba(255,255,255,.18);border-radius:999px;padding:.42rem .8rem;font-size:.82rem;font-weight:600;cursor:pointer}
.iconbtn{width:2.2rem;height:2.2rem;border-radius:11px;background:rgba(255,255,255,.14);
  border:1px solid rgba(255,255,255,.16);color:#fff;display:grid;place-items:center;cursor:pointer;position:relative;font-size:1rem}
.iconbtn .dot{position:absolute;top:6px;right:7px;width:7px;height:7px;border-radius:50%;background:#ff5b5b}
.avatar{width:2.2rem;height:2.2rem;border-radius:50%;background:rgba(255,255,255,.22);display:grid;place-items:center;font-weight:800;font-size:.9rem}
</style>
