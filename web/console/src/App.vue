<script setup lang="ts">
import { computed } from 'vue'
import { ElConfigProvider } from 'element-plus'
import zhCn from 'element-plus/es/locale/lang/zh-cn'
import en from 'element-plus/es/locale/lang/en'
import { useLocaleStore } from '@/stores/locale'
import { useScreenScale } from '@/composables/useScreenScale'
import DefaultLayout from '@/layouts/DefaultLayout.vue'

useScreenScale()
const localeStore = useLocaleStore()
const epLocale = computed(() => (localeStore.locale === 'zh-CN' ? zhCn : en))
</script>

<template>
  <el-config-provider :locale="epLocale">
    <DefaultLayout>
      <router-view />
    </DefaultLayout>
  </el-config-provider>
</template>
