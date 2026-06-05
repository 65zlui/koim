package org.example.websocket

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Component
import org.springframework.web.socket.TextMessage
import org.springframework.web.socket.WebSocketSession
import java.util.concurrent.ConcurrentHashMap

/**
 * Single-instance online registry backed by a ConcurrentHashMap.
 *
 * Active when `koim.online-manager=memory` (default if the property is absent).
 */
@Component
@ConditionalOnProperty(name = ["koim.online-manager"], havingValue = "memory", matchIfMissing = true)
class InMemoryOnlineUserManager : OnlineUserManager {
    private val sessions = ConcurrentHashMap<Long, WebSocketSession>()

    override fun register(uid: Long, session: WebSocketSession) {
        sessions[uid]?.let { runCatching { it.close() } }
        sessions[uid] = session
    }

    override fun unregister(uid: Long, session: WebSocketSession) {
        sessions.remove(uid, session)
    }

    override fun isOnline(uid: Long): Boolean = sessions[uid]?.isOpen == true

    override fun send(uid: Long, payload: String): Boolean {
        val s = sessions[uid] ?: return false
        if (!s.isOpen) return false
        return runCatching { s.sendMessage(TextMessage(payload)); true }.getOrDefault(false)
    }
}
