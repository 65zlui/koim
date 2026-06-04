<template>
  <div class="auth-card">
    <h2>Create account</h2>
    <div v-if="error" class="error">{{ error }}</div>
    <div class="field">
      <label>Username</label>
      <input v-model="username" />
    </div>
    <div class="field">
      <label>Nickname</label>
      <input v-model="nickname" />
    </div>
    <div class="field">
      <label>Password</label>
      <input v-model="password" type="password" @keyup.enter="submit" />
    </div>
    <div class="actions">
      <router-link to="/login">Have an account? Login</router-link>
      <button :disabled="loading" @click="submit">{{ loading ? '...' : 'Register' }}</button>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { register, login } from '@/api/user'
import { useAuthStore } from '@/stores/auth'
import { useWebSocketStore } from '@/stores/websocket'

const router = useRouter()
const auth = useAuthStore()
const ws = useWebSocketStore()

const username = ref('')
const nickname = ref('')
const password = ref('')
const loading = ref(false)
const error = ref('')

const submit = async () => {
  if (!username.value || !password.value) {
    error.value = 'Username and password are required'
    return
  }
  error.value = ''
  loading.value = true
  try {
    await register(username.value, password.value, nickname.value || username.value)
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
