package org.example.websocket

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.redis.connection.Message
import org.springframework.data.redis.connection.MessageListener
import org.springframework.data.redis.connection.RedisConnectionFactory
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.data.redis.listener.ChannelTopic
import org.springframework.data.redis.listener.RedisMessageListenerContainer
import org.springframework.data.redis.listener.adapter.MessageListenerAdapter
import org.springframework.stereotype.Component
import org.springframework.web.socket.TextMessage
import org.springframework.web.socket.WebSocketSession
import java.util.concurrent.ConcurrentHashMap

/**
 * Multi-instance online registry backed by Redis.
 *
 * Strategy:
 *  - Online presence: a Redis SET `koim:online` of currently-online uids.
 *  - Local sessions: each node keeps its own [ConcurrentHashMap] of WebSocketSession.
 *  - Cross-node delivery: pub/sub channel `koim:fanout`. When [send] cannot find a
 *    local session, the payload is published; every node receives the message and
 *    the one that owns the target uid writes it to its local WebSocket.
 *
 * Active when `koim.online-manager=redis`.
 */
@Component
@ConditionalOnProperty(name = ["koim.online-manager"], havingValue = "redis")
class RedisOnlineUserManager(
    private val redis: StringRedisTemplate,
    private val objectMapper: ObjectMapper,
) : OnlineUserManager, MessageListener {

    private val log = LoggerFactory.getLogger(javaClass)
    private val localSessions = ConcurrentHashMap<Long, WebSocketSession>()

    override fun register(uid: Long, session: WebSocketSession) {
        localSessions[uid]?.let { runCatching { it.close() } }
        localSessions[uid] = session
        redis.opsForSet().add(ONLINE_SET_KEY, uid.toString())
    }

    override fun unregister(uid: Long, session: WebSocketSession) {
        if (localSessions.remove(uid, session)) {
            // Only remove from the global online set if no local replacement exists.
            if (localSessions[uid] == null) {
                redis.opsForSet().remove(ONLINE_SET_KEY, uid.toString())
            }
        }
    }

    override fun isOnline(uid: Long): Boolean =
        redis.opsForSet().isMember(ONLINE_SET_KEY, uid.toString()) == true

    override fun send(uid: Long, payload: String): Boolean {
        // Fast path: target is connected to this node.
        val local = localSessions[uid]
        if (local != null && local.isOpen) {
            return runCatching { local.sendMessage(TextMessage(payload)); true }.getOrDefault(false)
        }
        // Cluster path: only publish if presence is registered somewhere.
        if (!isOnline(uid)) return false
        val envelope = objectMapper.writeValueAsString(Envelope(uid, payload))
        redis.convertAndSend(FANOUT_CHANNEL, envelope)
        return true
    }

    /** Triggered by [RedisMessageListenerContainer] for messages on [FANOUT_CHANNEL]. */
    override fun onMessage(message: Message, pattern: ByteArray?) {
        val raw = String(message.body)
        val env = runCatching { objectMapper.readValue<Envelope>(raw) }.getOrNull() ?: return
        val sess = localSessions[env.uid] ?: return
        if (!sess.isOpen) return
        runCatching { sess.sendMessage(TextMessage(env.payload)) }
            .onFailure { log.warn("Failed to deliver fanout message to uid=${env.uid}: ${it.message}") }
    }

    private data class Envelope(val uid: Long, val payload: String)

    companion object {
        const val ONLINE_SET_KEY = "koim:online"
        const val FANOUT_CHANNEL = "koim:fanout"
    }
}

/**
 * Wires the Redis pub/sub listener container only when the redis-backed manager is active.
 */
@Configuration
@ConditionalOnProperty(name = ["koim.online-manager"], havingValue = "redis")
class RedisOnlineUserManagerConfig {

    @Bean
    fun fanoutListenerContainer(
        connectionFactory: RedisConnectionFactory,
        manager: RedisOnlineUserManager,
    ): RedisMessageListenerContainer {
        val container = RedisMessageListenerContainer()
        container.setConnectionFactory(connectionFactory)
        container.addMessageListener(
            MessageListenerAdapter(manager, "onMessage"),
            ChannelTopic(RedisOnlineUserManager.FANOUT_CHANNEL),
        )
        return container
    }
}
