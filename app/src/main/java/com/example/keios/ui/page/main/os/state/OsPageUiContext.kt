package com.example.keios.ui.page.main.os.state

import android.content.Context
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.example.keios.core.ui.effect.rememberMiuixBlurBackdrop
import com.example.keios.ui.page.main.host.pager.MainPageBackdropSet
import com.example.keios.ui.page.main.host.pager.rememberMainPageBackdropSet
import kotlinx.coroutines.CoroutineScope
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.blur.LayerBackdrop

internal data class OsPageUiContext(
    val context: Context,
    val density: Density,
    val scope: CoroutineScope,
    val textBundle: OsPageTextBundle,
    val backdrops: MainPageBackdropSet,
    val topBarMaterialBackdrop: LayerBackdrop?,
    val isDark: Boolean,
    val inactiveColor: Color,
    val titleColor: Color,
    val cachedColor: Color,
    val refreshingColor: Color,
    val syncedColor: Color,
    val surfaceColor: Color,
    val searchBarHideThresholdPx: Float
)

@Composable
internal fun rememberOsPageUiContext(): OsPageUiContext {
    val context = LocalContext.current
    val density = LocalDensity.current
    val scope = rememberCoroutineScope()
    val textBundle = rememberOsPageTextBundle()
    val isDark = isSystemInDarkTheme()
    val backdrops = rememberMainPageBackdropSet(
        keyPrefix = "os",
        refreshOnCompositionEnter = true
    )
    val topBarMaterialBackdrop = rememberMiuixBlurBackdrop(enableBlur = true)
    val searchBarHideThresholdPx = with(density) { 28.dp.toPx() }
    return OsPageUiContext(
        context = context,
        density = density,
        scope = scope,
        textBundle = textBundle,
        backdrops = backdrops,
        topBarMaterialBackdrop = topBarMaterialBackdrop,
        isDark = isDark,
        inactiveColor = MiuixTheme.colorScheme.onBackgroundVariant,
        titleColor = MiuixTheme.colorScheme.onBackground,
        cachedColor = Color(0xFFF59E0B),
        refreshingColor = Color(0xFF3B82F6),
        syncedColor = Color(0xFF22C55E),
        surfaceColor = MiuixTheme.colorScheme.surface,
        searchBarHideThresholdPx = searchBarHideThresholdPx
    )
}
