<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { useI18n } from 'vue-i18n'
import { ElMessage } from 'element-plus'
import PageHelp from '@/components/PageHelp.vue'
import {
  getPlans, getSubscriptions, createSubscription,
  type PlanView, type SubscriptionView,
} from '@/api/catalog'

const { t } = useI18n()
const plans = ref<PlanView[]>([])
const subs = ref<SubscriptionView[]>([])
const fmtPrice = (n: number) => '¥' + n.toLocaleString('en-US')

async function load() {
  ;[plans.value, subs.value] = await Promise.all([getPlans(), getSubscriptions()])
}
onMounted(load)

const planName = (code: string) => {
  const p = plans.value.find((x) => x.code === code)
  return p ? t(p.planKey) : code
}

// 订阅对话框
const open = ref(false)
const submitting = ref(false)
const today = new Date().toISOString().slice(0, 10)
const nextYear = new Date(Date.now() + 365 * 864e5).toISOString().slice(0, 10)
const form = reactive({ planCode: '', tenantCode: 'T-100482', customer: '华东数据科技有限公司', qty: 1, startAt: today, endAt: nextYear })
const curPlan = computed(() => plans.value.find((p) => p.code === form.planCode))

function choose(p: PlanView) {
  form.planCode = p.code
  open.value = true
}
async function submit() {
  if (!form.tenantCode || !form.customer) { ElMessage.warning(t('subs.required')); return }
  submitting.value = true
  try {
    const r = await createSubscription({ ...form })
    ElMessage.success(t('subs.created', { id: r.licenseId }))
    open.value = false
    load()
  } catch (e: any) {
    ElMessage.error(e?.response?.data?.message || t('subs.failed'))
  } finally { submitting.value = false }
}
</script>

<template>
  <div class="wrap">
    <PageHelp id="plans" :title="t('help.plans.title')"
      :tips="[t('help.plans.t1'), t('help.plans.t2'), t('help.plans.t3')]" />
    <div class="section-title">
      <div><h2>{{ t('nav.plan') }}</h2><div class="sub" style="margin-top:.3rem">{{ t('subs.lead') }}</div></div>
    </div>

    <section class="grid" style="grid-template-columns:repeat(4,1fr);gap:1.1rem;margin-bottom:1.2rem">
      <div v-for="p in plans" :key="p.code" class="card plan" :class="{ hot: p.code === 'PROFESSIONAL' }">
        <div v-if="p.code === 'PROFESSIONAL'" class="ribbon">{{ t('subs.popular') }}</div>
        <div class="pname">{{ t(p.planKey) }}</div>
        <div class="pcode data faint">{{ p.code }}</div>
        <div class="price"><span class="data">{{ fmtPrice(p.price) }}</span><span class="per">{{ t('subs.perYear') }}</span></div>
        <div class="meta"><span>{{ t('subs.seats') }}</span><b class="data">{{ p.seats }}</b></div>
        <div class="meta"><span>{{ t('subs.versionRange') }}</span><b class="data">{{ p.versionRange }}</b></div>
        <div class="incl">{{ t('subs.includes') }}</div>
        <div class="mods"><el-tag v-for="m in p.modules" :key="m" size="small" effect="plain" style="margin:2px">{{ m }}</el-tag></div>
        <el-button :type="p.code === 'PROFESSIONAL' ? 'primary' : 'default'" class="choose" @click="choose(p)">{{ t('subs.choose') }}</el-button>
      </div>
    </section>

    <div class="card">
      <div class="card-head"><div><h3>📃 {{ t('subs.subsTitle') }}</h3><div class="sub">{{ t('subs.subsSub') }}</div></div></div>
      <el-table :data="subs" style="width:100%">
        <el-table-column :label="t('th.name')" min-width="200">
          <template #default="{ row }"><b>{{ row.customer }}</b> <span class="data faint" style="font-size:.74rem">{{ row.tenantCode }}</span></template>
        </el-table-column>
        <el-table-column :label="t('common.plan')" width="120">
          <template #default="{ row }">{{ planName(row.planCode) }}</template>
        </el-table-column>
        <el-table-column :label="t('subs.qty')" width="80">
          <template #default="{ row }"><span class="data">{{ row.qty }}</span></template>
        </el-table-column>
        <el-table-column :label="t('subs.period')" width="220">
          <template #default="{ row }"><span class="data">{{ row.startAt }} ~ {{ row.endAt }}</span></template>
        </el-table-column>
        <el-table-column :label="t('lic.id')" width="150">
          <template #default="{ row }"><span class="data">{{ row.licenseId || '—' }}</span></template>
        </el-table-column>
        <el-table-column :label="t('th.status')" width="110">
          <template #default="{ row }"><span class="status s-active"><i></i>{{ t('st.active') }}</span></template>
        </el-table-column>
      </el-table>
    </div>

    <!-- 订阅对话框 -->
    <el-dialog v-model="open" :title="t('subs.createTitle')" width="560px" align-center>
      <el-form label-position="top">
        <el-form-item :label="t('common.plan')">
          <el-input :model-value="curPlan ? t(curPlan.planKey) + ' · ' + curPlan.code : ''" readonly />
        </el-form-item>
        <div class="two">
          <el-form-item :label="t('m.tcode')"><el-input v-model="form.tenantCode" class="dataf" /></el-form-item>
          <el-form-item :label="t('th.name')"><el-input v-model="form.customer" /></el-form-item>
        </div>
        <div class="two">
          <el-form-item :label="t('subs.qty')"><el-input-number v-model="form.qty" :min="1" :max="999" style="width:100%" /></el-form-item>
          <el-form-item :label="t('subs.seatsTotal')">
            <el-input :model-value="curPlan ? String(curPlan.seats * form.qty) : ''" readonly class="dataf" />
          </el-form-item>
        </div>
        <div class="two">
          <el-form-item :label="t('subs.start')"><el-date-picker v-model="form.startAt" type="date" value-format="YYYY-MM-DD" style="width:100%" /></el-form-item>
          <el-form-item :label="t('subs.end')"><el-date-picker v-model="form.endAt" type="date" value-format="YYYY-MM-DD" style="width:100%" /></el-form-item>
        </div>
        <div class="notice">🔏 <div>{{ t('subs.autoIssue') }}</div></div>
      </el-form>
      <template #footer>
        <el-button @click="open = false">{{ t('common.cancel') }}</el-button>
        <el-button type="primary" :loading="submitting" @click="submit">{{ t('subs.submit') }}</el-button>
      </template>
    </el-dialog>
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
.two{display:grid;grid-template-columns:1fr 1fr;gap:.9rem}
.notice{display:flex;gap:.6rem;align-items:flex-start;background:color-mix(in srgb,var(--brand) 9%,transparent);
  border:1px solid color-mix(in srgb,var(--brand) 22%,transparent);border-radius:12px;padding:.7rem .9rem;font-size:.82rem;margin-top:.4rem}
:deep(.dataf .el-input__inner){font-family:var(--font-data);color:var(--data-ink)}
</style>
