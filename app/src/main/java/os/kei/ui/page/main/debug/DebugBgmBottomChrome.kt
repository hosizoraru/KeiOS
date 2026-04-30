package os.kei.ui.page.main.debug

import androidx.compose.foundation.background
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
import os.kei.ui.page.main.os.appLucideGridIcon
import os.kei.ui.page.main.os.appLucideHomeIcon
import os.kei.ui.page.main.os.appLucideLibraryIcon
import os.kei.ui.page.main.os.appLucideMusicIcon
import os.kei.ui.page.main.os.appLucidePauseIcon
import os.kei.ui.page.main.os.appLucidePlayIcon
import os.kei.ui.page.main.os.appLucideRadioIcon
import os.kei.ui.page.main.os.appLucideSearchIcon
import os.kei.ui.page.main.widget.core.AppTypographyTokens
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
internal fun DebugBgmFloatingBottomChrome(
    accent: Color,
    isInline: Boolean,
    selectedDockKey: String,
    onSelectedDockKeyChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val tabs = rememberDebugBgmDockTabs()
    FloatingTabBar(
        isInline = isInline,
        selectedTabKey = selectedDockKey,
        modifier = modifier.fillMaxWidth(),
        inlineAccessory = { accessoryModifier, _ ->
            DebugBgmMiniPlayer(
                accent = accent,
                compact = true,
                modifier = accessoryModifier.fillMaxSize()
            )
        },
        expandedAccessory = { accessoryModifier, _ ->
            DebugBgmMiniPlayer(
                accent = accent,
                compact = false,
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
            tabBarContentPadding = PaddingValues(horizontal = 6.dp, vertical = 4.dp),
            tabInlineContentPadding = PaddingValues(horizontal = 13.dp, vertical = 8.dp),
            tabExpandedContentPadding = PaddingValues(horizontal = 16.dp, vertical = 7.dp),
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
    iconSize: Dp = 22.dp
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
        fontSize = 10.sp,
        lineHeight = 12.sp,
        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
    )
}

@Composable
private fun DebugBgmMiniPlayer(
    accent: Color,
    compact: Boolean,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .padding(horizontal = if (compact) 10.dp else 14.dp, vertical = if (compact) 6.dp else 8.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(if (compact) 36.dp else 42.dp)
                .clip(RoundedCornerShape(if (compact) 9.dp else 11.dp))
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
                modifier = Modifier.size(if (compact) 19.dp else 22.dp)
            )
        }
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            Text(
                text = stringResource(R.string.debug_component_lab_mini_player_title),
                color = MiuixTheme.colorScheme.onBackground,
                fontSize = AppTypographyTokens.Supporting.fontSize,
                lineHeight = AppTypographyTokens.Supporting.lineHeight,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
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
            }
        }
        DebugBgmInlineIcon(
            icon = appLucidePlayIcon(),
            contentDescription = stringResource(R.string.debug_component_lab_action_play),
            tint = MiuixTheme.colorScheme.onBackground,
            size = if (compact) 30.dp else 34.dp,
            iconSize = 23.dp
        )
        if (!compact) {
            DebugBgmInlineIcon(
                icon = appLucidePauseIcon(),
                contentDescription = stringResource(R.string.debug_component_lab_action_pause),
                tint = MiuixTheme.colorScheme.onBackground,
                size = 34.dp,
                iconSize = 22.dp
            )
        }
    }
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
