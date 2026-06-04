package org.example.service

import com.fasterxml.jackson.databind.ObjectMapper
import org.example.dto.MessageDto
import org.example.dto.SendMessageRequest
import org.example.dto.SendMessageResponse
import org.example.entity.Conversation
import org.example.entity.ConversationId
import org.example.entity.Message
import org.example.entity.OfflineMessage
import org.example.repository.ConversationRepository
import org.example.repository.GroupMemberRepository
import org.example.repository.MessageRepository
import org.example.repository.OfflineMessageRepository
import org.example.websocket.OnlineUserManager
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant

@Service
class MessageService(
    private val messageRepository: MessageRepository,
    private val offlineMessageRepository: OfflineMessageRepository,
    private val conversationRepository: ConversationRepository,
    private val groupMemberRepository: GroupMemberRepository,
    private val onlineUserManager: OnlineUserManager,
    private val objectMapper: ObjectMapper,
) {

    companion object {
        const val MAX_SYNC_BATCH = 200
    }

    @Transactional
    fun send(fromUid: Long, req: SendMessageRequest): SendMessageResponse {
        require(req.toUid != null || req.groupId != null) { "toUid or groupId required" }
        require(req.toUid == null || req.groupId == null) { "exactly one of toUid/groupId" }

        // Idempotency: if msgId exists, return existing.
        val existing = messageRepository.findByMsgId(req.msgId).orElse(null)
        if (existing != null) {
            return SendMessageResponse(existing.msgId, existing.seq ?: 0, existing.sendTime)
        }

        val now = Instant.now()
        val msg = Message(
            msgId = req.msgId,
            fromUid = fromUid,
            toUid = req.toUid,
            groupId = req.groupId,
            msgType = req.type,
            content = req.content,
            sendTime = now,
        )
        val saved = messageRepository.saveAndFlush(msg)

        if (req.toUid != null) {
            dispatchSingle(saved)
        } else {
            dispatchGroup(saved)
        }

        return SendMessageResponse(saved.msgId, saved.seq ?: 0, saved.sendTime)
    }

    private fun dispatchSingle(msg: Message) {
        val toUid = msg.toUid!!
        // Update sender conversation (no unread bump for sender).
        upsertConversation(msg.fromUid, "u", toUid, msg.content, msg.sendTime, bumpUnread = false)
        // Update receiver conversation (bump unread).
        upsertConversation(toUid, "u", msg.fromUid, msg.content, msg.sendTime, bumpUnread = true)

        // Real-time push or offline.
        if (onlineUserManager.isOnline(toUid)) {
            onlineUserManager.send(toUid, buildPushPayload(msg))
        } else {
            offlineMessageRepository.save(toOffline(toUid, msg))
        }
    }

    private fun dispatchGroup(msg: Message) {
        val groupId = msg.groupId!!
        val members = groupMemberRepository.findUidsByGroupId(groupId)
        for (memberUid in members) {
            val isSender = memberUid == msg.fromUid
            upsertConversation(
                memberUid, "g", groupId, msg.content, msg.sendTime,
                bumpUnread = !isSender,
            )
            if (isSender) continue
            if (onlineUserManager.isOnline(memberUid)) {
                onlineUserManager.send(memberUid, buildPushPayload(msg))
            } else {
                offlineMessageRepository.save(toOffline(memberUid, msg))
            }
        }
    }

    private fun toOffline(uid: Long, msg: Message) = OfflineMessage(
        uid = uid,
        msgId = msg.msgId,
        fromUid = msg.fromUid,
        groupId = msg.groupId,
        msgType = msg.msgType,
        content = msg.content,
        sendTime = msg.sendTime,
        seq = msg.seq,
    )

    private fun buildPushPayload(msg: Message): String {
        val payload = mapOf(
            "type" to "msg",
            "msgId" to msg.msgId,
            "fromUid" to msg.fromUid,
            "toUid" to msg.toUid,
            "groupId" to msg.groupId,
            "msgType" to msg.msgType,
            "content" to msg.content,
            "seq" to msg.seq,
            "sendTime" to msg.sendTime.toString(),
        )
        return objectMapper.writeValueAsString(payload)
    }

    private fun upsertConversation(
        uid: Long,
        peerType: String,
        peerId: Long,
        lastContent: String?,
        lastTime: Instant,
        bumpUnread: Boolean,
    ) {
        val id = ConversationId(uid, peerType, peerId)
        val existing = conversationRepository.findById(id).orElse(null)
        if (existing == null) {
            conversationRepository.save(
                Conversation(
                    id = id,
                    lastMsgContent = lastContent,
                    lastMsgTime = lastTime,
                    unreadCount = if (bumpUnread) 1 else 0,
                )
            )
        } else {
            existing.lastMsgContent = lastContent
            existing.lastMsgTime = lastTime
            if (bumpUnread) existing.unreadCount += 1
            conversationRepository.save(existing)
        }
    }

    @Transactional(readOnly = true)
    fun sync(uid: Long, lastSeq: Long?): List<MessageDto> {
        return offlineMessageRepository.findByUidAfterSeq(uid, lastSeq)
            .take(MAX_SYNC_BATCH)
            .map {
                MessageDto(
                    msgId = it.msgId,
                    seq = it.seq,
                    fromUid = it.fromUid,
                    toUid = if (it.groupId == null) uid else null,
                    groupId = it.groupId,
                    msgType = it.msgType,
                    content = it.content,
                    sendTime = it.sendTime,
                )
            }
    }

    @Transactional
    fun confirm(uid: Long, msgIds: List<String>): Int {
        if (msgIds.isEmpty()) return 0
        return offlineMessageRepository.deleteByUidAndMsgIds(uid, msgIds)
    }
}
