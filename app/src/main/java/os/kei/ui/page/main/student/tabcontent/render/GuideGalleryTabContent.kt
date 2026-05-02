package os.kei.ui.page.main.student.tabcontent.render

import android.content.Context
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.ui.graphics.Color
import os.kei.ui.page.main.student.BaStudentGuideInfo
import os.kei.ui.page.main.widget.glass.LiquidInfoBlock
import com.kyant.backdrop.backdrops.LayerBackdrop

internal fun LazyListScope.renderGuideGalleryTabContent(
    tabLabel: String,
    info: BaStudentGuideInfo?,
    error: String?,
    backdrop: LayerBackdrop,
    accent: Color,
    context: Context,
    sourceUrl: String,
    galleryCacheRevision: Int,
    onOpenExternal: (String) -> Unit,
    onSaveMedia: (url: String, title: String) -> Unit,
    onSaveMediaPack: (items: List<Pair<String, String>>, packTitle: String) -> Unit
) {
    val guide = info
    if (guide == null) {
        item {
            LiquidInfoBlock(
                backdrop = backdrop,
                title = tabLabel,
                subtitle = info?.subtitle?.ifBlank { "GameKee" } ?: "GameKee",
                accent = accent
            )
        }
        return
    }

    val galleryState = resolveGuideGalleryTabState(guide)
    renderGuideGalleryStateContent(
        state = galleryState,
        error = error,
        backdrop = backdrop,
        context = context,
        sourceUrl = sourceUrl,
        studentTitle = guide.title,
        studentImageUrl = guide.imageUrl,
        galleryCacheRevision = galleryCacheRevision,
        onOpenExternal = onOpenExternal,
        onSaveMedia = onSaveMedia,
        onSaveMediaPack = onSaveMediaPack
    )
}
