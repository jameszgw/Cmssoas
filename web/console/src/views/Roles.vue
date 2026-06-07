<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { useI18n } from 'vue-i18n'
import { ElMessage } from 'element-plus'
import PageHelp from '@/components/PageHelp.vue'
import { useAuthStore } from '@/stores/auth'
import {
  getPermissionTree, getRoles, getRole, setRolePermissions,
  type PermNode, type RoleView,
} from '@/api/rbac'

const { t } = useI18n()
const auth = useAuthStore()

const roles = ref<RoleView[]>([])
const tree = ref<PermNode[]>([])
const current = ref<RoleView | null>(null)
const modes = reactive<Record<string, string>>({})
const saving = ref(false)

const MODE_OPTS = computed(() => [
  { label: t('roles.m.NONE'), value: 'NONE' },
  { label: t('roles.m.VIEW'), value: 'VIEW' },
  { label: t('roles.m.EDIT'), value: 'EDIT' },
  { label: t('roles.m.FULL'), value: 'FULL' },
])
const MODE_LABEL = computed<Record<string, string>>(() => ({
  NONE: t('roles.m.NONE'), VIEW: t('roles.m.VIEW'), EDIT: t('roles.m.EDIT'), FULL: t('roles.m.FULL'),
}))

function flatCodes(nodes: PermNode[], acc: string[] = []): string[] {
  for (const n of nodes) { acc.push(n.code); if (n.children?.length) flatCodes(n.children, acc) }
  return acc
}

async function selectRole(r: RoleView) {
  current.value = r
  const detail = await getRole(r.id)
  for (const c of flatCodes(tree.value)) modes[c] = detail.modes[c] || 'NONE'
}

/** 多态级联：设置某节点后，其所有子孙同步为相同状态。 */
function cascade(node: PermNode, val: string) {
  modes[node.code] = val
  for (const c of node.children || []) cascade(c, val)
}

async function save() {
  if (!current.value) return
  saving.value = true
  try {
    const items = flatCodes(tree.value).map((code) => ({ code, mode: modes[code] || 'NONE' }))
    await setRolePermissions(current.value.id, items)
    ElMessage.success(t('roles.saved'))
  } catch (e: any) {
    ElMessage.error(e?.response?.data?.message || t('roles.saveFail'))
  } finally { saving.value = false }
}

onMounted(async () => {
  ;[roles.value, tree.value] = await Promise.all([getRoles(), getPermissionTree()])
  for (const c of flatCodes(tree.value)) modes[c] = 'NONE'
  if (roles.value.length) selectRole(roles.value[0])
})
</script>

<template>
  <div class="wrap">
    <PageHelp id="roles" :title="t('roles.title')"
      :tips="[t('roles.t1'), t('roles.t2'), t('roles.t3')]" />
    <div class="section-title">
      <div><h2>{{ t('nav.system') }}</h2><div class="sub" style="margin-top:.3rem">{{ t('roles.lead') }}</div></div>
      <el-button type="primary" :loading="saving" :disabled="!auth.isSuper()" @click="save">{{ t('roles.save') }}</el-button>
    </div>

    <section class="grid" style="grid-template-columns:280px 1fr;gap:1.1rem">
      <!-- 角色列表 -->
      <div class="card">
        <div class="card-head"><div><h3>👥 {{ t('roles.roleList') }}</h3></div></div>
        <div class="roles">
          <div v-for="r in roles" :key="r.id" class="role" :class="{ on: current?.id === r.id }" @click="selectRole(r)">
            <div class="rname">{{ r.name }}</div>
            <div class="rcode data faint">{{ r.code }}</div>
            <div class="rdesc">{{ r.description }}</div>
          </div>
        </div>
      </div>

      <!-- 权限树（多态选择）-->
      <div class="card">
        <div class="card-head">
          <div><h3>🌳 {{ t('roles.permTree') }}</h3>
            <div class="sub">{{ t('roles.permSub') }}</div></div>
          <div class="legend">
            <span v-for="o in MODE_OPTS" :key="o.value" class="lg-item" :class="'m-' + o.value">{{ o.label }}</span>
          </div>
        </div>
        <el-alert v-if="!auth.isSuper()" :title="t('roles.readonly')" type="info" :closable="false" show-icon style="margin-bottom:12px" />
        <el-tree :data="tree" node-key="code" default-expand-all :expand-on-click-node="false" :indent="22">
          <template #default="{ data }">
            <div class="permnode">
              <span class="pn-name" :class="{ menu: data.type === 'MENU' }">
                {{ data.name }}
                <span class="pn-cur" :class="'m-' + (modes[data.code] || 'NONE')">{{ MODE_LABEL[modes[data.code] || 'NONE'] }}</span>
              </span>
              <el-segmented
                :model-value="modes[data.code] || 'NONE'"
                :options="MODE_OPTS" size="small" :disabled="!auth.isSuper()"
                @change="(v: string) => cascade(data, v)"
              />
            </div>
          </template>
        </el-tree>
      </div>
    </section>
  </div>
</template>

<style scoped>
.roles{display:flex;flex-direction:column;gap:.6rem}
.role{border:1px solid var(--border);border-radius:12px;padding:.8rem .9rem;cursor:pointer;transition:.15s}
.role:hover{background:var(--surface-2)}
.role.on{border-color:var(--brand);background:var(--surface-2);box-shadow:0 8px 20px -14px var(--ring)}
.rname{font-weight:700}
.rcode{font-size:.72rem}
.rdesc{font-size:.78rem;color:var(--muted);margin-top:.2rem}
.legend{display:flex;gap:.4rem}
.lg-item{font-size:.68rem;font-weight:700;padding:.12rem .45rem;border-radius:6px}
.m-NONE{background:var(--surface-3);color:var(--muted)}
.m-VIEW{background:color-mix(in srgb,var(--brand) 14%,transparent);color:var(--brand)}
.m-EDIT{background:color-mix(in srgb,var(--warning) 16%,transparent);color:var(--warning)}
.m-FULL{background:color-mix(in srgb,var(--success) 16%,transparent);color:var(--success)}
.permnode{display:flex;align-items:center;justify-content:space-between;width:100%;padding-right:14px}
.pn-name{font-size:.9rem;display:flex;align-items:center;gap:.5rem}
.pn-name.menu{font-weight:800}
.pn-cur{font-size:.66rem;font-weight:700;padding:.05rem .4rem;border-radius:6px}
:deep(.el-tree-node__content){height:auto;padding:6px 0}
</style>
