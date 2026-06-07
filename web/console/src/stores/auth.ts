import { defineStore } from 'pinia'
import { ref } from 'vue'
import * as authApi from '@/api/auth'

const TOKEN_KEY = 'codeman.token'

export const useAuthStore = defineStore('auth', () => {
  const token = ref<string>(localStorage.getItem(TOKEN_KEY) || '')
  const username = ref('')
  const role = ref('')
  const roleName = ref('')
  const mustChangePwd = ref(false)
  const perms = ref<Record<string, string>>({})

  function applyPerms(list: authApi.PermItem[]) {
    const m: Record<string, string> = {}
    for (const p of list) m[p.code] = p.mode
    perms.value = m
  }

  async function login(u: string, p: string) {
    const r = await authApi.login(u, p)
    token.value = r.token || ''
    localStorage.setItem(TOKEN_KEY, token.value)
    username.value = r.username
    role.value = r.role
    roleName.value = r.roleName
    mustChangePwd.value = r.mustChangePwd
    applyPerms(r.permissions)
  }

  async function fetchMe() {
    const r = await authApi.me()
    username.value = r.username
    role.value = r.role
    roleName.value = r.roleName
    mustChangePwd.value = r.mustChangePwd
    applyPerms(r.permissions)
  }

  async function changePassword(oldPassword: string, newPassword: string) {
    await authApi.changePassword(oldPassword, newPassword)
    mustChangePwd.value = false
  }

  function logout() {
    token.value = ''
    username.value = ''
    role.value = ''
    perms.value = {}
    localStorage.removeItem(TOKEN_KEY)
  }

  const isAuthed = () => !!token.value
  const isSuper = () => role.value === 'SUPER_ADMIN'
  /** 是否拥有某权限（mode 非 NONE 即视为有；可传 minMode 收紧）。 */
  const has = (code: string) => !!perms.value[code] && perms.value[code] !== 'NONE'

  return { token, username, role, roleName, mustChangePwd, perms, login, fetchMe, changePassword, logout, isAuthed, isSuper, has }
})
