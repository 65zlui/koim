<template>
  <div v-if="visible" class="group-settings">
    <header class="gs-header">
      <strong>Group Settings</strong>
      <button class="close-btn" @click="$emit('close')">×</button>
    </header>

    <!-- Group name -->
    <section class="gs-section">
      <label>Group Name</label>
      <div v-if="isOwner" class="gs-row">
        <input v-model="editName" placeholder="Group name" />
        <button :disabled="!editName.trim() || saving" @click="onUpdateName">Save</button>
      </div>
      <div v-else class="gs-name">{{ info?.name }}</div>
    </section>

    <!-- Owner -->
    <section class="gs-section">
      <label>Owner</label>
      <div class="gs-name">UID {{ info?.ownerUid }}</div>
    </section>

    <!-- Members -->
    <section class="gs-section">
      <label>Members ({{ members.length }})</label>
      <ul class="gs-members">
        <li v-for="m in members" :key="m.uid" class="gs-member">
          <span>{{ m.nickname || `UID ${m.uid}` }}</span>
          <span v-if="m.uid === info?.ownerUid" class="owner-badge">owner</span>
          <button
            v-if="isOwner && m.uid !== selfUid"
            class="kick-btn"
            :disabled="busy"
            @click="onKick(m.uid)"
          >Kick</button>
        </li>
      </ul>
    </section>

    <!-- Leave -->
    <section class="gs-section">
      <button class="leave-btn" :disabled="busy" @click="onLeave">Leave Group</button>
    </section>

    <div v-if="msg" class="gs-msg" :class="{ err }">{{ msg }}</div>
  </div>
</template>

<script setup>
import { ref, watch, onMounted } from 'vue'
import { groupInfo as fetchInfo, groupMembers, updateGroup, leaveGroup, kickMember } from '@/api/group'
import { useConversationStore } from '@/stores/conversation'

const props = defineProps({
  groupId: { type: Number, required: true },
  selfUid: { type: Number, required: true },
  visible: { type: Boolean, default: false },
})
const emit = defineEmits(['close', 'left', 'kicked', 'updated'])

const conv = useConversationStore()

const info = ref(null)
const members = ref([])
const editName = ref('')
const saving = ref(false)
const busy = ref(false)
const msg = ref('')
const err = ref(false)

const isOwner = ref(false)

const flash = (text, isErr = false) => {
  msg.value = text
  err.value = isErr
  setTimeout(() => { msg.value = '' }, 3000)
}

const load = async () => {
  try {
    const infoRes = await fetchInfo(props.groupId)
    info.value = infoRes.data
    editName.value = infoRes.data.name
    isOwner.value = infoRes.data.ownerUid === props.selfUid

    const memRes = await groupMembers(props.groupId)
    members.value = memRes.data || []
  } catch (e) {
    flash(e.message, true)
  }
}

const onUpdateName = async () => {
  saving.value = true
  try {
    const { data } = await updateGroup(props.groupId, editName.value.trim())
    info.value = data
    flash('Group name updated')
    emit('updated')
  } catch (e) {
    flash(e.message || e.response?.data?.error || 'Failed', true)
  } finally {
    saving.value = false
  }
}

const onKick = async (targetUid) => {
  if (!confirm(`Kick UID ${targetUid}?`)) return
  busy.value = true
  try {
    await kickMember(props.groupId, targetUid)
    flash(`Kicked UID ${targetUid}`)
    emit('kicked', targetUid)
    await load()
  } catch (e) {
    flash(e.message || e.response?.data?.error || 'Failed', true)
  } finally {
    busy.value = false
  }
}

const onLeave = async () => {
  if (!confirm('Leave this group?')) return
  busy.value = true
  try {
    await leaveGroup(props.groupId)
    conv.removeLocal('g', props.groupId)
    flash('Left group')
    emit('left')
  } catch (e) {
    flash(e.message || e.response?.data?.error || 'Failed', true)
  } finally {
    busy.value = false
  }
}

watch(() => props.visible, (v) => { if (v) load() })
watch(() => props.groupId, () => { if (props.visible) load() })
</script>

<style scoped>
.group-settings {
  width: 260px;
  border-left: 1px solid #e6e8eb;
  background: #fafbfc;
  display: flex;
  flex-direction: column;
  overflow-y: auto;
}
.gs-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 14px 16px;
  border-bottom: 1px solid #e6e8eb;
}
.close-btn {
  background: none;
  border: none;
  font-size: 20px;
  cursor: pointer;
  color: #646a73;
  padding: 0 4px;
}
.gs-section {
  padding: 12px 16px;
  border-bottom: 1px solid #f0f1f3;
}
.gs-section label {
  display: block;
  font-size: 11px;
  font-weight: 600;
  text-transform: uppercase;
  color: #8b919a;
  margin-bottom: 6px;
}
.gs-row {
  display: flex;
  gap: 8px;
}
.gs-row input { flex: 1; min-width: 0; }
.gs-row button { white-space: nowrap; padding: 4px 10px; font-size: 12px; }
.gs-name { font-size: 14px; color: #1f2329; }
.gs-members { list-style: none; margin: 0; padding: 0; }
.gs-member {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 6px 0;
  font-size: 13px;
  color: #1f2329;
}
.owner-badge {
  font-size: 10px;
  background: #eaf2ff;
  color: #3370ff;
  padding: 1px 6px;
  border-radius: 8px;
  font-weight: 600;
}
.kick-btn {
  margin-left: auto;
  font-size: 11px;
  color: #f54a45;
  background: none;
  border: 1px solid #f54a45;
  border-radius: 4px;
  padding: 2px 8px;
  cursor: pointer;
}
.kick-btn:hover { background: #fff5f5; }
.leave-btn {
  width: 100%;
  color: #f54a45;
  background: none;
  border: 1px solid #f54a45;
  border-radius: 6px;
  padding: 8px;
  cursor: pointer;
  font-size: 13px;
  font-weight: 600;
}
.leave-btn:hover { background: #fff5f5; }
.gs-msg { font-size: 12px; color: #22c55e; padding: 8px 16px; }
.gs-msg.err { color: #f54a45; }
</style>
