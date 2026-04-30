package os.kei.ui.page.main.debug

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDp
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.updateTransition
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
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
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.kyant.capsule.ContinuousCapsule
import os.kei.R
import os.kei.ui.page.main.os.appLucideSearchIcon
import os.kei.ui.page.main.widget.chrome.AppChromeTokens
import os.kei.ui.page.main.widget.motion.LocalTransitionAnimationsEnabled
import os.kei.ui.page.main.widget.motion.resolvedMotionDuration
import top.yukonga.miuix.kmp.theme.MiuixTheme

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
    val animationsEnabled = LocalTransitionAnimationsEnabled.current
    val transition = updateTransition(
        targetState = scrollState.isCompact,
        label = "debug_bgm_bottom_chrome"
    )
    val animationSpec = tween<Dp>(
        durationMillis = resolvedMotionDuration(DebugBgmBottomChromeSizeMotionMs, animationsEnabled),
        easing = FastOutSlowInEasing
    )
    val floatAnimationSpec = tween<Float>(
        durationMillis = resolvedMotionDuration(DebugBgmBottomChromeFadeMotionMs, animationsEnabled),
        easing = FastOutSlowInEasing
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
    val expandedProgress = expandedAlpha.coerceIn(0f, 1f)
    val compactProgress = compactAlpha.coerceIn(0f, 1f)

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
                expandedProgress = expandedProgress,
                compactProgress = compactProgress,
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
                expandedProgress = expandedProgress,
                compactProgress = compactProgress,
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

private fun boundedDp(
    value: Dp,
    min: Dp,
    max: Dp
): Dp = value.value.coerceIn(min.value, max.value).dp

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
private const val DebugBgmBottomChromeSizeMotionMs = 360
private const val DebugBgmBottomChromeFadeMotionMs = 240
