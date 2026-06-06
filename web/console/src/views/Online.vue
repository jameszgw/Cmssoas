<script setup lang="ts">
import { ref, onMounted, onUnmounted, computed } from 'vue'
import { useI18n } from 'vue-i18n'
import PageHelp from '@/components/PageHelp.vue'
import HelpTip from '@/components/HelpTip.vue'
import { listInstances, listSeats, onlineStats, type InstanceView, type SeatUsage, type OnlineStats } from '@/api/online'

const { t } = useI18n()
const instances = ref<InstanceView[]>([])
const seats = ref<SeatUsage[]>([])
const stats = ref<OnlineStats>({ onlineInstances: 0, graceInstances: 0, totalSeatsUsed: 0 })
let timer: number | undefined

const stateClass: Record<string, string> = { online: 's-active', grace: 's-soon', offline: 's-exp', released: 's-init' }

async function load() {
  const [i, s, st] = await Promise.all([listInstances(), listSeats(), onlineStats()])
  instances.value = i; seats.value = s; stats.value = st
}
function fmt(ts: string) { return ts ? ts.slice(0, 19).replace('T', ' ') : '' }
const seatPct = (s: SeatUsage) => (s.total ? Math.min(100, Math.round((s.used / s.total) * 100)) : 0)

onMounted(() => { load(); timer = window.setInterval(load, 5000) })
onUnmounted(() => { if (timer) clearInterval(timer) })
</script>

<template>
  <div class="wrap">
    <PageHelp id="online" :title="t('help.online.title')"
      :tips="[t('help.online.t1'), t('help.online.t2'), t('help.online.t3')]" />
    <div class="section-title">
      <div>
        <h2>{{ t('online.title') }}</h2>
        <div class="sub" style="margin-top:.3rem">{{ t('online.lead') }}</div>
      </div>
      <span class="tag">⟳ {{ t('online.auto') }}</span>
    </div>

    <!-- KPI -->
    <section class="grid kpis" style="margin-bottom:1.1rem">
      <div class="card kpi">
        <div class="top"><span class="label">{{ t('online.k1') }}</span><span class="ic">📡</span></div>
        <div class="val">{{ stats.onlineInstances }}</div>
        <div class="foot"><span class="muted">{{ t('online.k1d') }}</span></div>
      </div>
      <div class="card kpi">
        <div class="top"><span class="label">{{ t('online.k2') }}<HelpTip :content="t('help.online.t2')" /></span><span class="ic">⏳</span></div>
        <div class="val">{{ stats.graceInstances }}</div>
        <div class="foot"><span class="muted">{{ t('online.k2d') }}</span></div>
      </div>
      <div class="card kpi">
        <div class="top"><span class="label">{{ t('online.k3') }}</span><span class="ic">🪑</span></div>
        <div class="val">{{ stats.totalSeatsUsed }}</div>
        <div class="foot"><span class="muted">{{ t('online.k3d') }}</span></div>
      </div>
      <div class="card kpi">
        <div class="top"><span class="label">{{ t('online.k4') }}</span><span class="ic">🔑</span></div>
        <div class="val">{{ seats.length }}</div>
        <div class="foot"><span class="muted">{{ t('online.k4d') }}</span></div>
      </div>
    </section>

    <section class="grid" style="grid-template-columns:1.7fr 1fr;gap:1.1rem">
      <!-- 实例表 -->
      <div class="card">
        <div class="card-head"><div><h3>📡 {{ t('online.instances') }}</h3>
          <div class="sub">{{ t('online.instancesSub') }}</div></div></div>
        <el-table :data="instances" style="width:100%" max-height="520">
          <el-table-column :label="t('lic.id')" width="140">
            <template #default="{ row }"><span class="data">{{ row.licenseId }}</span></template>
          </el-table-column>
          <el-table-column :label="t('online.instance')" min-width="130">
            <template #default="{ row }"><span class="data">{{ row.instanceId }}</span></template>
          </el-table-column>
          <el-table-column :label="t('online.machine')" min-width="120">
            <template #default="{ row }"><span class="data faint">{{ row.machineCode || '—' }}</span></template>
          </el-table-column>
          <el-table-column label="IP" width="120">
            <template #default="{ row }"><span class="data">{{ row.ip || '—' }}</span></template>
          </el-table-column>
          <el-table-column :label="t('online.state')" width="110">
            <template #default="{ row }">
              <span class="status" :class="stateClass[row.state]"><i></i>{{ t('online.s.' + row.state) }}</span>
            </template>
          </el-table-column>
          <el-table-column :label="t('online.lastHb')" width="170">
            <template #default="{ row }"><span class="data">{{ fmt(row.lastHeartbeat) }}</span></template>
          </el-table-column>
        </el-table>
      </div>

      <!-- 席位用量 -->
      <div class="card">
        <div class="card-head"><div><h3>🪑 {{ t('online.seats') }}</h3>
          <div class="sub">{{ t('online.seatsSub') }}</div></div></div>
        <div class="bars">
          <div class="bar" v-for="s in seats" :key="s.licenseId">
            <div class="bl">
              <span>{{ s.customer }} <span class="faint data" style="font-size:.72rem">{{ s.licenseId }}</span></span>
              <b>{{ s.used }}/{{ s.total }}</b>
            </div>
            <div class="track"><div class="fill" :style="{ width: seatPct(s) + '%', background: seatPct(s) >= 100 ? 'var(--danger)' : undefined }"></div></div>
          </div>
          <div v-if="!seats.length" class="faint" style="text-align:center;padding:2rem 0">{{ t('online.empty') }}</div>
        </div>
      </div>
    </section>
  </div>
</template>
