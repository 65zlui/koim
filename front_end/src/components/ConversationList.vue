<template>
  <div class="conv-list">
    <div
      v-for="c in conversations"
      :key="c.peerType + ':' + c.peerId"
      class="conv-item"
      :class="{ active: isActive(c) }"
      @click="$emit('select', c)"
    >
      <div class="avatar">{{ avatarChar(c) }}</div>
      <div class="meta">
        <div class="row">
          <span class="name">{{ peerLabel(c) }}</span>
          <span class="time">{{ formatTime(c.lastMsgTime) }}</span>
        </div>
        <div class="row">
          <span class="preview">{{ c.lastMsgContent || '' }}</span>
          <span v-if="c.unreadCount > 0" class="badge">{{ c.unreadCount > 99 ? '99+' : c.unreadCount }}</span>
        </div>
      </div>
    </div>
    <div v-if="conversations.length === 0" class="empty">No conversations yet.</div>
  </div>
</template>

<script setup>
const props = defineProps({
  conversations: { type: Array, default: () => [] },
  active: { type: Object, default: null },
})
defineEmits(['select'])

const isActive = (c) =>
  props.active && c.peerType === props.active.peerType && c.peerId === props.active.peerId

const peerLabel = (c) => c.peerName || (c.peerType === 'g' ? `Group #${c.peerId}` : `User #${c.peerId}`)

const avatarChar = (c) => {
  const label = peerLabel(c)
  return label.charAt(0).toUpperCase()
}

const formatTime = (iso) => {
  if (!iso) return ''
  const d = new Date(iso)
  if (Number.isNaN(d.getTime())) return ''
  const today = new Date()
  if (d.toDateString() === today.toDateString()) {
    return d.toTimeString().slice(0, 5)
  }
  return `${d.getMonth() + 1}/${d.getDate()}`
}
</script>

<style scoped>
.conv-list {
  flex: 1;
  overflow-y: auto;
}
.conv-item {
  display: flex;
  padding: 12px 16px;
  cursor: pointer;
  border-bottom: 1px solid #f5f6f7;
}
.conv-item:hover { background: #f7f8fa; }
.conv-item.active { background: #eaf2ff; }
.avatar {
  width: 38px;
  height: 38px;
  border-radius: 50%;
  background: #3370ff;
  color: white;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 600;
  margin-right: 10px;
  flex-shrink: 0;
}
.meta { flex: 1; min-width: 0; }
.row { display: flex; justify-content: space-between; align-items: center; }
.name { font-size: 14px; font-weight: 600; color: #1f2329; }
.time { font-size: 12px; color: #a3a8b3; }
.preview {
  font-size: 13px;
  color: #646a73;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  max-width: 180px;
}
.badge {
  background: #f54a45;
  color: white;
  font-size: 11px;
  padding: 1px 6px;
  border-radius: 10px;
  min-width: 18px;
  text-align: center;
}
.empty {
  padding: 24px;
  text-align: center;
  color: #a3a8b3;
  font-size: 13px;
}
</style>
