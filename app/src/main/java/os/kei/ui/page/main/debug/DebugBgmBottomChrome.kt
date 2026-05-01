package os.kei.ui.page.main.debug

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDp
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.updateTransition
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp as lerpFloat
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy
import com.kyant.backdrop.highlight.Highlight
import com.kyant.backdrop.shadow.Shadow
import com.kyant.capsule.ContinuousCapsule
import os.kei.R
import os.kei.ui.page.main.os.appLucideSearchIcon
import os.kei.ui.page.main.widget.chrome.AppChromeTokens
import os.kei.ui.page.main.widget.core.AppTypographyTokens
import os.kei.ui.page.main.widget.glass.UiPerformanceBudget
import os.kei.ui.page.main.widget.motion.LocalTransitionAnimationsEnabled
import os.kei.ui.page.main.widget.motion.appMotionFloatState
import os.kei.ui.page.main.widget.motion.resolvedMotionDuration
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
internal fun DebugBgmFloatingBottomChrome(
    accent: Color,
    scrollState: DebugBgmBottomChromeScrollState,
    currentTrackTitle: String,
    isPlaying: Boolean,
    playbackProgress: Float,
    onPlaybackProgressChange: (Float) -> Unit,
    onPlaybackProgressChangeFinished: (Float) -> Unit,
    onPlaybackSliderInteractionChanged: (Boolean) -> Unit,
    onPlayPauseClick: () -> Unit,
    onPreviousClick: () -> Unit,
    onNextClick: () -> Unit,
    searchVisible: Boolean,
    searchInputActive: Boolean,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onSearchInputActiveChange: (Boolean) -> Unit,
    selectedDockKey: String,
    onSelectedDockKeyChange: (String) -> Unit,
    onCompactDockClick: () -> Unit,
    onSearchClick: () -> Unit,
    backdrop: Backdrop? = null,
    modifier: Modifier = Modifier
) {
    val tabs = rememberDebugBgmDockTabs()
    val animationsEnabled = LocalTransitionAnimationsEnabled.current
    val miniPlayerInteractionSource = remember { MutableInteractionSource() }
    val dockSurfaceInteractionSource = remember { MutableInteractionSource() }
    val searchFocusRequester = remember { FocusRequester() }
    val searchMode = when {
        searchInputActive -> DebugBgmBottomChromeMode.SearchInput
        searchVisible -> DebugBgmBottomChromeMode.SearchExpanded
        scrollState.isCompact -> DebugBgmBottomChromeMode.Compact
        else -> DebugBgmBottomChromeMode.Expanded
    }
    val transition = updateTransition(
        targetState = searchMode,
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
    ) { mode ->
        when (mode) {
            DebugBgmBottomChromeMode.SearchInput,
            DebugBgmBottomChromeMode.Compact -> DebugBgmCompactChromeHeight
            DebugBgmBottomChromeMode.Expanded,
            DebugBgmBottomChromeMode.SearchExpanded -> DebugBgmExpandedChromeHeight
        }
    }
    val tabGroupHeight by transition.animateDp(
        transitionSpec = { animationSpec },
        label = "debug_bgm_tab_height"
    ) {
        DebugBgmExpandedDockHeight
    }
    val tabGroupY by transition.animateDp(
        transitionSpec = { animationSpec },
        label = "debug_bgm_tab_y"
    ) { mode ->
        when (mode) {
            DebugBgmBottomChromeMode.Expanded,
            DebugBgmBottomChromeMode.SearchExpanded -> DebugBgmExpandedDockY
            DebugBgmBottomChromeMode.Compact,
            DebugBgmBottomChromeMode.SearchInput -> DebugBgmCompactControlInset
        }
    }
    val miniPlayerHeight by transition.animateDp(
        transitionSpec = { animationSpec },
        label = "debug_bgm_mini_height"
    ) { mode ->
        if (mode == DebugBgmBottomChromeMode.SearchInput) {
            0.dp
        } else {
            DebugBgmExpandedMiniHeight
        }
    }
    val miniPlayerY by transition.animateDp(
        transitionSpec = { animationSpec },
        label = "debug_bgm_mini_y"
    ) {
        DebugBgmCompactMiniY
    }
    val searchSize by transition.animateDp(
        transitionSpec = { animationSpec },
        label = "debug_bgm_search_size"
    ) {
        DebugBgmExpandedDockHeight
    }
    val searchY by transition.animateDp(
        transitionSpec = { animationSpec },
        label = "debug_bgm_search_y"
    ) { mode ->
        when (mode) {
            DebugBgmBottomChromeMode.Expanded,
            DebugBgmBottomChromeMode.SearchExpanded -> DebugBgmExpandedDockY
            DebugBgmBottomChromeMode.Compact,
            DebugBgmBottomChromeMode.SearchInput -> DebugBgmCompactControlInset
        }
    }
    val dockExpandedAlpha by transition.animateFloat(
        transitionSpec = { floatAnimationSpec },
        label = "debug_bgm_expanded_alpha"
    ) { mode ->
        when (mode) {
            DebugBgmBottomChromeMode.Expanded -> 1f
            else -> 0f
        }
    }
    val dockCompactAlpha by transition.animateFloat(
        transitionSpec = { floatAnimationSpec },
        label = "debug_bgm_compact_alpha"
    ) { mode ->
        when (mode) {
            DebugBgmBottomChromeMode.Expanded -> 0f
            else -> 1f
        }
    }
    val miniExpandedAlpha by transition.animateFloat(
        transitionSpec = { floatAnimationSpec },
        label = "debug_bgm_mini_expanded_alpha"
    ) { mode ->
        when (mode) {
            DebugBgmBottomChromeMode.Compact -> 0f
            else -> 1f
        }
    }
    val miniCompactAlpha by transition.animateFloat(
        transitionSpec = { floatAnimationSpec },
        label = "debug_bgm_mini_compact_alpha"
    ) { mode ->
        when (mode) {
            DebugBgmBottomChromeMode.Compact -> 1f
            else -> 0f
        }
    }
    val miniPlayerAlpha by transition.animateFloat(
        transitionSpec = { floatAnimationSpec },
        label = "debug_bgm_mini_alpha"
    ) { mode ->
        if (mode == DebugBgmBottomChromeMode.SearchInput) 0f else 1f
    }
    val dockExpandedProgress = dockExpandedAlpha.coerceIn(0f, 1f)
    val dockCompactProgress = dockCompactAlpha.coerceIn(0f, 1f)
    val miniExpandedProgress = miniExpandedAlpha.coerceIn(0f, 1f)
    val miniCompactProgress = miniCompactAlpha.coerceIn(0f, 1f)
    val searchFieldVisible = searchMode.isSearchMode

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .height(containerHeight)
    ) {
        val tabGroupExpandedWidth = (maxWidth - DebugBgmExpandedSearchSpacing - DebugBgmExpandedDockHeight)
            .coerceAtLeast(260.dp)
        val compactMiniWidth = boundedDp(
            value = maxWidth - (DebugBgmCompactControlSize * 2f) - (DebugBgmCompactMiniGap * 2f),
            min = 190.dp,
            max = DebugBgmCompactMiniMaxWidth
        )
        val searchFieldWidth = (maxWidth - DebugBgmCompactControlSize - DebugBgmSearchFieldSpacing)
            .coerceAtLeast(196.dp)
        val miniPlayerWidth by transition.animateDp(
            transitionSpec = { animationSpec },
            label = "debug_bgm_mini_width"
        ) { mode ->
            when (mode) {
                DebugBgmBottomChromeMode.Compact -> compactMiniWidth
                DebugBgmBottomChromeMode.SearchInput -> 0.dp
                DebugBgmBottomChromeMode.Expanded,
                DebugBgmBottomChromeMode.SearchExpanded -> maxWidth
            }
        }
        val miniPlayerX by transition.animateDp(
            transitionSpec = { animationSpec },
            label = "debug_bgm_mini_x"
        ) { mode ->
            if (mode == DebugBgmBottomChromeMode.Compact) {
                (maxWidth - compactMiniWidth) / 2f
            } else {
                0.dp
            }
        }
        val tabGroupWidth by transition.animateDp(
            transitionSpec = { animationSpec },
            label = "debug_bgm_tab_width"
        ) { mode ->
            when (mode) {
                DebugBgmBottomChromeMode.Expanded -> tabGroupExpandedWidth
                DebugBgmBottomChromeMode.Compact,
                DebugBgmBottomChromeMode.SearchExpanded,
                DebugBgmBottomChromeMode.SearchInput -> DebugBgmCompactControlSize
            }
        }
        val searchWidth by transition.animateDp(
            transitionSpec = { animationSpec },
            label = "debug_bgm_search_width"
        ) { mode ->
            if (mode.isSearchMode) searchFieldWidth else DebugBgmExpandedDockHeight
        }
        val searchX by transition.animateDp(
            transitionSpec = { animationSpec },
            label = "debug_bgm_search_x"
        ) { mode ->
            if (mode.isSearchMode) {
                DebugBgmCompactControlSize + DebugBgmSearchFieldSpacing
            } else {
                maxWidth - DebugBgmExpandedDockHeight
            }
        }

        if (miniPlayerHeight > 1.dp && miniPlayerWidth > 1.dp) {
            DebugBgmBottomSurface(
                modifier = Modifier
                    .offset(x = miniPlayerX, y = miniPlayerY)
                    .width(miniPlayerWidth)
                    .height(miniPlayerHeight)
                    .graphicsLayer { alpha = miniPlayerAlpha },
                shape = ContinuousCapsule,
                backdrop = backdrop,
                interactionSource = miniPlayerInteractionSource,
                consumeTouches = true
            ) {
                DebugBgmMiniPlayer(
                    accent = accent,
                    currentTrackTitle = currentTrackTitle,
                    isPlaying = isPlaying,
                    playbackProgress = playbackProgress,
                    onPlaybackProgressChange = onPlaybackProgressChange,
                    onPlaybackProgressChangeFinished = onPlaybackProgressChangeFinished,
                    onPlaybackSliderInteractionChanged = onPlaybackSliderInteractionChanged,
                    expandedProgress = miniExpandedProgress,
                    compactProgress = miniCompactProgress,
                    onPlayPauseClick = onPlayPauseClick,
                    onPreviousClick = onPreviousClick,
                    onNextClick = onNextClick,
                    controlInteractionSource = miniPlayerInteractionSource,
                    backdrop = backdrop,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }

        DebugBgmBottomSurface(
            modifier = Modifier
                .offset(x = 0.dp, y = tabGroupY)
                .width(tabGroupWidth)
                .height(tabGroupHeight),
            shape = ContinuousCapsule,
            backdrop = backdrop,
            interactionSource = dockSurfaceInteractionSource,
            clipContent = false
        ) {
            DebugBgmDockGroupContent(
                tabs = tabs,
                selectedDockKey = selectedDockKey,
                accent = accent,
                expandedProgress = dockExpandedProgress,
                compactProgress = dockCompactProgress,
                backdrop = backdrop,
                compactInteractionSource = dockSurfaceInteractionSource,
                onSelectedDockKeyChange = onSelectedDockKeyChange,
                onCompactDockClick = onCompactDockClick
            )
        }

        DebugBgmBottomSurface(
            modifier = Modifier
                .offset(x = searchX, y = searchY)
                .width(searchWidth)
                .height(searchSize),
            shape = if (searchFieldVisible) ContinuousCapsule else CircleShape,
            backdrop = backdrop,
            onClick = if (searchFieldVisible) null else onSearchClick
        ) {
            if (searchFieldVisible) {
                DebugBgmBottomSearchField(
                    query = searchQuery,
                    onQueryChange = onSearchQueryChange,
                    focusRequester = searchFocusRequester,
                    onFocusActiveChange = onSearchInputActiveChange,
                    accent = accent,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
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
}

@Composable
private fun DebugBgmBottomSurface(
    modifier: Modifier,
    shape: Shape,
    backdrop: Backdrop? = null,
    onClick: (() -> Unit)? = null,
    interactionSource: MutableInteractionSource? = null,
    clipContent: Boolean = true,
    consumeTouches: Boolean = false,
    content: @Composable () -> Unit
) {
    val resolvedInteractionSource = interactionSource ?: remember { MutableInteractionSource() }
    val pressed by resolvedInteractionSource.collectIsPressedAsState()
    val pressProgress by appMotionFloatState(
        targetValue = if (pressed) 1f else 0f,
        durationMillis = DebugBgmBottomSurfacePressMotionMs,
        label = "debug_bgm_bottom_surface_press"
    )
    val density = LocalDensity.current
    val isDark = isSystemInDarkTheme()
    val surfaceColor = MiuixTheme.colorScheme.surfaceContainer.copy(alpha = if (isDark) 0.20f else 0.40f)
    val overlayColor = if (isDark) {
        Color.White.copy(alpha = 0.04f)
    } else {
        Color.White.copy(alpha = 0.08f)
    }
    val borderColor = if (isDark) {
        Color.White.copy(alpha = 0.18f)
    } else {
        Color.White.copy(alpha = 0.54f)
    }
    Box(
        modifier = modifier
            .graphicsLayer {
                translationY = -with(density) { 1.25.dp.toPx() } * pressProgress
                scaleX = lerpFloat(1f, 1.010f, pressProgress)
                scaleY = lerpFloat(1f, 0.992f, pressProgress)
            },
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .then(
                    if (backdrop != null) {
                        Modifier.drawBackdrop(
                            backdrop = backdrop,
                            shape = { shape },
                            effects = {
                                vibrancy()
                                blur(UiPerformanceBudget.backdropBlur.toPx())
                                lens(
                                    (UiPerformanceBudget.backdropLens *
                                        (0.90f + 0.08f * pressProgress)).toPx(),
                                    (UiPerformanceBudget.backdropLens *
                                        (0.90f + 0.10f * pressProgress)).toPx()
                                )
                            },
                            highlight = {
                                Highlight.Default.copy(
                                    alpha = (if (isDark) 0.46f else 0.82f) + 0.06f * pressProgress
                                )
                            },
                            shadow = {
                                Shadow.Default.copy(
                                    color = Color.Black.copy(alpha = if (isDark) 0.20f else 0.10f)
                                )
                            },
                            onDrawSurface = { drawRect(surfaceColor) }
                        )
                    } else {
                        Modifier
                            .clip(shape)
                            .background(MiuixTheme.colorScheme.surfaceContainer.copy(alpha = 0.96f), shape)
                    }
                )
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(shape)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            overlayColor,
                            overlayColor.copy(alpha = overlayColor.alpha * 0.52f)
                        )
                    )
                )
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(shape)
                .border(1.dp, borderColor, shape)
        )
        if (consumeTouches && onClick == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(shape)
                    .clickable(
                        interactionSource = resolvedInteractionSource,
                        indication = null,
                        onClick = {}
                    )
            )
        }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .then(if (clipContent) Modifier.clip(shape) else Modifier)
                .then(
                    when {
                        onClick != null -> Modifier.clickable(
                            interactionSource = resolvedInteractionSource,
                            indication = null,
                            onClick = onClick
                        )
                        else -> Modifier
                    }
                ),
            contentAlignment = Alignment.Center
        ) {
            content()
        }
    }
}

@Composable
private fun DebugBgmBottomSearchField(
    query: String,
    onQueryChange: (String) -> Unit,
    focusRequester: FocusRequester,
    onFocusActiveChange: (Boolean) -> Unit,
    accent: Color,
    modifier: Modifier = Modifier
) {
    val focusManager = LocalFocusManager.current
    val placeholder = stringResource(R.string.debug_component_lab_search_placeholder)
    val contentColor = MiuixTheme.colorScheme.onBackground
    val placeholderColor = MiuixTheme.colorScheme.onBackgroundVariant.copy(alpha = 0.78f)
    val interactionSource = remember { MutableInteractionSource() }
    val textStyle = TextStyle(
        color = contentColor,
        fontSize = AppTypographyTokens.CardHeader.fontSize,
        lineHeight = AppTypographyTokens.CardHeader.lineHeight,
        platformStyle = PlatformTextStyle(includeFontPadding = false)
    )
    Row(
        modifier = modifier
            .clip(ContinuousCapsule)
            .clickable(
                interactionSource = interactionSource,
                indication = null
            ) {
                onFocusActiveChange(true)
                focusRequester.requestFocus()
            }
            .padding(horizontal = 18.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        DebugBgmDockTabIcon(
            icon = appLucideSearchIcon(),
            label = stringResource(R.string.debug_component_lab_nav_search),
            selected = true,
            accent = accent,
            iconSize = 25.dp
        )
        BasicTextField(
            value = query,
            onValueChange = onQueryChange,
            singleLine = true,
            textStyle = textStyle,
            cursorBrush = SolidColor(accent),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(
                onSearch = {
                    onFocusActiveChange(false)
                    focusManager.clearFocus()
                }
            ),
            modifier = Modifier
                .weight(1f)
                .defaultMinSize(minHeight = 24.dp)
                .focusRequester(focusRequester)
                .onFocusChanged { state ->
                    onFocusActiveChange(state.isFocused)
                },
            decorationBox = { innerTextField ->
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.CenterStart
                ) {
                    if (query.isBlank()) {
                        BasicText(
                            text = placeholder,
                            style = textStyle.copy(color = placeholderColor),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    innerTextField()
                }
            }
        )
    }
}

private fun boundedDp(
    value: Dp,
    min: Dp,
    max: Dp
): Dp = value.value.coerceIn(min.value, max.value).dp

private enum class DebugBgmBottomChromeMode {
    Expanded,
    Compact,
    SearchExpanded,
    SearchInput;

    val isSearchMode: Boolean
        get() = this == SearchExpanded || this == SearchInput
}

private val DebugBgmChromeControlHeight = AppChromeTokens.floatingBottomBarOuterHeight
private val DebugBgmChromeStackGap = AppChromeTokens.pageSectionGap
private val DebugBgmExpandedChromeHeight = DebugBgmChromeControlHeight * 2f + DebugBgmChromeStackGap
private val DebugBgmCompactChromeHeight = DebugBgmChromeControlHeight
private val DebugBgmExpandedMiniHeight = DebugBgmChromeControlHeight
private val DebugBgmCompactMiniY = 0.dp
private val DebugBgmExpandedDockHeight = DebugBgmChromeControlHeight
private val DebugBgmExpandedDockY = DebugBgmChromeControlHeight + DebugBgmChromeStackGap
private val DebugBgmExpandedSearchSpacing = AppChromeTokens.pageSectionGap
private val DebugBgmSearchFieldSpacing = 8.dp
private val DebugBgmCompactMiniGap = DebugBgmSearchFieldSpacing
private val DebugBgmCompactMiniMaxWidth = 288.dp
private val DebugBgmCompactControlSize = DebugBgmChromeControlHeight
private val DebugBgmCompactControlInset = 0.dp
private const val DebugBgmBottomChromeSizeMotionMs = 360
private const val DebugBgmBottomChromeFadeMotionMs = 240
private const val DebugBgmBottomSurfacePressMotionMs = 120
