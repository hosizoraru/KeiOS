@file:Suppress("FunctionName")
package os.kei.ui.page.main.student.model3d

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.ui.Alignment
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.unit.dp
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.kyant.shapes.RoundedRectangle
import os.kei.R
import os.kei.ui.page.main.os.appLucideRefreshIcon
import os.kei.ui.page.main.settings.section.SettingsLiquidKeyPointSlider
import os.kei.ui.page.main.widget.core.AppTypographyTokens
import os.kei.ui.page.main.widget.glass.*
import os.kei.ui.page.main.widget.sheet.*
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

/** Only this leaf reads the periodically sampled position; the WebView/chrome are not rebuilt per tick. */
@Composable
internal fun GuideModel3dTimeline(backdrop: Backdrop, position: FloatState, duration: FloatState, enabled: Boolean,
    onScrubbing: (Boolean) -> Unit, onSeek: (Float) -> Unit,
) {
    var preview by remember { mutableStateOf<Float?>(null) }
    val total = duration.floatValue
    val current = preview?.times(total) ?: position.floatValue
    Column(Modifier.fillMaxWidth()) {
        Text(stringResource(R.string.guide_model_3d_position_value, current, total), color = MiuixTheme.colorScheme.onBackground,
            fontSize = AppTypographyTokens.Supporting.fontSize, modifier = Modifier.padding(horizontal = 8.dp))
        LiquidMusicProgressSlider(
            value = { preview ?: if (total > 0) (position.floatValue / total).coerceIn(0f, 1f) else 0f },
            onValueChange = { preview = it }, onValueChangeFinished = { onSeek(it * total); preview = null },
            onInteractionChanged = { active -> if (!active) preview = null; onScrubbing(active) }, valueRange = 0f..1f, visibilityThreshold = 0.001f,
            backdrop = backdrop, enabled = enabled && total > 0,
            contentDescription = stringResource(R.string.guide_model_3d_position),
            modifier = Modifier.fillMaxWidth().height(48.dp).padding(horizontal = 4.dp),
        )
    }
}

@Composable
internal fun GuideModel3dToolsSheet(show: Boolean, onDismiss: () -> Unit, hasAnimation: Boolean,
    speed: Float, loop: Boolean, outline: Boolean, outlineWidth: Float,
    onSpeed: (Float) -> Unit, onLoop: (Boolean) -> Unit, onOutline: (Boolean) -> Unit, onOutlineWidth: (Float) -> Unit,
    onRetry: () -> Unit, onBackground: () -> Unit,
) {
    SnapshotWindowBottomSheet(show = show, title = stringResource(R.string.guide_model_3d_tools), onDismissRequest = onDismiss) {
        GuideModel3dToolsContent(hasAnimation, speed, loop, outline, outlineWidth,
            onSpeed, onLoop, onOutline, onOutlineWidth, onRetry, onBackground)
    }
}

@Composable
internal fun GuideModel3dToolsPanel(backdrop: Backdrop, backgroundColor: Color, onDismiss: () -> Unit, hasAnimation: Boolean,
    speed: Float, loop: Boolean, outline: Boolean, outlineWidth: Float,
    onSpeed: (Float) -> Unit, onLoop: (Boolean) -> Unit, onOutline: (Boolean) -> Unit, onOutlineWidth: (Float) -> Unit,
    onRetry: () -> Unit, onBackground: () -> Unit, backgroundExpanded: Boolean,
    background: @Composable () -> Unit,
) {
    val darkContent = model3dUsesDarkContent(backgroundColor)
    val panelBackdrop = rememberLayerBackdrop()
    val surface = if (darkContent) Color.White.copy(alpha = 0.18f) else Color(0xFF101722).copy(alpha = 0.28f)
    LiquidSurface(backdrop = backdrop, modifier = Modifier.fillMaxWidth(), shape = RoundedRectangle(28.dp),
        surfaceColor = surface, effectVariant = GlassVariant.Bar, depthEffect = true, isInteractive = false,
        exportedBackdrop = panelBackdrop) {
      CompositionLocalProvider(LocalLiquidParentBackdrop provides panelBackdrop,
          LocalLiquidParentBackdropOverridesFallback provides true) {
        Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.guide_model_3d_tools), modifier = Modifier.weight(1f),
                    fontSize = AppTypographyTokens.Body.fontSize)
                AppStandaloneLiquidTextButton(text = stringResource(R.string.common_close), onClick = onDismiss,
                    variant = GlassVariant.Compact)
            }
            AppStandaloneLiquidTextButton(text = stringResource(R.string.guide_model_3d_background), onClick = onBackground,
                variant = GlassVariant.SheetAction, modifier = Modifier.fillMaxWidth())
            AnimatedVisibility(backgroundExpanded) { background() }
            GuideModel3dToolsContent(hasAnimation, speed, loop, outline, outlineWidth,
                onSpeed, onLoop, onOutline, onOutlineWidth, onRetry, onBackground, scrollable = false,
                showBackgroundButton = false, containerColor = surface.copy(alpha = 0.10f))
        }
      }
    }
}

@Composable
private fun GuideModel3dToolsContent(hasAnimation: Boolean,
    speed: Float, loop: Boolean, outline: Boolean, outlineWidth: Float,
    onSpeed: (Float) -> Unit, onLoop: (Boolean) -> Unit, onOutline: (Boolean) -> Unit, onOutlineWidth: (Float) -> Unit,
    onRetry: () -> Unit, onBackground: () -> Unit,
    scrollable: Boolean = true,
    showBackgroundButton: Boolean = true,
    containerColor: Color? = null,
) {
    SheetContentColumn(Modifier.fillMaxWidth().padding(horizontal = 16.dp), scrollable = scrollable, verticalSpacing = 12.dp) {
        if (showBackgroundButton) AppStandaloneLiquidTextButton(text = stringResource(R.string.guide_model_3d_background), onClick = onBackground,
            variant = GlassVariant.SheetAction, modifier = Modifier.fillMaxWidth())
        SheetSurfaceCard(containerColor = containerColor) {
            SheetSectionHeader(stringResource(R.string.guide_model_3d_speed),
                summary = stringResource(R.string.guide_model_3d_percent, (speed * 100).toInt()))
            SettingsLiquidKeyPointSlider(speed, onSpeed, 0.25f..2f, listOf(0.25f, 0.5f, 1f, 1.5f, 2f), 0.07f, hasAnimation,
                contentDescription = stringResource(R.string.guide_model_3d_speed))
            val loopLabel = stringResource(R.string.guide_model_3d_loop)
            SheetControlRow(label = loopLabel) { AppSwitch(checked = loop, onCheckedChange = onLoop, enabled = hasAnimation,
                modifier = Modifier.semantics { contentDescription = loopLabel }) }
        }
        SheetSurfaceCard(containerColor = containerColor) {
            val outlineLabel = stringResource(R.string.guide_model_3d_outline)
            SheetControlRow(label = outlineLabel) { AppSwitch(checked = outline, onCheckedChange = onOutline,
                modifier = Modifier.semantics { contentDescription = outlineLabel }) }
            SheetSectionHeader(stringResource(R.string.guide_model_3d_outline_width),
                summary = stringResource(R.string.guide_model_3d_percent, (outlineWidth * 100).toInt()))
            LiquidVolumeSlider(value = { outlineWidth }, onValueChange = onOutlineWidth, valueRange = 0f..1f,
                visibilityThreshold = 0.001f, backdrop = LocalLiquidParentBackdrop.current, enabled = outline,
                contentDescription = stringResource(R.string.guide_model_3d_outline_width), modifier = Modifier.fillMaxWidth().height(48.dp))
        }
        AppStandaloneLiquidTextButton(text = stringResource(R.string.guide_action_retry), onClick = onRetry,
            leadingIcon = appLucideRefreshIcon(), variant = GlassVariant.SheetAction, modifier = Modifier.fillMaxWidth())
    }

}
