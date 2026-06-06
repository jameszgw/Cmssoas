<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useI18n } from 'vue-i18n'
import OnboardTenantDialog from '@/components/OnboardTenantDialog.vue'
import { listTenants } from '@/api/tenant'
import type { Tenant } from '@/types'

const { t } = useI18n()
const all = ref<Tenant[]>([])
const keyword = ref('')
const status = ref('')
const showOnboard = ref(false)

const statusClass: Record<string, string> = { active: 's-active', soon: 's-soon', init: 's-init', exp: 's-exp' }

const filtered = computed(() =>
  all.value.filter((x) => {
    const kw = keyword.value.trim().toLowerCase()
    const okKw = !kw || x.name.toLowerCase().includes(kw) || x.code.toLowerCase().includes(kw)
    const okSt = !status.value || x.status === status.value
    return okKw && okSt
  }),
)

async function load() { all.value = await listTenants() }
function onSuccess() { load() }
function reset() { keyword.value = ''; status.value = '' }

onMounted(load)
</script>

<template>
  <div class="wrap">
    <div class="section-title">
      <div>
        <h2>{{ t('tenants.title') }}</h2>
        <div class="sub" style="margin-top:.3rem">{{ t('tenants.lead') }}</div>
      </div>
      <el-button type="primary" size="large" @click="showOnboard = true">{{ t('tenants.onboard') }}</el-button>
    </div>

    <div class="card">
      <div class="toolbar">
        <el-input v-model="keyword" :placeholder="t('tenants.searchPlaceholder')" style="width:300px" clearable />
        <el-select v-model="status" :placeholder="t('common.status')" clearable style="width:170px">
          <el-option :label="t('st.active')" value="active" />
          <el-option :label="t('st.soon')" value="soon" />
          <el-option :label="t('st.init')" value="init" />
          <el-option :label="t('st.exp')" value="exp" />
        </el-select>
        <el-button @click="reset">{{ t('common.reset') }}</el-button>
        <span class="spacer" style="flex:1"></span>
        <span class="faint">{{ t('tenants.count', { n: filtered.length }) }}</span>
      </div>

      <el-table :data="filtered" style="width:100%">
        <el-table-column :label="t('th.code')" width="150">
          <template #default="{ row }"><span class="data">{{ row.code }}</span></template>
        </el-table-column>
        <el-table-column :label="t('th.name')" min-width="200">
          <template #default="{ row }"><span style="font-weight:700">{{ row.name }}</span></template>
        </el-table-column>
        <el-table-column :label="t('th.email')" min-width="220">
          <template #default="{ row }"><span class="data">{{ row.email }}</span></template>
        </el-table-column>
        <el-table-column :label="t('th.plan')" width="180">
          <template #default="{ row }">{{ t(row.plan) }} · <span class="data">{{ row.version }}</span></template>
        </el-table-column>
        <el-table-column :label="t('th.status')" width="130">
          <template #default="{ row }">
            <span class="status" :class="statusClass[row.status]"><i></i>{{ t('st.' + row.status) }}</span>
          </template>
        </el-table-column>
        <el-table-column :label="t('th.expire')" width="140">
          <template #default="{ row }"><span class="data">{{ row.expire }}</span></template>
        </el-table-column>
        <el-table-column :label="t('th.op')" width="120">
          <template #default="{ row }">
            <button class="linkbtn">{{ row.status === 'exp' || row.status === 'soon' ? t('common.renew') : t('common.detail') }}</button>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <OnboardTenantDialog v-model="showOnboard" @success="onSuccess" />
  </div>
</template>
