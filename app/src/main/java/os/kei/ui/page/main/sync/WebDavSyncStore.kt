package os.kei.ui.page.main.sync

import kotlinx.serialization.encodeToString
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import os.kei.core.json.KeiJson
import os.kei.core.json.decodeFromStringOrNull
import os.kei.feature.webdav.model.WebDavConfig
import os.kei.feature.webdav.model.workspaceIdentity
import kotlin.math.abs
import org.json.JSONObject

/**
 * Persists WebDAV connection config and per-item sync metadata in MMKV.
 *
 * The persisted [WebDavConfig] is the single source of truth — there is no separate
 * UI-layer config model. Per-item metadata (enabled flag, last-sync time, remote ETag and
 * the hash of the last-synced local content) lives alongside it so the sync engine can make
 * conditional writes and the auto-sync coordinator can detect real local changes.
 */
internal object WebDavSyncStore : WebDavSyncPersistence(MmkvWebDavPreferences()) {
    const val DEFAULT_REMOTE_DIR = WebDavSyncPersistence.DEFAULT_REMOTE_DIR
    const val DEFAULT_AUTO_SYNC_INTERVAL_HOURS = WebDavSyncPersistence.DEFAULT_AUTO_SYNC_INTERVAL_HOURS
    val AUTO_SYNC_INTERVAL_HOUR_OPTIONS = WebDavSyncPersistence.AUTO_SYNC_INTERVAL_HOUR_OPTIONS
}

internal open class WebDavSyncPersistence(private val mmkv: WebDavPreferences) {
    companion object {

        private const val KEY_PROVIDER = "provider"
        private const val KEY_SERVER_URL = "server_url"
        private const val KEY_USERNAME = "username"
        private const val KEY_APP_PASSWORD = "app_password"
        private const val KEY_REMOTE_DIR = "remote_dir"
        private const val KEY_AUTO_SYNC = "auto_sync"
        private const val KEY_AUTO_SYNC_INTERVAL_HOURS = "auto_sync_interval_hours"
        private const val KEY_LAST_FULL_SYNC = "last_full_sync"
        private const val KEY_LAST_REMOTE_PROBE = "last_remote_probe"
        private const val KEY_LAST_AUTO_SYNC_ATTEMPT = "last_auto_sync_attempt"
        private const val KEY_LAST_AUTO_SYNC_STATUS = "last_auto_sync_status"
        private const val KEY_LAST_AUTO_SYNC_REASON = "last_auto_sync_reason"
        private const val KEY_LAST_AUTO_SYNC_FINISHED = "last_auto_sync_finished"
        private const val KEY_LAST_AUTO_SYNC_TARGETS = "last_auto_sync_targets"
        private const val KEY_LAST_AUTO_SYNC_SUCCEEDED = "last_auto_sync_succeeded"
        private const val KEY_LAST_AUTO_SYNC_FAILED = "last_auto_sync_failed"
        private const val KEY_LAST_AUTO_SYNC_SKIPPED = "last_auto_sync_skipped"
        private const val KEY_SYNC_HISTORY = "sync_history_v1"
        private const val KEY_LAST_JOB_STOP_REASON = "last_job_stop_reason"
        private const val DEFAULT_FINGERPRINT_REVISION = 1

        const val DEFAULT_REMOTE_DIR = "KeiOS/"
        const val DEFAULT_AUTO_SYNC_INTERVAL_HOURS = 3
        val AUTO_SYNC_INTERVAL_HOUR_OPTIONS = listOf(1, 3, 6, 12, 24)
    }

    // ── Connection config ──────────────────────────────────────────────

    fun saveConfig(config: WebDavConfig, provider: WebDavProvider) {
        val previous = loadConfig()
        if (previous?.let { runCatching { it.workspaceIdentity() }.getOrNull() } != config.workspaceIdentity()) {
            clearRemoteMetadata()
        }
        mmkv.encode(KEY_PROVIDER, provider.name)
        mmkv.encode(KEY_SERVER_URL, config.serverUrl)
        mmkv.encode(KEY_USERNAME, config.username)
        mmkv.encode(KEY_APP_PASSWORD, config.appPassword)
        mmkv.encode(KEY_REMOTE_DIR, config.remoteDir)
        WebDavSyncStoreSignals.notifyChanged()
    }

    fun loadConfig(): WebDavConfig? {
        val url = mmkv.decodeString(KEY_SERVER_URL, null).orEmpty()
        val user = mmkv.decodeString(KEY_USERNAME, null).orEmpty()
        val pass = mmkv.decodeString(KEY_APP_PASSWORD, null).orEmpty()
        if (url.isBlank() || user.isBlank() || pass.isBlank()) return null
        return WebDavConfig(
            serverUrl = url,
            username = user,
            appPassword = pass,
            remoteDir = mmkv.decodeString(KEY_REMOTE_DIR, DEFAULT_REMOTE_DIR).orEmpty()
                .ifBlank { DEFAULT_REMOTE_DIR },
        )
    }

    fun loadProvider(): WebDavProvider {
        val raw = mmkv.decodeString(KEY_PROVIDER, null)
        return WebDavProvider.entries.firstOrNull { it.name == raw } ?: WebDavProvider.Jianguoyun
    }

    fun hasConfig(): Boolean = loadConfig() != null

    fun clearConfig() {
        val itemKeys = WebDavSyncItem.entries.flatMap { item ->
            listOf(
                enabledKey(item),
                lastSyncKey(item),
                etagKey(item),
                hashKey(item),
                checkpointKey(item),
                fingerprintRevisionKey(item),
                remoteCountKey(item),
                remoteByteSizeKey(item),
                remoteEtagKey(item),
                remoteProbedAtKey(item),
                remoteEmptyKey(item),
                pendingStateKey(item),
                pendingUpdatedAtKey(item),
            )
        }
        mmkv.removeValuesForKeys(
            (listOf(
                KEY_PROVIDER,
                KEY_SERVER_URL,
                KEY_USERNAME,
                KEY_APP_PASSWORD,
                KEY_REMOTE_DIR,
                KEY_AUTO_SYNC,
                KEY_AUTO_SYNC_INTERVAL_HOURS,
                KEY_LAST_FULL_SYNC,
                KEY_LAST_REMOTE_PROBE,
                KEY_LAST_AUTO_SYNC_ATTEMPT,
                KEY_LAST_AUTO_SYNC_STATUS,
                KEY_LAST_AUTO_SYNC_REASON,
                KEY_LAST_AUTO_SYNC_FINISHED,
                KEY_LAST_AUTO_SYNC_TARGETS,
                KEY_LAST_AUTO_SYNC_SUCCEEDED,
                KEY_LAST_AUTO_SYNC_FAILED,
                KEY_LAST_AUTO_SYNC_SKIPPED,
            ) + itemKeys).toTypedArray(),
        )
        WebDavSyncStoreSignals.notifyChanged()
    }

    // ── Auto-sync preference ───────────────────────────────────────────

    fun isAutoSyncEnabled(): Boolean = mmkv.decodeBool(KEY_AUTO_SYNC, false)

    fun setAutoSyncEnabled(enabled: Boolean) {
        mmkv.encode(KEY_AUTO_SYNC, enabled)
        WebDavSyncStoreSignals.notifyChanged()
    }

    fun getAutoSyncIntervalHours(): Int =
        normalizeAutoSyncIntervalHours(
            mmkv.decodeInt(KEY_AUTO_SYNC_INTERVAL_HOURS, DEFAULT_AUTO_SYNC_INTERVAL_HOURS),
        )

    fun setAutoSyncIntervalHours(hours: Int) {
        mmkv.encode(KEY_AUTO_SYNC_INTERVAL_HOURS, normalizeAutoSyncIntervalHours(hours))
        WebDavSyncStoreSignals.notifyChanged()
    }

    fun getAutoSyncIntervalMs(): Long =
        getAutoSyncIntervalHours() * 60L * 60L * 1000L

    fun normalizeAutoSyncIntervalHours(hours: Int): Int =
        AUTO_SYNC_INTERVAL_HOUR_OPTIONS.minByOrNull { option -> abs(option - hours) }
            ?: DEFAULT_AUTO_SYNC_INTERVAL_HOURS

    // ── Per-item enabled flag ──────────────────────────────────────────

    fun isItemEnabled(item: WebDavSyncItem): Boolean =
        mmkv.decodeBool(enabledKey(item), true)

    fun setItemEnabled(item: WebDavSyncItem, enabled: Boolean) {
        mmkv.encode(enabledKey(item), enabled)
        WebDavSyncStoreSignals.notifyChanged()
    }

    // ── Per-item last sync time ────────────────────────────────────────

    fun getLastSyncTime(item: WebDavSyncItem): Long =
        checkpoint(item)?.optLong("timeMs") ?: mmkv.decodeLong(lastSyncKey(item), 0L)

    fun setLastSyncTime(item: WebDavSyncItem, timeMs: Long) {
        mmkv.encode(lastSyncKey(item), timeMs)
    }

    // ── Per-item remote ETag (conditional writes) ──────────────────────

    fun getItemEtag(item: WebDavSyncItem): String? {
        checkpoint(item)?.let { return it.optString("etag").takeIf { value -> value.isNotBlank() && value != "null" } }
        return mmkv.decodeString(etagKey(item), null)?.takeIf { it.isNotBlank() }
    }

    fun setItemEtag(item: WebDavSyncItem, etag: String?) {
        if (etag.isNullOrBlank()) {
            mmkv.removeValueForKey(etagKey(item))
        } else {
            mmkv.encode(etagKey(item), etag)
        }
    }

    // ── Per-item local content hash (auto-sync change detection) ───────

    fun getItemContentHash(item: WebDavSyncItem): String? =
        checkpoint(item)?.optString("hash") ?: mmkv.decodeString(hashKey(item), null)?.takeIf { it.isNotBlank() }

    fun recordCheckpoint(item: WebDavSyncItem, etag: String?, hash: String, timeMs: Long) {
        mmkv.encode(checkpointKey(item), JSONObject().apply {
            put("etag", etag ?: JSONObject.NULL)
            put("hash", hash)
            put("timeMs", timeMs)
        }.toString())
        clearItemPendingState(item)
    }

    private fun checkpointKey(item: WebDavSyncItem) = "checkpoint_v2_${item.name}"

    private fun checkpoint(item: WebDavSyncItem): JSONObject? =
        mmkv.decodeString(checkpointKey(item), null)?.let { raw ->
            runCatching { JSONObject(raw) }.getOrNull()
        }

    private fun clearRemoteMetadata() {
        val keys = WebDavSyncItem.entries.flatMap { item ->
            listOf(
                checkpointKey(item), etagKey(item), hashKey(item), lastSyncKey(item),
                fingerprintRevisionKey(item), remoteCountKey(item), remoteByteSizeKey(item),
                remoteEtagKey(item), remoteProbedAtKey(item), remoteEmptyKey(item),
                pendingStateKey(item), pendingUpdatedAtKey(item),
            )
        }
        mmkv.removeValuesForKeys((keys + listOf(KEY_LAST_FULL_SYNC, KEY_LAST_REMOTE_PROBE)).toTypedArray())
    }

    fun setItemContentHash(item: WebDavSyncItem, hash: String) {
        mmkv.encode(hashKey(item), hash)
    }

    fun getItemFingerprintRevision(item: WebDavSyncItem): Int =
        mmkv.decodeInt(fingerprintRevisionKey(item), DEFAULT_FINGERPRINT_REVISION)
            .coerceAtLeast(DEFAULT_FINGERPRINT_REVISION)

    fun setItemFingerprintRevision(item: WebDavSyncItem, revision: Int) {
        mmkv.encode(
            fingerprintRevisionKey(item),
            revision.coerceAtLeast(DEFAULT_FINGERPRINT_REVISION),
        )
    }

    // ── Per-item pending state (auto-sync conflict / first-baseline guard) ─

    fun loadItemPendingSummary(item: WebDavSyncItem): WebDavSyncPendingSummary? {
        val raw = mmkv.decodeString(pendingStateKey(item), null).orEmpty()
        val state = WebDavSyncPendingState.entries.firstOrNull { it.name == raw } ?: return null
        return WebDavSyncPendingSummary(
            state = state,
            updatedAtMs = mmkv.decodeLong(pendingUpdatedAtKey(item), 0L).coerceAtLeast(0L),
        )
    }

    fun setItemPendingState(
        item: WebDavSyncItem,
        state: WebDavSyncPendingState,
        updatedAtMs: Long = System.currentTimeMillis(),
    ) {
        mmkv.encode(pendingStateKey(item), state.name)
        mmkv.encode(pendingUpdatedAtKey(item), updatedAtMs.coerceAtLeast(0L))
        WebDavSyncStoreSignals.notifyChanged()
    }

    fun clearItemPendingState(item: WebDavSyncItem) {
        val hadPending =
            mmkv.containsKey(pendingStateKey(item)) ||
                mmkv.containsKey(pendingUpdatedAtKey(item))
        mmkv.removeValueForKey(pendingStateKey(item))
        mmkv.removeValueForKey(pendingUpdatedAtKey(item))
        if (hadPending) {
            WebDavSyncStoreSignals.notifyChanged()
        }
    }

    // ── Last full sync time ────────────────────────────────────────────

    fun getLastFullSyncTime(): Long = mmkv.decodeLong(KEY_LAST_FULL_SYNC, 0L)

    fun setLastFullSyncTime(timeMs: Long) {
        mmkv.encode(KEY_LAST_FULL_SYNC, timeMs)
        WebDavSyncStoreSignals.notifyChanged()
    }

    // ── Launch auto-sync throttle ──────────────────────────────────────

    fun getLastAutoSyncAttemptTime(): Long = mmkv.decodeLong(KEY_LAST_AUTO_SYNC_ATTEMPT, 0L)

    fun setLastAutoSyncAttemptTime(timeMs: Long) {
        mmkv.encode(KEY_LAST_AUTO_SYNC_ATTEMPT, timeMs.coerceAtLeast(0L))
    }

    fun loadLastAutoSyncSummary(): WebDavAutoSyncSummary? {
        val rawStatus = mmkv.decodeString(KEY_LAST_AUTO_SYNC_STATUS, null).orEmpty()
        val status = WebDavAutoSyncStatus.entries.firstOrNull { it.name == rawStatus } ?: return null
        return WebDavAutoSyncSummary(
            status = status,
            reason = mmkv.decodeString(KEY_LAST_AUTO_SYNC_REASON, null).orEmpty(),
            finishedAtMs = mmkv.decodeLong(KEY_LAST_AUTO_SYNC_FINISHED, 0L).coerceAtLeast(0L),
            targetCount = mmkv.decodeInt(KEY_LAST_AUTO_SYNC_TARGETS, 0).coerceAtLeast(0),
            succeededCount = mmkv.decodeInt(KEY_LAST_AUTO_SYNC_SUCCEEDED, 0).coerceAtLeast(0),
            failedCount = mmkv.decodeInt(KEY_LAST_AUTO_SYNC_FAILED, 0).coerceAtLeast(0),
            skippedCount = mmkv.decodeInt(KEY_LAST_AUTO_SYNC_SKIPPED, 0).coerceAtLeast(0),
        )
    }

    fun saveLastAutoSyncSummary(summary: WebDavAutoSyncSummary) {
        mmkv.encode(KEY_LAST_AUTO_SYNC_STATUS, summary.status.name)
        mmkv.encode(KEY_LAST_AUTO_SYNC_REASON, summary.reason)
        mmkv.encode(KEY_LAST_AUTO_SYNC_FINISHED, summary.finishedAtMs.coerceAtLeast(0L))
        mmkv.encode(KEY_LAST_AUTO_SYNC_TARGETS, summary.targetCount.coerceAtLeast(0))
        mmkv.encode(KEY_LAST_AUTO_SYNC_SUCCEEDED, summary.succeededCount.coerceAtLeast(0))
        mmkv.encode(KEY_LAST_AUTO_SYNC_FAILED, summary.failedCount.coerceAtLeast(0))
        mmkv.encode(KEY_LAST_AUTO_SYNC_SKIPPED, summary.skippedCount.coerceAtLeast(0))
        WebDavSyncStoreSignals.notifyChanged()
    }

    // ── Local sync history ─────────────────────────────────────────────

    fun loadHistory(): List<WebDavSyncHistoryEntry> {
        val raw = mmkv.decodeString(KEY_SYNC_HISTORY, null).orEmpty()
        return KeiJson.lenient
            .decodeFromStringOrNull<WebDavSyncHistoryPayload>(raw)
            ?.entries
            ?.sortedWith(
                compareByDescending<WebDavSyncHistoryEntry> { it.finishedAtMs }
                    .thenByDescending { it.startedAtMs }
                    .thenBy { it.id },
            )
            ?.take(WebDavSyncHistoryMaxEntries)
            .orEmpty()
    }

    fun appendHistory(entry: WebDavSyncHistoryEntry): List<WebDavSyncHistoryEntry> {
        val updated = appendWebDavSyncHistory(loadHistory(), entry)
        mmkv.encode(
            KEY_SYNC_HISTORY,
            KeiJson.lenient.encodeToString(WebDavSyncHistoryPayload(entries = updated)),
        )
        WebDavSyncStoreSignals.notifyChanged()
        return updated
    }

    fun clearHistory() {
        val hadHistory = mmkv.containsKey(KEY_SYNC_HISTORY)
        mmkv.removeValueForKey(KEY_SYNC_HISTORY)
        if (hadHistory) {
            WebDavSyncStoreSignals.notifyChanged()
        }
    }

    fun setLastJobStopReason(reason: String) {
        if (reason.isBlank()) {
            mmkv.removeValueForKey(KEY_LAST_JOB_STOP_REASON)
        } else {
            mmkv.encode(KEY_LAST_JOB_STOP_REASON, reason.take(120))
        }
    }

    fun getLastJobStopReason(): String? =
        mmkv.decodeString(KEY_LAST_JOB_STOP_REASON, null)?.takeIf { it.isNotBlank() }

    fun clearLastJobStopReason() {
        mmkv.removeValueForKey(KEY_LAST_JOB_STOP_REASON)
    }

    // ── Remote summary (read-only probe results) ───────────────────────
    //
    // Captured by [WebDavSyncEngine.probeRemote] without touching local state. Lets the page
    // show "remote: <count> items, <size>, modified <time>" so other devices can decide how to
    // reconcile before pressing Sync / Upload / Download.

    fun getLastRemoteProbeTime(): Long = mmkv.decodeLong(KEY_LAST_REMOTE_PROBE, 0L)

    fun setLastRemoteProbeTime(timeMs: Long) {
        mmkv.encode(KEY_LAST_REMOTE_PROBE, timeMs)
    }

    fun loadRemoteSummary(item: WebDavSyncItem): WebDavRemoteSummary? {
        val probedAtMs = mmkv.decodeLong(remoteProbedAtKey(item), 0L)
        if (probedAtMs <= 0L) return null
        return WebDavRemoteSummary(
            empty = mmkv.decodeBool(remoteEmptyKey(item), false),
            itemCount = mmkv.decodeInt(remoteCountKey(item), -1),
            byteSize = mmkv.decodeLong(remoteByteSizeKey(item), -1L),
            etag = mmkv.decodeString(remoteEtagKey(item), null)?.takeIf { it.isNotBlank() },
            probedAtMs = probedAtMs,
        )
    }

    fun saveRemoteSummaryFound(
        item: WebDavSyncItem,
        itemCount: Int,
        byteSize: Long,
        etag: String?,
        probedAtMs: Long,
    ) {
        mmkv.encode(remoteEmptyKey(item), false)
        mmkv.encode(remoteCountKey(item), itemCount)
        mmkv.encode(remoteByteSizeKey(item), byteSize)
        if (etag.isNullOrBlank()) {
            mmkv.removeValueForKey(remoteEtagKey(item))
        } else {
            mmkv.encode(remoteEtagKey(item), etag)
        }
        mmkv.encode(remoteProbedAtKey(item), probedAtMs)
    }

    fun saveRemoteSummaryEmpty(item: WebDavSyncItem, probedAtMs: Long) {
        mmkv.encode(remoteEmptyKey(item), true)
        mmkv.encode(remoteCountKey(item), 0)
        mmkv.encode(remoteByteSizeKey(item), 0L)
        mmkv.removeValueForKey(remoteEtagKey(item))
        mmkv.encode(remoteProbedAtKey(item), probedAtMs)
    }

    private fun enabledKey(item: WebDavSyncItem) = "enabled_${item.name}"
    private fun lastSyncKey(item: WebDavSyncItem) = "last_sync_${item.name}"
    private fun etagKey(item: WebDavSyncItem) = "etag_${item.name}"
    private fun hashKey(item: WebDavSyncItem) = "hash_${item.name}"
    private fun fingerprintRevisionKey(item: WebDavSyncItem) = "hash_revision_${item.name}"
    private fun remoteCountKey(item: WebDavSyncItem) = "remote_count_${item.name}"
    private fun remoteByteSizeKey(item: WebDavSyncItem) = "remote_size_${item.name}"
    private fun remoteEtagKey(item: WebDavSyncItem) = "remote_etag_${item.name}"
    private fun remoteProbedAtKey(item: WebDavSyncItem) = "remote_probed_at_${item.name}"
    private fun remoteEmptyKey(item: WebDavSyncItem) = "remote_empty_${item.name}"
    private fun pendingStateKey(item: WebDavSyncItem) = "pending_state_${item.name}"
    private fun pendingUpdatedAtKey(item: WebDavSyncItem) = "pending_updated_at_${item.name}"
}

internal object WebDavSyncStoreSignals {
    val version = MutableStateFlow(0L)

    fun notifyChanged() {
        version.update { current -> current + 1L }
    }
}

/**
 * Read-only snapshot of what's on the remote for a single item, captured by
 * [WebDavSyncEngine.probeRemote] and persisted by the store. [empty] means the file isn't on
 * the server yet (so [itemCount] / [byteSize] are 0); otherwise the values reflect the most
 * recent successful probe.
 */
internal data class WebDavRemoteSummary(
    val empty: Boolean,
    val itemCount: Int,
    val byteSize: Long,
    val etag: String?,
    val probedAtMs: Long,
)

internal enum class WebDavSyncPendingState {
    LocalUploadPending,
    RemoteConflict,
    BaselineRequired,
}

internal data class WebDavSyncPendingSummary(
    val state: WebDavSyncPendingState,
    val updatedAtMs: Long,
)

internal enum class WebDavAutoSyncStatus {
    Success,
    NeedsReview,
    Failed,
    Skipped,
    Running,
}

internal data class WebDavAutoSyncSummary(
    val status: WebDavAutoSyncStatus,
    val reason: String,
    val finishedAtMs: Long,
    val targetCount: Int,
    val succeededCount: Int,
    val failedCount: Int,
    val skippedCount: Int,
) {
    val hasIssues: Boolean
        get() = status == WebDavAutoSyncStatus.NeedsReview || status == WebDavAutoSyncStatus.Failed
}
