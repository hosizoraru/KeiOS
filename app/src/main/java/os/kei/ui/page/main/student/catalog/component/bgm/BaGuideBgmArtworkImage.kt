@file:Suppress("FunctionName")

package os.kei.ui.page.main.student.catalog.component.bgm

import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.delay
import os.kei.ui.page.main.student.catalog.component.LocalBaGuideCatalogImageBitmaps
import os.kei.ui.page.main.student.catalog.component.LocalBaGuideCatalogImageRequester

@Composable
internal fun BaGuideBgmArtworkImage(
    imageUrl: String,
    contentScale: ContentScale,
    modifier: Modifier = Modifier,
) {
    val normalizedUrl = imageUrl.trim()
    val bitmap = LocalBaGuideCatalogImageBitmaps.current[normalizedUrl]
    val requestImages by rememberUpdatedState(LocalBaGuideCatalogImageRequester.current)
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    LaunchedEffect(normalizedUrl, bitmap != null, lifecycle) {
        if (normalizedUrl.isBlank() || bitmap != null) return@LaunchedEffect
        lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            // Exhaust the retry budget while visible; a new foreground entry gets a fresh budget.
            for (backoffMs in longArrayOf(0L, 1_000L, 3_000L, 10_000L)) {
                delay(backoffMs)
                requestImages(listOf(normalizedUrl))
            }
        }
    }
    val rendered = bitmap ?: return
    val imageBitmap = remember(rendered) { rendered.asImageBitmap() }
    Image(
        bitmap = imageBitmap,
        contentDescription = null,
        contentScale = contentScale,
        modifier = modifier,
    )
}
