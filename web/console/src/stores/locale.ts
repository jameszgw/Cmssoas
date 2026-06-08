import { defineStore } from 'pinia'
import { ref } from 'vue'

export type LocaleKey = 'zh-CN' | 'en-US'

export const useLocaleStore = defineStore('locale', () => {
  const locale = ref<LocaleKey>((localStorage.getItem('codeman.locale') as LocaleKey) || 'zh-CN')

  function setLocale(l: LocaleKey) {
    locale.value = l
    localStorage.setItem('codeman.locale', l)
    document.documentElement.lang = l
  }

  return { locale, setLocale }
})
