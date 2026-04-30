@file:OptIn(ExperimentalSharedTransitionApi::class)

package os.kei.ui.page.main.debug

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.BoundsTransform
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kyant.capsule.ContinuousCapsule
import os.kei.R
import os.kei.ui.component.floatingtabbar.FloatingTabBar
import os.kei.ui.component.floatingtabbar.FloatingTabBarDefaults
import os.kei.ui.component.floatingtabbar.FloatingTabBarScrollConnection
import os.kei.ui.page.main.os.appLucideGridIcon
import os.kei.ui.page.main.os.appLucideHomeIcon
import os.kei.ui.page.main.os.appLucideLibraryIcon
import os.kei.ui.page.main.os.appLucideMusicIcon
import os.kei.ui.page.main.os.appLucidePlayIcon
import os.kei.ui.page.main.os.appLucideRadioIcon
import os.kei.ui.page.main.os.appLucideSearchIcon
import os.kei.ui.page.main.os.appLucideSkipForwardIcon
import os.kei.ui.page.main.widget.core.AppTypographyTokens
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.LinearProgressIndicator
import top.yukonga.miuix.kmp.basic.ProgressIndicatorDefaults
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
internal fun DebugBgmFloatingBottomChrome(
    accent: Color,
    scrollConnection: FloatingTabBarScrollConnection,
    selectedDockKey: String,
    onSelectedDockKeyChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val tabs = rememberDebugBgmDockTabs()
    FloatingTabBar(
        selectedTabKey = selectedDockKey,
        scrollConnection = scrollConnection,
        modifier = modifier.fillMaxWidth(),
        inlineAccessory = { accessoryModifier, animatedVisibilityScope ->
            DebugBgmMiniPlayer(
                accent = accent,
                compact = true,
                animatedVisibilityScope = animatedVisibilityScope,
                modifier = accessoryModifier.fillMaxSize()
            )
        },
        expandedAccessory = { accessoryModifier, animatedVisibilityScope ->
            DebugBgmMiniPlayer(
                accent = accent,
                compact = false,
                animatedVisibilityScope = animatedVisibilityScope,
                modifier = accessoryModifier
                    .fillMaxWidth()
                    .height(64.dp)
            )
        },
        colors = FloatingTabBarDefaults.colors(
            backgroundColor = MiuixTheme.colorScheme.surfaceContainer.copy(alpha = 0.96f),
            accessoryBackgroundColor = MiuixTheme.colorScheme.surfaceContainer.copy(alpha = 0.96f)
        ),
        shapes = FloatingTabBarDefaults.shapes(
            tabBarShape = ContinuousCapsule,
            tabShape = ContinuousCapsule,
            standaloneTabShape = CircleShape,
            accessoryShape = ContinuousCapsule
        ),
        sizes = FloatingTabBarDefaults.sizes(
            tabBarContentPadding = PaddingValues(horizontal = 8.dp, vertical = 5.dp),
            tabInlineContentPadding = PaddingValues(12.dp),
            tabExpandedContentPadding = PaddingValues(horizontal = 20.dp, vertical = 7.dp),
            componentSpacing = 8.dp,
            tabSpacing = 0.dp
        ),
        elevations = FloatingTabBarDefaults.elevations(
            inlineElevation = 0.dp,
            expandedElevation = 0.dp
        ),
        contentKey = tabs.size
    ) {
        tabs.forEach { tab ->
            tab(
                key = tab.key,
                title = {
                    DebugBgmDockTabTitle(
                        label = tab.label,
                        selected = selectedDockKey == tab.key,
                        accent = accent
                    )
                },
                icon = {
                    DebugBgmDockTabIcon(
                        icon = tab.icon,
                        label = tab.label,
                        selected = selectedDockKey == tab.key,
                        accent = accent
                    )
                },
                onClick = { onSelectedDockKeyChange(tab.key) }
            )
        }
        standaloneTab(
            key = DebugBgmDockKeys.Search,
            icon = {
                DebugBgmDockTabIcon(
                    icon = appLucideSearchIcon(),
                    label = stringResource(R.string.debug_component_lab_nav_search),
                    selected = selectedDockKey == DebugBgmDockKeys.Search,
                    accent = accent,
                    iconSize = 25.dp
                )
            },
            onClick = { onSelectedDockKeyChange(DebugBgmDockKeys.Search) }
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
private fun DebugBgmDockTabTitle(
    label: String,
    selected: Boolean,
    accent: Color
) {
    Text(
        text = label,
        color = DebugBgmDockTint(selected = selected, accent = accent),
        fontSize = 11.sp,
        lineHeight = 13.sp,
        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
    )
}

@Composable
private fun SharedTransitionScope.DebugBgmMiniPlayer(
    accent: Color,
    compact: Boolean,
    animatedVisibilityScope: AnimatedVisibilityScope,
    modifier: Modifier = Modifier
) {
    val artworkSize = if (compact) 32.dp else 38.dp
    val artworkCornerRadius = if (compact) 8.dp else 10.dp
    val artworkIconSize = if (compact) 18.dp else 21.dp
    val contentPadding = PaddingValues(
        horizontal = if (compact) 10.dp else 14.dp,
        vertical = if (compact) 5.dp else 7.dp
    )
    val titleFontSize = if (compact) 12.sp else AppTypographyTokens.Supporting.fontSize
    val titleLineHeight = if (compact) 14.sp else AppTypographyTokens.Supporting.lineHeight
    val playButtonSize = if (compact) 28.dp else 30.dp
    val playIconSize = if (compact) 22.dp else 23.dp
    val miniPlayerInteraction = remember { MutableInteractionSource() }

    Row(
        modifier = modifier
            .clip(ContinuousCapsule)
            .clickable(
                interactionSource = miniPlayerInteraction,
                indication = null,
                onClick = {}
            )
            .padding(contentPadding),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .sharedElement(
                    sharedContentState = rememberSharedContentState("debug_bgm_artwork"),
                    animatedVisibilityScope = animatedVisibilityScope,
                    boundsTransform = DebugBgmMiniPlayerBoundsTransform
                )
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
                modifier = Modifier
                    .sharedBounds(
                        sharedContentState = rememberSharedContentState("debug_bgm_artwork_icon"),
                        enter = EnterTransition.None,
                        exit = ExitTransition.None,
                        animatedVisibilityScope = animatedVisibilityScope,
                        boundsTransform = DebugBgmMiniPlayerBoundsTransform
                    )
                    .size(artworkIconSize)
            )
        }
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            Text(
                text = stringResource(R.string.debug_component_lab_mini_player_title),
                color = MiuixTheme.colorScheme.onBackground,
                fontSize = titleFontSize,
                lineHeight = titleLineHeight,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .sharedBounds(
                        sharedContentState = rememberSharedContentState("debug_bgm_mini_title"),
                        animatedVisibilityScope = animatedVisibilityScope,
                        boundsTransform = DebugBgmMiniPlayerBoundsTransform
                    )
                    .skipToLookaheadSize()
            )
            if (!compact) {
                Text(
                    text = stringResource(R.string.debug_component_lab_mini_player_subtitle),
                    color = MiuixTheme.colorScheme.onBackgroundVariant,
                    fontSize = 11.sp,
                    lineHeight = 13.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                LinearProgressIndicator(
                    progress = DebugBgmMiniPlayerProgress,
                    modifier = Modifier
                        .padding(top = 5.dp)
                        .fillMaxWidth(),
                    height = 3.dp,
                    colors = ProgressIndicatorDefaults.progressIndicatorColors(
                        foregroundColor = accent,
                        backgroundColor = MiuixTheme.colorScheme.onBackgroundVariant.copy(alpha = 0.22f)
                    )
                )
            }
        }
        Box(
            modifier = Modifier
                .size(playButtonSize)
                .clip(CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = appLucidePlayIcon(),
                contentDescription = stringResource(R.string.debug_component_lab_action_play),
                tint = MiuixTheme.colorScheme.onBackground,
                modifier = Modifier
                    .sharedElement(
                        sharedContentState = rememberSharedContentState("debug_bgm_play_icon"),
                        animatedVisibilityScope = animatedVisibilityScope,
                        boundsTransform = DebugBgmMiniPlayerBoundsTransform
                    )
                    .size(playIconSize)
            )
        }
        if (!compact) {
            DebugBgmInlineIcon(
                icon = appLucideSkipForwardIcon(),
                contentDescription = stringResource(R.string.debug_component_lab_action_next),
                tint = MiuixTheme.colorScheme.onBackground,
                size = 30.dp,
                iconSize = 21.dp
            )
        }
    }
}

private const val DebugBgmMiniPlayerProgress = 0.42f

private val DebugBgmMiniPlayerBoundsTransform = BoundsTransform { _, _ ->
    spring(
        dampingRatio = Spring.DampingRatioNoBouncy,
        stiffness = Spring.StiffnessMedium
    )
}

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
    const val Search = "search"
}

private data class DebugBgmDockTab(
    val key: String,
    val icon: ImageVector,
    val label: String
)
