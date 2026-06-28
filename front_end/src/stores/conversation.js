import { defineStore } from 'pinia'
import { ref } from 'vue'
import * as convApi from '@/api/conversation'

export const useConversationStore = defineStore('conversation', () => {
  const list = ref([]) // ConversationDto[]
  const active = ref(null)

  const reload = async () => {
    const res = await convApi.fetchConversations()
    list.value = res.data || []
  }

  const setActive = (conv) => {
    active.value = conv
  }

  /**
   * Ensure a conversation entry exists locally and set it active.
   * Used to bootstrap a chat (group just created/joined or a peer UID we know)
   * before any message has been exchanged.
   */
  const openLocal = ({ peerType, peerId, peerName }) => {
    let item = list.value.find((c) => c.peerType === peerType && c.peerId === peerId)
    if (!item) {
      item = {
        peerType,
        peerId,
        peerName: peerName || null,
        lastMsgContent: '',
        lastMsgTime: null,
        unreadCount: 0,
      }
      list.value.unshift(item)
    } else if (peerName && !item.peerName) {
      item.peerName = peerName
    }
    active.value = item
    return item
  }

  /** Bump or insert a conversation entry locally when receiving a real-time message. */
  const upsertFromMessage = ({ peerType, peerId, peerName, content, sendTime, bumpUnread }) => {
    const idx = list.value.findIndex(
      (c) => c.peerType === peerType && c.peerId === peerId,
    )
    if (idx === -1) {
      list.value.unshift({
        peerType,
        peerId,
        peerName: peerName || null,
        lastMsgContent: content,
        lastMsgTime: sendTime,
        unreadCount: bumpUnread ? 1 : 0,
      })
    } else {
      const c = list.value[idx]
      c.lastMsgContent = content
      c.lastMsgTime = sendTime
      if (bumpUnread) c.unreadCount = (c.unreadCount || 0) + 1
      // Move to top.
      list.value.splice(idx, 1)
      list.value.unshift(c)
    }
  }

  const markActiveRead = async () => {
    if (!active.value) return
    await convApi.markRead(active.value.peerType, active.value.peerId)
    const item = list.value.find(
      (c) => c.peerType === active.value.peerType && c.peerId === active.value.peerId,
    )
    if (item) item.unreadCount = 0
  }

  /** Remove a conversation locally (e.g. after leaving a group or being kicked). */
  const removeLocal = (peerType, peerId) => {
    const idx = list.value.findIndex(
      (c) => c.peerType === peerType && c.peerId === peerId,
    )
    if (idx !== -1) list.value.splice(idx, 1)
    if (
      active.value &&
      active.value.peerType === peerType &&
      active.value.peerId === peerId
    ) {
      active.value = null
    }
  }

  return { list, active, reload, setActive, openLocal, upsertFromMessage, markActiveRead, removeLocal }
})
