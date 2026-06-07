<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import ThemeSwitcher from '@/components/ThemeSwitcher.vue'
import LangSwitcher from '@/components/LangSwitcher.vue'
import { portalOverview, clearPortalToken, getPortalToken, type PortalOverview } from '@/api/portal'

const { t } = useI18n()
const router = useRouter()
const data = ref<PortalOverview | null>(null)
const loading = ref(false)
const tab = ref('licenses')
const money = (n: number) => '¥' + (n || 0).toLocaleString('en-US')

const licStatus: Record<string, string> = { ACTIVE: 's-active', EXPIRED: 's-exp', REVOKED: 's-exp' }

async function load() {
  if (!getPortalToken()) { router.replace('/portal'); return }
  loading.value = true
  try { data.value = await portalOverview() }
  catch { clearPortalToken(); router.replace('/portal') }
  finally { loading.value = false }
}
onMounted(load)

function logout() { clearPortalToken(); router.replace('/portal') }
</script>

<template>
  <div class="phome">
    <header class="ptop">
      <div class="brand"><span class="logo">C</span><span>CODEMAN <small>{{ t('portal.subtitle') }}</small></span></div>
      <div class="spacer"></div>
      <ThemeSwitcher /><LangSwitcher />
      <div class="tenant" v-if="data">{{ data.tenantName }} <span class="data faint">{{ data.tenantCode }}</span></div>
      <button class="logout" @click="logout">🚪 {{ t('portal.logout') }}</button>
    </header>

    <main class="wrap" v-loading="loading">
      <div class="section-title">
        <div>
          <h2>{{ t('portal.myServices') }}</h2>
          <div class="sub" style="margin-top:.3rem">{{ t('portal.homeLead') }}</div>
        </div>
      </div>

      <section v-if="data" class="grid kpis" style="grid-template-columns:repeat(4,1fr);margin-bottom:1.1rem">
        <div class="card kpi"><div class="top"><span class="label">{{ t('portal.kLicense') }}</span><span class="ic">🔑</span></div>
          <div class="val data">{{ data.kpi.licenseCount }}<small style="font-size:.7rem;color:var(--faint)">/{{ data.kpi.activeLicenses }}{{ t('portal.activeShort') }}</small></div></div>
        <div class="card kpi"><div class="top"><span class="label">{{ t('portal.kContract') }}</span><span class="ic">📝</span></div>
          <div class="val data">{{ data.kpi.contractCount }}</div></div>
        <div class="card kpi"><div class="top"><span class="label">{{ t('portal.kSub') }}</span><span class="ic">📦</span></div>
          <div class="val data">{{ data.kpi.subscriptionCount }}</div></div>
        <div class="card kpi"><div class="top"><span class="label">{{ t('portal.kPending') }}</span><span class="ic">🧾</span></div>
          <div class="val data">{{ money(data.kpi.pendingAmount) }}</div></div>
      </section>

      <div v-if="data" class="card">
        <el-tabs v-model="tab">
          <el-tab-pane :label="t('portal.tabLicense') + ' (' + data.licenses.length + ')'" name="licenses">
            <el-table :data="data.licenses" max-height="460">
              <el-table-column :label="t('customer.cLicId')" min-width="180"><template #default="{ row }"><span class="data">{{ row.licenseId }}</span></template></el-table-column>
              <el-table-column :label="t('common.plan')" width="140"><template #default="{ row }">{{ row.edition || row.productCode }}</template></el-table-column>
              <el-table-column :label="t('th.status')" width="110"><template #default="{ row }"><span class="status" :class="licStatus[row.status] || 's-soon'"><i></i>{{ row.status }}</span></template></el-table-column>
              <el-table-column :label="t('th.expire')" width="140"><template #default="{ row }"><span class="data faint">{{ row.notAfter }}</span></template></el-table-column>
            </el-table>
          </el-tab-pane>
          <el-tab-pane :label="t('portal.tabContract') + ' (' + data.contracts.length + ')'" name="contracts">
            <el-table :data="data.contracts" max-height="460">
              <el-table-column :label="t('contract.colNo')" min-width="200"><template #default="{ row }">{{ row.title }}<br><span class="data faint" style="font-size:.7rem">{{ row.contractNo || '—' }}</span></template></el-table-column>
              <el-table-column :label="t('billing.amount')" width="120"><template #default="{ row }"><span class="data">{{ money(row.amount) }}</span></template></el-table-column>
              <el-table-column :label="t('th.status')" width="110"><template #default="{ row }"><span class="status" :class="row.status === 'SIGNED' ? 's-active' : 's-soon'"><i></i>{{ t('contract.s.' + row.status) }}</span></template></el-table-column>
            </el-table>
          </el-tab-pane>
          <el-tab-pane :label="t('portal.tabInvoice') + ' (' + data.invoices.length + ')'" name="invoices">
            <el-table :data="data.invoices" max-height="460">
              <el-table-column :label="t('common.plan')" width="160"><template #default="{ row }">{{ row.planCode }} {{ row.type }}</template></el-table-column>
              <el-table-column :label="t('billing.amount')" width="120"><template #default="{ row }"><span class="data">{{ money(row.amount) }}</span></template></el-table-column>
              <el-table-column :label="t('th.status')" width="110"><template #default="{ row }"><span class="status" :class="row.status === 'PENDING' ? 's-soon' : 's-active'"><i></i>{{ t('billing.s.' + row.status) }}</span></template></el-table-column>
              <el-table-column :label="t('billing.invoiceNo')" min-width="170"><template #default="{ row }"><span class="data faint">{{ row.invoiceNo || '—' }}</span></template></el-table-column>
            </el-table>
          </el-tab-pane>
          <el-tab-pane :label="t('portal.tabSub') + ' (' + data.subscriptions.length + ')'" name="subs">
            <el-table :data="data.subscriptions" max-height="460">
              <el-table-column :label="t('common.plan')" width="160"><template #default="{ row }">{{ row.planCode }} ×{{ row.qty }}</template></el-table-column>
              <el-table-column :label="t('th.status')" width="110"><template #default="{ row }"><span class="status s-active"><i></i>{{ row.status }}</span></template></el-table-column>
              <el-table-column :label="t('th.expire')" width="140"><template #default="{ row }"><span class="data faint">{{ row.endAt }}</span></template></el-table-column>
            </el-table>
          </el-tab-pane>
        </el-tabs>
      </div>
    </main>
  </div>
</template>

<style scoped>
.phome{min-height:100vh}
.ptop{position:sticky;top:0;z-index:30;display:flex;align-items:center;gap:.7rem;padding:.8rem clamp(28px,2.6vw,80px);
  background:var(--topbar);color:#fff;box-shadow:0 10px 30px -16px rgba(0,0,0,.45)}
.ptop .brand{display:flex;align-items:center;gap:.6rem;font-weight:800;font-size:1.1rem}
.ptop .brand .logo{width:2rem;height:2rem;border-radius:9px;display:grid;place-items:center;background:rgba(255,255,255,.18);font-weight:900}
.ptop .brand small{font-size:.62rem;opacity:.8;font-weight:600;display:block}
.spacer{flex:1}
.tenant{font-size:.84rem;font-weight:700;margin:0 .4rem}
.logout{border:0;background:rgba(255,255,255,.16);color:#fff;border-radius:10px;padding:.45rem .8rem;cursor:pointer;font-size:.82rem;font-weight:600}
</style>
