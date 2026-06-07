<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { useI18n } from 'vue-i18n'
import { ElMessage, ElMessageBox } from 'element-plus'
import PageHelp from '@/components/PageHelp.vue'
import HelpTip from '@/components/HelpTip.vue'
import { useAuthStore } from '@/stores/auth'
import {
  listLicenses, issueLicense, renewLicense, revokeLicense, licenseHistory, downloadUrl,
  type LicenseView, type HistoryView,
} from '@/api/license'

const { t } = useI18n()
const auth = useAuthStore()

const rows = ref<LicenseView[]>([])
const loading = ref(false)
const statusClass: Record<string, string> = { ACTIVE: 's-active', REVOKED: 's-exp', EXPIRED: 's-exp', SUSPENDED: 's-soon' }

async function load() {
  loading.value = true
  try { rows.value = await listLicenses() }
  catch { ElMessage.error(t('lic.loadFail')) }
  finally { loading.value = false }
}
onMounted(load)

// ---------- 签发 ----------
const issueOpen = ref(false)
const issuing = ref(false)
const today = new Date().toISOString().slice(0, 10)
const nextYear = new Date(Date.now() + 365 * 864e5).toISOString().slice(0, 10)
const form = reactive({
  tenantCode: 'T-100482', customer: '华东数据科技有限公司',
  edition: 'ENTERPRISE', mode: 'HYBRID',
  modules: ['RISK', 'REPORT', 'AUDIT'] as string[],
  featRealtime: true, featExport: true, maxUsers: 200,
  appVersionRange: '>=2.0.0 <3.0.0',
  notBefore: today, notAfter: nextYear, concurrency: 5,
})
const ALL_MODULES = ['RISK', 'REPORT', 'AUDIT', 'BI', 'DATA']
const EDITIONS = ['BASIC', 'PROFESSIONAL', 'ENTERPRISE', 'FLAGSHIP']
const MODES = ['OFFLINE', 'ONLINE', 'HYBRID']

async function submitIssue() {
  if (!form.tenantCode || !form.customer) { ElMessage.warning(t('lic.required')); return }
  issuing.value = true
  try {
    const features: Record<string, any> = {
      'RISK.REALTIME': form.featRealtime, 'REPORT.EXPORT': form.featExport, MAX_USERS: form.maxUsers,
    }
    const d = await issueLicense({
      tenantCode: form.tenantCode, customer: form.customer, edition: form.edition, mode: form.mode,
      modules: form.modules, features, appVersionRange: form.appVersionRange,
      notBefore: form.notBefore, notAfter: form.notAfter, concurrency: form.concurrency, reason: '控制台签发',
    })
    ElMessage.success(t('lic.issued', { id: d.licenseId }))
    issueOpen.value = false
    load()
  } catch (e: any) {
    ElMessage.error(e?.response?.data?.message || t('lic.issueFail'))
  } finally { issuing.value = false }
}

// ---------- 续期 ----------
async function onRenew(row: LicenseView) {
  try {
    const { value } = await ElMessageBox.prompt(t('lic.renewTip'), t('lic.renew'), {
      inputValue: row.notAfter, inputPattern: /^\d{4}-\d{2}-\d{2}$/, inputErrorMessage: 'YYYY-MM-DD',
    })
    await renewLicense(row.licenseId, value)
    ElMessage.success(t('lic.renewed')); load()
  } catch { /* cancelled */ }
}

// ---------- 吊销 ----------
async function onRevoke(row: LicenseView) {
  try {
    const { value } = await ElMessageBox.prompt(t('lic.revokeTip'), t('lic.revoke'), {
      confirmButtonText: t('lic.revoke'), type: 'warning', inputValue: '客户违约',
    })
    await revokeLicense(row.licenseId, value)
    ElMessage.success(t('lic.revoked')); load()
  } catch { /* cancelled */ }
}

function onDownload(row: LicenseView) {
  window.open(downloadUrl(row.licenseId), '_blank')
}

// ---------- 历史 + diff ----------
const histOpen = ref(false)
const histId = ref('')
const history = ref<HistoryView[]>([])
const selVersion = ref<number | null>(null)

async function openHistory(row: LicenseView) {
  histId.value = row.licenseId
  histOpen.value = true
  history.value = await licenseHistory(row.licenseId)
  selVersion.value = history.value.length ? history.value[0].version : null
}

function parse(j: string) { try { return JSON.parse(j) } catch { return {} } }
function flat(o: any): Record<string, string> {
  const r: Record<string, string> = {}
  for (const k in o) { if (k === 'issuedAt') continue; const v = o[k]; r[k] = (v !== null && typeof v === 'object') ? JSON.stringify(v) : String(v) }
  return r
}
const diffRows = computed(() => {
  const idx = history.value.findIndex((h) => h.version === selVersion.value)
  if (idx < 0) return []
  const cur = flat(parse(history.value[idx].claimsJson))
  const prev = idx + 1 < history.value.length ? flat(parse(history.value[idx + 1].claimsJson)) : {}
  const keys = Array.from(new Set([...Object.keys(cur), ...Object.keys(prev)]))
  return keys.map((k) => {
    const c = cur[k], p = prev[k]
    let state: 'added' | 'removed' | 'changed' | 'same' =
      p === undefined ? 'added' : c === undefined ? 'removed' : c !== p ? 'changed' : 'same'
    return { key: k, cur: c, prev: p, state }
  })
})
const opTag: Record<string, string> = { ISSUE: 's-init', RENEW: 's-active', MODIFY: 's-soon', REVOKE: 's-exp' }
</script>

<template>
  <div class="wrap">
    <PageHelp id="licensing" :title="t('help.licensing.title')"
      :tips="[t('help.licensing.t1'), t('help.licensing.t2'), t('help.licensing.t3')]" />
    <div class="section-title">
      <div>
        <h2>{{ t('nav.license') }}</h2>
        <div class="sub" style="margin-top:.3rem">{{ t('lic.lead') }}</div>
      </div>
      <el-button v-if="auth.has('license:issue')" type="primary" size="large" @click="issueOpen = true">{{ t('lic.issue') }}</el-button>
    </div>

    <div class="card">
      <el-table :data="rows" v-loading="loading" style="width:100%">
        <el-table-column :label="t('lic.id')" width="150">
          <template #default="{ row }"><span class="data">{{ row.licenseId }}</span></template>
        </el-table-column>
        <el-table-column :label="t('th.name')" min-width="200">
          <template #default="{ row }"><b>{{ row.customer }}</b><div class="faint" style="font-size:.75rem">{{ row.tenantCode }}</div></template>
        </el-table-column>
        <el-table-column :label="t('lic.edition')" width="130">
          <template #default="{ row }">{{ row.edition }}</template>
        </el-table-column>
        <el-table-column :label="t('lic.modules')" min-width="200">
          <template #default="{ row }">
            <el-tag v-for="m in row.modules" :key="m" size="small" effect="plain" style="margin:2px">{{ m }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column :label="t('lic.mode')" width="100">
          <template #default="{ row }"><span class="data">{{ row.mode }}</span></template>
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
        <el-table-column :label="t('th.op')" width="240">
          <template #default="{ row }">
            <el-tooltip v-if="auth.has('license:download')" :content="t('lic.tip.download')" placement="top" :show-after="80">
              <button class="linkbtn" @click="onDownload(row)">{{ t('lic.download') }}</button>
            </el-tooltip>
            <el-tooltip :content="t('lic.tip.hist')" placement="top" :show-after="80">
              <button class="linkbtn" style="margin-left:10px" @click="openHistory(row)">{{ t('lic.hist') }}</button>
            </el-tooltip>
            <el-tooltip :content="t('lic.tip.renew')" placement="top" :show-after="80" v-if="auth.has('license:renew') && row.status!=='REVOKED'">
              <button class="linkbtn" style="margin-left:10px" @click="onRenew(row)">{{ t('lic.renew') }}</button>
            </el-tooltip>
            <el-tooltip :content="t('lic.tip.revoke')" placement="top" :show-after="80" v-if="auth.has('license:revoke') && row.status!=='REVOKED'">
              <button class="linkbtn" style="margin-left:10px;color:var(--danger)" @click="onRevoke(row)">{{ t('lic.revoke') }}</button>
            </el-tooltip>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <!-- 签发向导 -->
    <el-dialog v-model="issueOpen" :title="t('lic.issueTitle')" width="720px" align-center>
      <el-form label-position="top">
        <div class="two">
          <el-form-item :label="t('m.tcode')"><el-input v-model="form.tenantCode" class="dataf" /></el-form-item>
          <el-form-item :label="t('th.name')"><el-input v-model="form.customer" /></el-form-item>
        </div>
        <div class="two">
          <el-form-item :label="t('lic.edition')">
            <el-select v-model="form.edition" style="width:100%"><el-option v-for="e in EDITIONS" :key="e" :label="e" :value="e" /></el-select>
          </el-form-item>
          <el-form-item :label="t('lic.mode')">
            <el-select v-model="form.mode" style="width:100%"><el-option v-for="m in MODES" :key="m" :label="m" :value="m" /></el-select>
          </el-form-item>
        </div>
        <el-form-item :label="t('lic.modules')">
          <el-checkbox-group v-model="form.modules">
            <el-checkbox v-for="m in ALL_MODULES" :key="m" :value="m" border>{{ m }}</el-checkbox>
          </el-checkbox-group>
        </el-form-item>
        <el-form-item>
          <template #label>{{ t('lic.features') }}<HelpTip :content="t('lic.tip.features')" /></template>
          <el-checkbox v-model="form.featRealtime">RISK.REALTIME</el-checkbox>
          <el-checkbox v-model="form.featExport">REPORT.EXPORT</el-checkbox>
          <span style="margin-left:14px">MAX_USERS</span>
          <el-input-number v-model="form.maxUsers" :min="1" :max="100000" size="small" style="margin-left:8px" />
        </el-form-item>
        <div class="two">
          <el-form-item>
            <template #label>{{ t('lic.range') }}<HelpTip :content="t('lic.tip.range')" /></template>
            <el-input v-model="form.appVersionRange" class="dataf" />
          </el-form-item>
          <el-form-item>
            <template #label>{{ t('lic.concurrency') }}<HelpTip :content="t('lic.tip.concurrency')" /></template>
            <el-input-number v-model="form.concurrency" :min="1" :max="10000" style="width:100%" />
          </el-form-item>
        </div>
        <div class="two">
          <el-form-item :label="t('lic.notBefore')"><el-date-picker v-model="form.notBefore" type="date" value-format="YYYY-MM-DD" style="width:100%" /></el-form-item>
          <el-form-item :label="t('lic.notAfter')"><el-date-picker v-model="form.notAfter" type="date" value-format="YYYY-MM-DD" style="width:100%" /></el-form-item>
        </div>
        <div class="notice">🔏 <div>{{ t('lic.signNote') }}</div></div>
      </el-form>
      <template #footer>
        <el-button @click="issueOpen = false">{{ t('common.cancel') }}</el-button>
        <el-button type="primary" :loading="issuing" @click="submitIssue">{{ t('lic.issueSubmit') }}</el-button>
      </template>
    </el-dialog>

    <!-- 历史 + diff -->
    <el-drawer v-model="histOpen" :title="t('lic.histTitle') + ' · ' + histId" size="56%">
      <div class="hist">
        <div class="timeline">
          <div v-for="h in history" :key="h.version" class="hitem" :class="{ on: selVersion === h.version }" @click="selVersion = h.version">
            <div class="hv data">v{{ h.version }}</div>
            <div class="hbody">
              <span class="status" :class="opTag[h.opType]"><i></i>{{ t('lic.op.' + h.opType) }}</span>
              <div class="faint" style="font-size:.74rem;margin-top:4px">{{ h.createdAt.slice(0,19).replace('T',' ') }} · {{ h.operator }}</div>
              <div v-if="h.reason" class="small" style="margin-top:2px">{{ h.reason }}</div>
            </div>
          </div>
        </div>
        <div class="diff">
          <div class="diff-h">{{ t('lic.diffTitle') }}</div>
          <table class="difftab">
            <thead><tr><th>{{ t('lic.field') }}</th><th>{{ t('lic.oldVal') }}</th><th>{{ t('lic.newVal') }}</th></tr></thead>
            <tbody>
              <tr v-for="d in diffRows" :key="d.key" :class="d.state">
                <td class="k">{{ d.key }}</td>
                <td class="data old">{{ d.prev ?? '—' }}</td>
                <td class="data new">{{ d.cur ?? '—' }}<span v-if="d.state!=='same'" class="badge">{{ t('lic.diff.' + d.state) }}</span></td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>
    </el-drawer>
  </div>
</template>

<style scoped>
.two{display:grid;grid-template-columns:1fr 1fr;gap:.9rem}
.notice{display:flex;gap:.6rem;align-items:flex-start;background:color-mix(in srgb,var(--brand) 9%,transparent);
  border:1px solid color-mix(in srgb,var(--brand) 22%,transparent);border-radius:12px;padding:.7rem .9rem;font-size:.82rem;margin-top:.4rem}
:deep(.dataf .el-input__inner){font-family:var(--font-data);color:var(--data-ink)}
.small{font-size:.78rem}
.hist{display:grid;grid-template-columns:230px 1fr;gap:1.1rem;height:100%}
.timeline{border-right:1px solid var(--border);padding-right:.6rem;overflow:auto}
.hitem{display:flex;gap:.6rem;padding:.6rem;border-radius:10px;cursor:pointer;border:1px solid transparent}
.hitem:hover{background:var(--surface-2)}
.hitem.on{background:var(--surface-2);border-color:var(--brand)}
.hv{font-weight:800;font-size:1rem;width:2.2rem;flex:none}
.diff-h{font-weight:700;margin-bottom:.7rem}
.difftab{width:100%;border-collapse:collapse;font-size:.82rem}
.difftab th{text-align:left;color:var(--faint);font-size:.72rem;text-transform:uppercase;padding:.4rem .5rem;border-bottom:1px solid var(--border)}
.difftab td{padding:.45rem .5rem;border-bottom:1px solid var(--border);vertical-align:top;word-break:break-all}
.difftab td.k{font-weight:700;font-family:var(--font-help);color:var(--text);white-space:nowrap}
.difftab tr.same .old,.difftab tr.same .new{color:var(--muted)}
.difftab tr.changed{background:color-mix(in srgb,var(--warning) 9%,transparent)}
.difftab tr.added{background:color-mix(in srgb,var(--success) 9%,transparent)}
.difftab tr.removed{background:color-mix(in srgb,var(--danger) 9%,transparent)}
.badge{margin-left:.4rem;font-size:.66rem;font-weight:700;padding:.05rem .35rem;border-radius:6px;background:var(--surface-3);color:var(--muted);font-family:var(--font-help)}
</style>
