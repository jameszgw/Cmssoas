<script setup lang="ts">
import { ref } from 'vue'

const props = defineProps<{ id: string; title: string; tips: string[] }>()
const KEY = 'codeman.help.' + props.id
const open = ref(localStorage.getItem(KEY) !== '0')
function close() { open.value = false; localStorage.setItem(KEY, '0') }
function reopen() { open.value = true; localStorage.removeItem(KEY) }
</script>

<template>
  <div v-if="open" class="pagehelp">
    <div class="ph-ic">💡</div>
    <div class="ph-body">
      <div class="ph-title">{{ title }}</div>
      <ul class="ph-list">
        <li v-for="(tip, i) in tips" :key="i">{{ tip }}</li>
      </ul>
    </div>
    <button class="ph-x" :title="'收起'" @click="close">✕</button>
  </div>
  <button v-else class="ph-reopen" @click="reopen">💡 {{ title }}</button>
</template>

<style scoped>
.pagehelp{display:flex;gap:.8rem;align-items:flex-start;font-family:var(--font-help);
  background:linear-gradient(100deg,color-mix(in srgb,var(--brand) 10%,transparent),color-mix(in srgb,var(--brand-2) 8%,transparent));
  border:1px solid color-mix(in srgb,var(--brand) 24%,transparent);
  border-radius:var(--r-lg);padding:.9rem 1.1rem;margin-bottom:1.1rem;position:relative}
.ph-ic{font-size:1.2rem;line-height:1.4}
.ph-title{font-weight:800;font-size:.92rem;margin-bottom:.35rem;color:var(--text)}
.ph-list{margin:0;padding-left:1.1rem;display:flex;flex-direction:column;gap:.25rem}
.ph-list li{font-size:.82rem;color:var(--muted);line-height:1.55}
.ph-x{position:absolute;top:.7rem;right:.8rem;border:0;background:transparent;color:var(--faint);
  cursor:pointer;font-size:.85rem;font-family:var(--font-help)}
.ph-x:hover{color:var(--text)}
.ph-reopen{display:inline-flex;align-items:center;gap:.4rem;border:1px dashed color-mix(in srgb,var(--brand) 40%,transparent);
  background:transparent;color:var(--brand);border-radius:999px;padding:.35rem .85rem;font-size:.78rem;font-weight:700;
  cursor:pointer;font-family:var(--font-help);margin-bottom:1.1rem}
</style>
