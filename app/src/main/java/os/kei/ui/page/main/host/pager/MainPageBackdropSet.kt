package os.kei.ui.page.main.host.pager

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import com.kyant.backdrop.backdrops.LayerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Immutable
data class MainPageBackdropSet(
    val topBar: LayerBackdrop,
    val content: LayerBackdrop,
    val sheet: LayerBackdrop,
)

@Composable
fun rememberMainPageBackdropSet(
    keyPrefix: String,
    refreshOnCompositionEnter: Boolean = false,
    distinctLayers: Boolean = true,
): MainPageBackdropSet {
    val surfaceColor = MiuixTheme.colorScheme.surface
    val instanceKeySuffix = if (refreshOnCompositionEnter) {
        var activationCount by rememberSaveable(keyPrefix) { mutableIntStateOf(0) }
        DisposableEffect(Unit) {
            activationCount++
            onDispose { }
        }
        "-$activationCount"
    } else {
        ""
    }

    @Composable
    fun rememberPageBackdrop(slot: String): LayerBackdrop = key("$keyPrefix-$slot$instanceKeySuffix") {
        rememberLayerBackdrop {
            drawRect(surfaceColor)
            drawContent()
        }
    }

    if (!distinctLayers) {
        val sharedBackdrop = rememberPageBackdrop("shared")
        return MainPageBackdropSet(
            topBar = sharedBackdrop,
            content = sharedBackdrop,
            sheet = sharedBackdrop
        )
    }

    val topBarBackdrop = rememberPageBackdrop("topbar")
    val contentBackdrop = rememberPageBackdrop("content")
    val sheetBackdrop = rememberPageBackdrop("sheet")
    return MainPageBackdropSet(
        topBar = topBarBackdrop,
        content = contentBackdrop,
        sheet = sheetBackdrop
    )
}
