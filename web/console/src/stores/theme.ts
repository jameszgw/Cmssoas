import { defineStore } from 'pinia'
import { ref, watch } from 'vue'

export type ThemeKey = 'tech' | 'midnight' | 'gold'

export const THEMES: { key: ThemeKey; labelKey: string; swatch: string }[] = [
  { key: 'tech', labelKey: 'theme.tech', swatch: '#2f6bff' },
  { key: 'midnight', labelKey: 'theme.midnight', swatch: '#0f1830' },
  { key: 'gold', labelKey: 'theme.gold', swatch: '#c07a12' },
]

export const useThemeStore = defineStore('theme', () => {
  const theme = ref<ThemeKey>((localStorage.getItem('codeman.theme') as ThemeKey) || 'tech')

  function apply(t: ThemeKey) {
    document.documentElement.dataset.theme = t
    // Element Plus 暗色：仅 midnight 启用官方 dark css-vars
    document.documentElement.classList.toggle('dark', t === 'midnight')
  }

  function setTheme(t: ThemeKey) {
    theme.value = t
  }

  watch(theme, (t) => {
    localStorage.setItem('codeman.theme', t)
    apply(t)
  }, { immediate: true })

  return { theme, setTheme }
})
