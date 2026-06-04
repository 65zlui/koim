<template>
  <div class="auth-card">
    <h2>Sign in to Koim</h2>
    <div v-if="error" class="error">{{ error }}</div>
    <div class="field">
      <label>Username</label>
      <input v-model="username" autocomplete="username" />
    </div>
    <div class="field">
      <label>Password</label>
      <input v-model="password" type="password" autocomplete="current-password" @keyup.enter="submit" />
    </div>
    <div class="actions">
      <router-link to="/register">Create account</router-link>
      <button :disabled="loading" @click="submit">{{ loading ? '...' : 'Login' }}</button>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { login } from '@/api/user'
import { useAuthStore } from '@/stores/auth'
import { useWebSocketStore } from '@/stores/websocket'

const router = useRouter()
const auth = useAuthStore()
const ws = useWebSocketStore()

const username = ref('')
const password = ref('')
const loading = ref(false)
const error = ref('')

const submit = async () => {
  if (!username.value || !password.value) {
    error.value = 'Please fill in both fields'
    return
  }
  error.value = ''
  loading.value = true
  try {
    const { data } = await login(username.value, password.value)
    auth.setAuth(data.token, data.uid)
    ws.connect(data.token)
    router.push('/')
  } catch (e) {
    error.value = e.message
  } finally {
    loading.value = false
  }
}
</script>
