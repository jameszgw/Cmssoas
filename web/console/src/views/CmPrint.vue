<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { useI18n } from 'vue-i18n'
import { ElMessage, ElMessageBox } from 'element-plus'
import PageHelp from '@/components/PageHelp.vue'
import HelpTip from '@/components/HelpTip.vue'
import { useAuthStore } from '@/stores/auth'
import { downloadFile } from '@/api/request'
import { renewLicense, revokeLicense, type LicenseView } from '@/api/license'
import {
  getCmprintEditions, listCmprintLicenses, issueCmprintLicense, queryCmprintAudit,
  type CmprintEditionsView, type CmprintAuditRow,
} from '@/api/cmprint'

const { t } = useI18n()
const auth = useAuthStore()

const statusClass: Record<string, string> = { ACTIVE: 's-active', REVOKED: 's-exp', EXPIRED: 's-exp', SUSPENDED: 's-soon' }

// ---------- 档位矩阵 ----------
const editions = ref<CmprintEditionsView | null>(null)
/** 矩阵只展示三档有差异的能力键(任一档预设里出现过的键);其余键三档全开。 */
const gatedKeys = computed(() => {
  if (!editions.value) return []
  const keys = new Set<string>()
  editions.value.editions.forEach((e) => Object.keys(e.preset).forEach((k) => keys.add(k)))
  return editions.value.capabilityKeys.filter((k) => keys.has(k))
})
function capOf(edition: string, key: string) {
  return editions.value?.editions.find((e) => e.edition === edition)?.capabilities[key] ?? true
}

// ---------- License 列表 ----------
const rows = ref<LicenseView[]>([])
const loading = ref(false)
async function load() {
  loading.value = true
  try { rows.value = await listCmprintLicenses() }
  catch { ElMessage.error(t('cmprint.loadFail')) }
  finally { loading.value = false }
}

onMounted(async () => {
  try { editions.value = await getCmprintEditions() } catch { /* 矩阵加载失败不阻塞列表 */ }
  await load()
  await loadAudit()
})

// ---------- 签发 ----------
const issueOpen = ref(false)
const issuing = ref(false)
const today = new Date().toISOString().slice(0, 10)
const nextYear = new Date(Date.now() + 365 * 864e5).toISOString().slice(0, 10)
const EDITION_CODES = ['COMMUNITY', 'PROFESSIONAL', 'ENTERPRISE']
const MODES = ['OFFLINE', 'ONLINE', 'HYBRID']
const form = reactive({
  tenantCode: '', customer: '', edition: 'PROFESSIONAL', mode: 'OFFLINE',
  appVersionRange: '', notBefore: today, notAfter: nextYear, concurrency: 1,
})
/** 能力微调开关表:打开签发框/切换档位时按档位预设初始化。 */
const caps = reactive<Record<string, boolean>>({})
function presetOf(edition: string) { return editions.value?.editions.find((e) => e.edition === edition)?.preset ?? {} }
function resetCaps() {
  const preset = presetOf(form.edition)
  gatedKeys.value.forEach((k) => { caps[k] = preset[k] !== false })
}
function openIssue() {
  form.appVersionRange = form.appVersionRange || editions.value?.defaultVersionRange || '>=0.5.0 <1.0.0'
  resetCaps()
  issueOpen.value = true
}
/** 相对档位预设被用户改动的键(只把「合同微调」发给后端,审计里一目了然)。 */
const overrides = computed(() => {
  const preset = presetOf(form.edition)
  const out: Record<string, boolean> = {}
  gatedKeys.value.forEach((k) => {
    const presetVal = preset[k] !== false
    if (caps[k] !== presetVal) out[k] = caps[k]
  })
  return out
})

async function submitIssue() {
  if (!form.tenantCode || !form.customer) { ElMessage.warning(t('cmprint.required')); return }
  issuing.value = true
  try {
    const d = await issueCmprintLicense({
      tenantCode: form.tenantCode, customer: form.customer, edition: form.edition, mode: form.mode,
      overrides: overrides.value, appVersionRange: form.appVersionRange,
      notBefore: form.notBefore, notAfter: form.notAfter, concurrency: form.concurrency,
    })
    ElMessage.success(t('cmprint.issued', { id: d.licenseId }))
    issueOpen.value = false
    await load()
    await loadAudit()
  } catch (e: any) {
    ElMessage.error(e?.response?.data?.message || t('cmprint.issueFail'))
  } finally { issuing.value = false }
}

// ---------- 续期 / 吊销 / 下载(复用通用 License 端点) ----------
async function onRenew(row: LicenseView) {
  try {
    const { value } = await ElMessageBox.prompt(t('lic.renewTip'), t('lic.renew'), {
      inputValue: row.notAfter, inputPattern: /^\d{4}-\d{2}-\d{2}$/, inputErrorMessage: 'YYYY-MM-DD',
    })
    await renewLicense(row.licenseId, value)
    ElMessage.success(t('lic.renewed')); await load(); await loadAudit()
  } catch { /* cancelled */ }
}
async function onRevoke(row: LicenseView) {
  try {
    const { value } = await ElMessageBox.prompt(t('lic.revokeTip'), t('lic.revoke'), {
      confirmButtonText: t('lic.revoke'), type: 'warning', inputValue: t('cmprint.revokeReason'),
    })
    await revokeLicense(row.licenseId, value)
    ElMessage.success(t('lic.revoked')); await load(); await loadAudit()
  } catch { /* cancelled */ }
}
function onDownload(row: LicenseView) {
  downloadFile(`/licenses/${row.licenseId}/download`, `${row.licenseId}.lic`)
}

// ---------- 审计查询 ----------
const ACTIONS = ['CMPRINT_LICENSE_ISSUE', 'LICENSE_ISSUE', 'LICENSE_RENEW', 'LICENSE_MODIFY', 'LICENSE_REVOKE', 'LICENSE_EXPIRED']
const audit = reactive({ action: '', keyword: '', range: [] as string[], page: 1, size: 20 })
const auditRows = ref<CmprintAuditRow[]>([])
const auditTotal = ref(0)
const auditLoading = ref(false)
async function loadAudit() {
  if (!auth.has('cmprint:audit')) return
  auditLoading.value = true
  try {
    const p = await queryCmprintAudit({
      action: audit.action || undefined, keyword: audit.keyword || undefined,
      from: audit.range?.[0] || undefined, to: audit.range?.[1] || undefined,
      page: audit.page - 1, size: audit.size,
    })
    auditRows.value = p.rows
    auditTotal.value = p.total
  } finally { auditLoading.value = false }
}
function searchAudit() { audit.page = 1; loadAudit() }
function auditCsv() {
  const q = new URLSearchParams()
  if (audit.action) q.set('action', audit.action)
  if (audit.keyword) q.set('keyword', audit.keyword)
  if (audit.range?.[0]) q.set('from', audit.range[0])
  if (audit.range?.[1]) q.set('to', audit.range[1])
  downloadFile(`/cmprint/audit/export.csv?${q.toString()}`, 'cmprint-audit.csv')
}
function fmt(ts: string) { return ts ? ts.slice(0, 19).replace('T', ' ') : '' }
function actionColor(action: string) {
  if (action.includes('REVOKE') || action.includes('EXPIRED')) return 's-exp'
  if (action.startsWith('CMPRINT')) return 's-active'
  return 's-soon'
}
</script>

<template>
  <div class="wrap">
    <PageHelp id="cmprint" :title="t('help.cmprint.title')"
      :tips="[t('help.cmprint.t1'), t('help.cmprint.t2'), t('help.cmprint.t3')]" />
    <div class="section-title">
      <div>
        <h2>{{ t('nav.cmprint') }}</h2>
        <div class="sub" style="margin-top:.3rem">{{ t('cmprint.lead') }}</div>
      </div>
      <el-button v-if="auth.has('cmprint:issue')" type="primary" size="large" @click="openIssue">{{ t('cmprint.issue') }}</el-button>
    </div>

    <!-- 版本档位 × 能力矩阵 -->
    <div class="card" style="margin-bottom:1.1rem">
      <div class="card-h">
        <b>{{ t('cmprint.matrix') }}</b>
        <span class="faint" style="font-size:.78rem">{{ t('cmprint.matrixSub') }}</span>
      </div>
      <table v-if="editions" class="captab">
        <thead>
          <tr>
            <th>{{ t('cmprint.capability') }}</th>
            <th v-for="e in EDITION_CODES" :key="e">{{ t('cmprint.ed.' + e) }}</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="k in gatedKeys" :key="k">
            <td class="k"><span class="data">{{ k }}</span><span class="faint" style="margin-left:.5rem">{{ t('cmprint.cap.' + k) }}</span></td>
            <td v-for="e in EDITION_CODES" :key="e" class="c" :class="capOf(e, k) ? 'on' : 'off'">{{ capOf(e, k) ? '✓' : '—' }}</td>
          </tr>
        </tbody>
      </table>
      <div class="faint" style="font-size:.76rem;margin-top:.5rem">{{ t('cmprint.matrixNote') }}</div>
    </div>

    <!-- CmPrint License 列表 -->
    <div class="card" style="margin-bottom:1.1rem">
      <div class="card-h"><b>{{ t('cmprint.licenses') }}</b></div>
      <el-table :data="rows" v-loading="loading" style="width:100%">
        <el-table-column :label="t('lic.id')" width="150">
          <template #default="{ row }"><span class="data">{{ row.licenseId }}</span></template>
        </el-table-column>
        <el-table-column :label="t('th.name')" min-width="180">
          <template #default="{ row }"><b>{{ row.customer }}</b><div class="faint" style="font-size:.75rem">{{ row.tenantCode }}</div></template>
        </el-table-column>
        <el-table-column :label="t('lic.edition')" width="140">
          <template #default="{ row }">{{ t('cmprint.ed.' + row.edition) !== 'cmprint.ed.' + row.edition ? t('cmprint.ed.' + row.edition) : row.edition }}</template>
        </el-table-column>
        <el-table-column :label="t('lic.mode')" width="100">
          <template #default="{ row }"><span class="data">{{ row.mode }}</span></template>
        </el-table-column>
        <el-table-column :label="t('lic.range')" width="160">
          <template #default="{ row }"><span class="data">{{ row.appVersionRange }}</span></template>
        </el-table-column>
        <el-table-column :label="t('lic.version')" width="80">
          <template #default="{ row }"><span class="data">v{{ row.version }}</span></template>
        </el-table-column>
        <el-table-column :label="t('th.expire')" width="120">
          <template #default="{ row }"><span class="data">{{ row.notAfter }}</span></template>
        </el-table-column>
        <el-table-column :label="t('th.status')" width="110">
          <template #default="{ row }"><span class="status" :class="statusClass[row.status]"><i></i>{{ t('lic.st.' + row.status) }}</span></template>
        </el-table-column>
        <el-table-column :label="t('th.op')" width="200">
          <template #default="{ row }">
            <button v-if="auth.has('license:download')" class="linkbtn" @click="onDownload(row)">{{ t('lic.download') }}</button>
            <button v-if="auth.has('license:renew') && row.status!=='REVOKED'" class="linkbtn" style="margin-left:10px" @click="onRenew(row)">{{ t('lic.renew') }}</button>
            <button v-if="auth.has('license:revoke') && row.status!=='REVOKED'" class="linkbtn" style="margin-left:10px;color:var(--danger)" @click="onRevoke(row)">{{ t('lic.revoke') }}</button>
          </template>
        </el-table-column>
      </el-table>
      <div v-if="!loading && !rows.length" class="faint" style="text-align:center;padding:2rem 0">{{ t('cmprint.empty') }}</div>
    </div>

    <!-- 审计查询 -->
    <div v-if="auth.has('cmprint:audit')" class="card">
      <div class="card-h">
        <b>{{ t('cmprint.auditTitle') }}</b>
        <span class="faint" style="font-size:.78rem">{{ t('cmprint.auditSub') }}</span>
      </div>
      <div class="audit-filters">
        <el-select v-model="audit.action" clearable :placeholder="t('audit.action')" style="width:230px">
          <el-option v-for="a in ACTIONS" :key="a" :label="a" :value="a" />
        </el-select>
        <el-input v-model="audit.keyword" :placeholder="t('cmprint.auditKw')" clearable style="width:240px" @keyup.enter="searchAudit" />
        <el-date-picker v-model="audit.range" type="daterange" value-format="YYYY-MM-DD"
          :start-placeholder="t('subs.start')" :end-placeholder="t('subs.end')" style="width:260px" />
        <el-button type="primary" @click="searchAudit">{{ t('cmprint.auditQuery') }}</el-button>
        <el-button @click="auditCsv">⬇ {{ t('common.exportCsv') }}</el-button>
      </div>
      <el-table :data="auditRows" v-loading="auditLoading" style="width:100%" max-height="480">
        <el-table-column :label="t('audit.time')" width="190">
          <template #default="{ row }"><span class="data">{{ fmt(row.createdAt) }}</span></template>
        </el-table-column>
        <el-table-column :label="t('audit.action')" width="220">
          <template #default="{ row }"><span class="status" :class="actionColor(row.action)"><i></i>{{ row.action }}</span></template>
        </el-table-column>
        <el-table-column :label="t('audit.actor')" width="160">
          <template #default="{ row }"><span class="data">{{ row.actor }}</span></template>
        </el-table-column>
        <el-table-column :label="t('audit.detail')" min-width="300">
          <template #default="{ row }">{{ row.detail }}</template>
        </el-table-column>
      </el-table>
      <div class="audit-foot">
        <span v-if="!auditLoading && !auditRows.length" class="faint">{{ t('audit.empty') }}</span>
        <el-pagination v-else layout="total, prev, pager, next" :total="auditTotal"
          :page-size="audit.size" v-model:current-page="audit.page" @current-change="loadAudit()" />
      </div>
    </div>

    <!-- 签发向导 -->
    <el-dialog v-model="issueOpen" :title="t('cmprint.issueTitle')" width="760px" align-center>
      <el-form label-position="top">
        <div class="two">
          <el-form-item :label="t('m.tcode')"><el-input v-model="form.tenantCode" class="dataf" placeholder="T-100482" /></el-form-item>
          <el-form-item :label="t('th.name')"><el-input v-model="form.customer" /></el-form-item>
        </div>
        <el-form-item :label="t('lic.edition')">
          <el-radio-group v-model="form.edition" @change="resetCaps">
            <el-radio-button v-for="e in EDITION_CODES" :key="e" :value="e">{{ t('cmprint.ed.' + e) }}</el-radio-button>
          </el-radio-group>
        </el-form-item>
        <el-form-item>
          <template #label>{{ t('cmprint.overrides') }}<HelpTip :content="t('cmprint.tip.overrides')" /></template>
          <div class="capgrid">
            <label v-for="k in gatedKeys" :key="k" class="capitem">
              <el-switch v-model="caps[k]" size="small" />
              <span class="data" style="font-size:.78rem">{{ k }}</span>
              <span class="faint" style="font-size:.74rem">{{ t('cmprint.cap.' + k) }}</span>
            </label>
          </div>
        </el-form-item>
        <div class="two">
          <el-form-item :label="t('lic.mode')">
            <el-select v-model="form.mode" style="width:100%"><el-option v-for="m in MODES" :key="m" :label="m" :value="m" /></el-select>
          </el-form-item>
          <el-form-item>
            <template #label>{{ t('lic.range') }}<HelpTip :content="t('cmprint.tip.range')" /></template>
            <el-input v-model="form.appVersionRange" class="dataf" />
          </el-form-item>
        </div>
        <div class="two">
          <el-form-item :label="t('lic.notBefore')"><el-date-picker v-model="form.notBefore" type="date" value-format="YYYY-MM-DD" style="width:100%" /></el-form-item>
          <el-form-item :label="t('lic.notAfter')"><el-date-picker v-model="form.notAfter" type="date" value-format="YYYY-MM-DD" style="width:100%" /></el-form-item>
        </div>
        <el-form-item>
          <template #label>{{ t('lic.concurrency') }}<HelpTip :content="t('lic.tip.concurrency')" /></template>
          <el-input-number v-model="form.concurrency" :min="1" :max="10000" />
        </el-form-item>
        <div class="notice">🔏 <div>{{ t('cmprint.signNote') }}</div></div>
      </el-form>
      <template #footer>
        <el-button @click="issueOpen = false">{{ t('common.cancel') }}</el-button>
        <el-button type="primary" :loading="issuing" @click="submitIssue">{{ t('lic.issueSubmit') }}</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.card-h{display:flex;align-items:baseline;gap:.8rem;margin-bottom:.8rem}
.captab{width:100%;border-collapse:collapse;font-size:.82rem}
.captab th{text-align:left;color:var(--faint);font-size:.72rem;text-transform:uppercase;padding:.4rem .5rem;border-bottom:1px solid var(--border)}
.captab th:not(:first-child),.captab td.c{text-align:center;width:110px}
.captab td{padding:.4rem .5rem;border-bottom:1px solid var(--border)}
.captab td.c.on{color:var(--success);font-weight:700}
.captab td.c.off{color:var(--faint)}
.audit-filters{display:flex;gap:.6rem;flex-wrap:wrap;margin-bottom:.8rem}
.audit-foot{display:flex;justify-content:center;padding:.7rem 0 .1rem}
.two{display:grid;grid-template-columns:1fr 1fr;gap:.9rem}
.capgrid{display:grid;grid-template-columns:1fr 1fr;gap:.35rem .9rem;width:100%}
.capitem{display:flex;align-items:center;gap:.5rem;padding:.18rem .3rem;border-radius:8px}
.capitem:hover{background:var(--surface-2)}
.notice{display:flex;gap:.6rem;align-items:flex-start;background:color-mix(in srgb,var(--brand) 9%,transparent);
  border:1px solid color-mix(in srgb,var(--brand) 22%,transparent);border-radius:12px;padding:.7rem .9rem;font-size:.82rem;margin-top:.4rem}
:deep(.dataf .el-input__inner){font-family:var(--font-data);color:var(--data-ink)}
</style>
