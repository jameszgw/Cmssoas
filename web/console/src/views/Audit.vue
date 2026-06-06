<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useI18n } from 'vue-i18n'
import PageHelp from '@/components/PageHelp.vue'
import { listAudit, type AuditEntry } from '@/api/audit'

const { t } = useI18n()
const rows = ref<AuditEntry[]>([])
const keyword = ref('')
const loading = ref(false)

async function load() {
  loading.value = true
  try { rows.value = await listAudit() } finally { loading.value = false }
}
onMounted(load)

const filtered = computed(() => {
  const k = keyword.value.trim().toLowerCase()
  return rows.value.filter((r) => !k ||
    r.action.toLowerCase().includes(k) || (r.detail || '').toLowerCase().includes(k) || r.actor.toLowerCase().includes(k))
})
function fmt(ts: string) { return ts ? ts.slice(0, 19).replace('T', ' ') : '' }
function color(action: string) {
  if (action.includes('REVOKE') || action.includes('FAILED')) return 's-exp'
  if (action.includes('CREATED') || action.includes('ACTIVATED') || action.includes('DONE')) return 's-active'
  if (action.includes('MIGRATE') || action.includes('SEED') || action.includes('PROVISION')) return 's-init'
  return 's-soon'
}
</script>

<template>
  <div class="wrap">
    <PageHelp id="audit" :title="t('help.audit.title')"
      :tips="[t('help.audit.t1'), t('help.audit.t2'), t('help.audit.t3')]" />
    <div class="section-title">
      <div><h2>{{ t('nav.audit') }}</h2><div class="sub" style="margin-top:.3rem">{{ t('audit.lead') }}</div></div>
      <el-input v-model="keyword" :placeholder="t('audit.search')" style="width:280px" clearable />
    </div>

    <div class="card">
      <el-table :data="filtered" v-loading="loading" style="width:100%" max-height="640">
        <el-table-column :label="t('audit.time')" width="190">
          <template #default="{ row }"><span class="data">{{ fmt(row.createdAt) }}</span></template>
        </el-table-column>
        <el-table-column :label="t('audit.action')" width="200">
          <template #default="{ row }"><span class="status" :class="color(row.action)"><i></i>{{ row.action }}</span></template>
        </el-table-column>
        <el-table-column :label="t('audit.actor')" width="200">
          <template #default="{ row }"><span class="data">{{ row.actor }}</span></template>
        </el-table-column>
        <el-table-column :label="t('audit.tenant')" width="110">
          <template #default="{ row }"><span class="data faint">{{ row.tenantId ?? '—' }}</span></template>
        </el-table-column>
        <el-table-column :label="t('audit.detail')" min-width="280">
          <template #default="{ row }">{{ row.detail }}</template>
        </el-table-column>
      </el-table>
      <div v-if="!loading && !filtered.length" class="faint" style="text-align:center;padding:2.5rem 0">{{ t('audit.empty') }}</div>
    </div>
  </div>
</template>
