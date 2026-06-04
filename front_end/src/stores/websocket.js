import { defineStore } from 'pinia'
import { ref } from 'vue'

// Same host-derivation trick as api/client.js: phone on LAN must hit the Mac's
// IP, not its own localhost.
const defaultWsBase = (() => {
  if (typeof window === 'undefined') return 'ws://localhost:8080/ws/chat'
  const host = window.location.hostname || 'localhost'
  const proto = window.location.protocol === 'https:' ? 'wss' : 'ws'
  return `${proto}://${host}:8080/ws/chat`
})()

const wsBase = import.meta.env.VITE_WS_BASE || defaultWsBase

export const useWebSocketStore = defineStore('websocket', () => {
  const ws = ref(null)
  const connected = ref(false)
  const handlers = ref([])
  let manualClose = false
  let retryTimer = null

  const dispatch = (data) => {
    handlers.value.forEach((h) => {
      try { h(data) } catch (e) { console.error('ws handler error', e) }
    })
  }

  const connect = (token) => {
    if (!token) return
    if (ws.value && (ws.value.readyState === WebSocket.OPEN || ws.value.readyState === WebSocket.CONNECTING)) {
      return
    }
    manualClose = false
    const url = `${wsBase}?token=${encodeURIComponent(token)}`
    const sock = new WebSocket(url)
    ws.value = sock

    sock.onopen = () => { connected.value = true }
    sock.onmessage = (ev) => {
      try { dispatch(JSON.parse(ev.data)) }
      catch (e) { console.warn('non-JSON ws message', ev.data) }
    }
    sock.onclose = () => {
      connected.value = false
      ws.value = null
      if (manualClose) return
      const t = localStorage.getItem('token')
      if (!t) return
      retryTimer = setTimeout(() => connect(t), 3000)
    }
    sock.onerror = () => { /* close handler will retry */ }
  }

  const disconnect = () => {
    manualClose = true
    if (retryTimer) { clearTimeout(retryTimer); retryTimer = null }
    if (ws.value) ws.value.close()
    ws.value = null
    connected.value = false
  }

  const addMessageHandler = (h) => { handlers.value.push(h) }
  const removeMessageHandler = (h) => {
    const i = handlers.value.indexOf(h)
    if (i !== -1) handlers.value.splice(i, 1)
  }

  return { ws, connected, connect, disconnect, addMessageHandler, removeMessageHandler }
})
