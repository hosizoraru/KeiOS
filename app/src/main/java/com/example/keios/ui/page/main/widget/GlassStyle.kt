package com.example.keios.ui.page.main.widget

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

enum class GlassVariant {
    Content,
    Compact,
    SheetInput,
    SheetAction,
    SheetPrimaryAction,
    SheetDangerAction,
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

internal fun glassStyle(
    isDark: Boolean,
    variant: GlassVariant,
    blurRadius: Dp?
): GlassStyle {
    return when (variant) {
        GlassVariant.Bar -> GlassStyle(
            blur = 8.dp,
            baseColor = Color.Transparent,
            overlayColor = Color.Transparent,
            borderColor = if (isDark) Color.White.copy(alpha = 0.10f) else Color.White.copy(alpha = 0.82f),
            borderWidth = 1.dp,
            highlightAlpha = 1f,
            shadowAlpha = if (isDark) 0.20f else 0.10f,
            fallbackAlpha = if (isDark) 0.40f else 0.56f,
            lensStart = 24.dp,
            lensEnd = 24.dp,
            showBorder = true
        )
        GlassVariant.SheetInput -> {
            val blur = blurRadius ?: if (isDark) 6.dp else 10.dp
            if (isDark) {
                GlassStyle(
                    blur = blur,
                    baseColor = Color(0xFF15181E).copy(alpha = 0.22f),
                    overlayColor = Color.White.copy(alpha = 0.05f),
                    borderColor = Color.White.copy(alpha = 0.16f),
                    borderWidth = 1.dp,
                    highlightAlpha = 0.62f,
                    shadowAlpha = 0.10f,
                    fallbackAlpha = 0.74f,
                    lensStart = 24.dp,
                    lensEnd = 24.dp,
                    showBorder = true
                )
            } else {
                GlassStyle(
                    blur = blur,
                    baseColor = Color.White.copy(alpha = 0.74f),
                    overlayColor = Color(0xFFD8E9FF).copy(alpha = 0.24f),
                    borderColor = Color.White.copy(alpha = 0.998f),
                    borderWidth = 1.45.dp,
                    highlightAlpha = 1f,
                    shadowAlpha = 0.22f,
                    fallbackAlpha = 0.99f,
                    lensStart = 26.dp,
                    lensEnd = 26.dp,
                    showBorder = true
                )
            }
        }
        GlassVariant.SheetAction -> {
            val blur = blurRadius ?: if (isDark) 6.dp else 11.dp
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
                    lensStart = 25.dp,
                    lensEnd = 25.dp,
                    showBorder = true
                )
            } else {
                GlassStyle(
                    blur = blur,
                    baseColor = Color.White.copy(alpha = 0.76f),
                    overlayColor = Color(0xFFDBECFF).copy(alpha = 0.32f),
                    borderColor = Color.White.copy(alpha = 1f),
                    borderWidth = 1.55.dp,
                    highlightAlpha = 1f,
                    shadowAlpha = 0.22f,
                    fallbackAlpha = 1f,
                    lensStart = 27.dp,
                    lensEnd = 27.dp,
                    showBorder = true
                )
            }
        }
        GlassVariant.SheetDangerAction -> {
            val blur = blurRadius ?: if (isDark) 6.dp else 11.dp
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
                    lensStart = 27.dp,
                    lensEnd = 27.dp,
                    showBorder = true
                )
            } else {
                GlassStyle(
                    blur = blur,
                    baseColor = Color.White.copy(alpha = 0.78f),
                    overlayColor = Color(0xFFFFE1E6).copy(alpha = 0.48f),
                    borderColor = Color(0xFFFFD0D8).copy(alpha = 0.96f),
                    borderWidth = 1.55.dp,
                    highlightAlpha = 1f,
                    shadowAlpha = 0.20f,
                    fallbackAlpha = 1f,
                    lensStart = 27.dp,
                    lensEnd = 27.dp,
                    showBorder = true
                )
            }
        }
        GlassVariant.SheetPrimaryAction -> {
            val blur = blurRadius ?: if (isDark) 6.dp else 11.dp
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
                    lensStart = 27.dp,
                    lensEnd = 27.dp,
                    showBorder = true
                )
            } else {
                GlassStyle(
                    blur = blur,
                    baseColor = Color.White.copy(alpha = 0.78f),
                    overlayColor = Color(0xFFE3F1FF).copy(alpha = 0.46f),
                    borderColor = Color(0xFFCEE4FF).copy(alpha = 0.96f),
                    borderWidth = 1.55.dp,
                    highlightAlpha = 1f,
                    shadowAlpha = 0.20f,
                    fallbackAlpha = 1f,
                    lensStart = 27.dp,
                    lensEnd = 27.dp,
                    showBorder = true
                )
            }
        }
        GlassVariant.Compact -> {
            val blur = blurRadius ?: if (isDark) 5.dp else 6.dp
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
                    lensStart = 22.dp,
                    lensEnd = 22.dp,
                    showBorder = true
                )
            } else {
                GlassStyle(
                    blur = blur,
                    baseColor = Color.White.copy(alpha = 0.60f),
                    overlayColor = Color(0xFFDCEBFF).copy(alpha = 0.18f),
                    borderColor = Color.White.copy(alpha = 0.95f),
                    borderWidth = 1.dp,
                    highlightAlpha = 1f,
                    shadowAlpha = 0.15f,
                    fallbackAlpha = 0.92f,
                    lensStart = 22.dp,
                    lensEnd = 22.dp,
                    showBorder = true
                )
            }
        }
        GlassVariant.Content -> {
            val blur = blurRadius ?: if (isDark) 7.dp else 11.dp
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
                    lensStart = 26.dp,
                    lensEnd = 28.dp,
                    showBorder = true
                )
            } else {
                GlassStyle(
                    blur = blur,
                    baseColor = Color.White.copy(alpha = 0.66f),
                    overlayColor = Color(0xFFDCEBFF).copy(alpha = 0.30f),
                    borderColor = Color.White.copy(alpha = 0.96f),
                    borderWidth = 1.dp,
                    highlightAlpha = 1f,
                    shadowAlpha = 0.18f,
                    fallbackAlpha = 0.98f,
                    lensStart = 26.dp,
                    lensEnd = 28.dp,
                    showBorder = true
                )
            }
        }
    }
}
