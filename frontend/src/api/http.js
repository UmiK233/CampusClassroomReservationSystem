import axios from 'axios'
import { ElMessage } from 'element-plus'
import pinia from '../stores'
import { useAuthStore } from '../stores/auth'
import router from '../router'

const http = axios.create({
  baseURL: '/api',
  timeout: 12000
})

http.interceptors.request.use(config => {
  const authStore = useAuthStore(pinia)
  const token = authStore.token
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

http.interceptors.response.use(
  response => {
    const body = response.data
    const silentError = response.config?.silentError
    if (body && typeof body.code !== 'undefined' && body.code !== 200) {
      const message = body.message || '请求失败'
      if (!silentError) {
        ElMessage.error(message)
      }
      if (body.code === 401) {
        const authStore = useAuthStore(pinia)
        authStore.clearAuth()
        router.replace('/login')
      }
      return Promise.reject(new Error(message))
    }
    return body?.data ?? body
  },
  error => {
    const status = error.response?.status
    const message = error.response?.data?.message || error.message || '网络请求失败'
    const silentError = error.config?.silentError
    if (status === 401) {
      const authStore = useAuthStore(pinia)
      authStore.clearAuth()
      router.replace('/login')
    }
    if (!silentError) {
      ElMessage.error(message)
    }
    return Promise.reject(error)
  }
)

export default http
