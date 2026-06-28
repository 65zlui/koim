<template>
  <div class="chat-shell">
    <aside class="sidebar">
      <header class="sidebar-header">
        <div>
          <strong>{{ auth.uid ? `UID ${auth.uid}` : '-' }}</strong>
          <span class="ws-dot" :class="{ on: ws.connected }" :title="ws.connected ? 'online' : 'offline'" />
        </div>
        <div class="header-btns">
          <button class="ghost" :class="{ active: showProfile }" @click="showProfile = !showProfile" title="Profile">👤</button>
          <button class="ghost" @click="logout">Logout</button>
        </div>
      </header>
      <UserProfile v-if="showProfile" @open-chat="onOpenChat" />
      <GroupPanel @reloaded="onReload" />
      <ConversationList
        :conversations="conv.list"
        :active="conv.active"
        @select="selectConversation"
      />
    </aside>
    <main class="main-pane">
      <template v-if="conv.active">
        <div class="chat-header">
          <span class="chat-title">{{ chatTitle }}</span>
          <button
            v-if="conv.active.peerType === 'g'"
            class="settings-btn"
            :class="{ active: showGroupSettings }"
            @click="showGroupSettings = !showGroupSettings"
          >⚙</button>
        </div>
        <div class="chat-body">
          <ChatBox
            :conversation="conv.active"
            :messages="currentMessages"
            :self-uid="auth.uid"
            @send="handleSend"
          />
          <GroupSettings
            v-if="conv.active.peerType === 'g'"
            :group-id="conv.active.peerId"
            :self-uid="auth.uid"
            :visible="showGroupSettings"
            @close="showGroupSettings = false"
            @left="onGroupLeft"
            @kicked="onReload"
            @updated="onReload"
          />
        </div>
      </template>
      <div v-else class="empty">Select a conversation to start chatting.</div>
    </main>
  </div>
</template>

<script setup>
import { computed, onMounted, onBeforeUnmount, ref } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { useWebSocketStore } from '@/stores/websocket'
import { useConversationStore } from '@/stores/conversation'
import { useMessageStore } from '@/stores/messages'
import { sendMessage, syncMessages, confirmMessages } from '@/api/message'
import ConversationList from '@/components/ConversationList.vue'
import ChatBox from '@/components/ChatBox.vue'
import GroupPanel from '@/components/GroupPanel.vue'
import GroupSettings from '@/components/GroupSettings.vue'
import UserProfile from '@/components/UserProfile.vue'

const router = useRouter()
const auth = useAuthStore()
const ws = useWebSocketStore()
const conv = useConversationStore()
const msgStore = useMessageStore()
const showGroupSettings = ref(false)
const showProfile = ref(false)

const chatTitle = computed(() => {
  const c = conv.active
  if (!c) return ''
  return c.peerName || (c.peerType === 'g' ? `Group #${c.peerId}` : `User #${c.peerId}`)
})

const currentMessages = computed(() => {
  if (!conv.active) return []
  return msgStore.get(conv.active.peerType, conv.active.peerId)
})

const lastSeq = () => Number(localStorage.getItem('lastSeq') || 0) || null
const setLastSeq = (s) => { if (s) localStorage.setItem('lastSeq', String(s)) }

/** Handle a message arriving via WS push or offline sync. */
const ingest = (msg) => {
  const peerType = msg.groupId ? 'g' : 'u'
  const peerId = msg.groupId ? msg.groupId : msg.fromUid
  msgStore.append(peerType, peerId, msg)

  const isActive = conv.active &&
    conv.active.peerType === peerType && conv.active.peerId === peerId
  conv.upsertFromMessage({
    peerType,
    peerId,
    content: msg.content,
    sendTime: msg.sendTime,
    bumpUnread: !isActive,
  })
  if (msg.seq) setLastSeq(Math.max(lastSeq() || 0, msg.seq))
  if (isActive) conv.markActiveRead().catch(() => {})
}

const pullOffline = async () => {
  const { data } = await syncMessages(lastSeq())
  const messages = data?.messages || []
  for (const m of messages) ingest(m)
  if (messages.length) {
    await confirmMessages(messages.map((m) => m.msgId))
  }
}

const handleSend = async (content) => {
  if (!conv.active || !content.trim()) return
  const target = conv.active
  const msgId = `${Date.now()}-${Math.random().toString(36).slice(2, 8)}`
  const toUid = target.peerType === 'u' ? target.peerId : null
  const groupId = target.peerType === 'g' ? target.peerId : null

  // Optimistic local echo.
  const localMsg = {
    msgId,
    seq: null,
    fromUid: auth.uid,
    toUid,
    groupId,
    msgType: 1,
    content,
    sendTime: new Date().toISOString(),
    pending: true,
  }
  msgStore.append(target.peerType, target.peerId, localMsg)
  conv.upsertFromMessage({
    peerType: target.peerType,
    peerId: target.peerId,
    content,
    sendTime: localMsg.sendTime,
    bumpUnread: false,
  })

  try {
    const { data } = await sendMessage(msgId, toUid, groupId, 1, content)
    msgStore.updateByMsgId(target.peerType, target.peerId, msgId, {
      seq: data.seq,
      sendTime: data.sendTime,
      pending: false,
    })
    if (data.seq) setLastSeq(Math.max(lastSeq() || 0, data.seq))
  } catch (e) {
    msgStore.updateByMsgId(target.peerType, target.peerId, msgId, {
      pending: false,
      failed: true,
    })
    console.error('send failed', e)
  }
}

const selectConversation = async (c) => {
  conv.setActive(c)
  try { await conv.markActiveRead() } catch (_) { /* noop */ }
}

const onReload = async () => {
  await conv.reload()
}

const onGroupLeft = () => {
  showGroupSettings.value = false
}

const onOpenChat = (user) => {
  showProfile.value = false
  conv.openLocal({ peerType: 'u', peerId: user.uid, peerName: user.nickname || user.username })
}

const logout = () => {
  ws.disconnect()
  auth.logout()
  msgStore.clear()
  router.push('/login')
}

const wsHandler = (data) => {
  if (data?.type === 'msg') ingest(data)
}

onMounted(async () => {
  ws.connect(auth.token)
  ws.addMessageHandler(wsHandler)
  await conv.reload()
  await pullOffline()
})

onBeforeUnmount(() => {
  ws.removeMessageHandler(wsHandler)
})
</script>

<style scoped>
.chat-shell {
  display: flex;
  height: 100vh;
}
.sidebar {
  width: 320px;
  background: white;
  border-right: 1px solid #e6e8eb;
  display: flex;
  flex-direction: column;
}
.sidebar-header {
  padding: 14px 16px;
  border-bottom: 1px solid #f0f1f3;
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.sidebar-header strong { font-weight: 600; }
.header-btns { display: flex; gap: 6px; align-items: center; }
.ghost.active { background: #eaf2ff; border-color: #3370ff; }
.ws-dot {
  display: inline-block;
  width: 8px;
  height: 8px;
  border-radius: 50%;
  margin-left: 6px;
  background: #c0c6cf;
}
.ws-dot.on { background: #22c55e; }
.ghost {
  background: transparent;
  color: #646a73;
  padding: 4px 10px;
  border: 1px solid #d6dbe2;
}
.ghost:hover { background: #f3f5f8; }
.main-pane { flex: 1; display: flex; flex-direction: column; background: #fff; min-width: 0; }
.chat-body { flex: 1; display: flex; min-height: 0; }
.chat-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 14px 20px;
  border-bottom: 1px solid #eef0f2;
  background: white;
}
.chat-title { font-weight: 600; font-size: 15px; }
.settings-btn {
  background: none;
  border: 1px solid #d6dbe2;
  border-radius: 6px;
  width: 32px;
  height: 32px;
  font-size: 16px;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
}
.settings-btn:hover { background: #f3f5f8; }
.settings-btn.active { background: #eaf2ff; border-color: #3370ff; }
.empty {
  margin: auto;
  color: #a3a8b3;
  font-size: 14px;
}
</style>
