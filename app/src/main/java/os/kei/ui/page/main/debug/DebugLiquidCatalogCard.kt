package os.kei.ui.page.main.debug

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.kyant.backdrop.Backdrop
import os.kei.R
import os.kei.ui.page.main.os.appLucideConfigIcon
import os.kei.ui.page.main.os.appLucideDownloadIcon
import os.kei.ui.page.main.os.appLucideFlaskIcon
import os.kei.ui.page.main.os.appLucideHeartIcon
import os.kei.ui.page.main.os.appLucideMoreIcon
import os.kei.ui.page.main.os.appLucidePlayIcon
import os.kei.ui.page.main.os.appLucideShuffleIcon
import os.kei.ui.page.main.widget.core.AppFeatureCard
import os.kei.ui.page.main.widget.core.AppTypographyTokens
import os.kei.ui.page.main.widget.core.CardLayoutRhythm
import os.kei.ui.page.main.widget.chrome.AppChromeTokens
import os.kei.ui.page.main.widget.glass.LiquidBottomTab
import os.kei.ui.page.main.widget.glass.LiquidBottomTabs
import os.kei.ui.page.main.widget.glass.LiquidButton
import os.kei.ui.page.main.widget.glass.LiquidKeyPointSlider
import os.kei.ui.page.main.widget.glass.LiquidMusicProgressSlider
import os.kei.ui.page.main.widget.glass.LiquidPrimaryToggle
import os.kei.ui.page.main.widget.glass.LiquidSlider
import os.kei.ui.page.main.widget.glass.LiquidSliderKeyPoint
import os.kei.ui.page.main.widget.glass.LiquidToggle
import os.kei.ui.page.main.widget.glass.LiquidVolumeSlider
import os.kei.ui.page.main.widget.glass.liquidBottomTabContentColor
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
internal fun DebugLiquidCatalogCard(
    accent: Color,
    backdrop: Backdrop
) {
    var toggleSelected by remember { mutableStateOf(true) }
    var primaryToggleSelected by remember { mutableStateOf(true) }
    var sliderValue by remember { mutableFloatStateOf(0.62f) }
    var volumeValue by remember { mutableFloatStateOf(0.74f) }
    var musicProgress by remember { mutableFloatStateOf(0.38f) }
    var keyPointProgress by remember { mutableFloatStateOf(0.46f) }
    var selectedTabIndex by remember { mutableIntStateOf(0) }
    val tabs = rememberDebugBgmDockTabs()
    val keyPoints = remember {
        listOf(
            LiquidSliderKeyPoint(0.18f),
            LiquidSliderKeyPoint(0.42f),
            LiquidSliderKeyPoint(0.68f),
            LiquidSliderKeyPoint(0.88f)
        )
    }
    val contentColor = MiuixTheme.colorScheme.onBackground
    val secondaryColor = MiuixTheme.colorScheme.onBackgroundVariant
    val buttonSurface = MiuixTheme.colorScheme.surfaceContainer.copy(alpha = 0.22f)

    AppFeatureCard(
        title = stringResource(R.string.debug_component_lab_liquid_catalog_title),
        subtitle = stringResource(R.string.debug_component_lab_liquid_catalog_subtitle),
        sectionIcon = appLucideFlaskIcon(),
        titleColor = accent,
        containerColor = MiuixTheme.colorScheme.surfaceContainer.copy(alpha = 0.58f),
        borderColor = accent.copy(alpha = 0.20f),
        contentVerticalSpacing = CardLayoutRhythm.sectionGap
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            LiquidButton(
                onClick = {},
                backdrop = backdrop,
                tint = accent.copy(alpha = 0.18f),
                surfaceColor = buttonSurface,
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    imageVector = appLucidePlayIcon(),
                    contentDescription = null,
                    tint = contentColor,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = stringResource(R.string.debug_component_lab_liquid_button_primary),
                    color = contentColor,
                    fontSize = AppTypographyTokens.Body.fontSize,
                    lineHeight = AppTypographyTokens.Body.lineHeight,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            LiquidButton(
                onClick = {},
                backdrop = backdrop,
                tint = accent.copy(alpha = 0.12f),
                surfaceColor = buttonSurface,
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    imageVector = appLucideShuffleIcon(),
                    contentDescription = null,
                    tint = contentColor,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = stringResource(R.string.debug_component_lab_liquid_button_shuffle),
                    color = contentColor,
                    fontSize = AppTypographyTokens.Body.fontSize,
                    lineHeight = AppTypographyTokens.Body.lineHeight,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            LiquidButton(
                onClick = {},
                backdrop = backdrop,
                tint = accent.copy(alpha = 0.10f),
                surfaceColor = buttonSurface,
                height = 42.dp,
                horizontalPadding = 12.dp,
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    imageVector = appLucideHeartIcon(),
                    contentDescription = null,
                    tint = contentColor,
                    modifier = Modifier.size(17.dp)
                )
                Text(
                    text = stringResource(R.string.debug_component_lab_liquid_button_compact),
                    color = contentColor,
                    fontSize = AppTypographyTokens.Supporting.fontSize,
                    lineHeight = AppTypographyTokens.Supporting.lineHeight,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            LiquidButton(
                onClick = {},
                backdrop = backdrop,
                surfaceColor = buttonSurface,
                height = 42.dp,
                horizontalPadding = 12.dp,
                enabled = false,
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    imageVector = appLucideDownloadIcon(),
                    contentDescription = null,
                    tint = contentColor,
                    modifier = Modifier.size(17.dp)
                )
                Text(
                    text = stringResource(R.string.debug_component_lab_liquid_button_disabled),
                    color = contentColor,
                    fontSize = AppTypographyTokens.Supporting.fontSize,
                    lineHeight = AppTypographyTokens.Supporting.lineHeight,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        Text(
            text = stringResource(R.string.debug_component_lab_liquid_button_shapes_label),
            color = contentColor,
            fontSize = AppTypographyTokens.Supporting.fontSize,
            lineHeight = AppTypographyTokens.Supporting.lineHeight,
            maxLines = 1
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            LiquidButton(
                onClick = {},
                backdrop = backdrop,
                tint = Color(0xFFFF5C8A).copy(alpha = 0.16f),
                surfaceColor = buttonSurface,
                shape = RoundedCornerShape(14.dp),
                height = 46.dp,
                horizontalPadding = 12.dp,
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    imageVector = appLucideConfigIcon(),
                    contentDescription = null,
                    tint = contentColor,
                    modifier = Modifier.size(17.dp)
                )
                Text(
                    text = stringResource(R.string.debug_component_lab_liquid_button_rounded),
                    color = contentColor,
                    fontSize = AppTypographyTokens.Supporting.fontSize,
                    lineHeight = AppTypographyTokens.Supporting.lineHeight,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            LiquidButton(
                onClick = {},
                backdrop = backdrop,
                tint = Color(0xFFFFC857).copy(alpha = 0.20f),
                surfaceColor = buttonSurface,
                height = 46.dp,
                horizontalPadding = 12.dp,
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    imageVector = appLucideShuffleIcon(),
                    contentDescription = null,
                    tint = contentColor,
                    modifier = Modifier.size(17.dp)
                )
                Text(
                    text = stringResource(R.string.debug_component_lab_liquid_button_oval),
                    color = contentColor,
                    fontSize = AppTypographyTokens.Supporting.fontSize,
                    lineHeight = AppTypographyTokens.Supporting.lineHeight,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            LiquidButton(
                onClick = {},
                backdrop = backdrop,
                tint = Color(0xFF7C5CFF).copy(alpha = 0.16f),
                surfaceColor = buttonSurface,
                shape = CircleShape,
                height = 46.dp,
                horizontalPadding = 0.dp,
                modifier = Modifier.size(46.dp)
            ) {
                Icon(
                    imageVector = appLucideMoreIcon(),
                    contentDescription = stringResource(R.string.debug_component_lab_liquid_button_circle),
                    tint = contentColor,
                    modifier = Modifier.size(19.dp)
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = stringResource(R.string.debug_component_lab_liquid_toggle_label),
                    color = contentColor,
                    fontSize = AppTypographyTokens.Body.fontSize,
                    lineHeight = AppTypographyTokens.Body.lineHeight,
                    maxLines = 1
                )
                Text(
                    text = stringResource(R.string.debug_component_lab_liquid_slider_label),
                    color = secondaryColor,
                    fontSize = AppTypographyTokens.Supporting.fontSize,
                    lineHeight = AppTypographyTokens.Supporting.lineHeight,
                    maxLines = 1
                )
                Text(
                    text = stringResource(R.string.debug_component_lab_liquid_toggle_primary_label),
                    color = secondaryColor,
                    fontSize = AppTypographyTokens.Supporting.fontSize,
                    lineHeight = AppTypographyTokens.Supporting.lineHeight,
                    maxLines = 1
                )
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                LiquidToggle(
                    selected = { toggleSelected },
                    onSelect = { toggleSelected = it },
                    backdrop = backdrop
                )
                LiquidPrimaryToggle(
                    selected = { primaryToggleSelected },
                    onSelect = { primaryToggleSelected = it },
                    backdrop = backdrop
                )
            }
        }

        LiquidSlider(
            value = { sliderValue },
            onValueChange = { sliderValue = it.coerceIn(0f, 1f) },
            valueRange = 0f..1f,
            visibilityThreshold = 0.001f,
            backdrop = backdrop,
            modifier = Modifier
                .fillMaxWidth()
                .height(32.dp)
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.debug_component_lab_liquid_volume_slider_label),
                color = contentColor,
                fontSize = AppTypographyTokens.Supporting.fontSize,
                lineHeight = AppTypographyTokens.Supporting.lineHeight,
                maxLines = 1
            )
            Text(
                text = stringResource(R.string.debug_component_lab_volume_value, (volumeValue * 100).toInt()),
                color = secondaryColor,
                fontSize = AppTypographyTokens.Supporting.fontSize,
                lineHeight = AppTypographyTokens.Supporting.lineHeight,
                maxLines = 1
            )
        }
        LiquidVolumeSlider(
            value = { volumeValue },
            onValueChange = { volumeValue = it.coerceIn(0f, 1f) },
            onValueChangeFinished = { volumeValue = it.coerceIn(0f, 1f) },
            valueRange = 0f..1f,
            visibilityThreshold = 0.001f,
            backdrop = backdrop,
            modifier = Modifier
                .fillMaxWidth()
                .height(32.dp)
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.debug_component_lab_liquid_music_slider_label),
                color = contentColor,
                fontSize = AppTypographyTokens.Supporting.fontSize,
                lineHeight = AppTypographyTokens.Supporting.lineHeight,
                maxLines = 1
            )
            Text(
                text = stringResource(R.string.debug_component_lab_time_sample),
                color = secondaryColor,
                fontSize = AppTypographyTokens.Supporting.fontSize,
                lineHeight = AppTypographyTokens.Supporting.lineHeight,
                maxLines = 1
            )
        }
        LiquidMusicProgressSlider(
            value = { musicProgress },
            onValueChange = { musicProgress = it.coerceIn(0f, 1f) },
            onValueChangeFinished = { musicProgress = it.coerceIn(0f, 1f) },
            valueRange = 0f..1f,
            visibilityThreshold = 0.001f,
            backdrop = backdrop,
            modifier = Modifier
                .fillMaxWidth()
                .height(28.dp)
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.debug_component_lab_liquid_key_points_slider_label),
                color = contentColor,
                fontSize = AppTypographyTokens.Supporting.fontSize,
                lineHeight = AppTypographyTokens.Supporting.lineHeight,
                maxLines = 1
            )
            Text(
                text = stringResource(R.string.debug_component_lab_volume_value, (keyPointProgress * 100).toInt()),
                color = secondaryColor,
                fontSize = AppTypographyTokens.Supporting.fontSize,
                lineHeight = AppTypographyTokens.Supporting.lineHeight,
                maxLines = 1
            )
        }
        LiquidKeyPointSlider(
            value = { keyPointProgress },
            onValueChange = { keyPointProgress = it.coerceIn(0f, 1f) },
            onValueChangeFinished = { keyPointProgress = it.coerceIn(0f, 1f) },
            valueRange = 0f..1f,
            visibilityThreshold = 0.001f,
            backdrop = backdrop,
            keyPoints = keyPoints,
            snapToKeyPoints = true,
            modifier = Modifier
                .fillMaxWidth()
                .height(32.dp)
        )

        LiquidBottomTabs(
            selectedTabIndex = { selectedTabIndex },
            onTabSelected = { selectedTabIndex = it },
            backdrop = backdrop,
            tabsCount = tabs.size,
            accentColor = accent,
            modifier = Modifier
                .fillMaxWidth()
                .height(AppChromeTokens.floatingBottomBarOuterHeight)
        ) {
            tabs.forEachIndexed { index, tab ->
                val sampledTint = liquidBottomTabContentColor(index)
                val tint = if (sampledTint == Color.Unspecified) secondaryColor else sampledTint
                LiquidBottomTab(
                    onClick = { selectedTabIndex = index },
                    tabIndex = index
                ) {
                    Icon(
                        imageVector = tab.icon,
                        contentDescription = tab.label,
                        tint = tint,
                        modifier = Modifier.size(21.dp)
                    )
                    Text(
                        text = tab.label,
                        color = tint,
                        fontSize = AppTypographyTokens.Supporting.fontSize,
                        lineHeight = AppTypographyTokens.Supporting.lineHeight,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}
