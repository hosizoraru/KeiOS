package com.example.keios.ui.page.main.widget

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import top.yukonga.miuix.kmp.theme.ColorSchemeMode
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.theme.ThemeController

enum class AppStatusPillSize {
    Compact,
    Default,
    Prominent
}

@Immutable
data class AppStatusPillMetrics(
    val contentPadding: PaddingValues,
    val typography: AppTypographyToken
)

internal object AppStatusPrimitives {
    val pillShape = RoundedCornerShape(999.dp)
    val compactPillMetrics = AppStatusPillMetrics(
        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
        typography = AppTypographyTokens.Caption
    )
    val defaultPillMetrics = AppStatusPillMetrics(
        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 5.dp),
        typography = AppTypographyTokens.Caption
    )
    val prominentPillMetrics = AppStatusPillMetrics(
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
        typography = AppTypographyTokens.Body
    )
}

@Composable
internal fun rememberAppStatusPillMetrics(size: AppStatusPillSize): AppStatusPillMetrics {
    return when (size) {
        AppStatusPillSize.Compact -> AppStatusPrimitives.compactPillMetrics
        AppStatusPillSize.Default -> AppStatusPrimitives.defaultPillMetrics
        AppStatusPillSize.Prominent -> AppStatusPrimitives.prominentPillMetrics
    }
}

@Composable
fun AppSupportingBlock(
    text: String,
    modifier: Modifier = Modifier,
    accentColor: Color = MiuixTheme.colorScheme.onBackgroundVariant,
    maxLines: Int = Int.MAX_VALUE,
    overflow: TextOverflow = TextOverflow.Clip
) {
    val isDark = isSystemInDarkTheme()
    val shape = RoundedCornerShape(12.dp)
    val backgroundColor = if (isDark) {
        MiuixTheme.colorScheme.surfaceContainer.copy(alpha = 0.56f)
    } else {
        MiuixTheme.colorScheme.surfaceContainer.copy(alpha = 0.76f)
    }
    val borderColor = accentColor.copy(alpha = if (isDark) 0.18f else 0.14f)
    top.yukonga.miuix.kmp.basic.Text(
        text = text,
        color = MiuixTheme.colorScheme.onBackgroundVariant.copy(alpha = 0.92f),
        fontSize = AppTypographyTokens.Supporting.fontSize,
        lineHeight = AppTypographyTokens.Supporting.lineHeight,
        modifier = modifier
            .clip(shape)
            .background(backgroundColor)
            .border(width = 1.dp, color = borderColor, shape = shape)
            .padding(horizontal = 12.dp, vertical = 9.dp),
        maxLines = maxLines,
        overflow = overflow
    )
}

@Preview(name = "Status Primitive Light", showBackground = true, backgroundColor = 0xFFF3F4F6)
@Composable
private fun AppStatusPrimitivePreviewLight() {
    MiuixTheme(controller = ThemeController(ColorSchemeMode.Light)) {
        AppSupportingBlock(
            text = "统一说明块会复用在设置 sheet、策略说明和诊断提示里。",
            modifier = Modifier.padding(16.dp)
        )
    }
}

@Preview(name = "Status Primitive Dark", showBackground = true, backgroundColor = 0xFF111827)
@Composable
private fun AppStatusPrimitivePreviewDark() {
    MiuixTheme(controller = ThemeController(ColorSchemeMode.Dark)) {
        AppSupportingBlock(
            text = "状态胶囊和说明块现在共享同一套节奏。",
            modifier = Modifier.padding(16.dp),
            accentColor = Color(0xFF60A5FA)
        )
    }
}
