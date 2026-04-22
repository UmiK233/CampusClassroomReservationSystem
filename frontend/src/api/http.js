import axios from 'axios'
import { ElMessage } from 'element-plus'
import { clearAuth, getToken } from '../stores/auth'
import router from '../router'

const http = axios.create({
  baseURL: '/api',
  timeout: 12000
})

http.interceptors.request.use(config => {
  const token = getToken()
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

http.interceptors.response.use(
  response => {
    const body = response.data
    if (body && typeof body.code !== 'undefined' && body.code !== 200) {
      const message = body.message || '请求失败'
      ElMessage.error(message)
      if (body.code === 401) {
        clearAuth()
        router.replace('/login')
      }
      return Promise.reject(new Error(message))
    }
    return body?.data ?? body
  },
  error => {
    const status = error.response?.status
    const message = error.response?.data?.message || error.message || '网络请求失败'
    if (status === 401) {
      clearAuth()
      router.replace('/login')
    }
    ElMessage.error(message)
    return Promise.reject(error)
  }
)

export default http
