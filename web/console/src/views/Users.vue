<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useI18n } from 'vue-i18n'
import { ElMessage, ElMessageBox } from 'element-plus'
import PageHelp from '@/components/PageHelp.vue'
import { useAuthStore } from '@/stores/auth'
import { listUsers, createUser, resetUserPwd, toggleUser, type UserView } from '@/api/user'
import { getRoles, type RoleView } from '@/api/rbac'

const { t } = useI18n()
const auth = useAuthStore()
const rows = ref<UserView[]>([])
const roles = ref<RoleView[]>([])
const loading = ref(false)
const canEdit = () => auth.has('user:edit')

const statusClass: Record<string, string> = { ACTIVE: 's-active', DISABLED: 's-exp' }

async function load() {
  loading.value = true
  try { [rows.value, roles.value] = await Promise.all([listUsers(), getRoles()]) }
  finally { loading.value = false }
}
onMounted(load)

const open = ref(false)
const submitting = ref(false)
const form = reactive({ username: '', roleId: undefined as number | undefined, password: '' })
function openCreate() { form.username = ''; form.roleId = roles.value[0]?.id; form.password = ''; open.value = true }
async function submit() {
  if (!form.username || !form.roleId) { ElMessage.warning(t('users.required')); return }
  submitting.value = true
  try {
    await createUser({ username: form.username, roleId: form.roleId, password: form.password || undefined })
    ElMessage.success(t('users.created'))
    open.value = false; load()
  } catch (e: any) {
    ElMessage.error(e?.response?.data?.message || t('users.createFail'))
  } finally { submitting.value = false }
}
async function reset(row: UserView) {
  try {
    await ElMessageBox.confirm(t('users.resetTip', { u: row.username }), t('users.reset'), { type: 'warning' })
    const r = await resetUserPwd(row.id)
    ElMessage.success(t('users.resetOk', { p: r.defaultPassword })); load()
  } catch { /* cancel */ }
}
async function toggle(row: UserView) {
  await toggleUser(row.id); load()
}
</script>

<template>
  <div class="wrap">
    <PageHelp id="users" :title="t('users.title')" :tips="[t('users.t1'), t('users.t2'), t('users.t3')]" />
    <div class="section-title">
      <div><h2>{{ t('nav.users') }}</h2><div class="sub" style="margin-top:.3rem">{{ t('users.lead') }}</div></div>
      <el-button type="primary" :disabled="!canEdit()" @click="openCreate">{{ t('users.create') }}</el-button>
    </div>

    <div class="card">
      <el-table :data="rows" v-loading="loading" style="width:100%">
        <el-table-column :label="t('login.username')" min-width="160">
          <template #default="{ row }"><b>{{ row.username }}</b></template>
        </el-table-column>
        <el-table-column :label="t('users.role')" width="180">
          <template #default="{ row }">{{ row.roleName }} <span class="data faint" style="font-size:.72rem">{{ row.roleCode }}</span></template>
        </el-table-column>
        <el-table-column :label="t('th.status')" width="120">
          <template #default="{ row }"><span class="status" :class="statusClass[row.status]"><i></i>{{ row.status === 'ACTIVE' ? t('st.active') : t('users.disabled') }}</span></template>
        </el-table-column>
        <el-table-column :label="t('users.mustChange')" width="120">
          <template #default="{ row }"><span class="data">{{ row.mustChangePwd ? t('common.confirm') : '—' }}</span></template>
        </el-table-column>
        <el-table-column :label="t('th.expire')" width="180">
          <template #default="{ row }"><span class="data">{{ row.createdAt.slice(0,19).replace('T',' ') }}</span></template>
        </el-table-column>
        <el-table-column :label="t('th.op')" width="180">
          <template #default="{ row }">
            <button class="linkbtn" :disabled="!canEdit()" @click="reset(row)">{{ t('users.reset') }}</button>
            <button class="linkbtn" style="margin-left:10px" :class="{ danger: row.status==='ACTIVE' }"
              :disabled="!canEdit() || row.username==='admin'" @click="toggle(row)">
              {{ row.status === 'ACTIVE' ? t('users.disable') : t('users.enable') }}
            </button>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <el-dialog v-model="open" :title="t('users.create')" width="460px" align-center>
      <el-form label-position="top">
        <el-form-item :label="t('login.username')"><el-input v-model="form.username" /></el-form-item>
        <el-form-item :label="t('users.role')">
          <el-select v-model="form.roleId" style="width:100%">
            <el-option v-for="r in roles" :key="r.id" :label="r.name + ' (' + r.code + ')'" :value="r.id" />
          </el-select>
        </el-form-item>
        <el-form-item :label="t('users.initPwd')">
          <el-input v-model="form.password" :placeholder="t('users.initPwdHint')" />
        </el-form-item>
        <div class="notice">🔐 <div>{{ t('users.note') }}</div></div>
      </el-form>
      <template #footer>
        <el-button @click="open = false">{{ t('common.cancel') }}</el-button>
        <el-button type="primary" :loading="submitting" @click="submit">{{ t('users.create') }}</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.linkbtn.danger{color:var(--danger)}
.linkbtn:disabled{opacity:.4;cursor:not-allowed}
.notice{display:flex;gap:.6rem;align-items:flex-start;background:color-mix(in srgb,var(--brand) 9%,transparent);
  border:1px solid color-mix(in srgb,var(--brand) 22%,transparent);border-radius:12px;padding:.7rem .9rem;font-size:.82rem;margin-top:.4rem}
</style>
