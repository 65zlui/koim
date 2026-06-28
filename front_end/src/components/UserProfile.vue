<template>
  <div class="user-panel">
    <!-- Profile section -->
    <section class="section">
      <label>Profile</label>
      <div class="field">
        <span class="label">Nickname</span>
        <input v-model="profile.nickname" placeholder="Nickname" />
      </div>
      <div class="field">
        <span class="label">Status</span>
        <input v-model="profile.status" placeholder="What's on your mind?" />
      </div>
      <div class="field">
        <span class="label">Avatar URL</span>
        <input v-model="profile.avatarUrl" placeholder="https://..." />
      </div>
      <button class="save-btn" :disabled="saving" @click="onSaveProfile">Save</button>
    </section>

    <!-- Password section -->
    <section class="section">
      <label>Change Password</label>
      <input v-model="pw.old" type="password" placeholder="Current password" />
      <input v-model="pw.new1" type="password" placeholder="New password (6+ chars)" />
      <input v-model="pw.new2" type="password" placeholder="Confirm new password" />
      <button class="save-btn" :disabled="changingPw" @click="onChangePassword">Update Password</button>
    </section>

    <!-- Search section -->
    <section class="section">
      <label>Find Users</label>
      <div class="search-row">
        <input v-model="searchQ" placeholder="Search by name..." @input="onSearch" />
      </div>
      <ul v-if="results.length" class="results">
        <li v-for="u in results" :key="u.uid" class="result-item" @click="$emit('openChat', u)">
          <span class="r-name">{{ u.nickname || u.username }}</span>
          <span class="r-uid">UID {{ u.uid }}</span>
          <span v-if="u.status" class="r-status">{{ u.status }}</span>
        </li>
      </ul>
    </section>

    <div v-if="msg" class="flash" :class="{ err }">{{ msg }}</div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { getProfile, updateProfile, changePassword, searchUsers } from '@/api/user'

defineEmits(['openChat'])

const profile = reactive({ nickname: '', status: '', avatarUrl: '' })
const pw = reactive({ old: '', new1: '', new2: '' })
const searchQ = ref('')
const results = ref([])
const saving = ref(false)
const changingPw = ref(false)
const msg = ref('')
const err = ref(false)
let searchTimer = null

const flash = (text, isErr = false) => {
  msg.value = text
  err.value = isErr
  setTimeout(() => { msg.value = '' }, 3000)
}

const loadProfile = async () => {
  try {
    const { data } = await getProfile()
    profile.nickname = data.nickname || ''
    profile.status = data.status || ''
    profile.avatarUrl = data.avatarUrl || ''
  } catch (_) { /* noop */ }
}

const onSaveProfile = async () => {
  saving.value = true
  try {
    await updateProfile({
      nickname: profile.nickname || null,
      status: profile.status || null,
      avatarUrl: profile.avatarUrl || null,
    })
    flash('Profile updated')
  } catch (e) {
    flash(e.message || 'Failed', true)
  } finally {
    saving.value = false
  }
}

const onChangePassword = async () => {
  if (!pw.old || !pw.new1) return flash('Fill all password fields', true)
  if (pw.new1 !== pw.new2) return flash('New passwords do not match', true)
  if (pw.new1.length < 6) return flash('Password must be 6+ characters', true)
  changingPw.value = true
  try {
    await changePassword(pw.old, pw.new1)
    flash('Password changed')
    pw.old = ''; pw.new1 = ''; pw.new2 = ''
  } catch (e) {
    flash(e.response?.data?.error || e.message || 'Failed', true)
  } finally {
    changingPw.value = false
  }
}

const onSearch = () => {
  clearTimeout(searchTimer)
  searchTimer = setTimeout(async () => {
    if (!searchQ.value.trim()) { results.value = []; return }
    try {
      const { data } = await searchUsers(searchQ.value.trim())
      results.value = data || []
    } catch (_) { results.value = [] }
  }, 300)
}

onMounted(loadProfile)
</script>

<style scoped>
.user-panel {
  padding: 10px 14px;
  border-bottom: 1px solid #f0f1f3;
  background: #fafbfc;
  max-height: 340px;
  overflow-y: auto;
}
.section { margin-bottom: 10px; }
.section label {
  display: block;
  font-size: 11px;
  font-weight: 600;
  text-transform: uppercase;
  color: #8b919a;
  margin-bottom: 4px;
}
.field { margin-bottom: 4px; }
.field .label {
  display: block;
  font-size: 11px;
  color: #646a73;
  margin-bottom: 1px;
}
input {
  width: 100%;
  padding: 5px 8px;
  font-size: 12px;
  border: 1px solid #d6dbe2;
  border-radius: 4px;
  box-sizing: border-box;
}
.save-btn {
  margin-top: 4px;
  width: 100%;
  padding: 5px;
  font-size: 12px;
  background: #3370ff;
  color: white;
  border: none;
  border-radius: 4px;
  cursor: pointer;
}
.save-btn:hover { background: #2860e0; }
.save-btn:disabled { opacity: 0.5; cursor: not-allowed; }
.search-row { display: flex; gap: 6px; }
.results {
  list-style: none;
  margin: 6px 0 0;
  padding: 0;
  max-height: 120px;
  overflow-y: auto;
}
.result-item {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 5px 6px;
  cursor: pointer;
  border-radius: 4px;
  font-size: 12px;
}
.result-item:hover { background: #eaf2ff; }
.r-name { font-weight: 600; color: #1f2329; }
.r-uid { font-size: 11px; color: #8b919a; }
.r-status {
  margin-left: auto;
  font-size: 10px;
  color: #646a73;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  max-width: 80px;
}
.flash { font-size: 11px; color: #22c55e; padding-top: 4px; }
.flash.err { color: #f54a45; }
</style>
