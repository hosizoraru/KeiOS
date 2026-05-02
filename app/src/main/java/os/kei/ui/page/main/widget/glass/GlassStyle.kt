package os.kei.ui.page.main.widget.glass

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.isSpecified
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

enum class GlassVariant {
    Content,
    Compact,
    SheetInput,
    SheetAction,
    SheetPrimaryAction,
    SheetDangerAction,
    Floating,
    Bar
}

internal data class GlassStyle(
    val blur: Dp,
    val baseColor: Color,
    val overlayColor: Color,
    val borderColor: Color,
    val borderWidth: Dp,
    val highlightAlpha: Float,
    val shadowAlpha: Float,
    val fallbackAlpha: Float,
    val lensStart: Dp,
    val lensEnd: Dp,
    val showBorder: Boolean
)

@Composable
@ReadOnlyComposable
internal fun glassStyle(
    isDark: Boolean,
    variant: GlassVariant,
    blurRadius: Dp?
): GlassStyle {
    val glassRuntime = glassEffectRuntime()
    fun blur(dp: Dp): Dp = (dp * glassRuntime.blurScaleFor(variant)).clampGlassBlur()
    fun lens(dp: Dp): Dp = dp * glassRuntime.lensScaleFor(variant)

    return when (variant) {
        GlassVariant.Bar -> GlassStyle(
            blur = blur(UiPerformanceBudget.backdropBlur),
            baseColor = Color.Transparent,
            overlayColor = Color.Transparent,
            borderColor = if (isDark) Color.White.copy(alpha = 0.10f) else Color(0xFFD9E6F8).copy(alpha = 0.78f),
            borderWidth = 1.dp,
            highlightAlpha = if (isDark) 1f else 0.82f,
            shadowAlpha = if (isDark) 0.20f else 0.10f,
            fallbackAlpha = if (isDark) 0.40f else 0.56f,
            lensStart = lens(24.dp),
            lensEnd = lens(24.dp),
            showBorder = true
        )
        GlassVariant.SheetInput -> {
            val blur = blur(blurRadius ?: if (isDark) 6.dp else 11.dp)
            if (isDark) {
                GlassStyle(
                    blur = blur,
                    baseColor = Color(0xFF131A23).copy(alpha = 0.26f),
                    overlayColor = Color(0xFF84B9F8).copy(alpha = 0.06f),
                    borderColor = Color(0xFF99C7FF).copy(alpha = 0.18f),
                    borderWidth = 1.dp,
                    highlightAlpha = 0.64f,
                    shadowAlpha = 0.10f,
                    fallbackAlpha = 0.76f,
                    lensStart = lens(25.dp),
                    lensEnd = lens(25.dp),
                    showBorder = true
                )
            } else {
                GlassStyle(
                    blur = blur,
                    baseColor = Color.White.copy(alpha = 0.74f),
                    overlayColor = Color(0xFFE4F0FF).copy(alpha = 0.26f),
                    borderColor = Color(0xFFC9DCF7).copy(alpha = 0.84f),
                    borderWidth = 1.2.dp,
                    highlightAlpha = 0.78f,
                    shadowAlpha = 0.14f,
                    fallbackAlpha = 1f,
                    lensStart = lens(27.dp),
                    lensEnd = lens(27.dp),
                    showBorder = true
                )
            }
        }
        GlassVariant.SheetAction -> {
            val blur = blur(blurRadius ?: if (isDark) 6.dp else 11.dp)
            if (isDark) {
                GlassStyle(
                    blur = blur,
                    baseColor = Color(0xFF15181E).copy(alpha = 0.24f),
                    overlayColor = Color.White.copy(alpha = 0.05f),
                    borderColor = Color.White.copy(alpha = 0.16f),
                    borderWidth = 1.dp,
                    highlightAlpha = 0.66f,
                    shadowAlpha = 0.10f,
                    fallbackAlpha = 0.76f,
                    lensStart = lens(25.dp),
                    lensEnd = lens(25.dp),
                    showBorder = true
                )
            } else {
                GlassStyle(
                    blur = blur,
                    baseColor = Color.White.copy(alpha = 0.74f),
                    overlayColor = Color(0xFFDBECFF).copy(alpha = 0.24f),
                    borderColor = Color(0xFFD1E3FB).copy(alpha = 0.82f),
                    borderWidth = 1.2.dp,
                    highlightAlpha = 0.78f,
                    shadowAlpha = 0.16f,
                    fallbackAlpha = 1f,
                    lensStart = lens(27.dp),
                    lensEnd = lens(27.dp),
                    showBorder = true
                )
            }
        }
        GlassVariant.SheetDangerAction -> {
            val blur = blur(blurRadius ?: if (isDark) 6.dp else 11.dp)
            if (isDark) {
                GlassStyle(
                    blur = blur,
                    baseColor = Color(0xFF1A1214).copy(alpha = 0.30f),
                    overlayColor = Color(0xFFFF8A9B).copy(alpha = 0.08f),
                    borderColor = Color(0xFFFFA1AF).copy(alpha = 0.24f),
                    borderWidth = 1.dp,
                    highlightAlpha = 0.68f,
                    shadowAlpha = 0.12f,
                    fallbackAlpha = 0.78f,
                    lensStart = lens(27.dp),
                    lensEnd = lens(27.dp),
                    showBorder = true
                )
            } else {
                GlassStyle(
                    blur = blur,
                    baseColor = Color.White.copy(alpha = 0.75f),
                    overlayColor = Color(0xFFFFE1E6).copy(alpha = 0.34f),
                    borderColor = Color(0xFFFFD3DB).copy(alpha = 0.84f),
                    borderWidth = 1.2.dp,
                    highlightAlpha = 0.80f,
                    shadowAlpha = 0.15f,
                    fallbackAlpha = 1f,
                    lensStart = lens(27.dp),
                    lensEnd = lens(27.dp),
                    showBorder = true
                )
            }
        }
        GlassVariant.SheetPrimaryAction -> {
            val blur = blur(blurRadius ?: if (isDark) 6.dp else 11.dp)
            if (isDark) {
                GlassStyle(
                    blur = blur,
                    baseColor = Color(0xFF131A23).copy(alpha = 0.30f),
                    overlayColor = Color(0xFF84B9F8).copy(alpha = 0.10f),
                    borderColor = Color(0xFF99C7FF).copy(alpha = 0.24f),
                    borderWidth = 1.dp,
                    highlightAlpha = 0.68f,
                    shadowAlpha = 0.12f,
                    fallbackAlpha = 0.78f,
                    lensStart = lens(27.dp),
                    lensEnd = lens(27.dp),
                    showBorder = true
                )
            } else {
                GlassStyle(
                    blur = blur,
                    baseColor = Color.White.copy(alpha = 0.75f),
                    overlayColor = Color(0xFFE3F1FF).copy(alpha = 0.34f),
                    borderColor = Color(0xFFC7DDFB).copy(alpha = 0.84f),
                    borderWidth = 1.2.dp,
                    highlightAlpha = 0.80f,
                    shadowAlpha = 0.15f,
                    fallbackAlpha = 1f,
                    lensStart = lens(27.dp),
                    lensEnd = lens(27.dp),
                    showBorder = true
                )
            }
        }
        GlassVariant.Floating -> {
            val blur = blur(blurRadius ?: if (isDark) 8.dp else 11.dp)
            if (isDark) {
                GlassStyle(
                    blur = blur,
                    baseColor = Color(0xFF10151D).copy(alpha = 0.24f),
                    overlayColor = Color.White.copy(alpha = 0.03f),
                    borderColor = Color.Transparent,
                    borderWidth = 0.dp,
                    highlightAlpha = 0.58f,
                    shadowAlpha = 0.12f,
                    fallbackAlpha = 0.68f,
                    lensStart = lens(25.dp),
                    lensEnd = lens(27.dp),
                    showBorder = false
                )
            } else {
                GlassStyle(
                    blur = blur,
                    baseColor = Color.White.copy(alpha = 0.58f),
                    overlayColor = Color(0xFFF5F9FF).copy(alpha = 0.12f),
                    borderColor = Color.Transparent,
                    borderWidth = 0.dp,
                    highlightAlpha = 0.70f,
                    shadowAlpha = 0.12f,
                    fallbackAlpha = 0.86f,
                    lensStart = lens(26.dp),
                    lensEnd = lens(28.dp),
                    showBorder = false
                )
            }
        }
        GlassVariant.Compact -> {
            val blur = blur(blurRadius ?: if (isDark) 5.dp else 6.dp)
            if (isDark) {
                GlassStyle(
                    blur = blur,
                    baseColor = Color(0xFF13161B).copy(alpha = 0.18f),
                    overlayColor = Color.White.copy(alpha = 0.04f),
                    borderColor = Color.White.copy(alpha = 0.14f),
                    borderWidth = 1.dp,
                    highlightAlpha = 0.54f,
                    shadowAlpha = 0.09f,
                    fallbackAlpha = 0.72f,
                    lensStart = lens(22.dp),
                    lensEnd = lens(22.dp),
                    showBorder = true
                )
            } else {
                GlassStyle(
                    blur = blur,
                    baseColor = Color.White.copy(alpha = 0.60f),
                    overlayColor = Color(0xFFDCEBFF).copy(alpha = 0.14f),
                    borderColor = Color(0xFFD8E5F8).copy(alpha = 0.80f),
                    borderWidth = 1.dp,
                    highlightAlpha = 0.78f,
                    shadowAlpha = 0.11f,
                    fallbackAlpha = 0.92f,
                    lensStart = lens(22.dp),
                    lensEnd = lens(22.dp),
                    showBorder = true
                )
            }
        }
        GlassVariant.Content -> {
            val blur = blur(blurRadius ?: if (isDark) 7.dp else 11.dp)
            if (isDark) {
                GlassStyle(
                    blur = blur,
                    baseColor = Color(0xFF0F1115).copy(alpha = 0.62f),
                    overlayColor = Color.White.copy(alpha = 0.07f),
                    borderColor = Color.White.copy(alpha = 0.26f),
                    borderWidth = 1.dp,
                    highlightAlpha = 0.96f,
                    shadowAlpha = 0.18f,
                    fallbackAlpha = 0.92f,
                    lensStart = lens(26.dp),
                    lensEnd = lens(28.dp),
                    showBorder = true
                )
            } else {
                GlassStyle(
                    blur = blur,
                    baseColor = Color.White.copy(alpha = 0.62f),
                    overlayColor = Color(0xFFDCEBFF).copy(alpha = 0.24f),
                    borderColor = Color(0xFFD8E5F8).copy(alpha = 0.84f),
                    borderWidth = 1.dp,
                    highlightAlpha = 0.80f,
                    shadowAlpha = 0.14f,
                    fallbackAlpha = 0.98f,
                    lensStart = lens(26.dp),
                    lensEnd = lens(28.dp),
                    showBorder = true
                )
            }
        }
    }
}

internal fun GlassStyle.tintWithAccent(
    accentColor: Color,
    isDark: Boolean,
): GlassStyle {
    val normalizedAccent = accentColor.copy(alpha = 1f)
    val baseTint = normalizedAccent.copy(alpha = if (isDark) 0.06f else 0.05f)
    val overlayTint = normalizedAccent.copy(alpha = if (isDark) 0.10f else 0.13f)
    val borderTint = normalizedAccent.copy(alpha = if (isDark) 0.20f else 0.28f)

    return copy(
        baseColor = baseTint.compositeOver(baseColor),
        overlayColor = if (overlayColor == Color.Transparent) {
            overlayTint
        } else {
            overlayTint.compositeOver(overlayColor)
        },
        borderColor = if (borderColor == Color.Transparent) {
            borderTint
        } else {
            borderTint.compositeOver(borderColor)
        }
    )
}

internal fun resolveGlassAccentColor(
    color: Color,
    isDark: Boolean
): Color {
    val fallback = if (isDark) Color(0xFF71ADFF) else Color(0xFF3B82F6)
    if (!color.isSpecified || color.alpha <= 0f) return fallback
    val opaque = color.copy(alpha = 1f)
    val maxChannel = maxOf(opaque.red, opaque.green, opaque.blue)
    val minChannel = minOf(opaque.red, opaque.green, opaque.blue)
    val chroma = maxChannel - minChannel
    val neutral = chroma <= 0.10f || opaque.luminance() <= 0.10f || opaque.luminance() >= 0.92f
    return if (neutral) fallback else opaque
}
