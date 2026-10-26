package os.kei.feature.github.data.local

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

object GitHubTrackStoreSignals {
    private val _version = MutableStateFlow(0L)
    val version: StateFlow<Long> = _version.asStateFlow()
    private val _persistedSummaryVersion = MutableStateFlow(0L)
    val persistedSummaryVersion: StateFlow<Long> = _persistedSummaryVersion.asStateFlow()
    private val trackRefreshLock = Any()
    private val _pendingTrackRefreshIds = MutableStateFlow<Set<String>>(emptySet())

    fun notifyChanged(atMillis: Long = System.currentTimeMillis()) {
        _version.update { previous ->
            atMillis.coerceAtLeast(previous + 1L)
        }
        notifySummaryChanged()
    }

    fun notifySummaryChanged() {
        _persistedSummaryVersion.update { it + 1L }
    }

    fun requestTrackRefresh(
        trackId: String,
        atMillis: Long = System.currentTimeMillis(),
        notifyChangeSignal: Boolean = true
    ) {
        val normalized = trackId.trim()
        if (normalized.isBlank()) return
        synchronized(trackRefreshLock) {
            _pendingTrackRefreshIds.value = _pendingTrackRefreshIds.value + normalized
        }
        if (notifyChangeSignal) {
            notifyChanged(atMillis)
        }
    }

    fun consumeTrackRefreshRequests(existingTrackIds: Set<String>): Set<String> {
        if (existingTrackIds.isEmpty()) return emptySet()
        return synchronized(trackRefreshLock) {
            val pending = _pendingTrackRefreshIds.value
            if (pending.isEmpty()) return@synchronized emptySet()
            val consume = pending.intersect(existingTrackIds)
            if (consume.isNotEmpty()) {
                _pendingTrackRefreshIds.value = pending - consume
            }
            consume
        }
    }
}
