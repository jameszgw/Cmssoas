<script setup lang="ts">
import { useI18n } from 'vue-i18n'

defineProps<{ items: { labelKey: string; value: number; pct: number }[] }>()
const { t, n } = useI18n()
</script>

<template>
  <div class="bars">
    <div class="bar" v-for="it in items" :key="it.labelKey">
      <div class="bl"><span>{{ t(it.labelKey) }}</span><b>{{ n(it.value) }}</b></div>
      <div class="track"><div class="fill" :style="{ width: it.pct + '%' }"></div></div>
    </div>
  </div>
</template>
