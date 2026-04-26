package os.kei.ui.page.main.student

import com.tencent.mmkv.MMKV
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject

private const val BA_GUIDE_BGM_FAVORITES_KV_ID = "ba_guide_bgm_favorites"
private const val KEY_BGM_FAVORITES_RAW = "bgm_favorites_raw"
internal const val GUIDE_BGM_FAVORITE_AUDIO_SCOPE_KEY = "guide_bgm_favorites"

internal data class GuideBgmFavoriteItem(
    val audioUrl: String,
    val title: String,
    val studentTitle: String,
    val studentImageUrl: String,
    val imageUrl: String,
    val sourceUrl: String,
    val note: String,
    val favoritedAtMs: Long
)

internal data class GuideBgmFavoriteImportResult(
    val importedCount: Int,
    val addedCount: Int,
    val updatedCount: Int
)

internal object GuideBgmFavoriteStore {
    private val store: MMKV by lazy { MMKV.mmkvWithID(BA_GUIDE_BGM_FAVORITES_KV_ID) }
    private val lock = Any()
    private val favoritesState = MutableStateFlow<List<GuideBgmFavoriteItem>>(emptyList())

    @Volatile
    private var loaded = false

    fun favoritesFlow(): StateFlow<List<GuideBgmFavoriteItem>> {
        ensureLoaded()
        return favoritesState.asStateFlow()
    }

    fun favoritesSnapshot(): List<GuideBgmFavoriteItem> {
        ensureLoaded()
        return favoritesState.value
    }

    fun isFavorite(audioUrl: String): Boolean {
        val normalizedAudioUrl = normalizeGuideMediaSource(audioUrl)
        if (normalizedAudioUrl.isBlank()) return false
        return favoritesSnapshot().any { it.audioUrl == normalizedAudioUrl }
    }

    fun toggleFavorite(item: GuideBgmFavoriteItem): Boolean {
        val normalized = normalizeFavorite(item) ?: return false
        return synchronized(lock) {
            ensureLoadedLocked()
            val next = favoritesState.value
                .filterNot { it.audioUrl == normalized.audioUrl }
                .toMutableList()
            val added = next.size == favoritesState.value.size
            if (added) {
                next += normalized
            }
            val frozen = sortedUniqueFavorites(next)
            saveFavoritesLocked(frozen)
            favoritesState.value = frozen
            added
        }
    }

    fun restoreFavorite(item: GuideBgmFavoriteItem) {
        val normalized = normalizeFavorite(item) ?: return
        synchronized(lock) {
            ensureLoadedLocked()
            val next = favoritesState.value
                .filterNot { it.audioUrl == normalized.audioUrl }
                .toMutableList()
            next += normalized
            val frozen = sortedUniqueFavorites(next)
            saveFavoritesLocked(frozen)
            favoritesState.value = frozen
        }
    }

    fun removeFavorite(audioUrl: String) {
        val normalizedAudioUrl = normalizeGuideMediaSource(audioUrl)
        if (normalizedAudioUrl.isBlank()) return
        synchronized(lock) {
            ensureLoadedLocked()
            val next = favoritesState.value.filterNot { it.audioUrl == normalizedAudioUrl }
            saveFavoritesLocked(next)
            favoritesState.value = next
        }
    }

    fun buildFavoritesExportJson(nowMs: Long = System.currentTimeMillis()): String {
        val favorites = favoritesSnapshot()
        return JSONObject().apply {
            put("type", "keios.ba.bgm_favorites")
            put("version", 1)
            put("exportedAtMs", nowMs.coerceAtLeast(1L))
            put(
                "favorites",
                JSONArray().apply {
                    favorites.forEach { item -> put(item.toJsonObject()) }
                }
            )
        }.toString()
    }

    fun importFavoritesJsonMerged(raw: String): GuideBgmFavoriteImportResult {
        val imported = parseFavoritesImport(raw)
        if (imported.isEmpty()) {
            return GuideBgmFavoriteImportResult(
                importedCount = 0,
                addedCount = 0,
                updatedCount = 0
            )
        }
        return synchronized(lock) {
            ensureLoadedLocked()
            val existingByAudioUrl = favoritesState.value.associateBy { it.audioUrl }
            val merged = existingByAudioUrl.toMutableMap()
            var added = 0
            var updated = 0
            imported.forEach { item ->
                val previous = merged[item.audioUrl]
                if (previous == null) {
                    added += 1
                } else if (previous != item) {
                    updated += 1
                }
                merged[item.audioUrl] = item
            }
            val frozen = sortedUniqueFavorites(merged.values.toList())
            saveFavoritesLocked(frozen)
            favoritesState.value = frozen
            GuideBgmFavoriteImportResult(
                importedCount = imported.size,
                addedCount = added,
                updatedCount = updated
            )
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
        favoritesState.value = loadFavoritesLocked()
        loaded = true
    }

    private fun loadFavoritesLocked(): List<GuideBgmFavoriteItem> {
        val raw = store.decodeString(KEY_BGM_FAVORITES_RAW, "").orEmpty()
        if (raw.isBlank()) return emptyList()
        return runCatching {
            val array = JSONArray(raw)
            buildList {
                for (index in 0 until array.length()) {
                    val item = array.optJSONObject(index) ?: continue
                    val favorite = GuideBgmFavoriteItem(
                        audioUrl = item.optString("audioUrl").trim(),
                        title = item.optString("title").trim(),
                        studentTitle = item.optString("studentTitle").trim(),
                        studentImageUrl = item.optString("studentImageUrl").trim(),
                        imageUrl = item.optString("imageUrl").trim(),
                        sourceUrl = item.optString("sourceUrl").trim(),
                        note = item.optString("note").trim(),
                        favoritedAtMs = item.optLong("favoritedAtMs", 0L).coerceAtLeast(0L)
                    )
                    normalizeFavorite(favorite)?.let(::add)
                }
            }.let(::sortedUniqueFavorites)
        }.getOrDefault(emptyList())
    }

    private fun saveFavoritesLocked(favorites: List<GuideBgmFavoriteItem>) {
        if (favorites.isEmpty()) {
            store.removeValueForKey(KEY_BGM_FAVORITES_RAW)
            return
        }
        val raw = JSONArray().apply {
            sortedUniqueFavorites(favorites).forEach { item ->
                put(item.toJsonObject())
            }
        }.toString()
        store.encode(KEY_BGM_FAVORITES_RAW, raw)
    }

    private fun normalizeFavorite(
        item: GuideBgmFavoriteItem,
        nowMs: Long = System.currentTimeMillis()
    ): GuideBgmFavoriteItem? {
        val audioUrl = normalizeGuideMediaSource(item.audioUrl)
        if (audioUrl.isBlank()) return null
        return item.copy(
            audioUrl = audioUrl,
            title = item.title.trim().ifBlank { "回忆大厅BGM" },
            studentTitle = item.studentTitle.trim(),
            studentImageUrl = normalizeGuideMediaSource(item.studentImageUrl),
            imageUrl = normalizeGuideMediaSource(item.imageUrl),
            sourceUrl = normalizeGuideMediaSource(item.sourceUrl),
            note = item.note.trim(),
            favoritedAtMs = item.favoritedAtMs.takeIf { it > 0L } ?: nowMs.coerceAtLeast(1L)
        )
    }

    private fun sortedUniqueFavorites(
        favorites: List<GuideBgmFavoriteItem>
    ): List<GuideBgmFavoriteItem> {
        val byAudioUrl = linkedMapOf<String, GuideBgmFavoriteItem>()
        favorites.forEach { item ->
            if (item.audioUrl.isNotBlank()) {
                byAudioUrl[item.audioUrl] = item
            }
        }
        return byAudioUrl.values
            .sortedByDescending { it.favoritedAtMs }
    }

    private fun GuideBgmFavoriteItem.toJsonObject(): JSONObject {
        return JSONObject().apply {
            put("audioUrl", audioUrl)
            put("title", title)
            put("studentTitle", studentTitle)
            put("studentImageUrl", studentImageUrl)
            put("imageUrl", imageUrl)
            put("sourceUrl", sourceUrl)
            put("note", note)
            put("favoritedAtMs", favoritedAtMs.coerceAtLeast(1L))
        }
    }

    private fun parseFavoritesImport(raw: String): List<GuideBgmFavoriteItem> {
        val trimmed = raw.trim()
        if (trimmed.isBlank()) return emptyList()
        return runCatching {
            val array = if (trimmed.startsWith("[")) {
                JSONArray(trimmed)
            } else {
                val root = JSONObject(trimmed)
                root.optJSONArray("favorites")
                    ?: root.optJSONArray("bgmFavorites")
                    ?: JSONArray()
            }
            buildList {
                for (index in 0 until array.length()) {
                    val item = array.optJSONObject(index) ?: continue
                    val favorite = GuideBgmFavoriteItem(
                        audioUrl = item.optString("audioUrl").trim(),
                        title = item.optString("title").trim(),
                        studentTitle = item.optString("studentTitle").trim(),
                        studentImageUrl = item.optString("studentImageUrl").trim(),
                        imageUrl = item.optString("imageUrl").trim(),
                        sourceUrl = item.optString("sourceUrl").trim(),
                        note = item.optString("note").trim(),
                        favoritedAtMs = item.optLong("favoritedAtMs", 0L).coerceAtLeast(0L)
                    )
                    normalizeFavorite(favorite)?.let(::add)
                }
            }.let(::sortedUniqueFavorites)
        }.getOrDefault(emptyList())
    }
}

internal fun isGuideBgmFavoriteCandidateTitle(rawTitle: String, displayTitle: String): Boolean {
    val normalizedRawTitle = normalizeGalleryTitle(rawTitle)
    val normalizedDisplayTitle = normalizeGalleryTitle(displayTitle)
    return normalizedRawTitle.startsWith("BGM", ignoreCase = true) ||
        normalizedDisplayTitle.startsWith("回忆大厅BGM") ||
        normalizedDisplayTitle.startsWith("回忆大厅")
}
