package os.kei.ui.page.main.student

import com.tencent.mmkv.MMKV
import org.json.JSONObject
import kotlin.math.abs

private const val BA_GUIDE_BGM_PLAYBACK_KV_ID = "ba_guide_bgm_favorite_playback"
private const val KEY_SELECTED_AUDIO_URL = "selected_audio_url"
private const val KEY_QUEUE_MODE_NAME = "queue_mode_name"
private const val KEY_PROGRESS_RAW = "progress_raw"
private const val PLAYBACK_PROGRESS_WRITE_STEP_MS = 1500L

internal data class GuideBgmFavoritePlaybackProgress(
    val audioUrl: String,
    val positionMs: Long,
    val durationMs: Long,
    val updatedAtMs: Long,
    val lastPlayedAtMs: Long
) {
    val resumePositionMs: Long
        get() {
            if (durationMs <= 0L) return positionMs.coerceAtLeast(0L)
            val safePosition = positionMs.coerceIn(0L, durationMs)
            return if (durationMs - safePosition <= 3000L) 0L else safePosition
        }
}

internal data class GuideBgmFavoritePlaybackSnapshot(
    val selectedAudioUrl: String,
    val queueModeName: String,
    val progressByAudioUrl: Map<String, GuideBgmFavoritePlaybackProgress>
) {
    fun progressFor(audioUrl: String): GuideBgmFavoritePlaybackProgress? {
        val normalized = normalizeGuideMediaSource(audioUrl)
        if (normalized.isBlank()) return null
        return progressByAudioUrl[normalized]
    }
}

internal object GuideBgmFavoritePlaybackStore {
    private val store: MMKV by lazy { MMKV.mmkvWithID(BA_GUIDE_BGM_PLAYBACK_KV_ID) }
    private val lock = Any()

    @Volatile
    private var loaded = false
    private var selectedAudioUrl: String = ""
    private var queueModeName: String = ""
    private var progressByAudioUrl: Map<String, GuideBgmFavoritePlaybackProgress> = emptyMap()

    fun snapshot(): GuideBgmFavoritePlaybackSnapshot {
        ensureLoaded()
        return GuideBgmFavoritePlaybackSnapshot(
            selectedAudioUrl = selectedAudioUrl,
            queueModeName = queueModeName,
            progressByAudioUrl = progressByAudioUrl
        )
    }

    fun progressFor(audioUrl: String): GuideBgmFavoritePlaybackProgress? {
        return snapshot().progressFor(audioUrl)
    }

    fun saveSelection(audioUrl: String, queueModeName: String) {
        val normalizedAudioUrl = normalizeGuideMediaSource(audioUrl)
        synchronized(lock) {
            ensureLoadedLocked()
            selectedAudioUrl = normalizedAudioUrl
            this.queueModeName = queueModeName.trim()
            store.encode(KEY_SELECTED_AUDIO_URL, selectedAudioUrl)
            store.encode(KEY_QUEUE_MODE_NAME, this.queueModeName)
        }
    }

    fun saveProgress(
        audioUrl: String,
        positionMs: Long,
        durationMs: Long,
        isPlaying: Boolean,
        nowMs: Long = System.currentTimeMillis()
    ) {
        val normalizedAudioUrl = normalizeGuideMediaSource(audioUrl)
        if (normalizedAudioUrl.isBlank()) return
        val safeDuration = durationMs.coerceAtLeast(0L)
        val safePosition = if (safeDuration > 0L) {
            positionMs.coerceIn(0L, safeDuration)
        } else {
            positionMs.coerceAtLeast(0L)
        }
        synchronized(lock) {
            ensureLoadedLocked()
            val previous = progressByAudioUrl[normalizedAudioUrl]
            val safeNow = nowMs.coerceAtLeast(1L)
            val next = GuideBgmFavoritePlaybackProgress(
                audioUrl = normalizedAudioUrl,
                positionMs = safePosition,
                durationMs = safeDuration,
                updatedAtMs = safeNow,
                lastPlayedAtMs = if (isPlaying) {
                    safeNow
                } else {
                    previous?.lastPlayedAtMs ?: 0L
                }
            )
            if (!shouldPersistProgress(previous, next, isPlaying)) return
            progressByAudioUrl = progressByAudioUrl + (normalizedAudioUrl to next)
            persistProgressLocked()
        }
    }

    private fun ensureLoaded() {
        if (loaded) return
        synchronized(lock) {
            ensureLoadedLocked()
        }
    }

    private fun ensureLoadedLocked() {
        if (loaded) return
        selectedAudioUrl = normalizeGuideMediaSource(
            store.decodeString(KEY_SELECTED_AUDIO_URL, "").orEmpty()
        )
        queueModeName = store.decodeString(KEY_QUEUE_MODE_NAME, "").orEmpty().trim()
        progressByAudioUrl = decodeProgress(store.decodeString(KEY_PROGRESS_RAW, "").orEmpty())
        loaded = true
    }

    private fun shouldPersistProgress(
        previous: GuideBgmFavoritePlaybackProgress?,
        next: GuideBgmFavoritePlaybackProgress,
        isPlaying: Boolean
    ): Boolean {
        previous ?: return true
        if (previous.durationMs != next.durationMs) return true
        if (isPlaying && next.updatedAtMs - previous.updatedAtMs >= PLAYBACK_PROGRESS_WRITE_STEP_MS) return true
        if (abs(previous.positionMs - next.positionMs) >= PLAYBACK_PROGRESS_WRITE_STEP_MS) return true
        if (previous.lastPlayedAtMs != next.lastPlayedAtMs) return true
        return false
    }

    private fun persistProgressLocked() {
        val raw = JSONObject().apply {
            progressByAudioUrl.values
                .sortedByDescending { it.updatedAtMs }
                .take(120)
                .forEach { progress ->
                    put(
                        progress.audioUrl,
                        JSONObject().apply {
                            put("positionMs", progress.positionMs)
                            put("durationMs", progress.durationMs)
                            put("updatedAtMs", progress.updatedAtMs)
                            put("lastPlayedAtMs", progress.lastPlayedAtMs)
                        }
                    )
                }
        }.toString()
        store.encode(KEY_PROGRESS_RAW, raw)
    }

    private fun decodeProgress(raw: String): Map<String, GuideBgmFavoritePlaybackProgress> {
        if (raw.isBlank()) return emptyMap()
        return runCatching {
            val root = JSONObject(raw)
            buildMap {
                root.keys().forEach { key ->
                    val normalizedAudioUrl = normalizeGuideMediaSource(key)
                    if (normalizedAudioUrl.isBlank()) return@forEach
                    val item = root.optJSONObject(key) ?: return@forEach
                    put(
                        normalizedAudioUrl,
                        GuideBgmFavoritePlaybackProgress(
                            audioUrl = normalizedAudioUrl,
                            positionMs = item.optLong("positionMs", 0L).coerceAtLeast(0L),
                            durationMs = item.optLong("durationMs", 0L).coerceAtLeast(0L),
                            updatedAtMs = item.optLong("updatedAtMs", 0L).coerceAtLeast(0L),
                            lastPlayedAtMs = item.optLong("lastPlayedAtMs", 0L).coerceAtLeast(0L)
                        )
                    )
                }
            }
        }.getOrDefault(emptyMap())
    }
}
