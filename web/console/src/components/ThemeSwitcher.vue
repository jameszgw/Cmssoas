<script setup lang="ts">
import { useThemeStore, THEMES, type ThemeKey } from '@/stores/theme'
import { useI18n } from 'vue-i18n'

const theme = useThemeStore()
const { t } = useI18n()
function pick(k: ThemeKey) { theme.setTheme(k) }
</script>

<template>
  <el-dropdown trigger="click" @command="pick">
    <button class="iconbtn" :title="t('theme.label')">🎨</button>
    <template #dropdown>
      <el-dropdown-menu>
        <el-dropdown-item v-for="it in THEMES" :key="it.key" :command="it.key">
          <span class="sw" :style="{ background: it.swatch }"></span>
          <span :class="{ on: theme.theme === it.key }">{{ t(it.labelKey) }}</span>
        </el-dropdown-item>
      </el-dropdown-menu>
    </template>
  </el-dropdown>
</template>

<style scoped>
.iconbtn{width:2.2rem;height:2.2rem;border-radius:11px;background:rgba(255,255,255,.14);
  border:1px solid rgba(255,255,255,.16);color:#fff;display:grid;place-items:center;cursor:pointer;font-size:1rem}
.sw{display:inline-block;width:.8rem;height:.8rem;border-radius:4px;margin-right:.5rem;vertical-align:-1px;
  box-shadow:inset 0 0 0 1px rgba(0,0,0,.15)}
.on{font-weight:800;color:var(--brand)}
</style>
