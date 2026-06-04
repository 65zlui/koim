<template>
  <div class="bubble-row" :class="{ mine }">
    <div class="bubble" :class="{ mine, pending: msg.pending, failed: msg.failed }">
      <div v-if="!mine" class="from">UID {{ msg.fromUid }}</div>
      <div class="content">{{ msg.content }}</div>
      <div class="meta">
        <span v-if="msg.failed" class="status err">failed</span>
        <span v-else-if="msg.pending" class="status">sending…</span>
        <span v-else class="time">{{ formatTime(msg.sendTime) }}</span>
      </div>
    </div>
  </div>
</template>

<script setup>
defineProps({
  msg: { type: Object, required: true },
  mine: { type: Boolean, default: false },
})

const formatTime = (iso) => {
  if (!iso) return ''
  const d = new Date(iso)
  return Number.isNaN(d.getTime()) ? '' : d.toTimeString().slice(0, 5)
}
</script>

<style scoped>
.bubble-row {
  display: flex;
  justify-content: flex-start;
  margin-bottom: 10px;
}
.bubble-row.mine { justify-content: flex-end; }
.bubble {
  max-width: 60%;
  padding: 8px 12px;
  border-radius: 8px;
  background: white;
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.04);
}
.bubble.mine { background: #3370ff; color: white; }
.bubble.pending { opacity: 0.7; }
.bubble.failed { background: #fde8e8; color: #c93939; }
.from { font-size: 11px; color: #a3a8b3; margin-bottom: 2px; }
.content { white-space: pre-wrap; word-break: break-word; font-size: 14px; }
.meta { font-size: 11px; opacity: 0.7; margin-top: 4px; text-align: right; }
.bubble.mine .from { color: rgba(255, 255, 255, 0.85); }
.status.err { color: #c93939; }
</style>
