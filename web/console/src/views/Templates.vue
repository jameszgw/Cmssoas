<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { useI18n } from 'vue-i18n'
import { ElMessage, ElMessageBox } from 'element-plus'
import PageHelp from '@/components/PageHelp.vue'
import { useAuthStore } from '@/stores/auth'
import { downloadFile } from '@/api/request'
import {
  listTemplates, getTemplate, getVersions, createTemplate, updateTemplate,
  submitTemplate, approveTemplate, rejectTemplate, rollbackTemplate,
  disableTemplate, enableTemplate, deleteTemplate,
  listGalleryKeys, ensureGalleryKey, resetGalleryKey,
  type TplView, type TplVersion, type GalleryKey,
} from '@/api/tpl'

const { t } = useI18n()
const auth = useAuthStore()

const statusClass: Record<string, string> = { APPROVED: 's-active', PENDING: 's-soon', DRAFT: 's-init', DISABLED: 's-exp' }

// ---------- 列表 ----------
const rows = ref<TplView[]>([])
const loading = ref(false)
const filter = reactive({ status: '', keyword: '' })
async function load() {
  loading.value = true
  try { rows.value = await listTemplates(filter.status, filter.keyword) }
  finally { loading.value = false }
}
onMounted(() => { load(); loadKeys() })

// ---------- 新建 / 编辑 ----------
const editOpen = ref(false)
const saving = ref(false)
const edit = reactive({ code: '', name: '', tenantCode: '', tags: '', content: '' })
function openCreate() {
  Object.assign(edit, { code: '', name: '', tenantCode: '', tags: '', content: '{\n  "panels": [\n    { "index": 0, "width": 210, "height": 297, "printElements": [] }\n  ]\n}' })
  editOpen.value = true
}
async function openEdit(row: TplView) {
  const d = await getTemplate(row.code)
  Object.assign(edit, {
    code: row.code, name: row.name, tenantCode: row.tenantCode || '', tags: row.tags || '',
    content: d.draftContent || d.content || '',
  })
  editOpen.value = true
}
async function saveEdit() {
  if (!edit.name.trim()) { ElMessage.warning(t('tpl.nameRequired')); return }
  try { JSON.parse(edit.content) } catch { ElMessage.warning(t('tpl.badJson')); return }
  saving.value = true
  try {
    if (edit.code) {
      await updateTemplate(edit.code, { name: edit.name, tenantCode: edit.tenantCode, tags: edit.tags, content: edit.content })
      ElMessage.success(t('common.saved'))
    } else {
      const v = await createTemplate({ name: edit.name, tenantCode: edit.tenantCode || undefined, tags: edit.tags || undefined, content: edit.content })
      ElMessage.success(t('tpl.created', { code: v.code }))
    }
    editOpen.value = false
    load()
  } catch (e: any) {
    ElMessage.error(e?.response?.data?.message || t('common.fail'))
  } finally { saving.value = false }
}

// ---------- 审批流 ----------
async function onSubmit(row: TplView) {
  try {
    const { value } = await ElMessageBox.prompt(t('tpl.submitTip'), t('tpl.submit'), { inputValue: '' })
    await submitTemplate(row.code, value)
    ElMessage.success(t('tpl.submitted')); load()
  } catch (e: any) { if (e !== 'cancel') ElMessage.error(e?.response?.data?.message || t('common.fail')) }
}
async function onApprove(row: TplView) {
  try {
    const { value } = await ElMessageBox.prompt(t('tpl.approveTip'), t('tpl.approve'), { inputValue: '' })
    await approveTemplate(row.code, value)
    ElMessage.success(t('tpl.approved')); load()
  } catch (e: any) { if (e !== 'cancel') ElMessage.error(e?.response?.data?.message || t('common.fail')) }
}
async function onReject(row: TplView) {
  try {
    const { value } = await ElMessageBox.prompt(t('tpl.rejectTip'), t('tpl.reject'), { type: 'warning', inputValue: '' })
    await rejectTemplate(row.code, value)
    ElMessage.success(t('tpl.rejected')); load()
  } catch (e: any) { if (e !== 'cancel') ElMessage.error(e?.response?.data?.message || t('common.fail')) }
}

// ---------- 版本 ----------
const verOpen = ref(false)
const verCode = ref('')
const versions = ref<TplVersion[]>([])
const verStatus: Record<string, string> = { APPROVED: 's-active', PENDING: 's-soon', REJECTED: 's-exp' }
async function openVersions(row: TplView) {
  verCode.value = row.code
  versions.value = await getVersions(row.code)
  verOpen.value = true
}
async function onRollback(v: TplVersion) {
  try {
    await ElMessageBox.confirm(t('tpl.rollbackTip', { v: v.version }), t('tpl.rollback'), { type: 'warning' })
    await rollbackTemplate(verCode.value, v.version)
    ElMessage.success(t('tpl.rolledBack')); verOpen.value = false; load()
  } catch (e: any) { if (e !== 'cancel') ElMessage.error(e?.response?.data?.message || t('common.fail')) }
}

// ---------- 其它操作 ----------
function onExport(row: TplView) { downloadFile(`/tpl/${row.code}/export`, `${row.code}.json`) }
async function onToggle(row: TplView) {
  const disable = row.status !== 'DISABLED'
  try {
    if (disable) await ElMessageBox.confirm(t('tpl.disableTip'), t('tpl.disable'), { type: 'warning' })
    await (disable ? disableTemplate(row.code) : enableTemplate(row.code))
    ElMessage.success(disable ? t('tpl.disabled') : t('tpl.enabled')); load()
  } catch (e: any) { if (e !== 'cancel') ElMessage.error(e?.response?.data?.message || t('common.fail')) }
}
async function onDelete(row: TplView) {
  try {
    await ElMessageBox.confirm(t('tpl.deleteTip', { name: row.name }), t('tpl.del'), { type: 'warning' })
    await deleteTemplate(row.code)
    ElMessage.success(t('tpl.deleted')); load()
  } catch (e: any) { if (e !== 'cancel') ElMessage.error(e?.response?.data?.message || t('common.fail')) }
}

// ---------- 模板库密钥(CmPrint cloudBaseUrl) ----------
const keys = ref<GalleryKey[]>([])
const newTenant = ref('')
async function loadKeys() {
  if (!auth.has('tpl:view')) return
  try { keys.value = await listGalleryKeys() } catch { /* 无权限时静默 */ }
}
function galleryUrl(k: GalleryKey) { return `${location.origin}/pub/cmprint/gallery/${k.galleryKey}` }
async function onEnsureKey() {
  const tc = newTenant.value.trim() || 'PUBLIC'
  await ensureGalleryKey(tc)
  newTenant.value = ''
  ElMessage.success(t('tpl.keyCreated', { t: tc })); loadKeys()
}
async function onResetKey(k: GalleryKey) {
  try {
    await ElMessageBox.confirm(t('tpl.keyResetTip'), t('tpl.keyReset'), { type: 'warning' })
    await resetGalleryKey(k.tenantCode)
    ElMessage.success(t('tpl.keyResetOk')); loadKeys()
  } catch (e: any) { if (e !== 'cancel') ElMessage.error(e?.response?.data?.message || t('common.fail')) }
}
async function copyText(s: string) {
  try { await navigator.clipboard.writeText(s); ElMessage.success(t('portalAdmin.copied')) } catch { /* ignore */ }
}

const STATUSES = ['DRAFT', 'PENDING', 'APPROVED', 'DISABLED']
const canEdit = computed(() => auth.has('tpl:edit'))
const canApprove = computed(() => auth.has('tpl:approve'))
</script>

<template>
  <div class="wrap">
    <PageHelp id="tpl" :title="t('help.tpl.title')"
      :tips="[t('help.tpl.t1'), t('help.tpl.t2'), t('help.tpl.t3')]" />
    <div class="section-title">
      <div>
        <h2>{{ t('nav.tpl') }}</h2>
        <div class="sub" style="margin-top:.3rem">{{ t('tpl.lead') }}</div>
      </div>
      <el-select v-model="filter.status" clearable :placeholder="t('common.status')" style="width:150px" @change="load">
        <el-option v-for="s in STATUSES" :key="s" :label="t('tpl.st.' + s)" :value="s" />
      </el-select>
      <el-input v-model="filter.keyword" :placeholder="t('tpl.search')" clearable style="width:230px" @keyup.enter="load" @clear="load" />
      <el-button v-if="canEdit" type="primary" size="large" @click="openCreate">{{ t('tpl.create') }}</el-button>
    </div>

    <div class="card" style="margin-bottom:1.1rem">
      <el-table :data="rows" v-loading="loading" style="width:100%">
        <el-table-column :label="t('tpl.code')" width="150">
          <template #default="{ row }"><span class="data">{{ row.code }}</span></template>
        </el-table-column>
        <el-table-column :label="t('tpl.name')" min-width="180">
          <template #default="{ row }">
            <b>{{ row.name }}</b>
            <el-tag v-if="row.hasDraftChanges && row.currentVersion > 0" size="small" type="warning" effect="plain" style="margin-left:6px">{{ t('tpl.dirty') }}</el-tag>
            <div class="faint" style="font-size:.75rem">{{ row.tags || '—' }}</div>
          </template>
        </el-table-column>
        <el-table-column :label="t('tpl.tenant')" width="120">
          <template #default="{ row }"><span class="data faint">{{ row.tenantCode || t('tpl.public') }}</span></template>
        </el-table-column>
        <el-table-column :label="t('lic.version')" width="80">
          <template #default="{ row }"><span class="data">v{{ row.currentVersion }}</span></template>
        </el-table-column>
        <el-table-column :label="t('tpl.useCount')" width="80">
          <template #default="{ row }"><span class="data">{{ row.useCount }}</span></template>
        </el-table-column>
        <el-table-column :label="t('th.status')" width="110">
          <template #default="{ row }"><span class="status" :class="statusClass[row.status]"><i></i>{{ t('tpl.st.' + row.status) }}</span></template>
        </el-table-column>
        <el-table-column :label="t('th.op')" min-width="320">
          <template #default="{ row }">
            <button v-if="canEdit && row.status !== 'PENDING'" class="linkbtn" @click="openEdit(row)">{{ t('common.edit') }}</button>
            <button v-if="canEdit && row.status !== 'PENDING' && row.status !== 'DISABLED'" class="linkbtn" style="margin-left:8px" @click="onSubmit(row)">{{ t('tpl.submit') }}</button>
            <template v-if="canApprove && row.status === 'PENDING'">
              <button class="linkbtn" style="margin-left:8px;color:var(--success)" @click="onApprove(row)">{{ t('tpl.approve') }}</button>
              <button class="linkbtn" style="margin-left:8px;color:var(--danger)" @click="onReject(row)">{{ t('tpl.reject') }}</button>
            </template>
            <button class="linkbtn" style="margin-left:8px" @click="openVersions(row)">{{ t('lic.hist') }}</button>
            <button v-if="auth.has('tpl:export')" class="linkbtn" style="margin-left:8px" @click="onExport(row)">{{ t('tpl.export') }}</button>
            <button v-if="canEdit && row.status !== 'PENDING'" class="linkbtn" style="margin-left:8px" @click="onToggle(row)">
              {{ row.status === 'DISABLED' ? t('tpl.enable') : t('tpl.disable') }}
            </button>
            <button v-if="auth.has('tpl:delete')" class="linkbtn" style="margin-left:8px;color:var(--danger)" @click="onDelete(row)">{{ t('tpl.del') }}</button>
          </template>
        </el-table-column>
      </el-table>
      <div v-if="!loading && !rows.length" class="faint" style="text-align:center;padding:2rem 0">{{ t('tpl.empty') }}</div>
    </div>

    <!-- 模板库密钥(CmPrint cloudBaseUrl) -->
    <div class="card">
      <div style="display:flex;align-items:baseline;gap:.8rem;margin-bottom:.8rem">
        <b>{{ t('tpl.keysTitle') }}</b>
        <span class="faint" style="font-size:.78rem">{{ t('tpl.keysSub') }}</span>
        <span style="flex:1"></span>
        <template v-if="canEdit">
          <el-input v-model="newTenant" :placeholder="t('tpl.keyTenantPh')" style="width:200px" size="small" />
          <el-button size="small" type="primary" @click="onEnsureKey">{{ t('tpl.keyCreate') }}</el-button>
        </template>
      </div>
      <div v-for="k in keys" :key="k.tenantCode" class="key-row">
        <span class="data" style="width:110px">{{ k.tenantCode }}</span>
        <code>{{ galleryUrl(k) }}</code>
        <button class="linkbtn" @click="copyText(galleryUrl(k))">{{ t('portalAdmin.copy') }}</button>
        <button v-if="canEdit" class="linkbtn" style="color:var(--danger)" @click="onResetKey(k)">{{ t('tpl.keyReset') }}</button>
      </div>
      <div v-if="!keys.length" class="faint" style="text-align:center;padding:1.2rem 0">{{ t('tpl.keysEmpty') }}</div>
      <div class="notice" style="margin-top:.7rem">🖨 <div>{{ t('tpl.keysNote') }}</div></div>
    </div>

    <!-- 新建/编辑 -->
    <el-dialog v-model="editOpen" :title="edit.code ? t('tpl.editTitle') + ' · ' + edit.code : t('tpl.createTitle')" width="760px" align-center>
      <el-form label-position="top">
        <div class="two">
          <el-form-item :label="t('tpl.name')"><el-input v-model="edit.name" /></el-form-item>
          <el-form-item :label="t('tpl.tenantOpt')"><el-input v-model="edit.tenantCode" class="dataf" :placeholder="t('tpl.tenantPh')" /></el-form-item>
        </div>
        <el-form-item :label="t('tpl.tags')"><el-input v-model="edit.tags" :placeholder="t('tpl.tagsPh')" /></el-form-item>
        <el-form-item :label="t('tpl.content')">
          <el-input v-model="edit.content" type="textarea" :rows="14" class="dataf" spellcheck="false" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="editOpen = false">{{ t('common.cancel') }}</el-button>
        <el-button type="primary" :loading="saving" @click="saveEdit">{{ t('common.save') }}</el-button>
      </template>
    </el-dialog>

    <!-- 版本历史 -->
    <el-drawer v-model="verOpen" :title="t('tpl.verTitle') + ' · ' + verCode" size="46%">
      <div v-for="v in versions" :key="v.version" class="ver-item">
        <div class="ver-head">
          <span class="data" style="font-weight:800">v{{ v.version }}</span>
          <span class="status" :class="verStatus[v.status]"><i></i>{{ t('tpl.vst.' + v.status) }}</span>
          <span class="faint data" style="font-size:.74rem">#{{ v.hash }}</span>
          <span style="flex:1"></span>
          <button v-if="canEdit" class="linkbtn" @click="onRollback(v)">{{ t('tpl.rollback') }}</button>
        </div>
        <div class="faint" style="font-size:.76rem;margin-top:4px">
          {{ t('tpl.submittedBy') }} {{ v.submittedBy || '—' }} · {{ v.createdAt.slice(0, 19).replace('T', ' ') }}
          <template v-if="v.submitNote"> · {{ v.submitNote }}</template>
        </div>
        <div v-if="v.reviewedBy" class="faint" style="font-size:.76rem;margin-top:2px">
          {{ t('tpl.reviewedBy') }} {{ v.reviewedBy }} · {{ (v.reviewedAt || '').slice(0, 19).replace('T', ' ') }}
          <template v-if="v.reviewNote"> · {{ v.reviewNote }}</template>
        </div>
      </div>
      <div v-if="!versions.length" class="faint" style="text-align:center;padding:2rem 0">{{ t('tpl.verEmpty') }}</div>
    </el-drawer>
  </div>
</template>

<style scoped>
.two{display:grid;grid-template-columns:1fr 1fr;gap:.9rem}
.key-row{display:flex;align-items:center;gap:.6rem;font-size:.8rem;padding:.35rem 0;border-bottom:1px dashed var(--border)}
.key-row code{background:var(--surface-2);border:1px solid var(--border);border-radius:6px;padding:.2rem .5rem;flex:1;font-family:var(--font-data);word-break:break-all}
.notice{display:flex;gap:.6rem;align-items:flex-start;background:color-mix(in srgb,var(--brand) 9%,transparent);
  border:1px solid color-mix(in srgb,var(--brand) 22%,transparent);border-radius:12px;padding:.7rem .9rem;font-size:.82rem}
.ver-item{padding:.7rem .2rem;border-bottom:1px solid var(--border)}
.ver-head{display:flex;align-items:center;gap:.7rem}
:deep(.dataf .el-input__inner),:deep(.dataf .el-textarea__inner){font-family:var(--font-data);color:var(--data-ink)}
</style>
