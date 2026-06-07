<script setup lang="ts">
import { ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { ElMessage } from 'element-plus'
import { useAuthStore } from '@/stores/auth'

const { t } = useI18n()
const auth = useAuthStore()
const oldPwd = ref('')
const newPwd = ref('')
const confirm = ref('')
const loading = ref(false)

async function submit() {
  if (newPwd.value.length < 6) { ElMessage.warning(t('pwd.short')); return }
  if (newPwd.value !== confirm.value) { ElMessage.warning(t('pwd.mismatch')); return }
  loading.value = true
  try {
    await auth.changePassword(oldPwd.value, newPwd.value)
    ElMessage.success(t('pwd.ok'))
    oldPwd.value = newPwd.value = confirm.value = ''
  } catch (e: any) {
    ElMessage.error(e?.response?.data?.message || t('pwd.fail'))
  } finally { loading.value = false }
}
</script>

<template>
  <el-dialog :model-value="auth.mustChangePwd" :title="t('pwd.title')" width="440px" align-center
    :show-close="false" :close-on-click-modal="false" :close-on-press-escape="false">
    <p class="muted" style="font-size:.85rem;margin-bottom:1rem">{{ t('pwd.lead') }}</p>
    <el-form label-position="top">
      <el-form-item :label="t('pwd.old')"><el-input v-model="oldPwd" type="password" show-password size="large" /></el-form-item>
      <el-form-item :label="t('pwd.new')"><el-input v-model="newPwd" type="password" show-password size="large" /></el-form-item>
      <el-form-item :label="t('pwd.confirm')"><el-input v-model="confirm" type="password" show-password size="large" @keyup.enter="submit" /></el-form-item>
    </el-form>
    <template #footer>
      <el-button type="primary" :loading="loading" @click="submit" style="width:100%">{{ t('pwd.submit') }}</el-button>
    </template>
  </el-dialog>
</template>
