import axios from 'axios'

/** 统一 axios 实例：对接后端 License/运营服务（开发期由 vite 代理到 :8080）。 */
export const http = axios.create({
  baseURL: '/api',
  timeout: 15000,
})

http.interceptors.request.use((cfg) => {
  const token = localStorage.getItem('cmssoas.token')
  if (token) cfg.headers.Authorization = `Bearer ${token}`
  return cfg
})

http.interceptors.response.use(
  (resp) => resp.data,
  (err) => {
    if (err?.response?.status === 401 && !location.pathname.startsWith('/login')) {
      localStorage.removeItem('cmssoas.token')
      const redirect = encodeURIComponent(location.pathname + location.search)
      location.href = '/login?redirect=' + redirect
    }
    return Promise.reject(err)
  },
)

/** 带鉴权的文件下载（CSV / .lic 等）：以 blob 拉取后触发浏览器下载，避免 window.open 丢失 JWT。 */
export async function downloadFile(path: string, filename: string) {
  const resp = await http.get(path, { responseType: 'blob' }) as unknown as Blob
  const url = URL.createObjectURL(resp)
  const a = document.createElement('a')
  a.href = url
  a.download = filename
  document.body.appendChild(a)
  a.click()
  a.remove()
  URL.revokeObjectURL(url)
}
