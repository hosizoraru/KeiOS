package os.kei.ui.page.main.debug

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
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
import os.kei.ui.page.main.os.appLucideRadioIcon
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
internal fun DebugBgmDockGroupContent(
    tabs: List<DebugBgmDockTab>,
    selectedDockKey: String,
    accent: Color,
    expandedProgress: Float,
    compactProgress: Float,
    onSelectedDockKeyChange: (String) -> Unit
) {
    val expanded = expandedProgress.coerceIn(0f, 1f)
    val compact = compactProgress.coerceIn(0f, 1f)
    val expandedEnabled = expanded > 0.54f
    val compactEnabled = compact > 0.54f
    Box(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    alpha = expanded
                    scaleX = 0.96f + 0.04f * expanded
                    scaleY = 0.96f + 0.04f * expanded
                }
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
                    enabled = expandedEnabled,
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
                .graphicsLayer {
                    alpha = compact
                    scaleX = 0.88f + 0.12f * compact
                    scaleY = 0.88f + 0.12f * compact
                }
                .clip(CircleShape)
                .clickable(enabled = compactEnabled) { onSelectedDockKeyChange(compactTab.key) },
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
    enabled: Boolean,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(ContinuousCapsule)
            .background(if (selected) accent.copy(alpha = 0.10f) else Color.Transparent)
            .clickable(enabled = enabled, onClick = onClick)
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
internal fun DebugBgmDockTabIcon(
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
internal fun rememberDebugBgmDockTabs(): List<DebugBgmDockTab> {
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

internal data class DebugBgmDockTab(
    val key: String,
    val icon: ImageVector,
    val label: String
)
