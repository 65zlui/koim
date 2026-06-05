package org.example.websocket

import org.springframework.web.socket.WebSocketSession

/**
 * Online presence + WebSocket message dispatch abstraction.
 *
 * Two implementations are provided:
 *  - [InMemoryOnlineUserManager] (default): single-instance, ConcurrentHashMap-based.
 *  - [RedisOnlineUserManager]: cross-node fanout via Redis pub/sub for multi-instance deployments.
 *
 * Switch via the `koim.online-manager` property (`memory` | `redis`).
 */
interface OnlineUserManager {
    /** Register a freshly opened session for [uid]. Existing session for the same uid is replaced. */
    fun register(uid: Long, session: WebSocketSession)

    /** Remove a session from the registry on close. */
    fun unregister(uid: Long, session: WebSocketSession)

    /** True if the user has at least one open session anywhere in the cluster. */
    fun isOnline(uid: Long): Boolean

    /**
     * Try to deliver [payload] to the user via WebSocket.
     * @return true iff the payload was handed to an open WebSocket on some node.
     */
    fun send(uid: Long, payload: String): Boolean
}
