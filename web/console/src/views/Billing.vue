<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useI18n } from 'vue-i18n'
import { ElMessage } from 'element-plus'
import PageHelp from '@/components/PageHelp.vue'
import { useAuthStore } from '@/stores/auth'
import { downloadFile } from '@/api/request'
import { listInvoices, issueInvoice, createPayment, getPayment, sandboxConfirm, type Invoice, type Payment } from '@/api/billing'

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

function exportCsv() { downloadFile('/invoices/export.csv', 'invoices.csv') }
async function issue(row: Invoice) {
  try { const r = await issueInvoice(row.id); ElMessage.success(t('billing.issuedOk', { no: r.invoiceNo })); load() }
  catch (e: any) { ElMessage.error(e?.response?.data?.message || t('common.fail')) }
}

// ---- 在线收款(扫码支付,通用渠道,默认沙箱)----
const payDlg = ref(false)
const payment = ref<Payment | null>(null)
const paying = ref(false)
let pollTimer: number | undefined

/** 由 paymentNo+amount 生成一个稳定的"二维码风格"点阵(纯视觉,沙箱演示用)。 */
const qrCells = computed(() => {
  const s = (payment.value?.qrContent || payment.value?.paymentNo || 'CMSSOAS')
  const cells: boolean[] = []
  let h = 2166136261
  for (let i = 0; i < 21 * 21; i++) {
    h ^= s.charCodeAt(i % s.length) + i * 31
    h = Math.imul(h, 16777619)
    cells.push(((h >>> (i % 13)) & 1) === 1)
  }
  return cells
})
const isSandbox = computed(() => payment.value?.channel === 'MOCK')

function stopPoll() { if (pollTimer) { clearInterval(pollTimer); pollTimer = undefined } }
function startPoll() {
  stopPoll()
  pollTimer = window.setInterval(async () => {
    if (!payment.value) return
    try {
      const p = await getPayment(payment.value.id)
      payment.value = p
      if (p.status === 'PAID') { stopPoll(); ElMessage.success(t('billing.payOk')); load() }
    } catch { /* ignore transient */ }
  }, 1500)
}

async function collect(row: Invoice) {
  try {
    payment.value = await createPayment(row.id)
    payDlg.value = true
    startPoll()
  } catch (e: any) { ElMessage.error(e?.response?.data?.message || t('common.fail')) }
}
async function simulatePay() {
  if (!payment.value) return
  paying.value = true
  try { payment.value = await sandboxConfirm(payment.value.id) }
  catch (e: any) { ElMessage.error(e?.response?.data?.message || t('common.fail')) }
  finally { paying.value = false }
}
function closePay() { payDlg.value = false; stopPoll() }
</script>

<template>
  <div class="wrap">
    <PageHelp id="billing" :title="t('billing.title')" :tips="[t('billing.t1'), t('billing.t2'), t('billing.t3')]" />
    <div class="section-title">
      <div><h2>{{ t('nav.billing') }}</h2><div class="sub" style="margin-top:.3rem">{{ t('billing.lead') }}</div></div>
      <el-button @click="exportCsv">⬇ {{ t('common.exportCsv') }}</el-button>
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
            <button v-if="row.status === 'PENDING'" class="linkbtn" :disabled="!canManage()" @click="collect(row)">{{ t('billing.collect') }}</button>
            <button v-if="row.status === 'PAID'" class="linkbtn" :disabled="!canManage()" @click="issue(row)">{{ t('billing.issue') }}</button>
            <span v-if="row.status === 'INVOICED'" class="faint">{{ t('billing.done') }}</span>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <!-- 扫码收款弹层 -->
    <el-dialog v-model="payDlg" :title="t('billing.scanTitle')" width="420px" align-center
      :close-on-click-modal="false" @closed="closePay">
      <div v-if="payment" class="pay-box">
        <div class="pay-amt data">{{ fmtMoney(payment.amount) }}</div>
        <div class="faint" style="font-size:.78rem;margin-bottom:.9rem">
          {{ payment.customer }} · {{ payment.paymentNo }} · {{ payment.channel }}
        </div>

        <div v-if="payment.status !== 'PAID'" class="qr-wrap">
          <div class="qr">
            <span v-for="(c, i) in qrCells" :key="i" :class="{ on: c }"></span>
          </div>
          <div class="qr-tip faint">{{ t('billing.scanHint') }}</div>
          <div class="poll"><span class="dot"></span>{{ t('billing.waiting') }}</div>
        </div>

        <div v-else class="paid-ok">
          <div class="ok-ic">✓</div>
          <div class="ok-t">{{ t('billing.payOk') }}</div>
          <div class="faint" style="font-size:.78rem">{{ payment.providerTxnId }} · {{ fmt(payment.paidAt) }}</div>
        </div>
      </div>
      <template #footer>
        <el-button @click="closePay">{{ payment?.status === 'PAID' ? t('common.confirm') : t('common.cancel') }}</el-button>
        <el-button v-if="isSandbox && payment?.status !== 'PAID'" type="primary" :loading="paying" @click="simulatePay">
          ✅ {{ t('billing.simulatePay') }}
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.linkbtn:disabled{opacity:.4;cursor:not-allowed}
.pay-box{text-align:center}
.pay-amt{font-size:2.1rem;font-weight:800;color:var(--data-ink);line-height:1.1}
.qr-wrap{display:flex;flex-direction:column;align-items:center;gap:.6rem}
.qr{width:200px;height:200px;display:grid;grid-template-columns:repeat(21,1fr);grid-template-rows:repeat(21,1fr);
  padding:10px;background:#fff;border:1px solid var(--border);border-radius:12px}
.qr span{background:transparent}
.qr span.on{background:#111}
.qr-tip{font-size:.8rem}
.poll{display:inline-flex;align-items:center;gap:.45rem;font-size:.82rem;color:var(--brand);font-weight:600}
.poll .dot{width:.5rem;height:.5rem;border-radius:50%;background:var(--brand);animation:csb 1s steps(2) infinite}
@keyframes csb{50%{opacity:.25}}
.paid-ok{display:flex;flex-direction:column;align-items:center;gap:.5rem;padding:1.4rem 0}
.paid-ok .ok-ic{width:3.6rem;height:3.6rem;border-radius:50%;display:grid;place-items:center;font-size:2rem;color:#fff;
  background:var(--success);box-shadow:0 10px 24px -10px var(--success)}
.paid-ok .ok-t{font-weight:800;font-size:1.05rem;color:var(--success)}
</style>
