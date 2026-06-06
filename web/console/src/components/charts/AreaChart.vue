<script setup lang="ts">
import { computed } from 'vue'

const props = defineProps<{ a: number[]; b: number[]; w?: number; h?: number }>()
const W = props.w ?? 720
const H = props.h ?? 230

function scale(arr: number[]) {
  const max = Math.max(...props.a, ...props.b) * 1.1
  const n = arr.length
  return arr.map((v, i) => ({ x: (i / (n - 1)) * W, y: H - (v / max) * (H - 24) - 8 }))
}
function line(arr: number[]) {
  return scale(arr).map((p, i) => `${i ? 'L' : 'M'}${p.x.toFixed(1)},${p.y.toFixed(1)}`).join(' ')
}
function area(arr: number[]) {
  const pts = scale(arr)
  return `${line(arr)} L${W},${H} L0,${H} Z`
}
const aLine = computed(() => line(props.a))
const aArea = computed(() => area(props.a))
const bLine = computed(() => line(props.b))
const bArea = computed(() => area(props.b))
const uid = Math.random().toString(36).slice(2, 7)
</script>

<template>
  <svg :viewBox="`0 0 ${W} ${H}`" width="100%" :height="H" preserveAspectRatio="none">
    <defs>
      <linearGradient :id="`ga${uid}`" x1="0" y1="0" x2="0" y2="1">
        <stop offset="0%" stop-color="var(--brand)" stop-opacity=".38" />
        <stop offset="100%" stop-color="var(--brand)" stop-opacity="0" />
      </linearGradient>
      <linearGradient :id="`gb${uid}`" x1="0" y1="0" x2="0" y2="1">
        <stop offset="0%" stop-color="var(--brand-2)" stop-opacity=".26" />
        <stop offset="100%" stop-color="var(--brand-2)" stop-opacity="0" />
      </linearGradient>
    </defs>
    <g stroke="var(--border)" stroke-width="1">
      <line x1="0" :y1="H*0.2" :x2="W" :y2="H*0.2" />
      <line x1="0" :y1="H*0.45" :x2="W" :y2="H*0.45" />
      <line x1="0" :y1="H*0.7" :x2="W" :y2="H*0.7" />
      <line x1="0" :y1="H-8" :x2="W" :y2="H-8" />
    </g>
    <path :d="aArea" :fill="`url(#ga${uid})`" />
    <path :d="bArea" :fill="`url(#gb${uid})`" />
    <path :d="bLine" fill="none" stroke="var(--brand-2)" stroke-width="2.5" />
    <path :d="aLine" fill="none" stroke="var(--brand)" stroke-width="3" />
  </svg>
</template>
