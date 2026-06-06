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
  (err) => Promise.reject(err),
)
