<script setup lang="ts">
import { useI18n } from 'vue-i18n'
import PageHelp from '@/components/PageHelp.vue'
import HelpTip from '@/components/HelpTip.vue'

const { t } = useI18n()

const versions = ['v2.2', 'v2.3', 'v2.4']
const modules = [
  {
    code: 'RISK', name: '风险管理', features: [
      { code: 'RISK.RULES', name: '规则引擎', avail: [true, true, true] },
      { code: 'RISK.REALTIME', name: '实时风控', avail: [false, true, true] },
    ],
  },
  {
    code: 'REPORT', name: '报表中心', features: [
      { code: 'REPORT.VIEW', name: '报表查看', avail: [true, true, true] },
      { code: 'REPORT.EXPORT', name: '报表导出', avail: [false, true, true] },
    ],
  },
  {
    code: 'AUDIT', name: '审计', features: [
      { code: 'AUDIT.TRAIL', name: '操作审计', avail: [true, true, true] },
    ],
  },
  {
    code: 'BI', name: '智能分析', features: [
      { code: 'BI.DASH', name: '分析大盘', avail: [false, false, true] },
      { code: 'BI.ML', name: '智能预测', avail: [false, false, true] },
    ],
  },
]
const flatFeatures = modules.flatMap((m) => m.features.map((f) => ({ ...f, module: m.code })))
</script>

<template>
  <div class="wrap">
    <PageHelp id="catalog" :title="t('help.catalog.title')"
      :tips="[t('help.catalog.t1'), t('help.catalog.t2'), t('help.catalog.t3')]" />
    <div class="section-title">
      <div><h2>{{ t('nav.catalog') }}</h2><div class="sub" style="margin-top:.3rem">{{ t('cat.lead') }}</div></div>
      <span class="tag">CMSSOAS</span>
    </div>

    <section class="grid" style="grid-template-columns:1fr 1.6fr;gap:1.1rem">
      <!-- 模块/功能 -->
      <div class="card">
        <div class="card-head"><div><h3>🧱 {{ t('cat.modules') }}</h3><div class="sub">{{ t('cat.modulesSub') }}</div></div></div>
        <div v-for="m in modules" :key="m.code" class="mod">
          <div class="mod-h"><b>{{ m.name }}</b><span class="data faint">{{ m.code }}</span></div>
          <div class="feat">
            <el-tag v-for="f in m.features" :key="f.code" size="small" effect="plain" style="margin:3px">{{ f.name }} · <span class="data">{{ f.code }}</span></el-tag>
          </div>
        </div>
      </div>

      <!-- 版本功能矩阵 -->
      <div class="card">
        <div class="card-head"><div><h3>🧩 {{ t('cat.matrix') }}<HelpTip :content="t('help.catalog.t2')" /></h3>
          <div class="sub">{{ t('cat.matrixSub') }}</div></div></div>
        <table class="matrix">
          <thead>
            <tr><th>{{ t('cat.feature') }}</th><th v-for="v in versions" :key="v" class="data vcol">{{ v }}</th></tr>
          </thead>
          <tbody>
            <tr v-for="f in flatFeatures" :key="f.code">
              <td><b>{{ f.name }}</b> <span class="data faint">{{ f.code }}</span></td>
              <td v-for="(ok, i) in f.avail" :key="i" class="cell">
                <span v-if="ok" class="yes">✓</span><span v-else class="no">—</span>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
    </section>
  </div>
</template>

<style scoped>
.mod{padding:.7rem 0;border-bottom:1px solid var(--border)}
.mod:last-child{border:0}
.mod-h{display:flex;justify-content:space-between;align-items:center;font-size:.92rem;margin-bottom:.4rem}
.matrix{width:100%;border-collapse:collapse;font-size:.86rem}
.matrix th{text-align:left;color:var(--faint);font-size:.74rem;text-transform:uppercase;padding:.55rem .6rem;border-bottom:1px solid var(--border)}
.matrix th.vcol,.matrix td.cell{text-align:center;width:90px}
.matrix td{padding:.6rem;border-bottom:1px solid var(--border)}
.matrix tr:last-child td{border-bottom:0}
.yes{color:var(--success);font-weight:800}
.no{color:var(--faint)}
.feat{display:flex;flex-wrap:wrap}
</style>
