<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useI18n } from 'vue-i18n'
import { ElMessage } from 'element-plus'
import PageHelp from '@/components/PageHelp.vue'
import { useAuthStore } from '@/stores/auth'
import { listInvoices, payInvoice, issueInvoice, type Invoice } from '@/api/billing'

const { t } = useI18n()
const auth = useAuthStore()
const rows = ref<Invoice[]>([])
const loading = ref(false)
const canManage = () => auth.has('billing:manage')

const statusClass: Record<string, string> = { PENDING: 's-soon', PAID: 's-active', INVOICED: 's-init', VOID: 's-exp' }
const typeText: Record<string, string> = { SUBSCRIBE: '订阅', CHANGE: '变更', RENEW: '续期' }
const fmtMoney = (n: number) => '¥' + n.toLocaleString('en-US')
const fmt = (s: string | null) => (s ? s.slice(0, 19).replace('T', ' ') : '—')

const totalPending = computed(() => rows.value.filter((r) => r.status === 'PENDING').reduce((a, b) => a + b.amount, 0))
const totalPaid = computed(() => rows.value.filter((r) => r.status !== 'PENDING' && r.status !== 'VOID').reduce((a, b) => a + b.amount, 0))

async function load() {
  loading.value = true
  try { rows.value = await listInvoices() } finally { loading.value = false }
}
onMounted(load)

async function pay(row: Invoice) {
  try { await payInvoice(row.id); ElMessage.success(t('billing.paidOk')); load() }
  catch (e: any) { ElMessage.error(e?.response?.data?.message || t('common.fail')) }
}
async function issue(row: Invoice) {
  try { const r = await issueInvoice(row.id); ElMessage.success(t('billing.issuedOk', { no: r.invoiceNo })); load() }
  catch (e: any) { ElMessage.error(e?.response?.data?.message || t('common.fail')) }
}
</script>

<template>
  <div class="wrap">
    <PageHelp id="billing" :title="t('billing.title')" :tips="[t('billing.t1'), t('billing.t2'), t('billing.t3')]" />
    <div class="section-title">
      <div><h2>{{ t('nav.billing') }}</h2><div class="sub" style="margin-top:.3rem">{{ t('billing.lead') }}</div></div>
    </div>

    <section class="grid kpis" style="grid-template-columns:repeat(3,1fr);margin-bottom:1.1rem">
      <div class="card kpi"><div class="top"><span class="label">{{ t('billing.kPending') }}</span><span class="ic">🧾</span></div>
        <div class="val data">{{ fmtMoney(totalPending) }}</div></div>
      <div class="card kpi"><div class="top"><span class="label">{{ t('billing.kPaid') }}</span><span class="ic">💰</span></div>
        <div class="val data">{{ fmtMoney(totalPaid) }}</div></div>
      <div class="card kpi"><div class="top"><span class="label">{{ t('billing.kCount') }}</span><span class="ic">📊</span></div>
        <div class="val data">{{ rows.length }}</div></div>
    </section>

    <div class="card">
      <el-table :data="rows" v-loading="loading" style="width:100%" max-height="600">
        <el-table-column :label="t('th.name')" min-width="180">
          <template #default="{ row }"><b>{{ row.customer }}</b> <span class="data faint" style="font-size:.72rem">{{ row.tenantCode }}</span></template>
        </el-table-column>
        <el-table-column :label="t('common.plan')" width="120">
          <template #default="{ row }"><span class="data">{{ row.planCode }}</span> <span class="faint">{{ typeText[row.type] || row.type }}</span></template>
        </el-table-column>
        <el-table-column :label="t('billing.amount')" width="120">
          <template #default="{ row }"><span class="data" style="font-weight:700">{{ fmtMoney(row.amount) }}</span></template>
        </el-table-column>
        <el-table-column :label="t('billing.invoiceNo')" width="200">
          <template #default="{ row }"><span class="data faint">{{ row.invoiceNo || '—' }}</span></template>
        </el-table-column>
        <el-table-column :label="t('th.status')" width="110">
          <template #default="{ row }"><span class="status" :class="statusClass[row.status]"><i></i>{{ t('billing.s.' + row.status) }}</span></template>
        </el-table-column>
        <el-table-column :label="t('th.expire')" width="170">
          <template #default="{ row }"><span class="data">{{ fmt(row.createdAt) }}</span></template>
        </el-table-column>
        <el-table-column :label="t('th.op')" width="150">
          <template #default="{ row }">
            <button v-if="row.status === 'PENDING'" class="linkbtn" :disabled="!canManage()" @click="pay(row)">{{ t('billing.pay') }}</button>
            <button v-if="row.status === 'PAID'" class="linkbtn" :disabled="!canManage()" @click="issue(row)">{{ t('billing.issue') }}</button>
            <span v-if="row.status === 'INVOICED'" class="faint">{{ t('billing.done') }}</span>
          </template>
        </el-table-column>
      </el-table>
    </div>
  </div>
</template>

<style scoped>
.linkbtn:disabled{opacity:.4;cursor:not-allowed}
</style>
