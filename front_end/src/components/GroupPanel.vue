<template>
  <div class="group-panel">
    <div class="row">
      <input v-model="newGroupName" placeholder="New group name" />
      <button :disabled="!newGroupName.trim() || busy" @click="onCreate">Create</button>
    </div>
    <div class="row">
      <input v-model.number="joinId" type="number" placeholder="Group ID to join" />
      <button :disabled="!joinId || busy" @click="onJoin">Join</button>
    </div>
    <div class="row">
      <input v-model.number="peerUid" type="number" placeholder="Peer UID for single chat" />
      <button :disabled="!peerUid || busy" @click="onOpenSingle">Open</button>
    </div>
    <div v-if="msg" class="msg" :class="{ err: !!err }">{{ msg }}</div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { createGroup, joinGroup } from '@/api/group'
import { useConversationStore } from '@/stores/conversation'
import { useAuthStore } from '@/stores/auth'

const emit = defineEmits(['reloaded'])
const conv = useConversationStore()
const auth = useAuthStore()

const newGroupName = ref('')
const joinId = ref('')
const peerUid = ref('')
const busy = ref(false)
const msg = ref('')
const err = ref(false)

const flash = (text, isErr = false) => {
  msg.value = text
  err.value = isErr
  setTimeout(() => { msg.value = '' }, 2500)
}

const onCreate = async () => {
  busy.value = true
  try {
    const name = newGroupName.value.trim()
    const { data } = await createGroup(name, [])
    flash(`Created group #${data.groupId}`)
    newGroupName.value = ''
    emit('reloaded')
    // Open the freshly created group so the user can immediately type.
    conv.openLocal({ peerType: 'g', peerId: data.groupId, peerName: name })
  } catch (e) {
    flash(e.message, true)
  } finally {
    busy.value = false
  }
}

const onJoin = async () => {
  busy.value = true
  try {
    const gid = Number(joinId.value)
    const { data } = await joinGroup(gid)
    flash(`Joined "${data.name}" (${data.memberCount} members)`)
    joinId.value = ''
    emit('reloaded')
    // Open the joined group as the active conversation.
    conv.openLocal({ peerType: 'g', peerId: gid, peerName: data.name })
  } catch (e) {
    flash(e.message, true)
  } finally {
    busy.value = false
  }
}

const onOpenSingle = () => {
  const uid = Number(peerUid.value)
  if (!uid) return
  if (uid === auth.uid) {
    flash('Cannot chat with yourself', true)
    return
  }
  conv.openLocal({ peerType: 'u', peerId: uid })
  flash(`Opened chat with UID ${uid}`)
  peerUid.value = ''
}
</script>

<style scoped>
.group-panel {
  padding: 12px 16px;
  border-bottom: 1px solid #f0f1f3;
  background: #fafbfc;
}
.row {
  display: flex;
  gap: 8px;
  margin-bottom: 6px;
}
.row input { flex: 1; min-width: 0; }
.row button { white-space: nowrap; padding: 6px 12px; font-size: 13px; }
.msg { font-size: 12px; color: #22c55e; padding-top: 4px; }
.msg.err { color: #f54a45; }
</style>
