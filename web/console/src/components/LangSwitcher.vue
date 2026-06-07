<script setup lang="ts">
import { useI18n } from 'vue-i18n'
import { useLocaleStore, type LocaleKey } from '@/stores/locale'

const { locale } = useI18n()
const store = useLocaleStore()

function pick(l: LocaleKey) {
  store.setLocale(l)
  locale.value = l
}
</script>

<template>
  <el-dropdown trigger="click" @command="pick">
    <button class="pillbtn">{{ store.locale === 'zh-CN' ? '中文' : 'EN' }} ▾</button>
    <template #dropdown>
      <el-dropdown-menu>
        <el-dropdown-item command="zh-CN" :class="{ on: store.locale === 'zh-CN' }">中文</el-dropdown-item>
        <el-dropdown-item command="en-US" :class="{ on: store.locale === 'en-US' }">English</el-dropdown-item>
      </el-dropdown-menu>
    </template>
  </el-dropdown>
</template>

<style scoped>
.pillbtn{display:inline-flex;align-items:center;gap:.4rem;background:rgba(255,255,255,.14);
  color:#fff;border:1px solid rgba(255,255,255,.18);border-radius:999px;padding:.42rem .8rem;
  font-size:.82rem;font-weight:600;cursor:pointer}
.on{font-weight:800;color:var(--brand)}
</style>
