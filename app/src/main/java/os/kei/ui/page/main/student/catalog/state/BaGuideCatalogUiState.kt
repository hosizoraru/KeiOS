package os.kei.ui.page.main.student.catalog.state

import androidx.annotation.StringRes
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import os.kei.R
import os.kei.ui.page.main.student.catalog.BaGuideCatalogEntry
import os.kei.ui.page.main.widget.motion.resolvedMotionDuration

internal enum class BaGuideCatalogSortMode(@get:StringRes val labelRes: Int) {
    Default(R.string.ba_catalog_sort_default),
    ReleaseDateDesc(R.string.ba_catalog_sort_release_date_desc),
    ReleaseDateAsc(R.string.ba_catalog_sort_release_date_asc),
}

@Composable
internal fun rememberCatalogSyncProgress(
    loading: Boolean,
    animationsEnabled: Boolean
): Float {
    val progress = remember { Animatable(0f) }
    LaunchedEffect(loading, animationsEnabled) {
        if (!animationsEnabled) {
            progress.snapTo(if (loading) 0.9f else 1f)
            return@LaunchedEffect
        }
        if (loading) {
            progress.snapTo(0.12f)
            progress.animateTo(
                targetValue = 0.68f,
                animationSpec = tween(
                    durationMillis = resolvedMotionDuration(520, animationsEnabled),
                    easing = FastOutSlowInEasing
                ),
            )
            progress.animateTo(
                targetValue = 0.90f,
                animationSpec = tween(
                    durationMillis = resolvedMotionDuration(1800, animationsEnabled),
                    easing = LinearEasing
                ),
            )
        } else {
            progress.animateTo(
                targetValue = 1f,
                animationSpec = tween(
                    durationMillis = resolvedMotionDuration(260, animationsEnabled),
                    easing = FastOutSlowInEasing
                ),
            )
        }
    }
    return progress.value
}

internal fun List<BaGuideCatalogEntry>.sortedByMode(
    mode: BaGuideCatalogSortMode,
    favoriteCatalogEntries: Map<Long, Long>
): List<BaGuideCatalogEntry> {
    val sortedBase = when (mode) {
        BaGuideCatalogSortMode.Default -> sortedBy { it.order }
        BaGuideCatalogSortMode.ReleaseDateDesc -> sortedWith(
            compareByDescending<BaGuideCatalogEntry> {
                when {
                    it.releaseDateSec > 0L -> it.releaseDateSec
                    it.createdAtSec > 0L -> it.createdAtSec
                    else -> Long.MIN_VALUE
                }
            }.thenBy { it.order }
        )
        BaGuideCatalogSortMode.ReleaseDateAsc -> sortedWith(
            compareBy<BaGuideCatalogEntry> {
                when {
                    it.releaseDateSec > 0L -> it.releaseDateSec
                    it.createdAtSec > 0L -> it.createdAtSec
                    else -> Long.MAX_VALUE
                }
            }.thenBy { it.order }
        )
    }
    if (favoriteCatalogEntries.isEmpty()) return sortedBase
    val pinnedFavorites = sortedBase
        .filter { entry -> favoriteCatalogEntries.containsKey(entry.contentId) }
        .sortedWith(
            compareBy<BaGuideCatalogEntry> {
                favoriteCatalogEntries[it.contentId] ?: Long.MAX_VALUE
            }.thenBy { it.order }
        )
    val regularEntries = sortedBase.filterNot { entry ->
        favoriteCatalogEntries.containsKey(entry.contentId)
    }
    return pinnedFavorites + regularEntries
}
