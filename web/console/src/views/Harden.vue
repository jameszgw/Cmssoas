<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useI18n } from 'vue-i18n'
import { ElMessage } from 'element-plus'
import PageHelp from '@/components/PageHelp.vue'
import { useAuthStore } from '@/stores/auth'
import { downloadFile } from '@/api/request'
import {
  getHardenConfig, saveHardenConfig, listHardenJobs, submitHardenJob,
  type HardenConfig, type HardenJob,
} from '@/api/harden'

const { t } = useI18n()
const auth = useAuthStore()
const canConfig = () => auth.has('harden:config')
const canRun = () => auth.has('harden:run')

const fmt = (s: string | null) => (s ? s.slice(0, 19).replace('T', ' ') : '—')
const kb = (n: number | null) => (n == null ? '—' : (n / 1024).toFixed(1) + ' KB')
const statusClass: Record<string, string> = { QUEUED: 's-soon', RUNNING: 's-init', DONE: 's-active', FAILED: 's-exp' }

// ---- 设置(租户级) ----
const cfgTenant = ref('')
const cfg = ref<HardenConfig>({ tenantCode: '', mode: 'BUILD', obfuscate: true, encryptBind: false, fatjarEncrypt: false })
async function loadCfg() { cfg.value = await getHardenConfig(cfgTenant.value) }
async function saveCfg() {
  try { cfg.value = await saveHardenConfig({ ...cfg.value, tenantCode: cfgTenant.value }); ElMessage.success(t('common.saved')) }
  catch (e: any) { ElMessage.error(e?.response?.data?.message || t('common.fail')) }
}

// ---- 任务 ----
const jobs = ref<HardenJob[]>([])
const loading = ref(false)
async function loadJobs() { loading.value = true; try { jobs.value = await listHardenJobs() } finally { loading.value = false } }
onMounted(() => { loadCfg(); loadJobs() })

// ---- 提交 ----
const file = ref<File | null>(null)
const techs = ref<string[]>(['OBFUSCATE'])
const tenantCode = ref('')
const bindLicense = ref('')
const passphrase = ref('')
const encryptPrefix = ref('')
const submitting = ref(false)
const needBind = computed(() => techs.value.includes('ENCRYPT_BIND'))
const needPass = computed(() => techs.value.includes('FATJAR_ENCRYPT'))
function onFile(e: Event) { file.value = (e.target as HTMLInputElement).files?.[0] || null }

async function submit() {
  if (!file.value) { ElMessage.warning(t('harden.pickFile')); return }
  if (!techs.value.length) { ElMessage.warning(t('harden.pickTech')); return }
  if (needBind.value && !bindLicense.value.trim()) { ElMessage.warning(t('harden.needBind')); return }
  if (needPass.value && !passphrase.value.trim()) { ElMessage.warning(t('harden.needPass')); return }
  const fd = new FormData()
  fd.append('file', file.value)
  fd.append('techniques', techs.value.join(','))
  if (tenantCode.value.trim()) fd.append('tenantCode', tenantCode.value.trim())
  if (bindLicense.value.trim()) fd.append('bindLicense', bindLicense.value.trim())
  if (passphrase.value.trim()) fd.append('passphrase', passphrase.value.trim())
  if (encryptPrefix.value.trim()) fd.append('encryptPrefix', encryptPrefix.value.trim())
  submitting.value = true
  try { await submitHardenJob(fd); ElMessage.success(t('harden.submitted')); loadJobs() }
  catch (e: any) { ElMessage.error(e?.response?.data?.message || t('common.fail')) }
  finally { submitting.value = false }
}
function download(j: HardenJob) { downloadFile(`/harden/jobs/${j.id}/download`, 'hardened-' + j.sourceName) }
</script>

<template>
  <div class="wrap">
    <PageHelp id="harden" :title="t('harden.help.title')"
      :tips="[t('harden.help.t1'), t('harden.help.t2'), t('harden.help.t3'), t('harden.help.t4')]" />

    <div class="section-title">
      <div>
        <h2>{{ t('nav.harden') }}</h2>
        <div class="sub" style="margin-top:.3rem">{{ t('harden.lead') }}</div>
      </div>
      <el-button @click="loadJobs">↻ {{ t('common.reset') }}</el-button>
    </div>

    <section class="grid" style="grid-template-columns:1fr 1.3fr;gap:1.1rem;margin-bottom:1.1rem">
      <!-- 设置 -->
      <div class="card">
        <h3 style="font-size:1rem;margin-bottom:.2rem">⚙️ {{ t('harden.cfgTitle') }}</h3>
        <div class="sub" style="margin-bottom:.9rem">{{ t('harden.cfgLead') }}</div>
        <el-form label-width="92px">
          <el-form-item :label="t('harden.tenant')"><el-input v-model="cfgTenant" placeholder="留空=平台级" style="width:200px" @blur="loadCfg" /></el-form-item>
          <el-form-item :label="t('harden.mode')">
            <el-radio-group v-model="cfg.mode">
              <el-radio-button value="BUILD">{{ t('harden.modeBuild') }}</el-radio-button>
              <el-radio-button value="ONLINE">{{ t('harden.modeOnline') }}</el-radio-button>
              <el-radio-button value="BOTH">{{ t('harden.modeBoth') }}</el-radio-button>
            </el-radio-group>
          </el-form-item>
          <el-form-item :label="t('harden.defTech')">
            <div>
              <el-checkbox v-model="cfg.obfuscate">{{ t('harden.tObf') }}</el-checkbox>
              <el-checkbox v-model="cfg.encryptBind">{{ t('harden.tBind') }}</el-checkbox>
              <el-checkbox v-model="cfg.fatjarEncrypt">{{ t('harden.tFat') }}</el-checkbox>
            </div>
          </el-form-item>
          <el-button type="primary" :disabled="!canConfig()" @click="saveCfg">{{ t('common.save') }}</el-button>
          <div class="faint" style="font-size:.74rem;margin-top:.5rem">{{ t('harden.cfgNote') }}</div>
        </el-form>
      </div>

      <!-- 提交任务 -->
      <div class="card">
        <h3 style="font-size:1rem;margin-bottom:.2rem">⬆️ {{ t('harden.newTitle') }}</h3>
        <div class="sub" style="margin-bottom:.9rem">{{ t('harden.newLead') }}</div>
        <el-form label-width="92px">
          <el-form-item :label="t('harden.file')"><input type="file" accept=".jar" @change="onFile" /></el-form-item>
          <el-form-item :label="t('harden.tech')">
            <el-checkbox-group v-model="techs">
              <el-checkbox value="OBFUSCATE">{{ t('harden.tObf') }}</el-checkbox>
              <el-checkbox value="ENCRYPT_BIND">{{ t('harden.tBind') }}</el-checkbox>
              <el-checkbox value="FATJAR_ENCRYPT">{{ t('harden.tFat') }}</el-checkbox>
            </el-checkbox-group>
          </el-form-item>
          <el-form-item :label="t('harden.tenant')"><el-input v-model="tenantCode" placeholder="留空=平台级" style="width:200px" /></el-form-item>
          <el-form-item v-if="needBind" :label="t('harden.bindLic')"><el-input v-model="bindLicense" placeholder="LIC-...." style="width:240px" /></el-form-item>
          <el-form-item v-if="needPass" :label="t('harden.pass')"><el-input v-model="passphrase" show-password style="width:240px" /></el-form-item>
          <el-form-item :label="t('harden.prefix')"><el-input v-model="encryptPrefix" :placeholder="t('harden.prefixPh')" style="width:240px" /></el-form-item>
          <el-button type="primary" :loading="submitting" :disabled="!canRun()" @click="submit">⬆ {{ t('harden.submit') }}</el-button>
        </el-form>
      </div>
    </section>

    <div class="card">
      <el-table :data="jobs" v-loading="loading" style="width:100%" max-height="520">
        <el-table-column :label="t('harden.jobNo')" min-width="170">
          <template #default="{ row }"><b>{{ row.sourceName }}</b><br><span class="data faint" style="font-size:.72rem">{{ row.jobNo }}</span></template>
        </el-table-column>
        <el-table-column :label="t('harden.tech')" min-width="200">
          <template #default="{ row }"><span class="faint" style="font-size:.78rem">{{ row.techniques }}</span></template>
        </el-table-column>
        <el-table-column :label="t('th.status')" width="110">
          <template #default="{ row }"><span class="status" :class="statusClass[row.status]"><i></i>{{ t('harden.s.' + row.status) }}</span></template>
        </el-table-column>
        <el-table-column :label="t('harden.outSize')" width="100">
          <template #default="{ row }"><span class="data faint">{{ kb(row.outSize) }}</span></template>
        </el-table-column>
        <el-table-column :label="t('audit.time')" width="170">
          <template #default="{ row }"><span class="data faint">{{ fmt(row.createdAt) }}</span></template>
        </el-table-column>
        <el-table-column :label="t('th.op')" width="120">
          <template #default="{ row }">
            <button v-if="row.status === 'DONE'" class="linkbtn" @click="download(row)">⬇ {{ t('harden.download') }}</button>
            <span v-else-if="row.status === 'FAILED'" class="faint" :title="row.message">✗</span>
          </template>
        </el-table-column>
        <el-table-column type="expand">
          <template #default="{ row }"><div class="faint" style="font-size:.78rem;padding:.4rem 1rem;white-space:pre-wrap">{{ row.message || '—' }}</div></template>
        </el-table-column>
      </el-table>
      <div v-if="!loading && !jobs.length" class="faint" style="text-align:center;padding:2.5rem 0">{{ t('harden.empty') }}</div>
    </div>
  </div>
</template>

<style scoped>
.linkbtn:disabled{opacity:.4;cursor:not-allowed}
</style>
