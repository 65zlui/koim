package org.example.websocket

import org.example.security.JwtService
import org.springframework.stereotype.Component
import org.springframework.web.socket.CloseStatus
import org.springframework.web.socket.TextMessage
import org.springframework.web.socket.WebSocketSession
import org.springframework.web.socket.handler.TextWebSocketHandler

@Component
class ChatWebSocketHandler(
    private val onlineUserManager: OnlineUserManager,
    private val jwtService: JwtService,
) : TextWebSocketHandler() {

    override fun afterConnectionEstablished(session: WebSocketSession) {
        val uid = resolveUid(session)
        if (uid == null) {
            session.close(CloseStatus.NOT_ACCEPTABLE)
            return
        }
        session.attributes["uid"] = uid
        onlineUserManager.register(uid, session)
    }

    override fun afterConnectionClosed(session: WebSocketSession, status: CloseStatus) {
        (session.attributes["uid"] as? Long)?.let { onlineUserManager.unregister(it, session) }
    }

    override fun handleTextMessage(session: WebSocketSession, message: TextMessage) {
        // Only ack messages are expected from client; ignore content.
    }

    private fun resolveUid(session: WebSocketSession): Long? {
        val token = session.uri?.query
            ?.split('&')
            ?.firstOrNull { it.startsWith("token=") }
            ?.substringAfter('=')
            ?: return null
        return jwtService.parseUid(token)
    }
}
