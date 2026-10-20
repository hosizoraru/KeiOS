@file:Suppress("FunctionName")

package os.kei.ui.page.main.widget.sheet

import androidx.compose.foundation.background
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.kyant.backdrop.backdrops.rememberCombinedBackdrop
import os.kei.ui.page.main.widget.glass.drawSheetGlassBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.vibrancy
import com.kyant.backdrop.highlight.Highlight
import com.kyant.backdrop.shadow.InnerShadow
import com.kyant.backdrop.shadow.Shadow
import com.kyant.shapes.RoundedRectangle
import os.kei.ui.page.main.widget.glass.LocalLiquidControlsEnabled
import os.kei.ui.page.main.widget.glass.LocalLiquidOverlayHost
import os.kei.ui.page.main.widget.glass.presentationGlassBlur
import os.kei.ui.page.main.widget.glass.presentationGlassLens
import os.kei.ui.page.main.widget.glass.safeLiquidLens

private const val LIQUID_SHEET_LENS_SCALE = 0.34f
private const val LIQUID_SHEET_REFRACTION_AMOUNT_SCALE = 1.30f

/**
 * The sheet's material: the modifier that paints it, plus the backdrops it hands on.
 *
 * [exportedBackdrop] is the sheet's own surface published for the controls inside it, so a glass
 * button on a glass sheet samples the sheet rather than the page behind it. The library is explicit
 * that the alternative — a second `layerBackdrop` after `drawBackdrop` — is a draw loop that
 * SIGSEGVs the RenderThread, and `exportedBackdrop` exists precisely to break it.
 */
internal class LiquidSheetSurface(
    val modifier: Modifier,
    val exportedBackdrop: Backdrop?,
    val glassEnabled: Boolean,
)

/**
 * Builds the sheet material.
 *
 * Falls back to a plain filled surface whenever real glass is impossible or unwanted: liquid
 * controls switched off, a caller-supplied [explicitBackgroundColor], or no overlay host in scope.
 * That last case matters — outside [SceneBackdropHost] the sheet is composed in place and
 * `LocalSceneBackdrop` is whatever the surrounding window provides, which for a preview or a
 * Robolectric harness is `emptyBackdrop()`. Asking for blur there yields a *transparent* sheet, not a
 * blurred one, so the fallback is the readable option rather than a cosmetic downgrade.
 */
@Composable
internal fun rememberLiquidSheetSurface(
    cornerRadius: Dp,
    solidness: Float,
    surfaceTone: LiquidSheetSurfaceTone,
    isDark: Boolean,
    explicitBackgroundColor: Color?,
    fallbackColor: Color,
    offsetProvider: () -> Float,
): LiquidSheetSurface {
    val liquidControlsEnabled = LocalLiquidControlsEnabled.current
    val inOverlay = LocalLiquidOverlayHost.current != null
    val sceneBackdrop = LocalSceneBackdrop.current
    val sheetBackdrop = rememberLayerBackdrop()
    val glassEnabled = liquidControlsEnabled && inOverlay && explicitBackgroundColor == null

    val shape = remember(cornerRadius) { RoundedRectangle(cornerRadius) }

    if (!glassEnabled) {
        return LiquidSheetSurface(
            modifier = Modifier
                .graphicsLayer { translationY = offsetProvider() }
                .background(
                    color = explicitBackgroundColor ?: fallbackColor,
                    shape = shape,
                ),
            exportedBackdrop = null,
            glassEnabled = false,
        )
    }

    val blurRadius = presentationGlassBlur()
    val lens =
        presentationGlassLens(
            lensScale = LIQUID_SHEET_LENS_SCALE,
            refractionScale = LIQUID_SHEET_REFRACTION_AMOUNT_SCALE,
        )
    val glassFill = liquidSheetGlassSurfaceColor(
        isDark = isDark,
        solidness = solidness,
        surfaceTone = surfaceTone,
    )
    val topEdgeColor =
        if (isDark) {
            Color.White.copy(alpha = lerp(0.14f, 0.18f, solidness))
        } else {
            Color.White.copy(alpha = lerp(0.56f, 0.64f, solidness))
        }
    val frontBackdrop = rememberLayerBackdrop()
    val materialBackdrop = rememberCombinedBackdrop(sheetBackdrop, frontBackdrop)

    return LiquidSheetSurface(
        modifier = Modifier.drawSheetGlassBackdrop(
            backdrop = sceneBackdrop,
            shape = { shape },
            effects = {
                vibrancy()
                blur(blurRadius.toPx())
                safeLiquidLens(
                    lens.refractionHeight.toPx(),
                    lens.refractionAmount.toPx(),
                    chromaticAberration = false,
                    depthEffect = true,
                )
            },
            // This *is* the sheet's slide. `drawBackdrop` applies the block as the element's own
            // graphicsLayer and inverse-transforms the sample by it, so the sheet moves while the
            // background it refracts stays anchored to the screen. Applying the translation with a
            // separate graphicsLayer instead would move the sheet and drag the refraction along with
            // it, and the glass would read as a moving decal rather than a lens.
            layerBlock = { translationY = offsetProvider() },
            exportedBackdrop = sheetBackdrop,
            exportedFrontBackdrop = frontBackdrop,
            highlight = { Highlight.Default.copy(alpha = if (isDark) 0.74f else 0.88f) },
            shadow = {
                Shadow.Default.copy(
                    color = Color.Black.copy(alpha = if (isDark) 0.22f else 0.14f),
                )
            },
            innerShadow = {
                InnerShadow(radius = 8.dp, alpha = if (isDark) 0.18f else 0.11f)
            },
            onDrawSurface = {
                drawRect(glassFill)
            },
            onDrawFront = {
                // A specular line just inside the top edge — the brightest part of a real glass lip.
                val inset = cornerRadius.toPx()
                drawLine(
                    color = topEdgeColor,
                    start = Offset(x = inset, y = 1.dp.toPx()),
                    end = Offset(x = size.width - inset, y = 1.dp.toPx()),
                    strokeWidth = 1.dp.toPx(),
                )
            },
        ),
        exportedBackdrop = materialBackdrop,
        glassEnabled = true,
    )
}
