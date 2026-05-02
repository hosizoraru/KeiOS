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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.dp
import com.kyant.backdrop.Backdrop
import os.kei.R
import os.kei.ui.page.main.os.appLucideConfigIcon
import os.kei.ui.page.main.os.appLucideDownloadIcon
import os.kei.ui.page.main.os.appLucideFlaskIcon
import os.kei.ui.page.main.os.appLucideHeartIcon
import os.kei.ui.page.main.os.appLucideLayersIcon
import os.kei.ui.page.main.os.appLucideMoreIcon
import os.kei.ui.page.main.os.appLucidePlayIcon
import os.kei.ui.page.main.os.appLucideShareIcon
import os.kei.ui.page.main.os.appLucideShuffleIcon
import os.kei.ui.page.main.os.appLucideTimeIcon
import os.kei.ui.page.main.widget.chrome.AppChromeTokens
import os.kei.ui.page.main.widget.core.AppFeatureCard
import os.kei.ui.page.main.widget.core.AppTypographyTokens
import os.kei.ui.page.main.widget.core.CardLayoutRhythm
import os.kei.ui.page.main.widget.glass.LiquidBottomTab
import os.kei.ui.page.main.widget.glass.LiquidBottomTabs
import os.kei.ui.page.main.widget.glass.AppLiquidButton
import os.kei.ui.page.main.widget.glass.LiquidGlassDropdownActionItem
import os.kei.ui.page.main.widget.glass.LiquidGlassDropdownColumn
import os.kei.ui.page.main.widget.glass.LiquidGlassDropdownSingleChoiceList
import os.kei.ui.page.main.widget.glass.LiquidKeyPointSlider
import os.kei.ui.page.main.widget.glass.LiquidMusicProgressSlider
import os.kei.ui.page.main.widget.glass.AppLiquidPrimaryToggle
import os.kei.ui.page.main.widget.glass.LiquidSlider
import os.kei.ui.page.main.widget.glass.LiquidSliderKeyPoint
import os.kei.ui.page.main.widget.glass.AppLiquidToggle
import os.kei.ui.page.main.widget.glass.LiquidVolumeSlider
import os.kei.ui.page.main.widget.glass.liquidBottomTabContentColor
import os.kei.ui.page.main.widget.sheet.SnapshotPopupPlacement
import os.kei.ui.page.main.widget.sheet.SnapshotWindowListPopup
import os.kei.ui.page.main.widget.sheet.capturePopupAnchor
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.PopupPositionProvider
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
internal fun DebugLiquidCatalogIntroCard(accent: Color) {
    val contentColor = MiuixTheme.colorScheme.onBackground
    AppFeatureCard(
        title = stringResource(R.string.debug_component_lab_liquid_catalog_title),
        subtitle = stringResource(R.string.debug_component_lab_liquid_catalog_subtitle),
        sectionIcon = appLucideFlaskIcon(),
        titleColor = accent,
        containerColor = MiuixTheme.colorScheme.surfaceContainer.copy(alpha = 0.58f),
        borderColor = accent.copy(alpha = 0.20f)
    ) {
        Text(
            text = stringResource(R.string.debug_component_lab_liquid_entry_note),
            color = contentColor,
            fontSize = AppTypographyTokens.Body.fontSize,
            lineHeight = AppTypographyTokens.Body.lineHeight
        )
    }
}

@Composable
internal fun DebugLiquidButtonsCard(
    accent: Color,
    backdrop: Backdrop
) {
    val contentColor = MiuixTheme.colorScheme.onBackground
    val buttonSurface = MiuixTheme.colorScheme.surfaceContainer.copy(alpha = 0.22f)

    AppFeatureCard(
        title = stringResource(R.string.debug_component_lab_liquid_button_shapes_label),
        subtitle = stringResource(R.string.debug_component_lab_liquid_catalog_subtitle),
        sectionIcon = appLucideConfigIcon(),
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
            AppLiquidButton(
                onClick = {},
                backdrop = backdrop,
                tint = accent.copy(alpha = 0.42f),
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
            AppLiquidButton(
                onClick = {},
                backdrop = backdrop,
                tint = accent.copy(alpha = 0.34f),
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
            AppLiquidButton(
                onClick = {},
                backdrop = backdrop,
                tint = accent.copy(alpha = 0.28f),
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
            AppLiquidButton(
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
            AppLiquidButton(
                onClick = {},
                backdrop = backdrop,
                tint = Color(0xFFFF5C8A).copy(alpha = 0.40f),
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
            AppLiquidButton(
                onClick = {},
                backdrop = backdrop,
                tint = Color(0xFFFFC857).copy(alpha = 0.44f),
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
            AppLiquidButton(
                onClick = {},
                backdrop = backdrop,
                tint = Color(0xFF7C5CFF).copy(alpha = 0.42f),
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
    }
}

@Composable
internal fun DebugLiquidGlassDropdownCard(
    accent: Color,
    backdrop: Backdrop
) {
    var expanded by remember { mutableStateOf(false) }
    var anchorBounds by remember { mutableStateOf<IntRect?>(null) }
    var selectedIntervalIndex by remember { mutableIntStateOf(1) }
    val contentColor = MiuixTheme.colorScheme.onBackground
    val buttonSurface = MiuixTheme.colorScheme.surfaceContainer.copy(alpha = 0.20f)
    val intervalIcon = appLucideTimeIcon()
    val intervalOptions = listOf(
        stringResource(R.string.github_refresh_interval_1h),
        stringResource(R.string.github_refresh_interval_3h),
        stringResource(R.string.github_refresh_interval_6h),
        stringResource(R.string.github_refresh_interval_12h)
    )

    AppFeatureCard(
        title = stringResource(R.string.debug_component_lab_liquid_dropdown_label),
        subtitle = stringResource(R.string.debug_component_lab_liquid_dropdown_subtitle),
        sectionIcon = appLucideMoreIcon(),
        titleColor = accent,
        containerColor = MiuixTheme.colorScheme.surfaceContainer.copy(alpha = 0.58f),
        borderColor = accent.copy(alpha = 0.20f),
        contentVerticalSpacing = CardLayoutRhythm.sectionGap
    ) {
        LiquidGlassDropdownColumn(
            accentColor = accent,
            backdrop = backdrop
        ) {
            LiquidGlassDropdownActionItem(
                text = stringResource(R.string.debug_component_lab_action_play),
                highlighted = true,
                onClick = {},
                leadingIcon = appLucidePlayIcon(),
                index = 0,
                optionSize = 4,
                accentColor = accent
            )
            LiquidGlassDropdownActionItem(
                text = stringResource(R.string.debug_component_lab_action_favorite),
                onClick = {},
                leadingIcon = appLucideHeartIcon(),
                index = 1,
                optionSize = 4,
                accentColor = accent
            )
            LiquidGlassDropdownActionItem(
                text = stringResource(R.string.debug_component_lab_action_download),
                onClick = {},
                leadingIcon = appLucideDownloadIcon(),
                index = 2,
                optionSize = 4,
                accentColor = accent
            )
            LiquidGlassDropdownActionItem(
                text = stringResource(R.string.debug_component_lab_action_share),
                onClick = {},
                leadingIcon = appLucideShareIcon(),
                index = 3,
                optionSize = 4,
                accentColor = accent
            )
        }

        Text(
            text = stringResource(R.string.debug_component_lab_liquid_dropdown_single_choice),
            color = contentColor,
            fontSize = AppTypographyTokens.Supporting.fontSize,
            lineHeight = AppTypographyTokens.Supporting.lineHeight,
            fontWeight = FontWeight.Medium
        )
        LiquidGlassDropdownColumn(
            accentColor = accent,
            backdrop = backdrop
        ) {
            LiquidGlassDropdownSingleChoiceList(
                options = intervalOptions,
                selectedIndex = selectedIntervalIndex,
                onSelectedIndexChange = { selectedIntervalIndex = it },
                leadingIcon = intervalIcon,
                accentColor = accent
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            AppLiquidButton(
                onClick = { expanded = true },
                backdrop = backdrop,
                tint = Color.Unspecified,
                surfaceColor = buttonSurface,
                height = 44.dp,
                horizontalPadding = 14.dp,
                modifier = Modifier.capturePopupAnchor { anchorBounds = it }
            ) {
                Icon(
                    imageVector = appLucideMoreIcon(),
                    contentDescription = null,
                    tint = contentColor,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = stringResource(R.string.debug_component_lab_liquid_dropdown_open),
                    color = contentColor,
                    fontSize = AppTypographyTokens.Supporting.fontSize,
                    lineHeight = AppTypographyTokens.Supporting.lineHeight,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        if (expanded) {
            SnapshotWindowListPopup(
                show = true,
                alignment = PopupPositionProvider.Align.BottomEnd,
                anchorBounds = anchorBounds,
                placement = SnapshotPopupPlacement.ButtonEnd,
                enableWindowDim = false,
                onDismissRequest = { expanded = false }
            ) {
                LiquidGlassDropdownColumn(accentColor = accent) {
                    LiquidGlassDropdownActionItem(
                        text = stringResource(R.string.debug_component_lab_action_shuffle),
                        onClick = { expanded = false },
                        leadingIcon = appLucideShuffleIcon(),
                        index = 0,
                        optionSize = 3,
                        accentColor = accent
                    )
                    LiquidGlassDropdownActionItem(
                        text = stringResource(R.string.debug_component_lab_action_favorite),
                        onClick = { expanded = false },
                        leadingIcon = appLucideHeartIcon(),
                        index = 1,
                        optionSize = 3,
                        accentColor = accent
                    )
                    LiquidGlassDropdownActionItem(
                        text = stringResource(R.string.debug_component_lab_action_share),
                        onClick = { expanded = false },
                        leadingIcon = appLucideShareIcon(),
                        index = 2,
                        optionSize = 3,
                        accentColor = accent
                    )
                }
            }
        }
    }
}

@Composable
internal fun DebugLiquidBackdropCard(accent: Color) {
    val contentColor = MiuixTheme.colorScheme.onBackground
    val secondaryColor = MiuixTheme.colorScheme.onBackgroundVariant
    AppFeatureCard(
        title = stringResource(R.string.debug_component_lab_liquid_backdrop_playground_label),
        subtitle = stringResource(R.string.debug_component_lab_liquid_large_clear_card_body),
        sectionIcon = appLucideFlaskIcon(),
        titleColor = accent,
        containerColor = MiuixTheme.colorScheme.surfaceContainer.copy(alpha = 0.58f),
        borderColor = accent.copy(alpha = 0.20f),
        contentVerticalSpacing = CardLayoutRhythm.sectionGap
    ) {
        DebugLiquidBackdropPlaygroundSample(
            accent = accent,
            contentColor = contentColor,
            secondaryColor = secondaryColor
        )
    }
}

@Composable
internal fun DebugLiquidTransparentButtonsCard(
    accent: Color,
    backdrop: Backdrop
) {
    val contentColor = MiuixTheme.colorScheme.onBackground
    AppFeatureCard(
        title = stringResource(R.string.debug_component_lab_liquid_clear_label),
        subtitle = stringResource(R.string.debug_component_lab_liquid_clear_capsule),
        sectionIcon = appLucidePlayIcon(),
        titleColor = accent,
        containerColor = MiuixTheme.colorScheme.surfaceContainer.copy(alpha = 0.58f),
        borderColor = accent.copy(alpha = 0.20f),
        contentVerticalSpacing = CardLayoutRhythm.sectionGap
    ) {
        DebugLiquidTransparentButtonSamples(
            backdrop = backdrop,
            contentColor = contentColor
        )
    }
}

@Composable
internal fun DebugLiquidSurfaceCardsCard(
    accent: Color,
    backdrop: Backdrop
) {
    val contentColor = MiuixTheme.colorScheme.onBackground
    val secondaryColor = MiuixTheme.colorScheme.onBackgroundVariant
    AppFeatureCard(
        title = stringResource(R.string.debug_component_lab_liquid_surface_family_label),
        subtitle = stringResource(R.string.debug_component_lab_liquid_cluster_card_body),
        sectionIcon = appLucideLayersIcon(),
        titleColor = accent,
        containerColor = MiuixTheme.colorScheme.surfaceContainer.copy(alpha = 0.58f),
        borderColor = accent.copy(alpha = 0.20f),
        contentVerticalSpacing = CardLayoutRhythm.sectionGap
    ) {
        DebugLiquidSurfaceFamilySamples(
            backdrop = backdrop,
            accent = accent,
            contentColor = contentColor,
            secondaryColor = secondaryColor
        )

        DebugLiquidClusterCardSample(
            backdrop = backdrop,
            accent = accent,
            contentColor = contentColor,
            secondaryColor = secondaryColor
        )
    }
}

@Composable
internal fun DebugLiquidParameterCard(
    accent: Color,
    backdrop: Backdrop
) {
    var chromaticAberrationEnabled by remember { mutableStateOf(true) }
    var cornerDemoValue by remember { mutableFloatStateOf(0.58f) }
    var blurDemoValue by remember { mutableFloatStateOf(0.16f) }
    var refractionHeightDemoValue by remember { mutableFloatStateOf(0.42f) }
    var refractionDemoValue by remember { mutableFloatStateOf(0.24f) }
    val contentColor = MiuixTheme.colorScheme.onBackground
    val secondaryColor = MiuixTheme.colorScheme.onBackgroundVariant
    AppFeatureCard(
        title = stringResource(R.string.debug_component_lab_liquid_parameter_panel_title),
        subtitle = stringResource(R.string.debug_component_lab_liquid_parameter_preview_body),
        sectionIcon = appLucideConfigIcon(),
        titleColor = accent,
        containerColor = MiuixTheme.colorScheme.surfaceContainer.copy(alpha = 0.58f),
        borderColor = accent.copy(alpha = 0.20f),
        contentVerticalSpacing = CardLayoutRhythm.sectionGap
    ) {
        DebugLiquidParameterPanelSample(
            backdrop = backdrop,
            accent = accent,
            contentColor = contentColor,
            secondaryColor = secondaryColor,
            cornerDemoValue = cornerDemoValue,
            onCornerDemoValueChange = { cornerDemoValue = it.coerceIn(0f, 1f) },
            blurDemoValue = blurDemoValue,
            onBlurDemoValueChange = { blurDemoValue = it.coerceIn(0f, 1f) },
            refractionHeightValue = refractionHeightDemoValue,
            onRefractionHeightValueChange = { refractionHeightDemoValue = it.coerceIn(0f, 1f) },
            refractionDemoValue = refractionDemoValue,
            onRefractionDemoValueChange = { refractionDemoValue = it.coerceIn(0f, 1f) },
            chromaticAberrationEnabled = chromaticAberrationEnabled,
            onChromaticAberrationChange = { chromaticAberrationEnabled = it }
        )
    }
}

@Composable
internal fun DebugLiquidControlsCard(
    accent: Color,
    backdrop: Backdrop
) {
    var toggleSelected by remember { mutableStateOf(true) }
    var primaryToggleSelected by remember { mutableStateOf(true) }
    var sliderValue by remember { mutableFloatStateOf(0.62f) }
    var volumeValue by remember { mutableFloatStateOf(0.74f) }
    var musicProgress by remember { mutableFloatStateOf(0.38f) }
    var keyPointProgress by remember { mutableFloatStateOf(0.46f) }
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
    AppFeatureCard(
        title = stringResource(R.string.debug_component_lab_liquid_slider_label),
        subtitle = stringResource(R.string.debug_component_lab_liquid_key_points_slider_label),
        sectionIcon = appLucideConfigIcon(),
        titleColor = accent,
        containerColor = MiuixTheme.colorScheme.surfaceContainer.copy(alpha = 0.58f),
        borderColor = accent.copy(alpha = 0.20f),
        contentVerticalSpacing = CardLayoutRhythm.sectionGap
    ) {

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
                AppLiquidToggle(
                    selected = { toggleSelected },
                    onSelect = { toggleSelected = it },
                    backdrop = backdrop
                )
                AppLiquidPrimaryToggle(
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

        DebugLiquidSliderSamples(
            volumeValue = volumeValue,
            onVolumeValueChange = { volumeValue = it.coerceIn(0f, 1f) },
            musicProgress = musicProgress,
            onMusicProgressChange = { musicProgress = it.coerceIn(0f, 1f) },
            keyPointProgress = keyPointProgress,
            onKeyPointProgressChange = { keyPointProgress = it.coerceIn(0f, 1f) },
            keyPoints = keyPoints,
            backdrop = backdrop,
            contentColor = contentColor,
            secondaryColor = secondaryColor
        )
    }
}

@Composable
internal fun DebugLiquidBottomTabsCard(
    accent: Color,
    backdrop: Backdrop
) {
    var selectedTabIndex by remember { mutableIntStateOf(0) }
    val tabs = rememberDebugBgmDockTabs()
    val secondaryColor = MiuixTheme.colorScheme.onBackgroundVariant
    AppFeatureCard(
        title = stringResource(R.string.debug_component_lab_liquid_bottom_tabs_label),
        subtitle = stringResource(R.string.debug_component_lab_liquid_catalog_subtitle),
        sectionIcon = appLucideLayersIcon(),
        titleColor = accent,
        containerColor = MiuixTheme.colorScheme.surfaceContainer.copy(alpha = 0.58f),
        borderColor = accent.copy(alpha = 0.20f),
        contentVerticalSpacing = CardLayoutRhythm.sectionGap
    ) {
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

@Composable
private fun DebugLiquidSliderSamples(
    volumeValue: Float,
    onVolumeValueChange: (Float) -> Unit,
    musicProgress: Float,
    onMusicProgressChange: (Float) -> Unit,
    keyPointProgress: Float,
    onKeyPointProgressChange: (Float) -> Unit,
    keyPoints: List<LiquidSliderKeyPoint>,
    backdrop: Backdrop,
    contentColor: Color,
    secondaryColor: Color
) {
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
        onValueChange = onVolumeValueChange,
        onValueChangeFinished = onVolumeValueChange,
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
        onValueChange = onMusicProgressChange,
        onValueChangeFinished = onMusicProgressChange,
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
        onValueChange = onKeyPointProgressChange,
        onValueChangeFinished = onKeyPointProgressChange,
        valueRange = 0f..1f,
        visibilityThreshold = 0.001f,
        backdrop = backdrop,
        keyPoints = keyPoints,
        snapToKeyPoints = true,
        modifier = Modifier
            .fillMaxWidth()
            .height(32.dp)
    )
}
