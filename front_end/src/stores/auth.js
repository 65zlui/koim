import { defineStore } from 'pinia'

export const useAuthStore = defineStore('auth', {
  state: () => ({
    token: localStorage.getItem('token') || '',
    uid: parseInt(localStorage.getItem('uid') || '0', 10) || 0,
  }),
  actions: {
    setAuth(token, uid) {
      this.token = token
      this.uid = Number(uid)
      localStorage.setItem('token', token)
      localStorage.setItem('uid', String(uid))
    },
    logout() {
      this.token = ''
      this.uid = 0
      localStorage.removeItem('token')
      localStorage.removeItem('uid')
      localStorage.removeItem('lastSeq')
    },
  },
})
