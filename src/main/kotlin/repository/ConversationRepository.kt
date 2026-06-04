package org.example.repository

import org.example.entity.Conversation
import org.example.entity.ConversationId
import org.springframework.data.jpa.repository.JpaRepository

interface ConversationRepository : JpaRepository<Conversation, ConversationId> {
    fun findByIdUidOrderByLastMsgTimeDesc(uid: Long): List<Conversation>
}
