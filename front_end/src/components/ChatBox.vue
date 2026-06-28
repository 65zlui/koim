<template>
  <div class="chat-box">
    <div ref="scrollEl" class="messages">
      <MessageBubble
        v-for="m in messages"
        :key="m.msgId"
        :msg="m"
        :mine="m.fromUid === selfUid"
      />
      <div v-if="messages.length === 0" class="hint">No messages yet — say hello.</div>
    </div>
    <footer class="composer">
      <div class="emoji-wrapper">
        <button
          type="button"
          class="emoji-btn"
          :class="{ active: showEmoji }"
          @click="toggleEmoji"
          aria-label="Insert emoji"
        >😊</button>
        <div v-show="showEmoji" class="emoji-popover" @click.stop>
          <emoji-picker ref="pickerEl" class="light"></emoji-picker>
        </div>
      </div>
      <textarea
        ref="inputEl"
        v-model="text"
        rows="1"
        placeholder="Type a message and hit Enter to send"
        @keydown.enter.exact.prevent="send"
      />
      <button :disabled="!canSend" @click="send">Send</button>
    </footer>
  </div>
</template>

<script setup>
import { computed, nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import MessageBubble from './MessageBubble.vue'
import 'emoji-picker-element'

const props = defineProps({
  conversation: { type: Object, required: true },
  messages: { type: Array, default: () => [] },
  selfUid: { type: Number, required: true },
})
const emit = defineEmits(['send'])

const text = ref('')
const scrollEl = ref(null)
const inputEl = ref(null)
const pickerEl = ref(null)
const showEmoji = ref(false)

const canSend = computed(() => text.value.trim().length > 0)

const send = () => {
  if (!canSend.value) return
  emit('send', text.value.trim())
  text.value = ''
  showEmoji.value = false
}

const toggleEmoji = () => {
  showEmoji.value = !showEmoji.value
}

const insertEmoji = (emoji) => {
  const el = inputEl.value
  if (!el) {
    text.value += emoji
    return
  }
  const start = el.selectionStart ?? text.value.length
  const end = el.selectionEnd ?? text.value.length
  text.value = text.value.slice(0, start) + emoji + text.value.slice(end)
  nextTick(() => {
    el.focus()
    const pos = start + emoji.length
    el.selectionStart = el.selectionEnd = pos
  })
}

const onEmojiClick = (event) => {
  const emoji = event.detail?.unicode
  if (emoji) insertEmoji(emoji)
}

const onDocClick = (event) => {
  if (!showEmoji.value) return
  const target = event.target
  if (!target.closest || !target.closest('.emoji-wrapper')) {
    showEmoji.value = false
  }
}

watch(
  () => props.messages.length,
  () => {
    nextTick(() => {
      if (scrollEl.value) scrollEl.value.scrollTop = scrollEl.value.scrollHeight
    })
  },
)

// Switching conversation should hide the picker.
watch(
  () => props.conversation && (props.conversation.peerType + ':' + props.conversation.peerId),
  () => { showEmoji.value = false },
)

onMounted(() => {
  document.addEventListener('click', onDocClick)
  // The picker is a Web Component; native event listener is required.
  nextTick(() => {
    pickerEl.value?.addEventListener('emoji-click', onEmojiClick)
  })
})

onBeforeUnmount(() => {
  document.removeEventListener('click', onDocClick)
  pickerEl.value?.removeEventListener('emoji-click', onEmojiClick)
})
</script>

<style scoped>
.chat-box { flex: 1; display: flex; flex-direction: column; min-height: 0; }
.messages {
  flex: 1;
  overflow-y: auto;
  padding: 16px 20px;
  background: #f7f8fa;
}
.hint { color: #a3a8b3; text-align: center; padding: 40px 0; font-size: 13px; }
.composer {
  position: relative;
  display: flex;
  align-items: flex-end;
  padding: 10px 14px;
  border-top: 1px solid #eef0f2;
  background: white;
  gap: 10px;
}
.composer textarea {
  flex: 1;
  resize: none;
  height: 40px;
  font-family: inherit;
}
.emoji-wrapper { position: relative; display: flex; align-items: center; }
.emoji-btn {
  background: transparent;
  border: 1px solid #d6dbe2;
  font-size: 20px;
  width: 40px;
  height: 40px;
  border-radius: 8px;
  cursor: pointer;
  padding: 0;
  line-height: 1;
}
.emoji-btn:hover { background: #f3f5f8; }
.emoji-btn.active { background: #eaf2ff; border-color: #3370ff; }
.emoji-popover {
  position: absolute;
  bottom: 48px;
  left: 0;
  z-index: 1000;
  box-shadow: 0 8px 24px rgba(15, 23, 42, 0.18);
  border-radius: 8px;
  overflow: hidden;
}
emoji-picker {
  --background: #ffffff;
  --border-color: #e6e8eb;
  height: 360px;
  width: 320px;
}
</style>
