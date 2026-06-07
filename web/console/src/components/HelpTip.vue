<script setup lang="ts">
defineProps<{ content: string }>()
</script>

<template>
  <el-tooltip :content="content" placement="top" effect="dark" :show-after="80" popper-class="help-pop">
    <span class="help-tip">?</span>
  </el-tooltip>
</template>

<style scoped>
.help-tip{display:inline-grid;place-items:center;width:1.05rem;height:1.05rem;border-radius:50%;
  font-size:.7rem;font-weight:800;color:var(--brand);background:color-mix(in srgb,var(--brand) 15%,transparent);
  cursor:help;font-family:var(--font-help);margin-left:.35rem;vertical-align:middle;user-select:none}
.help-tip:hover{background:color-mix(in srgb,var(--brand) 26%,transparent)}
</style>
<style>
.help-pop{max-width:320px!important;font-family:var(--font-help);line-height:1.6}
</style>
