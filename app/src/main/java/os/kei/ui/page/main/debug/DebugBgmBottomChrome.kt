package os.kei.ui.page.main.debug

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import os.kei.ui.page.main.widget.core.AppTypographyTokens
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
internal fun DebugBgmFloatingBottomChrome(
    accent: Color,
    collapseProgress: Float,
    selectedDockKey: String,
    onSelectedDockKeyChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    // Mirrors floating-tab-bar's layout language while Compose SharedTransition ABI compatibility catches up.
    val tabs = rememberDebugBgmDockTabs()
    val isInline = collapseProgress >= 0.58f
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (!isInline) {
            DebugBgmGlassSurface(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(70.dp),
                accent = accent,
                shape = ContinuousCapsule
            ) {
                DebugBgmMiniPlayer(
                    accent = accent,
                    compact = false,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            DebugBgmGlassSurface(
                modifier = Modifier
                    .weight(1f)
                    .height(if (isInline) 62.dp else 78.dp),
                accent = accent,
                shape = ContinuousCapsule
            ) {
                if (isInline) {
                    val selectedTab = tabs.firstOrNull { it.key == selectedDockKey } ?: tabs.last()
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 8.dp, vertical = 5.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        DebugBgmDockTabButton(
                            tab = selectedTab,
                            selected = true,
                            showLabel = true,
                            accent = accent,
                            onClick = { onSelectedDockKeyChange(selectedTab.key) },
                            modifier = Modifier.weight(0.72f)
                        )
                        DebugBgmMiniPlayer(
                            accent = accent,
                            compact = true,
                            modifier = Modifier.weight(1.28f)
                        )
                    }
                } else {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 10.dp, vertical = 5.dp),
                        horizontalArrangement = Arrangement.spacedBy(2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        tabs.forEach { tab ->
                            DebugBgmDockTabButton(
                                tab = tab,
                                selected = selectedDockKey == tab.key,
                                showLabel = true,
                                accent = accent,
                                onClick = { onSelectedDockKeyChange(tab.key) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
            DebugBgmGlassSurface(
                modifier = Modifier.size(if (isInline) 62.dp else 78.dp),
                accent = accent,
                shape = CircleShape,
                selected = selectedDockKey == DebugBgmDockKeys.Search
            ) {
                DebugBgmInlineIcon(
                    icon = appLucideSearchIcon(),
                    contentDescription = stringResource(R.string.debug_component_lab_nav_search),
                    tint = DebugBgmDockTint(
                        selected = selectedDockKey == DebugBgmDockKeys.Search,
                        accent = accent
                    ),
                    size = if (isInline) 50.dp else 62.dp,
                    iconSize = 25.dp,
                    onClick = { onSelectedDockKeyChange(DebugBgmDockKeys.Search) }
                )
            }
        }
    }
}

@Composable
private fun DebugBgmDockTabButton(
    tab: DebugBgmDockTab,
    selected: Boolean,
    showLabel: Boolean,
    accent: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .clip(ContinuousCapsule)
            .background(if (selected) accent.copy(alpha = 0.12f) else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(horizontal = 5.dp, vertical = 5.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = tab.icon,
            contentDescription = tab.label,
            tint = DebugBgmDockTint(selected = selected, accent = accent),
            modifier = Modifier.size(if (showLabel) 21.dp else 24.dp)
        )
        if (showLabel) {
            Text(
                text = tab.label,
                color = DebugBgmDockTint(selected = selected, accent = accent),
                fontSize = 10.sp,
                lineHeight = 12.sp,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun DebugBgmMiniPlayer(
    accent: Color,
    compact: Boolean,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .height(if (compact) 54.dp else 62.dp)
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
