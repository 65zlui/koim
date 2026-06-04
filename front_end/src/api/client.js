import axios from 'axios'

// Derive backend base from the page host so phones on the same Wi-Fi work too.
// When the page is loaded via http://192.168.x.x:5173, the API will go to
// http://192.168.x.x:8080/api automatically.
const defaultApiBase = (() => {
  if (typeof window === 'undefined') return 'http://localhost:8080/api'
  const host = window.location.hostname || 'localhost'
  return `http://${host}:8080/api`
})()

const client = axios.create({
  baseURL: import.meta.env.VITE_API_BASE || defaultApiBase,
  timeout: 10000,
})

client.interceptors.request.use((config) => {
  const token = localStorage.getItem('token')
  if (token) config.headers.Authorization = `Bearer ${token}`
  return config
})

client.interceptors.response.use(
  (resp) => {
    // Backend wraps payloads in { ok, data, error }; surface .data when ok.
    const body = resp.data
    if (body && typeof body === 'object' && 'ok' in body) {
      if (!body.ok) {
        return Promise.reject(new Error(body.error || 'request failed'))
      }
      resp.data = body.data
    }
    return resp
  },
  (err) => {
    if (err.response?.status === 401) {
      localStorage.clear()
      if (location.pathname !== '/login') location.assign('/login')
    }
    const msg = err.response?.data?.error || err.message || 'network error'
    return Promise.reject(new Error(msg))
  },
)

export default client
