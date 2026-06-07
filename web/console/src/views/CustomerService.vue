<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useI18n } from 'vue-i18n'
import PageHelp from '@/components/PageHelp.vue'
import { allConversations, conversationMessages, csStatus, type CsConversation, type CsMessage, type CsStatus } from '@/api/cs'

const { t } = useI18n()
const rows = ref<CsConversation[]>([])
const loading = ref(false)
const status = ref<CsStatus | null>(null)

const statusClass: Record<string, string> = { OPEN: 's-active', ESCALATED: 's-soon', CLOSED: 's-exp' }
const fmt = (s: string | null) => (s ? s.slice(0, 19).replace('T', ' ') : '—')

async function load() {
  loading.value = true
  try {
    rows.value = await allConversations()
    status.value = await csStatus()
  } finally { loading.value = false }
}
onMounted(load)

// 抽屉：会话消息
const drawer = ref(false)
const msgs = ref<CsMessage[]>([])
const current = ref<CsConversation | null>(null)
async function openConv(c: CsConversation) {
  current.value = c; drawer.value = true; msgs.value = []
  msgs.value = await conversationMessages(c.id)
}
</script>

<template>
  <div class="wrap">
    <PageHelp id="cs" :title="t('cs.help.title')"
      :tips="[t('cs.help.t1'), t('cs.help.t2'), t('cs.help.t3'), t('cs.help.t4')]" />

    <div class="section-title">
      <div>
        <h2>{{ t('nav.cs') }}</h2>
        <div class="sub" style="margin-top:.3rem">{{ t('cs.lead') }}</div>
      </div>
      <span class="status" :class="status?.ready ? 's-active' : 's-soon'"><i></i>
        {{ status?.ready ? t('cs.online', { m: status.model }) : t('cs.kbMode') }}
      </span>
    </div>

    <section class="grid kpis" style="grid-template-columns:repeat(3,1fr);margin-bottom:1.1rem">
      <div class="card kpi"><div class="top"><span class="label">{{ t('cs.kConv') }}</span><span class="ic">💬</span></div>
        <div class="val data">{{ rows.length }}</div></div>
      <div class="card kpi"><div class="top"><span class="label">{{ t('cs.kEscalated') }}</span><span class="ic">🙋</span></div>
        <div class="val data">{{ rows.filter((r) => r.status === 'ESCALATED').length }}</div></div>
      <div class="card kpi"><div class="top"><span class="label">{{ t('cs.kKb') }}</span><span class="ic">📚</span></div>
        <div class="val data">{{ status?.kbSize ?? '—' }}</div></div>
    </section>

    <div class="card">
      <el-table :data="rows" v-loading="loading" style="width:100%" max-height="600" @row-click="openConv">
        <el-table-column :label="t('cs.colTitle')" min-width="240">
          <template #default="{ row }"><b>{{ row.title || t('cs.untitled') }}</b></template>
        </el-table-column>
        <el-table-column :label="t('cs.colUser')" width="160">
          <template #default="{ row }"><span class="data">{{ row.userRef }}</span></template>
        </el-table-column>
        <el-table-column :label="t('th.status')" width="120">
          <template #default="{ row }"><span class="status" :class="statusClass[row.status]"><i></i>{{ t('cs.cs.' + row.status) }}</span></template>
        </el-table-column>
        <el-table-column :label="t('cs.updated')" width="180">
          <template #default="{ row }"><span class="data faint">{{ fmt(row.updatedAt) }}</span></template>
        </el-table-column>
        <el-table-column :label="t('th.op')" width="100">
          <template #default><button class="linkbtn">{{ t('cs.viewMsgs') }}</button></template>
        </el-table-column>
      </el-table>
      <div v-if="!loading && !rows.length" class="faint" style="text-align:center;padding:2.5rem 0">{{ t('cs.empty') }}</div>
    </div>

    <el-drawer v-model="drawer" :title="current?.title || t('cs.untitled')" size="460px">
      <div class="faint" style="font-size:.78rem;margin-bottom:1rem">
        {{ current?.userRef }} · {{ current && t('cs.cs.' + current.status) }}
      </div>
      <div class="conv">
        <div v-for="m in msgs" :key="m.id" class="cv-row" :class="m.role">
          <div class="cv-bubble">{{ m.content }}</div>
          <div class="cv-time faint">{{ fmt(m.createdAt) }}</div>
        </div>
        <div v-if="!msgs.length" class="faint" style="text-align:center;padding:2rem 0">{{ t('cs.noMsgs') }}</div>
      </div>
    </el-drawer>
  </div>
</template>

<style scoped>
.conv{display:flex;flex-direction:column;gap:.9rem}
.cv-row{display:flex;flex-direction:column;gap:.2rem}
.cv-row.user{align-items:flex-end}
.cv-bubble{max-width:88%;padding:.55rem .8rem;border-radius:11px;font-size:.85rem;line-height:1.6;white-space:pre-wrap;word-break:break-word}
.cv-row.assistant .cv-bubble{background:var(--surface-2);border:1px solid var(--border);color:var(--text)}
.cv-row.user .cv-bubble{background:linear-gradient(120deg,var(--brand),var(--brand-2));color:#fff}
.cv-time{font-size:.66rem}
:deep(.el-table__row){cursor:pointer}
</style>
