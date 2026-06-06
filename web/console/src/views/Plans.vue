<script setup lang="ts">
import { useI18n } from 'vue-i18n'
import PageHelp from '@/components/PageHelp.vue'

const { t } = useI18n()

const plans = [
  { key: 'basic', code: 'BASIC', price: 19800, modules: ['RISK', 'REPORT'], versionRange: '>=2.2.0', seats: 2, popular: false },
  { key: 'pro', code: 'PROFESSIONAL', price: 49800, modules: ['RISK', 'REPORT', 'AUDIT'], versionRange: '>=2.3.0', seats: 5, popular: true },
  { key: 'ent', code: 'ENTERPRISE', price: 99800, modules: ['RISK', 'REPORT', 'AUDIT', 'BI'], versionRange: '>=2.4.0', seats: 20, popular: false },
  { key: 'flag', code: 'FLAGSHIP', price: 198000, modules: ['RISK', 'REPORT', 'AUDIT', 'BI', 'DATA'], versionRange: '>=2.4.0', seats: 50, popular: false },
]
const subs = [
  { tenant: '长江证券股份公司', code: 'T-100480', plan: 'plan.flag', qty: 1, period: '2026-06-06 ~ 2027-05-30', status: 'active' },
  { tenant: '华东数据科技有限公司', code: 'T-100482', plan: 'plan.ent', qty: 1, period: '2026-06-06 ~ 2027-06-06', status: 'active' },
  { tenant: '瑞康医疗集团', code: 'T-100481', plan: 'plan.pro', qty: 1, period: '2025-06-29 ~ 2026-06-29', status: 'soon' },
]
const statusClass: Record<string, string> = { active: 's-active', soon: 's-soon', exp: 's-exp' }
const fmtPrice = (n: number) => '¥' + n.toLocaleString('en-US')
</script>

<template>
  <div class="wrap">
    <PageHelp id="plans" :title="t('help.plans.title')"
      :tips="[t('help.plans.t1'), t('help.plans.t2'), t('help.plans.t3')]" />
    <div class="section-title">
      <div><h2>{{ t('nav.plan') }}</h2><div class="sub" style="margin-top:.3rem">{{ t('subs.lead') }}</div></div>
    </div>

    <!-- 套餐卡 -->
    <section class="grid" style="grid-template-columns:repeat(4,1fr);gap:1.1rem;margin-bottom:1.2rem">
      <div v-for="p in plans" :key="p.key" class="card plan" :class="{ hot: p.popular }">
        <div v-if="p.popular" class="ribbon">{{ t('subs.popular') }}</div>
        <div class="pname">{{ t('plan.' + p.key) }}</div>
        <div class="pcode data faint">{{ p.code }}</div>
        <div class="price"><span class="data">{{ fmtPrice(p.price) }}</span><span class="per">{{ t('subs.perYear') }}</span></div>
        <div class="meta"><span>{{ t('subs.seats') }}</span><b class="data">{{ p.seats }}</b></div>
        <div class="meta"><span>{{ t('subs.versionRange') }}</span><b class="data">{{ p.versionRange }}</b></div>
        <div class="incl">{{ t('subs.includes') }}</div>
        <div class="mods"><el-tag v-for="m in p.modules" :key="m" size="small" effect="plain" style="margin:2px">{{ m }}</el-tag></div>
        <el-button :type="p.popular ? 'primary' : 'default'" class="choose">{{ t('subs.choose') }}</el-button>
      </div>
    </section>

    <!-- 订阅表 -->
    <div class="card">
      <div class="card-head"><div><h3>📃 {{ t('subs.subsTitle') }}</h3><div class="sub">{{ t('subs.subsSub') }}</div></div></div>
      <el-table :data="subs" style="width:100%">
        <el-table-column :label="t('th.name')" min-width="200">
          <template #default="{ row }"><b>{{ row.tenant }}</b> <span class="data faint" style="font-size:.74rem">{{ row.code }}</span></template>
        </el-table-column>
        <el-table-column :label="t('common.plan')" width="140">
          <template #default="{ row }">{{ t(row.plan) }}</template>
        </el-table-column>
        <el-table-column :label="t('subs.qty')" width="90">
          <template #default="{ row }"><span class="data">{{ row.qty }}</span></template>
        </el-table-column>
        <el-table-column :label="t('subs.period')" width="240">
          <template #default="{ row }"><span class="data">{{ row.period }}</span></template>
        </el-table-column>
        <el-table-column :label="t('th.status')" width="120">
          <template #default="{ row }"><span class="status" :class="statusClass[row.status]"><i></i>{{ t('st.' + row.status) }}</span></template>
        </el-table-column>
      </el-table>
    </div>
  </div>
</template>

<style scoped>
.plan{display:flex;flex-direction:column;gap:.5rem;position:relative;overflow:hidden}
.plan.hot{border-color:var(--brand);box-shadow:0 14px 40px -20px var(--ring)}
.ribbon{position:absolute;top:14px;right:-30px;transform:rotate(45deg);background:linear-gradient(100deg,var(--brand),var(--brand-2));
  color:#fff;font-size:.66rem;font-weight:800;padding:.2rem 2.4rem}
.pname{font-size:1.15rem;font-weight:800}
.pcode{font-size:.72rem;margin-top:-.3rem}
.price{display:flex;align-items:baseline;gap:.3rem;margin:.4rem 0}
.price .data{font-size:1.6rem;font-weight:800;color:var(--data-ink)}
.price .per{font-size:.74rem;color:var(--muted)}
.meta{display:flex;justify-content:space-between;font-size:.82rem;color:var(--muted);padding:.15rem 0}
.incl{font-size:.74rem;color:var(--faint);margin-top:.5rem;text-transform:uppercase;letter-spacing:.5px}
.mods{display:flex;flex-wrap:wrap;min-height:2.2rem}
.choose{margin-top:.6rem;width:100%}
</style>
