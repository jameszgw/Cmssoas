<script setup lang="ts">
/**
 * 右下角悬浮智能客服窗：知识库问答 + 流式回复 + 转人工。
 * 通用、不绑定厂商：上游是否接入大模型由后端配置决定，未接入时优雅降级为知识库匹配。
 */
import { ref, nextTick, onMounted } from 'vue'
import { useI18n } from 'vue-i18n'
import { ElMessage } from 'element-plus'
import { useAuthStore } from '@/stores/auth'
import { csStatus, chatStream, escalateConversation, type CsStatus } from '@/api/cs'

const { t } = useI18n()
const auth = useAuthStore()

interface Bubble { role: 'user' | 'assistant'; content: string }
const open = ref(false)
const input = ref('')
const sending = ref(false)
const conversationId = ref<number | null>(null)
const bubbles = ref<Bubble[]>([])
const status = ref<CsStatus | null>(null)
const body = ref<HTMLElement | null>(null)

const show = () => auth.has('cs:use')

onMounted(async () => {
  if (!show()) return
  try { status.value = await csStatus() } catch { /* ignore */ }
})

function toggle() {
  open.value = !open.value
  if (open.value && bubbles.value.length === 0) {
    bubbles.value.push({ role: 'assistant', content: t('cs.greeting') })
  }
}

async function scroll() {
  await nextTick()
  if (body.value) body.value.scrollTop = body.value.scrollHeight
}

async function send() {
  const q = input.value.trim()
  if (!q || sending.value) return
  input.value = ''
  bubbles.value.push({ role: 'user', content: q })
  const reply: Bubble = { role: 'assistant', content: '' }
  bubbles.value.push(reply)
  sending.value = true
  scroll()
  await chatStream(
    { conversationId: conversationId.value, question: q },
    {
      onMeta: (id) => { conversationId.value = id },
      onDelta: (txt) => { reply.content += txt; scroll() },
      onDone: () => { sending.value = false; if (!reply.content) reply.content = t('cs.noReply'); scroll() },
      onError: (msg) => { sending.value = false; reply.content = t('cs.error') + msg },
    },
  )
}

async function escalate() {
  if (!conversationId.value) { ElMessage.info(t('cs.escalateFirst')); return }
  try {
    await escalateConversation(conversationId.value)
    bubbles.value.push({ role: 'assistant', content: t('cs.escalatedMsg') })
    ElMessage.success(t('cs.escalatedOk'))
    scroll()
  } catch (e: any) { ElMessage.error(e?.response?.data?.message || t('common.fail')) }
}
</script>

<template>
  <div v-if="show()" class="cs-root">
    <transition name="cs-pop">
      <div v-if="open" class="cs-panel">
        <div class="cs-head">
          <div class="cs-h-l">
            <span class="cs-dot" :class="status?.ready ? 'on' : 'off'"></span>
            <div>
              <div class="cs-title">{{ t('cs.title') }}</div>
              <div class="cs-sub">{{ status?.ready ? t('cs.online', { m: status.model }) : t('cs.kbMode') }}</div>
            </div>
          </div>
          <button class="cs-x" @click="open = false">✕</button>
        </div>

        <div ref="body" class="cs-body">
          <div v-for="(b, i) in bubbles" :key="i" class="cs-row" :class="b.role">
            <div class="cs-bubble">{{ b.content }}<span v-if="sending && i === bubbles.length - 1 && b.role === 'assistant'" class="cs-caret">▍</span></div>
          </div>
        </div>

        <div class="cs-foot">
          <div class="cs-tools">
            <button class="cs-link" @click="escalate">🙋 {{ t('cs.escalate') }}</button>
            <span class="cs-hint faint">{{ t('cs.disclaimer') }}</span>
          </div>
          <div class="cs-input">
            <el-input v-model="input" type="textarea" :rows="2" resize="none"
              :placeholder="t('cs.placeholder')" @keydown.enter.exact.prevent="send" />
            <el-button type="primary" :loading="sending" @click="send">{{ t('cs.send') }}</el-button>
          </div>
        </div>
      </div>
    </transition>

    <button class="cs-fab" :title="t('cs.title')" @click="toggle">
      <span v-if="!open">💬</span><span v-else>▾</span>
    </button>
  </div>
</template>

<style scoped>
.cs-root{position:fixed;right:clamp(16px,2vw,40px);bottom:clamp(16px,2.5vh,40px);z-index:2000}
.cs-fab{width:3.4rem;height:3.4rem;border-radius:50%;border:0;cursor:pointer;font-size:1.5rem;color:#fff;
  background:linear-gradient(120deg,var(--brand),var(--brand-2));box-shadow:0 12px 30px -10px var(--ring)}
.cs-fab:hover{filter:brightness(1.06)}
.cs-panel{position:absolute;right:0;bottom:4.2rem;width:min(380px,86vw);height:min(560px,72vh);
  display:flex;flex-direction:column;background:var(--surface);border:1px solid var(--border);
  border-radius:var(--r-lg);box-shadow:var(--shadow);overflow:hidden}
.cs-head{display:flex;align-items:center;justify-content:space-between;padding:.8rem 1rem;color:#fff;
  background:linear-gradient(120deg,var(--brand),var(--brand-2))}
.cs-h-l{display:flex;align-items:center;gap:.6rem}
.cs-title{font-weight:800;font-size:.95rem}
.cs-sub{font-size:.7rem;opacity:.9}
.cs-dot{width:.6rem;height:.6rem;border-radius:50%;display:inline-block}
.cs-dot.on{background:#4ade80;box-shadow:0 0 0 3px rgba(74,222,128,.3)}
.cs-dot.off{background:#fbbf24;box-shadow:0 0 0 3px rgba(251,191,36,.3)}
.cs-x{border:0;background:rgba(255,255,255,.18);color:#fff;width:1.7rem;height:1.7rem;border-radius:8px;cursor:pointer}
.cs-body{flex:1;overflow-y:auto;padding:1rem .9rem;display:flex;flex-direction:column;gap:.7rem;background:var(--app-bg)}
.cs-row{display:flex}
.cs-row.user{justify-content:flex-end}
.cs-bubble{max-width:82%;padding:.6rem .8rem;border-radius:12px;font-size:.85rem;line-height:1.6;white-space:pre-wrap;word-break:break-word}
.cs-row.assistant .cs-bubble{background:var(--surface);border:1px solid var(--border);color:var(--text);border-top-left-radius:3px}
.cs-row.user .cs-bubble{background:linear-gradient(120deg,var(--brand),var(--brand-2));color:#fff;border-top-right-radius:3px}
.cs-caret{animation:csb 1s steps(2) infinite}
@keyframes csb{50%{opacity:0}}
.cs-foot{border-top:1px solid var(--border);padding:.6rem .8rem;background:var(--surface)}
.cs-tools{display:flex;align-items:center;justify-content:space-between;margin-bottom:.45rem}
.cs-link{border:0;background:transparent;color:var(--brand);font-weight:700;font-size:.78rem;cursor:pointer}
.cs-hint{font-size:.66rem}
.cs-input{display:flex;gap:.5rem;align-items:flex-end}
.cs-pop-enter-active,.cs-pop-leave-active{transition:.18s ease}
.cs-pop-enter-from,.cs-pop-leave-to{opacity:0;transform:translateY(10px)}
</style>
