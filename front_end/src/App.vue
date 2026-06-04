<template>
  <router-view />
</template>

<script setup>
import { onMounted } from 'vue'
import { useAuthStore } from '@/stores/auth'
import { useWebSocketStore } from '@/stores/websocket'

const auth = useAuthStore()
const ws = useWebSocketStore()

// Re-establish WS on hard reload while still authenticated.
onMounted(() => {
  if (auth.token) ws.connect(auth.token)
})
</script>
