<script setup lang="ts">
import { useI18n } from 'vue-i18n'
const { t } = useI18n()
</script>
<template>
  <div class="wrap">
    <div class="section-title"><h2>{{ t('nav.license') }}</h2></div>
    <div class="card" style="text-align:center;padding:4rem 2rem">
      <div style="font-size:2.4rem">🔑</div>
      <h3 style="justify-content:center;margin-top:1rem">{{ t('placeholder.wip') }}</h3>
      <div class="sub" style="margin-top:.4rem">{{ t('placeholder.wipSub') }}</div>
    </div>
  </div>
</template>
