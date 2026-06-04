package org.example.service

import org.example.dto.ConversationDto
import org.example.entity.ConversationId
import org.example.repository.ChatGroupRepository
import org.example.repository.ConversationRepository
import org.example.repository.UserRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class ConversationService(
    private val conversationRepository: ConversationRepository,
    private val userRepository: UserRepository,
    private val chatGroupRepository: ChatGroupRepository,
) {

    @Transactional(readOnly = true)
    fun list(uid: Long): List<ConversationDto> {
        val convs = conversationRepository.findByIdUidOrderByLastMsgTimeDesc(uid)
        return convs.map { c ->
            val name = when (c.id.peerType) {
                "u" -> userRepository.findById(c.id.peerId).map { it.nickname ?: it.username }.orElse(null)
                "g" -> chatGroupRepository.findById(c.id.peerId).map { it.name }.orElse(null)
                else -> null
            }
            ConversationDto(
                peerType = c.id.peerType,
                peerId = c.id.peerId,
                peerName = name,
                lastMsgContent = c.lastMsgContent,
                lastMsgTime = c.lastMsgTime,
                unreadCount = c.unreadCount,
            )
        }
    }

    @Transactional
    fun markRead(uid: Long, peerType: String, peerId: Long) {
        val id = ConversationId(uid, peerType, peerId)
        conversationRepository.findById(id).ifPresent { c ->
            c.unreadCount = 0
            conversationRepository.save(c)
        }
    }
}
