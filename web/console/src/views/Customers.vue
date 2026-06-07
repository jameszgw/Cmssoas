<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useI18n } from 'vue-i18n'
import { ElMessage } from 'element-plus'
import PageHelp from '@/components/PageHelp.vue'
import { useAuthStore } from '@/stores/auth'
import { downloadFile } from '@/api/request'
import {
  listCustomers, customerOverview, createCustomer, updateCustomer,
  type Customer, type Customer360,
} from '@/api/customers'

const { t } = useI18n()
const auth = useAuthStore()
const canEdit = () => auth.has('customer:edit')

const rows = ref<Customer[]>([])
const keyword = ref('')
const loading = ref(false)
const fmt = (s: string | null) => (s ? s.slice(0, 19).replace('T', ' ') : '—')
const money = (n: number) => '¥' + (n || 0).toLocaleString('en-US')

const filtered = computed(() => {
  const k = keyword.value.trim().toLowerCase()
  return rows.value.filter((r) => !k || r.name.toLowerCase().includes(k) ||
    (r.contact || '').toLowerCase().includes(k) || r.code.toLowerCase().includes(k))
})

async function load() {
  loading.value = true
  try { rows.value = await listCustomers() } finally { loading.value = false }
}
onMounted(load)
function exportCsv() { downloadFile('/customers/export.csv', 'customers.csv') }

// ---- 新建/编辑 ----
const dlg = ref(false)
const editing = ref<Customer | null>(null)
const form = ref<Partial<Customer>>({ name: '', contact: '', email: '', phone: '', industry: '', tenantCode: '', status: 'ACTIVE', note: '' })
function openCreate() { editing.value = null; form.value = { name: '', contact: '', email: '', phone: '', industry: '', tenantCode: '', status: 'ACTIVE', note: '' }; dlg.value = true }
function openEdit(c: Customer) { editing.value = c; form.value = { ...c }; dlg.value = true }
async function save() {
  if (!form.value.name?.trim()) { ElMessage.warning(t('customer.nameRequired')); return }
  try {
    if (editing.value) await updateCustomer(editing.value.id, form.value)
    else await createCustomer(form.value)
    ElMessage.success(t('common.saved')); dlg.value = false; load()
  } catch (e: any) { ElMessage.error(e?.response?.data?.message || t('common.fail')) }
}

// ---- 客户360 ----
const drawer = ref(false)
const c360 = ref<Customer360 | null>(null)
const tab = ref('licenses')
async function open360(c: Customer) { drawer.value = true; c360.value = null; tab.value = 'licenses'; c360.value = await customerOverview(c.id) }
const licStatus: Record<string, string> = { ACTIVE: 's-active', EXPIRED: 's-exp', REVOKED: 's-exp', PENDING: 's-soon' }
</script>

<template>
  <div class="wrap">
    <PageHelp id="customer" :title="t('customer.help.title')"
      :tips="[t('customer.help.t1'), t('customer.help.t2'), t('customer.help.t3'), t('customer.help.t4')]" />

    <div class="section-title">
      <div>
        <h2>{{ t('nav.customer') }}</h2>
        <div class="sub" style="margin-top:.3rem">{{ t('customer.lead') }}</div>
      </div>
      <div style="display:flex;gap:.6rem">
        <el-input v-model="keyword" :placeholder="t('customer.search')" style="width:240px" clearable />
        <el-button @click="exportCsv">⬇ {{ t('common.exportCsv') }}</el-button>
        <el-button v-if="canEdit()" type="primary" @click="openCreate">＋ {{ t('customer.create') }}</el-button>
      </div>
    </div>

    <section class="grid kpis" style="grid-template-columns:repeat(3,1fr);margin-bottom:1.1rem">
      <div class="card kpi"><div class="top"><span class="label">{{ t('customer.kTotal') }}</span><span class="ic">🏢</span></div>
        <div class="val data">{{ rows.length }}</div></div>
      <div class="card kpi"><div class="top"><span class="label">{{ t('customer.kActive') }}</span><span class="ic">✅</span></div>
        <div class="val data">{{ rows.filter((r) => r.status === 'ACTIVE').length }}</div></div>
      <div class="card kpi"><div class="top"><span class="label">{{ t('customer.kIndustry') }}</span><span class="ic">🗂️</span></div>
        <div class="val data">{{ new Set(rows.map((r) => r.industry).filter(Boolean)).size }}</div></div>
    </section>

    <div class="card">
      <el-table :data="filtered" v-loading="loading" style="width:100%" max-height="600" @row-click="open360">
        <el-table-column :label="t('customer.name')" min-width="220">
          <template #default="{ row }"><b>{{ row.name }}</b> <span class="data faint" style="font-size:.72rem">{{ row.code }}</span></template>
        </el-table-column>
        <el-table-column :label="t('customer.contact')" width="150">
          <template #default="{ row }">{{ row.contact || '—' }}</template>
        </el-table-column>
        <el-table-column :label="t('customer.email')" width="200">
          <template #default="{ row }"><span class="data faint">{{ row.email || '—' }}</span></template>
        </el-table-column>
        <el-table-column :label="t('customer.industry')" width="130">
          <template #default="{ row }"><span class="faint">{{ row.industry || '—' }}</span></template>
        </el-table-column>
        <el-table-column :label="t('th.status')" width="100">
          <template #default="{ row }"><span class="status" :class="row.status === 'ACTIVE' ? 's-active' : 's-exp'"><i></i>{{ t('customer.s.' + row.status) }}</span></template>
        </el-table-column>
        <el-table-column :label="t('th.op')" width="150">
          <template #default="{ row }">
            <button class="linkbtn" @click.stop="open360(row)">{{ t('customer.view360') }}</button>
            <button v-if="canEdit()" class="linkbtn" style="margin-left:.6rem" @click.stop="openEdit(row)">{{ t('common.edit') }}</button>
          </template>
        </el-table-column>
      </el-table>
      <div v-if="!loading && !filtered.length" class="faint" style="text-align:center;padding:2.5rem 0">{{ t('customer.empty') }}</div>
    </div>

    <!-- 新建/编辑 -->
    <el-dialog v-model="dlg" :title="editing ? t('customer.editTitle') : t('customer.create')" width="560px">
      <el-form label-width="86px">
        <el-form-item :label="t('customer.name')"><el-input v-model="form.name" :disabled="!!editing" :placeholder="t('customer.namePh')" /></el-form-item>
        <el-form-item :label="t('customer.contact')"><el-input v-model="form.contact" /></el-form-item>
        <el-form-item :label="t('customer.email')"><el-input v-model="form.email" /></el-form-item>
        <el-form-item :label="t('customer.phone')"><el-input v-model="form.phone" style="width:220px" /></el-form-item>
        <el-form-item :label="t('customer.industry')"><el-input v-model="form.industry" style="width:220px" /></el-form-item>
        <el-form-item :label="t('customer.tenant')"><el-input v-model="form.tenantCode" style="width:220px" placeholder="T-xxxxxx" /></el-form-item>
        <el-form-item v-if="editing" :label="t('th.status')">
          <el-switch v-model="form.status" active-value="ACTIVE" inactive-value="INACTIVE" />
        </el-form-item>
        <el-form-item :label="t('customer.note')"><el-input v-model="form.note" type="textarea" :rows="2" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dlg = false">{{ t('common.cancel') }}</el-button>
        <el-button type="primary" @click="save">{{ t('common.save') }}</el-button>
      </template>
    </el-dialog>

    <!-- 客户360 -->
    <el-drawer v-model="drawer" :title="c360?.customer.name || t('customer.view360')" size="640px">
      <div v-if="c360">
        <div class="faint" style="font-size:.78rem;margin-bottom:.9rem">
          {{ c360.customer.code }} · {{ c360.customer.contact || '—' }} · {{ c360.customer.email || '—' }}
        </div>

        <section class="grid" style="grid-template-columns:repeat(4,1fr);gap:.7rem;margin-bottom:1rem">
          <div class="m360"><div class="lbl">{{ t('customer.mLicense') }}</div><div class="v data">{{ c360.kpi.licenseCount }}<small>/{{ c360.kpi.activeLicenses }}{{ t('customer.activeShort') }}</small></div></div>
          <div class="m360"><div class="lbl">{{ t('customer.mContract') }}</div><div class="v data">{{ c360.kpi.contractCount }}</div></div>
          <div class="m360"><div class="lbl">{{ t('customer.mPaid') }}</div><div class="v data">{{ money(c360.kpi.paidAmount) }}</div></div>
          <div class="m360"><div class="lbl">{{ t('customer.mPending') }}</div><div class="v data">{{ money(c360.kpi.pendingAmount) }}</div></div>
        </section>

        <el-tabs v-model="tab">
          <el-tab-pane :label="t('customer.tabLicense') + ' (' + c360.licenses.length + ')'" name="licenses">
            <el-table :data="c360.licenses" max-height="380" size="small">
              <el-table-column :label="t('customer.cLicId')" min-width="160"><template #default="{ row }"><span class="data">{{ row.licenseId }}</span></template></el-table-column>
              <el-table-column :label="t('common.plan')" width="120"><template #default="{ row }">{{ row.edition || row.productCode }}</template></el-table-column>
              <el-table-column :label="t('th.status')" width="100"><template #default="{ row }"><span class="status" :class="licStatus[row.status] || 's-soon'"><i></i>{{ row.status }}</span></template></el-table-column>
              <el-table-column :label="t('th.expire')" width="120"><template #default="{ row }"><span class="data faint">{{ row.notAfter }}</span></template></el-table-column>
            </el-table>
          </el-tab-pane>
          <el-tab-pane :label="t('customer.tabContract') + ' (' + c360.contracts.length + ')'" name="contracts">
            <el-table :data="c360.contracts" max-height="380" size="small">
              <el-table-column :label="t('contract.colNo')" min-width="180"><template #default="{ row }">{{ row.title }}<br><span class="data faint" style="font-size:.7rem">{{ row.contractNo || '—' }}</span></template></el-table-column>
              <el-table-column :label="t('billing.amount')" width="110"><template #default="{ row }"><span class="data">{{ money(row.amount) }}</span></template></el-table-column>
              <el-table-column :label="t('th.status')" width="100"><template #default="{ row }"><span class="status" :class="row.status === 'SIGNED' ? 's-active' : 's-soon'"><i></i>{{ t('contract.s.' + row.status) }}</span></template></el-table-column>
            </el-table>
          </el-tab-pane>
          <el-tab-pane :label="t('customer.tabInvoice') + ' (' + c360.invoices.length + ')'" name="invoices">
            <el-table :data="c360.invoices" max-height="380" size="small">
              <el-table-column :label="t('common.plan')" width="120"><template #default="{ row }">{{ row.planCode }} {{ row.type }}</template></el-table-column>
              <el-table-column :label="t('billing.amount')" width="110"><template #default="{ row }"><span class="data">{{ money(row.amount) }}</span></template></el-table-column>
              <el-table-column :label="t('th.status')" width="100"><template #default="{ row }"><span class="status" :class="row.status === 'PENDING' ? 's-soon' : 's-active'"><i></i>{{ t('billing.s.' + row.status) }}</span></template></el-table-column>
              <el-table-column :label="t('billing.invoiceNo')" min-width="160"><template #default="{ row }"><span class="data faint">{{ row.invoiceNo || '—' }}</span></template></el-table-column>
            </el-table>
          </el-tab-pane>
          <el-tab-pane :label="t('customer.tabSub') + ' (' + c360.subscriptions.length + ')'" name="subs">
            <el-table :data="c360.subscriptions" max-height="380" size="small">
              <el-table-column :label="t('common.plan')" width="140"><template #default="{ row }">{{ row.planCode }} ×{{ row.qty }}</template></el-table-column>
              <el-table-column :label="t('th.status')" width="100"><template #default="{ row }"><span class="status s-active"><i></i>{{ row.status }}</span></template></el-table-column>
              <el-table-column :label="t('th.expire')" width="120"><template #default="{ row }"><span class="data faint">{{ row.endAt }}</span></template></el-table-column>
            </el-table>
          </el-tab-pane>
        </el-tabs>
      </div>
    </el-drawer>
  </div>
</template>

<style scoped>
.linkbtn:disabled{opacity:.4;cursor:not-allowed}
.m360{background:var(--surface-2);border:1px solid var(--border);border-radius:12px;padding:.7rem .8rem}
.m360 .lbl{font-size:.72rem;color:var(--muted);font-weight:600;margin-bottom:.3rem}
.m360 .v{font-size:1.25rem;font-weight:800;color:var(--data-ink)}
.m360 .v small{font-size:.66rem;color:var(--faint);font-weight:600;margin-left:.2rem}
:deep(.el-table__row){cursor:pointer}
</style>
