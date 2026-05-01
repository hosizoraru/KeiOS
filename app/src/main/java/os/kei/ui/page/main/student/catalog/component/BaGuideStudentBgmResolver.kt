package os.kei.ui.page.main.student.catalog.component

import os.kei.ui.page.main.student.BaGuideGalleryItem
import os.kei.ui.page.main.student.BaStudentGuideInfo
import os.kei.ui.page.main.student.BaStudentGuideStore
import os.kei.ui.page.main.student.GuideBgmFavoriteItem
import os.kei.ui.page.main.student.fetchGuideInfo
import os.kei.ui.page.main.student.isGuideBgmFavoriteCandidateTitle
import os.kei.ui.page.main.student.catalog.BaGuideCatalogEntry
import os.kei.ui.page.main.student.normalizeGuideMediaSource

internal data class BaGuideStudentBgmResolvedItem(
    val favorite: GuideBgmFavoriteItem,
    val fromCache: Boolean,
    val fromFavorite: Boolean = false
)

internal fun loadCachedStudentBgmFavorite(
    entry: BaGuideCatalogEntry
): BaGuideStudentBgmResolvedItem? {
    val info = BaStudentGuideStore.loadInfoSnapshot(entry.detailUrl).info ?: return null
    return info.toStudentBgmFavorite(entry = entry, fromCache = true)
}

internal fun fetchStudentBgmFavorite(
    entry: BaGuideCatalogEntry
): BaGuideStudentBgmResolvedItem? {
    val cached = loadCachedStudentBgmFavorite(entry)
    if (cached != null) return cached
    val info = fetchGuideInfo(entry.detailUrl)
    BaStudentGuideStore.saveInfo(info)
    return info.toStudentBgmFavorite(entry = entry, fromCache = false)
}

private fun BaStudentGuideInfo.toStudentBgmFavorite(
    entry: BaGuideCatalogEntry,
    fromCache: Boolean
): BaGuideStudentBgmResolvedItem? {
    val item = firstStudentBgmGalleryItem() ?: return null
    val audioUrl = normalizeGuideMediaSource(item.mediaUrl)
    if (audioUrl.isBlank()) return null
    val studentImage = entry.iconUrl.ifBlank { imageUrl }
    return BaGuideStudentBgmResolvedItem(
        favorite = GuideBgmFavoriteItem(
            audioUrl = audioUrl,
            title = item.title,
            studentTitle = entry.name.ifBlank { title },
            studentImageUrl = studentImage,
            imageUrl = item.imageUrl.ifBlank { studentImage },
            sourceUrl = entry.detailUrl.ifBlank { sourceUrl },
            note = item.note.ifBlank { item.memoryUnlockLevel },
            favoritedAtMs = 0L
        ),
        fromCache = fromCache
    )
}

private fun BaStudentGuideInfo.firstStudentBgmGalleryItem(): BaGuideGalleryItem? {
    val audios = galleryItems.filter { item ->
        item.mediaType.equals("audio", ignoreCase = true) &&
            normalizeGuideMediaSource(item.mediaUrl).isNotBlank()
    }
    if (audios.isEmpty()) return null
    return audios.firstOrNull { item ->
        val title = item.title.trim()
        title.contains("回忆大厅", ignoreCase = true) ||
            title.contains("BGM", ignoreCase = true) ||
            isGuideBgmFavoriteCandidateTitle(title, title)
    } ?: audios.firstOrNull()
}
