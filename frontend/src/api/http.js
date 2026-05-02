import axios from 'axios'
import { ElMessage } from 'element-plus'
import pinia from '../stores'
import { useAuthStore } from '../stores/auth'
import router from '../router'

const http = axios.create({
  baseURL: '/api',
  timeout: 12000
})

const refreshClient = axios.create({
  baseURL: '/api',
  timeout: 12000
})

let refreshPromise = null

function buildApiError(message, body) {
  const apiError = new Error(message)
  apiError.businessCode = body?.code
  apiError.response = { data: body }
  return apiError
}

function isAuthRoute(url = '') {
  return url.includes('/auth/login') || url.includes('/auth/refresh')
}

function redirectToLogin() {
  if (router.currentRoute.value.path !== '/login') {
    router.replace('/login')
  }
}

async function refreshAccessToken(authStore) {
  if (!authStore.refreshToken) {
    throw new Error('missing refresh token')
  }

  if (!refreshPromise) {
    refreshPromise = refreshClient
      .post('/auth/refresh', { refreshToken: authStore.refreshToken })
      .then(response => {
        const body = response.data
        if (body && typeof body.code !== 'undefined' && body.code !== 200) {
          throw buildApiError(body.message || '刷新令牌失败', body)
        }
        const data = body?.data ?? body
        authStore.setAuth(data.accessToken, data.refreshToken, data.userInfo ?? authStore.user)
        return data.accessToken
      })
      .finally(() => {
        refreshPromise = null
      })
  }

  return refreshPromise
}

async function retryWithRefresh(config, originalError) {
  const authStore = useAuthStore(pinia)
  if (config?._retry || config?.skipAuthRefresh || isAuthRoute(config?.url) || !authStore.refreshToken) {
    authStore.clearAuth()
    redirectToLogin()
    throw originalError
  }

  config._retry = true
  try {
    const accessToken = await refreshAccessToken(authStore)
    config.headers = config.headers ?? {}
    config.headers.Authorization = `Bearer ${accessToken}`
    return http(config)
  } catch (refreshError) {
    authStore.clearAuth()
    redirectToLogin()
    throw refreshError
  }
}

http.interceptors.request.use(config => {
  const authStore = useAuthStore(pinia)
  const accessToken = authStore.accessToken
  if (accessToken) {
    config.headers.Authorization = `Bearer ${accessToken}`
  }
  return config
})

http.interceptors.response.use(
  response => {
    const body = response.data
    const silentError = response.config?.silentError
    if (body && typeof body.code !== 'undefined' && body.code !== 200) {
      if (body.code === 401) {
        return retryWithRefresh(response.config, buildApiError(body.message || '请求失败', body))
      }
      if (!silentError) {
        ElMessage.error(body.message || '请求失败')
      }
      return Promise.reject(buildApiError(body.message || '请求失败', body))
    }
    return body?.data ?? body
  },
  error => {
    const status = error.response?.status
    const message = error.response?.data?.message || error.message || '网络请求失败'
    const silentError = error.config?.silentError
    if (status === 401) {
      return retryWithRefresh(error.config, error)
    }
    if (!silentError) {
      ElMessage.error(message)
    }
    return Promise.reject(error)
  }
)

export default http
