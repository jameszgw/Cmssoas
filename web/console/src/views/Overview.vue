<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useI18n } from 'vue-i18n'
import { useRouter } from 'vue-router'
import AreaChart from '@/components/charts/AreaChart.vue'
import DonutChart from '@/components/charts/DonutChart.vue'
import BarList from '@/components/charts/BarList.vue'
import OnboardTenantDialog from '@/components/OnboardTenantDialog.vue'
import PageHelp from '@/components/PageHelp.vue'
import { mockTenants, planMix, trendIssue, trendRenew } from '@/api/mock'
import { getStats, getAlerts, type OverviewStats, type OverviewAlert } from '@/api/overview'

const { t } = useI18n()
const router = useRouter()
const recent = mockTenants.slice(0, 5)
const showOnboard = ref(false)

// 真实统计与告警（后端 /api/overview/*）
const stats = ref<OverviewStats>({ licensesTotal: 0, licensesActive: 0, licensesExpireSoon: 0, licensesRevoked: 0, tenants: 0, subscriptions: 0, onlineInstances: 0, graceInstances: 0, seatsUsed: 0 })
const alerts = ref<OverviewAlert[]>([])
const alertClass: Record<string, string> = { danger: 's-exp', warning: 's-soon', info: 's-init' }
async function loadOverview() {
  try { [stats.value, alerts.value] = await Promise.all([getStats(), getAlerts()]) } catch { /* 未登录或无权限时忽略 */ }
}
onMounted(loadOverview)

const modeSegs = [
  { value: 52, color: 'var(--brand)' },
  { value: 31, color: 'var(--brand-2)' },
  { value: 17, color: 'var(--accent)' },
]
const statusClass: Record<string, string> = { active: 's-active', soon: 's-soon', init: 's-init', exp: 's-exp' }
</script>

<template>
  <div class="wrap">
    <PageHelp id="overview" :title="t('help.overview.title')"
      :tips="[t('help.overview.t1'), t('help.overview.t2'), t('help.overview.t3')]" />
    <!-- Hero -->
    <section class="hero">
      <div class="crumbs">{{ t('hero.crumbs') }}</div>
      <h1>{{ t('hero.title') }}</h1>
      <p>{{ t('hero.desc') }}</p>
      <div class="cta">
        <button class="btn primary" @click="showOnboard = true">{{ t('hero.cta1') }}</button>
        <button class="btn ghost" @click="router.push('/licensing')">{{ t('hero.cta2') }}</button>
      </div>
      <div class="stat-inline">
        <div><div class="v">99.97%</div><div class="l">{{ t('hero.s1') }}</div></div>
        <div><div class="v">12,840</div><div class="l">{{ t('hero.s2') }}</div></div>
      </div>
    </section>

    <!-- KPI -->
    <section class="grid kpis" style="margin-bottom:1.1rem">
      <div class="card kpi">
        <div class="top"><span class="label">{{ t('kpi.tenants') }}</span><span class="ic">🏢</span></div>
        <div class="val data">{{ stats.tenants }}</div>
        <div class="foot"><span class="muted">{{ t('kpi.realtime') }}</span></div>
      </div>
      <div class="card kpi">
        <div class="top"><span class="label">{{ t('kpi.active') }}</span><span class="ic">🔑</span></div>
        <div class="val data">{{ stats.licensesActive }}</div>
        <div class="foot"><span class="muted">{{ t('kpi.ofTotal', { n: stats.licensesTotal }) }}</span></div>
      </div>
      <div class="card kpi">
        <div class="top"><span class="label">{{ t('kpi.subs') }}</span><span class="ic">✨</span></div>
        <div class="val data">{{ stats.subscriptions }}</div>
        <div class="foot"><span class="muted">{{ t('kpi.online', { n: stats.onlineInstances }) }}</span></div>
      </div>
      <div class="card kpi">
        <div class="top"><span class="label">{{ t('kpi.expire') }}</span><span class="ic">⏳</span></div>
        <div class="val data">{{ stats.licensesExpireSoon }}</div>
        <div class="foot"><span class="muted">{{ t('kpi.attention') }}</span></div>
      </div>
    </section>

    <!-- 图表行 -->
    <section class="grid cols-3" style="margin-bottom:1.1rem">
      <div class="card">
        <div class="card-head">
          <div><h3>📈 {{ t('chart.trend') }}</h3><div class="sub">{{ t('chart.trendSub') }}</div></div>
          <span class="tag">{{ t('chart.year') }}</span>
        </div>
        <AreaChart :a="trendIssue" :b="trendRenew" />
        <div class="chart-legend">
          <span class="lg"><i style="background:var(--brand)"></i>{{ t('chart.issue') }}</span>
          <span class="lg"><i style="background:var(--brand-2)"></i>{{ t('chart.renew') }}</span>
          <span class="lg muted">x：<span class="data">2025-07 → 2026-06</span></span>
        </div>
      </div>

      <div class="card">
        <div class="card-head"><div><h3>🧩 {{ t('chart.mode') }}</h3><div class="sub">{{ t('chart.modeSub') }}</div></div></div>
        <div style="display:flex;align-items:center;gap:1.2rem;justify-content:center;padding:.4rem 0">
          <DonutChart :segments="modeSegs">
            <div><div class="data" style="font-size:1.5rem;font-weight:800">3,572</div>
              <div class="faint" style="font-size:.68rem">{{ t('chart.total') }}</div></div>
          </DonutChart>
          <div style="display:flex;flex-direction:column;gap:.7rem">
            <span class="lg"><i style="background:var(--brand)"></i>{{ t('mode.hybrid') }} &nbsp;<b class="data">52%</b></span>
            <span class="lg"><i style="background:var(--brand-2)"></i>{{ t('mode.online') }} &nbsp;<b class="data">31%</b></span>
            <span class="lg"><i style="background:var(--accent)"></i>{{ t('mode.offline') }} &nbsp;<b class="data">17%</b></span>
          </div>
        </div>
      </div>

      <div class="card">
        <div class="card-head"><div><h3>📦 {{ t('chart.plan') }}</h3><div class="sub">{{ t('chart.planSub') }}</div></div></div>
        <BarList :items="planMix" />
        <div class="chart-legend" style="margin-top:1.1rem">
          <span class="lg muted">{{ t('chart.arpu') }} <b class="data">¥38,200</b></span>
        </div>
      </div>
    </section>

    <!-- 表格 + 待办 + 快捷 -->
    <section class="grid cols-3b" style="margin-bottom:1.1rem">
      <div class="card">
        <div class="card-head">
          <div><h3>🏢 {{ t('recent.title') }}</h3><div class="sub">{{ t('recent.sub') }}</div></div>
          <button class="linkbtn" @click="router.push('/tenants')">{{ t('common.viewAll') }} →</button>
        </div>
        <el-table :data="recent" style="width:100%">
          <el-table-column :label="t('th.code')" width="140">
            <template #default="{ row }"><span class="data">{{ row.code }}</span></template>
          </el-table-column>
          <el-table-column :label="t('th.name')">
            <template #default="{ row }"><span style="font-weight:700">{{ row.name }}</span></template>
          </el-table-column>
          <el-table-column :label="t('th.plan')" width="170">
            <template #default="{ row }">{{ t(row.plan) }} · <span class="data">{{ row.version }}</span></template>
          </el-table-column>
          <el-table-column :label="t('th.status')" width="120">
            <template #default="{ row }">
              <span class="status" :class="statusClass[row.status]"><i></i>{{ t('st.' + row.status) }}</span>
            </template>
          </el-table-column>
          <el-table-column :label="t('th.expire')" width="130">
            <template #default="{ row }"><span class="data">{{ row.expire }}</span></template>
          </el-table-column>
        </el-table>
      </div>

      <div class="card">
        <div class="card-head"><div><h3>📌 {{ t('todo.title') }}</h3><div class="sub">{{ t('todo.sub') }}</div></div></div>
        <div class="todo">
          <div v-for="(a, i) in alerts" :key="i" class="item">
            <div class="badge" :class="a.level === 'danger' ? 'b-warn' : a.level === 'warning' ? 'b-warn' : 'b-info'">
              {{ a.type === 'HEARTBEAT' ? '📡' : a.type === 'EXPIRED' ? '⛔' : '⏳' }}
            </div>
            <div><div class="ttl">{{ t('todo.a.' + a.type) }}</div>
              <div class="desc">{{ a.message }}</div></div>
          </div>
          <div v-if="!alerts.length" class="faint" style="padding:1.5rem 0;text-align:center">{{ t('todo.empty') }}</div>
        </div>
      </div>

      <div class="card">
        <div class="card-head"><div><h3>⚡ {{ t('quick.title') }}</h3><div class="sub">{{ t('quick.sub') }}</div></div></div>
        <div class="quick">
          <div class="qtile" @click="showOnboard = true"><div class="qi">🏢</div><div class="qt">{{ t('quick.q1') }}</div><div class="qd">{{ t('quick.d1') }}</div></div>
          <div class="qtile" @click="router.push('/licensing')"><div class="qi">🔑</div><div class="qt">{{ t('quick.q2') }}</div><div class="qd">{{ t('quick.d2') }}</div></div>
          <div class="qtile" @click="router.push('/licensing')"><div class="qi">🔄</div><div class="qt">{{ t('quick.q3') }}</div><div class="qd">{{ t('quick.d3') }}</div></div>
          <div class="qtile" @click="router.push('/plans')"><div class="qi">🧩</div><div class="qt">{{ t('quick.q4') }}</div><div class="qd">{{ t('quick.d4') }}</div></div>
        </div>
        <div class="notif">
          <div class="n"><span class="nd" style="background:var(--brand)"></span>
            <div><div class="nt">{{ t('notif.t1') }}</div><div class="nm"><span class="data">2026-06-05</span> · {{ t('notif.m1') }}</div></div></div>
          <div class="n"><span class="nd" style="background:var(--success)"></span>
            <div><div class="nt">{{ t('notif.t2') }}</div><div class="nm"><span class="data">2026-06-03</span> · {{ t('notif.m2') }}</div></div></div>
        </div>
      </div>
    </section>

    <!-- 排版规范 -->
    <section class="card">
      <div class="card-head">
        <div><h3>🔤 {{ t('type.title') }}</h3><div class="sub">{{ t('type.sub') }}</div></div>
        <span class="tag">Typography</span>
      </div>
      <div class="type-demo">
        <div class="type-box">
          <div class="k">Help · {{ t('type.helpK') }}</div>
          <div class="sample-help">{{ t('type.helpSample') }}</div>
          <div class="note">font-family: var(--font-help) · {{ t('type.helpNote') }}</div>
        </div>
        <div class="type-box">
          <div class="k">Data · {{ t('type.dataK') }}</div>
          <div class="sample-data">T-100482 &nbsp; v2.4.0 &nbsp; ¥128,640.00 &nbsp; 2027-03-18</div>
          <div class="note">font-family: var(--font-data) · tabular-nums · {{ t('type.dataNote') }}</div>
        </div>
      </div>
    </section>

    <div class="foot-note">CMSSOAS · {{ t('foot') }}</div>

    <OnboardTenantDialog v-model="showOnboard" />
  </div>
</template>
