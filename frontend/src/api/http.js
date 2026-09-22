import axios from 'axios'
import { ElMessage } from 'element-plus'

const TOKEN_KEY = 'hotel_token'
const USER_KEY = 'hotel_user'

const api = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || '/api',
  timeout: 15000,
})

api.interceptors.request.use((config) => {
  const token = localStorage.getItem(TOKEN_KEY)
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

api.interceptors.response.use(
  (response) => {
    const payload = response.data
    if (!payload || typeof payload !== 'object' || !('code' in payload)) {
      return payload
    }
    if (payload.code !== 0) {
      if (payload.code === 1001) {
        localStorage.removeItem(TOKEN_KEY)
        localStorage.removeItem(USER_KEY)
        if (window.location.pathname !== '/login') {
          window.location.href = '/login'
        }
      }
      return Promise.reject(new Error(payload.message || '请求失败'))
    }
    return payload
  },
  (error) => {
    const message = error.response?.data?.message || error.message || '网络请求失败'
    if (!error.response) {
      ElMessage.error('无法连接后端服务，请确认后端已启动')
    }
    return Promise.reject(new Error(message))
  },
)

export { TOKEN_KEY, USER_KEY }
export default api
