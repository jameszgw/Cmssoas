<script setup lang="ts">
import { computed } from 'vue'

interface Seg { value: number; color: string }
const props = defineProps<{ segments: Seg[] }>()

const total = computed(() => props.segments.reduce((s, x) => s + x.value, 0))
const arcs = computed(() => {
  let offset = 25 // 起点在顶部
  return props.segments.map((s) => {
    const pct = (s.value / total.value) * 100
    const arc = { dash: `${pct} ${100 - pct}`, offset: offset, color: s.color }
    offset -= pct
    return arc
  })
})
</script>

<template>
  <div class="donut">
    <svg viewBox="0 0 42 42" width="180" height="180">
      <circle cx="21" cy="21" r="15.9" fill="none" stroke="var(--surface-3)" stroke-width="6" />
      <circle
        v-for="(a, i) in arcs" :key="i"
        cx="21" cy="21" r="15.9" fill="none" :stroke="a.color" stroke-width="6"
        :stroke-dasharray="a.dash" :stroke-dashoffset="a.offset" stroke-linecap="round"
      />
    </svg>
    <div class="center">
      <slot />
    </div>
  </div>
</template>

<style scoped>
.donut{position:relative;width:180px;height:180px}
.center{position:absolute;inset:0;display:grid;place-items:center;text-align:center}
</style>
