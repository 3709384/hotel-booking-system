import { defineStore } from 'pinia'
import api, { TOKEN_KEY, USER_KEY } from '../api/http'

function readUser() {
  try {
    return JSON.parse(localStorage.getItem(USER_KEY) || 'null')
  } catch {
    return null
  }
}

export const useAuthStore = defineStore('auth', {
  state: () => ({
    user: readUser(),
    token: localStorage.getItem(TOKEN_KEY),
  }),
  getters: {
    isLoggedIn: (state) => Boolean(state.token && state.user),
    role: (state) => state.user?.role || '',
    roleLabel: (state) => ({
      MEMBER: '会员',
      RECEPTIONIST: '前台',
      ADMIN: '管理员',
    })[state.user?.role] || '访客',
    defaultRoute: (state) => {
      if (state.user?.role === 'ADMIN') return '/admin'
      if (state.user?.role === 'RECEPTIONIST') return '/pms'
      return '/rooms'
    },
  },
  actions: {
    async login(phone, password) {
      const response = await api.post('/auth/login', { phone, password })
      this.token = response.data.token
      this.user = { ...response.data, phone }
      localStorage.setItem(TOKEN_KEY, this.token)
      localStorage.setItem(USER_KEY, JSON.stringify(this.user))
      return this.user
    },
    logout() {
      this.token = null
      this.user = null
      localStorage.removeItem(TOKEN_KEY)
      localStorage.removeItem(USER_KEY)
    },
  },
})
