<script setup lang="ts">
import { reactive, ref, computed } from 'vue'
import { useI18n } from 'vue-i18n'
import { ElMessage } from 'element-plus'
import { onboardTenant } from '@/api/tenant'

const props = defineProps<{ modelValue: boolean }>()
const emit = defineEmits<{ 'update:modelValue': [boolean]; success: [string] }>()
const { t } = useI18n()

const form = reactive({
  name: '华东数据科技有限公司',
  plan: '企业版',
  version: 'v2.4.0',
  email: 'admin@huadong-tech.com',
  isolation: '共享库 · 行级隔离 (RLS)',
  mode: '混合 (HYBRID)',
})
const submitting = ref(false)
const tcode = 'T-100482'

const emailValid = computed(() => /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(form.email))

function close() { emit('update:modelValue', false) }

async function submit() {
  if (!form.name.trim()) { ElMessage.warning(t('m.nameRequired')); return }
  if (!emailValid.value) { ElMessage.warning(t('m.emailInvalid')); return }
  submitting.value = true
  try {
    const res = await onboardTenant({ ...form })
    ElMessage.success(t('m.success', { email: res.email }))
    emit('success', res.code)
    close()
  } finally {
    submitting.value = false
  }
}
</script>

<template>
  <el-dialog
    :model-value="modelValue" width="1120px" align-center :show-close="false"
    class="onboard-dlg" @update:model-value="emit('update:modelValue', $event)"
  >
    <div class="modal">
      <!-- 左：表单 -->
      <div class="left">
        <h2>{{ t('m.title') }}</h2>
        <p class="lead">{{ t('m.lead') }}</p>

        <el-form label-position="top" @submit.prevent>
          <el-form-item :label="t('m.tname')">
            <el-input v-model="form.name" />
          </el-form-item>
          <div class="two">
            <el-form-item :label="t('m.tcode')">
              <el-input :model-value="tcode" readonly class="data-input" />
            </el-form-item>
            <el-form-item :label="t('m.plan')">
              <el-select v-model="form.plan" style="width:100%">
                <el-option v-for="p in ['旗舰版','企业版','专业版','基础版']" :key="p" :label="p" :value="p" />
              </el-select>
            </el-form-item>
          </div>
          <el-form-item :label="t('m.email')">
            <el-input v-model="form.email" class="data-input" />
          </el-form-item>
          <div class="two">
            <el-form-item :label="t('m.iso')">
              <el-select v-model="form.isolation" style="width:100%">
                <el-option label="共享库 · 行级隔离 (RLS)" value="共享库 · 行级隔离 (RLS)" />
                <el-option label="独立 Schema" value="独立 Schema" />
                <el-option label="独立数据库" value="独立数据库" />
              </el-select>
            </el-form-item>
            <el-form-item :label="t('m.mode')">
              <el-select v-model="form.mode" style="width:100%">
                <el-option label="混合 (HYBRID)" value="混合 (HYBRID)" />
                <el-option label="在线 (ONLINE)" value="在线 (ONLINE)" />
                <el-option label="离线 (OFFLINE)" value="离线 (OFFLINE)" />
              </el-select>
            </el-form-item>
          </div>
        </el-form>

        <div class="notice">✉️
          <div><span class="em">{{ t('m.noticeEm') }}</span>{{ t('m.noticeBody') }}</div>
        </div>

        <div class="actions">
          <el-button type="primary" size="large" :loading="submitting" @click="submit">
            {{ t('m.submit') }}
          </el-button>
          <el-button size="large" @click="close">{{ t('m.cancel') }}</el-button>
        </div>
      </div>

      <!-- 右：邮件实时预览 -->
      <div class="right">
        <div class="ptitle">📧 {{ t('m.preview') }}</div>
        <div class="mail">
          <div class="mhead">
            <div class="row"><span class="lbl">{{ t('mail.from') }}</span><span class="data">no-reply@codeman.com</span></div>
            <div class="row"><span class="lbl">{{ t('mail.to') }}</span><span class="data">{{ form.email }}</span></div>
            <div class="row"><span class="lbl">{{ t('mail.subj') }}</span><span>{{ t('mail.subject') }}</span></div>
          </div>
          <div class="mtop">
            <div class="logo">C</div>
            <h4>{{ t('mail.welcome') }}</h4>
            <p>{{ t('mail.tagline') }}</p>
          </div>
          <div class="mcontent">
            <p>{{ t('mail.hi') }}</p>
            <p>{{ t('mail.p1') }}</p>
            <div class="kv">
              <div class="l"><span>{{ t('mail.kt') }}</span><b>{{ form.name }}</b></div>
              <div class="l"><span>{{ t('mail.kc') }}</span><b>{{ tcode }}</b></div>
              <div class="l"><span>{{ t('mail.kp') }}</span><b>{{ form.version }}</b></div>
              <div class="l"><span>{{ t('mail.ke') }}</span><b>24:00:00</b></div>
            </div>
            <span class="mbtn">{{ t('mail.btn') }}</span>
            <div class="mnote">{{ t('mail.note') }}</div>
          </div>
        </div>
      </div>
    </div>
  </el-dialog>
</template>

<style scoped>
.modal{display:grid;grid-template-columns:1.05fr 1fr;margin:-1.2rem -1.2rem}
.left{padding:.4rem 1.6rem 1rem}
.right{background:var(--surface-2);border-left:1px solid var(--border);padding:1rem 1.4rem 1.4rem}
h2{font-size:1.25rem;font-weight:800}
.lead{color:var(--muted);font-size:.86rem;margin:.3rem 0 1rem}
.two{display:grid;grid-template-columns:1fr 1fr;gap:.9rem}
.actions{display:flex;gap:.7rem;margin-top:.6rem}
.ptitle{font-size:.78rem;font-weight:800;color:var(--muted);letter-spacing:.5px;margin-bottom:.7rem}
:deep(.data-input .el-input__inner){font-family:var(--font-data);color:var(--data-ink)}
</style>
