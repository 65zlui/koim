package org.example.websocket

import org.springframework.stereotype.Component
import org.springframework.web.socket.TextMessage
import org.springframework.web.socket.WebSocketSession
import java.util.concurrent.ConcurrentHashMap

/**
 * In-memory online user registry. Single-instance only — for multi-instance use Redis pub/sub.
 */
@Component
class OnlineUserManager {
    private val sessions = ConcurrentHashMap<Long, WebSocketSession>()

    fun register(uid: Long, session: WebSocketSession) {
        sessions[uid]?.let { runCatching { it.close() } }
        sessions[uid] = session
    }

    fun unregister(uid: Long, session: WebSocketSession) {
        sessions.remove(uid, session)
    }

    fun isOnline(uid: Long): Boolean = sessions[uid]?.isOpen == true

    fun send(uid: Long, payload: String): Boolean {
        val s = sessions[uid] ?: return false
        if (!s.isOpen) return false
        return runCatching { s.sendMessage(TextMessage(payload)); true }.getOrDefault(false)
    }
}
