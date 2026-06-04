package org.example.repository

import org.example.entity.Message
import org.springframework.data.jpa.repository.JpaRepository
import java.util.Optional

interface MessageRepository : JpaRepository<Message, Long> {
    fun findByMsgId(msgId: String): Optional<Message>
    fun existsByMsgId(msgId: String): Boolean
}
