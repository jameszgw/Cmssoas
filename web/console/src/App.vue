<script setup lang="ts">
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import { ElConfigProvider } from 'element-plus'
import zhCn from 'element-plus/es/locale/lang/zh-cn'
import en from 'element-plus/es/locale/lang/en'
import { useLocaleStore } from '@/stores/locale'
import { useScreenScale } from '@/composables/useScreenScale'
import DefaultLayout from '@/layouts/DefaultLayout.vue'

useScreenScale()
const route = useRoute()
const localeStore = useLocaleStore()
const epLocale = computed(() => (localeStore.locale === 'zh-CN' ? zhCn : en))
const isPublic = computed(() => route.meta.public === true)
</script>

<template>
  <el-config-provider :locale="epLocale">
    <DefaultLayout v-if="!isPublic">
      <router-view />
    </DefaultLayout>
    <router-view v-else />
  </el-config-provider>
</template>
