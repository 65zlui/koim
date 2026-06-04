import { defineStore } from 'pinia'
import { ref } from 'vue'

/**
 * In-memory message buffer keyed by conversation. The backend `/message/sync`
 * only returns offline messages, so this store represents what the user has
 * seen during this session.
 */
export const useMessageStore = defineStore('messages', () => {
  // Map<peerKey, MessageDto[]>
  const byConversation = ref(new Map())

  const peerKey = (peerType, peerId) => `${peerType}:${peerId}`

  const get = (peerType, peerId) => byConversation.value.get(peerKey(peerType, peerId)) || []

  const append = (peerType, peerId, msg) => {
    const k = peerKey(peerType, peerId)
    const list = byConversation.value.get(k) || []
    if (list.some((m) => m.msgId === msg.msgId)) return
    list.push(msg)
    list.sort((a, b) => (a.seq || 0) - (b.seq || 0))
    byConversation.value.set(k, list)
    // Trigger reactivity for Map.
    byConversation.value = new Map(byConversation.value)
  }

  /**
   * Patch a message in-place via the reactive list so the UI re-renders.
   * Plain mutation of the raw object reference (passed into `append`) does
   * not trigger reactivity because the list stores the Proxy-wrapped copy.
   */
  const updateByMsgId = (peerType, peerId, msgId, patch) => {
    const k = peerKey(peerType, peerId)
    const list = byConversation.value.get(k)
    if (!list) return
    const target = list.find((m) => m.msgId === msgId)
    if (!target) return
    Object.assign(target, patch)
    list.sort((a, b) => (a.seq || 0) - (b.seq || 0))
    byConversation.value = new Map(byConversation.value)
  }

  const clear = () => {
    byConversation.value = new Map()
  }

  return { byConversation, get, append, updateByMsgId, clear, peerKey }
})
