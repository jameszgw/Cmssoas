<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useI18n } from 'vue-i18n'
import { ElMessage, ElMessageBox } from 'element-plus'
import PageHelp from '@/components/PageHelp.vue'
import { useAuthStore } from '@/stores/auth'
import { downloadFile } from '@/api/request'
import {
  listNotices, createNotice, updateNotice, publishNotice, listConsents,
  type Notice, type Consent,
} from '@/api/notices'

const { t } = useI18n()
const auth = useAuthStore()
const canEdit = () => auth.has('notice:edit')
const canConsent = () => auth.has('consent:view')

const tab = ref<'notices' | 'consents'>('notices')
const notices = ref<Notice[]>([])
const consents = ref<Consent[]>([])
const loading = ref(false)

const types = ['TERMS', 'PRIVACY', 'NOTICE', 'ANNOUNCEMENT']
const statusClass: Record<string, string> = { DRAFT: 's-soon', PUBLISHED: 's-active', ARCHIVED: 's-exp' }
const fmt = (s: string | null) => (s ? s.slice(0, 19).replace('T', ' ') : '—')

async function load() {
  loading.value = true
  try {
    notices.value = await listNotices()
    if (canConsent()) consents.value = await listConsents()
  } finally { loading.value = false }
}
onMounted(load)

const publishedCount = computed(() => notices.value.filter((n) => n.status === 'PUBLISHED').length)
const forceCount = computed(() => notices.value.filter((n) => n.status === 'PUBLISHED' && n.forceAck).length)

// ---- 编辑弹层 ----
const dlg = ref(false)
const editing = ref<Notice | null>(null)
const form = ref({ type: 'TERMS', title: '', contentHtml: '', forceAck: false })

function openCreate() {
  editing.value = null
  form.value = { type: 'TERMS', title: '', contentHtml: '', forceAck: false }
  dlg.value = true
}
function openEdit(n: Notice) {
  editing.value = n
  form.value = { type: n.type, title: n.title, contentHtml: n.contentHtml, forceAck: n.forceAck }
  dlg.value = true
}
async function save() {
  if (!form.value.title.trim() || !form.value.contentHtml.trim()) {
    ElMessage.warning(t('notice.required')); return
  }
  try {
    if (editing.value) {
      await updateNotice(editing.value.id, { title: form.value.title, contentHtml: form.value.contentHtml, forceAck: form.value.forceAck })
    } else {
      await createNotice({ ...form.value })
    }
    ElMessage.success(t('common.saved')); dlg.value = false; load()
  } catch (e: any) { ElMessage.error(e?.response?.data?.message || t('common.fail')) }
}
async function publish(n: Notice) {
  try {
    await ElMessageBox.confirm(t('notice.publishConfirm', { v: n.version }), t('notice.publish'), { type: 'warning' })
    await publishNotice(n.id); ElMessage.success(t('notice.publishedOk')); load()
  } catch (e: any) { if (e !== 'cancel') ElMessage.error(e?.response?.data?.message || t('common.fail')) }
}

// ---- 预览 ----
const preview = ref(false)
const previewNotice = ref<Notice | null>(null)
function openPreview(n: Notice) { previewNotice.value = n; preview.value = true }

function exportConsents() { downloadFile('/notices/consents/export.csv', 'consents.csv') }
</script>

<template>
  <div class="wrap">
    <PageHelp id="notice" :title="t('notice.help.title')"
      :tips="[t('notice.help.t1'), t('notice.help.t2'), t('notice.help.t3'), t('notice.help.t4')]" />

    <div class="section-title">
      <div>
        <h2>{{ t('nav.notice') }}</h2>
        <div class="sub" style="margin-top:.3rem">{{ t('notice.lead') }}</div>
      </div>
      <div style="display:flex;gap:.6rem">
        <el-button v-if="tab === 'notices' && canEdit()" type="primary" @click="openCreate">＋ {{ t('notice.create') }}</el-button>
        <el-button v-if="tab === 'consents' && canConsent()" @click="exportConsents">⬇ {{ t('common.exportCsv') }}</el-button>
      </div>
    </div>

    <section class="grid kpis" style="grid-template-columns:repeat(3,1fr);margin-bottom:1.1rem">
      <div class="card kpi"><div class="top"><span class="label">{{ t('notice.kPublished') }}</span><span class="ic">📋</span></div>
        <div class="val data">{{ publishedCount }}</div></div>
      <div class="card kpi"><div class="top"><span class="label">{{ t('notice.kForce') }}</span><span class="ic">🔒</span></div>
        <div class="val data">{{ forceCount }}</div></div>
      <div class="card kpi"><div class="top"><span class="label">{{ t('notice.kConsent') }}</span><span class="ic">✍️</span></div>
        <div class="val data">{{ consents.length }}</div></div>
    </section>

    <el-tabs v-model="tab" style="margin-bottom:.4rem">
      <el-tab-pane :label="t('notice.tabNotices')" name="notices" />
      <el-tab-pane v-if="canConsent()" :label="t('notice.tabConsents')" name="consents" />
    </el-tabs>

    <!-- 须知管理 -->
    <div v-show="tab === 'notices'" class="card">
      <el-table :data="notices" v-loading="loading" style="width:100%" max-height="600">
        <el-table-column :label="t('notice.colType')" width="130">
          <template #default="{ row }"><span class="tag">{{ t('notice.type.' + row.type) }}</span></template>
        </el-table-column>
        <el-table-column :label="t('notice.colTitle')" min-width="220">
          <template #default="{ row }">
            <b>{{ row.title }}</b>
            <span v-if="row.forceAck" class="status s-soon" style="margin-left:.5rem"><i></i>{{ t('notice.force') }}</span>
          </template>
        </el-table-column>
        <el-table-column :label="t('notice.colVersion')" width="90">
          <template #default="{ row }"><span class="data">v{{ row.version }}</span></template>
        </el-table-column>
        <el-table-column :label="t('th.status')" width="110">
          <template #default="{ row }"><span class="status" :class="statusClass[row.status]"><i></i>{{ t('notice.s.' + row.status) }}</span></template>
        </el-table-column>
        <el-table-column :label="t('notice.colEffective')" width="170">
          <template #default="{ row }"><span class="data faint">{{ fmt(row.effectiveAt) }}</span></template>
        </el-table-column>
        <el-table-column :label="t('th.op')" width="200">
          <template #default="{ row }">
            <button class="linkbtn" @click="openPreview(row)">{{ t('notice.preview') }}</button>
            <button v-if="row.status === 'DRAFT' && canEdit()" class="linkbtn" style="margin-left:.7rem" @click="openEdit(row)">{{ t('common.edit') }}</button>
            <button v-if="row.status === 'DRAFT' && canEdit()" class="linkbtn" style="margin-left:.7rem" @click="publish(row)">{{ t('notice.publish') }}</button>
          </template>
        </el-table-column>
      </el-table>
      <div v-if="!loading && !notices.length" class="faint" style="text-align:center;padding:2.5rem 0">{{ t('notice.empty') }}</div>
    </div>

    <!-- 授权记录 -->
    <div v-show="tab === 'consents'" class="card">
      <el-table :data="consents" v-loading="loading" style="width:100%" max-height="600">
        <el-table-column :label="t('notice.cSubject')" min-width="160">
          <template #default="{ row }"><b>{{ row.subject }}</b></template>
        </el-table-column>
        <el-table-column :label="t('notice.colType')" width="130">
          <template #default="{ row }"><span class="tag">{{ t('notice.type.' + row.noticeType) }}</span> <span class="data faint">v{{ row.version }}</span></template>
        </el-table-column>
        <el-table-column :label="t('notice.cAction')" width="110">
          <template #default="{ row }"><span class="status" :class="row.action === 'GRANTED' ? 's-active' : 's-exp'"><i></i>{{ t('notice.a.' + row.action) }}</span></template>
        </el-table-column>
        <el-table-column :label="t('notice.cChannel')" width="90">
          <template #default="{ row }"><span class="faint">{{ row.channel }}</span></template>
        </el-table-column>
        <el-table-column label="IP" width="140">
          <template #default="{ row }"><span class="data faint">{{ row.ip || '—' }}</span></template>
        </el-table-column>
        <el-table-column :label="t('audit.time')" width="180">
          <template #default="{ row }"><span class="data">{{ fmt(row.createdAt) }}</span></template>
        </el-table-column>
      </el-table>
      <div v-if="!loading && !consents.length" class="faint" style="text-align:center;padding:2.5rem 0">{{ t('notice.consentEmpty') }}</div>
    </div>

    <!-- 编辑弹层 -->
    <el-dialog v-model="dlg" :title="editing ? t('notice.editTitle') : t('notice.create')" width="640px">
      <el-form label-width="92px">
        <el-form-item :label="t('notice.colType')">
          <el-select v-model="form.type" :disabled="!!editing" style="width:220px">
            <el-option v-for="ty in types" :key="ty" :label="t('notice.type.' + ty)" :value="ty" />
          </el-select>
        </el-form-item>
        <el-form-item :label="t('notice.colTitle')">
          <el-input v-model="form.title" :placeholder="t('notice.titlePh')" />
        </el-form-item>
        <el-form-item :label="t('notice.content')">
          <el-input v-model="form.contentHtml" type="textarea" :rows="10" :placeholder="t('notice.contentPh')" />
          <div class="faint" style="font-size:.74rem;margin-top:.3rem">{{ t('notice.contentHint') }}</div>
        </el-form-item>
        <el-form-item :label="t('notice.force')">
          <el-switch v-model="form.forceAck" />
          <span class="faint" style="font-size:.76rem;margin-left:.6rem">{{ t('notice.forceHint') }}</span>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dlg = false">{{ t('common.cancel') }}</el-button>
        <el-button type="primary" @click="save">{{ t('common.save') }}</el-button>
      </template>
    </el-dialog>

    <!-- 预览 -->
    <el-dialog v-model="preview" :title="previewNotice?.title" width="640px">
      <div v-if="previewNotice" class="faint" style="font-size:.78rem;margin-bottom:.6rem">
        {{ t('notice.type.' + previewNotice.type) }} · v{{ previewNotice.version }} · {{ t('notice.s.' + previewNotice.status) }}
      </div>
      <div class="notice-body" v-html="previewNotice?.contentHtml"></div>
    </el-dialog>
  </div>
</template>

<style scoped>
.notice-body{font-size:.9rem;line-height:1.7;color:var(--text);white-space:pre-wrap;max-height:52vh;overflow:auto}
.linkbtn:disabled{opacity:.4;cursor:not-allowed}
</style>
