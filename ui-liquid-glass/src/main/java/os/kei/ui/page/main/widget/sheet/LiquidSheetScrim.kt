@file:Suppress("FunctionName")

package os.kei.ui.page.main.widget.sheet

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.effects.blur
import os.kei.ui.page.main.widget.isAppInDarkTheme
import os.kei.ui.page.main.widget.glass.drawSheetGlassBackdrop
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * Pushes the app behind a sheet back: a dim over the whole window, plus a blurred plate covering the
 * part of the app the sheet does not.
 *
 * This is the recess Apple's sheets get for free, and it is the honest half of the legibility fix.
 * The previous sheet leaned entirely on a flat 87–99% fill, because it lived in a Dialog window where
 * `LocalSceneBackdrop` is blanked to `emptyBackdrop()` and every `blur()` it asked for drew nothing —
 * so the only thing separating sheet text from busy content underneath was that fill's opacity.
 *
 * Two details keep this affordable:
 *
 * - **The blur radius is fixed; only alpha animates.** Animating a radius rebuilds the RenderEffect
 *   every frame, while alpha is a layer property the RenderThread already handles. Same constraint as
 *   everything else in `docs/planning/hwui-frame-budget.md`.
 * - **The visible plate stops where its caller says it does.** Keep the full-resolution blur recording
 *   at a fixed window size, then crop it through layer properties. Moving the crop and alpha does not
 *   resize or re-record the blur on every animation frame. The source and radius remain live. Sheets
 *   spanning both edges crop at their top edge; narrower presentations retain the full plate. See
 *   [liquidSheetScrimBlurHeightPx].
 */
@Composable
internal fun LiquidSheetScrim(
    backdrop: Backdrop?,
    blurRadius: Dp,
    presentationProvider: () -> Float,
    blurHeightPxProvider: () -> Int,
) {
    val dimColor = MiuixTheme.colorScheme.windowDimming
    val isDark = isAppInDarkTheme()
    Box(
        modifier = Modifier
            .fillMaxSize()
            .drawBehind {
                val presentation = presentationProvider().coerceIn(0f, 1f)
                drawRect(dimColor.copy(alpha = dimColor.alpha * presentation))
            },
    ) {
        if (backdrop != null) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxSize()
                    .graphicsLayer {
                        // Keep blur + tint in this one retained target. Alpha/crop are properties
                        // of its composition, so moving the sheet does not replay either draw.
                        compositingStrategy = CompositingStrategy.Offscreen
                        // Reaches 0 exactly as the sheet reaches the bottom edge, so the scrim is
                        // never cut away while still visible. That cut was the dismiss flinch.
                        alpha = presentationProvider().coerceIn(0f, 1f)
                        clip = true
                        shape = ScrimBlurClip(blurHeightPxProvider())
                    }.drawSheetGlassBackdrop(
                        backdrop = backdrop,
                        shape = { RectangleShape },
                        effects = { blur(blurRadius.toPx()) },
                        layerBlock = {},
                        exportedBackdrop = null,
                        highlight = null,
                        shadow = null,
                        innerShadow = null,
                        onDrawSurface = {
                            // A whisper of tint so the blurred plate reads as a receded surface
                            // rather than a smeared copy of the page.
                            drawRect(
                                if (isDark) {
                                    Color.Black.copy(alpha = 0.10f)
                                } else {
                                    Color.White.copy(alpha = 0.06f)
                                },
                            )
                        },
                        onDrawFront = {},
                        // The outer animated crop already clips the whole blur/tint group.
                        clip = false,
                    ),
            )
        }
    }
}

private data class ScrimBlurClip(val heightPx: Int) : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline =
        Outline.Rectangle(Rect(0f, 0f, size.width, heightPx.toFloat().coerceIn(0f, size.height)))
}
