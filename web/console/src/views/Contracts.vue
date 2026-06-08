<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useI18n } from 'vue-i18n'
import { ElMessage, ElMessageBox } from 'element-plus'
import PageHelp from '@/components/PageHelp.vue'
import { useAuthStore } from '@/stores/auth'
import { downloadFile } from '@/api/request'
import {
  listContracts, contractDetail, createContract, sendContract, signParty, voidContract,
  listTemplates, createTemplate,
  type Contract, type ContractParty, type ContractTemplate, type PartyReq,
} from '@/api/contracts'

const { t } = useI18n()
const auth = useAuthStore()
const canEdit = () => auth.has('contract:edit')
const canSign = () => auth.has('contract:sign')

const tab = ref<'contracts' | 'templates'>('contracts')
const rows = ref<Contract[]>([])
const templates = ref<ContractTemplate[]>([])
const loading = ref(false)

const statusClass: Record<string, string> = { DRAFT: 's-soon', SENT: 's-init', SIGNING: 's-init', SIGNED: 's-active', VOID: 's-exp' }
const fmt = (s: string | null) => (s ? s.slice(0, 19).replace('T', ' ') : '—')
const money = (n: number) => '¥' + n.toLocaleString('en-US')

const signedCount = computed(() => rows.value.filter((r) => r.status === 'SIGNED').length)
const pendingCount = computed(() => rows.value.filter((r) => r.status === 'SENT' || r.status === 'SIGNING').length)
const signedAmount = computed(() => rows.value.filter((r) => r.status === 'SIGNED').reduce((a, b) => a + b.amount, 0))

async function load() {
  loading.value = true
  try { rows.value = await listContracts(); templates.value = await listTemplates() }
  finally { loading.value = false }
}
onMounted(load)

// ---- 新建合同 ----
const dlg = ref(false)
const form = ref<{ templateId: number | null; tenantCode: string; customer: string; planCode: string; title: string; contentHtml: string; amount: number; parties: PartyReq[] }>(
  { templateId: null, tenantCode: '', customer: '', planCode: '', title: '', contentHtml: '', amount: 0, parties: [{ name: '', partyRole: '甲方' }, { name: '', partyRole: '乙方' }] },
)
function openCreate() {
  form.value = { templateId: templates.value[0]?.id ?? null, tenantCode: '', customer: '', planCode: '', title: '', contentHtml: '', amount: 0,
    parties: [{ name: '', partyRole: '甲方' }, { name: '', partyRole: '乙方' }] }
  dlg.value = true
}
function addParty() { form.value.parties.push({ name: '', partyRole: '' }) }
function removeParty(i: number) { form.value.parties.splice(i, 1) }
async function save() {
  if (!form.value.customer.trim()) { ElMessage.warning(t('contract.custRequired')); return }
  if (!form.value.templateId && !form.value.contentHtml.trim()) { ElMessage.warning(t('contract.contentRequired')); return }
  const parties = form.value.parties.filter((p) => p.name.trim())
  if (!parties.length) { ElMessage.warning(t('contract.partyRequired')); return }
  try {
    await createContract({
      templateId: form.value.templateId, tenantCode: form.value.tenantCode, customer: form.value.customer,
      planCode: form.value.planCode, title: form.value.title, contentHtml: form.value.contentHtml,
      amount: Number(form.value.amount) || 0, parties,
    })
    ElMessage.success(t('common.saved')); dlg.value = false; load()
  } catch (e: any) { ElMessage.error(e?.response?.data?.message || t('common.fail')) }
}

// ---- 详情/签署 ----
const drawer = ref(false)
const detail = ref<{ contract: Contract; parties: ContractParty[] } | null>(null)
async function openDetail(c: Contract) { drawer.value = true; detail.value = null; detail.value = await contractDetail(c.id) }
async function refreshDetail() { if (detail.value) detail.value = await contractDetail(detail.value.contract.id) }

async function doSend(c: Contract) {
  try {
    await ElMessageBox.confirm(t('contract.sendConfirm'), t('contract.send'), { type: 'warning' })
    await sendContract(c.id); ElMessage.success(t('contract.sentOk')); load(); refreshDetail()
  } catch (e: any) { if (e !== 'cancel') ElMessage.error(e?.response?.data?.message || t('common.fail')) }
}
async function doSign(p: ContractParty) {
  try {
    await ElMessageBox.confirm(t('contract.signConfirm', { n: p.name }), t('contract.sign'), { type: 'warning' })
    await signParty(p.contractId, p.id); ElMessage.success(t('contract.signedOk')); load(); refreshDetail()
  } catch (e: any) { if (e !== 'cancel') ElMessage.error(e?.response?.data?.message || t('common.fail')) }
}
async function doVoid(c: Contract) {
  try {
    await ElMessageBox.confirm(t('contract.voidConfirm'), t('contract.void'), { type: 'warning' })
    await voidContract(c.id); ElMessage.success(t('contract.voidedOk')); load(); refreshDetail()
  } catch (e: any) { if (e !== 'cancel') ElMessage.error(e?.response?.data?.message || t('common.fail')) }
}

// ---- 模板 ----
const tplDlg = ref(false)
const tplForm = ref({ name: '', contentHtml: '', variables: 'customer,amount,plan,tenant,date' })
function openTpl() { tplForm.value = { name: '', contentHtml: '', variables: 'customer,amount,plan,tenant,date' }; tplDlg.value = true }
async function saveTpl() {
  if (!tplForm.value.name.trim() || !tplForm.value.contentHtml.trim()) { ElMessage.warning(t('contract.tplRequired')); return }
  try { await createTemplate({ ...tplForm.value }); ElMessage.success(t('common.saved')); tplDlg.value = false; load() }
  catch (e: any) { ElMessage.error(e?.response?.data?.message || t('common.fail')) }
}

function exportCsv() { downloadFile('/contracts/export.csv', 'contracts.csv') }
</script>

<template>
  <div class="wrap">
    <PageHelp id="contract" :title="t('contract.help.title')"
      :tips="[t('contract.help.t1'), t('contract.help.t2'), t('contract.help.t3'), t('contract.help.t4')]" />

    <div class="section-title">
      <div>
        <h2>{{ t('nav.contract') }}</h2>
        <div class="sub" style="margin-top:.3rem">{{ t('contract.lead') }}</div>
      </div>
      <div style="display:flex;gap:.6rem">
        <el-button v-if="tab === 'contracts'" @click="exportCsv">⬇ {{ t('common.exportCsv') }}</el-button>
        <el-button v-if="tab === 'contracts' && canEdit()" type="primary" @click="openCreate">＋ {{ t('contract.create') }}</el-button>
        <el-button v-if="tab === 'templates' && canEdit()" type="primary" @click="openTpl">＋ {{ t('contract.newTpl') }}</el-button>
      </div>
    </div>

    <section class="grid kpis" style="grid-template-columns:repeat(3,1fr);margin-bottom:1.1rem">
      <div class="card kpi"><div class="top"><span class="label">{{ t('contract.kSigned') }}</span><span class="ic">✅</span></div>
        <div class="val data">{{ signedCount }}</div></div>
      <div class="card kpi"><div class="top"><span class="label">{{ t('contract.kPending') }}</span><span class="ic">⏳</span></div>
        <div class="val data">{{ pendingCount }}</div></div>
      <div class="card kpi"><div class="top"><span class="label">{{ t('contract.kAmount') }}</span><span class="ic">💰</span></div>
        <div class="val data">{{ money(signedAmount) }}</div></div>
    </section>

    <el-tabs v-model="tab" style="margin-bottom:.4rem">
      <el-tab-pane :label="t('contract.tabContracts')" name="contracts" />
      <el-tab-pane :label="t('contract.tabTemplates')" name="templates" />
    </el-tabs>

    <!-- 合同列表 -->
    <div v-show="tab === 'contracts'" class="card">
      <el-table :data="rows" v-loading="loading" style="width:100%" max-height="560" @row-click="openDetail">
        <el-table-column :label="t('contract.colNo')" min-width="200">
          <template #default="{ row }"><b>{{ row.title }}</b><br><span class="data faint" style="font-size:.72rem">{{ row.contractNo || t('contract.noYet') }}</span></template>
        </el-table-column>
        <el-table-column :label="t('th.name')" width="150">
          <template #default="{ row }"><span class="data">{{ row.customer }}</span></template>
        </el-table-column>
        <el-table-column :label="t('billing.amount')" width="120">
          <template #default="{ row }"><span class="data" style="font-weight:700">{{ money(row.amount) }}</span></template>
        </el-table-column>
        <el-table-column :label="t('th.status')" width="120">
          <template #default="{ row }"><span class="status" :class="statusClass[row.status]"><i></i>{{ t('contract.s.' + row.status) }}</span></template>
        </el-table-column>
        <el-table-column :label="t('th.expire')" width="170">
          <template #default="{ row }"><span class="data faint">{{ fmt(row.createdAt) }}</span></template>
        </el-table-column>
        <el-table-column :label="t('th.op')" width="160">
          <template #default="{ row }">
            <button class="linkbtn" @click.stop="openDetail(row)">{{ t('contract.detail') }}</button>
            <button v-if="row.status === 'DRAFT' && canEdit()" class="linkbtn" style="margin-left:.6rem" @click.stop="doSend(row)">{{ t('contract.send') }}</button>
          </template>
        </el-table-column>
      </el-table>
      <div v-if="!loading && !rows.length" class="faint" style="text-align:center;padding:2.5rem 0">{{ t('contract.empty') }}</div>
    </div>

    <!-- 模板 -->
    <div v-show="tab === 'templates'" class="card">
      <el-table :data="templates" v-loading="loading" style="width:100%" max-height="560">
        <el-table-column :label="t('contract.tplName')" min-width="220">
          <template #default="{ row }"><b>{{ row.name }}</b></template>
        </el-table-column>
        <el-table-column :label="t('contract.tplVars')" min-width="240">
          <template #default="{ row }"><span class="data faint">{{ row.variables || '—' }}</span></template>
        </el-table-column>
        <el-table-column :label="t('th.expire')" width="180">
          <template #default="{ row }"><span class="data faint">{{ fmt(row.createdAt) }}</span></template>
        </el-table-column>
      </el-table>
      <div v-if="!loading && !templates.length" class="faint" style="text-align:center;padding:2.5rem 0">{{ t('contract.tplEmpty') }}</div>
    </div>

    <!-- 新建合同弹层 -->
    <el-dialog v-model="dlg" :title="t('contract.create')" width="660px">
      <el-form label-width="100px">
        <el-form-item :label="t('contract.template')">
          <el-select v-model="form.templateId" clearable :placeholder="t('contract.tplNonePh')" style="width:280px">
            <el-option v-for="tp in templates" :key="tp.id" :label="tp.name" :value="tp.id" />
          </el-select>
          <span class="faint" style="font-size:.74rem;margin-left:.6rem">{{ t('contract.tplOrContent') }}</span>
        </el-form-item>
        <el-form-item :label="t('th.name')"><el-input v-model="form.customer" :placeholder="t('contract.custPh')" /></el-form-item>
        <el-form-item :label="t('contract.titleField')"><el-input v-model="form.title" :placeholder="t('contract.titlePh')" /></el-form-item>
        <el-form-item :label="t('common.plan')"><el-input v-model="form.planCode" placeholder="ENTERPRISE / ..." style="width:220px" /></el-form-item>
        <el-form-item :label="t('billing.amount')"><el-input-number v-model="form.amount" :min="0" :step="1000" /></el-form-item>
        <el-form-item v-if="!form.templateId" :label="t('notice.content')">
          <el-input v-model="form.contentHtml" type="textarea" :rows="6" :placeholder="t('contract.contentPh')" />
        </el-form-item>
        <el-form-item :label="t('contract.parties')">
          <div style="width:100%">
            <div v-for="(p, i) in form.parties" :key="i" style="display:flex;gap:.5rem;margin-bottom:.5rem">
              <el-input v-model="p.partyRole" :placeholder="t('contract.role')" style="width:90px" />
              <el-input v-model="p.name" :placeholder="t('contract.partyName')" style="width:150px" />
              <el-input v-model="p.email" placeholder="email" style="width:160px" />
              <button class="linkbtn" @click="removeParty(i)">✕</button>
            </div>
            <button class="linkbtn" @click="addParty">＋ {{ t('contract.addParty') }}</button>
          </div>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dlg = false">{{ t('common.cancel') }}</el-button>
        <el-button type="primary" @click="save">{{ t('common.save') }}</el-button>
      </template>
    </el-dialog>

    <!-- 模板弹层 -->
    <el-dialog v-model="tplDlg" :title="t('contract.newTpl')" width="620px">
      <el-form label-width="92px">
        <el-form-item :label="t('contract.tplName')"><el-input v-model="tplForm.name" /></el-form-item>
        <el-form-item :label="t('contract.tplVars')"><el-input v-model="tplForm.variables" /></el-form-item>
        <el-form-item :label="t('notice.content')">
          <el-input v-model="tplForm.contentHtml" type="textarea" :rows="9" :placeholder="t('contract.tplContentPh')" />
          <div class="faint" style="font-size:.74rem;margin-top:.3rem">{{ t('contract.tplPlaceholderHint') }}</div>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="tplDlg = false">{{ t('common.cancel') }}</el-button>
        <el-button type="primary" @click="saveTpl">{{ t('common.save') }}</el-button>
      </template>
    </el-dialog>

    <!-- 详情抽屉 -->
    <el-drawer v-model="drawer" :title="detail?.contract.title || t('contract.detail')" size="520px">
      <div v-if="detail">
        <div class="faint" style="font-size:.78rem;margin-bottom:.8rem">
          {{ detail.contract.contractNo || t('contract.noYet') }} · {{ t('contract.s.' + detail.contract.status) }} · {{ money(detail.contract.amount) }}
        </div>

        <div class="card" style="margin-bottom:1rem">
          <h3 style="font-size:.92rem;margin-bottom:.6rem">{{ t('contract.parties') }}</h3>
          <div v-for="p in detail.parties" :key="p.id" class="party">
            <div>
              <b>{{ p.name }}</b> <span class="faint">{{ p.partyRole }}</span>
              <span class="status" :class="p.signStatus === 'SIGNED' ? 's-active' : 's-soon'" style="margin-left:.5rem"><i></i>{{ t('contract.ps.' + p.signStatus) }}</span>
            </div>
            <button v-if="p.signStatus === 'PENDING' && (detail.contract.status === 'SENT' || detail.contract.status === 'SIGNING') && canSign()"
              class="linkbtn" @click="doSign(p)">{{ t('contract.sign') }}</button>
            <span v-else-if="p.signHash" class="data faint" :title="p.signHash" style="font-size:.7rem">#{{ p.signHash.slice(0, 12) }}…</span>
          </div>
        </div>

        <div class="card" style="margin-bottom:1rem">
          <h3 style="font-size:.92rem;margin-bottom:.6rem">{{ t('contract.evidence') }}</h3>
          <div class="kv-row"><span class="faint">{{ t('contract.contentHash') }}</span><span class="data" style="font-size:.72rem;word-break:break-all">{{ detail.contract.contentHash || '—' }}</span></div>
          <div class="kv-row"><span class="faint">{{ t('contract.sentAt') }}</span><span class="data">{{ fmt(detail.contract.sentAt) }}</span></div>
          <div class="kv-row"><span class="faint">{{ t('contract.signedAt') }}</span><span class="data">{{ fmt(detail.contract.signedAt) }}</span></div>
        </div>

        <div class="card" style="margin-bottom:1rem">
          <h3 style="font-size:.92rem;margin-bottom:.6rem">{{ t('contract.contentSnapshot') }}</h3>
          <div class="snapshot" v-html="detail.contract.contentHtml"></div>
        </div>

        <div style="display:flex;gap:.6rem">
          <el-button v-if="detail.contract.status === 'DRAFT' && canEdit()" type="primary" @click="doSend(detail.contract)">{{ t('contract.send') }}</el-button>
          <el-button v-if="detail.contract.status !== 'SIGNED' && detail.contract.status !== 'VOID' && canEdit()" @click="doVoid(detail.contract)">{{ t('contract.void') }}</el-button>
        </div>
      </div>
    </el-drawer>
  </div>
</template>

<style scoped>
.party{display:flex;align-items:center;justify-content:space-between;padding:.5rem 0;border-bottom:1px dashed var(--border)}
.party:last-child{border:0}
.kv-row{display:flex;justify-content:space-between;gap:1rem;padding:.3rem 0;font-size:.82rem}
.snapshot{font-size:.84rem;line-height:1.7;white-space:pre-wrap;max-height:30vh;overflow:auto;color:var(--text)}
:deep(.el-table__row){cursor:pointer}
.linkbtn:disabled{opacity:.4;cursor:not-allowed}
</style>
