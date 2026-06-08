<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useI18n } from 'vue-i18n'
import { ElMessage, ElMessageBox } from 'element-plus'
import PageHelp from '@/components/PageHelp.vue'
import { useAuthStore } from '@/stores/auth'
import { portalAdminList, portalEnable, portalReset, portalDisable, type PortalStatus } from '@/api/portal'

const { t } = useI18n()
const auth = useAuthStore()
const canManage = () => auth.has('tenant:portal')
const rows = ref<PortalStatus[]>([])
const loading = ref(false)

const portalUrl = computed(() => location.origin + '/portal')
const enabledCount = computed(() => rows.value.filter((r) => r.enabled).length)

async function load() {
  loading.value = true
  try { rows.value = await portalAdminList() } finally { loading.value = false }
}
onMounted(load)

async function enable(r: PortalStatus) {
  try { await portalEnable(r.tenantCode); ElMessage.success(t('portalAdmin.enabledOk')); load() }
  catch (e: any) { ElMessage.error(e?.response?.data?.message || t('common.fail')) }
}
async function reset(r: PortalStatus) {
  try {
    await ElMessageBox.confirm(t('portalAdmin.resetConfirm'), t('portalAdmin.reset'), { type: 'warning' })
    await portalReset(r.tenantCode); ElMessage.success(t('portalAdmin.resetOk')); load()
  } catch (e: any) { if (e !== 'cancel') ElMessage.error(e?.response?.data?.message || t('common.fail')) }
}
async function disable(r: PortalStatus) {
  try {
    await ElMessageBox.confirm(t('portalAdmin.disableConfirm'), t('portalAdmin.disable'), { type: 'warning' })
    await portalDisable(r.tenantCode); ElMessage.success(t('portalAdmin.disabledOk')); load()
  } catch (e: any) { if (e !== 'cancel') ElMessage.error(e?.response?.data?.message || t('common.fail')) }
}
async function copy(r: PortalStatus) {
  const txt = `${t('portal.tenantCode')}: ${r.tenantCode}\n${t('portal.accessCode')}: ${r.accessCode}\n${portalUrl.value}`
  try { await navigator.clipboard.writeText(txt); ElMessage.success(t('portalAdmin.copied')) } catch { /* ignore */ }
}
</script>

<template>
  <div class="wrap">
    <PageHelp id="portalAdmin" :title="t('portalAdmin.help.title')"
      :tips="[t('portalAdmin.help.t1'), t('portalAdmin.help.t2'), t('portalAdmin.help.t3'), t('portalAdmin.help.t4')]" />

    <div class="section-title">
      <div>
        <h2>{{ t('nav.portalAdmin') }}</h2>
        <div class="sub" style="margin-top:.3rem">{{ t('portalAdmin.lead') }}</div>
      </div>
      <div class="status s-init"><i></i>{{ t('portalAdmin.portalUrl') }}: {{ portalUrl }}</div>
    </div>

    <section class="grid kpis" style="grid-template-columns:repeat(3,1fr);margin-bottom:1.1rem">
      <div class="card kpi"><div class="top"><span class="label">{{ t('portalAdmin.kTenants') }}</span><span class="ic">🏢</span></div>
        <div class="val data">{{ rows.length }}</div></div>
      <div class="card kpi"><div class="top"><span class="label">{{ t('portalAdmin.kEnabled') }}</span><span class="ic">🌐</span></div>
        <div class="val data">{{ enabledCount }}</div></div>
      <div class="card kpi"><div class="top"><span class="label">{{ t('portalAdmin.kDisabled') }}</span><span class="ic">🔒</span></div>
        <div class="val data">{{ rows.length - enabledCount }}</div></div>
    </section>

    <div class="card">
      <el-table :data="rows" v-loading="loading" style="width:100%" max-height="600">
        <el-table-column :label="t('th.name')" min-width="200">
          <template #default="{ row }"><b>{{ row.name }}</b> <span class="data faint" style="font-size:.72rem">{{ row.tenantCode }}</span></template>
        </el-table-column>
        <el-table-column :label="t('portalAdmin.status')" width="120">
          <template #default="{ row }"><span class="status" :class="row.enabled ? 's-active' : 's-exp'"><i></i>{{ row.enabled ? t('portalAdmin.on') : t('portalAdmin.off') }}</span></template>
        </el-table-column>
        <el-table-column :label="t('portal.accessCode')" width="160">
          <template #default="{ row }"><span class="data" style="letter-spacing:1px">{{ row.enabled && row.accessCode ? row.accessCode : '—' }}</span></template>
        </el-table-column>
        <el-table-column :label="t('th.op')" min-width="240">
          <template #default="{ row }">
            <button v-if="!row.enabled" class="linkbtn" :disabled="!canManage()" @click="enable(row)">{{ t('portalAdmin.enable') }}</button>
            <template v-else>
              <button class="linkbtn" @click="copy(row)">{{ t('portalAdmin.copy') }}</button>
              <button class="linkbtn" style="margin-left:.7rem" :disabled="!canManage()" @click="reset(row)">{{ t('portalAdmin.reset') }}</button>
              <button class="linkbtn" style="margin-left:.7rem" :disabled="!canManage()" @click="disable(row)">{{ t('portalAdmin.disable') }}</button>
            </template>
          </template>
        </el-table-column>
      </el-table>
      <div v-if="!loading && !rows.length" class="faint" style="text-align:center;padding:2.5rem 0">{{ t('portalAdmin.empty') }}</div>
    </div>
  </div>
</template>

<style scoped>
.linkbtn:disabled{opacity:.4;cursor:not-allowed}
</style>
