package os.kei.ui.page.main.debug

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDp
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.updateTransition
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kyant.capsule.ContinuousCapsule
import os.kei.R
import os.kei.ui.page.main.os.appLucideGridIcon
import os.kei.ui.page.main.os.appLucideHomeIcon
import os.kei.ui.page.main.os.appLucideLibraryIcon
import os.kei.ui.page.main.os.appLucideMusicIcon
import os.kei.ui.page.main.os.appLucidePauseIcon
import os.kei.ui.page.main.os.appLucidePlayIcon
import os.kei.ui.page.main.os.appLucideRadioIcon
import os.kei.ui.page.main.os.appLucideSearchIcon
import os.kei.ui.page.main.os.appLucideSkipBackIcon
import os.kei.ui.page.main.os.appLucideSkipForwardIcon
import os.kei.ui.page.main.widget.chrome.AppChromeTokens
import os.kei.ui.page.main.widget.core.AppTypographyTokens
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.LinearProgressIndicator
import top.yukonga.miuix.kmp.basic.ProgressIndicatorDefaults
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Stable
internal class DebugBgmBottomChromeScrollState(
    private val scrollThresholdPx: Float
) : NestedScrollConnection {
    var isCompact by mutableStateOf(false)
        private set

    private var accumulatedScroll = 0f

    fun expand() {
        isCompact = false
        accumulatedScroll = 0f
    }

    fun compact() {
        isCompact = true
        accumulatedScroll = 0f
    }

    override fun onPreScroll(
        available: androidx.compose.ui.geometry.Offset,
        source: NestedScrollSource
    ): androidx.compose.ui.geometry.Offset {
        val scrollDelta = available.y
        if ((accumulatedScroll > 0f && scrollDelta < 0f) || (accumulatedScroll < 0f && scrollDelta > 0f)) {
            accumulatedScroll = 0f
        }

        accumulatedScroll += scrollDelta
        if (accumulatedScroll <= -scrollThresholdPx && !isCompact) {
            compact()
        } else if (accumulatedScroll >= scrollThresholdPx && isCompact) {
            expand()
        }
        return androidx.compose.ui.geometry.Offset.Zero
    }
}

@Composable
internal fun rememberDebugBgmBottomChromeScrollState(
    scrollThreshold: Dp = 56.dp
): DebugBgmBottomChromeScrollState = with(LocalDensity.current) {
    val thresholdPx = scrollThreshold.toPx()
    remember(thresholdPx) { DebugBgmBottomChromeScrollState(thresholdPx) }
}

@Composable
internal fun DebugBgmFloatingBottomChrome(
    accent: Color,
    scrollState: DebugBgmBottomChromeScrollState,
    currentTrackTitle: String,
    isPlaying: Boolean,
    onPlayPauseClick: () -> Unit,
    onPreviousClick: () -> Unit,
    onNextClick: () -> Unit,
    searchVisible: Boolean,
    selectedDockKey: String,
    onSelectedDockKeyChange: (String) -> Unit,
    onSearchClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val tabs = rememberDebugBgmDockTabs()
    val transition = updateTransition(
        targetState = scrollState.isCompact,
        label = "debug_bgm_bottom_chrome"
    )
    val animationSpec = spring<Dp>(
        dampingRatio = Spring.DampingRatioNoBouncy,
        stiffness = Spring.StiffnessMediumLow
    )
    val floatAnimationSpec = spring<Float>(
        dampingRatio = Spring.DampingRatioNoBouncy,
        stiffness = Spring.StiffnessMediumLow
    )
    val containerHeight by transition.animateDp(
        transitionSpec = { animationSpec },
        label = "debug_bgm_chrome_height"
    ) { compact ->
        if (compact) DebugBgmCompactChromeHeight else DebugBgmExpandedChromeHeight
    }
    val tabGroupHeight by transition.animateDp(
        transitionSpec = { animationSpec },
        label = "debug_bgm_tab_height"
    ) { compact ->
        if (compact) DebugBgmCompactControlSize else DebugBgmExpandedDockHeight
    }
    val tabGroupY by transition.animateDp(
        transitionSpec = { animationSpec },
        label = "debug_bgm_tab_y"
    ) { compact ->
        if (compact) DebugBgmCompactControlInset else DebugBgmExpandedDockY
    }
    val miniPlayerHeight by transition.animateDp(
        transitionSpec = { animationSpec },
        label = "debug_bgm_mini_height"
    ) { compact ->
        if (compact) DebugBgmCompactMiniHeight else DebugBgmExpandedMiniHeight
    }
    val miniPlayerY by transition.animateDp(
        transitionSpec = { animationSpec },
        label = "debug_bgm_mini_y"
    ) { compact ->
        if (compact) DebugBgmCompactMiniY else 0.dp
    }
    val searchSize by transition.animateDp(
        transitionSpec = { animationSpec },
        label = "debug_bgm_search_size"
    ) { compact ->
        if (compact) DebugBgmCompactControlSize else DebugBgmExpandedDockHeight
    }
    val searchY by transition.animateDp(
        transitionSpec = { animationSpec },
        label = "debug_bgm_search_y"
    ) { compact ->
        if (compact) DebugBgmCompactControlInset else DebugBgmExpandedDockY
    }
    val expandedAlpha by transition.animateFloat(
        transitionSpec = { floatAnimationSpec },
        label = "debug_bgm_expanded_alpha"
    ) { compact ->
        if (compact) 0f else 1f
    }
    val compactAlpha by transition.animateFloat(
        transitionSpec = { floatAnimationSpec },
        label = "debug_bgm_compact_alpha"
    ) { compact ->
        if (compact) 1f else 0f
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .height(containerHeight)
    ) {
        val tabGroupExpandedWidth = (maxWidth - DebugBgmExpandedSearchSpacing - DebugBgmExpandedDockHeight)
            .coerceAtLeast(260.dp)
        val compactMiniWidth = boundedDp(
            value = maxWidth - (DebugBgmCompactControlSize * 2f) - 34.dp,
            min = 190.dp,
            max = 244.dp
        )
        val miniPlayerWidth by transition.animateDp(
            transitionSpec = { animationSpec },
            label = "debug_bgm_mini_width"
        ) { compact ->
            if (compact) compactMiniWidth else maxWidth
        }
        val miniPlayerX by transition.animateDp(
            transitionSpec = { animationSpec },
            label = "debug_bgm_mini_x"
        ) { compact ->
            if (compact) (maxWidth - compactMiniWidth) / 2f else 0.dp
        }
        val tabGroupWidth by transition.animateDp(
            transitionSpec = { animationSpec },
            label = "debug_bgm_tab_width"
        ) { compact ->
            if (compact) DebugBgmCompactControlSize else tabGroupExpandedWidth
        }
        val searchX by transition.animateDp(
            transitionSpec = { animationSpec },
            label = "debug_bgm_search_x"
        ) { compact ->
            maxWidth - if (compact) DebugBgmCompactControlSize else DebugBgmExpandedDockHeight
        }

        DebugBgmBottomSurface(
            modifier = Modifier
                .offset(x = miniPlayerX, y = miniPlayerY)
                .width(miniPlayerWidth)
                .height(miniPlayerHeight),
            shape = ContinuousCapsule,
            onClick = {}
        ) {
            DebugBgmMiniPlayer(
                accent = accent,
                currentTrackTitle = currentTrackTitle,
                isPlaying = isPlaying,
                compact = scrollState.isCompact,
                expandedAlpha = expandedAlpha,
                onPlayPauseClick = onPlayPauseClick,
                onPreviousClick = onPreviousClick,
                onNextClick = onNextClick,
                modifier = Modifier.fillMaxSize()
            )
        }

        DebugBgmBottomSurface(
            modifier = Modifier
                .offset(x = 0.dp, y = tabGroupY)
                .width(tabGroupWidth)
                .height(tabGroupHeight),
            shape = ContinuousCapsule
        ) {
            DebugBgmDockGroupContent(
                tabs = tabs,
                selectedDockKey = selectedDockKey,
                accent = accent,
                compact = scrollState.isCompact,
                expandedAlpha = expandedAlpha,
                compactAlpha = compactAlpha,
                onSelectedDockKeyChange = onSelectedDockKeyChange
            )
        }

        DebugBgmBottomSurface(
            modifier = Modifier
                .offset(x = searchX, y = searchY)
                .size(searchSize),
            shape = CircleShape,
            onClick = onSearchClick
        ) {
            DebugBgmDockTabIcon(
                icon = appLucideSearchIcon(),
                label = stringResource(R.string.debug_component_lab_nav_search),
                selected = searchVisible,
                accent = accent,
                iconSize = 27.dp
            )
        }
    }
}

@Composable
private fun DebugBgmBottomSurface(
    modifier: Modifier,
    shape: Shape,
    onClick: (() -> Unit)? = null,
    content: @Composable () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    Box(
        modifier = modifier
            .clip(shape)
            .background(MiuixTheme.colorScheme.surfaceContainer.copy(alpha = 0.96f), shape)
            .border(1.dp, Color.White.copy(alpha = 0.24f), shape)
            .then(
                if (onClick != null) {
                    Modifier.clickable(
                        interactionSource = interactionSource,
                        indication = null,
                        onClick = onClick
                    )
                } else {
                    Modifier
                }
            ),
        contentAlignment = Alignment.Center
    ) {
        content()
    }
}

@Composable
private fun DebugBgmDockGroupContent(
    tabs: List<DebugBgmDockTab>,
    selectedDockKey: String,
    accent: Color,
    compact: Boolean,
    expandedAlpha: Float,
    compactAlpha: Float,
    onSelectedDockKeyChange: (String) -> Unit
) {
    Box(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer { alpha = expandedAlpha }
                .padding(horizontal = 8.dp, vertical = 5.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            tabs.forEach { tab ->
                DebugBgmExpandedDockTab(
                    tab = tab,
                    selected = selectedDockKey == tab.key,
                    accent = accent,
                    onClick = { onSelectedDockKeyChange(tab.key) },
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                )
            }
        }

        val compactTab = tabs.firstOrNull { it.key == selectedDockKey } ?: tabs.last()
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer { alpha = compactAlpha }
                .clip(CircleShape)
                .then(
                    if (compact) {
                        Modifier.clickable { onSelectedDockKeyChange(compactTab.key) }
                    } else {
                        Modifier
                    }
                ),
            contentAlignment = Alignment.Center
        ) {
            DebugBgmDockTabIcon(
                icon = compactTab.icon,
                label = compactTab.label,
                selected = true,
                accent = accent,
                iconSize = 27.dp
            )
        }
    }
}

@Composable
private fun DebugBgmExpandedDockTab(
    tab: DebugBgmDockTab,
    selected: Boolean,
    accent: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(ContinuousCapsule)
            .background(if (selected) accent.copy(alpha = 0.10f) else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        DebugBgmDockTabIcon(
            icon = tab.icon,
            label = tab.label,
            selected = selected,
            accent = accent
        )
        Text(
            text = tab.label,
            color = DebugBgmDockTint(selected = selected, accent = accent),
            fontSize = 11.sp,
            lineHeight = 13.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun DebugBgmDockTabIcon(
    icon: ImageVector,
    label: String,
    selected: Boolean,
    accent: Color,
    iconSize: Dp = 24.dp
) {
    Icon(
        imageVector = icon,
        contentDescription = label,
        tint = DebugBgmDockTint(selected = selected, accent = accent),
        modifier = Modifier.size(iconSize)
    )
}

@Composable
private fun DebugBgmMiniPlayer(
    accent: Color,
    currentTrackTitle: String,
    isPlaying: Boolean,
    compact: Boolean,
    expandedAlpha: Float,
    onPlayPauseClick: () -> Unit,
    onPreviousClick: () -> Unit,
    onNextClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val artworkSize = if (compact) 38.dp else 42.dp
    val artworkCornerRadius = if (compact) 10.dp else 11.dp
    val contentPadding = PaddingValues(
        horizontal = if (compact) 10.dp else 14.dp,
        vertical = if (compact) 8.dp else 7.dp
    )
    val titleFontSize = if (compact) 12.sp else AppTypographyTokens.Supporting.fontSize
    val titleLineHeight = if (compact) 14.sp else AppTypographyTokens.Supporting.lineHeight
    val playIconSize = if (compact) 27.dp else 25.dp

    Row(
        modifier = modifier.padding(contentPadding),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(artworkSize)
                .clip(RoundedCornerShape(artworkCornerRadius))
                .background(
                    Brush.linearGradient(
                        colors = listOf(Color(0xFFFFC857), accent, Color(0xFFFF4D6D))
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = appLucideMusicIcon(),
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(if (compact) 21.dp else 23.dp)
            )
        }
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = currentTrackTitle,
                color = MiuixTheme.colorScheme.onBackground,
                fontSize = titleFontSize,
                lineHeight = titleLineHeight,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (!compact) {
                LinearProgressIndicator(
                    progress = DebugBgmMiniPlayerProgress,
                    modifier = Modifier
                        .padding(top = 5.dp)
                        .fillMaxWidth()
                        .graphicsLayer { alpha = expandedAlpha },
                    height = 3.dp,
                    colors = ProgressIndicatorDefaults.progressIndicatorColors(
                        foregroundColor = accent,
                        backgroundColor = MiuixTheme.colorScheme.onBackgroundVariant.copy(alpha = 0.22f)
                    )
                )
            }
        }
        if (!compact) {
            DebugBgmInlineIcon(
                icon = appLucideSkipBackIcon(),
                contentDescription = stringResource(R.string.debug_component_lab_action_previous),
                tint = MiuixTheme.colorScheme.onBackground,
                size = 32.dp,
                iconSize = 22.dp,
                onClick = onPreviousClick
            )
        }
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .clickable(onClick = onPlayPauseClick),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (isPlaying) appLucidePauseIcon() else appLucidePlayIcon(),
                contentDescription = stringResource(
                    if (isPlaying) R.string.debug_component_lab_action_pause else R.string.debug_component_lab_action_play
                ),
                tint = MiuixTheme.colorScheme.onBackground,
                modifier = Modifier.size(playIconSize)
            )
        }
        if (!compact) {
            DebugBgmInlineIcon(
                icon = appLucideSkipForwardIcon(),
                contentDescription = stringResource(R.string.debug_component_lab_action_next),
                tint = MiuixTheme.colorScheme.onBackground,
                size = 32.dp,
                iconSize = 22.dp,
                onClick = onNextClick
            )
        }
    }
}

private fun boundedDp(
    value: Dp,
    min: Dp,
    max: Dp
): Dp = value.value.coerceIn(min.value, max.value).dp

private const val DebugBgmMiniPlayerProgress = 0.42f
private val DebugBgmChromeControlHeight = AppChromeTokens.floatingBottomBarOuterHeight
private val DebugBgmChromeStackGap = AppChromeTokens.pageSectionGap
private val DebugBgmExpandedChromeHeight = DebugBgmChromeControlHeight * 2f + DebugBgmChromeStackGap
private val DebugBgmCompactChromeHeight = DebugBgmChromeControlHeight
private val DebugBgmExpandedMiniHeight = DebugBgmChromeControlHeight
private val DebugBgmCompactMiniHeight = DebugBgmChromeControlHeight
private val DebugBgmCompactMiniY = 0.dp
private val DebugBgmExpandedDockHeight = DebugBgmChromeControlHeight
private val DebugBgmExpandedDockY = DebugBgmChromeControlHeight + DebugBgmChromeStackGap
private val DebugBgmExpandedSearchSpacing = AppChromeTokens.pageSectionGap
private val DebugBgmCompactControlSize = DebugBgmChromeControlHeight
private val DebugBgmCompactControlInset = 0.dp

@Composable
private fun rememberDebugBgmDockTabs(): List<DebugBgmDockTab> {
    val homeLabel = stringResource(R.string.debug_component_lab_nav_home)
    val discoverLabel = stringResource(R.string.debug_component_lab_nav_discover)
    val radioLabel = stringResource(R.string.debug_component_lab_nav_radio)
    val libraryLabel = stringResource(R.string.debug_component_lab_nav_library)
    val homeIcon = appLucideHomeIcon()
    val discoverIcon = appLucideGridIcon()
    val radioIcon = appLucideRadioIcon()
    val libraryIcon = appLucideLibraryIcon()
    return remember(
        homeLabel,
        discoverLabel,
        radioLabel,
        libraryLabel,
        homeIcon,
        discoverIcon,
        radioIcon,
        libraryIcon
    ) {
        listOf(
            DebugBgmDockTab(DebugBgmDockKeys.Home, homeIcon, homeLabel),
            DebugBgmDockTab(DebugBgmDockKeys.Discover, discoverIcon, discoverLabel),
            DebugBgmDockTab(DebugBgmDockKeys.Radio, radioIcon, radioLabel),
            DebugBgmDockTab(DebugBgmDockKeys.Library, libraryIcon, libraryLabel)
        )
    }
}

@Composable
private fun DebugBgmDockTint(
    selected: Boolean,
    accent: Color
): Color = if (selected) accent else MiuixTheme.colorScheme.onBackground

internal object DebugBgmDockKeys {
    const val Home = "home"
    const val Discover = "discover"
    const val Radio = "radio"
    const val Library = "library"
}

private data class DebugBgmDockTab(
    val key: String,
    val icon: ImageVector,
    val label: String
)
