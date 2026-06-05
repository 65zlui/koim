package org.example.metrics

import org.example.repository.OfflineMessageRepository
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

/**
 * Periodically polls `offline_messages` table size and publishes it as the
 * gauge `koim.offline.queue.size`.
 *
 * Why polling rather than instrumenting save/delete sites:
 *  - Save/delete happens in many places (single dispatch, group dispatch, sync confirm,
 *    future TTL cleaner, etc.). A periodic SELECT COUNT(*) is a single source of truth.
 *  - The cost of `count()` on a small/medium table is trivial; for very large tables
 *    consider a partial index or a bounded estimate.
 */
@Component
class OfflineQueueMetrics(
    private val metrics: ImMetrics,
    private val offlineMessageRepository: OfflineMessageRepository,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @Scheduled(fixedDelayString = "PT30S", initialDelayString = "PT10S")
    fun refresh() {
        runCatching { offlineMessageRepository.count() }
            .onSuccess { metrics.offlineQueueSize.set(it) }
            .onFailure { log.warn("Failed to refresh offline queue gauge: ${it.message}") }
    }
}
