<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { useI18n } from 'vue-i18n'
import { ElMessage, ElMessageBox } from 'element-plus'
import PageHelp from '@/components/PageHelp.vue'
import { useAuthStore } from '@/stores/auth'
import { downloadFile } from '@/api/request'
import { renewLicense, revokeLicense, type LicenseView } from '@/api/license'
import {
  getCmReportEditions, listCmReportLicenses, issueCmReportLicense,
  getCmReportPublicKey, verifyCmReportLicense,
  type CmReportEditions, type CmReportPublicKey,
} from '@/api/cmreport'

const { t } = useI18n()
const auth = useAuthStore()

const rows = ref<LicenseView[]>([])
const loading = ref(false)
const editions = ref<CmReportEditions | null>(null)
const statusClass: Record<string, string> = { ACTIVE: 's-active', REVOKED: 's-exp', EXPIRED: 's-exp' }

async function load() {
  loading.value = true
  try {
    rows.value = await listCmReportLicenses()
    if (!editions.value) editions.value = await getCmReportEditions()
  } catch { ElMessage.error(t('cmr.loadFail')) }
  finally { loading.value = false }
}
onMounted(load)

// ---------- 按版本签发 ----------
const issueOpen = ref(false)
const issuing = ref(false)
const today = new Date().toISOString().slice(0, 10)
const nextYear = new Date(Date.now() + 365 * 864e5).toISOString().slice(0, 10)
const form = reactive({
  tenantCode: '', customer: '',
  edition: 'pro',
  addons: [] as string[],
  concurrency: 100, nodes: 0, users: 0, instances: 0,
  fingerprint: '',
  notBefore: today, notAfter: nextYear,
})

// 当前所选版本的累计能力集(预览)
const editionCaps = computed(() => editions.value?.matrix?.[form.edition] ?? [])
// 附加包候选:排除所选版本基线内已含的能力
const addonOptions = computed(() =>
  (editions.value?.addons ?? []).filter((a) => !editionCaps.value.includes(a)))

async function submitIssue() {
  if (!form.tenantCode || !form.customer) { ElMessage.warning(t('cmr.required')); return }
  issuing.value = true
  try {
    const limits: Record<string, number> = {}
    if (form.concurrency > 0) limits.concurrency = form.concurrency
    if (form.nodes > 0) limits.nodes = form.nodes
    if (form.users > 0) limits.users = form.users
    if (form.instances > 0) limits.instances = form.instances
    const d = await issueCmReportLicense({
      tenantCode: form.tenantCode, customer: form.customer, edition: form.edition,
      addons: form.addons, limits, fingerprint: form.fingerprint || undefined,
      notBefore: form.notBefore, notAfter: form.notAfter, reason: t('cmr.issueReason'),
    })
    ElMessage.success(t('cmr.issued', { id: d.licenseId }))
    issueOpen.value = false
    load()
  } catch (e: any) {
    ElMessage.error(e?.response?.data?.message || t('cmr.issueFail'))
  } finally { issuing.value = false }
}

// ---------- 行操作:下载 / 自检 / 续期 / 吊销 ----------
function onDownload(row: LicenseView) {
  downloadFile(`/licenses/${row.licenseId}/download`, `${row.licenseId}.lic`)
}
async function onVerify(row: LicenseView) {
  try {
    const r = await verifyCmReportLicense(row.licenseId)
    if (r.valid) {
      ElMessageBox.alert(`<pre style="white-space:pre-wrap;word-break:break-all">${escapeHtml(pretty(r.payload))}</pre>`,
        t('cmr.verifyOk'), { dangerouslyUseHTMLString: true, customClass: 'cmr-verify-box' })
    } else {
      ElMessage.error(t('cmr.verifyFail'))
    }
  } catch (e: any) { ElMessage.error(e?.response?.data?.message || t('common.fail')) }
}
async function onRenew(row: LicenseView) {
  try {
    const { value } = await ElMessageBox.prompt(t('lic.renewTip'), t('lic.renew'), {
      inputValue: row.notAfter, inputPattern: /^\d{4}-\d{2}-\d{2}$/, inputErrorMessage: 'YYYY-MM-DD',
    })
    await renewLicense(row.licenseId, value)
    ElMessage.success(t('lic.renewed')); load()
  } catch { /* cancelled */ }
}
async function onRevoke(row: LicenseView) {
  try {
    const { value } = await ElMessageBox.prompt(t('lic.revokeTip'), t('lic.revoke'), {
      confirmButtonText: t('lic.revoke'), type: 'warning', inputValue: t('cmr.revokeDefault'),
    })
    await revokeLicense(row.licenseId, value)
    ElMessage.success(t('lic.revoked')); load()
  } catch { /* cancelled */ }
}

// ---------- 验签公钥(产品侧配置) ----------
const pkOpen = ref(false)
const pk = ref<CmReportPublicKey | null>(null)
async function openPk() {
  pkOpen.value = true
  if (!pk.value) {
    try { pk.value = await getCmReportPublicKey() } catch { ElMessage.error(t('common.fail')) }
  }
}
async function copyText(s: string) {
  try { await navigator.clipboard.writeText(s); ElMessage.success(t('lic.copied')) } catch { /* ignore */ }
}

function pretty(j: string) { try { return JSON.stringify(JSON.parse(j), null, 2) } catch { return j } }
function escapeHtml(s: string) { return s.replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;') }
</script>

<template>
  <div class="wrap">
    <PageHelp id="cmreport" :title="t('help.cmreport.title')"
      :tips="[t('help.cmreport.t1'), t('help.cmreport.t2'), t('help.cmreport.t3')]" />
    <div class="section-title">
      <div>
        <h2>{{ t('nav.cmreport') }}</h2>
        <div class="sub" style="margin-top:.3rem">{{ t('cmr.lead') }}</div>
      </div>
      <el-button @click="openPk">🔑 {{ t('cmr.pubKey') }}</el-button>
      <el-button v-if="auth.has('license:issue')" type="primary" size="large" @click="issueOpen = true">{{ t('cmr.issue') }}</el-button>
    </div>

    <div class="card">
      <el-table :data="rows" v-loading="loading" style="width:100%">
        <el-table-column :label="t('lic.id')" width="150">
          <template #default="{ row }"><span class="data">{{ row.licenseId }}</span></template>
        </el-table-column>
        <el-table-column :label="t('th.name')" min-width="180">
          <template #default="{ row }"><b>{{ row.customer }}</b><div class="faint" style="font-size:.75rem">{{ row.tenantCode }}</div></template>
        </el-table-column>
        <el-table-column :label="t('cmr.edition')" width="120">
          <template #default="{ row }"><el-tag effect="dark" size="small">{{ row.edition }}</el-tag></template>
        </el-table-column>
        <el-table-column :label="t('lic.version')" width="80">
          <template #default="{ row }">v{{ row.version }}</template>
        </el-table-column>
        <el-table-column :label="t('lic.notAfter')" width="120">
          <template #default="{ row }">{{ row.notAfter }}</template>
        </el-table-column>
        <el-table-column :label="t('th.status')" width="100">
          <template #default="{ row }"><span :class="['status', statusClass[row.status] || '']">{{ row.status }}</span></template>
        </el-table-column>
        <el-table-column :label="t('th.actions')" min-width="260">
          <template #default="{ row }">
            <el-button size="small" @click="onDownload(row)">⬇ .lic</el-button>
            <el-button size="small" @click="onVerify(row)">{{ t('cmr.verify') }}</el-button>
            <el-button v-if="auth.has('license:renew')" size="small" @click="onRenew(row)">{{ t('lic.renew') }}</el-button>
            <el-button v-if="auth.has('license:revoke')" size="small" type="danger" plain @click="onRevoke(row)">{{ t('lic.revoke') }}</el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <!-- 按版本签发 -->
    <el-dialog v-model="issueOpen" :title="t('cmr.issue')" width="640px">
      <el-form label-width="110px">
        <el-form-item :label="t('lic.tenant')"><el-input v-model="form.tenantCode" placeholder="T-100482" /></el-form-item>
        <el-form-item :label="t('th.name')"><el-input v-model="form.customer" :placeholder="t('cmr.customerPh')" /></el-form-item>
        <el-form-item :label="t('cmr.edition')">
          <el-radio-group v-model="form.edition">
            <el-radio-button v-for="e in editions?.editions ?? []" :key="e" :value="e">{{ e }}</el-radio-button>
          </el-radio-group>
          <div class="faint" style="font-size:.75rem;margin-top:.4rem;line-height:1.6">
            {{ t('cmr.capPreview', { n: editionCaps.length }) }}
            <el-tag v-for="c in editionCaps" :key="c" size="small" effect="plain" style="margin:1px 2px">{{ c }}</el-tag>
          </div>
        </el-form-item>
        <el-form-item :label="t('cmr.addons')">
          <el-checkbox-group v-model="form.addons">
            <el-checkbox v-for="a in addonOptions" :key="a" :value="a">{{ a }}</el-checkbox>
          </el-checkbox-group>
          <div v-if="!addonOptions.length" class="faint" style="font-size:.75rem">{{ t('cmr.addonsNone') }}</div>
        </el-form-item>
        <el-form-item :label="t('cmr.limits')">
          <div class="cmr-limits">
            <label>{{ t('lic.concurrency') }}<el-input-number v-model="form.concurrency" :min="0" size="small" /></label>
            <label>{{ t('cmr.nodes') }}<el-input-number v-model="form.nodes" :min="0" size="small" /></label>
            <label>{{ t('cmr.users') }}<el-input-number v-model="form.users" :min="0" size="small" /></label>
            <label>{{ t('cmr.instances') }}<el-input-number v-model="form.instances" :min="0" size="small" /></label>
          </div>
          <div class="faint" style="font-size:.75rem">{{ t('cmr.limitsTip') }}</div>
        </el-form-item>
        <el-form-item :label="t('cmr.fingerprint')">
          <el-input v-model="form.fingerprint" :placeholder="t('cmr.fingerprintPh')" />
        </el-form-item>
        <el-form-item :label="t('lic.notBefore')"><el-input v-model="form.notBefore" placeholder="YYYY-MM-DD" /></el-form-item>
        <el-form-item :label="t('lic.notAfter')"><el-input v-model="form.notAfter" placeholder="YYYY-MM-DD" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="issueOpen = false">{{ t('common.cancel') }}</el-button>
        <el-button type="primary" :loading="issuing" @click="submitIssue">{{ t('cmr.doIssue') }}</el-button>
      </template>
    </el-dialog>

    <!-- 验签公钥 -->
    <el-dialog v-model="pkOpen" :title="t('cmr.pubKey')" width="640px">
      <template v-if="pk">
        <p class="faint" style="margin-top:0">{{ t('cmr.pubKeyTip') }}</p>
        <div class="cmr-pk-meta"><b>{{ t('cmr.alg') }}:</b> {{ pk.algorithm }} · <b>kid:</b> <span class="data">{{ pk.kid }}</span></div>
        <el-input type="textarea" :rows="6" :model-value="pk.publicKeyBase64" readonly />
        <div style="margin-top:.6rem">
          <el-button size="small" @click="copyText(pk.publicKeyBase64)">📋 {{ t('cmr.copyKey') }}</el-button>
          <el-button size="small" @click="copyText('cmreport:\n  license:\n    enabled: true\n    public-key: ' + pk.publicKeyBase64)">📋 {{ t('cmr.copyYaml') }}</el-button>
        </div>
      </template>
      <div v-else v-loading="true" style="height:120px" />
    </el-dialog>
  </div>
</template>

<style scoped>
.cmr-limits { display: flex; gap: 1rem; flex-wrap: wrap; }
.cmr-limits label { display: flex; align-items: center; gap: .4rem; font-size: .85rem; }
.cmr-pk-meta { margin-bottom: .5rem; font-size: .85rem; }
</style>
