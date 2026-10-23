package os.kei.ui.page.main.student.catalog.component

import os.kei.ui.page.main.student.GuideBgmFavoriteItem
import os.kei.ui.page.main.student.catalog.BaGuideCatalogBundle
import os.kei.ui.page.main.student.catalog.BaGuideCatalogEntry
import os.kei.ui.page.main.student.fetch.extractGuideContentIdFromUrl
import java.util.Locale

internal fun GuideBgmFavoriteItem.resolveStudentArtworkImageUrl(
    catalog: BaGuideCatalogBundle
): String {
    return sequenceOf(
        studentImageUrl,
        resolveCatalogEntry(catalog)?.iconUrl.orEmpty(),
        imageUrl
    )
        .map { it.trim() }
        .firstOrNull { it.isNotBlank() }
        .orEmpty()
}

internal fun GuideBgmFavoriteItem.withResolvedCatalogStudentMetadata(
    catalog: BaGuideCatalogBundle
): GuideBgmFavoriteItem = withCatalogEntryStudentMetadata(resolveCatalogEntry(catalog))

internal fun GuideBgmFavoriteItem.withCatalogEntryStudentMetadata(
    entry: BaGuideCatalogEntry?
): GuideBgmFavoriteItem {
    if (entry == null) return this
    val contentId = extractGuideContentIdFromUrl(sourceUrl)
    if (contentId != null && contentId != entry.contentId) return this
    val entryName = entry.name.trim()
    val entryIconUrl = entry.iconUrl.trim()
    val entryDetailUrl = entry.detailUrl.trim()
    val resolvedStudentImageUrl = studentImageUrl.trim().ifBlank { entryIconUrl }
    val resolvedImageUrl = imageUrl.trim().ifBlank { resolvedStudentImageUrl.ifBlank { entryIconUrl } }
    val resolvedStudentTitle = studentTitle.trim().ifBlank { entryName }
    val resolvedSourceUrl = sourceUrl.trim().ifBlank { entryDetailUrl }
    if (
        resolvedStudentImageUrl == studentImageUrl &&
        resolvedImageUrl == imageUrl &&
        resolvedStudentTitle == studentTitle &&
        resolvedSourceUrl == sourceUrl
    ) {
        return this
    }
    return copy(
        studentTitle = resolvedStudentTitle,
        studentImageUrl = resolvedStudentImageUrl,
        imageUrl = resolvedImageUrl,
        sourceUrl = resolvedSourceUrl,
    )
}

internal fun GuideBgmFavoriteItem.resolvePlaybackArtworkImageUrl(): String {
    return sequenceOf(studentImageUrl, imageUrl)
        .map { it.trim() }
        .firstOrNull { it.isNotBlank() }
        .orEmpty()
}

private fun GuideBgmFavoriteItem.resolveCatalogEntry(
    catalog: BaGuideCatalogBundle
): BaGuideCatalogEntry? {
    val entries = catalog.entriesByTab.values
        .asSequence()
        .flatten()
        .distinctBy { it.contentId }
        .toList()
    val contentId = extractGuideContentIdFromUrl(sourceUrl)
    // A missing explicit identity must not borrow another student's artwork by name.
    if (contentId != null) return entries.firstOrNull { it.contentId == contentId }
    val normalizedSourceUrl = sourceUrl.trim()
    if (normalizedSourceUrl.isNotBlank()) {
        entries.singleOrNull { it.detailUrl.trim() == normalizedSourceUrl }?.let { return it }
    }

    val target = studentTitle.trim().ifBlank { title }.catalogNameKey()
    if (target.isBlank()) return null
    val keysByEntry = entries.map { entry ->
        entry to sequenceOf(entry.name, entry.alias, entry.aliasDisplay)
            .map { it.catalogNameKey() }
            .filter { it.isNotBlank() }
            .toSet()
    }
    val preciseMatches = keysByEntry.filter { (_, keys) -> target in keys }
    if (preciseMatches.isNotEmpty()) return preciseMatches.singleOrNull()?.first
    return keysByEntry.singleOrNull { (_, keys) ->
        keys.any { key -> key.contains(target) || target.contains(key) }
    }?.first
}

private fun String.catalogNameKey(): String {
    return trim()
        .lowercase(Locale.ROOT)
        .replace("（", "(")
        .replace("）", ")")
        .filterNot { it.isWhitespace() }
        .replace("・", "·")
}
