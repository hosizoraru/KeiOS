package os.kei.ui.page.main.widget.glass

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.Dp

val LocalReducedGlassEffectsEnabled = staticCompositionLocalOf { false }

internal fun Dp.reduceGlassBlurIfNeeded(reduced: Boolean): Dp =
    if (reduced) this * 0.72f else this

internal fun Dp.reduceGlassLensIfNeeded(reduced: Boolean): Dp =
    if (reduced) this * 0.70f else this
