package org.example.metrics

import io.micrometer.core.instrument.Counter
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.Timer
import org.springframework.stereotype.Component
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong

/**
 * Central holder for koim domain metrics. Generic JVM/HTTP metrics come from
 * Spring Boot actuator + Micrometer auto-config; this class adds the IM-specific ones.
 *
 * Naming follows Micrometer dot-style (auto-translated to underscores by the
 * Prometheus registry, e.g. `koim.ws.sessions.active` -> `koim_ws_sessions_active`).
 */
@Component
class ImMetrics(private val registry: MeterRegistry) {

    /** Number of WebSocket sessions currently held by THIS process. */
    val wsSessionsActive: AtomicInteger = AtomicInteger(0)

    /** Rows in `offline_messages` table — refreshed by [OfflineQueueMetrics]. */
    val offlineQueueSize: AtomicLong = AtomicLong(0)

    val wsConnectTotal: Counter = Counter.builder("koim.ws.connect.total")
        .description("WebSocket connections accepted")
        .register(registry)

    val wsDisconnectTotal: Counter = Counter.builder("koim.ws.disconnect.total")
        .description("WebSocket connections closed")
        .register(registry)

    val fanoutPublishedTotal: Counter = Counter.builder("koim.redis.fanout.published.total")
        .description("Cross-node messages published to Redis pub/sub")
        .register(registry)

    val fanoutReceivedTotal: Counter = Counter.builder("koim.redis.fanout.received.total")
        .description("Cross-node messages received from Redis pub/sub")
        .register(registry)

    /** End-to-end latency of [org.example.service.MessageService.send]. */
    val messageDelivery: Timer = Timer.builder("koim.message.delivery")
        .description("MessageService.send end-to-end latency")
        .publishPercentiles(0.5, 0.95, 0.99)
        .register(registry)

    init {
        registry.gauge("koim.ws.sessions.active", wsSessionsActive)
        registry.gauge("koim.offline.queue.size", offlineQueueSize)
    }

    /**
     * Tag-based counter for message throughput. Micrometer caches by name+tags,
     * so calling this repeatedly with the same arguments is cheap and returns
     * the same underlying counter.
     */
    fun messageSent(peerType: String, result: String): Counter =
        Counter.builder("koim.message.sent.total")
            .description("Messages processed by MessageService.send")
            .tags("peerType", peerType, "result", result)
            .register(registry)
}
