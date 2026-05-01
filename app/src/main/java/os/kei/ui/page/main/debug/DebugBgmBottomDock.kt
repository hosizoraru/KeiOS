package os.kei.ui.page.main.debug

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.zIndex
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy
import com.kyant.backdrop.highlight.Highlight
import com.kyant.backdrop.shadow.InnerShadow
import com.kyant.backdrop.shadow.Shadow
import com.kyant.capsule.ContinuousCapsule
import os.kei.R
import os.kei.ui.page.main.os.appLucideGridIcon
import os.kei.ui.page.main.os.appLucideHomeIcon
import os.kei.ui.page.main.os.appLucideLibraryIcon
import os.kei.ui.page.main.os.appLucideRadioIcon
import os.kei.ui.page.main.widget.glass.UiPerformanceBudget
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
    backdrop: Backdrop? = null,
    onSelectedDockKeyChange: (String) -> Unit
) {
    val expanded = expandedProgress.coerceIn(0f, 1f)
    val compact = compactProgress.coerceIn(0f, 1f)
    val expandedEnabled = expanded > 0.54f
    val compactEnabled = compact > 0.54f
    val selectedIndex = tabs.indexOfFirst { it.key == selectedDockKey }.coerceAtLeast(0)
    val compactInteractionSource = remember { MutableInteractionSource() }
    val isDark = isSystemInDarkTheme()

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val safeTabCount = tabs.size.coerceAtLeast(1)
        val contentHorizontalPadding = 8.dp
        val contentVerticalPadding = 5.dp
        val selectedPillWidth = ((maxWidth - contentHorizontalPadding * 2f) / safeTabCount.toFloat())
            .coerceAtLeast(42.dp)
        val selectedPillOffset by animateDpAsState(
            targetValue = contentHorizontalPadding + selectedPillWidth * selectedIndex.toFloat(),
            animationSpec = tween(durationMillis = 220, easing = FastOutSlowInEasing),
            label = "debug_bgm_dock_selection_offset"
        )

        DebugBgmDockSelectionPill(
            modifier = Modifier
                .offset(x = selectedPillOffset, y = contentVerticalPadding)
                .width(selectedPillWidth)
                .height((maxHeight - contentVerticalPadding * 2f).coerceAtLeast(44.dp))
                .zIndex(0f)
                .graphicsLayer {
                    alpha = expanded
                    scaleX = 0.92f + 0.08f * expanded
                    scaleY = 0.92f + 0.08f * expanded
                },
            backdrop = backdrop,
            isDark = isDark
        )

        Row(
            modifier = Modifier
                .fillMaxSize()
                .zIndex(if (expanded >= compact) 1f else 0f)
                .graphicsLayer {
                    alpha = expanded
                    scaleX = 0.96f + 0.04f * expanded
                    scaleY = 0.96f + 0.04f * expanded
                }
                .padding(horizontal = contentHorizontalPadding, vertical = contentVerticalPadding),
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
                .zIndex(if (compact > expanded) 1f else 0f)
                .graphicsLayer {
                    alpha = compact
                    scaleX = 0.88f + 0.12f * compact
                    scaleY = 0.88f + 0.12f * compact
                }
                .clip(CircleShape)
                .then(
                    if (compactEnabled) {
                        Modifier.clickable(
                            interactionSource = compactInteractionSource,
                            indication = null
                        ) {
                            onSelectedDockKeyChange(compactTab.key)
                        }
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
private fun DebugBgmDockSelectionPill(
    backdrop: Backdrop?,
    isDark: Boolean,
    modifier: Modifier = Modifier
) {
    val neutralFill = if (isDark) {
        Color.White.copy(alpha = 0.10f)
    } else {
        Color.Black.copy(alpha = 0.08f)
    }
    val borderColor = if (isDark) {
        Color.White.copy(alpha = 0.16f)
    } else {
        Color.White.copy(alpha = 0.38f)
    }
    Box(
        modifier = modifier.then(
            if (backdrop != null) {
                Modifier.drawBackdrop(
                    backdrop = backdrop,
                    shape = { ContinuousCapsule },
                    effects = {
                        vibrancy()
                        blur((UiPerformanceBudget.backdropBlur * 0.52f).toPx())
                        lens(8.dp.toPx(), 12.dp.toPx(), true)
                    },
                    highlight = {
                        Highlight.Default.copy(alpha = if (isDark) 0.44f else 0.76f)
                    },
                    shadow = {
                        Shadow.Default.copy(color = Color.Black.copy(alpha = if (isDark) 0.14f else 0.08f))
                    },
                    innerShadow = {
                        InnerShadow(radius = 7.dp, alpha = if (isDark) 0.18f else 0.10f)
                    },
                    onDrawSurface = {
                        drawRect(neutralFill)
                    }
                )
            } else {
                Modifier
                    .clip(ContinuousCapsule)
                    .background(neutralFill, ContinuousCapsule)
            }
        )
    ) {
        Box(
            modifier = Modifier
                .matchParentSize()
                .clip(ContinuousCapsule)
                .background(
                    if (isDark) {
                        Color.White.copy(alpha = 0.03f)
                    } else {
                        Color.White.copy(alpha = 0.08f)
                    },
                    ContinuousCapsule
                )
        )
        Box(
            modifier = Modifier
                .matchParentSize()
                .clip(ContinuousCapsule)
                .border(1.dp, borderColor, ContinuousCapsule)
        )
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
    val interactionSource = remember { MutableInteractionSource() }
    Column(
        modifier = modifier
            .clip(ContinuousCapsule)
            .then(
                if (enabled) {
                    Modifier.clickable(
                        interactionSource = interactionSource,
                        indication = null,
                        onClick = onClick
                    )
                } else {
                    Modifier
                }
            )
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
