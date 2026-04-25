package os.kei.ui.page.main.student.catalog.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import os.kei.ui.page.main.student.catalog.BaGuideCatalogEntry
import top.yukonga.miuix.kmp.basic.CircularProgressIndicator
import top.yukonga.miuix.kmp.basic.ProgressIndicatorDefaults
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

internal fun LazyListScope.renderBaGuideCatalogEntryListAdapter(
    displayedEntries: List<BaGuideCatalogEntry>,
    hasMoreEntries: Boolean,
    favoriteCatalogEntries: Map<Long, Long>,
    loadAvatarImages: Boolean,
    accent: Color,
    loadingMoreText: String,
    onOpenGuide: (String) -> Unit,
    onToggleFavorite: (Long) -> Unit
) {
    items(
        items = displayedEntries,
        key = { "${it.tab.name}-${it.entryId}-${it.contentId}" }
    ) { entry ->
        BaGuideCatalogEntryCard(
            entry = entry,
            isFavorite = favoriteCatalogEntries.containsKey(entry.contentId),
            loadAvatarImage = loadAvatarImages,
            onOpenGuide = onOpenGuide,
            onToggleFavorite = onToggleFavorite
        )
    }

    if (hasMoreEntries) {
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 10.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                CircularProgressIndicator(
                    progress = 0.3f,
                    size = 16.dp,
                    strokeWidth = 2.dp,
                    colors = ProgressIndicatorDefaults.progressIndicatorColors(
                        foregroundColor = accent,
                        backgroundColor = accent.copy(alpha = 0.30f),
                    ),
                )
                Text(
                    text = loadingMoreText,
                    color = MiuixTheme.colorScheme.onBackgroundVariant,
                    fontSize = 12.sp
                )
            }
        }
    }
}
