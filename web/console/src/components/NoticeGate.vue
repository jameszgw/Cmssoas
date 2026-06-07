<script setup lang="ts">
/**
 * 强制须知确认门：登录后若存在“强制确认”的已发布须知且当前用户未同意，弹层阻断使用直至逐条确认。
 * 与“用户授权(同意)”联动：确认即写一条 GRANTED 同意记录(含 IP/UA/版本/时间)。
 */
import { ref, computed, onMounted } from 'vue'
import { useI18n } from 'vue-i18n'
import { ElMessage } from 'element-plus'
import { useAuthStore } from '@/stores/auth'
import { pendingNotices, ackNotice, type Notice } from '@/api/notices'

const { t } = useI18n()
const auth = useAuthStore()
const queue = ref<Notice[]>([])
const idx = ref(0)
const agreed = ref(false)
const loading = ref(false)

const current = computed(() => queue.value[idx.value] || null)
const visible = computed(() => !!current.value)

async function refresh() {
  if (!auth.isAuthed()) return
  try { queue.value = await pendingNotices(); idx.value = 0; agreed.value = false } catch { /* ignore */ }
}
onMounted(refresh)

async function accept() {
  const n = current.value
  if (!n || !agreed.value) return
  loading.value = true
  try {
    await ackNotice(n.id)
    idx.value += 1
    agreed.value = false
    if (idx.value >= queue.value.length) { queue.value = []; ElMessage.success(t('notice.ackAllOk')) }
  } catch (e: any) {
    ElMessage.error(e?.response?.data?.message || t('common.fail'))
  } finally { loading.value = false }
}
</script>

<template>
  <el-dialog :model-value="visible" :title="t('notice.gateTitle')" width="600px" align-center
    :show-close="false" :close-on-click-modal="false" :close-on-press-escape="false">
    <div v-if="current">
      <div class="faint" style="font-size:.78rem;margin-bottom:.5rem">
        {{ t('notice.type.' + current.type) }} · v{{ current.version }}
        <span v-if="queue.length > 1">· {{ idx + 1 }}/{{ queue.length }}</span>
      </div>
      <h3 style="font-size:1.05rem;font-weight:800;margin-bottom:.6rem">{{ current.title }}</h3>
      <div class="gate-body" v-html="current.contentHtml"></div>
      <el-checkbox v-model="agreed" style="margin-top:1rem">{{ t('notice.agree') }}</el-checkbox>
    </div>
    <template #footer>
      <el-button type="primary" :disabled="!agreed" :loading="loading" @click="accept" style="width:100%">
        {{ t('notice.agreeContinue') }}
      </el-button>
    </template>
  </el-dialog>
</template>

<style scoped>
.gate-body{font-size:.88rem;line-height:1.7;color:var(--text);white-space:pre-wrap;
  max-height:46vh;overflow:auto;background:var(--surface-2);border:1px solid var(--border);
  border-radius:10px;padding:.9rem 1.05rem}
</style>
